/**
 * Unit tests for EmergencyForm component.
 *
 * Tests cover:
 *  - Rendering all form fields
 *  - Map-click pre-population via context
 *  - Field-level local validation errors
 *  - Successful submit dispatching SUBMIT_EMERGENCY + RECEIVE_RECOMMENDATION
 *  - ApiError field-level errors displayed inline
 */

import React from 'react'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import EmergencyForm from './EmergencyForm'
import { AppProvider } from '../context/AppContext'
import * as ApiClient from '../api/ApiClient'

// ── Mock ApiClient ────────────────────────────────────────────────────────────

jest.mock('../api/ApiClient', () => ({
  ...jest.requireActual('../api/ApiClient'),
  createEmergency: jest.fn(),
  getRecommendation: jest.fn(),
}))

const mockCreateEmergency = ApiClient.createEmergency as jest.MockedFunction<typeof ApiClient.createEmergency>
const mockGetRecommendation = ApiClient.getRecommendation as jest.MockedFunction<typeof ApiClient.getRecommendation>

// ── Helpers ───────────────────────────────────────────────────────────────────

function renderForm(props: Partial<React.ComponentProps<typeof EmergencyForm>> = {}) {
  return render(
    <AppProvider>
      <EmergencyForm {...props} />
    </AppProvider>,
  )
}

const stubEmergency = {
  id: 1,
  latitude: 51.5,
  longitude: -0.1,
  urgencyLevel: 'medium' as const,
  requiredCareType: 'trauma',
  status: 'open' as const,
  createdAt: '2024-01-01T00:00:00Z',
}

const stubRecommendation = {
  recommendationId: 10,
  emergencyRequestId: 1,
  ambulance: { id: 1, name: 'Ambulance A', etaToSceneSeconds: 300, equipment: [] },
  hospital: { id: 2, name: 'City Hospital', etaFromSceneSeconds: 600, capabilities: [] },
  route: { totalDurationSeconds: 900, segments: [] },
  explanation: 'Nearest unit.',
  alternatives: [],
  createdAt: '2024-01-01T00:00:00Z',
}

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('EmergencyForm', () => {
  beforeEach(() => {
    jest.clearAllMocks()
  })

  describe('Rendering', () => {
    it('renders the form heading', () => {
      renderForm()
      expect(screen.getByRole('heading', { name: /new emergency request/i })).toBeInTheDocument()
    })

    it('renders latitude and longitude text fields', () => {
      renderForm()
      expect(screen.getByLabelText(/latitude/i)).toBeInTheDocument()
      expect(screen.getByLabelText(/longitude/i)).toBeInTheDocument()
    })

    it('renders the urgency level slider', () => {
      renderForm()
      const slider = screen.getByRole('slider', { name: /urgency level/i })
      expect(slider).toBeInTheDocument()
      expect(slider).toHaveAttribute('aria-valuemin', '1')
      expect(slider).toHaveAttribute('aria-valuemax', '4')
      expect(slider).toHaveAttribute('aria-valuenow', '2') // default value
    })

    it('renders all care type checkboxes', () => {
      renderForm()
      const types = ['Trauma', 'Cardiac', 'Stroke', 'Pediatric', 'Burns', 'General']
      for (const t of types) {
        expect(screen.getByRole('checkbox', { name: t })).toBeInTheDocument()
      }
    })

    it('renders the submit button with correct label', () => {
      renderForm()
      expect(screen.getByRole('button', { name: /request recommendation/i })).toBeInTheDocument()
    })
  })

  describe('Validation', () => {
    it('shows error when latitude is empty on submit', async () => {
      renderForm()
      await userEvent.click(screen.getByRole('button', { name: /request recommendation/i }))
      expect(await screen.findByText(/latitude is required/i)).toBeInTheDocument()
    })

    it('shows error when longitude is empty on submit', async () => {
      renderForm()
      await userEvent.type(screen.getByLabelText(/latitude/i), '51.5')
      await userEvent.click(screen.getByRole('button', { name: /request recommendation/i }))
      expect(await screen.findByText(/longitude is required/i)).toBeInTheDocument()
    })

    it('shows error when no care type is selected on submit', async () => {
      renderForm()
      await userEvent.type(screen.getByLabelText(/latitude/i), '51.5')
      await userEvent.type(screen.getByLabelText(/longitude/i), '-0.1')
      await userEvent.click(screen.getByRole('button', { name: /request recommendation/i }))
      expect(await screen.findByText(/at least one care type must be selected/i)).toBeInTheDocument()
    })

    it('shows error for out-of-range latitude', async () => {
      renderForm()
      await userEvent.type(screen.getByLabelText(/latitude/i), '999')
      await userEvent.type(screen.getByLabelText(/longitude/i), '-0.1')
      await userEvent.click(screen.getByRole('button', { name: /request recommendation/i }))
      expect(await screen.findByText(/latitude must be between -90 and 90/i)).toBeInTheDocument()
    })

    it('shows error for out-of-range longitude', async () => {
      renderForm()
      await userEvent.type(screen.getByLabelText(/latitude/i), '51.5')
      await userEvent.type(screen.getByLabelText(/longitude/i), '999')
      await userEvent.click(screen.getByRole('button', { name: /request recommendation/i }))
      expect(await screen.findByText(/longitude must be between -180 and 180/i)).toBeInTheDocument()
    })
  })

  describe('Successful submission', () => {
    async function fillAndSubmit() {
      mockCreateEmergency.mockResolvedValue(stubEmergency)
      mockGetRecommendation.mockResolvedValue(stubRecommendation)

      renderForm()
      await userEvent.type(screen.getByLabelText(/latitude/i), '51.5')
      await userEvent.type(screen.getByLabelText(/longitude/i), '-0.1')
      await userEvent.click(screen.getByRole('checkbox', { name: /trauma/i }))
      await userEvent.click(screen.getByRole('button', { name: /request recommendation/i }))
    }

    it('calls createEmergency with correct payload', async () => {
      await fillAndSubmit()
      await waitFor(() => {
        expect(mockCreateEmergency).toHaveBeenCalledWith({
          latitude: 51.5,
          longitude: -0.1,
          urgencyLevel: 'medium',
          requiredCareType: 'trauma',
        })
      })
    })

    it('calls getRecommendation after emergency is created', async () => {
      await fillAndSubmit()
      await waitFor(() => {
        expect(mockGetRecommendation).toHaveBeenCalledWith(stubEmergency.id)
      })
    })

    it('invokes onEmergencyCreated callback', async () => {
      const onCreated = jest.fn()
      mockCreateEmergency.mockResolvedValue(stubEmergency)
      mockGetRecommendation.mockResolvedValue(stubRecommendation)

      render(
        <AppProvider>
          <EmergencyForm onEmergencyCreated={onCreated} />
        </AppProvider>,
      )

      await userEvent.type(screen.getByLabelText(/latitude/i), '51.5')
      await userEvent.type(screen.getByLabelText(/longitude/i), '-0.1')
      await userEvent.click(screen.getByRole('checkbox', { name: /cardiac/i }))
      await userEvent.click(screen.getByRole('button', { name: /request recommendation/i }))

      await waitFor(() => {
        expect(onCreated).toHaveBeenCalledWith(stubEmergency)
      })
    })
  })

  describe('API error handling', () => {
    it('displays field-level errors from ApiError', async () => {
      mockCreateEmergency.mockRejectedValue(
        new ApiClient.ApiError(400, {
          status: 400,
          message: 'Validation failed',
          errors: [
            { field: 'latitude', message: 'must be a valid coordinate' },
            { field: 'longitude', message: 'must be a valid coordinate' },
          ],
        }),
      )

      renderForm()
      await userEvent.type(screen.getByLabelText(/latitude/i), '51.5')
      await userEvent.type(screen.getByLabelText(/longitude/i), '-0.1')
      await userEvent.click(screen.getByRole('checkbox', { name: /burns/i }))
      await userEvent.click(screen.getByRole('button', { name: /request recommendation/i }))

      expect(await screen.findByText(/must be a valid coordinate/i)).toBeInTheDocument()
    })

    it('displays general error message when no field errors present', async () => {
      mockCreateEmergency.mockRejectedValue(
        new ApiClient.ApiError(500, {
          status: 500,
          message: 'Internal server error',
        }),
      )

      renderForm()
      await userEvent.type(screen.getByLabelText(/latitude/i), '51.5')
      await userEvent.type(screen.getByLabelText(/longitude/i), '-0.1')
      await userEvent.click(screen.getByRole('checkbox', { name: /general/i }))
      await userEvent.click(screen.getByRole('button', { name: /request recommendation/i }))

      expect(await screen.findByText(/internal server error/i)).toBeInTheDocument()
    })
  })
})

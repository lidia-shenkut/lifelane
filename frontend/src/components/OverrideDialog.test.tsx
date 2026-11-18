// OverrideDialog.test.tsx
import React from 'react'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { OverrideDialog } from './OverrideDialog'
import { AppProvider } from '../context/AppContext'
import * as ApiClient from '../api/ApiClient'

jest.mock('../api/ApiClient')

// Helper to render with a pre-populated context
function renderWithContext(ui: React.ReactElement) {
  // We need ambulances and hospitals in context for the dropdowns.
  // The AppProvider starts with empty arrays, so we wrap and dispatch.
  return render(<AppProvider>{ui}</AppProvider>)
}

describe('OverrideDialog', () => {
  const defaultProps = {
    open: true,
    emergencyId: 42,
    onClose: jest.fn(),
    onSubmitted: jest.fn(),
  }

  it('renders dialog with title when open', () => {
    renderWithContext(<OverrideDialog {...defaultProps} />)
    expect(screen.getByText(/Override Recommendation/i)).toBeInTheDocument()
  })

  it('renders ambulance and hospital select dropdowns', () => {
    renderWithContext(<OverrideDialog {...defaultProps} />)
    expect(screen.getByLabelText(/Select ambulance for override/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/Select hospital for override/i)).toBeInTheDocument()
  })

  it('renders optional comment text field', () => {
    renderWithContext(<OverrideDialog {...defaultProps} />)
    expect(screen.getByLabelText(/Dispatcher comment/i)).toBeInTheDocument()
  })

  it('shows validation error when submitting without selections', async () => {
    renderWithContext(<OverrideDialog {...defaultProps} />)
    fireEvent.click(screen.getByRole('button', { name: /Submit override decision/i }))
    await waitFor(() => {
      expect(screen.getByText(/Please select an ambulance/i)).toBeInTheDocument()
    })
  })

  it('does not render dialog when closed', () => {
    renderWithContext(<OverrideDialog {...defaultProps} open={false} />)
    expect(screen.queryByText(/Override Recommendation/i)).not.toBeInTheDocument()
  })
})

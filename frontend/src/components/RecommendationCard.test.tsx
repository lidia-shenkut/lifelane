// RecommendationCard.test.tsx
import React from 'react'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { RecommendationCard } from './RecommendationCard'
import { AppProvider } from '../context/AppContext'
import * as ApiClient from '../api/ApiClient'

jest.mock('../api/ApiClient')

type MockRecommendation = {
  ambulance: { name: string; etaToSceneSeconds: number; equipment: string[] }
  hospital: { name: string; etaFromSceneSeconds: number; capabilities: string[] }
  route: { totalDurationSeconds: number; segments: any[] }
  explanation: string
  alternatives: any[]
}

const mockRecommendation: MockRecommendation = {
  ambulance: { name: 'Ambulance A', etaToSceneSeconds: 300, equipment: ['defib'] },
  hospital: { name: 'Hospital X', etaFromSceneSeconds: 600, capabilities: ['cardiology'] },
  route: { totalDurationSeconds: 900, segments: [] },
  explanation: 'Best option',
  alternatives: [],
}

describe('RecommendationCard', () => {
  it('renders recommendation details', () => {
    render(
      <AppProvider>
        <RecommendationCard recommendation={mockRecommendation as any} emergencyId={1} />
      </AppProvider>
    )
    expect(screen.getByText(/Recommended Ambulance/i)).toBeInTheDocument()
    expect(screen.getByText('Ambulance A')).toBeInTheDocument()
    expect(screen.getByText(/Recommended Hospital/i)).toBeInTheDocument()
    expect(screen.getByText('Hospital X')).toBeInTheDocument()
  })

  it('calls recordDecision on Accept', async () => {
    const mockRecord = ApiClient.recordDecision as jest.MockedFunction<typeof ApiClient.recordDecision>
    mockRecord.mockResolvedValueOnce(undefined)
    render(
      <AppProvider>
        <RecommendationCard recommendation={mockRecommendation as any} emergencyId={1} />
      </AppProvider>
    )
    fireEvent.click(screen.getByRole('button', { name: /Accept Recommendation/i }))
    await waitFor(() => expect(mockRecord).toHaveBeenCalledWith(1, { action: 'accept' }))
  })

  it('opens OverrideDialog when Override clicked', () => {
    render(
      <AppProvider>
        <RecommendationCard recommendation={mockRecommendation as any} emergencyId={1} />
      </AppProvider>
    )
    fireEvent.click(screen.getByRole('button', { name: /Override/i }))
    expect(screen.getByRole('dialog')).toBeInTheDocument()
  })
})

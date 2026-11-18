/**
 * Unit tests for the MapView component.
 *
 * Leaflet is swapped for the jsdom-safe stub via jest moduleNameMapper.
 * Tests verify that the component renders without crashing and exercises the
 * correct Leaflet APIs for each layer type.
 */

import { render, screen } from '@testing-library/react'
import { AppProvider } from '../context/AppContext'
import MapView, { type FleetAmbulance, type FleetHospital } from './MapView'
// The real leaflet is replaced by the mock via jest.config moduleNameMapper.
import L from 'leaflet'

// ── Fixtures ──────────────────────────────────────────────────────────────────

const mockAmbulances: FleetAmbulance[] = [
  {
    id: 1,
    name: 'AMB-001',
    etaToSceneSeconds: 0,
    equipment: ['oxygen', 'defibrillator'],
    latitude: 9.01,
    longitude: 38.76,
    status: 'available',
  },
  {
    id: 2,
    name: 'AMB-002',
    etaToSceneSeconds: 0,
    equipment: [],
    latitude: 9.02,
    longitude: 38.77,
    status: 'busy',
  },
  {
    id: 3,
    name: 'AMB-003',
    etaToSceneSeconds: 0,
    equipment: [],
    latitude: 9.03,
    longitude: 38.78,
    status: 'out-of-service',
  },
]

const mockHospitals: FleetHospital[] = [
  {
    id: 10,
    name: 'Tikur Anbessa Specialized Hospital',
    etaFromSceneSeconds: 0,
    capabilities: ['trauma'],
    latitude: 9.005,
    longitude: 38.763,
    available: true,
  },
]

// ── Helper ────────────────────────────────────────────────────────────────────

function renderMapView(
  ambulances = mockAmbulances,
  hospitals = mockHospitals,
) {
  return render(
    <AppProvider>
      <MapView fleetAmbulances={ambulances} fleetHospitals={hospitals} />
    </AppProvider>,
  )
}

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('MapView', () => {
  beforeEach(() => {
    jest.clearAllMocks()
  })

  it('renders the map container div', () => {
    renderMapView()
    expect(screen.getByTestId('map-container')).toBeInTheDocument()
  })

  it('has the correct accessible role and aria-label on the container', () => {
    renderMapView()
    const container = screen.getByTestId('map-container')
    expect(container).toHaveAttribute('role', 'application')
    expect(container).toHaveAttribute('aria-label', expect.stringContaining('Addis Ababa'))
  })

  it('initialises a Leaflet map instance', () => {
    renderMapView()
    expect(L.map).toHaveBeenCalledTimes(1)
  })

  it('adds an OSM tile layer with attribution', () => {
    renderMapView()
    expect(L.tileLayer).toHaveBeenCalledWith(
      expect.stringContaining('openstreetmap.org'),
      expect.objectContaining({ attribution: expect.stringContaining('OpenStreetMap') }),
    )
  })

  it('creates one marker per ambulance using divIcon', () => {
    renderMapView()
    // One divIcon call per ambulance
    expect(L.divIcon).toHaveBeenCalledTimes(
      mockAmbulances.length + mockHospitals.length,
    )
    expect(L.marker).toHaveBeenCalledTimes(
      mockAmbulances.length + mockHospitals.length,
    )
  })

  it('creates a marker for each hospital', () => {
    renderMapView([], mockHospitals)
    expect(L.marker).toHaveBeenCalledTimes(mockHospitals.length)
  })

  it('renders without ambulances or hospitals', () => {
    renderMapView([], [])
    expect(screen.getByTestId('map-container')).toBeInTheDocument()
    expect(L.marker).not.toHaveBeenCalled()
  })

  it('re-renders cleanly when props change', () => {
    const { rerender } = renderMapView([], [])
    rerender(
      <AppProvider>
        <MapView fleetAmbulances={mockAmbulances} fleetHospitals={mockHospitals} />
      </AppProvider>,
    )
    expect(screen.getByTestId('map-container')).toBeInTheDocument()
  })
})

/**
 * MapView — Leaflet map component for the LifeLane dispatch dashboard.
 *
 * Renders:
 *  - OSM tile layer (with attribution)
 *  - Ambulance markers (coloured by status via divIcon SVG)
 *  - Hospital markers (cross/plus SVG via divIcon)
 *  - Route polyline decoded from OSRM GeoJSON geometry (two legs, two colours)
 *  - Road-event circles (semi-transparent red)
 *  - Map click handler that dispatches SET_COORDS
 *
 * Requirements: 1.6, 8.4, 13.3
 *
 * Implementation notes:
 *  - useRef + useEffect initialise Leaflet imperatively to avoid SSR issues.
 *  - Map is cleaned up on unmount.
 *  - Component accepts `fleetAmbulances` and `fleetHospitals` as props because
 *    the base Ambulance / Hospital types in AppContext lack lat/lon; the fleet
 *    listing endpoint returns the full objects (see FleetAmbulance / FleetHospital).
 */

import { useEffect, useRef } from 'react'
import type L from 'leaflet'
import { useAppContext } from '../context/AppContext'

// ── Extended fleet types ──────────────────────────────────────────────────────

/**
 * Full ambulance object as returned by `GET /api/ambulances`.
 * Extends the leaner recommendation-context Ambulance with position + status.
 */
export interface FleetAmbulance {
  id: number
  name: string
  etaToSceneSeconds: number
  equipment: string[]
  latitude: number
  longitude: number
  status: 'available' | 'busy' | 'out-of-service'
}

/**
 * Full hospital object as returned by `GET /api/hospitals`.
 * Extends the leaner recommendation-context Hospital with position + availability.
 */
export interface FleetHospital {
  id: number
  name: string
  etaFromSceneSeconds: number
  capabilities: string[]
  latitude: number
  longitude: number
  available: boolean
}

// ── Props ─────────────────────────────────────────────────────────────────────

export interface MapViewProps {
  fleetAmbulances: FleetAmbulance[]
  fleetHospitals: FleetHospital[]
}

// ── Constants ─────────────────────────────────────────────────────────────────

/** Addis Ababa city centre coordinates used as the initial map view. */
const AA_CENTER: [number, number] = [9.0107, 38.7612]
const AA_ZOOM = 13

/** Addis Ababa bounding box used to restrict maxBounds. */
const AA_BOUNDS: [[number, number], [number, number]] = [
  [8.85, 38.65],
  [9.17, 38.90],
]

/** Status-to-colour mapping for ambulance markers. */
const AMBULANCE_COLOURS: Record<FleetAmbulance['status'], string> = {
  available: '#4caf50',
  busy: '#ff9800',
  'out-of-service': '#f44336',
}

/** Colour for ambulance-to-scene route leg. */
const COLOUR_AMB_TO_SCENE = '#1565c0'
/** Colour for scene-to-hospital route leg. */
const COLOUR_SCENE_TO_HOSP = '#d32f2f'
/** Colour for hospital markers. */
const COLOUR_HOSPITAL = '#1565c0'

// ── SVG helpers ───────────────────────────────────────────────────────────────

/** Inline SVG ambulance icon coloured by status. */
function ambulanceSvg(colour: string): string {
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 40 40" width="36" height="36" aria-hidden="true">
    <!-- vehicle body -->
    <rect x="2" y="14" width="36" height="18" rx="3" fill="${colour}" />
    <!-- cab -->
    <rect x="22" y="8" width="14" height="10" rx="2" fill="${colour}" />
    <!-- red cross -->
    <rect x="7" y="17" width="10" height="3" fill="white" />
    <rect x="11" y="13" width="3" height="10" fill="white" />
    <!-- windows -->
    <rect x="24" y="10" width="5" height="5" rx="1" fill="white" opacity="0.8" />
    <!-- wheels -->
    <circle cx="10" cy="33" r="4" fill="#333" />
    <circle cx="30" cy="33" r="4" fill="#333" />
    <circle cx="10" cy="33" r="2" fill="#666" />
    <circle cx="30" cy="33" r="2" fill="#666" />
  </svg>`
}

/** Inline SVG hospital cross icon. */
function hospitalSvg(colour: string): string {
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 40 40" width="32" height="32" aria-hidden="true">
    <circle cx="20" cy="20" r="18" fill="${colour}" />
    <rect x="8" y="16" width="24" height="8" fill="white" />
    <rect x="16" y="8" width="8" height="24" fill="white" />
  </svg>`
}

// ── Component ─────────────────────────────────────────────────────────────────

export default function MapView({ fleetAmbulances, fleetHospitals }: MapViewProps) {
  const { state, dispatch } = useAppContext()
  const { activeRoadEvents, currentRecommendation } = state

  /** DOM node that Leaflet mounts into. */
  const containerRef = useRef<HTMLDivElement>(null)
  /** Stable reference to the L.Map instance. */
  const mapRef = useRef<L.Map | null>(null)

  // ── Initialise map once ────────────────────────────────────────────────────
  useEffect(() => {
    if (!containerRef.current || mapRef.current) return

    // Dynamic import avoids SSR issues when bundled with SSR frameworks.
    // In a pure Vite/CRA setup this is synchronous in the browser.
    // eslint-disable-next-line @typescript-eslint/no-require-imports
    const L = require('leaflet') as typeof import('leaflet')

    const map = L.map(containerRef.current, {
      center: AA_CENTER,
      zoom: AA_ZOOM,
      maxBounds: AA_BOUNDS,
      maxBoundsViscosity: 0.8,
    })

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap contributors</a>',
      maxZoom: 19,
    }).addTo(map)

    map.on('click', (e: L.LeafletMouseEvent) => {
      dispatch({
        type: 'SET_COORDS',
        payload: { lat: e.latlng.lat, lng: e.latlng.lng },
      })
    })

    mapRef.current = map

    return () => {
      map.remove()
      mapRef.current = null
    }
  }, [dispatch])

  // ── Ambulance markers ──────────────────────────────────────────────────────
  useEffect(() => {
    const map = mapRef.current
    if (!map) return

    // eslint-disable-next-line @typescript-eslint/no-require-imports
    const L = require('leaflet') as typeof import('leaflet')

    const markers: L.Marker[] = []

    for (const amb of fleetAmbulances) {
      const colour = AMBULANCE_COLOURS[amb.status] ?? AMBULANCE_COLOURS['out-of-service']
      const icon = L.divIcon({
        html: ambulanceSvg(colour),
        className: '',
        iconSize: [36, 36],
        iconAnchor: [18, 36],
        popupAnchor: [0, -36],
      })

      const marker = L.marker([amb.latitude, amb.longitude], { icon })
        .addTo(map)
        .bindPopup(`<strong>${amb.name}</strong><br/>Status: ${amb.status}`)

      // Accessibility: set aria-label on the underlying marker element
      const el = (marker as unknown as { _icon?: HTMLElement })._icon
      if (el) {
        el.setAttribute('aria-label', `Ambulance ${amb.name} - ${amb.status}`)
        el.setAttribute('role', 'img')
      }

      markers.push(marker)
    }

    return () => {
      markers.forEach((m) => m.remove())
    }
  }, [fleetAmbulances])

  // ── Hospital markers ───────────────────────────────────────────────────────
  useEffect(() => {
    const map = mapRef.current
    if (!map) return

    // eslint-disable-next-line @typescript-eslint/no-require-imports
    const L = require('leaflet') as typeof import('leaflet')

    const markers: L.Marker[] = []

    for (const hosp of fleetHospitals) {
      const icon = L.divIcon({
        html: hospitalSvg(COLOUR_HOSPITAL),
        className: '',
        iconSize: [32, 32],
        iconAnchor: [16, 32],
        popupAnchor: [0, -32],
      })

      const marker = L.marker([hosp.latitude, hosp.longitude], { icon })
        .addTo(map)
        .bindPopup(`<strong>${hosp.name}</strong><br/>${hosp.available ? 'Available' : 'Unavailable'}`)

      const el = (marker as unknown as { _icon?: HTMLElement })._icon
      if (el) {
        el.setAttribute('aria-label', `Hospital ${hosp.name}`)
        el.setAttribute('role', 'img')
      }

      markers.push(marker)
    }

    return () => {
      markers.forEach((m) => m.remove())
    }
  }, [fleetHospitals])

  // ── Route polylines ────────────────────────────────────────────────────────
  useEffect(() => {
    const map = mapRef.current
    if (!map) return

    // eslint-disable-next-line @typescript-eslint/no-require-imports
    const L = require('leaflet') as typeof import('leaflet')

    const lines: L.Polyline[] = []

    if (currentRecommendation?.route?.segments) {
      for (const segment of currentRecommendation.route.segments) {
        // geometry is a GeoJSON LineString JSON string.
        // Coordinates are [lng, lat] — flip to [lat, lng] for Leaflet.
        let latlngs: [number, number][] = []
        try {
          const geojson = JSON.parse(segment.geometry) as {
            type: string
            coordinates: [number, number][]
          }
          if (geojson.type === 'LineString' && Array.isArray(geojson.coordinates)) {
            latlngs = geojson.coordinates.map(([lng, lat]) => [lat, lng])
          }
        } catch {
          // Malformed geometry — skip this segment
          continue
        }

        if (latlngs.length === 0) continue

        const colour =
          segment.leg === 'ambulance_to_scene'
            ? COLOUR_AMB_TO_SCENE
            : COLOUR_SCENE_TO_HOSP

        const line = L.polyline(latlngs, {
          color: colour,
          weight: 5,
          opacity: 0.85,
        }).addTo(map)

        lines.push(line)
      }
    }

    return () => {
      lines.forEach((l) => l.remove())
    }
  }, [currentRecommendation])

  // ── Road-event circles ─────────────────────────────────────────────────────
  useEffect(() => {
    const map = mapRef.current
    if (!map) return

    // eslint-disable-next-line @typescript-eslint/no-require-imports
    const L = require('leaflet') as typeof import('leaflet')

    const circles: L.Circle[] = []

    for (const event of activeRoadEvents) {
      const circle = L.circle([event.latitude, event.longitude], {
        radius: 200,
        color: '#f44336',
        fillColor: '#f44336',
        fillOpacity: 0.3,
        weight: 2,
      }).addTo(map)

      circles.push(circle)
    }

    return () => {
      circles.forEach((c) => c.remove())
    }
  }, [activeRoadEvents])

  // ── Render ─────────────────────────────────────────────────────────────────
  return (
    <div
      ref={containerRef}
      style={{ width: '100%', height: '100%', minHeight: '400px' }}
      aria-label="Emergency dispatch map centred on Addis Ababa"
      role="application"
      data-testid="map-container"
    />
  )
}

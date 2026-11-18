/**
 * Lightweight Leaflet stub for Jest / jsdom.
 *
 * Leaflet relies on browser APIs (canvas, SVG, requestAnimationFrame) that are
 * not available in jsdom.  Tests that import components using Leaflet will get
 * this stub instead of the real library so they can exercise React rendering
 * without hitting jsdom limitations.
 *
 * Covers all APIs used by MapView: L.map, L.tileLayer, L.divIcon, L.marker,
 * L.polyline, L.circle, L.geoJSON, L.icon, L.latLng, L.latLngBounds.
 */

/** Shared marker-like stub factory. */
function makeLayerStub() {
  return {
    addTo: jest.fn().mockReturnThis(),
    bindPopup: jest.fn().mockReturnThis(),
    setLatLng: jest.fn().mockReturnThis(),
    remove: jest.fn(),
    _icon: undefined as HTMLElement | undefined,
  }
}

const L = {
  map: jest.fn(() => ({
    setView: jest.fn().mockReturnThis(),
    remove: jest.fn(),
    on: jest.fn(),
    off: jest.fn(),
    addLayer: jest.fn(),
    removeLayer: jest.fn(),
  })),

  tileLayer: jest.fn(() => ({
    addTo: jest.fn().mockReturnThis(),
    remove: jest.fn(),
  })),

  marker: jest.fn(() => makeLayerStub()),

  polyline: jest.fn(() => ({
    addTo: jest.fn().mockReturnThis(),
    remove: jest.fn(),
  })),

  circle: jest.fn(() => ({
    addTo: jest.fn().mockReturnThis(),
    remove: jest.fn(),
  })),

  geoJSON: jest.fn(() => ({
    addTo: jest.fn().mockReturnThis(),
    remove: jest.fn(),
  })),

  latLng: jest.fn((lat: number, lng: number) => ({ lat, lng })),

  latLngBounds: jest.fn(() => ({
    extend: jest.fn().mockReturnThis(),
  })),

  icon: jest.fn((options: object) => options),

  divIcon: jest.fn((options: object) => options),

  DomUtil: {
    create: jest.fn(() => document.createElement('div')),
  },
}

export default L
module.exports = L

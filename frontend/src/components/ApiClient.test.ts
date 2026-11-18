// ApiClient.test.ts
import {
  createEmergency,
  getRecommendation,
  recordDecision,
  getAmbulances,
  getHospitals,
  patchRoadEvent,
  getAnalytics,
  ApiError,
} from '../api/ApiClient'

// ── Global fetch mock ────────────────────────────────────────────────────────

const mockFetch = jest.fn() as jest.MockedFunction<typeof fetch>
global.fetch = mockFetch

function jsonResponse(body: unknown, status = 200) {
  return Promise.resolve({
    ok: status >= 200 && status < 300,
    status,
    statusText: status === 200 ? 'OK' : 'Error',
    json: () => Promise.resolve(body),
    headers: new Headers({ 'Content-Type': 'application/json' }),
  } as Response)
}

beforeEach(() => {
  mockFetch.mockReset()
})

// ── createEmergency ──────────────────────────────────────────────────────────

describe('createEmergency', () => {
  it('sends POST /api/emergencies with correct body', async () => {
    const payload = {
      latitude: 9.02,
      longitude: 38.75,
      urgencyLevel: 'HIGH',
      requiredCareType: 'cardiology',
    }
    const serverResponse = { id: 1, ...payload }
    mockFetch.mockReturnValueOnce(jsonResponse(serverResponse, 200))

    const result = await createEmergency(payload)

    expect(mockFetch).toHaveBeenCalledTimes(1)
    const [url, opts] = mockFetch.mock.calls[0]
    expect(url).toBe('/api/emergencies')
    expect(opts?.method).toBe('POST')
    expect(JSON.parse(opts?.body as string)).toEqual(payload)
    expect(result).toEqual(serverResponse)
  })

  it('throws ApiError on 400', async () => {
    const errorBody = { status: 400, message: 'Validation failed', errors: ['lat out of range'] }
    mockFetch.mockReturnValueOnce(jsonResponse(errorBody, 400))

    await expect(createEmergency({ latitude: 999, longitude: 0, urgencyLevel: 'HIGH', requiredCareType: 'x' }))
      .rejects.toThrow(ApiError)
  })
})

// ── getRecommendation ────────────────────────────────────────────────────────

describe('getRecommendation', () => {
  it('sends POST /api/emergencies/{id}/recommendations', async () => {
    const rec = { ambulance: {}, hospital: {}, route: {}, explanation: 'ok', alternatives: [] }
    mockFetch.mockReturnValueOnce(jsonResponse(rec))

    await getRecommendation(5)

    const [url, opts] = mockFetch.mock.calls[0]
    expect(url).toBe('/api/emergencies/5/recommendations')
    expect(opts?.method).toBe('POST')
  })
})

// ── recordDecision ───────────────────────────────────────────────────────────

describe('recordDecision', () => {
  it('sends POST /api/emergencies/{id}/decision with accept action', async () => {
    mockFetch.mockReturnValueOnce(jsonResponse({ ok: true }))

    await recordDecision(10, { action: 'accept' })

    const [url, opts] = mockFetch.mock.calls[0]
    expect(url).toBe('/api/emergencies/10/decision')
    expect(opts?.method).toBe('POST')
    expect(JSON.parse(opts?.body as string)).toEqual({ action: 'accept' })
  })

  it('sends override decision with ambulanceId, hospitalId, and comment', async () => {
    mockFetch.mockReturnValueOnce(jsonResponse({ ok: true }))

    await recordDecision(10, {
      action: 'override',
      ambulanceId: 3,
      hospitalId: 7,
      comment: 'Closer unit',
    })

    const body = JSON.parse(mockFetch.mock.calls[0][1]?.body as string)
    expect(body.action).toBe('override')
    expect(body.ambulanceId).toBe(3)
    expect(body.hospitalId).toBe(7)
    expect(body.comment).toBe('Closer unit')
  })
})

// ── getAmbulances ────────────────────────────────────────────────────────────

describe('getAmbulances', () => {
  it('sends GET /api/ambulances', async () => {
    const ambulances = [{ id: 1, name: 'A1' }]
    mockFetch.mockReturnValueOnce(jsonResponse(ambulances))

    const result = await getAmbulances()

    expect(mockFetch.mock.calls[0][0]).toBe('/api/ambulances')
    expect(result).toEqual(ambulances)
  })
})

// ── getHospitals ─────────────────────────────────────────────────────────────

describe('getHospitals', () => {
  it('sends GET /api/hospitals', async () => {
    const hospitals = [{ id: 1, name: 'H1' }]
    mockFetch.mockReturnValueOnce(jsonResponse(hospitals))

    const result = await getHospitals()

    expect(mockFetch.mock.calls[0][0]).toBe('/api/hospitals')
    expect(result).toEqual(hospitals)
  })
})

// ── patchRoadEvent ───────────────────────────────────────────────────────────

describe('patchRoadEvent', () => {
  it('sends PATCH /api/road-events/{id} with status body', async () => {
    const updated = { id: 2, status: 'active' }
    mockFetch.mockReturnValueOnce(jsonResponse(updated))

    const result = await patchRoadEvent(2, 'active')

    const [url, opts] = mockFetch.mock.calls[0]
    expect(url).toBe('/api/road-events/2')
    expect(opts?.method).toBe('PATCH')
    expect(JSON.parse(opts?.body as string)).toEqual({ status: 'active' })
    expect(result).toEqual(updated)
  })
})

// ── getAnalytics ─────────────────────────────────────────────────────────────

describe('getAnalytics', () => {
  it('sends GET /api/analytics/summary', async () => {
    const summary = { totalEmergencyRequests: 10, averageTotalEtaSeconds: 500, overrideRatePercent: 20 }
    mockFetch.mockReturnValueOnce(jsonResponse(summary))

    const result = await getAnalytics()

    expect(mockFetch.mock.calls[0][0]).toBe('/api/analytics/summary')
    expect(result).toEqual(summary)
  })
})

/**
 * ApiClient — typed fetch wrappers for all LifeLane Mini backend endpoints.
 *
 * All methods throw an `ApiError` for non-2xx responses so callers can handle
 * HTTP-level failures without inspecting raw `Response` objects.
 *
 * Base URL: requests go through the Vite `/api` proxy → `http://localhost:8080`
 * in development, or directly to the backend origin in production.
 *
 * Requirements: 11.1
 */

import type {
  Ambulance,
  AnalyticsSummary,
  DecisionRequest,
  EmergencyRequest,
  ErrorResponse,
  Hospital,
  Recommendation,
  RoadEvent,
} from '../types';

// ── Error type ───────────────────────────────────────────────────────────────

/**
 * Thrown for every non-2xx HTTP response.
 * The `body` field contains the parsed `ErrorResponse` when the server returns
 * `application/json`; otherwise it contains a synthetic error body.
 */
export class ApiError extends Error {
  constructor(
    public readonly statusCode: number,
    public readonly body: ErrorResponse,
  ) {
    super(body.message);
    this.name = 'ApiError';
  }
}

// ── Internal helpers ─────────────────────────────────────────────────────────

const JSON_HEADERS: Record<string, string> = {
  'Content-Type': 'application/json',
  Accept: 'application/json',
};

/**
 * Performs a fetch and throws `ApiError` for any non-2xx status.
 * Returns the parsed JSON body on success.
 */
async function request<T>(
  url: string,
  options: RequestInit = {},
): Promise<T> {
  const response = await fetch(url, {
    ...options,
    headers: {
      ...JSON_HEADERS,
      ...options.headers,
    },
  });

  if (!response.ok) {
    let errorBody: ErrorResponse;
    try {
      errorBody = (await response.json()) as ErrorResponse;
    } catch {
      // Server returned a non-JSON body (e.g. 502 from proxy)
      errorBody = {
        status: response.status,
        message: response.statusText || 'Unexpected error',
      };
    }
    throw new ApiError(response.status, errorBody);
  }

  // 204 No Content — return undefined cast to T
  if (response.status === 204) {
    return undefined as unknown as T;
  }

  return response.json() as Promise<T>;
}

// ── Public API ───────────────────────────────────────────────────────────────

/**
 * Request body for `POST /api/emergencies`.
 */
export interface CreateEmergencyPayload {
  latitude: number;
  longitude: number;
  urgencyLevel: string;
  requiredCareType: string;
}

/**
 * 1. POST /api/emergencies
 *    Creates a new emergency request.
 *    Returns: EmergencyRequest (HTTP 201)
 */
export async function createEmergency(
  payload: CreateEmergencyPayload,
): Promise<EmergencyRequest> {
  return request<EmergencyRequest>('/api/emergencies', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

/**
 * 2. POST /api/emergencies/{id}/recommendations
 *    Triggers the recommendation engine for the given emergency.
 *    Returns: Recommendation (HTTP 200)
 */
export async function getRecommendation(
  emergencyId: number,
): Promise<Recommendation> {
  return request<Recommendation>(
    `/api/emergencies/${emergencyId}/recommendations`,
    { method: 'POST' },
  );
}

/**
 * 3. POST /api/emergencies/{id}/decision
 *    Records the dispatcher's accept or override decision.
 *    Returns: the persisted decision payload (HTTP 200)
 */
export async function recordDecision(
  emergencyId: number,
  payload: DecisionRequest,
): Promise<unknown> {
  return request<unknown>(
    `/api/emergencies/${emergencyId}/decision`,
    {
      method: 'POST',
      body: JSON.stringify(payload),
    },
  );
}

/**
 * 4. GET /api/ambulances
 *    Returns the current status and equipment list of every ambulance.
 *    Returns: Ambulance[] (HTTP 200)
 */
export async function getAmbulances(): Promise<Ambulance[]> {
  return request<Ambulance[]>('/api/ambulances');
}

/**
 * 5. GET /api/hospitals
 *    Returns the current capabilities and availability of every hospital.
 *    Returns: Hospital[] (HTTP 200)
 */
export async function getHospitals(): Promise<Hospital[]> {
  return request<Hospital[]>('/api/hospitals');
}

/**
 * 6. PATCH /api/road-events/{id}
 *    Activates or deactivates a road event by setting its status.
 *    Returns: updated RoadEvent (HTTP 200)
 */
export async function patchRoadEvent(
  roadEventId: number,
  status: 'active' | 'inactive',
): Promise<RoadEvent> {
  return request<RoadEvent>(`/api/road-events/${roadEventId}`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  });
}

/**
 * 7. GET /api/analytics/summary
 *    Returns aggregate metrics over all recorded decisions.
 *    Returns: AnalyticsSummary (HTTP 200)
 */
export async function getAnalytics(): Promise<AnalyticsSummary> {
  return request<AnalyticsSummary>('/api/analytics/summary');
}

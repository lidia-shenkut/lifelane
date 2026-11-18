/**
 * TypeScript interfaces matching all API response and request shapes
 * for the LifeLane Mini backend (Requirements: 11.1).
 */

// ── Emergency Request ────────────────────────────────────────────────────────

/** Status values for an emergency request lifecycle. */
export type EmergencyRequestStatus = 'open' | 'dispatched' | 'closed';

/** Urgency level submitted by the dispatcher. */
export type UrgencyLevel = 'low' | 'medium' | 'high' | 'critical';

/**
 * Represents a created emergency request as returned by
 * `POST /api/emergencies`.
 */
export interface EmergencyRequest {
  id: number;
  latitude: number;
  longitude: number;
  urgencyLevel: UrgencyLevel;
  requiredCareType: string;
  status: EmergencyRequestStatus;
  createdAt: string; // ISO-8601 UTC timestamp
}

// ── Ambulance ────────────────────────────────────────────────────────────────

/** Status values for an ambulance resource. */
export type AmbulanceStatus = 'available' | 'busy' | 'out-of-service';

/**
 * Represents an ambulance as returned by `GET /api/ambulances`.
 * Also used as the nested ambulance within a `Recommendation`.
 * When appearing in the fleet listing `etaToSceneSeconds` is 0
 * (no incident context); within a recommendation it carries the real ETA.
 */
export interface Ambulance {
  id: number;
  name: string;
  /** Estimated time in seconds for the ambulance to reach the incident scene. */
  etaToSceneSeconds: number;
  equipment: string[];
}

// ── Hospital ─────────────────────────────────────────────────────────────────

/**
 * Represents a hospital as returned by `GET /api/hospitals`.
 * Also used as the nested hospital within a `Recommendation`.
 * When appearing in the hospital listing `etaFromSceneSeconds` is 0
 * (no incident context); within a recommendation it carries the real ETA.
 */
export interface Hospital {
  id: number;
  name: string;
  /** Estimated time in seconds from the incident scene to the hospital. */
  etaFromSceneSeconds: number;
  capabilities: string[];
}

// ── Route ────────────────────────────────────────────────────────────────────

/** Identifies which leg of the two-part dispatch journey this segment covers. */
export type RouteLeg = 'ambulance_to_scene' | 'scene_to_hospital';

/** One leg of the two-part emergency dispatch route. */
export interface RouteSegment {
  leg: RouteLeg;
  /** Duration of this route leg in seconds. */
  durationSeconds: number;
  /** Encoded polyline (or GeoJSON string) for map rendering. */
  geometry: string;
}

/** Aggregated route information for the entire emergency dispatch trip. */
export interface Route {
  /** Total ETA in seconds (ambulance-to-scene + scene-to-hospital). */
  totalDurationSeconds: number;
  segments: RouteSegment[];
}

// ── Recommendation ───────────────────────────────────────────────────────────

/**
 * A lower-ranked alternative ambulance/hospital pair that was not selected
 * as the primary recommendation.
 */
export interface Alternative {
  ambulanceId: number;
  ambulanceName: string;
  hospitalId: number;
  hospitalName: string;
  /** Total ETA in seconds for this alternative pair. */
  totalEtaSeconds: number;
  reason: string;
}

/**
 * Dispatch recommendation returned by
 * `POST /api/emergencies/{id}/recommendations`.
 */
export interface Recommendation {
  recommendationId: number;
  emergencyRequestId: number;
  ambulance: Ambulance;
  hospital: Hospital;
  route: Route;
  explanation: string;
  alternatives: Alternative[];
  createdAt: string; // ISO-8601 UTC timestamp
}

// ── Road Event ───────────────────────────────────────────────────────────────

/** Whether a road event is currently affecting routing. */
export type RoadEventStatus = 'active' | 'inactive';

/** Type of disruption a road event represents. */
export type RoadEventType = 'delay' | 'closure';

/**
 * Represents a road event as returned by `PATCH /api/road-events/{id}`.
 */
export interface RoadEvent {
  id: number;
  name: string;
  status: RoadEventStatus;
  delaySeconds: number;
  eventType: RoadEventType;
  latitude: number;
  longitude: number;
  updatedAt: string; // ISO-8601 UTC timestamp
}

// ── Analytics Summary ────────────────────────────────────────────────────────

/** ML model performance metrics; present only when the ML feature flag is enabled. */
export interface MlMetrics {
  meanAbsoluteError: number;
  baselineOsrmMae: number;
  improvementPercent: number;
  note: string | null;
}

/**
 * Analytics summary returned by `GET /api/analytics/summary`.
 */
export interface AnalyticsSummary {
  totalEmergencyRequests: number;
  averageTotalEtaSeconds: number;
  overrideRatePercent: number;
  recommendationsPerScenario: Record<string, number>;
  mlMetrics: MlMetrics | null;
}

// ── Decision Request ─────────────────────────────────────────────────────────

/** Action a dispatcher takes on a recommendation. */
export type DecisionAction = 'accept' | 'override';

/**
 * Request body sent to `POST /api/emergencies/{id}/decision`.
 * When `action` is `"override"`, both `ambulanceId` and `hospitalId` are required.
 */
export interface DecisionRequest {
  action: DecisionAction;
  /** Required when action is "override". */
  ambulanceId?: number;
  /** Required when action is "override". */
  hospitalId?: number;
  comment?: string;
}

// ── Error Response ───────────────────────────────────────────────────────────

/** A single field-level validation failure. */
export interface FieldError {
  field: string;
  message: string;
}

/**
 * Standard error body returned by the API for non-2xx responses.
 * The `errors` array is populated for HTTP 400 validation failures.
 */
export interface ErrorResponse {
  status: number;
  message: string;
  errors?: FieldError[];
}

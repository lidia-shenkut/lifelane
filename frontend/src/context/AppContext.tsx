/**
 * AppContext — global state management for LifeLane Dashboard.
 *
 * Uses React Context + useReducer to manage all application-level state
 * without additional dependencies (Requirements: 13.2).
 *
 * Exported:
 *  - AppProvider  : wrap the app tree to provide state
 *  - useAppContext: consume state and dispatch from any child component
 */

import React, {
  createContext,
  useContext,
  useReducer,
  type ReactNode,
} from 'react'
import type {
  Ambulance,
  Hospital,
  RoadEvent,
  EmergencyRequest,
  Recommendation,
  AnalyticsSummary,
} from '../types'

// ── State ─────────────────────────────────────────────────────────────────────

export interface AppState {
  ambulances: Ambulance[]
  hospitals: Hospital[]
  activeRoadEvents: RoadEvent[]
  currentEmergency: EmergencyRequest | null
  currentRecommendation: Recommendation | null
  pendingDecision: boolean
  analytics: AnalyticsSummary | null
  /** Coordinates selected by clicking on the map; pre-fills the emergency form. */
  selectedCoords: { lat: number; lng: number } | null
}

const initialState: AppState = {
  ambulances: [],
  hospitals: [],
  activeRoadEvents: [],
  currentEmergency: null,
  currentRecommendation: null,
  pendingDecision: false,
  analytics: null,
  selectedCoords: null,
}

// ── Actions ───────────────────────────────────────────────────────────────────

export type AppAction =
  | { type: 'SET_AMBULANCES'; payload: Ambulance[] }
  | { type: 'SET_HOSPITALS'; payload: Hospital[] }
  | { type: 'SET_ROAD_EVENTS'; payload: RoadEvent[] }
  | { type: 'SUBMIT_EMERGENCY'; payload: EmergencyRequest }
  | { type: 'RECEIVE_RECOMMENDATION'; payload: Recommendation }
  | { type: 'RECORD_DECISION' }
  | { type: 'REFRESH_ANALYTICS'; payload: AnalyticsSummary }
  | { type: 'SET_COORDS'; payload: { lat: number; lng: number } | null }

// ── Reducer ───────────────────────────────────────────────────────────────────

export function appReducer(state: AppState, action: AppAction): AppState {
  switch (action.type) {
    case 'SET_AMBULANCES':
      return { ...state, ambulances: action.payload }

    case 'SET_HOSPITALS':
      return { ...state, hospitals: action.payload }

    case 'SET_ROAD_EVENTS':
      return { ...state, activeRoadEvents: action.payload }

    case 'SUBMIT_EMERGENCY':
      return {
        ...state,
        currentEmergency: action.payload,
        // Reset recommendation when a new emergency is submitted
        currentRecommendation: null,
        pendingDecision: false,
      }

    case 'RECEIVE_RECOMMENDATION':
      return {
        ...state,
        currentRecommendation: action.payload,
        pendingDecision: true,
      }

    case 'RECORD_DECISION':
      return { ...state, pendingDecision: false }

    case 'REFRESH_ANALYTICS':
      return { ...state, analytics: action.payload }

    case 'SET_COORDS':
      return { ...state, selectedCoords: action.payload }

    default:
      return state
  }
}

// ── Context ───────────────────────────────────────────────────────────────────

interface AppContextValue {
  state: AppState
  dispatch: React.Dispatch<AppAction>
}

const AppContext = createContext<AppContextValue | undefined>(undefined)

// ── Provider ──────────────────────────────────────────────────────────────────

interface AppProviderProps {
  children: ReactNode
}

export function AppProvider({ children }: AppProviderProps) {
  const [state, dispatch] = useReducer(appReducer, initialState)

  return (
    <AppContext.Provider value={{ state, dispatch }}>
      {children}
    </AppContext.Provider>
  )
}

// ── Hook ──────────────────────────────────────────────────────────────────────

/**
 * Returns the current AppState and the dispatch function.
 * Must be called inside an `<AppProvider>` tree.
 */
export function useAppContext(): AppContextValue {
  const ctx = useContext(AppContext)
  if (ctx === undefined) {
    throw new Error('useAppContext must be used within an AppProvider')
  }
  return ctx
}

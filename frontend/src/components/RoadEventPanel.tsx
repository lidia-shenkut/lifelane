/**
 * RoadEventPanel — displays a list of road events with toggle switches.
 *
 * Each road event shows its name, type, and delay. The switch calls
 * `ApiClient.patchRoadEvent()` to activate or deactivate the event,
 * then dispatches `SET_ROAD_EVENTS` so the map layer stays in sync.
 *
 * Requirements: 8.4, 13.2
 */

import { useState } from 'react'
import {
  Alert,
  Box,
  CircularProgress,
  List,
  ListItem,
  ListItemText,
  Snackbar,
  Switch,
  Typography,
} from '@mui/material'

import { useAppContext } from '../context/AppContext'
import { patchRoadEvent, ApiError } from '../api/ApiClient'
import type { RoadEvent } from '../types'

// ── Props ─────────────────────────────────────────────────────────────────────

interface RoadEventPanelProps {
  /** Full list of all known road events (active + inactive). */
  roadEvents: RoadEvent[]
}

// ── Component ─────────────────────────────────────────────────────────────────

export function RoadEventPanel({ roadEvents }: RoadEventPanelProps) {
  const { dispatch } = useAppContext()

  // Local copy of the event list so toggles are reflected immediately.
  const [eventList, setEventList] = useState<RoadEvent[]>(roadEvents)

  // Tracks which event IDs are currently being toggled (shows spinner).
  const [loadingIds, setLoadingIds] = useState<Set<number>>(new Set())

  // Snackbar error state
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  // ── Toggle handler ─────────────────────────────────────────────────────────

  async function handleToggle(event: RoadEvent, checked: boolean) {
    const newStatus = checked ? 'active' : 'inactive'

    // Mark this event as loading
    setLoadingIds((prev) => new Set(prev).add(event.id))

    try {
      const updated = await patchRoadEvent(event.id, newStatus)

      // Update the local list with the server-confirmed state
      const newList = eventList.map((e) => (e.id === updated.id ? updated : e))
      setEventList(newList)

      // Dispatch only the active events to global state (for map layer)
      const activeEvents = newList.filter((e) => e.status === 'active')
      dispatch({ type: 'SET_ROAD_EVENTS', payload: activeEvents })
    } catch (err) {
      if (err instanceof ApiError) {
        setErrorMessage(err.body.message)
      } else {
        setErrorMessage('Failed to update road event. Please try again.')
      }
    } finally {
      setLoadingIds((prev) => {
        const next = new Set(prev)
        next.delete(event.id)
        return next
      })
    }
  }

  // ── Render ─────────────────────────────────────────────────────────────────

  return (
    <Box>
      <Typography variant="h6" gutterBottom>
        Road Events
      </Typography>

      {eventList.length === 0 ? (
        <Typography variant="body2" color="text.secondary">
          No road events configured.
        </Typography>
      ) : (
        <List disablePadding>
          {eventList.map((event) => {
            const isLoading = loadingIds.has(event.id)
            const isActive = event.status === 'active'

            return (
              <ListItem
                key={event.id}
                disableGutters
                secondaryAction={
                  isLoading ? (
                    <CircularProgress
                      size={24}
                      aria-label={`Updating ${event.name}`}
                    />
                  ) : (
                    <Switch
                      edge="end"
                      checked={isActive}
                      onChange={(e) => handleToggle(event, e.target.checked)}
                      disabled={isLoading}
                      aria-label={`Toggle road event ${event.name}`}
                      inputProps={{
                        'aria-label': `Toggle ${event.name}`,
                      }}
                    />
                  )
                }
                sx={{ pr: 7 }}
              >
                <ListItemText
                  primary={event.name}
                  secondary={`${event.eventType} · ${event.delaySeconds}s delay`}
                  primaryTypographyProps={{ fontWeight: isActive ? 'bold' : 'normal' }}
                  secondaryTypographyProps={{
                    color: isActive ? 'error.main' : 'text.secondary',
                  }}
                />
              </ListItem>
            )
          })}
        </List>
      )}

      {/* Error snackbar */}
      <Snackbar
        open={errorMessage !== null}
        autoHideDuration={5000}
        onClose={() => setErrorMessage(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert
          severity="error"
          onClose={() => setErrorMessage(null)}
          sx={{ width: '100%' }}
        >
          {errorMessage}
        </Alert>
      </Snackbar>
    </Box>
  )
}

export default RoadEventPanel

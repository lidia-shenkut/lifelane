/**
 * OverrideDialog — lets a dispatcher override the AI recommendation
 * by manually selecting an ambulance and hospital.
 *
 * Requirements: 7.2, 7.3, 7.4, 13.2
 */

import React, { useState } from 'react'
import {
  Alert,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  type SelectChangeEvent,
  TextField,
} from '@mui/material'
import { useAppContext } from '../context/AppContext'
import { recordDecision, ApiError } from '../api/ApiClient'

// ── Props ─────────────────────────────────────────────────────────────────────

export interface OverrideDialogProps {
  open: boolean
  emergencyId: number
  onClose: () => void
  onSubmitted?: () => void
}

// ── Component ─────────────────────────────────────────────────────────────────

/**
 * Modal dialog that allows a dispatcher to override the system recommendation.
 *
 * - Ambulance and hospital selects are populated from the global context fleet.
 * - Both selects are required; the comment field is optional.
 * - MUI's built-in `Dialog` provides a focus trap for keyboard navigation.
 * - All interactive elements carry `aria-label` attributes for screen readers.
 */
export function OverrideDialog({
  open,
  emergencyId,
  onClose,
  onSubmitted,
}: OverrideDialogProps) {
  const { state, dispatch } = useAppContext()

  // ── Local state ────────────────────────────────────────────────────────────
  const [selectedAmbulanceId, setSelectedAmbulanceId] = useState<number | ''>(
    '',
  )
  const [selectedHospitalId, setSelectedHospitalId] = useState<number | ''>('')
  const [comment, setComment] = useState<string>('')
  const [loading, setLoading] = useState<boolean>(false)
  const [error, setError] = useState<string | null>(null)

  // ── Derived data ───────────────────────────────────────────────────────────
  // The context stores base Ambulance[] / Hospital[] types.
  // We cast to any to access the runtime `status`/`available` fields that
  // the fleet list returns but the TypeScript interface omits.
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const ambulances = (state.ambulances as any[]).filter(
    (a) => a.status === undefined || a.status === 'available',
  )

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const hospitals = (state.hospitals as any[]).filter(
    (h) => h.available === undefined || h.available === true,
  )

  // ── Handlers ───────────────────────────────────────────────────────────────

  function handleAmbulanceChange(e: SelectChangeEvent<number | ''>) {
    setSelectedAmbulanceId(e.target.value as number | '')
    setError(null)
  }

  function handleHospitalChange(e: SelectChangeEvent<number | ''>) {
    setSelectedHospitalId(e.target.value as number | '')
    setError(null)
  }

  function handleCommentChange(e: React.ChangeEvent<HTMLInputElement>) {
    setComment(e.target.value)
  }

  /** Reset local state when dialog is closed. */
  function handleClose() {
    if (loading) return // prevent closing while a request is in flight
    setSelectedAmbulanceId('')
    setSelectedHospitalId('')
    setComment('')
    setError(null)
    onClose()
  }

  async function handleSubmit() {
    // Client-side validation
    if (selectedAmbulanceId === '') {
      setError('Please select an ambulance')
      return
    }
    if (selectedHospitalId === '') {
      setError('Please select a hospital')
      return
    }

    setLoading(true)
    setError(null)

    try {
      await recordDecision(emergencyId, {
        action: 'override',
        ambulanceId: selectedAmbulanceId as number,
        hospitalId: selectedHospitalId as number,
        comment: comment.trim() || undefined,
      })

      dispatch({ type: 'RECORD_DECISION' })
      onSubmitted?.()
      handleClose()
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.body.message || 'Failed to record override decision.')
      } else {
        setError('An unexpected error occurred. Please try again.')
      }
    } finally {
      setLoading(false)
    }
  }

  // ── Render ─────────────────────────────────────────────────────────────────

  const submitDisabled =
    loading || selectedAmbulanceId === '' || selectedHospitalId === ''

  return (
    <Dialog
      open={open}
      onClose={handleClose}
      aria-labelledby="override-dialog-title"
      maxWidth="sm"
      fullWidth
    >
      <DialogTitle id="override-dialog-title">
        Override Recommendation
      </DialogTitle>

      <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 1 }}>
        {/* Ambulance select */}
        <FormControl fullWidth required>
          <InputLabel id="ambulance-select-label">Select Ambulance *</InputLabel>
          <Select<number | ''>
            labelId="ambulance-select-label"
            value={selectedAmbulanceId}
            label="Select Ambulance *"
            onChange={handleAmbulanceChange}
            inputProps={{
              'aria-label': 'Select ambulance for override',
              id: 'ambulance-select',
            }}
            disabled={loading}
          >
            {ambulances.map((a) => (
              <MenuItem key={a.id} value={a.id}>
                {a.name}
              </MenuItem>
            ))}
          </Select>
        </FormControl>

        {/* Hospital select */}
        <FormControl fullWidth required>
          <InputLabel id="hospital-select-label">Select Hospital *</InputLabel>
          <Select<number | ''>
            labelId="hospital-select-label"
            value={selectedHospitalId}
            label="Select Hospital *"
            onChange={handleHospitalChange}
            inputProps={{
              'aria-label': 'Select hospital for override',
              id: 'hospital-select',
            }}
            disabled={loading}
          >
            {hospitals.map((h) => (
              <MenuItem key={h.id} value={h.id}>
                {h.name}
              </MenuItem>
            ))}
          </Select>
        </FormControl>

        {/* Optional comment */}
        <TextField
          label="Dispatcher Comment (Optional)"
          value={comment}
          onChange={handleCommentChange}
          multiline
          rows={3}
          fullWidth
          disabled={loading}
          inputProps={{
            'aria-label': 'Dispatcher comment (optional)',
          }}
        />

        {/* Error feedback */}
        {error !== null && (
          <Alert severity="error" role="alert">
            {error}
          </Alert>
        )}
      </DialogContent>

      <DialogActions>
        <Button
          onClick={handleClose}
          disabled={loading}
          aria-label="Cancel override"
        >
          Cancel
        </Button>

        <Button
          variant="contained"
          color="primary"
          onClick={handleSubmit}
          disabled={submitDisabled}
          aria-label="Submit override decision"
          startIcon={loading ? <CircularProgress size={16} color="inherit" /> : undefined}
        >
          {loading ? 'Submitting…' : 'Submit Override'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

export default OverrideDialog

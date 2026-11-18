/**
 * EmergencyForm — dispatcher form for creating a new emergency request.
 *
 * Features:
 *  - Latitude / Longitude TextFields, pre-populated from map-click coords via context
 *  - Urgency Level MUI Slider (1=Low → 4=Critical) with ARIA attributes
 *  - Required Care Type FormGroup with Checkbox checklist
 *  - Field-level validation errors displayed inline
 *  - Calls ApiClient.createEmergency() then ApiClient.getRecommendation() on submit
 *  - Dispatches SUBMIT_EMERGENCY and RECEIVE_RECOMMENDATION on success
 *
 * Requirements: 1.6, 1.7, 13.2, 13.7
 */

import React, { useEffect, useState } from 'react'
import {
  Box,
  Button,
  Checkbox,
  CircularProgress,
  FormControlLabel,
  FormGroup,
  FormHelperText,
  FormLabel,
  Slider,
  Stack,
  TextField,
  Typography,
} from '@mui/material'

import { useAppContext } from '../context/AppContext'
import { createEmergency, getRecommendation, ApiError } from '../api/ApiClient'
import type { EmergencyRequest } from '../types'

// ── Constants ─────────────────────────────────────────────────────────────────

const CARE_TYPES = ['trauma', 'cardiac', 'stroke', 'pediatric', 'burns', 'general'] as const

const URGENCY_MARKS = [
  { value: 1, label: 'Low' },
  { value: 2, label: 'Medium' },
  { value: 3, label: 'High' },
  { value: 4, label: 'Critical' },
]

const URGENCY_MAP: Record<number, 'low' | 'medium' | 'high' | 'critical'> = {
  1: 'low',
  2: 'medium',
  3: 'high',
  4: 'critical',
}

// ── Field-level error state ───────────────────────────────────────────────────

interface FormErrors {
  latitude?: string
  longitude?: string
  careType?: string
  general?: string
}

// ── Props ─────────────────────────────────────────────────────────────────────

export interface EmergencyFormProps {
  /** Optional callback invoked after the emergency is successfully created. */
  onEmergencyCreated?: (emergency: EmergencyRequest) => void
}

// ── Component ─────────────────────────────────────────────────────────────────

export default function EmergencyForm({ onEmergencyCreated }: EmergencyFormProps) {
  const { state, dispatch } = useAppContext()

  // Form field state
  const [latitude, setLatitude] = useState<string>('')
  const [longitude, setLongitude] = useState<string>('')
  const [urgency, setUrgency] = useState<number>(2)
  const [selectedCareTypes, setSelectedCareTypes] = useState<Set<string>>(new Set())

  // UI state
  const [errors, setErrors] = useState<FormErrors>({})
  const [loading, setLoading] = useState(false)

  // Pre-populate lat/lng when the user clicks on the map
  useEffect(() => {
    if (state.selectedCoords) {
      setLatitude(String(state.selectedCoords.lat))
      setLongitude(String(state.selectedCoords.lng))
      // Clear any prior coord-related validation errors
      setErrors(prev => ({ ...prev, latitude: undefined, longitude: undefined }))
    }
  }, [state.selectedCoords])

  // ── Handlers ────────────────────────────────────────────────────────────────

  function handleCareTypeChange(careType: string, checked: boolean) {
    setSelectedCareTypes(prev => {
      const next = new Set(prev)
      if (checked) {
        next.add(careType)
      } else {
        next.delete(careType)
      }
      return next
    })
    // Clear care type error once user starts selecting
    if (checked) {
      setErrors(prev => ({ ...prev, careType: undefined }))
    }
  }

  function validate(): boolean {
    const next: FormErrors = {}

    const lat = parseFloat(latitude)
    const lng = parseFloat(longitude)

    if (latitude.trim() === '') {
      next.latitude = 'Latitude is required.'
    } else if (isNaN(lat) || lat < -90 || lat > 90) {
      next.latitude = 'Latitude must be between -90 and 90.'
    }

    if (longitude.trim() === '') {
      next.longitude = 'Longitude is required.'
    } else if (isNaN(lng) || lng < -180 || lng > 180) {
      next.longitude = 'Longitude must be between -180 and 180.'
    }

    if (selectedCareTypes.size === 0) {
      next.careType = 'At least one care type must be selected.'
    }

    setErrors(next)
    return Object.keys(next).length === 0
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()

    if (!validate()) return

    setLoading(true)
    setErrors({})

    try {
      const emergency = await createEmergency({
        latitude: parseFloat(latitude),
        longitude: parseFloat(longitude),
        urgencyLevel: URGENCY_MAP[urgency],
        requiredCareType: Array.from(selectedCareTypes).join(','),
      })

      dispatch({ type: 'SUBMIT_EMERGENCY', payload: emergency })
      onEmergencyCreated?.(emergency)

      // Immediately request a recommendation
      const recommendation = await getRecommendation(emergency.id)
      dispatch({ type: 'RECEIVE_RECOMMENDATION', payload: recommendation })
    } catch (err) {
      if (err instanceof ApiError) {
        const fieldErrors: FormErrors = {}
        if (err.body.errors && err.body.errors.length > 0) {
          for (const fe of err.body.errors) {
            // Map backend field names to our local state keys
            if (fe.field === 'latitude') fieldErrors.latitude = fe.message
            else if (fe.field === 'longitude') fieldErrors.longitude = fe.message
            else if (fe.field === 'requiredCareType') fieldErrors.careType = fe.message
            else {
              // Any unmapped field error goes into general
              fieldErrors.general = fe.message
            }
          }
        } else {
          fieldErrors.general = err.body.message
        }
        setErrors(fieldErrors)
      } else {
        setErrors({ general: 'An unexpected error occurred. Please try again.' })
      }
    } finally {
      setLoading(false)
    }
  }

  // ── Render ───────────────────────────────────────────────────────────────────

  return (
    <Box
      component="form"
      onSubmit={handleSubmit}
      aria-label="Emergency Request Form"
      noValidate
    >
      <Stack spacing={3}>
        <Typography variant="h6" component="h2">
          New Emergency Request
        </Typography>

        {/* Latitude */}
        <TextField
          label="Latitude"
          type="number"
          value={latitude}
          onChange={e => {
            setLatitude(e.target.value)
            setErrors(prev => ({ ...prev, latitude: undefined }))
          }}
          error={Boolean(errors.latitude)}
          helperText={errors.latitude}
          inputProps={{ step: 'any', min: -90, max: 90 }}
          required
          fullWidth
        />

        {/* Longitude */}
        <TextField
          label="Longitude"
          type="number"
          value={longitude}
          onChange={e => {
            setLongitude(e.target.value)
            setErrors(prev => ({ ...prev, longitude: undefined }))
          }}
          error={Boolean(errors.longitude)}
          helperText={errors.longitude}
          inputProps={{ step: 'any', min: -180, max: 180 }}
          required
          fullWidth
        />

        {/* Urgency Level */}
        <Box>
          <FormLabel component="legend" sx={{ mb: 1 }}>
            Urgency Level
          </FormLabel>
          <Slider
            value={urgency}
            onChange={(_e, value) => setUrgency(value as number)}
            min={1}
            max={4}
            step={1}
            marks={URGENCY_MARKS}
            aria-label="Urgency Level"
            aria-valuemin={1}
            aria-valuemax={4}
            aria-valuenow={urgency}
            valueLabelDisplay="off"
            sx={{ mx: 1 }}
          />
        </Box>

        {/* Required Care Type */}
        <Box>
          <FormLabel component="legend" error={Boolean(errors.careType)} required>
            Required Care Type
          </FormLabel>
          <FormGroup row>
            {CARE_TYPES.map(careType => (
              <FormControlLabel
                key={careType}
                control={
                  <Checkbox
                    checked={selectedCareTypes.has(careType)}
                    onChange={e => handleCareTypeChange(careType, e.target.checked)}
                    name={careType}
                  />
                }
                label={careType.charAt(0).toUpperCase() + careType.slice(1)}
              />
            ))}
          </FormGroup>
          {errors.careType && (
            <FormHelperText error>{errors.careType}</FormHelperText>
          )}
        </Box>

        {/* General / server error */}
        {errors.general && (
          <FormHelperText error role="alert">
            {errors.general}
          </FormHelperText>
        )}

        {/* Submit */}
        <Button
          type="submit"
          variant="contained"
          color="primary"
          disabled={loading}
          startIcon={loading ? <CircularProgress size={18} color="inherit" /> : null}
          fullWidth
        >
          {loading ? 'Submitting…' : 'Request Recommendation'}
        </Button>
      </Stack>
    </Box>
  )
}

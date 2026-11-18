/**
 * RecommendationCard — displays the AI-generated dispatch recommendation.
 *
 * Shows the recommended ambulance, recommended hospital, full route summary,
 * the engine's explanation text, ranked alternatives, and the
 * Accept / Override action buttons.
 *
 * Requirements: 5.2, 6.1, 7.1, 13.4
 */

import { useState } from 'react'
import {
  Box,
  Button,
  Card,
  CardActions,
  CardContent,
  Chip,
  CircularProgress,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material'

import { useAppContext } from '../context/AppContext'
import { recordDecision, ApiError } from '../api/ApiClient'
// Import OverrideDialog component for manual overrides
import OverrideDialog from './OverrideDialog'
import type { Recommendation } from '../types'

// ── Props ─────────────────────────────────────────────────────────────────────

interface RecommendationCardProps {
  recommendation: Recommendation
  emergencyId: number
  onAccepted?: () => void
  onOverride?: () => void
}

// ── Helpers ───────────────────────────────────────────────────────────────────

/**
 * Formats a raw second count into a human-readable "X min Y sec" string.
 * e.g. 265 → "4 min 25 sec"
 */
function formatSeconds(s: number): string {
  const mins = Math.floor(s / 60)
  const secs = s % 60
  return `${mins} min ${secs} sec`
}

// ── Component ─────────────────────────────────────────────────────────────────

export function RecommendationCard({
  recommendation,
  emergencyId,
  onAccepted,
  onOverride,
}: RecommendationCardProps) {
  const { state, dispatch } = useAppContext()
+  const [overrideOpen, setOverrideOpen] = useState(false)
+  const handleOverrideClick = () => {
+    setOverrideOpen(true)
+  }
+  const handleOverrideClose = () => {
+    setOverrideOpen(false)
+  }
+  const handleOverrideSubmitted = () => {
+    // propagate to parent if needed
+    onOverride?.()
+    setOverrideOpen(false)
+  }
+  const { ambulance, hospital, route, explanation, alternatives } =
+    recommendation

  const { state, dispatch } = useAppContext()
  const [loading, setLoading] = useState(false)
  const [acceptError, setAcceptError] = useState<string | null>(null)

  // Buttons are disabled once a decision has already been recorded
  // (pendingDecision flips to false after RECORD_DECISION is dispatched).
  const buttonsDisabled = !state.pendingDecision || loading

  const { ambulance, hospital, route, explanation, alternatives } =
    recommendation

  // Locate the two route segments for the breakdown table
  const ambulanceToScene = route.segments.find(
    (seg) => seg.leg === 'ambulance_to_scene',
  )
  const sceneToHospital = route.segments.find(
    (seg) => seg.leg === 'scene_to_hospital',
  )

  // ── Accept handler ─────────────────────────────────────────────────────────

  async function handleAccept() {
    setLoading(true)
    setAcceptError(null)
    try {
      await recordDecision(emergencyId, { action: 'accept' })
      dispatch({ type: 'RECORD_DECISION' })
      onAccepted?.()
    } catch (err) {
      if (err instanceof ApiError) {
        setAcceptError(err.body.message)
      } else {
        setAcceptError('An unexpected error occurred. Please try again.')
      }
    } finally {
      setLoading(false)
    }
  }

  // ── Render ─────────────────────────────────────────────────────────────────

  return (
    <Card variant="outlined" sx={{ mt: 2 }}>
      <CardContent>
        {/* ── Ambulance section ── */}
        <Typography variant="h6" gutterBottom>
          Recommended Ambulance
        </Typography>
        <Typography variant="subtitle1" fontWeight="bold">
          {ambulance.name}
        </Typography>
        <Typography variant="body2" color="text.secondary" gutterBottom>
          ETA to scene: {formatSeconds(ambulance.etaToSceneSeconds)}
        </Typography>
        {ambulance.equipment.length > 0 && (
          <Stack direction="row" flexWrap="wrap" gap={0.5} sx={{ mb: 2 }}>
            {ambulance.equipment.map((item) => (
              <Chip key={item} label={item} size="small" />
            ))}
          </Stack>
        )}

        {/* ── Hospital section ── */}
        <Typography variant="h6" gutterBottom>
          Recommended Hospital
        </Typography>
        <Typography variant="subtitle1" fontWeight="bold">
          {hospital.name}
        </Typography>
        <Typography variant="body2" color="text.secondary" gutterBottom>
          ETA from scene: {formatSeconds(hospital.etaFromSceneSeconds)}
        </Typography>
        {hospital.capabilities.length > 0 && (
          <Stack direction="row" flexWrap="wrap" gap={0.5} sx={{ mb: 2 }}>
            {hospital.capabilities.map((cap) => (
              <Chip key={cap} label={cap} size="small" color="primary" variant="outlined" />
            ))}
          </Stack>
        )}

        {/* ── Route summary ── */}
        <Typography variant="h6" gutterBottom>
          Route Summary
        </Typography>
        <Typography variant="body2" sx={{ mb: 1 }}>
          Total ETA: <strong>{formatSeconds(route.totalDurationSeconds)}</strong>
        </Typography>
        <Table size="small" sx={{ mb: 2 }}>
          <TableHead>
            <TableRow>
              <TableCell>
                <strong>Leg</strong>
              </TableCell>
              <TableCell>
                <strong>Duration</strong>
              </TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            <TableRow>
              <TableCell>Ambulance → Scene</TableCell>
              <TableCell>
                {ambulanceToScene
                  ? formatSeconds(ambulanceToScene.durationSeconds)
                  : formatSeconds(ambulance.etaToSceneSeconds)}
              </TableCell>
            </TableRow>
            <TableRow>
              <TableCell>Scene → Hospital</TableCell>
              <TableCell>
                {sceneToHospital
                  ? formatSeconds(sceneToHospital.durationSeconds)
                  : formatSeconds(hospital.etaFromSceneSeconds)}
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>

        {/* ── Explanation ── */}
        <Typography variant="h6" gutterBottom>
          Explanation
        </Typography>
        <Paper variant="outlined" sx={{ p: 2, mb: 2, bgcolor: 'grey.50' }}>
          <Typography variant="body2">{explanation}</Typography>
        </Paper>

        {/* ── Alternatives table ── */}
        {alternatives.length > 0 && (
          <Box sx={{ mb: 1 }}>
            <Typography variant="h6" gutterBottom>
              Alternatives
            </Typography>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>
                    <strong>Ambulance</strong>
                  </TableCell>
                  <TableCell>
                    <strong>Hospital</strong>
                  </TableCell>
                  <TableCell>
                    <strong>Total ETA</strong>
                  </TableCell>
                  <TableCell>
                    <strong>Reason</strong>
                  </TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {alternatives.map((alt) => (
                  <TableRow key={`${alt.ambulanceId}-${alt.hospitalId}`}>
                    <TableCell>{alt.ambulanceName}</TableCell>
                    <TableCell>{alt.hospitalName}</TableCell>
                    <TableCell>{formatSeconds(alt.totalEtaSeconds)}</TableCell>
                    <TableCell>{alt.reason}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Box>
        )}

        {/* ── Accept error feedback ── */}
        {acceptError && (
          <Typography variant="body2" color="error" sx={{ mt: 1 }}>
            {acceptError}
          </Typography>
        )}
      </CardContent>

      {/* ── Action buttons ── */}
      <CardActions sx={{ px: 2, pb: 2, gap: 1 }}>
        <Button
          variant="contained"
          disabled={buttonsDisabled}
          onClick={handleAccept}
          sx={{
            bgcolor: 'success.main',
            '&:hover': { bgcolor: 'success.dark' },
          }}
          startIcon={loading ? <CircularProgress size={16} color="inherit" /> : null}
        >
          {loading ? 'Accepting…' : 'Accept Recommendation'}
        </Button>
        <Button
          variant="outlined"
          disabled={buttonsDisabled}
          onClick={handleOverrideClick}
        >
          Override
        </Button>
      </CardActions>
    </Card>
  )
}

export default RecommendationCard

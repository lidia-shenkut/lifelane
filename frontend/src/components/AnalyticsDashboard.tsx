/**
 * AnalyticsDashboard — summary stats panel for dispatch analytics.
 *
 * Displays four metric cards (total requests, average ETA, override rate,
 * ML MAE when available) and a scenario breakdown table. A refresh button
 * re-fetches data from `GET /api/analytics/summary` and dispatches
 * `REFRESH_ANALYTICS` to update global state.
 *
 * Requirements: 10.1, 13.2
 */

import { useEffect, useState } from 'react'
import {
  Alert,
  Box,
  CircularProgress,
  Grid,
  IconButton,
  LinearProgress,
  Paper,
  Snackbar,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Tooltip,
  Typography,
} from '@mui/material'
import RefreshIcon from '@mui/icons-material/Refresh'

import { useAppContext } from '../context/AppContext'
import { getAnalytics, ApiError } from '../api/ApiClient'
import type { AnalyticsSummary } from '../types'

// ── Helpers ───────────────────────────────────────────────────────────────────

/**
 * Formats raw seconds into "X min Y sec".
 * e.g. 512 → "8 min 32 sec"
 */
function formatSeconds(s: number): string {
  const mins = Math.floor(s / 60)
  const secs = Math.round(s % 60)
  return `${mins} min ${secs} sec`
}

// ── Metric Card ───────────────────────────────────────────────────────────────

interface MetricCardProps {
  title: string
  value: React.ReactNode
  subtitle?: React.ReactNode
}

function MetricCard({ title, value, subtitle }: MetricCardProps) {
  return (
    <Paper
      variant="outlined"
      sx={{ p: 2, height: '100%', display: 'flex', flexDirection: 'column', gap: 0.5 }}
    >
      <Typography variant="overline" color="text.secondary" lineHeight={1.4}>
        {title}
      </Typography>
      <Typography variant="h4" fontWeight="bold">
        {value}
      </Typography>
      {subtitle && (
        <Box sx={{ mt: 0.5 }}>
          {subtitle}
        </Box>
      )}
    </Paper>
  )
}

// ── Component ─────────────────────────────────────────────────────────────────

export function AnalyticsDashboard() {
  const { state, dispatch } = useAppContext()
  const [loading, setLoading] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  const analytics: AnalyticsSummary | null = state.analytics

  // ── Fetch analytics ────────────────────────────────────────────────────────

  async function fetchAnalytics() {
    setLoading(true)
    try {
      const data = await getAnalytics()
      dispatch({ type: 'REFRESH_ANALYTICS', payload: data })
    } catch (err) {
      if (err instanceof ApiError) {
        setErrorMessage(err.body.message)
      } else {
        setErrorMessage('Failed to load analytics. Please try again.')
      }
    } finally {
      setLoading(false)
    }
  }

  // Auto-fetch on mount
  useEffect(() => {
    void fetchAnalytics()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // ── Render ─────────────────────────────────────────────────────────────────

  return (
    <Box>
      {/* Header row */}
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          mb: 2,
        }}
      >
        <Typography variant="h5" component="h2">
          Analytics Dashboard
        </Typography>
        <Tooltip title="Refresh analytics data">
          <span>
            <IconButton
              onClick={() => void fetchAnalytics()}
              disabled={loading}
              aria-label="Refresh analytics data"
              color="primary"
            >
              {loading ? (
                <CircularProgress size={20} color="inherit" />
              ) : (
                <RefreshIcon />
              )}
            </IconButton>
          </span>
        </Tooltip>
      </Box>

      {/* Empty state before first load */}
      {!analytics && !loading && (
        <Typography variant="body2" color="text.secondary">
          No analytics data available. Click refresh to load.
        </Typography>
      )}

      {/* Loading skeleton hint */}
      {loading && !analytics && (
        <Box sx={{ mt: 2 }}>
          <CircularProgress aria-label="Loading analytics" />
        </Box>
      )}

      {analytics && (
        <Grid container spacing={2}>
          {/* ── Row 1: Metric cards ── */}

          {/* Total Requests */}
          <Grid item xs={12} sm={6} md={3}>
            <MetricCard
              title="Total Requests"
              value={analytics.totalEmergencyRequests}
            />
          </Grid>

          {/* Average ETA */}
          <Grid item xs={12} sm={6} md={3}>
            <MetricCard
              title="Avg ETA"
              value={formatSeconds(analytics.averageTotalEtaSeconds)}
            />
          </Grid>

          {/* Override Rate */}
          <Grid item xs={12} sm={6} md={3}>
            <MetricCard
              title="Override Rate"
              value={`${analytics.overrideRatePercent.toFixed(1)}%`}
              subtitle={
                <LinearProgress
                  variant="determinate"
                  value={Math.min(analytics.overrideRatePercent, 100)}
                  aria-label="Override rate progress"
                  sx={{ height: 8, borderRadius: 4 }}
                />
              }
            />
          </Grid>

          {/* ML MAE card — only when ML metrics are present */}
          {analytics.mlMetrics ? (
            <Grid item xs={12} sm={6} md={3}>
              <MetricCard
                title="ML MAE"
                value={`${analytics.mlMetrics.meanAbsoluteError.toFixed(1)}s`}
                subtitle={
                  <Typography variant="body2" color="text.secondary">
                    {analytics.mlMetrics.improvementPercent.toFixed(1)}% improvement
                    over OSRM baseline
                  </Typography>
                }
              />
            </Grid>
          ) : (
            /* Placeholder card so the row is balanced when ML is off */
            <Grid item xs={12} sm={6} md={3}>
              <MetricCard
                title="ML Prediction"
                value="—"
                subtitle={
                  <Typography variant="body2" color="text.secondary">
                    ML feature disabled
                  </Typography>
                }
              />
            </Grid>
          )}

          {/* ── Row 2: Scenario breakdown ── */}
          <Grid item xs={12}>
            <Paper variant="outlined" sx={{ p: 2 }}>
              <Typography variant="h6" gutterBottom>
                Recommendations per Scenario
              </Typography>

              {Object.keys(analytics.recommendationsPerScenario).length === 0 ? (
                <Typography variant="body2" color="text.secondary">
                  No scenario data recorded yet.
                </Typography>
              ) : (
                <Table size="small" aria-label="Recommendations per scenario">
                  <TableHead>
                    <TableRow>
                      <TableCell>
                        <strong>Scenario</strong>
                      </TableCell>
                      <TableCell align="right">
                        <strong>Count</strong>
                      </TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {Object.entries(analytics.recommendationsPerScenario).map(
                      ([scenario, count]) => (
                        <TableRow key={scenario}>
                          <TableCell>{scenario}</TableCell>
                          <TableCell align="right">{count}</TableCell>
                        </TableRow>
                      ),
                    )}
                  </TableBody>
                </Table>
              )}
            </Paper>
          </Grid>
        </Grid>
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

export default AnalyticsDashboard

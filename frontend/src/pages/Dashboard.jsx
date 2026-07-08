import React, { useEffect, useState } from 'react'
import {
  ResponsiveContainer, LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip,
  BarChart, Bar, Cell
} from 'recharts'
import api from '../api/axios.js'
import StatusStamp from '../components/StatusStamp.jsx'
import AiChatWidget from '../components/AiChatWidget.jsx'

const STATUS_COLORS = {
  SUBMITTED: '#2F855A',
  DRAFT: '#8A93A3',
  PENDING: '#B7791F',
  LATE: '#C0392B'
}

export default function Dashboard() {
  const [summary, setSummary] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    api.get('/api/dashboard/summary')
      .then(({ data }) => setSummary(data))
      .catch((err) => setError(err?.response?.data?.message || 'Could not load the dashboard.'))
      .finally(() => setLoading(false))
  }, [])

  const statusBarData = summary
    ? Object.entries(
        summary.submissionStatusByMember.reduce((acc, s) => {
          acc[s.status] = (acc[s.status] || 0) + 1
          return acc
        }, {})
      ).map(([status, count]) => ({ status, count }))
    : []

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Team Dashboard</h1>
          <div className="sub">Live view of this week's submissions, blockers, and workload.</div>
        </div>
      </div>

      {loading && <div className="loading-dots">Loading dashboard…</div>}
      {error && <div className="error-banner">{error}</div>}

      {summary && (
        <>
          <div className="stat-grid">
            <div className="stat-card">
              <div className="label">Submitted this week</div>
              <div className="value">{summary.reportsSubmittedThisWeek} / {summary.totalTeamMembers}</div>
            </div>
            <div className="stat-card">
              <div className="label">Compliance rate</div>
              <div className="value">{summary.complianceRate}%</div>
            </div>
            <div className="stat-card">
              <div className="label">Open blockers</div>
              <div className="value">{summary.openBlockersCount}</div>
            </div>
            <div className="stat-card">
              <div className="label">Team members</div>
              <div className="value">{summary.totalTeamMembers}</div>
            </div>
          </div>

          <div className="chart-grid">
            <div className="card">
              <h3>Reports submitted — last 8 weeks</h3>
              <div className="card-sub">Team-wide report volume over time</div>
              <ResponsiveContainer width="100%" height={220}>
                <LineChart data={summary.tasksCompletedTrend}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#E2E4E9" />
                  <XAxis dataKey="week" tick={{ fontSize: 11 }} />
                  <YAxis allowDecimals={false} tick={{ fontSize: 11 }} />
                  <Tooltip />
                  <Line type="monotone" dataKey="reportCount" stroke="#2E6F6E" strokeWidth={2} dot={{ r: 3 }} />
                </LineChart>
              </ResponsiveContainer>
            </div>

            <div className="card">
              <h3>Submission status</h3>
              <div className="card-sub">This week, by member</div>
              <ResponsiveContainer width="100%" height={220}>
                <BarChart data={statusBarData}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#E2E4E9" />
                  <XAxis dataKey="status" tick={{ fontSize: 11 }} />
                  <YAxis allowDecimals={false} tick={{ fontSize: 11 }} />
                  <Tooltip />
                  <Bar dataKey="count" radius={[4, 4, 0, 0]}>
                    {statusBarData.map((entry, i) => (
                      <Cell key={i} fill={STATUS_COLORS[entry.status] || '#2E6F6E'} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>

          <div className="chart-grid">
            <div className="card">
              <h3>Workload by project</h3>
              <div className="card-sub">Report count per project, this week</div>
              <ResponsiveContainer width="100%" height={220}>
                <BarChart data={summary.workloadByProject} layout="vertical">
                  <CartesianGrid strokeDasharray="3 3" stroke="#E2E4E9" />
                  <XAxis type="number" allowDecimals={false} tick={{ fontSize: 11 }} />
                  <YAxis type="category" dataKey="projectName" width={120} tick={{ fontSize: 11 }} />
                  <Tooltip />
                  <Bar dataKey="reportCount" fill="#B7791F" radius={[0, 4, 4, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>

            <div className="card">
              <h3>Recent activity</h3>
              <div className="card-sub">Latest report updates across the team</div>
              <div>
                {summary.recentActivity.length === 0 && <div className="empty-state">No activity yet.</div>}
                {summary.recentActivity.map((a, i) => (
                  <div key={i} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '9px 0', borderBottom: i < summary.recentActivity.length - 1 ? '1px solid var(--line)' : 'none' }}>
                    <div>
                      <div style={{ fontWeight: 600, fontSize: 13.5 }}>{a.memberName}</div>
                      <div style={{ fontSize: 12, color: 'var(--ink-soft)' }}>{a.projectName}</div>
                    </div>
                    <StatusStamp status={a.status} />
                  </div>
                ))}
              </div>
            </div>
          </div>
        </>
      )}

      {/* Always rendered, independent of whether the summary above loaded
          successfully - so the assistant is never blocked by a dashboard error. */}
      <AiChatWidget />
    </div>
  )
}

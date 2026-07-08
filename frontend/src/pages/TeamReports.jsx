import React, { useEffect, useState } from 'react'
import api from '../api/axios.js'
import StatusStamp from '../components/StatusStamp.jsx'

export default function TeamReports() {
  const [reports, setReports] = useState([])
  const [members, setMembers] = useState([])
  const [projects, setProjects] = useState([])
  const [loading, setLoading] = useState(true)

  const [userId, setUserId] = useState('')
  const [projectId, setProjectId] = useState('')
  const [status, setStatus] = useState('')
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')

  async function loadFilters() {
    const [membersRes, projectsRes] = await Promise.all([
      api.get('/api/users/team-members'),
      api.get('/api/projects')
    ])
    setMembers(membersRes.data)
    setProjects(projectsRes.data)
  }

  async function loadReports() {
    setLoading(true)
    const params = {}
    if (userId) params.userId = userId
    if (projectId) params.projectId = projectId
    if (status) params.status = status
    if (from) params.from = from
    if (to) params.to = to
    const { data } = await api.get('/api/reports/team', { params })
    setReports(data)
    setLoading(false)
  }

  useEffect(() => { loadFilters() }, [])
  useEffect(() => { loadReports() }, [userId, projectId, status, from, to])

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Team Reports</h1>
          <div className="sub">Filter across the whole team by member, project, status, or date range.</div>
        </div>
      </div>

      <div className="filters-row card">
        <div className="field">
          <label>Team member</label>
          <select value={userId} onChange={(e) => setUserId(e.target.value)}>
            <option value="">All members</option>
            {members.map((m) => <option key={m.id} value={m.id}>{m.fullName}</option>)}
          </select>
        </div>
        <div className="field">
          <label>Project</label>
          <select value={projectId} onChange={(e) => setProjectId(e.target.value)}>
            <option value="">All projects</option>
            {projects.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
          </select>
        </div>
        <div className="field">
          <label>Status</label>
          <select value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="">Any status</option>
            <option value="SUBMITTED">Submitted</option>
            <option value="DRAFT">Draft</option>
            <option value="LATE">Late</option>
          </select>
        </div>
        <div className="field">
          <label>Week from</label>
          <input type="date" value={from} onChange={(e) => setFrom(e.target.value)} />
        </div>
        <div className="field">
          <label>Week to</label>
          <input type="date" value={to} onChange={(e) => setTo(e.target.value)} />
        </div>
      </div>

      {!loading && reports.length === 0 && (
        <div className="empty-state">No reports match these filters.</div>
      )}

      {reports.length > 0 && (
        <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
          <table>
            <thead>
              <tr>
                <th>Member</th>
                <th>Week</th>
                <th>Project</th>
                <th>Status</th>
                <th>Blockers</th>
              </tr>
            </thead>
            <tbody>
              {reports.map((r) => (
                <tr key={r.id}>
                  <td>{r.userFullName}</td>
                  <td className="report-week">{r.weekStartDate}</td>
                  <td>{r.projectName}</td>
                  <td><StatusStamp status={r.status} /></td>
                  <td>{r.blockers || '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

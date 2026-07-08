import React, { useEffect, useState } from 'react'
import api from '../api/axios.js'
import StatusStamp from '../components/StatusStamp.jsx'
import ReportForm from '../components/ReportForm.jsx'

export default function MyReports() {
  const [reports, setReports] = useState([])
  const [projects, setProjects] = useState([])
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [editing, setEditing] = useState(null)

  async function load() {
    setLoading(true)
    const [reportsRes, projectsRes] = await Promise.all([
      api.get('/api/reports/mine'),
      api.get('/api/projects')
    ])
    setReports(reportsRes.data)
    setProjects(projectsRes.data.filter((p) => p.active))
    setLoading(false)
  }

  useEffect(() => { load() }, [])

  async function handleSave(payload) {
    if (editing) {
      await api.put(`/api/reports/${editing.id}`, payload)
    } else {
      await api.post('/api/reports', payload)
    }
    setShowForm(false)
    setEditing(null)
    await load()
  }

  async function handleDelete(id) {
    if (!window.confirm('Delete this report? This cannot be undone.')) return
    await api.delete(`/api/reports/${id}`)
    await load()
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>My Weekly Reports</h1>
          <div className="sub">Same fields, every week — makes it easy for your manager to compare progress.</div>
        </div>
        <button
          className="btn btn-accent"
          onClick={() => { setEditing(null); setShowForm(true) }}
          disabled={projects.length === 0}
        >
          + New report
        </button>
      </div>

      {projects.length === 0 && !loading && (
        <div className="empty-state">No projects have been set up yet. Ask a manager to add one before submitting a report.</div>
      )}

      {!loading && reports.length === 0 && projects.length > 0 && (
        <div className="empty-state">You haven't submitted any reports yet. Click "New report" to get started.</div>
      )}

      {reports.map((r) => (
        <div className="report-card" key={r.id}>
          <div className="report-card-top">
            <div>
              <span className="week">Week of {r.weekStartDate} → {r.weekEndDate}</span>
              <span className="proj-tag">{r.projectName}</span>
            </div>
            <StatusStamp status={r.status} />
          </div>

          <div className="report-field">
            <div className="k">Tasks completed</div>
            <div className="v">{r.tasksCompleted}</div>
          </div>
          {r.tasksPlanned && (
            <div className="report-field">
              <div className="k">Planned next week</div>
              <div className="v">{r.tasksPlanned}</div>
            </div>
          )}
          {r.blockers && (
            <div className="report-field">
              <div className="k">Blockers</div>
              <div className="v">{r.blockers}</div>
            </div>
          )}
          {(r.hoursWorked || r.notes) && (
            <div className="report-field">
              <div className="k">Hours / Notes</div>
              <div className="v">{r.hoursWorked ? `${r.hoursWorked}h` : ''} {r.notes}</div>
            </div>
          )}

          <div className="report-card-actions">
            <button className="btn btn-ghost" onClick={() => { setEditing(r); setShowForm(true) }}>Edit</button>
            <button className="btn btn-danger" onClick={() => handleDelete(r.id)}>Delete</button>
          </div>
        </div>
      ))}

      {showForm && (
        <ReportForm
          projects={projects}
          initial={editing}
          onCancel={() => { setShowForm(false); setEditing(null) }}
          onSave={handleSave}
        />
      )}
    </div>
  )
}

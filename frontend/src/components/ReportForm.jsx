import React, { useState, useEffect } from 'react'

function mondayOf(dateStr) {
  const d = new Date(dateStr)
  const day = d.getDay()
  const diff = (day === 0 ? -6 : 1) - day
  d.setDate(d.getDate() + diff)
  return d.toISOString().slice(0, 10)
}

export default function ReportForm({ projects, initial, onCancel, onSave }) {
  const [weekStartDate, setWeekStartDate] = useState(initial?.weekStartDate || mondayOf(new Date()))
  const [projectId, setProjectId] = useState(initial?.projectId || projects[0]?.id || '')
  const [tasksCompleted, setTasksCompleted] = useState(initial?.tasksCompleted || '')
  const [tasksPlanned, setTasksPlanned] = useState(initial?.tasksPlanned || '')
  const [blockers, setBlockers] = useState(initial?.blockers || '')
  const [hoursWorked, setHoursWorked] = useState(initial?.hoursWorked ?? '')
  const [notes, setNotes] = useState(initial?.notes || '')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!projectId && projects[0]) setProjectId(projects[0].id)
  }, [projects])

  async function handleSave(submit) {
    setError('')
    if (!tasksCompleted.trim()) {
      setError('Tasks completed is required.')
      return
    }
    if (!projectId) {
      setError('Please select a project.')
      return
    }
    setSaving(true)
    try {
      await onSave({
        weekStartDate: mondayOf(weekStartDate),
        projectId: Number(projectId),
        tasksCompleted,
        tasksPlanned,
        blockers,
        hoursWorked: hoursWorked === '' ? null : Number(hoursWorked),
        notes,
        submit
      })
    } catch (err) {
      setError(err?.response?.data?.message || 'Could not save the report.')
      setSaving(false)
    }
  }

  return (
    <div className="modal-backdrop" onClick={onCancel}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h2>{initial ? 'Edit weekly report' : 'New weekly report'}</h2>
        {error && <div className="error-banner">{error}</div>}

        <div className="form-row-2">
          <div className="field">
            <label>Week starting (Monday)</label>
            <input type="date" value={weekStartDate} onChange={(e) => setWeekStartDate(e.target.value)} />
          </div>
          <div className="field">
            <label>Project / category</label>
            <select value={projectId} onChange={(e) => setProjectId(e.target.value)}>
              {projects.map((p) => (
                <option key={p.id} value={p.id}>{p.name}</option>
              ))}
            </select>
          </div>
        </div>

        <div className="field">
          <label>Tasks completed</label>
          <textarea value={tasksCompleted} onChange={(e) => setTasksCompleted(e.target.value)} placeholder="What did you get done this week?" />
        </div>

        <div className="field">
          <label>Tasks planned for next week</label>
          <textarea value={tasksPlanned} onChange={(e) => setTasksPlanned(e.target.value)} placeholder="What's next?" />
        </div>

        <div className="field">
          <label>Blockers / challenges</label>
          <textarea value={blockers} onChange={(e) => setBlockers(e.target.value)} placeholder="Anything slowing you down?" />
        </div>

        <div className="form-row-2">
          <div className="field">
            <label>Hours worked (optional)</label>
            <input type="number" min="0" step="0.5" value={hoursWorked} onChange={(e) => setHoursWorked(e.target.value)} placeholder="e.g. 38" />
          </div>
          <div className="field">
            <label>Notes / links (optional)</label>
            <input value={notes} onChange={(e) => setNotes(e.target.value)} placeholder="Links, extra context…" />
          </div>
        </div>

        <div className="modal-actions">
          <button className="btn btn-ghost" onClick={onCancel} disabled={saving}>Cancel</button>
          <button className="btn btn-ghost" onClick={() => handleSave(false)} disabled={saving}>Save draft</button>
          <button className="btn btn-accent" onClick={() => handleSave(true)} disabled={saving}>
            {saving ? 'Saving…' : 'Submit report'}
          </button>
        </div>
      </div>
    </div>
  )
}

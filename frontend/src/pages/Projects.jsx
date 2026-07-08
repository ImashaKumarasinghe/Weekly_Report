import React, { useEffect, useState } from 'react'
import api from '../api/axios.js'

export default function Projects() {
  const [projects, setProjects] = useState([])
  const [members, setMembers] = useState([])
  const [loading, setLoading] = useState(true)

  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [selectedMemberIds, setSelectedMemberIds] = useState([])
  const [editingId, setEditingId] = useState(null)
  const [error, setError] = useState('')

  async function load() {
    setLoading(true)
    const [projectsRes, membersRes] = await Promise.all([
      api.get('/api/projects'),
      api.get('/api/users/team-members')
    ])
    setProjects(projectsRes.data)
    setMembers(membersRes.data)
    setLoading(false)
  }

  useEffect(() => { load() }, [])

  function startEdit(p) {
    setEditingId(p.id)
    setName(p.name)
    setDescription(p.description || '')
    setSelectedMemberIds(p.members.map((m) => m.id))
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  function resetForm() {
    setEditingId(null)
    setName('')
    setDescription('')
    setSelectedMemberIds([])
    setError('')
  }

  function toggleMember(id) {
    setSelectedMemberIds((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]
    )
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    try {
      const payload = { name, description, memberIds: selectedMemberIds }
      if (editingId) {
        await api.put(`/api/projects/${editingId}`, payload)
      } else {
        await api.post('/api/projects', payload)
      }
      resetForm()
      await load()
    } catch (err) {
      setError(err?.response?.data?.message || 'Could not save the project.')
    }
  }

  async function handleDelete(id) {
    if (!window.confirm('Deactivate this project? Past reports referencing it stay intact.')) return
    await api.delete(`/api/projects/${id}`)
    await load()
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Projects &amp; Categories</h1>
          <div className="sub">Manage the tags team members attach to their weekly reports, and control who can see each one.</div>
        </div>
      </div>

      <div className="card" style={{ marginBottom: 24, maxWidth: 560 }}>
        <h3>{editingId ? 'Edit project' : 'Add a project'}</h3>
        <form onSubmit={handleSubmit} style={{ marginTop: 12 }}>
          {error && <div className="error-banner">{error}</div>}
          <div className="field">
            <label>Name</label>
            <input required value={name} onChange={(e) => setName(e.target.value)} placeholder="e.g. Client A" />
          </div>
          <div className="field">
            <label>Description (optional)</label>
            <input value={description} onChange={(e) => setDescription(e.target.value)} placeholder="Short description" />
          </div>

          <div className="field">
            <label>Assign team members</label>
            <div className="member-picker">
              {members.length === 0 && (
                <div style={{ fontSize: 13, color: 'var(--ink-soft)' }}>No team members have registered yet.</div>
              )}
              {members.map((m) => (
                <label key={m.id} className="member-check">
                  <input
                    type="checkbox"
                    checked={selectedMemberIds.includes(m.id)}
                    onChange={() => toggleMember(m.id)}
                  />
                  {m.fullName}
                </label>
              ))}
            </div>
            <div style={{ fontSize: 12, color: 'var(--ink-soft)', marginTop: 6 }}>
              {selectedMemberIds.length === 0
                ? 'Leave everyone unchecked to keep this project open to the whole team.'
                : `Only the ${selectedMemberIds.length} selected member(s) will see this project.`}
            </div>
          </div>

          <div style={{ display: 'flex', gap: 10, marginTop: 4 }}>
            <button className="btn btn-accent" type="submit">{editingId ? 'Save changes' : 'Add project'}</button>
            {editingId && <button className="btn btn-ghost" type="button" onClick={resetForm}>Cancel</button>}
          </div>
        </form>
      </div>

      {!loading && projects.length === 0 && (
        <div className="empty-state">No projects yet. Add your first one above.</div>
      )}

      {projects.map((p) => (
        <div className="project-row" key={p.id}>
          <div>
            <div className="name">{p.name} {!p.active && <span style={{ color: 'var(--ink-soft)', fontWeight: 400 }}>(inactive)</span>}</div>
            {p.description && <div className="desc">{p.description}</div>}
            <div className="desc" style={{ marginTop: 4 }}>
              {p.members.length === 0
                ? 'Open to all team members'
                : `Assigned: ${p.members.map((m) => m.fullName).join(', ')}`}
            </div>
          </div>
          <div style={{ display: 'flex', gap: 8 }}>
            <button className="btn btn-ghost" onClick={() => startEdit(p)}>Edit</button>
            {p.active && <button className="btn btn-danger" onClick={() => handleDelete(p.id)}>Deactivate</button>}
          </div>
        </div>
      ))}
    </div>
  )
}

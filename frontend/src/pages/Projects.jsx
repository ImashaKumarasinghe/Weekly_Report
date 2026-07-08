import React, { useEffect, useState } from 'react'
import api from '../api/axios.js'

export default function Projects() {
  const [projects, setProjects] = useState([])
  const [loading, setLoading] = useState(true)
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [editingId, setEditingId] = useState(null)
  const [error, setError] = useState('')

  async function load() {
    setLoading(true)
    const { data } = await api.get('/api/projects')
    setProjects(data)
    setLoading(false)
  }

  useEffect(() => { load() }, [])

  function startEdit(p) {
    setEditingId(p.id)
    setName(p.name)
    setDescription(p.description || '')
  }

  function resetForm() {
    setEditingId(null)
    setName('')
    setDescription('')
    setError('')
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    try {
      if (editingId) {
        await api.put(`/api/projects/${editingId}`, { name, description })
      } else {
        await api.post('/api/projects', { name, description })
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
          <div className="sub">Manage the tags team members attach to their weekly reports.</div>
        </div>
      </div>

      <div className="card" style={{ marginBottom: 24, maxWidth: 520 }}>
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
          <div style={{ display: 'flex', gap: 10 }}>
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

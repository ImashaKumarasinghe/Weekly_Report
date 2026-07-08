import React, { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'

export default function Register() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [role, setRole] = useState('TEAM_MEMBER')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const data = await register(fullName, email, password, role)
      navigate(data.role === 'MANAGER' ? '/dashboard' : '/reports')
    } catch (err) {
      setError(err?.response?.data?.message || 'Could not create your account.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-shell">
      <div className="auth-hero">
        <div className="brand">Ledger<span className="dot">.</span></div>
        <div>
          <h1>One fixed report format. Total visibility for managers.</h1>
          <p style={{ marginTop: 16 }}>
            Every report — same fields, same order, every week — so managers can compare
            progress, catch blockers early, and see workload across projects at a glance.
          </p>
        </div>
        <div className="auth-stamp">● role-based access</div>
      </div>
      <div className="auth-panel">
        <form className="auth-card" onSubmit={handleSubmit}>
          <h2>Create your account</h2>
          <p className="sub">Get set up as a team member or a manager.</p>
          {error && <div className="error-banner">{error}</div>}
          <div className="field">
            <label>Full name</label>
            <input required value={fullName} onChange={(e) => setFullName(e.target.value)} placeholder="Jordan Lee" />
          </div>
          <div className="field">
            <label>Email</label>
            <input type="email" required value={email} onChange={(e) => setEmail(e.target.value)} placeholder="you@company.com" />
          </div>
          <div className="field">
            <label>Password</label>
            <input type="password" required minLength={6} value={password} onChange={(e) => setPassword(e.target.value)} placeholder="At least 6 characters" />
          </div>
          <div className="field">
            <label>Role</label>
            <select value={role} onChange={(e) => setRole(e.target.value)}>
              <option value="TEAM_MEMBER">Team Member</option>
              <option value="MANAGER">Manager</option>
            </select>
          </div>
          <button className="btn btn-primary" disabled={loading} type="submit">
            {loading ? 'Creating account…' : 'Create account'}
          </button>
          <div className="auth-switch">
            Already have an account? <Link to="/login">Sign in</Link>
          </div>
        </form>
      </div>
    </div>
  )
}

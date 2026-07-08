import React, { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const data = await login(email, password)
      navigate(data.role === 'MANAGER' ? '/dashboard' : '/reports')
    } catch (err) {
      setError(err?.response?.data?.message || 'Could not sign in. Check your credentials.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-shell">
      <div className="auth-hero">
        <div className="brand">Ledger<span className="dot">.</span></div>
        <div>
          <h1>Weekly reports, one consistent format for the whole team.</h1>
          <p style={{ marginTop: 16 }}>
            Submit your status in the same structure every week, and give managers
            a live view of progress, blockers, and workload across every project.
          </p>
        </div>
        <div className="auth-stamp">● week of {new Date().toISOString().slice(0, 10)}</div>
      </div>
      <div className="auth-panel">
        <form className="auth-card" onSubmit={handleSubmit}>
          <h2>Sign in</h2>
          <p className="sub">Enter your details to access your reports.</p>
          {error && <div className="error-banner">{error}</div>}
          <div className="field">
            <label>Email</label>
            <input type="email" required value={email} onChange={(e) => setEmail(e.target.value)} placeholder="you@company.com" />
          </div>
          <div className="field">
            <label>Password</label>
            <input type="password" required value={password} onChange={(e) => setPassword(e.target.value)} placeholder="••••••••" />
          </div>
          <button className="btn btn-primary" disabled={loading} type="submit">
            {loading ? 'Signing in…' : 'Sign in'}
          </button>
          <div className="auth-switch">
            No account yet? <Link to="/register">Create one</Link>
          </div>
        </form>
      </div>
    </div>
  )
}

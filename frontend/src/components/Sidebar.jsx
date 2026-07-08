import React from 'react'
import { NavLink } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'

export default function Sidebar() {
  const { user, isManager, logout } = useAuth()

  return (
    <aside className="sidebar">
      <div className="brand">Ledger<span className="dot">.</span></div>
      <nav>
        <NavLink to="/reports" className={({ isActive }) => (isActive ? 'active' : '')}>
          My Reports
        </NavLink>
        {isManager && (
          <>
            <NavLink to="/dashboard" className={({ isActive }) => (isActive ? 'active' : '')}>
              Team Dashboard
            </NavLink>
            <NavLink to="/team-reports" className={({ isActive }) => (isActive ? 'active' : '')}>
              Team Reports
            </NavLink>
            <NavLink to="/projects" className={({ isActive }) => (isActive ? 'active' : '')}>
              Projects
            </NavLink>
          </>
        )}
      </nav>
      <div className="user-box">
        <div className="user-name">{user?.fullName}</div>
        <div className="user-role">{user?.role?.replace('_', ' ')}</div>
        <button className="logout" onClick={logout}>Sign out</button>
      </div>
    </aside>
  )
}

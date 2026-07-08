import React from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from './context/AuthContext.jsx'
import Sidebar from './components/Sidebar.jsx'
import ProtectedRoute from './components/ProtectedRoute.jsx'
import Login from './pages/Login.jsx'
import Register from './pages/Register.jsx'
import MyReports from './pages/MyReports.jsx'
import Dashboard from './pages/Dashboard.jsx'
import TeamReports from './pages/TeamReports.jsx'
import Projects from './pages/Projects.jsx'

function AppLayout({ children }) {
  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main">{children}</main>
    </div>
  )
}

export default function App() {
  const { user } = useAuth()

  return (
    <Routes>
      <Route path="/login" element={user ? <Navigate to="/reports" replace /> : <Login />} />
      <Route path="/register" element={user ? <Navigate to="/reports" replace /> : <Register />} />

      <Route path="/reports" element={
        <ProtectedRoute><AppLayout><MyReports /></AppLayout></ProtectedRoute>
      } />

      <Route path="/dashboard" element={
        <ProtectedRoute managerOnly><AppLayout><Dashboard /></AppLayout></ProtectedRoute>
      } />

      <Route path="/team-reports" element={
        <ProtectedRoute managerOnly><AppLayout><TeamReports /></AppLayout></ProtectedRoute>
      } />

      <Route path="/projects" element={
        <ProtectedRoute managerOnly><AppLayout><Projects /></AppLayout></ProtectedRoute>
      } />

      <Route path="*" element={<Navigate to={user ? '/reports' : '/login'} replace />} />
    </Routes>
  )
}

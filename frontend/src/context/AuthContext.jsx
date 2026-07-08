import React, { createContext, useContext, useState, useCallback } from 'react'
import api from '../api/axios.js'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const raw = localStorage.getItem('wrg_user')
    return raw ? JSON.parse(raw) : null
  })

  const login = useCallback(async (email, password) => {
    const { data } = await api.post('/api/auth/login', { email, password })
    persist(data)
    return data
  }, [])

  const register = useCallback(async (fullName, email, password, role) => {
    const { data } = await api.post('/api/auth/register', { fullName, email, password, role })
    persist(data)
    return data
  }, [])

  function persist(data) {
    localStorage.setItem('wrg_token', data.token)
    const userInfo = { id: data.userId, fullName: data.fullName, email: data.email, role: data.role }
    localStorage.setItem('wrg_user', JSON.stringify(userInfo))
    setUser(userInfo)
  }

  const logout = useCallback(() => {
    localStorage.removeItem('wrg_token')
    localStorage.removeItem('wrg_user')
    setUser(null)
  }, [])

  return (
    <AuthContext.Provider value={{ user, login, register, logout, isManager: user?.role === 'MANAGER' }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}

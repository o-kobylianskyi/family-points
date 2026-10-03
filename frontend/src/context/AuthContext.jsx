import {
  createContext,
  useContext,
  useEffect,
  useState,
} from 'react'

import {
  getCurrentUser,
  login as loginRequest,
} from '../api/authApi'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [currentUser, setCurrentUser] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const handleUnauthorized = () => {
      localStorage.removeItem('accessToken')
      setCurrentUser(null)
    }

    window.addEventListener(
      'auth:unauthorized',
      handleUnauthorized
    )

    const restoreSession = async () => {
      const token = localStorage.getItem('accessToken')

      if (!token) {
        setLoading(false)
        return
      }

      try {
        const user = await getCurrentUser(token)
        setCurrentUser(user)
      } catch {
        localStorage.removeItem('accessToken')
        setCurrentUser(null)
      } finally {
        setLoading(false)
      }
    }

    restoreSession()

    return () => {
      window.removeEventListener(
        'auth:unauthorized',
        handleUnauthorized
      )
    }
  }, [])

  const login = async (username, password) => {
    const loginResponse = await loginRequest(
      username,
      password
    )

    localStorage.setItem(
      'accessToken',
      loginResponse.accessToken
    )

    try {
      const user = await getCurrentUser(
        loginResponse.accessToken
      )

      setCurrentUser(user)

      return user
    } catch (error) {
      localStorage.removeItem('accessToken')
      throw error
    }
  }

  const logout = () => {
    localStorage.removeItem('accessToken')
    setCurrentUser(null)
  }

  const getAccessToken = () => {
    return localStorage.getItem('accessToken')
  }

  return (
    <AuthContext.Provider
      value={{
        currentUser,
        loading,
        login,
        logout,
        getAccessToken,
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)

  if (!context) {
    throw new Error(
      'useAuth must be used inside AuthProvider'
    )
  }

  return context
}
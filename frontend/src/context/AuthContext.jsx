import {
  createContext,
  useContext,
  useEffect,
  useRef,
  useState,
} from 'react'

import {
  getCurrentUser,
  login as loginRequest,
  refreshAccessToken,
  listAccountWorkspaces,
  activateAccountWorkspace,
  createFirstFamily as createFirstFamilyRequest,
} from '../api/authApi'

const AuthContext = createContext(null)

const LAST_ACTIVITY_KEY = 'lastActivityAt'
const IDLE_TIMEOUT_MS = 60 * 60 * 1000
const REFRESH_THRESHOLD_MS = 15 * 60 * 1000
const REFRESH_CHECK_INTERVAL_MS = 60 * 1000
const ACTIVITY_WRITE_THROTTLE_MS = 30 * 1000

function getTokenExpirationMs(token) {
  try {
    const payloadPart = token.split('.')[1]
    if (!payloadPart) return null

    const normalized = payloadPart
      .replace(/-/g, '+')
      .replace(/_/g, '/')

    const padded = normalized.padEnd(
      normalized.length + ((4 - normalized.length % 4) % 4),
      '='
    )

    const payload = JSON.parse(atob(padded))
    return payload.exp ? payload.exp * 1000 : null
  } catch {
    return null
  }
}

export function AuthProvider({ children }) {
  const [currentUser, setCurrentUser] = useState(null)
  const [loading, setLoading] = useState(true)
  const [needsWorkspace, setNeedsWorkspace] = useState(false)
  const refreshInFlight = useRef(false)
  const lastActivityWrite = useRef(0)

  const clearSession = () => {
    localStorage.removeItem('accessToken')
    localStorage.removeItem(LAST_ACTIVITY_KEY)
    setCurrentUser(null)
    setNeedsWorkspace(false)
  }

  const markActivity = () => {
    if (!localStorage.getItem('accessToken')) return

    const now = Date.now()
    if (now - lastActivityWrite.current < ACTIVITY_WRITE_THROTTLE_MS) {
      return
    }

    lastActivityWrite.current = now
    localStorage.setItem(LAST_ACTIVITY_KEY, String(now))
  }

  useEffect(() => {
    const handleUnauthorized = () => {
      clearSession()
    }

    window.addEventListener(
      'auth:unauthorized',
      handleUnauthorized
    )

    const restoreSession = async () => {
      const token = localStorage.getItem('accessToken')
      const lastActivity = Number(
        localStorage.getItem(LAST_ACTIVITY_KEY) || 0
      )

      if (!token) {
        setLoading(false)
        return
      }

      if (
        lastActivity &&
        Date.now() - lastActivity >= IDLE_TIMEOUT_MS
      ) {
        clearSession()
        setLoading(false)
        return
      }

      try {
        const choices = await listAccountWorkspaces(token)
        // Keep a valid current workspace when already selected.
        let user = null
        if (choices.length && getTokenExpirationMs(token)) {
          try {
            user = await getCurrentUser(token)
          } catch {
            // Global account sessions have no member context yet.
          }
        }
        if (!user && choices.length === 1) {
          const selected = await activateAccountWorkspace(token, choices[0].workspaceId)
          const verified = await getCurrentUser(selected.accessToken)
          if (Number(verified.workspaceId) !== Number(choices[0].workspaceId)) {
            throw new Error('Unexpected workspace context')
          }
          localStorage.setItem('accessToken', selected.accessToken)
          user = verified
        }
        setCurrentUser(user)
        setNeedsWorkspace(!user)
        markActivity()
      } catch {
        clearSession()
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

  useEffect(() => {
    const activityEvents = [
      'pointerdown',
      'keydown',
      'touchstart',
      'scroll',
    ]

    activityEvents.forEach((eventName) => {
      window.addEventListener(
        eventName,
        markActivity,
        { passive: true }
      )
    })

    return () => {
      activityEvents.forEach((eventName) => {
        window.removeEventListener(
          eventName,
          markActivity
        )
      })
    }
  }, [])

  useEffect(() => {
    if (!currentUser && !needsWorkspace) return

    const checkSession = async () => {
      const token = localStorage.getItem('accessToken')
      if (!token) {
        clearSession()
        return
      }

      const now = Date.now()
      const lastActivity = Number(
        localStorage.getItem(LAST_ACTIVITY_KEY) || 0
      )

      if (
        !lastActivity ||
        now - lastActivity >= IDLE_TIMEOUT_MS
      ) {
        clearSession()
        return
      }

      const expiresAt = getTokenExpirationMs(token)
      if (!expiresAt) {
        clearSession()
        return
      }

      const recentlyActive =
        now - lastActivity < REFRESH_THRESHOLD_MS

      if (
        expiresAt - now <= REFRESH_THRESHOLD_MS &&
        recentlyActive &&
        !refreshInFlight.current
      ) {
        refreshInFlight.current = true

        try {
          const refreshed = await refreshAccessToken(token)
          localStorage.setItem(
            'accessToken',
            refreshed.accessToken
          )
        } catch {
          clearSession()
        } finally {
          refreshInFlight.current = false
        }
      }
    }

    const intervalId = window.setInterval(
      checkSession,
      REFRESH_CHECK_INTERVAL_MS
    )

    checkSession()

    return () => window.clearInterval(intervalId)
  }, [currentUser, needsWorkspace])

  const login = async (username, password) => {
    const loginResponse = await loginRequest(
      username,
      password
    )

    localStorage.setItem(
      'accessToken',
      loginResponse.accessToken
    )
    localStorage.setItem(
      LAST_ACTIVITY_KEY,
      String(Date.now())
    )

    try {
      const choices = await listAccountWorkspaces(loginResponse.accessToken)
      if (choices.length === 1) {
        const selected = await activateAccountWorkspace(
          loginResponse.accessToken, choices[0].workspaceId
        )
        const user = await getCurrentUser(selected.accessToken)
        if (Number(user.workspaceId) !== Number(choices[0].workspaceId)) {
          throw new Error('Unexpected workspace context')
        }
        localStorage.setItem('accessToken', selected.accessToken)
        setCurrentUser(user)
        setNeedsWorkspace(false)
        return user
      }
      setCurrentUser(null)
      setNeedsWorkspace(true)
      return null
    } catch (error) {
      clearSession()
      throw error
    }
  }

  const getAvailableWorkspaces = async () => {
    const token = localStorage.getItem('accessToken')
    if (!token) throw new Error('Not authenticated')
    return listAccountWorkspaces(token)
  }

  const switchWorkspace = async (workspaceId) => {
    const token = localStorage.getItem('accessToken')
    if (!token) throw new Error('Not authenticated')
    const next = await activateAccountWorkspace(token, workspaceId)
    // Verify the new identity BEFORE replacing the existing session.
    const user = await getCurrentUser(next.accessToken)
    if (Number(user.workspaceId) !== Number(workspaceId)) {
      throw new Error('Unexpected workspace context')
    }
    localStorage.setItem('accessToken', next.accessToken)
    localStorage.setItem(LAST_ACTIVITY_KEY, String(Date.now()))
    setCurrentUser(user)
    setNeedsWorkspace(false)
    // Remount every feature store and clear in-memory state for the previous tenant.
    window.location.replace('/')
  }

  const createFirstFamily = async (familyName, ownerName) => {
    const token = localStorage.getItem('accessToken')
    if (!token) throw new Error('Not authenticated')
    const created = await createFirstFamilyRequest(token, familyName, ownerName)
    await switchWorkspace(created.workspaceId)
  }

  const logout = () => {
    clearSession()
  }

  const getAccessToken = () => {
    return localStorage.getItem('accessToken')
  }

  return (
    <AuthContext.Provider
      value={{
        currentUser,
        loading,
        needsWorkspace,
        login,
        logout,
        getAccessToken,
        getAvailableWorkspaces,
        switchWorkspace,
        createFirstFamily,
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

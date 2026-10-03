import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'

import { useAuth } from '../context/AuthContext'
import LanguageSwitcher from '../components/LanguageSwitcher'

function LoginPage() {
  const { t } = useTranslation()

  const {
    currentUser,
    loading: authLoading,
    login,
  } = useAuth()

  const navigate = useNavigate()

  const [username, setUsername] = useState('oleh')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  if (authLoading) {
    return (
      <div className="app-loading">
        {t('common.loading')}
      </div>
    )
  }

  if (currentUser) {
    return <Navigate to="/" replace />
  }

  const handleSubmit = async (event) => {
    event.preventDefault()

    setError('')
    setLoading(true)

    try {
      await login(username, password)

      setPassword('')
      navigate('/', { replace: true })
    } catch {
      setError(t('login.invalidCredentials'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-page">
      <div className="login-card">
        <div className="login-language">
          <LanguageSwitcher />
        </div>

        <div className="brand">
          <div className="brand-icon">FP</div>

          <div>
            <h1>FamilyPoints</h1>
            <p>{t('login.subtitle')}</p>
          </div>
        </div>

        <form onSubmit={handleSubmit}>
          <label>
            {t('login.username')}

            <input
              type="text"
              value={username}
              onChange={(event) =>
                setUsername(event.target.value)
              }
              autoComplete="username"
              disabled={loading}
            />
          </label>

          <label>
            {t('login.password')}

            <div className="password-input-wrapper">
              <input
                type={showPassword ? 'text' : 'password'}
                value={password}
                onChange={(event) =>
                  setPassword(event.target.value)
                }
                autoComplete="current-password"
                disabled={loading}
              />

              <button
                type="button"
                className="password-toggle"
                onClick={() => setShowPassword((value) => !value)}
                disabled={loading}
                aria-label={showPassword ? 'Приховати пароль' : 'Показати пароль'}
                title={showPassword ? 'Приховати пароль' : 'Показати пароль'}
              >
                {showPassword ? (
                  /* eye-off */
                  <svg
                    width="20"
                    height="20"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    aria-hidden="true"
                  >
                    <path d="M3 3l18 18" />
                    <path d="M10.6 10.6a2 2 0 0 0 2.8 2.8" />
                    <path d="M9.9 4.2A10.8 10.8 0 0 1 12 4c5.5 0 9 5 9 5a15.6 15.6 0 0 1-2.1 2.7" />
                    <path d="M6.6 6.6C4.4 8 3 10 3 10s3.5 5 9 5a10.5 10.5 0 0 0 3-.4" />
                  </svg>
                ) : (
                  /* eye */
                  <svg
                    width="20"
                    height="20"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    aria-hidden="true"
                  >
                    <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z" />
                    <circle cx="12" cy="12" r="3" />
                  </svg>
                )}
              </button>
            </div>
          </label>

          {error && (
            <div className="error-message">
              {error}
            </div>
          )}

          <button
            type="submit"
            disabled={loading}
          >
            {loading
              ? t('login.submitting')
              : t('login.submit')}
          </button>
        </form>
      </div>
    </div>
  )
}

export default LoginPage
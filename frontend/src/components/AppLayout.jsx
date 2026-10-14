import { useEffect, useState } from 'react'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'

import Sidebar from './Sidebar'
import LanguageSwitcher from './LanguageSwitcher'
import { useAuth } from '../context/AuthContext'
import { workspaceRoleLabel } from '../utils/workspaceRoleLabel'

function AppLayout() {
  const { t } = useTranslation()
  const { currentUser, logout, getAvailableWorkspaces, switchWorkspace, createAdditionalFamily } = useAuth()
  const [workspaceChoices, setWorkspaceChoices] = useState([])
  const [switching, setSwitching] = useState(false)
  const [switchError, setSwitchError] = useState('')
  const [showCreateFamily, setShowCreateFamily] = useState(false)
  const [newFamilyName, setNewFamilyName] = useState('')
  const [newOwnerName, setNewOwnerName] = useState('')
  const canCreateFamily = currentUser?.permissions?.includes('MANAGE_MEMBERS') ||
    currentUser?.permissions?.includes('ADMIN_OVERRIDE')
  const navigate = useNavigate()
  const location = useLocation()
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)

  useEffect(() => {
    let cancelled = false
    getAvailableWorkspaces().then((choices) => {
      if (!cancelled) setWorkspaceChoices(choices)
    }).catch(() => {})
    return () => { cancelled = true }
  }, [currentUser?.workspaceId])

  useEffect(() => {
    setMobileMenuOpen(false)
  }, [location.pathname])

  useEffect(() => {
    document.body.classList.toggle('mobile-menu-open', mobileMenuOpen)
    return () => document.body.classList.remove('mobile-menu-open')
  }, [mobileMenuOpen])

  const handleLogout = () => {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className={`app-layout${mobileMenuOpen ? ' mobile-nav-open' : ''}`}>
      <div className="mobile-sidebar-shell" aria-hidden={!mobileMenuOpen}>
        <Sidebar />
      </div>

      {mobileMenuOpen && (
        <button
          type="button"
          className="mobile-sidebar-backdrop"
          aria-label={t('common.close')}
          onClick={() => setMobileMenuOpen(false)}
        />
      )}

      <div className="app-main">
        <header className="app-header">
          <button
            type="button"
            className="mobile-menu-button"
            aria-label="Menu"
            aria-expanded={mobileMenuOpen}
            onClick={() => setMobileMenuOpen((value) => !value)}
          >
            <span />
            <span />
            <span />
          </button>

          <div className="header-workspace">
            {workspaceChoices.length > 1 && (
              <select
                className="language-switcher workspace-switcher"
                aria-label="Активна сім'я / Active workspace"
                title="Активна сім'я"
                value={currentUser.workspaceId}
                disabled={switching}
                onChange={async (event) => {
                  setSwitching(true)
                  setSwitchError('')
                  try {
                    await switchWorkspace(Number(event.target.value))
                  } catch {
                    setSwitchError('Не вдалося змінити сім’ю')
                    setSwitching(false)
                  }
                }}
              >
                {workspaceChoices.map((choice) => (
                  <option key={choice.workspaceId} value={choice.workspaceId}>
                    {choice.workspaceName}
                  </option>
                ))}
              </select>
            )}
            {canCreateFamily && (
              <button className="header-create-family" type="button" disabled={switching}
                onClick={() => { setSwitchError(''); setShowCreateFamily(true) }}>
                + Створити сім’ю
              </button>
            )}
            {switchError && <span role="alert">{switchError}</span>}
          </div>
          <div className="header-user">
            <LanguageSwitcher />
            <div className="header-user-info">
              <strong>{currentUser.memberName}</strong>
              <span>{currentUser.workspaceRoleCode === "PARENT"
                ? t("workspaceRoles.PARENT_INDIVIDUAL", { defaultValue: "Батько / мати" })
                : workspaceRoleLabel(t, currentUser.workspaceRoleCode, currentUser.workspaceRoleName)}</span>
            </div>

            <button
              className="header-logout"
              type="button"
              onClick={handleLogout}
            >{t('common.logout')}</button>
          </div>
        </header>

        {showCreateFamily && (
          <div role="presentation" style={{
            position: 'fixed', inset: 0, zIndex: 1000,
            background: 'rgba(0,0,0,.45)', display: 'grid', placeItems: 'center',
            padding: 16,
          }}>
            <section role="dialog" aria-modal="true" aria-labelledby="new-family-title"
              style={{
                background: 'var(--surface, #fff)', color: 'var(--text-primary, #172033)',
                padding: 24, borderRadius: 14, width: 'min(100%, 430px)',
                maxHeight: '90vh', overflowY: 'auto',
              }}>
              <h2 id="new-family-title">Створити сім’ю</h2>
              <form onSubmit={async (event) => {
                event.preventDefault()
                setSwitching(true)
                setSwitchError('')
                try {
                  await createAdditionalFamily(newFamilyName, newOwnerName)
                } catch (error) {
                  setSwitchError(error.message || 'Не вдалося створити сім’ю')
                  setSwitching(false)
                }
              }}>
                <label style={{ display: 'block', marginBottom: 12 }}>
                  Назва сім’ї
                  <input autoFocus required maxLength={100} value={newFamilyName}
                    disabled={switching} onChange={(e) => setNewFamilyName(e.target.value)}
                    style={{ display: 'block', width: '100%' }} />
                </label>
                <label style={{ display: 'block', marginBottom: 12 }}>
                  Ім’я власника
                  <input required maxLength={100} value={newOwnerName}
                    disabled={switching} onChange={(e) => setNewOwnerName(e.target.value)}
                    style={{ display: 'block', width: '100%' }} />
                </label>
                {switchError && <p role="alert" className="error-message">{switchError}</p>}
                <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
                  <button type="submit" disabled={switching || !newFamilyName.trim() || !newOwnerName.trim()}>
                    {switching ? 'Створюємо…' : 'Створити'}
                  </button>
                  <button type="button" disabled={switching}
                    onClick={() => setShowCreateFamily(false)}>Скасувати</button>
                </div>
              </form>
            </section>
          </div>
        )}

        <main className="page-content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}

export default AppLayout

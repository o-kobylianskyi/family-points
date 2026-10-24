import { useEffect, useState } from 'react'
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'

import Sidebar from './Sidebar'
import LanguageSwitcher from './LanguageSwitcher'
import { useAuth } from '../context/AuthContext'
import { workspaceRoleLabel } from '../utils/workspaceRoleLabel'
import SoftFamilyIcon from './SoftFamilyIcon'
import MemberAvatar from './MemberAvatar'

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
            aria-label={t('workspaceUi.menu')}
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
                aria-label={t('workspaceUi.activeFamily')}
                title={t('workspaceUi.activeFamily')}
                value={currentUser.workspaceId}
                disabled={switching}
                onChange={async (event) => {
                  setSwitching(true)
                  setSwitchError('')
                  try {
                    await switchWorkspace(Number(event.target.value))
                  } catch {
                    setSwitchError(t('workspaceUi.switchError'))
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
                {t('workspaceUi.createFamily')}
              </button>
            )}
            {switchError && <span role="alert">{switchError}</span>}
          </div>
          <div className="header-user">
            <LanguageSwitcher />
            <details className="fp-profile-menu">
              <summary aria-label={currentUser.memberName}>
                <MemberAvatar memberId={currentUser.memberId} name={currentUser.memberName} className="fp-profile-avatar" />
                <span className="fp-profile-summary">
                  <strong>{currentUser.memberName}</strong>
                  <small>{currentUser.workspaceRoleCode === 'PARENT'
                    ? t('workspaceRoles.PARENT_INDIVIDUAL', { defaultValue: 'Батько / мати' })
                    : workspaceRoleLabel(t, currentUser.workspaceRoleCode, currentUser.workspaceRoleName)}</small>
                </span>
                <span className="fp-profile-chevron" aria-hidden="true">⌄</span>
              </summary>
              <div className="fp-profile-dropdown">
                <Link to="/settings"><SoftFamilyIcon name="settings" size={20} />{t('navigation.settings')}</Link>
                <button type="button" onClick={handleLogout}>{t('common.logout')}</button>
              </div>
            </details>
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
              <h2 id="new-family-title">{t("workspaceUi.createFamilyTitle")}</h2>
              <form onSubmit={async (event) => {
                event.preventDefault()
                setSwitching(true)
                setSwitchError('')
                try {
                  await createAdditionalFamily(newFamilyName, newOwnerName)
                } catch (error) {
                  setSwitchError(error.message || t('workspaceUi.creationError'))
                  setSwitching(false)
                }
              }}>
                <label style={{ display: 'block', marginBottom: 12 }}>
                  {t('workspaceUi.familyName')}
                  <input autoFocus required maxLength={100} value={newFamilyName}
                    disabled={switching} onChange={(e) => setNewFamilyName(e.target.value)}
                    style={{ display: 'block', width: '100%' }} />
                </label>
                <label style={{ display: 'block', marginBottom: 12 }}>
                  {t('workspaceUi.ownerName')}
                  <input required maxLength={100} value={newOwnerName}
                    disabled={switching} onChange={(e) => setNewOwnerName(e.target.value)}
                    style={{ display: 'block', width: '100%' }} />
                </label>
                {switchError && <p role="alert" className="error-message">{switchError}</p>}
                <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
                  <button type="submit" disabled={switching || !newFamilyName.trim() || !newOwnerName.trim()}>
                    {switching ? t('workspaceUi.creating') : t('workspaceUi.create')}
                  </button>
                  <button type="button" disabled={switching}
                    onClick={() => setShowCreateFamily(false)}>{t('workspaceUi.cancel')}</button>
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

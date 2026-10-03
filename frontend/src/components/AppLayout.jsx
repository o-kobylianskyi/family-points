import { useEffect, useState } from 'react'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'

import Sidebar from './Sidebar'
import LanguageSwitcher from './LanguageSwitcher'
import { useAuth } from '../context/AuthContext'

function AppLayout() {
  const { t } = useTranslation()
  const { currentUser, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)

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

          <div className="header-user">
            <LanguageSwitcher />

            <div className="header-user-info">
              <strong>{currentUser.memberName}</strong>
              <span>{currentUser.workspaceRoleName}</span>
            </div>

            <button
              className="header-logout"
              type="button"
              onClick={handleLogout}
            >{t('common.logout')}</button>
          </div>
        </header>

        <main className="page-content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}

export default AppLayout

import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../context/AuthContext'
import { getWorkspaceMembers } from '../api/workspaceApi'
import { changeOwnPassword, createMemberAccount, getWorkspaceAccounts, resetMemberPassword, updateMemberAccount } from '../api/accountApi'
import RoleCatalogSettings from '../components/RoleCatalogSettings'
import CurrencyFormsSettings from '../components/CurrencyFormsSettings'

function SettingsPage() {
  const { t } = useTranslation()
  const { currentUser, getAccessToken } = useAuth()
  const [members, setMembers] = useState([])
  const [accounts, setAccounts] = useState([])
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [editing, setEditing] = useState(null)
  const [editingError, setEditingError] = useState('')
  const [own, setOwn] = useState({ currentPassword: '', newPassword: '', confirm: '' })

  const canManage = currentUser?.permissions?.includes('MANAGE_MEMBERS')
  const canManageRoles = currentUser?.permissions?.includes('MANAGE_ROLES')
    || currentUser?.permissions?.includes('ADMIN_OVERRIDE')
  const accountByMember = useMemo(() => new Map(accounts.map(a => [a.workspaceMemberId, a])), [accounts])

  async function load() {
    if (!currentUser?.workspaceId) return
    setError('')
    try {
      const token = getAccessToken()
      const memberData = await getWorkspaceMembers(token, currentUser.workspaceId)
      setMembers(memberData)
      if (canManage) setAccounts(await getWorkspaceAccounts(token, currentUser.workspaceId))
    } catch (e) { setError(e.message) }
  }
  useEffect(() => { load() }, [currentUser?.workspaceId, canManage])

  function startCreate(member) {
    setEditingError('')
    setEditing({
      mode: 'create',
      member,
      username: '',
      password: '',
      confirm: '',
      enabled: true
    })
  }

  function startEdit(member, account) {
    setEditingError('')
    setEditing({
      mode: 'edit',
      member,
      account,
      username: account.username,
      password: '',
      confirm: '',
      enabled: account.enabled
    })
  }

  async function saveAccount(e) {
    e.preventDefault()
    setEditingError('')
    setNotice('')

    if (!editing.username.trim()) {
      return setEditingError(t('settings.accounts.usernameRequired'))
    }

    if (editing.mode === 'create' && editing.password.length < 8) {
      return setEditingError(t('settings.accounts.passwordMin'))
    }

    if (editing.mode === 'create' && editing.password !== editing.confirm) {
      return setEditingError(t('settings.accounts.passwordMismatch'))
    }

    if (editing.mode === 'edit' && editing.password) {
      if (editing.password.length < 8) {
        return setEditingError(t('settings.accounts.passwordMin'))
      }

      if (editing.password !== editing.confirm) {
        return setEditingError(t('settings.accounts.passwordMismatch'))
      }
    }

    setBusy(true)

    try {
      const token = getAccessToken()

      if (editing.mode === 'create') {
        await createMemberAccount(
          token,
          currentUser.workspaceId,
          editing.member.id,
          {
            username: editing.username.trim(),
            password: editing.password
          }
        )
      } else {
        await updateMemberAccount(
          token,
          currentUser.workspaceId,
          editing.member.id,
          {
            username: editing.username.trim(),
            enabled: editing.enabled
          }
        )

        if (editing.password) {
          await resetMemberPassword(
            token,
            currentUser.workspaceId,
            editing.member.id,
            editing.password
          )
        }
      }

      setEditing(null)
      setEditingError('')
      setNotice(t('settings.accounts.saved'))
      await load()

    } catch (e2) {
      setEditingError(e2.message)
    } finally {
      setBusy(false)
    }
  }

  async function saveOwnPassword(e) {
    e.preventDefault(); setError(''); setNotice('')
    if (own.newPassword.length < 8) return setError(t('settings.accounts.passwordMin'))
    if (own.newPassword !== own.confirm) return setError(t('settings.accounts.passwordMismatch'))
    setBusy(true)
    try {
      await changeOwnPassword(getAccessToken(), own.currentPassword, own.newPassword)
      setOwn({ currentPassword: '', newPassword: '', confirm: '' }); setNotice(t('settings.password.changed'))
    } catch (e2) { setError(e2.message) } finally { setBusy(false) }
  }

  return <>
    <h1>{t('settings.title')}</h1>
    <p>{t('settings.description')}</p>
    {error && <div className="error-message settings-message">{error}</div>}
    {notice && <div className="settings-success settings-message">{notice}</div>}

    <section className="settings-section">
      <h2>{t('settings.password.title')}</h2>
      <p>{t('settings.password.description')}</p>
      <form className="settings-form" onSubmit={saveOwnPassword}>
        <label>{t('settings.password.current')}<input type="password" value={own.currentPassword} onChange={e => setOwn({ ...own, currentPassword: e.target.value })} required /></label>
        <label>{t('settings.password.new')}<input type="password" value={own.newPassword} onChange={e => setOwn({ ...own, newPassword: e.target.value })} required /></label>
        <label>{t('settings.password.confirm')}<input type="password" value={own.confirm} onChange={e => setOwn({ ...own, confirm: e.target.value })} required /></label>
        <button className="primary-button" disabled={busy}>{t('settings.password.change')}</button>
      </form>
    </section>

    {canManage && <section className="settings-section">
      <h2>{t('settings.accounts.title')}</h2><p>{t('settings.accounts.description')}</p>
      <div className="account-grid">
        {members.map(member => { const account = accountByMember.get(member.id); return <article className="account-card" key={member.id}>
          <div>
            <h3>{member.name}</h3>
            <div className="account-meta">
              {member.workspaceRoleName} · {t(`memberType.${member.memberType}`, { defaultValue: member.memberType })}
            </div>
          </div>
          {account ? <><div className="account-login">@{account.username}</div><span className={`account-status ${account.enabled ? 'enabled' : 'disabled'}`}>{account.enabled ? t('settings.accounts.enabled') : t('settings.accounts.disabled')}</span><button onClick={() => startEdit(member, account)}>{t('settings.accounts.manage')}</button></>
          : <><div className="account-missing">{t('settings.accounts.missing')}</div><button className="primary-button" onClick={() => startCreate(member)}>{t('settings.accounts.create')}</button></>}
        </article> })}
      </div>
    </section>}

    <CurrencyFormsSettings token={getAccessToken()} workspaceId={currentUser.workspaceId}
      canManage={currentUser.permissions?.includes('MANAGE_ECONOMY') || currentUser.permissions?.includes('ADMIN_OVERRIDE')} />

    <RoleCatalogSettings token={getAccessToken()} workspaceId={currentUser.workspaceId} canManage={canManageRoles} />

    {editing && <div className="modal-backdrop"><div className="modal-card account-modal" role="dialog" aria-modal="true">
      <div className="modal-header"><div><h2>{editing.mode === 'create' ? t('settings.accounts.createTitle') : t('settings.accounts.editTitle')}</h2><p>{editing.member.name}</p></div><button className="modal-close" type="button" onClick={() => setEditing(null)}>×</button></div>
      <form onSubmit={saveAccount}>
          {editingError && (
            <div className="error-message">
              {editingError}
            </div>
          )}
        <label>{t('settings.accounts.username')}<input value={editing.username} onChange={e => setEditing({ ...editing, username: e.target.value })} autoComplete="off" /></label>
        <label>{editing.mode === 'create' ? t('settings.accounts.initialPassword') : t('settings.accounts.newPasswordOptional')}<input type="password" value={editing.password} onChange={e => setEditing({ ...editing, password: e.target.value })} /></label>
        <label>{t('settings.password.confirm')}<input type="password" value={editing.confirm} onChange={e => setEditing({ ...editing, confirm: e.target.value })} /></label>
        {editing.mode === 'edit' && <label className="form-checkbox"><input type="checkbox" checked={editing.enabled} disabled={editing.account.workspaceMemberId === currentUser.memberId} onChange={e => setEditing({ ...editing, enabled: e.target.checked })} /><span>{t('settings.accounts.allowLogin')}</span></label>}
        <div className="modal-actions"><button type="button" onClick={() => setEditing(null)}>{t('common.cancel')}</button><button className="primary-button" disabled={busy}>{busy ? t('common.saving') : t('settings.accounts.save')}</button></div>
      </form>
    </div></div>}
  </>
}
export default SettingsPage

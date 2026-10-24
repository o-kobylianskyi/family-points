import UsernameAvailability from './UsernameAvailability'
import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { workspaceRoleLabel } from '../utils/workspaceRoleLabel'
import { useAuth } from '../context/AuthContext'
import GooglePasswordSetup from './GooglePasswordSetup'
import EntityMedia from './EntityMedia'
import FamilyAvatar from './FamilyAvatar'

function hasSameMemberId(a, b) { return a != null && b != null && String(a) === String(b) }

function MemberEditor({ member, roles, busy, externalError, onSave, onDelete, onClose }) {
  const { t } = useTranslation()
  const { currentUser } = useAuth()
  const isOwnAccount = Boolean(member && hasSameMemberId(member.id, currentUser?.memberId))
  const [showPasswordSetup, setShowPasswordSetup] = useState(false)
  const editing = Boolean(member)
  const hasAccount = member?.accessStatus === 'ACTIVE' || member?.accessStatus === 'DISABLED'
  const invitePending = member?.accessStatus === 'PENDING_INVITATION'
  const [name, setName] = useState(member?.name ?? '')
  const [memberType, setMemberType] = useState(member?.memberType ?? 'CHILD')
  const [workspaceRoleId, setWorkspaceRoleId] = useState(member?.workspaceRoleId ?? roles[0]?.id ?? '')
  const [error, setError] = useState('')
  const [accessMode, setAccessMode] = useState('profile')
  const [childUsername, setChildUsername] = useState('')
  const [childPassword, setChildPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [inviteEmail, setInviteEmail] = useState(member?.invitationEmail ?? '')

  useEffect(() => {
    const onKey = (e) => { if (e.key === 'Escape' && !busy) onClose() }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [busy, onClose])

  const submit = async (e) => {
    e.preventDefault()
    if (!name.trim() || !workspaceRoleId) {
      setError(t('memberFormUi.required'))
      return
    }
    if (accessMode === 'account') {
      if (!/^[a-zA-Z0-9._-]{3,64}$/.test(childUsername.trim())) {
        setError(t('memberEditorUi.usernameInvalid'))
        return
      }
      if (childPassword.length < 8 || childPassword !== confirmPassword) {
        setError(t('memberEditorUi.passwordInvalid'))
        return
      }
    }
    if (accessMode === 'invite' && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(inviteEmail.trim())) {
      setError(t('memberEditorUi.emailInvalid'))
      return
    }
    setError('')
    await onSave({ name: name.trim(), memberType, workspaceRoleId: Number(workspaceRoleId) },
      accessMode === 'account'
        ? { username: childUsername.trim(), password: childPassword }
        : null,
      accessMode === 'invite' ? inviteEmail.trim() : null)
  }

  const remove = async () => {
    const ok = window.confirm(t('memberFormUi.deleteConfirm'))
    if (ok) await onDelete()
  }

  return <div className="modal-backdrop" role="presentation">
    <div className="modal-card member-editor" role="dialog" aria-modal="true">
      <div className="modal-header">
        <div>
          <h2>{editing ? t('memberFormUi.editTitle') : t('memberFormUi.createTitle')}</h2>
          {editing && <p>{member.name}</p>}
        </div>
        <button type="button" className="modal-close" onClick={onClose} disabled={busy}>×</button>
      </div>
      {editing && <EntityMedia inlineEditor type="MEMBER" targetId={member.id} slot="AVATAR" canEdit>
        <FamilyAvatar name={member.name} kind="member" />
      </EntityMedia>}
      <form onSubmit={submit}>
        {(error || externalError) && <div className="error-message" role="alert">{error || externalError}</div>}
        <label>{t('memberFormUi.name')}<input value={name} onChange={e => setName(e.target.value)} autoFocus /></label>
        <label>{t('memberFormUi.type')}
          <select value={memberType} onChange={e => setMemberType(e.target.value)}>
            <option value="PARENT">{t('memberType.PARENT')}</option>
            <option value="CHILD">{t('memberType.CHILD')}</option>
            <option value="OTHER">{t('memberType.OTHER')}</option>
          </select>
        </label>
        <label>{t('memberFormUi.role')}
          <select value={workspaceRoleId} onChange={e => setWorkspaceRoleId(e.target.value)}>
            {roles.map(role => <option key={role.id} value={role.id}>{workspaceRoleLabel(t, role.code, role.name)}</option>)}
          </select>
        </label>
        {<>
          <fieldset className="member-access-options"><legend>{t('memberEditorUi.access')}</legend>
            {hasAccount && <p className="member-access-hint">
              {t(member.accessStatus === 'DISABLED' ? 'memberEditorUi.disabledAccount' : 'memberEditorUi.hasAccount')}
            </p>}
            {hasAccount && isOwnAccount && <>
              <button type="button" className="secondary-button" onClick={() => setShowPasswordSetup(value => !value)}>
                {t('googlePassword.title')}
              </button>
              {showPasswordSetup && <GooglePasswordSetup />}
            </>}
            {invitePending && <p className="member-access-hint">
              {t('memberEditorUi.pendingNotice', { email: member.invitationEmail })}
            </p>}
            <label className="member-access-option">
              <input type="radio" name="member-access" value="profile" checked={accessMode === 'profile'} onChange={() => setAccessMode('profile')} />
              <span>{t(editing ? 'memberEditorUi.unchanged' : 'memberEditorUi.profile')}</span>
            </label>
            {!hasAccount && <label className="member-access-option">
              <input type="radio" name="member-access" value="account" checked={accessMode === 'account'} onChange={() => setAccessMode('account')} />
              <span>{t('memberEditorUi.createLogin')}</span>
            </label>}
            {!hasAccount && <label className="member-access-option">
              <input type="radio" name="member-access" value="invite" checked={accessMode === 'invite'} onChange={() => setAccessMode('invite')} />
              <span>{t(invitePending ? 'memberEditorUi.resend' : 'memberEditorUi.invite')}</span>
            </label>}
          </fieldset>
          {accessMode === 'invite' && <label style={{ marginTop: 12 }}>{t('memberEditorUi.inviteEmail')}
            <input type="email" autoComplete="email" required value={inviteEmail}
              onChange={e => setInviteEmail(e.target.value)} />
          </label>}
          {accessMode === 'account' && <div style={{ display: 'grid', gap: 10, marginTop: 12 }}>
            <p>{t('memberEditorUi.loginInfo')}</p>
            <label>{t('memberEditorUi.childUsername')}<input autoComplete="off" value={childUsername} onChange={e => setChildUsername(e.target.value)} required /><UsernameAvailability username={childUsername} /></label>
            <label>{t('memberEditorUi.password')}<input type="password" autoComplete="new-password" value={childPassword} onChange={e => setChildPassword(e.target.value)} minLength={8} required /></label>
            <label>{t('memberEditorUi.confirmPassword')}<input type="password" autoComplete="new-password" value={confirmPassword} onChange={e => setConfirmPassword(e.target.value)} minLength={8} required /></label>
          </div>}

        </>}
        <div className="modal-actions member-editor-actions">
          {editing && <button type="button" className="danger-button" onClick={remove} disabled={busy}>{t('memberFormUi.delete')}</button>}
          <span className="member-editor-spacer" />
          <button type="button" className="secondary-button" onClick={onClose} disabled={busy}>{t('common.cancel')}</button>
          <button type="submit" className="primary-button" disabled={busy}>{busy ? t('memberFormUi.saving') : t('memberFormUi.save')}</button>
        </div>
      </form>
    </div>
  </div>
}

export default MemberEditor

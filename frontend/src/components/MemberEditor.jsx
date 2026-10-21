import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { workspaceRoleLabel } from '../utils/workspaceRoleLabel'

function MemberEditor({ member, roles, busy, externalError, onSave, onDelete, onClose }) {
  const { t } = useTranslation()
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
      setError(t('workspace.memberEditor.required', { defaultValue: 'Заповніть ім’я та роль' }))
      return
    }
    if (accessMode === 'account') {
      if (!/^[a-zA-Z0-9._-]{3,64}$/.test(childUsername.trim())) {
        setError('Логін: 3–64 символи, латинські літери, цифри, крапка, _ або -')
        return
      }
      if (childPassword.length < 8 || childPassword !== confirmPassword) {
        setError('Пароль має містити щонайменше 8 символів і збігатися з підтвердженням')
        return
      }
    }
    if (accessMode === 'invite' && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(inviteEmail.trim())) {
      setError('Вкажіть правильну адресу email для запрошення')
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
    const ok = window.confirm(t('workspace.memberEditor.deleteConfirm', {
      defaultValue: 'Видалити учасника? Історія завдань, балів і покупок залишиться, але учасник у ній відображатиметься як «Видалено».'
    }))
    if (ok) await onDelete()
  }

  return <div className="modal-backdrop" role="presentation">
    <div className="modal-card member-editor" role="dialog" aria-modal="true">
      <div className="modal-header">
        <div>
          <h2>{editing ? t('workspace.memberEditor.editTitle', { defaultValue: 'Редагувати учасника' }) : t('workspace.memberEditor.createTitle', { defaultValue: 'Новий учасник' })}</h2>
          {editing && <p>{member.name}</p>}
        </div>
        <button type="button" className="modal-close" onClick={onClose} disabled={busy}>×</button>
      </div>
      <form onSubmit={submit}>
        {(error || externalError) && <div className="error-message" role="alert">{error || externalError}</div>}
        <label>{t('workspace.memberEditor.name', { defaultValue: 'Ім’я' })}<input value={name} onChange={e => setName(e.target.value)} autoFocus /></label>
        <label>{t('workspace.memberEditor.type', { defaultValue: 'Тип' })}
          <select value={memberType} onChange={e => setMemberType(e.target.value)}>
            <option value="PARENT">{t('memberType.PARENT')}</option>
            <option value="CHILD">{t('memberType.CHILD')}</option>
            <option value="OTHER">{t('memberType.OTHER')}</option>
          </select>
        </label>
        <label>{t('workspace.memberEditor.role', { defaultValue: 'Роль у просторі' })}
          <select value={workspaceRoleId} onChange={e => setWorkspaceRoleId(e.target.value)}>
            {roles.map(role => <option key={role.id} value={role.id}>{workspaceRoleLabel(t, role.code, role.name)}</option>)}
          </select>
        </label>
        {<>
          <fieldset className="member-access-options"><legend>Доступ учасника</legend>
            {hasAccount && <p className="member-access-hint">
              {member.accessStatus === 'DISABLED' ? 'Акаунт вимкнено.' : 'Акаунт уже створено.'}
              {' '}Повторно створити логін або надіслати запрошення для цього профілю неможливо.
            </p>}
            {invitePending && <p className="member-access-hint">
              Запрошення надіслано на {member.invitationEmail}. Можна надіслати нове
              (не частіше ніж раз на 2 хвилини); попереднє посилання перестане діяти.
            </p>}
            <label className="member-access-option">
              <input type="radio" name="member-access" value="profile" checked={accessMode === 'profile'} onChange={() => setAccessMode('profile')} />
              <span>{editing ? 'Без змін доступу' : 'Лише профіль, без входу'}</span>
            </label>
            {!hasAccount && <label className="member-access-option">
              <input type="radio" name="member-access" value="account" checked={accessMode === 'account'} onChange={() => setAccessMode('account')} />
              <span>Створити окремий логін для дитини</span>
            </label>}
            {!hasAccount && <label className="member-access-option">
              <input type="radio" name="member-access" value="invite" checked={accessMode === 'invite'} onChange={() => setAccessMode('invite')} />
              <span>{invitePending ? 'Надіслати запрошення повторно' : 'Надіслати запрошення на email'}</span>
            </label>}
          </fieldset>
          {accessMode === 'invite' && <label style={{ marginTop: 12 }}>Email для запрошення
            <input type="email" autoComplete="email" required value={inviteEmail}
              onChange={e => setInviteEmail(e.target.value)} />
          </label>}
          {accessMode === 'account' && <div style={{ display: 'grid', gap: 10, marginTop: 12 }}>
            <p>Логін і пароль задають батьки. Підтвердження через email батьків буде додано окремо; зараз цей спосіб не використовує пошту.</p>
            <label>Логін дитини<input autoComplete="off" value={childUsername} onChange={e => setChildUsername(e.target.value)} required /></label>
            <label>Пароль<input type="password" autoComplete="new-password" value={childPassword} onChange={e => setChildPassword(e.target.value)} minLength={8} required /></label>
            <label>Повторіть пароль<input type="password" autoComplete="new-password" value={confirmPassword} onChange={e => setConfirmPassword(e.target.value)} minLength={8} required /></label>
          </div>}

        </>}
        <div className="modal-actions member-editor-actions">
          {editing && <button type="button" className="danger-button" onClick={remove} disabled={busy}>{t('common.delete', { defaultValue: 'Видалити' })}</button>}
          <span className="member-editor-spacer" />
          <button type="button" className="secondary-button" onClick={onClose} disabled={busy}>{t('common.cancel')}</button>
          <button type="submit" className="primary-button" disabled={busy}>{busy ? t('common.saving') : t('common.save', { defaultValue: 'Зберегти' })}</button>
        </div>
      </form>
    </div>
  </div>
}

export default MemberEditor

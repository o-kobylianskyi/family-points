import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'

function MemberEditor({ member, roles, busy, onSave, onDelete, onClose }) {
  const { t } = useTranslation()
  const editing = Boolean(member)
  const [name, setName] = useState(member?.name ?? '')
  const [memberType, setMemberType] = useState(member?.memberType ?? 'CHILD')
  const [workspaceRoleId, setWorkspaceRoleId] = useState(member?.workspaceRoleId ?? roles[0]?.id ?? '')
  const [error, setError] = useState('')

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
    setError('')
    await onSave({ name: name.trim(), memberType, workspaceRoleId: Number(workspaceRoleId) })
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
        {error && <div className="error-message">{error}</div>}
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
            {roles.map(role => <option key={role.id} value={role.id}>{role.name}</option>)}
          </select>
        </label>
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

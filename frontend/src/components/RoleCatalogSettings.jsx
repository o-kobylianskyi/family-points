import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  getRoleSets, createRoleSet, updateRoleSet, deleteRoleSet,
  getRoleDefinitions, createRoleDefinition, updateRoleDefinition, deleteRoleDefinition
} from '../api/roleCatalogApi'

export default function RoleCatalogSettings({ token, workspaceId, canManage }) {
  const { t } = useTranslation()
  const [sets, setSets] = useState([])
  const [roles, setRoles] = useState([])
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [editingSet, setEditingSet] = useState(null)
  const [editingRole, setEditingRole] = useState(null)

  const load = async () => {
    setError('')
    try {
      const [setData, roleData] = await Promise.all([
        getRoleSets(token, workspaceId),
        getRoleDefinitions(token, workspaceId),
      ])
      setSets(setData)
      setRoles(roleData)
    } catch (e) {
      setError(e.message)
    }
  }

  useEffect(() => {
    if (workspaceId) load()
  }, [workspaceId])

  const rolesBySet = useMemo(() => {
    const map = new Map()
    for (const role of roles) {
      const key = role.roleSetId ?? 'none'
      if (!map.has(key)) map.set(key, [])
      map.get(key).push(role)
    }
    return map
  }, [roles])

  const setLabel = set => set.systemCode
    ? t(`roles.roleSets.${set.systemCode}`, { defaultValue: set.name })
    : set.name

  const roleLabel = role => role.systemCode
    ? t(`roles.system.${role.systemCode}`, { defaultValue: role.name })
    : role.name

  const run = async action => {
    setBusy(true)
    setError('')
    try {
      await action()
      await load()
    } catch (e) {
      setError(e.message)
    } finally {
      setBusy(false)
    }
  }

  const newSet = () => setEditingSet({ id: null, name: '', description: '', systemDefault: false })
  const editSet = set => setEditingSet({ ...set })

  const saveSet = async () => {
    if (!editingSet?.name?.trim()) return
    await run(async () => {
      const body = { name: editingSet.name.trim(), description: editingSet.description?.trim() || null }
      if (editingSet.id) await updateRoleSet(token, workspaceId, editingSet.id, body)
      else await createRoleSet(token, workspaceId, body)
      setEditingSet(null)
    })
  }

  const newRole = roleSetId => setEditingRole({
    id: null,
    roleSetId: roleSetId ?? null,
    name: '',
    description: '',
    visibility: 'SHARED',
    ownerContextType: 'WORKSPACE',
    ownerContextId: null,
    systemDefault: false,
  })

  const editRole = role => setEditingRole({ ...role })

  const saveRole = async () => {
    if (!editingRole?.name?.trim()) return
    await run(async () => {
      const body = {
        name: editingRole.name.trim(),
        description: editingRole.description?.trim() || null,
        roleSetId: editingRole.roleSetId ? Number(editingRole.roleSetId) : null,
        visibility: editingRole.visibility || 'SHARED',
        ownerContextType: editingRole.ownerContextType || 'WORKSPACE',
        ownerContextId: editingRole.ownerContextType === 'WORKSPACE' ? null : editingRole.ownerContextId,
      }
      if (editingRole.id) await updateRoleDefinition(token, workspaceId, editingRole.id, body)
      else await createRoleDefinition(token, workspaceId, body)
      setEditingRole(null)
    })
  }

  return <section className="settings-section role-catalog-settings">
    <div className="role-catalog-heading">
      <div>
        <h2>{t('roles.title')}</h2>
        <p>{t('roles.description')}</p>
      </div>
      {canManage && <button type="button" className="primary-button" onClick={newSet}>{t('roles.addSet')}</button>}
    </div>

    {error && <div className="error-message settings-message">{error}</div>}

    <div className="role-catalog-grid">
      {sets.map(set => {
        const setRoles = rolesBySet.get(set.id) || []
        return <article className="role-set-card" key={set.id}>
          <div className="role-set-card-header">
            <div>
              <div className="role-set-title-row">
                <h3>{setLabel(set)}</h3>
                {set.systemDefault && <span className="role-system-badge">{t('roles.systemBadge')}</span>}
              </div>
              {set.description && <p>{set.description}</p>}
            </div>
            {canManage && !set.systemDefault && <button type="button" className="secondary-button" onClick={() => editSet(set)}>{t('common.edit')}</button>}
          </div>

          <div className="role-definition-list">
            {setRoles.map(role => <div className="role-definition-row" key={role.id}>
              <div>
                <strong>{roleLabel(role)}</strong>
                {role.description && <small>{role.description}</small>}
              </div>
              <div className="role-definition-meta">
                <span>{role.visibility === 'PRIVATE' ? t('roles.private') : t('roles.shared')}</span>
                {role.systemDefault && <span>{t('roles.systemBadge')}</span>}
                {canManage && !role.systemDefault && <button type="button" onClick={() => editRole(role)}>{t('common.edit')}</button>}
              </div>
            </div>)}
            {!setRoles.length && <div className="role-muted">{t('roles.emptySet')}</div>}
          </div>

          {canManage && <button type="button" className="role-inline-add" onClick={() => newRole(set.id)}>+ {t('roles.addRole')}</button>}
        </article>
      })}

      {(rolesBySet.get('none') || []).length > 0 && <article className="role-set-card">
        <div className="role-set-card-header"><div><h3>{t('roles.withoutSet')}</h3></div></div>
        <div className="role-definition-list">
          {(rolesBySet.get('none') || []).map(role => <div className="role-definition-row" key={role.id}>
            <div><strong>{roleLabel(role)}</strong>{role.description && <small>{role.description}</small>}</div>
            <div className="role-definition-meta">
              <span>{role.visibility === 'PRIVATE' ? t('roles.private') : t('roles.shared')}</span>
              {canManage && !role.systemDefault && <button type="button" onClick={() => editRole(role)}>{t('common.edit')}</button>}
            </div>
          </div>)}
        </div>
        {canManage && <button type="button" className="role-inline-add" onClick={() => newRole(null)}>+ {t('roles.addRole')}</button>}
      </article>}
    </div>

    {editingSet && <div className="modal-backdrop"><div className="modal-card role-catalog-modal">
      <div className="modal-header"><div><h2>{editingSet.id ? t('roles.editSet') : t('roles.newSet')}</h2></div><button type="button" className="modal-close" onClick={() => setEditingSet(null)}>×</button></div>
      <label>{t('roles.name')}<input value={editingSet.name} onChange={e => setEditingSet({ ...editingSet, name: e.target.value })}/></label>
      <label>{t('roles.descriptionField')}<textarea value={editingSet.description || ''} onChange={e => setEditingSet({ ...editingSet, description: e.target.value })}/></label>
      <div className="modal-actions">
        {editingSet.id && !editingSet.systemDefault && <button type="button" className="danger-button" onClick={() => window.confirm(t('roles.deleteSetConfirm')) && run(async () => { await deleteRoleSet(token, workspaceId, editingSet.id); setEditingSet(null) })}>{t('roles.delete')}</button>}
        <button type="button" className="secondary-button" onClick={() => setEditingSet(null)}>{t('common.cancel')}</button>
        <button type="button" className="primary-button" disabled={busy || !editingSet.name.trim()} onClick={saveSet}>{busy ? t('common.saving') : t('roles.save')}</button>
      </div>
    </div></div>}

    {editingRole && <div className="modal-backdrop"><div className="modal-card role-catalog-modal">
      <div className="modal-header"><div><h2>{editingRole.id ? t('roles.editRole') : t('roles.newRole')}</h2></div><button type="button" className="modal-close" onClick={() => setEditingRole(null)}>×</button></div>
      <label>{t('roles.name')}<input value={editingRole.name} onChange={e => setEditingRole({ ...editingRole, name: e.target.value })}/></label>
      <label>{t('roles.roleSet')}<select value={editingRole.roleSetId || ''} onChange={e => setEditingRole({ ...editingRole, roleSetId: e.target.value })}><option value="">{t('roles.withoutSet')}</option>{sets.map(set => <option key={set.id} value={set.id}>{setLabel(set)}</option>)}</select></label>
      <label>{t('roles.descriptionField')}<textarea value={editingRole.description || ''} onChange={e => setEditingRole({ ...editingRole, description: e.target.value })}/></label>
      <label>{t('roles.visibility')}<select value={editingRole.visibility || 'SHARED'} onChange={e => setEditingRole({ ...editingRole, visibility: e.target.value })}><option value="SHARED">{t('roles.shared')}</option><option value="PRIVATE">{t('roles.private')}</option></select></label>
      <div className="modal-actions">
        {editingRole.id && !editingRole.systemDefault && <button type="button" className="danger-button" onClick={() => window.confirm(t('roles.deleteRoleConfirm')) && run(async () => { await deleteRoleDefinition(token, workspaceId, editingRole.id); setEditingRole(null) })}>{t('roles.delete')}</button>}
        <button type="button" className="secondary-button" onClick={() => setEditingRole(null)}>{t('common.cancel')}</button>
        <button type="button" className="primary-button" disabled={busy || !editingRole.name.trim()} onClick={saveRole}>{busy ? t('common.saving') : t('roles.save')}</button>
      </div>
    </div></div>}
  </section>
}

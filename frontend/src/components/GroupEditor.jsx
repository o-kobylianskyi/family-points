import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  updateMemberGroup, addGroupMember, removeGroupMember, addChildGroup, removeChildGroup,
  addGroupRole, updateGroupRole, deleteGroupRole, updateGroupMemberRoles,
  getGroupPermissions, addGroupPermission, removeGroupPermission,
} from '../api/memberGroupApi'

const permissionGroups = [
  { key: 'tasks', permissions: ['TASK_VIEW', 'TASK_CREATE', 'TASK_ASSIGN', 'TASK_MANAGE', 'TASK_APPROVE'] },
  { key: 'members', permissions: ['MEMBER_VIEW', 'MEMBER_MANAGE'] },
  { key: 'groups', permissions: ['GROUP_VIEW', 'GROUP_MANAGE', 'SUBGROUP_MANAGE'] },
  { key: 'points', permissions: ['POINT_VIEW', 'POINT_AWARD', 'POINT_SPEND'] },
]

const permissionProfiles = {
  NONE: {},
  EXECUTOR: {
    TASK_VIEW: 'GROUP',
  },
  SENIOR: {
    TASK_VIEW: 'GROUP',
    TASK_ASSIGN: 'GROUP',
    TASK_APPROVE: 'GROUP',
    MEMBER_VIEW: 'GROUP',
    GROUP_VIEW: 'GROUP',
  },
  LEADER: {
    TASK_VIEW: 'GROUP',
    TASK_CREATE: 'GROUP',
    TASK_ASSIGN: 'GROUP',
    TASK_MANAGE: 'GROUP',
    TASK_APPROVE: 'GROUP',
    MEMBER_VIEW: 'GROUP',
    MEMBER_MANAGE: 'GROUP',
    GROUP_VIEW: 'GROUP',
    GROUP_MANAGE: 'GROUP',
    SUBGROUP_MANAGE: 'GROUP',
    POINT_VIEW: 'GROUP',
    POINT_AWARD: 'GROUP',
  },
  CONTROL: {
    TASK_VIEW: 'GROUP',
    TASK_APPROVE: 'GROUP',
    MEMBER_VIEW: 'GROUP',
    GROUP_VIEW: 'GROUP',
    POINT_VIEW: 'GROUP',
  },
}

function samePermissions(a, b) {
  const ak = Object.keys(a || {}).sort()
  const bk = Object.keys(b || {}).sort()
  return ak.length === bk.length && ak.every((key, index) => key === bk[index] && a[key] === b[key])
}

function detectProfile(permissions) {
  for (const [profile, values] of Object.entries(permissionProfiles)) {
    if (samePermissions(permissions, values)) return profile
  }
  return 'CUSTOM'
}

function firstFreeName(baseName, roles, currentRoleId = null) {
  const base = (baseName || '').trim()
  if (!base) return ''
  const used = new Set(
    roles
      .filter(role => role.id !== currentRoleId)
      .map(role => (role.name || '').trim().toLocaleLowerCase())
  )
  if (!used.has(base.toLocaleLowerCase())) return base
  let n = 2
  while (used.has(`${base} ${n}`.toLocaleLowerCase())) n += 1
  return `${base} ${n}`
}

export default function GroupEditor({ group, groups, members, token, workspaceId, onChanged, onClose }) {
  const { t } = useTranslation()

  const [name, setName] = useState(group.name)
  const [description, setDescription] = useState(group.description || '')
  const [showInNavigation, setShowInNavigation] = useState(Boolean(group.showInNavigation))
  const [memberId, setMemberId] = useState('')
  const [childId, setChildId] = useState('')
  const [permissionGrants, setPermissionGrants] = useState([])
  const [roleEditor, setRoleEditor] = useState(null)
  const [memberRoleEditor, setMemberRoleEditor] = useState(null)
  const [showAdvancedPermissions, setShowAdvancedPermissions] = useState(false)
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  const reloadAux = async () => {
    const grants = await getGroupPermissions(token, workspaceId, group.id)
    setPermissionGrants(grants)
  }

  useEffect(() => {
    reloadAux().catch(e => setError(e.message))
  }, [token, workspaceId, group.id])

  useEffect(() => {
    const handler = e => {
      if (e.key !== 'Escape') return
      if (roleEditor) setRoleEditor(null)
      else if (memberRoleEditor) setMemberRoleEditor(null)
      else onClose()
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [onClose, roleEditor, memberRoleEditor])

  const run = async action => {
    try {
      setError('')
      await action()
      await onChanged()
      await reloadAux()
      return true
    } catch (e) {
      setError(e.message || t('roles.saveFailed'))
      return false
    }
  }

  const availableMembers = useMemo(
    () => members.filter(m => !group.members.some(x => x.memberId === m.id)),
    [members, group]
  )

  const availableGroups = useMemo(
    () => groups.filter(g => g.id !== group.id && !group.childGroups.some(x => x.groupId === g.id)),
    [groups, group]
  )

  const grantsFor = id => permissionGrants.filter(g => g.roleId === id)
  const roleDisplayName = role => role?.systemCode
    ? t(`roles.system.${role.systemCode}`, { defaultValue: role.name })
    : role?.name
  const roleName = id => roleDisplayName(group.roles.find(r => r.id === id)) || `#${id}`

  const permissionsForRole = roleId => {
    const permissions = {}
    grantsFor(roleId).forEach(grant => { permissions[grant.permission] = grant.scope })
    return permissions
  }

  const duplicatePermissionRole = roleEditor
    ? group.roles.find(role =>
        role.id !== roleEditor.id &&
        samePermissions(permissionsForRole(role.id), roleEditor.permissions)
      )
    : null

  const roleProfileBaseName = profile => {
    if (profile === 'NONE' || profile === 'CUSTOM') return t('roles.defaultLocalRoleName')
    return t(`roles.permissionProfiles.${profile}`)
  }

  const suggestedRoleName = (profile, modified = false, currentRoleId = null) => {
    const baseProfile = profile === 'CUSTOM' ? 'NONE' : profile
    const baseName = roleProfileBaseName(baseProfile)
    const rawName = modified ? `${baseName} ${t('roles.modifiedSuffix')}` : baseName
    return firstFreeName(rawName, group.roles, currentRoleId)
  }

  async function saveMain() {
    if (!name.trim()) return setError(t('roles.groupNameRequired'))
    try {
      setSaving(true)
      setError('')
      await updateMemberGroup(token, workspaceId, group.id, {
        name: name.trim(),
        description: description.trim() || null,
        showInNavigation,
      })
      await onChanged()
      onClose()
    } catch (e) {
      setError(e.message)
    } finally {
      setSaving(false)
    }
  }

  const openNewRole = () => {
    setError('')
    setShowAdvancedPermissions(false)
    setRoleEditor({
      id: null,
      name: suggestedRoleName('NONE'),
      nameTouched: false,
      description: '',
      roleSetId: null,
      permissions: {},
      profile: 'NONE',
      baseProfile: 'NONE',
    })
  }

  const openRole = role => {
    const permissions = {}
    grantsFor(role.id).forEach(g => { permissions[g.permission] = g.scope })
    setError('')
    setShowAdvancedPermissions(false)
    const detectedProfile = detectProfile(permissions)
    setRoleEditor({
      id: role.id,
      name: role.name || '',
      nameTouched: true,
      description: role.description || '',
      roleSetId: role.roleSetId || null,
      permissions,
      profile: detectedProfile,
      baseProfile: detectedProfile === 'CUSTOM' ? 'NONE' : detectedProfile,
    })
  }

  const applyProfile = profile => {
    if (profile === 'CUSTOM') {
      setRoleEditor({ ...roleEditor, profile: 'CUSTOM' })
      return
    }
    const next = {
      ...roleEditor,
      profile,
      baseProfile: profile,
      permissions: { ...permissionProfiles[profile] },
    }
    if (!roleEditor.nameTouched) {
      next.name = suggestedRoleName(profile, false, roleEditor.id)
    }
    setRoleEditor(next)
  }

  const updatePermission = (permission, checked, scope = 'GROUP') => {
    const nextPermissions = { ...roleEditor.permissions }
    if (checked) nextPermissions[permission] = scope
    else delete nextPermissions[permission]

    const detected = detectProfile(nextPermissions)
    const next = {
      ...roleEditor,
      permissions: nextPermissions,
      profile: detected,
    }

    if (detected !== 'CUSTOM') next.baseProfile = detected

    if (!roleEditor.nameTouched) {
      const baseProfile = detected === 'CUSTOM'
        ? (roleEditor.baseProfile || 'NONE')
        : detected
      next.name = suggestedRoleName(baseProfile, detected === 'CUSTOM', roleEditor.id)
    }

    setRoleEditor(next)
  }

  const saveRole = async event => {
    event?.preventDefault?.()
    const sourceRole = roleEditor?.id ? group.roles.find(r => r.id === roleEditor.id) : null
    const effectiveName = (roleEditor?.name || sourceRole?.name || '').trim()
    if (!effectiveName) {
      setError(t('roles.roleNameRequired'))
      return
    }

    const duplicateName = group.roles.some(role =>
      role.id !== roleEditor.id &&
      (role.name || '').trim().toLocaleLowerCase() === effectiveName.toLocaleLowerCase()
    )
    if (duplicateName) {
      setError(t('roles.duplicateRoleName', { name: effectiveName }))
      return
    }

    setSaving(true)
    const ok = await run(async () => {
      let roleId = roleEditor.id
      const body = {
        name: effectiveName,
        description: roleEditor.description?.trim() || null,
        roleSetId: sourceRole?.roleSetId || null,
      }

      if (roleId) {
        await updateGroupRole(token, workspaceId, group.id, roleId, body)
      } else {
        const updated = await addGroupRole(token, workspaceId, group.id, body)
        roleId = updated.roles.find(r => r.name === effectiveName)?.id
        if (!roleId) throw new Error(t('roles.roleIdResolveFailed'))
      }

      const existing = grantsFor(roleId)

      for (const grant of existing) {
        if (!roleEditor.permissions[grant.permission] || roleEditor.permissions[grant.permission] !== grant.scope) {
          await removeGroupPermission(token, workspaceId, group.id, grant.id)
        }
      }

      for (const [permission, scope] of Object.entries(roleEditor.permissions)) {
        if (scope && !existing.some(g => g.permission === permission && g.scope === scope)) {
          await addGroupPermission(token, workspaceId, group.id, roleId, { permission, scope })
        }
      }
    })

    setSaving(false)
    if (ok) setRoleEditor(null)
  }

  const removeRole = async role => {
    if (!window.confirm(t('roles.deleteLocalRoleConfirm', { name: role.name }))) return
    await run(() => deleteGroupRole(token, workspaceId, group.id, role.id))
  }

  const saveMemberRoles = async () => {
    setSaving(true)
    const ok = await run(() => updateGroupMemberRoles(
      token,
      workspaceId,
      group.id,
      memberRoleEditor.memberId,
      [...memberRoleEditor.roleIds]
    ))
    setSaving(false)
    if (ok) setMemberRoleEditor(null)
  }

  return <div className="modal-backdrop">
    <div className="modal-card group-editor" role="dialog" aria-modal="true">
      <div className="group-editor-header">
        <h2>{t('roles.groupEditorTitle')}</h2>
        <button type="button" className="group-editor-close-x" onClick={onClose}>×</button>
      </div>

      <div className="group-editor-scroll">
        {error && <div className="page-error group-editor-error">{error}</div>}

        <div className="group-editor-grid">
          <section className="group-editor-panel">
            <h3>{t('roles.basicData')}</h3>
            <label className="form-field">
              <span>{t('roles.name')}</span>
              <input value={name} onChange={e => setName(e.target.value)} />
            </label>
            <label className="form-field">
              <span>{t('roles.descriptionField')}</span>
              <textarea value={description} onChange={e => setDescription(e.target.value)} />
            </label>
            <label className="group-navigation-toggle">
              <input type="checkbox" checked={showInNavigation} onChange={e => setShowInNavigation(e.target.checked)} />
              <span>{t('roles.showInNavigation')}</span>
            </label>
          </section>

          <section className="group-editor-panel">
            <div className="role-section-heading"><h3>{t('roles.peopleAndRoles')}</h3></div>

            <div className="group-edit-row">
              <select value={memberId} onChange={e => setMemberId(e.target.value)}>
                <option value="">{t('roles.selectPerson')}</option>
                {availableMembers.map(m => <option key={m.id} value={m.id}>{m.name}</option>)}
              </select>
              <button
                type="button"
                disabled={!memberId}
                onClick={() => run(async () => {
                  await addGroupMember(token, workspaceId, group.id, { memberId: Number(memberId), roleIds: [] })
                  setMemberId('')
                })}
              >{t('roles.add')}</button>
            </div>

            <div className="group-editor-list">
              {group.members.map(m => <div className="role-member-card" key={m.memberId}>
                <div className="group-edit-item">
                  <strong>{m.memberName}</strong>
                  <span className="group-edit-actions">
                    <button
                      type="button"
                      onClick={() => setMemberRoleEditor({
                        memberId: m.memberId,
                        memberName: m.memberName,
                        roleIds: new Set(m.roleIds || []),
                      })}
                    >{t('roles.chooseRoles')}</button>
                    <button type="button" onClick={() => run(() => removeGroupMember(token, workspaceId, group.id, m.memberId))}>×</button>
                  </span>
                </div>
                <div className="role-chip-list">
                  {(m.roleIds || []).length
                    ? (m.roleIds || []).map(id => <span className="role-chip" key={id}>{roleName(id)}</span>)
                    : <span className="role-muted">{t('roles.noRolesAssigned')}</span>}
                </div>
              </div>)}
            </div>
          </section>

          <section className="group-editor-panel">
            <h3>{t('roles.subgroups')}</h3>

            <div className="group-edit-row">
              <select value={childId} onChange={e => setChildId(e.target.value)}>
                <option value="">{t('roles.selectGroup')}</option>
                {availableGroups.map(g => <option key={g.id} value={g.id}>{g.name}</option>)}
              </select>
              <button
                type="button"
                disabled={!childId}
                onClick={() => run(async () => {
                  await addChildGroup(token, workspaceId, group.id, Number(childId))
                  setChildId('')
                })}
              >{t('roles.add')}</button>
            </div>

            <div className="group-editor-list">
              {group.childGroups.map(c => <div className="group-edit-item" key={c.groupId}>
                <span>↳ {c.groupName}</span>
                <button type="button" onClick={() => run(() => removeChildGroup(token, workspaceId, group.id, c.groupId))}>×</button>
              </div>)}
            </div>

            <div className="role-section-heading">
              <div>
                <h3>{t('roles.availableRoles')}</h3>
                <small className="role-muted">{t('roles.availableRolesHint')}</small>
              </div>
            </div>

            <div className="group-editor-list">
              {group.roles.length
                ? group.roles.map(role => {
                    const isCustomLocal = role.visibility === 'PRIVATE' && !role.systemDefault
                    return <div className="role-list-row role-list-row-static" key={role.id}>
                      <span>
                        <strong>{roleDisplayName(role)}</strong>
                        <small>
                          {role.systemDefault
                            ? t('roles.predefinedRole')
                            : isCustomLocal
                              ? t('roles.customGroupRole')
                              : t('roles.sharedRole')}
                        </small>
                        {role.description && !role.systemDefault && <small>{role.description}</small>}
                      </span>
                      {isCustomLocal
                        ? <span className="group-edit-actions">
                            <button type="button" onClick={() => openRole(role)}>{t('common.edit')}</button>
                            <button type="button" className="danger-link" onClick={() => removeRole(role)}>{t('roles.delete')}</button>
                          </span>
                        : <span className="role-system-badge">{t('roles.readyToAssign')}</span>}
                    </div>
                  })
                : <div className="role-muted">{t('roles.noAvailableRoles')}</div>}
            </div>

            <button type="button" className="secondary-button role-add-button" onClick={openNewRole}>
              + {t('roles.createCustomRole')}
            </button>
          </section>
        </div>
      </div>

      <div className="modal-actions group-editor-actions">
        <button type="button" className="secondary-button" onClick={onClose}>{t('common.cancel')}</button>
        <button type="button" className="add-member-button" onClick={saveMain} disabled={saving || !name.trim()}>
          {saving ? t('common.saving') : t('roles.save')}
        </button>
      </div>

      {roleEditor && <div className="nested-modal-backdrop">
        <form className="nested-modal-card role-editor-modal simplified-role-editor" onSubmit={saveRole}>
          <div className="group-editor-header">
            <div>
              <h3>{roleEditor.id ? t('roles.editLocalRole') : t('roles.newLocalRole')}</h3>
              <p className="role-muted">{t('roles.localRoleEditorHint')}</p>
            </div>
            <button type="button" className="group-editor-close-x" onClick={() => setRoleEditor(null)}>×</button>
          </div>

          {error && <div className="page-error" style={{ marginTop: '12px' }}>{error}</div>}

          <label className="form-field">
            <span>{t('roles.name')}</span>
            <input value={roleEditor.name} onChange={e => setRoleEditor({ ...roleEditor, name: e.target.value, nameTouched: true })} />
          </label>

          <label className="form-field">
            <span>{t('roles.descriptionField')}</span>
            <textarea value={roleEditor.description || ''} onChange={e => setRoleEditor({ ...roleEditor, description: e.target.value })} />
          </label>

          {duplicatePermissionRole && <div className="role-duplicate-warning">
            {t('roles.duplicatePermissionsWarning', { name: duplicatePermissionRole.name })}
          </div>}

          <label className="form-field">
            <span>{t('roles.permissionProfile')}</span>
            <select value={roleEditor.profile} onChange={e => applyProfile(e.target.value)}>
              {['NONE', 'EXECUTOR', 'SENIOR', 'LEADER', 'CONTROL', 'CUSTOM'].map(profile =>
                <option key={profile} value={profile}>{t(`roles.permissionProfiles.${profile}`)}</option>
              )}
            </select>
            <small className="role-muted">{t('roles.permissionProfileHint')}</small>
          </label>

          <button
            type="button"
            className="advanced-permissions-toggle"
            onClick={() => setShowAdvancedPermissions(v => !v)}
          >
            {showAdvancedPermissions ? '▾' : '▸'} {t('roles.advancedPermissions')}
          </button>

          {showAdvancedPermissions && <div className="role-permission-groups">
            {permissionGroups.map(groupItem => <section className="role-permission-group" key={groupItem.key}>
              <h5>{t(`rolePermissions.groups.${groupItem.key}`)}</h5>
              <div className="role-permissions">
                {groupItem.permissions.map(permission => {
                  const scope = roleEditor.permissions[permission]
                  return <div className="role-permission-row" key={permission}>
                    <label className="role-permission-check">
                      <input
                        type="checkbox"
                        checked={Boolean(scope)}
                        onChange={e => updatePermission(permission, e.target.checked)}
                      />
                      <span>{t(`rolePermissions.permissions.${permission}`, { defaultValue: permission })}</span>
                    </label>
                    {scope && <select
                      value={scope}
                      onChange={e => updatePermission(permission, true, e.target.value)}
                    >
                      <option value="GROUP">{t('rolePermissions.scopes.GROUP')}</option>
                      <option value="GROUP_SUBTREE">{t('rolePermissions.scopes.GROUP_SUBTREE')}</option>
                    </select>}
                  </div>
                })}
              </div>
            </section>)}
          </div>}

          <div className="modal-actions">
            <button type="button" className="secondary-button" onClick={() => setRoleEditor(null)}>{t('common.cancel')}</button>
            <button type="submit" className="add-member-button" disabled={saving}>
              {saving ? t('common.saving') : t('roles.save')}
            </button>
          </div>
        </form>
      </div>}

      {memberRoleEditor && <div className="nested-modal-backdrop">
        <div className="nested-modal-card">
          <div className="group-editor-header">
            <h3>{t('roles.rolesFor', { name: memberRoleEditor.memberName })}</h3>
            <button type="button" className="group-editor-close-x" onClick={() => setMemberRoleEditor(null)}>×</button>
          </div>

          {error && <div className="page-error" style={{ marginTop: '12px' }}>{error}</div>}

          {group.roles.map(role => <label className="role-choice-row" key={role.id}>
            <input
              type="checkbox"
              checked={memberRoleEditor.roleIds.has(role.id)}
              onChange={() => {
                const ids = new Set(memberRoleEditor.roleIds)
                ids.has(role.id) ? ids.delete(role.id) : ids.add(role.id)
                setMemberRoleEditor({ ...memberRoleEditor, roleIds: ids })
              }}
            />
            <span>{roleDisplayName(role)}</span>
          </label>)}

          {!group.roles.length && <div className="role-muted">{t('roles.createLocalRoleFirst')}</div>}

          <div className="modal-actions">
            <button type="button" className="secondary-button" onClick={() => setMemberRoleEditor(null)}>{t('common.cancel')}</button>
            <button type="button" className="add-member-button" disabled={saving} onClick={saveMemberRoles}>
              {saving ? t('common.saving') : t('roles.saveRoles')}
            </button>
          </div>
        </div>
      </div>}
    </div>
  </div>
}

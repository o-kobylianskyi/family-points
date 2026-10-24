import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'

import { getWorkspaceMembers, createWorkspaceMember, updateWorkspaceMember, deleteWorkspaceMember, getWorkspaceRoles } from '../api/workspaceApi'
import MemberEditor from '../components/MemberEditor'
import FamilyAvatar from '../components/FamilyAvatar'
import EntityMedia from '../components/EntityMedia'
import { createMemberAccount, issueMemberEmailInvitation } from '../api/accountApi'
import { workspaceRoleLabel } from '../utils/workspaceRoleLabel'
import {
  getMemberGroups,
  createMemberGroup,
} from '../api/memberGroupApi'
import GroupEditor from '../components/GroupEditor'
import { useAuth } from '../context/AuthContext'

function MembersPage() {
  const { t } = useTranslation()

  const {
    currentUser,
    getAccessToken,
  } = useAuth()

  const [members, setMembers] = useState([])
  const [loading, setLoading] = useState(true)
  const [groups, setGroups] = useState([])
  const [newGroupName, setNewGroupName] = useState('')
  const [error, setError] = useState('')
  const [editingGroupId, setEditingGroupId] = useState(null)
  const [pageTab, setPageTab] = useState('members')
  const [roles, setRoles] = useState([])
  const [editingMember, setEditingMember] = useState(undefined)
  const [memberBusy, setMemberBusy] = useState(false)

  useEffect(() => {
    const loadData = async () => {
      try {
        setError('')

        const token = getAccessToken()

        const [membersData, groupsData, rolesData] = await Promise.all([
          getWorkspaceMembers(token, currentUser.workspaceId),
          getMemberGroups(token, currentUser.workspaceId),
          getWorkspaceRoles(token, currentUser.workspaceId),
        ])

        setMembers(membersData)
        setGroups(groupsData)
        setRoles(rolesData)
      } catch (error) {
        setError(error.message)
      } finally {
        setLoading(false)
      }
    }

    loadData()
  }, [currentUser.workspaceId])

  const refreshMembers = async () => {
    setMembers(await getWorkspaceMembers(getAccessToken(), currentUser.workspaceId))
  }

  const saveMember = async (body, account = null, inviteEmail = null) => {
    try {
      setMemberBusy(true)
      setError('')
      if (editingMember) {
        await updateWorkspaceMember(getAccessToken(), currentUser.workspaceId, editingMember.id, body)
        if (inviteEmail) await issueMemberEmailInvitation(getAccessToken(), currentUser.workspaceId, editingMember.id, inviteEmail)
        else if (account) await createMemberAccount(getAccessToken(), currentUser.workspaceId, editingMember.id, account)
      } else {
        const created = await createWorkspaceMember(getAccessToken(), currentUser.workspaceId, body)
        if (account || inviteEmail) {
          try {
            if (inviteEmail) await issueMemberEmailInvitation(getAccessToken(), currentUser.workspaceId, created.id, inviteEmail)
            else
            await createMemberAccount(getAccessToken(), currentUser.workspaceId, created.id, account)
          } catch (accountError) {
            await refreshMembers()
            setEditingMember(created)
            throw new Error(`Учасника створено, але доступ ще не налаштовано: ${accountError.message}. Профіль уже існує; не створюйте його вдруге.`)
          }
        }
      }
      await refreshMembers()
      await refreshGroups()
      setEditingMember(undefined)
    } catch (e) { setError(e.message) } finally { setMemberBusy(false) }
  }

  const removeMember = async () => {
    if (!editingMember) return
    try {
      setMemberBusy(true)
      setError('')
      await deleteWorkspaceMember(getAccessToken(), currentUser.workspaceId, editingMember.id)
      await refreshMembers()
      await refreshGroups()
      setEditingMember(undefined)
    } catch (e) { setError(e.message) } finally { setMemberBusy(false) }
  }

  const refreshGroups = async () => {
    const token = getAccessToken()

    setGroups(
      await getMemberGroups(
        token,
        currentUser.workspaceId
      )
    )
  }

  const handleCreateGroup = async () => {
    const name = newGroupName.trim()

    if (!name) {
      return
    }

    try {
      setError('')

      await createMemberGroup(
        getAccessToken(),
        currentUser.workspaceId,
        {
          name,
          description: null,
        }
      )

      setNewGroupName('')
      await refreshGroups()
    } catch (error) {
      setError(error.message)
    }
  }

  const getInitials = (name) => {
    if (!name) {
      return '?'
    }

    return name
      .trim()
      .split(/\s+/)
      .slice(0, 2)
      .map((part) => part[0])
      .join('')
      .toUpperCase()
  }

  const canManageMembers =
    currentUser.permissions.includes('MANAGE_MEMBERS')

  /*
   * Backend currently returns groups as a flat collection.
   * childGroups contains references to nested groups.
   *
   * Find every group that is referenced as a child.
   */
  const childGroupIds = useMemo(() => {
    const ids = new Set()

    groups.forEach((group) => {
      group.childGroups?.forEach((child) => {
        ids.add(child.groupId)
      })
    })

    return ids
  }, [groups])

  /*
   * Only root groups become top-level cards.
   * Nested groups are rendered recursively.
   */
  const rootGroups = useMemo(
    () => groups.filter(
      (group) => !childGroupIds.has(group.id)
    ),
    [groups, childGroupIds]
  )

  const groupById = useMemo(
    () => new Map(
      groups.map((group) => [group.id, group])
    ),
    [groups]
  )

  const roleDisplayName = (role) =>
    role?.systemCode
      ? t(`roles.system.${role.systemCode}`, {
          defaultValue: role.name,
        })
      : role?.name

  const pointTypeDisplayName = (balance) =>
    t(`pointType.${balance.code}`, {
      defaultValue: balance.name || balance.code,
    })

  const renderGroupTree = (
    group,
    visited = new Set()
  ) => {
    /*
     * UI protection as well.
     * Backend should reject cycles, but rendering must
     * never recurse forever even if bad data exists.
     */
    if (visited.has(group.id)) {
      return (
        <div
          className="group-tree-cycle"
          key={`cycle-${group.id}`}
        >
          {t('memberUi.cyclic')}: {group.name}
        </div>
      )
    }

    const nextVisited = new Set(visited)
    nextVisited.add(group.id)

    const directMembers = group.members ?? []
    const childReferences = group.childGroups ?? []

    return (
      <div
        className="group-tree-node"
        key={group.id}
      >
        <div className="group-tree-node-header">
          <div className="group-tree-marker" />

          <div className="group-tree-node-content">
            <strong>{group.name}</strong>
            <EntityMedia type="MEMBER_GROUP" targetId={group.id} slot="ICON" canEdit={currentUser.permissions?.includes('MANAGE_GROUPS')||currentUser.permissions?.includes('ADMIN_OVERRIDE')}/>

            <span>
              {directMembers.length} {t('memberUi.people')} ·{' '}
              {childReferences.length} {t('memberUi.subgroups')}
            </span>

            {canManageMembers && (
              <button
                type="button"
                className="group-tree-edit-button"
                title="Редагувати групу"
                onClick={(event) => {
                  event.stopPropagation()
                  setEditingGroupId(group.id)
                }}
              >
                ✎
              </button>
            )}
          </div>
        </div>

        {directMembers.length > 0 && (
          <div className="group-tree-members">
            {directMembers.map((member) => (
              <div
                className="group-tree-member"
                key={member.memberId}
              >
                <span className="group-tree-person-icon">
                  {getInitials(member.memberName)}
                </span>

                <span>{member.memberName}</span>
              </div>
            ))}
          </div>
        )}

        {childReferences.length > 0 && (
          <div className="group-tree-children">
            {childReferences.map((child) => {
              const fullChild =
                groupById.get(child.groupId)

              if (!fullChild) {
                return (
                  <div
                    className="group-tree-missing"
                    key={child.groupId}
                  >
                    {child.groupName}
                  </div>
                )
              }

              return renderGroupTree(
                fullChild,
                nextVisited
              )
            })}
          </div>
        )}
      </div>
    )
  }

  const renderGroupCard = (group) => {
    const directMembers = group.members ?? []
    const childGroups = group.childGroups ?? []
    const roles = group.roles ?? []
    const balances = group.balances ?? []

    return (
      <article
        className="group-card-v2"
        key={group.id}
      >
        <div className="group-card-header">
          <div>
            <h3>{group.name}</h3>

            <div className="group-card-summary">
              {directMembers.length} {t('memberUi.people')} ·{' '}
              {childGroups.length} {t('memberUi.subgroups')}
            </div>
          </div>

          {canManageMembers && (
            <button
              type="button"
              className="group-edit-button"
              onClick={() =>
                setEditingGroupId(group.id)
              }
            >
              Редагувати
            </button>
          )}
        </div>

        {group.description && (
          <p className="group-description">
            {group.description}
          </p>
        )}

        {roles.length > 0 && (
          <div className="group-card-section">
            <div className="group-card-label">
              {t('memberUi.roles')}
            </div>

            <div className="group-tags">
              {roles.map((role) => (
                <span key={role.id}>
                  {roleDisplayName(role)}
                </span>
              ))}
            </div>
          </div>
        )}

        <div className="group-card-section">
          <div className="group-card-label">
            {t('memberUi.structure')}
          </div>

          <div className="group-tree-root">
            {renderGroupTree(group)}
          </div>
        </div>

        {balances.length > 0 && (
          <div className="group-card-section">
            <div className="group-card-label">
              {t('memberUi.balances')}
            </div>

            <div className="group-balances">
              {balances.map((balance) => (
                <span key={balance.pointTypeId}>
                  <strong>{balance.amount}</strong>{' '}
                  {pointTypeDisplayName(balance)}
                </span>
              ))}
            </div>
          </div>
        )}
      </article>
    )
  }

  return (
    <>
      <div className="page-title">
        <h1>{t('memberUi.title')}</h1>

        <p>{t('memberUi.subtitle')}</p>
      </div>

      <div className="page-tabs members-page-tabs">
        <button type="button" className={pageTab === 'members' ? 'page-tab active' : 'page-tab'} onClick={() => setPageTab('members')}>{t('memberUi.members')}</button>
        <button type="button" className={pageTab === 'groups' ? 'page-tab active' : 'page-tab'} onClick={() => setPageTab('groups')}>{t('memberUi.groups')}</button>
      </div>

      <div className={pageTab === 'members' ? 'section-header' : 'section-header ui-hidden'}>
        <h2>
          {t('workspace.members', {
            count: members.length,
          })}
        </h2>

        {canManageMembers && (
          <button
            type="button"
            className="add-member-button"
            onClick={() => { setError(''); setEditingMember(null) }}
          >
            {t('workspace.addMember')}
          </button>
        )}
      </div>

      {loading && (
        <div className="page-loading">
          {t('common.loading')}
        </div>
      )}

      {error && (
        <div className="page-error">
          {error}
        </div>
      )}

      {!loading && (
        <div className={pageTab === 'members' ? 'members-grid' : 'members-grid ui-hidden'}>
          {members.map((member) => (
            <div
              className="member-card"
              key={member.id}
              onClick={() => { if (canManageMembers) { setError(''); setEditingMember(member) } }}
              role={canManageMembers ? "button" : undefined}
              tabIndex={canManageMembers ? 0 : undefined}
            >
              <EntityMedia compact type="MEMBER" targetId={member.id} slot="AVATAR" canEdit={canManageMembers}>
                <FamilyAvatar name={member.name} kind="member" className="member-avatar" />
              </EntityMedia>

              <div className="member-info">
                <strong>{member.name}</strong>

                {canManageMembers && (member.accountEmail || member.invitationEmail) && (
                  <div className="member-email" title={member.accountEmail || member.invitationEmail}>
                    {t('memberUi.email')}: {member.accountEmail || member.invitationEmail}
                  </div>
                )}

                {canManageMembers && member.accessStatus && (
                  <div className={member.accessStatus === 'PENDING_INVITATION' ? 'member-access-status pending' : 'member-access-status'}>
                    {{
                      ACTIVE: t('memberUi.active'),
                      DISABLED: t('memberUi.disabled'),
                      PENDING_INVITATION: t('memberUi.pending'),
                      NO_ACCOUNT: t('memberUi.noAccount'),
                    }[member.accessStatus] || member.accessStatus}
                  </div>
                )}

                {(roles.find(role => role.id === member.workspaceRoleId)?.code || member.workspaceRoleCode) !== member.memberType && <div className="member-type">
                  {t(`memberType.${member.memberType}`)}
                </div>}
              </div>

              <div className="member-role">
                {workspaceRoleLabel(t,
                  roles.find(role => role.id === member.workspaceRoleId)?.code || member.workspaceRoleCode,
                  member.workspaceRoleName)}
              </div>
            </div>
          ))}
        </div>
      )}

      {!loading && (
        <section className={pageTab === 'groups' ? 'member-groups-section' : 'member-groups-section ui-hidden'}>
          <div className="section-header">
            <div>
              <h2>{t('workspace.groups.title')}</h2>

              <p>
                {t('workspace.groups.description')}
              </p>
            </div>

            {canManageMembers && (
              <div className="group-create-row">
                <input
                  value={newGroupName}
                  placeholder={t("memberUi.groupNamePlaceholder")}
                  onChange={(event) =>
                    setNewGroupName(
                      event.target.value
                    )
                  }
                  onKeyDown={(event) => {
                    if (event.key === 'Enter') {
                      handleCreateGroup()
                    }
                  }}
                />

                <button
                  type="button"
                  className="add-member-button"
                  onClick={handleCreateGroup}
                >
                  {t('workspace.groups.create')}
                </button>
              </div>
            )}
          </div>

          {rootGroups.length === 0 ? (
            <div className="groups-empty">
              {t('memberUi.groupsEmpty')}
            </div>
          ) : (
            <div className="groups-grid-v2">
              {rootGroups.map(renderGroupCard)}
            </div>
          )}
        </section>
      )}

      {editingMember !== undefined && (
        <MemberEditor
          member={editingMember}
          roles={roles}
          busy={memberBusy}
          externalError={error}
          onSave={saveMember}
          onDelete={removeMember}
          onClose={() => setEditingMember(undefined)}
        />
      )}

      {editingGroupId && (() => {
        const group = groups.find(
          (item) => item.id === editingGroupId
        )

        return group ? (
          <GroupEditor
            group={group}
            groups={groups}
            members={members}
            token={getAccessToken()}
            workspaceId={currentUser.workspaceId}
            onChanged={refreshGroups}
            onClose={() =>
              setEditingGroupId(null)
            }
          />
        ) : null
      })()}
    </>
  )
}

export default MembersPage
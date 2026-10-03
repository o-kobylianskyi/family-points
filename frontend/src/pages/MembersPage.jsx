import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'

import { getWorkspaceMembers, createWorkspaceMember, updateWorkspaceMember, deleteWorkspaceMember, getWorkspaceRoles } from '../api/workspaceApi'
import MemberEditor from '../components/MemberEditor'
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

  const saveMember = async (body) => {
    try {
      setMemberBusy(true)
      setError('')
      if (editingMember) {
        await updateWorkspaceMember(getAccessToken(), currentUser.workspaceId, editingMember.id, body)
      } else {
        await createWorkspaceMember(getAccessToken(), currentUser.workspaceId, body)
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
          Циклічне посилання: {group.name}
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

            <span>
              {directMembers.length} осіб ·{' '}
              {childReferences.length} підгруп
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
              {directMembers.length} осіб ·{' '}
              {childGroups.length} підгруп
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
              Ролі
            </div>

            <div className="group-tags">
              {roles.map((role) => (
                <span key={role.id}>
                  {role.name}
                </span>
              ))}
            </div>
          </div>
        )}

        <div className="group-card-section">
          <div className="group-card-label">
            Структура
          </div>

          <div className="group-tree-root">
            {renderGroupTree(group)}
          </div>
        </div>

        {balances.length > 0 && (
          <div className="group-card-section">
            <div className="group-card-label">
              Баланс
            </div>

            <div className="group-balances">
              {balances.map((balance) => (
                <span key={balance.pointTypeId}>
                  <strong>{balance.amount}</strong>{' '}
                  {balance.code}
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
        <h1>Учасники</h1>

        <p>Люди та групи вашого простору</p>
      </div>

      <div className="page-tabs">
        <button type="button" className={pageTab === 'members' ? 'page-tab active' : 'page-tab'} onClick={() => setPageTab('members')}>Учасники</button>
        <button type="button" className={pageTab === 'groups' ? 'page-tab active' : 'page-tab'} onClick={() => setPageTab('groups')}>Групи</button>
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
            onClick={() => setEditingMember(null)}
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
              onClick={() => canManageMembers && setEditingMember(member)}
              role={canManageMembers ? "button" : undefined}
              tabIndex={canManageMembers ? 0 : undefined}
            >
              <div className="member-avatar">
                {getInitials(member.name)}
              </div>

              <div className="member-info">
                <strong>{member.name}</strong>

                <div className="member-type">
                  {t(
                    `memberType.${member.memberType}`
                  )}
                </div>
              </div>

              <div className="member-role">
                {member.workspaceRoleName}
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
                  placeholder={t(
                    'workspace.groups.namePlaceholder'
                  )}
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
              Груп поки немає
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
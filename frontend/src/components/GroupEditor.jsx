import { useEffect, useMemo, useState } from 'react'

import {
  updateMemberGroup,
  addGroupMember,
  removeGroupMember,
  addChildGroup,
  removeChildGroup,
  addGroupRole,
} from '../api/memberGroupApi'

export default function GroupEditor({
  group,
  groups,
  members,
  token,
  workspaceId,
  onChanged,
  onClose,
}) {
  const [name, setName] = useState(group.name)
  const [description, setDescription] = useState(
    group.description || ''
  )
  const [showInNavigation, setShowInNavigation] = useState(Boolean(group.showInNavigation))

  const [memberId, setMemberId] = useState('')
  const [childId, setChildId] = useState('')
  const [roleName, setRoleName] = useState('')
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  const availableMembers = useMemo(
    () =>
      members.filter(
        (member) =>
          !group.members.some(
            (item) => item.memberId === member.id
          )
      ),
    [members, group]
  )

  const availableGroups = useMemo(
    () =>
      groups.filter(
        (candidate) =>
          candidate.id !== group.id &&
          !group.childGroups.some(
            (child) =>
              child.groupId === candidate.id
          )
      ),
    [groups, group]
  )

  const hasUnsavedMainChanges =
    name.trim() !== group.name ||
    description.trim() !== (group.description || '') ||
    showInNavigation !== Boolean(group.showInNavigation)

  /*
   * ESC = close without saving name/description.
   */
  useEffect(() => {
    const handleKeyDown = (event) => {
      if (event.key === 'Escape') {
        event.preventDefault()
        onClose()
      }
    }

    window.addEventListener('keydown', handleKeyDown)

    return () => {
      window.removeEventListener(
        'keydown',
        handleKeyDown
      )
    }
  }, [onClose])

  async function run(action) {
    try {
      setError('')
      await action()
      await onChanged()
    } catch (error) {
      setError(error.message)
    }
  }

  async function handleSave() {
    const normalizedName = name.trim()

    if (!normalizedName) {
      setError('Назва групи не може бути порожньою.')
      return
    }

    try {
      setSaving(true)
      setError('')

      await updateMemberGroup(
        token,
        workspaceId,
        group.id,
        {
          name: normalizedName,
          description:
            description.trim() || null,
          showInNavigation,
        }
      )

      await onChanged()
      onClose()
    } catch (error) {
      setError(error.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="modal-backdrop">
      <div
        className="modal-card group-editor"
        role="dialog"
        aria-modal="true"
        aria-labelledby="group-editor-title"
      >
        <div className="group-editor-header">
          <h2 id="group-editor-title">
            Редагування групи
          </h2>

          <button
            type="button"
            className="group-editor-close-x"
            onClick={onClose}
            title="Закрити"
            aria-label="Закрити"
          >
            ×
          </button>
        </div>

        {error && (
          <div className="page-error">
            {error}
          </div>
        )}

        <label className="form-field">
          <span>Назва</span>

          <input
            value={name}
            onChange={(event) =>
              setName(event.target.value)
            }
          />
        </label>

        <label className="form-field">
          <span>Опис</span>

          <textarea
            value={description}
            onChange={(event) =>
              setDescription(event.target.value)
            }
          />
        </label>

        <label className="group-navigation-toggle">
          <input
            type="checkbox"
            checked={showInNavigation}
            onChange={(event) => setShowInNavigation(event.target.checked)}
          />
          <span>Показувати в навігації</span>
        </label>

        <h3>Особи</h3>

        <div className="group-edit-row">
          <select
            value={memberId}
            onChange={(event) =>
              setMemberId(event.target.value)
            }
          >
            <option value="">
              Оберіть особу…
            </option>

            {availableMembers.map((member) => (
              <option
                key={member.id}
                value={member.id}
              >
                {member.name}
              </option>
            ))}
          </select>

          <button
            type="button"
            disabled={!memberId}
            onClick={() =>
              run(async () => {
                await addGroupMember(
                  token,
                  workspaceId,
                  group.id,
                  {
                    memberId: Number(memberId),
                    roleIds: [],
                  }
                )

                setMemberId('')
              })
            }
          >
            Додати
          </button>
        </div>

        {group.members.map((member) => (
          <div
            className="group-edit-item"
            key={member.memberId}
          >
            <span>{member.memberName}</span>

            <button
              type="button"
              onClick={() =>
                run(() =>
                  removeGroupMember(
                    token,
                    workspaceId,
                    group.id,
                    member.memberId
                  )
                )
              }
              title="Видалити з групи"
            >
              ×
            </button>
          </div>
        ))}

        <h3>Підгрупи</h3>

        <div className="group-edit-row">
          <select
            value={childId}
            onChange={(event) =>
              setChildId(event.target.value)
            }
          >
            <option value="">
              Оберіть групу…
            </option>

            {availableGroups.map(
              (candidate) => (
                <option
                  key={candidate.id}
                  value={candidate.id}
                >
                  {candidate.name}
                </option>
              )
            )}
          </select>

          <button
            type="button"
            disabled={!childId}
            onClick={() =>
              run(async () => {
                await addChildGroup(
                  token,
                  workspaceId,
                  group.id,
                  Number(childId)
                )

                setChildId('')
              })
            }
          >
            Додати
          </button>
        </div>

        {group.childGroups.map((child) => (
          <div
            className="group-edit-item"
            key={child.groupId}
          >
            <span>↳ {child.groupName}</span>

            <button
              type="button"
              onClick={() =>
                run(() =>
                  removeChildGroup(
                    token,
                    workspaceId,
                    group.id,
                    child.groupId
                  )
                )
              }
              title="Видалити підгрупу"
            >
              ×
            </button>
          </div>
        ))}

        <h3>Ролі / керівництво</h3>

        <div className="group-edit-row">
          <input
            value={roleName}
            onChange={(event) =>
              setRoleName(event.target.value)
            }
            placeholder="LEADER, SENIOR…"
          />

          <button
            type="button"
            disabled={!roleName.trim()}
            onClick={() =>
              run(async () => {
                await addGroupRole(
                  token,
                  workspaceId,
                  group.id,
                  {
                    name: roleName.trim(),
                    description: null,
                  }
                )

                setRoleName('')
              })
            }
          >
            Додати роль
          </button>
        </div>

        <div className="group-tags">
          {group.roles.map((role) => (
            <span key={role.id}>
              {role.name}
            </span>
          ))}
        </div>

        <div className="modal-actions group-editor-actions">
          <button
            type="button"
            className="secondary-button"
            onClick={onClose}
            disabled={saving}
          >
            Скасувати
          </button>

          <button
            type="button"
            className="add-member-button"
            onClick={handleSave}
            disabled={
              saving ||
              !name.trim()
            }
          >
            {saving
              ? 'Збереження…'
              : 'Зберегти'}
          </button>
        </div>

        {hasUnsavedMainChanges && (
          <div className="group-editor-unsaved">
            Є незбережені зміни назви або опису
          </div>
        )}
      </div>
    </div>
  )
}
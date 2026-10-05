import { useEffect, useState } from 'react'

import { useAuth } from '../context/AuthContext'
import { getPointTypes } from '../api/economyApi'
import { getTaskDefinitions } from '../api/taskApi'
import {
  approveRewardRequest,
  createReward,
  createRewardCategory,
  createRewardRequest,
  getRewardCategories,
  getOpenRewardObligations,
  getRewardPurchases,
  getRewardRequests,
  getRewards,
  purchaseReward,
  purchaseRewardRequest,
  refreshRewardRequest,
  rejectRewardRequest,
} from '../api/rewardApi'

const emptyRewardForm = {
  title: '',
  description: '',
  pointTypeId: '',
  priceAmount: 20,
  minimumReputation: '',
  acquisitionMode: 'DIRECT',
  requiresApproval: false,
  categoryIds: [],
  beforeTaskIds: [],
  afterTaskIds: [],
  blockingMode: 'NONE',
  blockedCategoryIds: [],
  blockedRewardDefinitionIds: [],
}

const emptyReviewForm = {
  pointTypeId: '',
  priceAmount: 20,
  minimumReputation: '',
  beforeTaskIds: [],
  afterTaskIds: [],
  blockingMode: 'NONE',
  blockedCategoryIds: [],
  blockedRewardDefinitionIds: [],
}

function toggleId(items, id) {
  return items.includes(id)
    ? items.filter((item) => item !== id)
    : [...items, id]
}

function taskRequirements(form) {
  const before = form.beforeTaskIds.map((taskDefinitionId) => ({
    phase: 'BEFORE_REWARD',
    requirementType: 'TASK_COMPLETED',
    taskDefinitionId,
    timeScope: 'TODAY',
    required: true,
    blockingMode: 'NONE',
    blockedCategoryIds: [],
    blockedRewardDefinitionIds: [],
  }))

  const after = form.afterTaskIds.map((taskDefinitionId) => ({
    phase: 'AFTER_REWARD',
    requirementType: 'TASK_COMPLETED',
    taskDefinitionId,
    timeScope: 'TODAY',
    required: true,
    blockingMode: form.blockingMode,
    blockedCategoryIds:
      form.blockingMode === 'CATEGORIES'
        ? form.blockedCategoryIds
        : [],
    blockedRewardDefinitionIds:
      form.blockingMode === 'SPECIFIC_REWARDS'
        ? form.blockedRewardDefinitionIds
        : [],
  }))

  return [...before, ...after]
}

function RewardsPage() {
  const { currentUser, getAccessToken } = useAuth()

  const [rewards, setRewards] = useState([])
  const [categories, setCategories] = useState([])
  const [requests, setRequests] = useState([])
  const [purchases, setPurchases] = useState([])
  const [obligations, setObligations] = useState([])
  const [pointTypes, setPointTypes] = useState([])
  const [tasks, setTasks] = useState([])

  const [pageTab, setPageTab] = useState('catalog')
  const [loading, setLoading] = useState(true)
  const [processing, setProcessing] = useState(false)
  const [error, setError] = useState('')

  const [categoryName, setCategoryName] = useState('')
  const [rewardForm, setRewardForm] = useState(emptyRewardForm)
  const [customRequestTitle, setCustomRequestTitle] = useState('')
  const [customRequestDescription, setCustomRequestDescription] = useState('')

  const [reviewingRequestId, setReviewingRequestId] = useState(null)
  const [reviewForm, setReviewForm] = useState(emptyReviewForm)

  const canManage = currentUser.permissions?.includes('MANAGE_REWARDS')
    || currentUser.permissions?.includes('ADMIN_OVERRIDE')

  const loadAll = async () => {
    try {
      setLoading(true)
      setError('')

      const token = getAccessToken()
      const workspaceId = currentUser.workspaceId

      const [
        rewardData,
        categoryData,
        requestData,
        purchaseData,
        obligationData,
        pointTypeData,
        taskData,
      ] = await Promise.all([
        getRewards(token, workspaceId),
        getRewardCategories(token, workspaceId),
        getRewardRequests(token, workspaceId),
        getRewardPurchases(token, workspaceId),
        getOpenRewardObligations(token, workspaceId),
        getPointTypes(token, workspaceId),
        getTaskDefinitions(token, workspaceId),
      ])

      setRewards(rewardData)
      setCategories(categoryData)
      setRequests(requestData)
      setPurchases(purchaseData)
      setObligations(obligationData)
      setPointTypes(pointTypeData)
      setTasks(taskData.filter((task) => task.active !== false))

      const defaultPointTypeId = pointTypeData[0]?.id ?? ''
      setRewardForm((current) => ({
        ...current,
        pointTypeId: current.pointTypeId || defaultPointTypeId,
      }))
      setReviewForm((current) => ({
        ...current,
        pointTypeId: current.pointTypeId || defaultPointTypeId,
      }))
    } catch (loadError) {
      setError(loadError.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadAll()
  }, [currentUser.workspaceId])

  const run = async (action) => {
    try {
      setProcessing(true)
      setError('')
      await action()
      await loadAll()
    } catch (actionError) {
      setError(actionError.message)
    } finally {
      setProcessing(false)
    }
  }

  const createCategory = async (event) => {
    event.preventDefault()
    const name = categoryName.trim()
    if (!name) return

    await run(async () => {
      await createRewardCategory(
        getAccessToken(),
        currentUser.workspaceId,
        { name, sortOrder: categories.length }
      )
      setCategoryName('')
    })
  }

  const createCatalogReward = async (event) => {
    event.preventDefault()
    if (!rewardForm.title.trim() || !rewardForm.pointTypeId) return

    await run(async () => {
      await createReward(
        getAccessToken(),
        currentUser.workspaceId,
        {
          title: rewardForm.title.trim(),
          description: rewardForm.description.trim() || null,
          pointTypeId: Number(rewardForm.pointTypeId),
          priceAmount: Number(rewardForm.priceAmount || 0),
          minimumReputation:
            rewardForm.minimumReputation === ''
              ? null
              : Number(rewardForm.minimumReputation),
          requiresApproval: rewardForm.requiresApproval,
          acquisitionMode: rewardForm.acquisitionMode,
          categoryIds: rewardForm.categoryIds,
          requirements: taskRequirements(rewardForm),
        }
      )

      setRewardForm((current) => ({
        ...emptyRewardForm,
        pointTypeId: current.pointTypeId,
      }))
    })
  }

  const requestCatalogReward = (reward) => run(async () => {
    await createRewardRequest(
      getAccessToken(),
      currentUser.workspaceId,
      { rewardDefinitionId: reward.id }
    )
    setPageTab('requests')
  })

  const createCustomRequest = async (event) => {
    event.preventDefault()
    if (!customRequestTitle.trim()) return

    await run(async () => {
      await createRewardRequest(
        getAccessToken(),
        currentUser.workspaceId,
        {
          rewardDefinitionId: null,
          title: customRequestTitle.trim(),
          description: customRequestDescription.trim() || null,
        }
      )
      setCustomRequestTitle('')
      setCustomRequestDescription('')
      setPageTab('requests')
    })
  }

  const beginReview = (request) => {
    const reward = rewards.find((item) => item.id === request.rewardDefinitionId)
    setReviewingRequestId(request.id)
    setReviewForm({
      ...emptyReviewForm,
      pointTypeId:
        request.pointTypeId
        || reward?.pointTypeId
        || pointTypes[0]?.id
        || '',
      priceAmount:
        request.priceAmount
        ?? reward?.priceAmount
        ?? 20,
      minimumReputation:
        request.minimumReputation
        ?? reward?.minimumReputation
        ?? '',
    })
  }

  const approveRequest = (requestId) => run(async () => {
    await approveRewardRequest(
      getAccessToken(),
      currentUser.workspaceId,
      requestId,
      {
        pointTypeId: Number(reviewForm.pointTypeId),
        priceAmount: Number(reviewForm.priceAmount || 0),
        minimumReputation:
          reviewForm.minimumReputation === ''
            ? null
            : Number(reviewForm.minimumReputation),
        requirements: taskRequirements(reviewForm),
      }
    )

    setReviewingRequestId(null)
  })

  const pointLabel = (pointTypeId, code) => {
    const type = pointTypes.find((item) => item.id === pointTypeId)
    return type?.name || code || ''
  }

  const requestStatusLabel = (status) => ({
    REQUESTED: 'Очікує рішення',
    APPROVED: 'Схвалено',
    WAITING_REQUIREMENTS: 'Очікує виконання умов',
    READY_TO_PURCHASE: 'Можна отримати',
    PURCHASED: 'Отримано',
    REJECTED: 'Відхилено',
    CANCELLED: 'Скасовано',
  }[status] || status)

  const acquisitionLabel = (mode) => ({
    DIRECT: 'Без запиту',
    REQUEST: 'Тільки за запитом',
    DIRECT_OR_REQUEST: 'Напряму або за запитом',
  }[mode] || mode)

  if (loading) {
    return <div className="page-loading">Завантаження…</div>
  }

  return (
    <div className="rewards-page">
      <div className="page-title">
        <div>
          <h1>Нагороди</h1>
          <p>Каталог винагород, запити, умови та авансові домовленості.</p>
        </div>
      </div>

      {error && <div className="page-error">{error}</div>}

      {obligations.length > 0 && (
        <section className="reward-obligation-banner">
          <strong>Невиконані домовленості після нагород</strong>
          {obligations.map((obligation) => (
            <div key={obligation.id} className="reward-obligation-row">
              <span>
                {obligation.memberName}: {obligation.title}
                {' · '}
                після «{obligation.rewardTitle}»
              </span>
              <span>{obligation.blockingMode}</span>
            </div>
          ))}
        </section>
      )}

      <div className="page-tabs">
        <button
          type="button"
          className={pageTab === 'catalog' ? 'page-tab active' : 'page-tab'}
          onClick={() => setPageTab('catalog')}
        >
          Каталог
        </button>
        <button
          type="button"
          className={pageTab === 'requests' ? 'page-tab active' : 'page-tab'}
          onClick={() => setPageTab('requests')}
        >
          Запити ({requests.length})
        </button>
        {canManage && (
          <button
            type="button"
            className={pageTab === 'manage' ? 'page-tab active' : 'page-tab'}
            onClick={() => setPageTab('manage')}
          >
            Налаштування
          </button>
        )}
      </div>

      {pageTab === 'catalog' && (
        <>
          <section className="rewards-grid">
            {rewards.length === 0 ? (
              <div className="empty-state">
                <strong>Нагород ще немає</strong>
                <p>Батьки можуть створити перші нагороди у вкладці «Налаштування».</p>
              </div>
            ) : rewards.map((reward) => (
              <article className="reward-card" key={reward.id}>
                <div className="reward-card-top">
                  <div>
                    <h3>{reward.title}</h3>
                    {reward.description && <p>{reward.description}</p>}
                  </div>
                  <strong className="reward-price">
                    {reward.priceAmount} {pointLabel(reward.pointTypeId, reward.pointTypeCode)}
                  </strong>
                </div>

                {reward.requirements?.length > 0 && (
                  <div className="reward-requirements-summary">
                    <strong>Умови:</strong>
                    {reward.requirements.map((requirement) => (
                      <div key={requirement.id}>
                        {requirement.phase === 'AFTER_REWARD' ? 'Після нагороди: ' : 'До нагороди: '}
                        {requirement.taskTitle || requirement.description || requirement.requirementType}
                      </div>
                    ))}
                  </div>
                )}

                <div className="reward-tags">
                  {reward.categories?.map((category) => (
                    <span key={category.id}>{category.name}</span>
                  ))}
                  <span>{acquisitionLabel(reward.acquisitionMode)}</span>
                  {reward.minimumReputation != null && (
                    <span>Репутація ≥ {reward.minimumReputation}</span>
                  )}
                </div>

                <div className="reward-actions">
                  {reward.acquisitionMode !== 'REQUEST' && !reward.requiresApproval && (
                    <button
                      type="button"
                      className="primary-button"
                      disabled={processing}
                      onClick={() => run(() =>
                        purchaseReward(
                          getAccessToken(),
                          currentUser.workspaceId,
                          reward.id
                        )
                      )}
                    >
                      Отримати
                    </button>
                  )}

                  {(reward.acquisitionMode !== 'DIRECT' || reward.requiresApproval) && (
                    <button
                      type="button"
                      className="secondary-button"
                      disabled={processing}
                      onClick={() => requestCatalogReward(reward)}
                    >
                      Попросити
                    </button>
                  )}
                </div>
              </article>
            ))}
          </section>

          <section className="reward-request-box">
            <h2>Хочу іншу нагороду</h2>
            <p>Наприклад: «хочу велосипед» або «хочу ще 30 хвилин пограти».</p>
            <form onSubmit={createCustomRequest} className="reward-request-form">
              <input
                value={customRequestTitle}
                maxLength={150}
                placeholder="Що ви хочете?"
                onChange={(event) => setCustomRequestTitle(event.target.value)}
              />
              <textarea
                value={customRequestDescription}
                rows={2}
                maxLength={1000}
                placeholder="Коментар (необов'язково)"
                onChange={(event) => setCustomRequestDescription(event.target.value)}
              />
              <button type="submit" className="primary-button" disabled={processing}>
                Надіслати запит
              </button>
            </form>
          </section>
        </>
      )}

      {pageTab === 'requests' && (
        <section className="reward-request-list">
          {requests.length === 0 ? (
            <div className="empty-state">Запитів поки немає.</div>
          ) : requests.map((request) => (
            <article className="reward-request-card" key={request.id}>
              <div className="reward-request-head">
                <div>
                  <h3>{request.title}</h3>
                  <p>{request.requestedByMemberName}</p>
                </div>
                <span className="reward-status">{requestStatusLabel(request.status)}</span>
              </div>

              {request.description && <p>{request.description}</p>}

              {request.priceAmount != null && (
                <div className="reward-request-price">
                  Ціна: <strong>{request.priceAmount} {pointLabel(request.pointTypeId, request.pointTypeCode)}</strong>
                </div>
              )}

              {request.requirements?.length > 0 && (
                <div className="reward-requirements-summary">
                  <strong>Умови:</strong>
                  {request.requirements.map((requirement) => (
                    <div key={requirement.id}>
                      {requirement.phase === 'AFTER_REWARD' ? 'Після нагороди: ' : 'До нагороди: '}
                      {requirement.taskTitle || requirement.description || requirement.requirementType}
                      {requirement.phase === 'AFTER_REWARD' && requirement.blockingMode !== 'NONE'
                        ? ` · блокування: ${requirement.blockingMode}`
                        : ''}
                    </div>
                  ))}
                </div>
              )}

              {canManage && request.status === 'REQUESTED' && reviewingRequestId !== request.id && (
                <div className="reward-actions">
                  <button type="button" className="primary-button" onClick={() => beginReview(request)}>
                    Налаштувати й схвалити
                  </button>
                  <button
                    type="button"
                    className="secondary-button"
                    disabled={processing}
                    onClick={() => run(() =>
                      rejectRewardRequest(
                        getAccessToken(),
                        currentUser.workspaceId,
                        request.id
                      )
                    )}
                  >
                    Відхилити
                  </button>
                </div>
              )}

              {canManage && reviewingRequestId === request.id && (
                <div className="reward-review-panel">
                  <div className="reward-form-grid">
                    <label>
                      <span>Тип балів</span>
                      <select
                        value={reviewForm.pointTypeId}
                        onChange={(event) => setReviewForm({
                          ...reviewForm,
                          pointTypeId: event.target.value,
                        })}
                      >
                        {pointTypes.map((pointType) => (
                          <option key={pointType.id} value={pointType.id}>{pointType.name}</option>
                        ))}
                      </select>
                    </label>
                    <label>
                      <span>Ціна</span>
                      <input
                        type="number"
                        min="0"
                        value={reviewForm.priceAmount}
                        onChange={(event) => setReviewForm({
                          ...reviewForm,
                          priceAmount: event.target.value,
                        })}
                      />
                    </label>
                    <label>
                      <span>Мінімальна репутація</span>
                      <input
                        type="number"
                        min="0"
                        value={reviewForm.minimumReputation}
                        placeholder="Без обмеження"
                        onChange={(event) => setReviewForm({
                          ...reviewForm,
                          minimumReputation: event.target.value,
                        })}
                      />
                    </label>
                  </div>

                  <TaskConditionPicker
                    title="Виконати ДО нагороди"
                    tasks={tasks}
                    selected={reviewForm.beforeTaskIds}
                    onToggle={(id) => setReviewForm({
                      ...reviewForm,
                      beforeTaskIds: toggleId(reviewForm.beforeTaskIds, id),
                    })}
                  />

                  <TaskConditionPicker
                    title="Виконати ПІСЛЯ нагороди (аванс)"
                    tasks={tasks}
                    selected={reviewForm.afterTaskIds}
                    onToggle={(id) => setReviewForm({
                      ...reviewForm,
                      afterTaskIds: toggleId(reviewForm.afterTaskIds, id),
                    })}
                  />

                  {reviewForm.afterTaskIds.length > 0 && (
                    <BlockingEditor
                      categories={categories}
                      rewards={rewards}
                      form={reviewForm}
                      setForm={setReviewForm}
                    />
                  )}

                  <div className="reward-actions">
                    <button type="button" className="primary-button" disabled={processing} onClick={() => approveRequest(request.id)}>
                      Схвалити
                    </button>
                    <button type="button" className="secondary-button" onClick={() => setReviewingRequestId(null)}>
                      Скасувати
                    </button>
                  </div>
                </div>
              )}

              {!canManage && request.status === 'WAITING_REQUIREMENTS' && (
                <button
                  type="button"
                  className="secondary-button"
                  disabled={processing}
                  onClick={() => run(() =>
                    refreshRewardRequest(
                      getAccessToken(),
                      currentUser.workspaceId,
                      request.id
                    )
                  )}
                >
                  Перевірити умови
                </button>
              )}

              {!canManage && request.status === 'READY_TO_PURCHASE' && (
                <button
                  type="button"
                  className="primary-button"
                  disabled={processing}
                  onClick={() => run(() =>
                    purchaseRewardRequest(
                      getAccessToken(),
                      currentUser.workspaceId,
                      request.id
                    )
                  )}
                >
                  Отримати нагороду
                </button>
              )}
            </article>
          ))}
        </section>
      )}

      {pageTab === 'manage' && canManage && (
        <div className="reward-manage-layout">
          <section className="reward-admin-panel">
            <h2>Категорії</h2>
            <div className="reward-tags">
              {categories.map((category) => (
                <span key={category.id}>{category.name}</span>
              ))}
            </div>
            <form onSubmit={createCategory} className="reward-inline-form">
              <input
                value={categoryName}
                placeholder="Нова категорія"
                maxLength={100}
                onChange={(event) => setCategoryName(event.target.value)}
              />
              <button type="submit" className="secondary-button" disabled={processing}>
                Додати
              </button>
            </form>
          </section>

          <section className="reward-admin-panel">
            <h2>Нова нагорода</h2>
            <form onSubmit={createCatalogReward} className="reward-editor">
              <label>
                <span>Назва</span>
                <input
                  required
                  maxLength={150}
                  value={rewardForm.title}
                  onChange={(event) => setRewardForm({
                    ...rewardForm,
                    title: event.target.value,
                  })}
                />
              </label>

              <label>
                <span>Опис</span>
                <textarea
                  rows={2}
                  maxLength={1000}
                  value={rewardForm.description}
                  onChange={(event) => setRewardForm({
                    ...rewardForm,
                    description: event.target.value,
                  })}
                />
              </label>

              <div className="reward-form-grid">
                <label>
                  <span>Тип балів</span>
                  <select
                    value={rewardForm.pointTypeId}
                    onChange={(event) => setRewardForm({
                      ...rewardForm,
                      pointTypeId: event.target.value,
                    })}
                  >
                    {pointTypes.map((pointType) => (
                      <option key={pointType.id} value={pointType.id}>{pointType.name}</option>
                    ))}
                  </select>
                </label>

                <label>
                  <span>Ціна</span>
                  <input
                    type="number"
                    min="0"
                    value={rewardForm.priceAmount}
                    onChange={(event) => setRewardForm({
                      ...rewardForm,
                      priceAmount: event.target.value,
                    })}
                  />
                </label>

                <label>
                  <span>Мінімальна репутація</span>
                  <input
                    type="number"
                    min="0"
                    value={rewardForm.minimumReputation}
                    placeholder="Без обмеження"
                    onChange={(event) => setRewardForm({
                      ...rewardForm,
                      minimumReputation: event.target.value,
                    })}
                  />
                </label>

                <label>
                  <span>Отримання</span>
                  <select
                    value={rewardForm.acquisitionMode}
                    onChange={(event) => setRewardForm({
                      ...rewardForm,
                      acquisitionMode: event.target.value,
                    })}
                  >
                    <option value="DIRECT">Без запиту</option>
                    <option value="REQUEST">Тільки за запитом</option>
                    <option value="DIRECT_OR_REQUEST">Напряму або за запитом</option>
                  </select>
                </label>
              </div>

              <label className="form-checkbox">
                <input
                  type="checkbox"
                  checked={rewardForm.requiresApproval}
                  onChange={(event) => setRewardForm({
                    ...rewardForm,
                    requiresApproval: event.target.checked,
                  })}
                />
                <span>Потрібне підтвердження дорослим</span>
              </label>

              <div>
                <strong>Категорії</strong>
                <div className="reward-checkbox-grid">
                  {categories.map((category) => (
                    <label className="form-checkbox" key={category.id}>
                      <input
                        type="checkbox"
                        checked={rewardForm.categoryIds.includes(category.id)}
                        onChange={() => setRewardForm({
                          ...rewardForm,
                          categoryIds: toggleId(rewardForm.categoryIds, category.id),
                        })}
                      />
                      <span>{category.name}</span>
                    </label>
                  ))}
                </div>
              </div>

              <TaskConditionPicker
                title="Умови ДО нагороди"
                tasks={tasks}
                selected={rewardForm.beforeTaskIds}
                onToggle={(id) => setRewardForm({
                  ...rewardForm,
                  beforeTaskIds: toggleId(rewardForm.beforeTaskIds, id),
                })}
              />

              <TaskConditionPicker
                title="Умови ПІСЛЯ нагороди (аванс)"
                tasks={tasks}
                selected={rewardForm.afterTaskIds}
                onToggle={(id) => setRewardForm({
                  ...rewardForm,
                  afterTaskIds: toggleId(rewardForm.afterTaskIds, id),
                })}
              />

              {rewardForm.afterTaskIds.length > 0 && (
                <BlockingEditor
                  categories={categories}
                  rewards={rewards}
                  form={rewardForm}
                  setForm={setRewardForm}
                />
              )}

              <button type="submit" className="primary-button" disabled={processing}>
                Створити нагороду
              </button>
            </form>
          </section>

          <section className="reward-admin-panel">
            <h2>Останні отримання</h2>
            {purchases.length === 0 ? (
              <p>Ще немає.</p>
            ) : purchases.slice(0, 10).map((purchase) => (
              <div className="reward-purchase-row" key={purchase.id}>
                <strong>{purchase.rewardTitle}</strong>
                <span>{purchase.memberName}</span>
                <span>{purchase.priceAmount} {pointLabel(purchase.pointTypeId, purchase.pointTypeCode)}</span>
              </div>
            ))}
          </section>
        </div>
      )}
    </div>
  )
}

function TaskConditionPicker({ title, tasks, selected, onToggle }) {
  return (
    <div className="reward-condition-picker">
      <strong>{title}</strong>
      <small>У початковій версії перевірка виконується для сьогоднішнього TaskInstance.</small>
      <div className="reward-checkbox-grid">
        {tasks.map((task) => (
          <label className="form-checkbox" key={task.id}>
            <input
              type="checkbox"
              checked={selected.includes(task.id)}
              onChange={() => onToggle(task.id)}
            />
            <span>№{task.id} · {task.title}</span>
          </label>
        ))}
      </div>
    </div>
  )
}

function BlockingEditor({ categories, rewards, form, setForm }) {
  return (
    <div className="reward-blocking-editor">
      <strong>Що робити, поки післяумова не виконана?</strong>
      <select
        value={form.blockingMode}
        onChange={(event) => setForm({
          ...form,
          blockingMode: event.target.value,
        })}
      >
        <option value="NONE">Не блокувати</option>
        <option value="WARN_ONLY">Лише попереджати</option>
        <option value="ALL_REWARDS">Блокувати всі нагороди</option>
        <option value="CATEGORIES">Блокувати вибрані категорії</option>
        <option value="SPECIFIC_REWARDS">Блокувати конкретні нагороди</option>
      </select>

      {form.blockingMode === 'CATEGORIES' && (
        <div className="reward-checkbox-grid">
          {categories.map((category) => (
            <label className="form-checkbox" key={category.id}>
              <input
                type="checkbox"
                checked={form.blockedCategoryIds.includes(category.id)}
                onChange={() => setForm({
                  ...form,
                  blockedCategoryIds: toggleId(form.blockedCategoryIds, category.id),
                })}
              />
              <span>{category.name}</span>
            </label>
          ))}
        </div>
      )}

      {form.blockingMode === 'SPECIFIC_REWARDS' && (
        <div className="reward-checkbox-grid">
          {rewards.map((reward) => (
            <label className="form-checkbox" key={reward.id}>
              <input
                type="checkbox"
                checked={form.blockedRewardDefinitionIds.includes(reward.id)}
                onChange={() => setForm({
                  ...form,
                  blockedRewardDefinitionIds: toggleId(
                    form.blockedRewardDefinitionIds,
                    reward.id
                  ),
                })}
              />
              <span>{reward.title}</span>
            </label>
          ))}
        </div>
      )}
    </div>
  )
}

export default RewardsPage

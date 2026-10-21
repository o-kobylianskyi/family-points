import { useEffect, useRef, useState } from 'react'

import { useAuth } from '../context/AuthContext'
import { getPointTypes, getPointNameForms } from '../api/economyApi'
import { getMemberBalances } from '../api/pointsApi'
import { getMyReputation } from '../api/reputationApi'
import { currencyQuantity } from '../utils/formatCurrencyAmount'
import { useTranslation } from 'react-i18next'
import { getTaskDefinitions } from '../api/taskApi'
import {
  approveRewardRequest,
  createReward,
  updateReward,
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
  cooldownSeconds: 30,
  memberTotalLimit: '',
  memberDailyLimit: '',
  memberWeeklyLimit: '',
  workspaceTotalLimit: '',
  workspaceDailyLimit: '',
  workspaceWeeklyLimit: '',
  extraRequestAllowed: false,
  minimumReputation: '',
  acquisitionMode: 'DIRECT',
  rewardKind: 'STANDARD',
  defaultDurationMinutes: '',
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
  durationMinutes: '',
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
    timeScope: 'SINCE_REWARD',
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
  const { i18n } = useTranslation()
  const [currencyForms, setCurrencyForms] = useState({})

  const [balances, setBalances] = useState([])
  const [reputation, setReputation] = useState(null)
  const [notice, setNotice] = useState('')
  const actionLock = useRef(false)
  const purchaseLocks = useRef(new Set())
  const [pendingPurchases, setPendingPurchases] = useState([])
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
  const [editingRewardId, setEditingRewardId] = useState(null)
  const [customRequestTitle, setCustomRequestTitle] = useState('')
  const [customRequestDescription, setCustomRequestDescription] = useState('')

  const [reviewingRequestId, setReviewingRequestId] = useState(null)
  const [reviewForm, setReviewForm] = useState(emptyReviewForm)

  const myRequestForReward = (rewardId) => requests.filter((request) =>
    request.rewardDefinitionId === rewardId &&
    request.requestedByMemberId === currentUser.memberId &&
    ['REQUESTED', 'APPROVED', 'WAITING_REQUIREMENTS', 'READY_TO_PURCHASE'].includes(request.status)
  ).sort((a, b) => b.id - a.id)[0]

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
        balanceData,
        reputationData,
      ] = await Promise.all([
        getRewards(token, workspaceId),
        getRewardCategories(token, workspaceId),
        getRewardRequests(token, workspaceId),
        getRewardPurchases(token, workspaceId),
        getOpenRewardObligations(token, workspaceId),
        getPointTypes(token, workspaceId),
        getTaskDefinitions(token, workspaceId),
        getMemberBalances(token, workspaceId, currentUser.memberId),
        getMyReputation(token),
      ])

      setBalances(balanceData.balances || [])
      setReputation(reputationData.balance)
      setRewards(rewardData)
      setCategories(categoryData)
      setRequests(requestData)
      setPurchases(purchaseData)
      setObligations(obligationData)
      setPointTypes(pointTypeData)
      const forms = await Promise.all(pointTypeData.map(async type => {
        try { return [type.id, await getPointNameForms(token, workspaceId, type.id)] }
        catch { return [type.id, []] }
      }))
      setCurrencyForms(Object.fromEntries(forms))
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

  const run = async (action, successMessage = '') => {
    // Synchronous guard: React state updates alone cannot stop rapid double taps.
    if (actionLock.current) return
    actionLock.current = true
    try {
      setProcessing(true)
      setError('')
      setNotice('')
      await action()
      if (successMessage) setNotice(successMessage)
      await loadAll()
    } catch (actionError) {
      setError(actionError.message)
    } finally {
      actionLock.current = false
      setProcessing(false)
    }
  }

  const purchaseOnce = async (rewardKey, action, rewardTitle) => {
    // Lock only this reward, allowing another distinct reward to be obtained.
    if (purchaseLocks.current.has(rewardKey)) return
    purchaseLocks.current.add(rewardKey)
    setPendingPurchases(Array.from(purchaseLocks.current))
    setError('')
    setNotice('')
    try {
      await action()
      setNotice(t('rewardsUi.received', { title: rewardTitle }))
      await loadAll()
    } catch (actionError) {
      setError(actionError.message)
    } finally {
      purchaseLocks.current.delete(rewardKey)
      setPendingPurchases(Array.from(purchaseLocks.current))
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

  const beginEditReward = (reward) => {
    const before = (reward.requirements || []).filter(r =>
      r.phase === 'BEFORE_REWARD' && r.requirementType === 'TASK_COMPLETED' && r.taskDefinitionId != null)
    const after = (reward.requirements || []).filter(r =>
      r.phase === 'AFTER_REWARD' && r.requirementType === 'TASK_COMPLETED' && r.taskDefinitionId != null)
    const blocking = after.find(r => r.blockingMode && r.blockingMode !== 'NONE')
    setEditingRewardId(reward.id)
    setRewardForm({
      ...emptyRewardForm,
      title: reward.title,
      description: reward.description || '',
      pointTypeId: reward.pointTypeId,
      priceAmount: reward.priceAmount,
      cooldownSeconds: reward.cooldownSeconds ?? 30,
      memberTotalLimit: reward.memberTotalLimit ?? '',
      memberDailyLimit: reward.memberDailyLimit ?? '',
      memberWeeklyLimit: reward.memberWeeklyLimit ?? '',
      workspaceTotalLimit: reward.workspaceTotalLimit ?? '',
      workspaceDailyLimit: reward.workspaceDailyLimit ?? '',
      workspaceWeeklyLimit: reward.workspaceWeeklyLimit ?? '',
      extraRequestAllowed: reward.extraRequestAllowed ?? false,
      minimumReputation: reward.minimumReputation ?? '',
      requiresApproval: reward.requiresApproval,
      acquisitionMode: reward.acquisitionMode,
      rewardKind: reward.rewardKind,
      defaultDurationMinutes: reward.defaultDurationMinutes ?? '',
      categoryIds: (reward.categories || []).map(c => c.id),
      beforeTaskIds: before.map(r => r.taskDefinitionId),
      afterTaskIds: after.map(r => r.taskDefinitionId),
      blockingMode: blocking?.blockingMode || 'NONE',
      blockedCategoryIds: blocking?.blockedCategoryIds || [],
      blockedRewardDefinitionIds: blocking?.blockedRewardDefinitionIds || [],
    })
    setPageTab('manage')
  }

  const cancelEditReward = () => {
    setEditingRewardId(null)
    setRewardForm({ ...emptyRewardForm, pointTypeId: pointTypes[0]?.id ?? '' })
  }

  const createCatalogReward = async (event) => {
    event.preventDefault()
    if (!rewardForm.title.trim() || !rewardForm.pointTypeId) return

    await run(async () => {
      const payload = {
          title: rewardForm.title.trim(),
          description: rewardForm.description.trim() || null,
          pointTypeId: Number(rewardForm.pointTypeId),
          priceAmount: Number(rewardForm.priceAmount || 0),
          cooldownSeconds: Number(rewardForm.cooldownSeconds ?? 30),
          memberTotalLimit: rewardForm.memberTotalLimit === '' ? null : Number(rewardForm.memberTotalLimit),
          memberDailyLimit: rewardForm.memberDailyLimit === '' ? null : Number(rewardForm.memberDailyLimit),
          memberWeeklyLimit: rewardForm.memberWeeklyLimit === '' ? null : Number(rewardForm.memberWeeklyLimit),
          workspaceTotalLimit: rewardForm.workspaceTotalLimit === '' ? null : Number(rewardForm.workspaceTotalLimit),
          workspaceDailyLimit: rewardForm.workspaceDailyLimit === '' ? null : Number(rewardForm.workspaceDailyLimit),
          workspaceWeeklyLimit: rewardForm.workspaceWeeklyLimit === '' ? null : Number(rewardForm.workspaceWeeklyLimit),
          extraRequestAllowed: rewardForm.extraRequestAllowed,
          minimumReputation:
            rewardForm.minimumReputation === ''
              ? null
              : Number(rewardForm.minimumReputation),
          requiresApproval: rewardForm.requiresApproval,
          acquisitionMode: rewardForm.acquisitionMode,
          rewardKind: rewardForm.rewardKind,
          defaultDurationMinutes:
            rewardForm.rewardKind === 'TIME_BASED' && rewardForm.defaultDurationMinutes !== ''
              ? Number(rewardForm.defaultDurationMinutes)
              : null,
          categoryIds: rewardForm.categoryIds,
          requirements: taskRequirements(rewardForm),
        }
      if (editingRewardId != null) {
        await updateReward(getAccessToken(), currentUser.workspaceId, editingRewardId, payload)
      } else {
        await createReward(getAccessToken(), currentUser.workspaceId, payload)
      }
      setEditingRewardId(null)

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
      durationMinutes:
        request.durationMinutes
        ?? reward?.defaultDurationMinutes
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
        durationMinutes:
          reviewForm.durationMinutes === ''
            ? null
            : Number(reviewForm.durationMinutes),
        requirements: taskRequirements(reviewForm),
      }
    )

    setReviewingRequestId(null)
  })

  const pointAmountLabel = (amount, id, code) => {
    const type = pointTypes.find(item => item.id === id || item.code === code)
    return currencyQuantity(amount, {
      code: type?.code || code,
      name: pointLabel(id, code),
      forms: currencyForms[type?.id || id] || [],
      language: i18n.language,
    })
  }

  const pointLabel = (pointTypeId, code) => {
    const type = pointTypes.find((item) => item.id === pointTypeId)
    return type?.name || code || ''
  }

  const requestStatusLabel = (status) => ({
    REQUESTED: t('rewardsUi.statusRequested'),
    APPROVED: t('rewardsUi.statusApproved'),
    WAITING_REQUIREMENTS: t('rewardsUi.statusWait'),
    READY_TO_PURCHASE: t('rewardsUi.statusReady'),
    PURCHASED: t('rewardsUi.statusPurchased'),
    REJECTED: t('rewardsUi.statusRejected'),
    CANCELLED: t('rewardsUi.statusCancelled'),
  }[status] || status)

  const acquisitionLabel = (mode) => ({
    DIRECT: t('rewardsUi.acquisitionDirect'),
    REQUEST: t('rewardsUi.acquisitionRequest'),
    DIRECT_OR_REQUEST: t('rewardsUi.acquisitionEither'),
  }[mode] || mode)

  if (loading) {
    return <div className="page-loading">{t('common.loading')}</div>
  }

  return (
    <div className="rewards-page">
      <div className="page-title">
        <div>
          <h1>{t('rewardsUi.title')}</h1>
          <p>{t('rewardsUi.description')}</p>
        </div>
      </div>

      {error && <div className="page-error" role="alert">{error}</div>}
      {notice && <div className="reward-obligation-banner" role="status">✓ {notice}</div>}
      <section className="reward-obligation-banner" aria-label={t("rewardsUi.yourBalance")}>
        <strong>{t('rewardsUi.yourPoints')}</strong>
        <div className="reward-tags">
          {balances.length ? balances.map((item) => (
            <span key={item.pointTypeId}>{pointAmountLabel(item.balance, item.pointTypeId, item.code)}</span>
          )) : <span>{t('rewardsUi.balanceNotLoaded')}</span>}
          {reputation !== null && <span>{t('rewardsUi.reputation')}: {reputation}</span>}
        </div>
      </section>

      {obligations.length > 0 && (
        <section className="reward-obligation-banner">
          <strong>{t('rewardsUi.outstanding')}</strong>
          {obligations.map((obligation) => (
            <div key={obligation.id} className="reward-obligation-row">
              <span>
                {obligation.memberName}: {obligation.title}
                {' · '}
                {t('rewardsUi.afterReward', { title: obligation.rewardTitle })}
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
          {t('rewardsUi.catalog')}
        </button>
        <button
          type="button"
          className={pageTab === 'requests' ? 'page-tab active' : 'page-tab'}
          onClick={() => setPageTab('requests')}
        >
          {t('rewardsUi.requests', { count: requests.length })}
        </button>
        {canManage && (
          <button
            type="button"
            className={pageTab === 'manage' ? 'page-tab active' : 'page-tab'}
            onClick={() => setPageTab('manage')}
          >
            {t('rewardsUi.settings')}
          </button>
        )}
      </div>

      {pageTab === 'catalog' && (
        <>
          <section className="rewards-grid">
            {rewards.length === 0 ? (
              <div className="empty-state">
                <strong>{t('rewardsUi.emptyRewards')}</strong>
                <p>{t('rewardsUi.emptyRewardsHelp')}</p>
              </div>
            ) : rewards.map((reward) => (
              <article className="reward-card" key={reward.id}>
                <div className="reward-card-top">
                  <div>
                    <h3>{reward.title}</h3>
                    {reward.description && <p>{reward.description}</p>}
                  </div>
                  <strong className="reward-price">
                    {pointAmountLabel(reward.priceAmount, reward.pointTypeId, reward.pointTypeCode)}
                  </strong>
                </div>

                {reward.requirements?.length > 0 && (
                  <div className="reward-requirements-summary">
                    <strong>{t('rewardsUi.conditions')}</strong>
                    {reward.requirements.map((requirement) => (
                      <div key={requirement.id}>
                        {requirement.phase === 'AFTER_REWARD' ? t('rewardsUi.after') : t('rewardsUi.before')}
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
                  {reward.cooldownSeconds > 0 && (
                    <span>{t('rewardsUi.cooldown', { seconds: reward.cooldownSeconds })}</span>
                  )}
                  {reward.minimumReputation != null && (
                    <span>{t('rewardsUi.reputationMin', { value: reward.minimumReputation })}</span>
                  )}
                  {reward.rewardKind === 'TIME_BASED' && (
                    <span>
                      {t('rewardsUi.timeBased')}
                      {reward.defaultDurationMinutes
                        ? ` · ${t('rewardsUi.minuteSuffix', { value: reward.defaultDurationMinutes })}`
                        : ` · ${t('rewardsUi.byAgreement')}`}
                    </span>
                  )}
                </div>

                <div className="reward-actions">
                  {canManage && (
                    <button type="button" className="secondary-button" disabled={processing}
                      onClick={() => beginEditReward(reward)}>
                      {t('rewardsUi.edit')}
                    </button>
                  )}
                  {myRequestForReward(reward.id)?.status === 'READY_TO_PURCHASE' && (
                    <button type="button" className="primary-button" disabled={pendingPurchases.includes(`reward:${reward.id}`)}
                      onClick={() => purchaseOnce(`reward:${reward.id}`, () => purchaseRewardRequest(
                        getAccessToken(), currentUser.workspaceId, myRequestForReward(reward.id).id
                      ), reward.title)}>
                      {t('rewardsUi.receive')}
                    </button>
                  )}
                  {myRequestForReward(reward.id) &&
                    myRequestForReward(reward.id).status !== 'READY_TO_PURCHASE' && (
                    <span className="reward-status">{requestStatusLabel(myRequestForReward(reward.id).status)}</span>
                  )}
                  {reward.acquisitionMode !== 'REQUEST' && !reward.requiresApproval && (
                    <button
                      type="button"
                      className="primary-button"
                      disabled={pendingPurchases.includes(`reward:${reward.id}`)}
                      onClick={() => purchaseOnce(`reward:${reward.id}`, () =>
                        purchaseReward(
                          getAccessToken(),
                          currentUser.workspaceId,
                          reward.id
                        ), reward.title
                      )}
                    >
                      {t('rewardsUi.receive')}
                    </button>
                  )}

                  {!myRequestForReward(reward.id) && (reward.acquisitionMode !== 'DIRECT' || reward.requiresApproval) && (
                    <button
                      type="button"
                      className="secondary-button"
                      disabled={processing}
                      onClick={() => requestCatalogReward(reward)}
                    >
                      {t('rewardsUi.ask')}
                    </button>
                  )}
                </div>
              </article>
            ))}
          </section>

          <section className="reward-request-box">
            <h2>{t('rewardsUi.customTitle')}</h2>
            <p>{t('rewardsUi.customExample')}</p>
            <form onSubmit={createCustomRequest} className="reward-request-form">
              <input
                value={customRequestTitle}
                maxLength={150}
                placeholder={t("rewardsUi.whatWant")}
                onChange={(event) => setCustomRequestTitle(event.target.value)}
              />
              <textarea
                value={customRequestDescription}
                rows={2}
                maxLength={1000}
                placeholder={t("rewardsUi.commentOptional")}
                onChange={(event) => setCustomRequestDescription(event.target.value)}
              />
              <button type="submit" className="primary-button" disabled={processing}>
                {t('rewardsUi.send')}
              </button>
            </form>
          </section>
        </>
      )}

      {pageTab === 'requests' && (
        <section className="reward-request-list">
          {requests.length === 0 ? (
            <div className="empty-state">{t('rewardsUi.requestsEmpty')}</div>
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
                  {t('rewardsUi.price')} <strong>{pointAmountLabel(request.priceAmount, request.pointTypeId, request.pointTypeCode)}</strong>
                  {request.durationMinutes
                    ? <span> · {t('rewardsUi.minuteSuffix', { value: request.durationMinutes })}</span>
                    : ''}
                </div>
              )}

              {request.requirements?.length > 0 && (
                <div className="reward-requirements-summary">
                  <strong>{t('rewardsUi.conditions')}</strong>
                  {request.requirements.map((requirement) => (
                    <div key={requirement.id}>
                      {requirement.phase === 'AFTER_REWARD' ? t('rewardsUi.after') : t('rewardsUi.before')}
                      {requirement.taskTitle || requirement.description || requirement.requirementType}
                      {requirement.phase === 'AFTER_REWARD' && requirement.blockingMode !== 'NONE'
                        ? ` · ${t('rewardsUi.blocking', { mode: requirement.blockingMode })}`
                        : ''}
                    </div>
                  ))}
                </div>
              )}

              {canManage && request.status === 'REQUESTED' && reviewingRequestId !== request.id && (
                <div className="reward-actions">
                  <button type="button" className="primary-button" onClick={() => beginReview(request)}>
                    {t('rewardsUi.reviewApprove')}
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
                    {t('rewardsUi.reject')}
                  </button>
                </div>
              )}

              {canManage && reviewingRequestId === request.id && (
                <div className="reward-review-panel">
                  <div className="reward-form-grid">
                    <label>
                      <span>{t('rewardsUi.pointType')}</span>
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
                      <span>{t('rewardsUi.priceLabel')}</span>
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
                      <span>{t('rewardsUi.minimumReputation')}</span>
                      <input
                        type="number"
                        min="0"
                        value={reviewForm.minimumReputation}
                        placeholder={t("rewardsUi.unrestricted")}
                        onChange={(event) => setReviewForm({
                          ...reviewForm,
                          minimumReputation: event.target.value,
                        })}
                      />
                    </label>
                    {rewards.find((item) => item.id === request.rewardDefinitionId)?.rewardKind === 'TIME_BASED' && (
                      <label>
                        <span>{t('rewardsUi.durationMinutes')}</span>
                        <input
                          type="number"
                          min="1"
                          value={reviewForm.durationMinutes}
                          placeholder={t("rewardsUi.any")}
                          onChange={(event) => setReviewForm({
                            ...reviewForm,
                            durationMinutes: event.target.value,
                          })}
                        />
                      </label>
                    )}
                  </div>

                  <TaskConditionPicker
                    title={t("rewardsUi.beforeTask")}
                    tasks={tasks}
                    selected={reviewForm.beforeTaskIds}
                    onToggle={(id) => setReviewForm({
                      ...reviewForm,
                      beforeTaskIds: toggleId(reviewForm.beforeTaskIds, id),
                    })}
                  />

                  <TaskConditionPicker
                    title={t("rewardsUi.afterTask")}
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
                      {t('rewardsUi.approve')}
                    </button>
                    <button type="button" className="secondary-button" onClick={() => setReviewingRequestId(null)}>
                      {t('rewardsUi.cancel')}
                    </button>
                  </div>
                </div>
              )}

              {request.requestedByMemberId === currentUser.memberId && request.status === 'WAITING_REQUIREMENTS' && (
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
                  {t('rewardsUi.checkConditions')}
                </button>
              )}

              {request.requestedByMemberId === currentUser.memberId && request.status === 'READY_TO_PURCHASE' && (
                <button
                  type="button"
                  className="primary-button"
                  disabled={pendingPurchases.includes(`reward:${request.rewardDefinitionId ?? 'request:' + request.id}`)}
                  onClick={() => purchaseOnce(`reward:${request.rewardDefinitionId ?? 'request:' + request.id}`, () =>
                    purchaseRewardRequest(
                      getAccessToken(),
                      currentUser.workspaceId,
                      request.id
                    ), request.title
                  )}
                >
                  {t('rewardsUi.claim')}
                </button>
              )}
            </article>
          ))}
        </section>
      )}

      {pageTab === 'manage' && canManage && (
        <div className="reward-manage-layout">
          <section className="reward-admin-panel">
            <h2>{t('rewardsUi.categories')}</h2>
            <div className="reward-tags">
              {categories.map((category) => (
                <span key={category.id}>{category.name}</span>
              ))}
            </div>
            <form onSubmit={createCategory} className="reward-inline-form">
              <input
                value={categoryName}
                placeholder={t("rewardsUi.newCategory")}
                maxLength={100}
                onChange={(event) => setCategoryName(event.target.value)}
              />
              <button type="submit" className="secondary-button" disabled={processing}>
                {t('rewardsUi.add')}
              </button>
            </form>
          </section>

          <section className="reward-admin-panel">
            <h2>{editingRewardId != null ? t('rewardsUi.editReward') : t('rewardsUi.newReward')}</h2>
            <form onSubmit={createCatalogReward} className="reward-editor">
              <label>
                <span>{t('rewardsUi.name')}</span>
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
                <span>{t('rewardsUi.descriptionLabel')}</span>
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
                  <span>{t('rewardsUi.pointType')}</span>
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
                  <span>{t('rewardsUi.priceLabel')}</span>
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
                  <span>{t('rewardsUi.cooldownLabel')}</span>
                  <input type="number" min="0" value={rewardForm.cooldownSeconds}
                    onChange={(event) => setRewardForm({
                      ...rewardForm, cooldownSeconds: event.target.value
                    })} />
                  <small>{t('rewardsUi.cooldownHelp')}</small>
                </label>

                <div className="reward-form-grid">
                  <label>
                    <span>{t('rewardsUi.memberTotal')}</span>
                    <input type="number" min="1" placeholder={t("rewardsUi.noLimits")}
                      value={rewardForm.memberTotalLimit}
                      onChange={(event) => setRewardForm({ ...rewardForm, memberTotalLimit: event.target.value })} />
                  </label>
                  <label>
                    <span>{t('rewardsUi.memberDaily')}</span>
                    <input type="number" min="1" placeholder={t("rewardsUi.noLimits")}
                      value={rewardForm.memberDailyLimit}
                      onChange={(event) => setRewardForm({ ...rewardForm, memberDailyLimit: event.target.value })} />
                  </label>
                  <label>
                    <span>{t('rewardsUi.memberWeekly')}</span>
                    <input type="number" min="1" placeholder={t("rewardsUi.noLimits")}
                      value={rewardForm.memberWeeklyLimit}
                      onChange={(event) => setRewardForm({ ...rewardForm, memberWeeklyLimit: event.target.value })} />
                  </label>
                  <label>
                    <span>{t('rewardsUi.workspaceTotal')}</span>
                    <input type="number" min="1" placeholder={t("rewardsUi.noLimits")}
                      value={rewardForm.workspaceTotalLimit}
                      onChange={(event) => setRewardForm({ ...rewardForm, workspaceTotalLimit: event.target.value })} />
                  </label>
                  <label>
                    <span>{t('rewardsUi.workspaceDaily')}</span>
                    <input type="number" min="1" placeholder={t("rewardsUi.noLimits")}
                      value={rewardForm.workspaceDailyLimit}
                      onChange={(event) => setRewardForm({ ...rewardForm, workspaceDailyLimit: event.target.value })} />
                  </label>
                  <label>
                    <span>{t('rewardsUi.workspaceWeekly')}</span>
                    <input type="number" min="1" placeholder={t("rewardsUi.noLimits")}
                      value={rewardForm.workspaceWeeklyLimit}
                      onChange={(event) => setRewardForm({ ...rewardForm, workspaceWeeklyLimit: event.target.value })} />
                  </label>
                </div>
                <label className="form-checkbox">
                  <input type="checkbox" checked={rewardForm.extraRequestAllowed}
                    onChange={event => setRewardForm({ ...rewardForm, extraRequestAllowed: event.target.checked })} />
                  <span>{t('rewardsUi.extraRequests')}</span>
                </label>

                <label>
                  <span>{t('rewardsUi.minimumReputation')}</span>
                  <input
                    type="number"
                    min="0"
                    value={rewardForm.minimumReputation}
                    placeholder={t("rewardsUi.unrestricted")}
                    onChange={(event) => setRewardForm({
                      ...rewardForm,
                      minimumReputation: event.target.value,
                    })}
                  />
                </label>

                <label>
                  <span>{t('rewardsUi.acquisition')}</span>
                  <select
                    value={rewardForm.acquisitionMode}
                    onChange={(event) => setRewardForm({
                      ...rewardForm,
                      acquisitionMode: event.target.value,
                    })}
                  >
                    <option value="DIRECT">{t('rewardsUi.acquisitionDirect')}</option>
                    <option value="REQUEST">{t('rewardsUi.acquisitionRequest')}</option>
                    <option value="DIRECT_OR_REQUEST">{t('rewardsUi.acquisitionEither')}</option>
                  </select>
                </label>

                <label>
                  <span>{t('rewardsUi.rewardKind')}</span>
                  <select
                    value={rewardForm.rewardKind}
                    onChange={(event) => setRewardForm({
                      ...rewardForm,
                      rewardKind: event.target.value,
                      defaultDurationMinutes:
                        event.target.value === 'TIME_BASED'
                          ? rewardForm.defaultDurationMinutes
                          : '',
                    })}
                  >
                    <option value="STANDARD">{t("rewardsUi.standard")}</option>
                    <option value="TIME_BASED">{t("rewardsUi.timeBased")}</option>
                  </select>
                </label>

                {rewardForm.rewardKind === 'TIME_BASED' && (
                  <label>
                    <span>{t('rewardsUi.defaultDuration')}</span>
                    <input
                      type="number"
                      min="1"
                      value={rewardForm.defaultDurationMinutes}
                      placeholder={t("rewardsUi.byAgreement")}
                      onChange={(event) => setRewardForm({
                        ...rewardForm,
                        defaultDurationMinutes: event.target.value,
                      })}
                    />
                  </label>
                )}
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
                <span>{t('rewardsUi.requiresApproval')}</span>
              </label>

              <div>
                <strong>{t('rewardsUi.categories')}</strong>
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
                {editingRewardId != null ? t('rewardsUi.saveChanges') : t('rewardsUi.createReward')}
              </button>
              {editingRewardId != null && (
                <button type="button" className="secondary-button" onClick={cancelEditReward} disabled={processing}>
                  {t('rewardsUi.cancelEditing')}
                </button>
              )}
            </form>
          </section>

          <section className="reward-admin-panel">
            <h2>{t('rewardsUi.recentPurchases')}</h2>
            {purchases.length === 0 ? (
              <p>{t('rewardsUi.noneYet')}</p>
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
  const { t } = useTranslation()
  return (
    <div className="reward-condition-picker">
      <strong>{title}</strong>
      <small>{t('rewardsUi.taskTodayNote')}</small>
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
  const { t } = useTranslation()
  return (
    <div className="reward-blocking-editor">
      <strong>{t('rewardsUi.blockingQuestion')}</strong>
      <select
        value={form.blockingMode}
        onChange={(event) => setForm({
          ...form,
          blockingMode: event.target.value,
        })}
      >
        <option value="NONE">{t('rewardsUi.blockNone')}</option>
        <option value="WARN_ONLY">{t('rewardsUi.blockWarn')}</option>
        <option value="ALL_REWARDS">{t('rewardsUi.blockAll')}</option>
        <option value="CATEGORIES">{t('rewardsUi.blockCategories')}</option>
        <option value="SPECIFIC_REWARDS">{t('rewardsUi.blockSpecific')}</option>
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

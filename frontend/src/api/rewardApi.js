import { apiRequest } from './client'

async function handleResponse(response, fallbackMessage) {
  if (!response.ok) {
    let message = response.status === 409
      ? 'Нагороду вже отримано. Зачекайте перед повторним отриманням.'
      : fallbackMessage
    try {
      const error = await response.json()
      if (error.message) message = error.message
    } catch {
      // Backend did not return JSON.
    }
    throw new Error(message)
  }
  return response.status === 204 ? null : response.json()
}

export async function getRewardAvailability(token, workspaceId, rewardId) {
  const response = await apiRequest(`/workspaces/${workspaceId}/rewards/${rewardId}/availability`, { token })
  return handleResponse(response, 'Failed to load reward availability')
}
export async function updateRewardAvailability(token, workspaceId, rewardId, schedule) {
  const response = await apiRequest(`/workspaces/${workspaceId}/rewards/${rewardId}/availability`,
    { token, method: 'PUT', body: schedule })
  return handleResponse(response, 'Failed to update reward availability')
}

export async function getRewards(token, workspaceId) {
  const response = await apiRequest(`/workspaces/${workspaceId}/rewards`, { token })
  return handleResponse(response, 'Failed to load rewards')
}

export async function createReward(token, workspaceId, reward) {
  const response = await apiRequest(`/workspaces/${workspaceId}/rewards`, {
    token,
    method: 'POST',
    body: reward,
  })
  return handleResponse(response, 'Failed to create reward')
}

export async function updateReward(token, workspaceId, rewardId, reward) {
  const response = await apiRequest(`/workspaces/${workspaceId}/rewards/${rewardId}`, {
    token, method: 'PUT', body: reward,
  })
  return handleResponse(response, 'Failed to update reward')
}

export async function getRewardCategories(token, workspaceId) {
  const response = await apiRequest(`/workspaces/${workspaceId}/rewards/categories`, { token })
  return handleResponse(response, 'Failed to load reward categories')
}

export async function createRewardCategory(token, workspaceId, category) {
  const response = await apiRequest(`/workspaces/${workspaceId}/rewards/categories`, {
    token,
    method: 'POST',
    body: category,
  })
  return handleResponse(response, 'Failed to create reward category')
}

export async function getRewardRequests(token, workspaceId) {
  const response = await apiRequest(`/workspaces/${workspaceId}/rewards/requests`, { token })
  return handleResponse(response, 'Failed to load reward requests')
}

export async function createRewardRequest(token, workspaceId, request) {
  const response = await apiRequest(`/workspaces/${workspaceId}/rewards/requests`, {
    token,
    method: 'POST',
    body: request,
  })
  return handleResponse(response, 'Failed to create reward request')
}

export async function approveRewardRequest(token, workspaceId, requestId, review) {
  const response = await apiRequest(
    `/workspaces/${workspaceId}/rewards/requests/${requestId}/approve`,
    { token, method: 'POST', body: review }
  )
  return handleResponse(response, 'Failed to approve reward request')
}

export async function rejectRewardRequest(token, workspaceId, requestId) {
  const response = await apiRequest(
    `/workspaces/${workspaceId}/rewards/requests/${requestId}/reject`,
    { token, method: 'POST' }
  )
  return handleResponse(response, 'Failed to reject reward request')
}

export async function refreshRewardRequest(token, workspaceId, requestId) {
  const response = await apiRequest(
    `/workspaces/${workspaceId}/rewards/requests/${requestId}/refresh`,
    { token, method: 'POST' }
  )
  return handleResponse(response, 'Failed to refresh reward request')
}

export async function purchaseReward(token, workspaceId, rewardId) {
  const response = await apiRequest(
    `/workspaces/${workspaceId}/rewards/${rewardId}/purchase`,
    { token, method: 'POST' }
  )
  return handleResponse(response, 'Failed to purchase reward')
}

export async function purchaseRewardRequest(token, workspaceId, requestId) {
  const response = await apiRequest(
    `/workspaces/${workspaceId}/rewards/requests/${requestId}/purchase`,
    { token, method: 'POST' }
  )
  return handleResponse(response, 'Failed to purchase reward request')
}

export async function getRewardPurchases(token, workspaceId) {
  const response = await apiRequest(`/workspaces/${workspaceId}/rewards/purchases`, { token })
  return handleResponse(response, 'Failed to load reward purchases')
}


export async function getOpenRewardObligations(token, workspaceId) {
  const response = await apiRequest(
    `/workspaces/${workspaceId}/rewards/obligations/open`,
    { token }
  )
  return handleResponse(response, 'Failed to load reward obligations')
}

// Purchase-only history v1. Backend enforces own-member access and manager ACL.
// Cursor is opaque; never derive it from client timestamps.
export async function getRewardPurchaseHistory(token, workspaceId, filters = {}) {
  const query = new URLSearchParams()
  for (const key of ['memberId', 'status', 'from', 'to', 'cursor', 'limit']) {
    const value = filters[key]
    if (value !== undefined && value !== null && value !== '') query.set(key, String(value))
  }
  const response = await apiRequest(
    `/workspaces/${workspaceId}/rewards/history?${query.toString()}`,
    { token, cache: 'no-store' }
  )
  return handleResponse(response, 'Failed to load reward purchase history')
}

 
// The server purchase owns both ledger debit and reward history. No automatic retry:
// existing purchase endpoint has no verified idempotent receipt for unknown responses.
export async function purchaseRewardForMember(token, workspaceId, rewardId, memberId) {
  const response = await apiRequest(
    `/workspaces/${workspaceId}/rewards/${rewardId}/purchase-for-member/${memberId}`,
    { token, method: 'POST' }
  )
  return handleResponse(response, 'Failed to purchase reward for member')
}

// Read-only PR57 receipt lookup; the caller must validate the returned body
// against the persisted original operation before claiming any purchase.
// 404, 401 and 409 are NOT permission to resubmit a purchase.
export async function getRewardPurchaseOperationReceipt(token, workspaceId, operationId) {
  const response = await apiRequest(
    `/workspaces/${workspaceId}/rewards/purchases/receipts/${operationId}`,
    { token, method: 'GET', cache: 'no-store' }
  )
  return {
    status: response.status,
    body: response.status === 200 ? await response.json() : null,
  }
}

import { apiRequest } from './client'

async function handleResponse(response, fallbackMessage) {
  if (!response.ok) {
    let message = fallbackMessage
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

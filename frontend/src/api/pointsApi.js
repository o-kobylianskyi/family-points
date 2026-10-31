import { apiRequest } from './client'

async function handleResponse(response, fallbackMessage) {
  if (!response.ok) {
    let message = fallbackMessage

    try {
      const error = await response.json()

      if (error.message) {
        message = error.message
      }
    } catch {
      // Backend не повернув JSON
    }

    throw new Error(message)
  }

  return response.json()
}

export async function getMemberBalance(
  token,
  workspaceId,
  memberId
) {
  const response = await apiRequest(
    `/workspaces/${workspaceId}/members/${memberId}/points/balance`,
    { token }
  )

  return handleResponse(
    response,
    `Failed to load member balance: ${memberId}`
  )
}

export async function getMemberPointHistory(
  token,
  workspaceId,
  memberId
) {
  const response = await apiRequest(
    `/workspaces/${workspaceId}/members/${memberId}/points/history`,
    { token }
  )

  return handleResponse(
    response,
    `Failed to load point history: ${memberId}`
  )
}

export async function getMemberBalances(
  token,
  workspaceId,
  memberId
) {
  const response = await apiRequest(
    `/workspaces/${workspaceId}/members/${memberId}/points/balances`,
    { token }
  )

  return handleResponse(
    response,
    `Failed to load point balances: ${memberId}`
  )
}

async function createPointOperation(
  token,
  workspaceId,
  memberId,
  operation,
  amount,
  description
) {
  const response = await apiRequest(
    `/workspaces/${workspaceId}/members/${memberId}/points/${operation}`,
    {
      token,
      method: 'POST',
      body: {
        amount,
        description,
      },
    }
  )

  return handleResponse(
    response,
    `Failed to perform point operation: ${operation}`
  )
}

export function earnPoints(
  token,
  workspaceId,
  memberId,
  amount,
  description
) {
  return createPointOperation(
    token,
    workspaceId,
    memberId,
    'earn',
    amount,
    description
  )
}

export function spendPoints(
  token,
  workspaceId,
  memberId,
  amount,
  description
) {
  return createPointOperation(
    token,
    workspaceId,
    memberId,
    'spend',
    amount,
    description
  )
}

export function penalizePoints(
  token,
  workspaceId,
  memberId,
  amount,
  description
) {
  return createPointOperation(
    token,
    workspaceId,
    memberId,
    'penalty',
    amount,
    description
  )
}
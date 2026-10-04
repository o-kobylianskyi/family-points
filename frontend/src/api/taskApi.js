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

export async function getMemberTasks(
  token,
  workspaceId,
  memberId,
  date
) {
  const response = await apiRequest(
    `/workspaces/${workspaceId}/tasks/members/${memberId}?date=${date}`,
    { token }
  )

  return handleResponse(
    response,
    `Failed to load tasks for member ${memberId}`
  )
}

async function changeTaskStatus(
  token,
  workspaceId,
  instanceId,
  action
) {
  const response = await apiRequest(
    `/workspaces/${workspaceId}/tasks/instances/${instanceId}/${action}`,
    {
      token,
      method: 'POST',
    }
  )

  return handleResponse(
    response,
    `Failed to ${action} task ${instanceId}`
  )
}

export function startTask(
  token,
  workspaceId,
  instanceId
) {
  return changeTaskStatus(
    token,
    workspaceId,
    instanceId,
    'start'
  )
}

export function completeTask(token, workspaceId, instanceId) {
  return changeTaskStatus(token, workspaceId, instanceId, 'complete')
}

export function pauseTask(token, workspaceId, instanceId) {
  return changeTaskStatus(token, workspaceId, instanceId, 'pause')
}

export function resumeTask(token, workspaceId, instanceId) {
  return changeTaskStatus(token, workspaceId, instanceId, 'resume')
}

export function cancelTask(token, workspaceId, instanceId) {
  return changeTaskStatus(token, workspaceId, instanceId, 'cancel')
}

export function releaseTask(token, workspaceId, instanceId) {
  return changeTaskStatus(token, workspaceId, instanceId, 'release')
}

export async function createTaskDefinition(
  token,
  workspaceId,
  task
) {
  const response = await apiRequest(
    `/workspaces/${workspaceId}/tasks/definitions`,
    {
      token,
      method: 'POST',
      body: task,
    }
  )

  return handleResponse(
    response,
    'Failed to create task'
  )
}

export async function getOpenTasks(
  token,
  workspaceId,
  date,
  memberId
) {
  const params = new URLSearchParams({ date })

  if (memberId) {
    params.set('memberId', memberId)
  }

  const response = await apiRequest(
    `/workspaces/${workspaceId}/tasks/open?${params.toString()}`,
    { token }
  )

  return handleResponse(
    response,
    'Failed to load open tasks'
  )
}

export async function claimTask(
  token,
  workspaceId,
  definitionId,
  date,
  memberId
) {
  const params = new URLSearchParams({
    date,
    memberId,
  })

  const response = await apiRequest(
    `/workspaces/${workspaceId}/tasks/definitions/${definitionId}/claim?${params.toString()}`,
    {
      token,
      method: 'POST',
    }
  )

  return handleResponse(
    response,
    'Failed to claim task'
  )
}

export async function delegateTask(
  token,
  workspaceId,
  instanceId,
  toMemberId,
  reason = null
) {
  const response = await apiRequest(
    `/workspaces/${workspaceId}/tasks/instances/${instanceId}/delegate`,
    {
      token,
      method: 'POST',
      body: {
        toMemberId,
        reason,
      },
    }
  )

  return handleResponse(
    response,
    'Failed to delegate task'
  )
}

export async function getTaskDefinitions(token, workspaceId) {
  const response = await apiRequest(`/workspaces/${workspaceId}/tasks/definitions`, { token })
  return handleResponse(response, 'Failed to load task definitions')
}

export async function getTaskDefinitionsByView(token, workspaceId, view) {
  const response = await apiRequest(`/workspaces/${workspaceId}/tasks/definitions/views/${view}`, { token })
  return handleResponse(response, `Failed to load ${view} tasks`)
}

export async function getTaskParticipants(token, workspaceId, definitionId) {
  const response = await apiRequest(`/workspaces/${workspaceId}/tasks/definitions/${definitionId}/participants`, { token })
  return handleResponse(response, 'Failed to load task participants')
}

export async function updateTaskDefinition(token, workspaceId, definitionId, task) {
  const response = await apiRequest(`/workspaces/${workspaceId}/tasks/definitions/${definitionId}`, {
    token,
    method: 'PUT',
    body: task,
  })
  return handleResponse(response, 'Failed to update task')
}

export async function setTaskDefinitionActive(token, workspaceId, definitionId, active) {
  const response = await apiRequest(`/workspaces/${workspaceId}/tasks/definitions/${definitionId}/active?active=${active}`, {
    token,
    method: 'PATCH',
  })
  return handleResponse(response, 'Failed to change task activity')
}


export async function getTaskInstanceHistory(token, workspaceId, instanceId) {
  const response = await apiRequest(
    `/workspaces/${workspaceId}/tasks/instances/${instanceId}/history`,
    { token }
  )
  return handleResponse(response, 'Failed to load task history')
}

export async function getTaskDefinitionHistory(token, workspaceId, definitionId) {
  const response = await apiRequest(
    `/workspaces/${workspaceId}/tasks/definitions/${definitionId}/history`,
    { token }
  )
  return handleResponse(response, 'Failed to load task history')
}


export async function getTaskDefinition(token, workspaceId, definitionId) {
  const response = await apiRequest(
    `/workspaces/${workspaceId}/tasks/definitions/${definitionId}`,
    { token }
  )
  return handleResponse(response, 'Failed to load task')
}


export async function getTaskSubtasks(token, workspaceId, definitionId) {
  const response = await apiRequest(
    `/workspaces/${workspaceId}/tasks/definitions/${definitionId}/subtasks`,
    { token }
  )
  return handleResponse(response, 'Failed to load subtasks')
}


export async function getTaskDefinitionInstance(token, workspaceId, definitionId, date) {
  const response = await apiRequest(
    `/workspaces/${workspaceId}/tasks/definitions/${definitionId}/instance?date=${encodeURIComponent(date)}`,
    { token }
  )
  if (response.status === 204) return null
  return handleResponse(response, 'Failed to load task execution')
}

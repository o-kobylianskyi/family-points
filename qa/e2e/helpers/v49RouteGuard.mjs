// Synthetic V49 browser tests must never dispatch an unexpected write after
// QA authentication. Register this BEFORE the narrowly mocked setup routes:
// Playwright resolves matching routes in reverse registration order.
const MUTATING_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE'])

export function isUnexpectedSyntheticWrite(method) {
  return MUTATING_METHODS.has(String(method).toUpperCase())
}

export async function registerV49UnexpectedWriteGuard(context, blocked) {
  if (!Array.isArray(blocked)) throw new Error('V49_GUARD_REQUIRES_AUDIT_ARRAY')
  await context.route('**/*', route => {
    const request = route.request()
    if (isUnexpectedSyntheticWrite(request.method())) {
      blocked.push(request.method() + ' ' + new URL(request.url()).pathname)
      return route.abort('blockedbyclient')
    }
    return route.continue()
  })
}

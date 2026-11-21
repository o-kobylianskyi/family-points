// One amount per member and operation. History is newest-first at the API boundary.
// Fall back to a useful amount instead of opening a blank form.
export const POINT_AMOUNT_PRESETS = Object.freeze(Array.from({length:20}, (_, index) => (index + 1) * 5))

export function initialPointOperationAmount(history, operation, memberId) {
  const wanted = {earn:'EARN',spend:'SPEND',penalty:'PENALTY'}[operation]
  if (!wanted || !Array.isArray(history)) return 5
  const matching = history.find(row =>
    row && row.type === wanted &&
    String(row.memberId) === String(memberId) &&
    Number.isSafeInteger(row.amount) && row.amount !== 0 &&
    Math.abs(row.amount) <= 100000
  )
  return matching ? Math.abs(matching.amount) : 5
}

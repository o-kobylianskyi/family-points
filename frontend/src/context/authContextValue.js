import { createContext, useContext } from 'react'

// The provider and hook intentionally live in separate modules so React Fast
// Refresh never mixes component exports with non-component exports.
export const AuthContext = createContext(null)

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside AuthProvider')
  return context
}

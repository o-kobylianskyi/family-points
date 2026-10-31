import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

function ProtectedRoute({ children }) {
  const { currentUser, loading, needsWorkspace } = useAuth()

  if (loading) {
    return (
      <div className="app-loading">
        Завантаження FamilyPoints...
      </div>
    )
  }

  if (needsWorkspace) return <Navigate to="/choose-family" replace />

  if (!currentUser) {
    return <Navigate to="/login" replace />
  }

  return children
}

export default ProtectedRoute
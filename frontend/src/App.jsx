import {
  BrowserRouter,
  Navigate,
  Route,
  Routes,
} from 'react-router-dom'

import { AuthProvider } from './context/AuthContext'
import ProtectedRoute from './components/ProtectedRoute'
import AppLayout from './components/AppLayout'

import LoginPage from './pages/LoginPage'
import ChooseFamilyPage from './pages/ChooseFamilyPage'
import RegisterAccountPage from './pages/RegisterAccountPage'
import EmailActionPage from './pages/EmailActionPage'
import AcceptMemberInvitePage from './pages/AcceptMemberInvitePage'
import RegisterFamilyPage from './pages/RegisterFamilyPage'
import DashboardPage from './pages/DashboardPage'
import MembersPage from './pages/MembersPage'
import GroupPage from './pages/GroupPage'
import TasksPage from './pages/TasksPage'
import TaskDetailsPage from './pages/TaskDetailsPage'
import PointsPage from './pages/PointsPage'
import RewardsPage from './pages/RewardsPage'
import SettingsPage from './pages/SettingsPage'

import './App.css'
import './Navigation.css'

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/register" element={<RegisterFamilyPage />} />
          <Route path="/choose-family" element={<ChooseFamilyPage />} />
          <Route path="/register-account" element={<RegisterAccountPage />} />
          <Route path="/accept-member-invite" element={<AcceptMemberInvitePage />} />
          <Route path="/verify-email" element={<EmailActionPage action="verify" />} />
          <Route path="/forgot-password" element={<EmailActionPage action="request" />} />
          <Route path="/reset-password" element={<EmailActionPage action="reset" />} />
          <Route
            path="/login"
            element={<LoginPage />}
          />

          <Route
            element={
              <ProtectedRoute>
                <AppLayout />
              </ProtectedRoute>
            }
          >
            <Route
              path="/"
              element={<DashboardPage />}
            />

            <Route
              path="/members"
              element={<MembersPage />}
            />

            <Route path="/workspace" element={<Navigate to="/members" replace />} />

            <Route path="/groups/:groupId" element={<GroupPage />} />

            <Route
              path="/tasks"
              element={<TasksPage />}
            />

            <Route
              path="/tasks/:definitionId"
              element={<TaskDetailsPage />}
            />

            <Route
              path="/points"
              element={<PointsPage />}
            />

            <Route
              path="/rewards"
              element={<RewardsPage />}
            />

            <Route
              path="/settings"
              element={<SettingsPage />}
            />
          </Route>

          <Route
            path="*"
            element={<Navigate to="/" replace />}
          />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}

export default App
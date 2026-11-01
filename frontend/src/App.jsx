import { lazy, Suspense } from 'react'
import {
  BrowserRouter,
  Navigate,
  Route,
  Routes,
} from 'react-router-dom'

import { AuthProvider } from './context/AuthContext'
import ProtectedRoute from './components/ProtectedRoute'
import AppLayout from './components/AppLayout'

const LoginPage = lazy(() => import('./pages/LoginPage'))
const DownloadAppPage = lazy(() => import('./pages/DownloadAppPage'))
const ChooseFamilyPage = lazy(() => import('./pages/ChooseFamilyPage'))
const RegisterAccountPage = lazy(() => import('./pages/RegisterAccountPage'))
const EmailActionPage = lazy(() => import('./pages/EmailActionPage'))
const AcceptMemberInvitePage = lazy(() => import('./pages/AcceptMemberInvitePage'))
const RegisterFamilyPage = lazy(() => import('./pages/RegisterFamilyPage'))
const DashboardPage = lazy(() => import('./pages/DashboardPage'))
const MembersPage = lazy(() => import('./pages/MembersPage'))
const GroupPage = lazy(() => import('./pages/GroupPage'))
const TasksPage = lazy(() => import('./pages/TasksPage'))
const TaskDetailsPage = lazy(() => import('./pages/TaskDetailsPage'))
const PointsPage = lazy(() => import('./pages/PointsPage'))
const RewardsPage = lazy(() => import('./pages/RewardsPage'))
const SettingsPage = lazy(() => import('./pages/SettingsPage'))
const TemplatesPage = lazy(() => import('./pages/TemplatesPage'))
const FamilySetupPage = lazy(() => import('./pages/FamilySetupPage'))
const CalendarPage = lazy(() => import('./pages/CalendarPage'))

import { ThemeInitializer } from './components/ThemePreference'

import './App.css'
import './Navigation.css'

function App() {
  return (
    <BrowserRouter>
      <ThemeInitializer />
      <AuthProvider>
        <Suspense fallback={<div className="app-loading" role="status">FamilyPoints…</div>}>
        <Routes>
          <Route path="/download" element={<DownloadAppPage />} />
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

            <Route path="/calendar" element={<CalendarPage />} />

            <Route
              path="/points"
              element={<PointsPage />}
            />

            <Route
              path="/rewards"
              element={<RewardsPage />}
            />

            <Route path="/templates" element={<TemplatesPage />} />

            <Route path="/family-setup" element={<FamilySetupPage />} />

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
        </Suspense>
      </AuthProvider>
    </BrowserRouter>
  )
}

export default App
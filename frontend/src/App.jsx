import React, { Suspense, lazy } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './hooks/useAuth'
import { ToastProvider } from './hooks/useToast'
import { ThemeProvider } from './hooks/useTheme'
import MainLayout from './components/layout/MainLayout'
import LoadingSpinner from './components/ui/LoadingSpinner'

// Lazy load pages for performance
const Login          = lazy(() => import('./pages/auth/Login'))
const Register       = lazy(() => import('./pages/auth/Register'))
const ForgotPassword = lazy(() => import('./pages/auth/ForgotPassword'))
const ResetPassword  = lazy(() => import('./pages/auth/ResetPassword'))

const Dashboard          = lazy(() => import('./pages/dashboard/Dashboard'))
const AdminDashboard     = lazy(() => import('./pages/admin/AdminDashboard'))
const AdminUsers         = lazy(() => import('./pages/admin/AdminUsers'))
const AdminProjects      = lazy(() => import('./pages/admin/AdminProjects'))
const AdminAuditLogs     = lazy(() => import('./pages/admin/AdminAuditLogs'))
const AdminSettings      = lazy(() => import('./pages/admin/AdminSettings'))
const Projects           = lazy(() => import('./pages/projects/Projects'))
const ProjectDetails     = lazy(() => import('./pages/projects/ProjectDetails'))
const SourceCodeScanner  = lazy(() => import('./pages/scanner/SourceCodeScanner'))
const Reports            = lazy(() => import('./pages/reports/Reports'))
const ReportDetail       = lazy(() => import('./pages/reports/ReportDetail'))
const ScanHistory        = lazy(() => import('./pages/history/ScanHistory'))
const SecurityChat       = lazy(() => import('./pages/chat/SecurityChat'))
const Settings           = lazy(() => import('./pages/settings/Settings'))
const Profile            = lazy(() => import('./pages/profile/Profile'))

const PageLoader = () => (
  <div className="h-full flex items-center justify-center min-h-[400px]">
    <LoadingSpinner size="lg" />
  </div>
)

export default function App() {
  return (
    <ThemeProvider>
      <BrowserRouter>
        <AuthProvider>
          <ToastProvider>
            <Suspense fallback={<PageLoader />}>
              <Routes>
                {/* Public Routes */}
                <Route path="/login"           element={<Login />} />
                <Route path="/register"        element={<Register />} />
                <Route path="/forgot-password" element={<ForgotPassword />} />
                <Route path="/reset-password"  element={<ResetPassword />} />

                {/* Protected Routes */}
                <Route path="/" element={<MainLayout />}>
                  <Route index element={<Dashboard />} />

                  {/* Primary workflow */}
                  <Route path="projects"       element={<Projects />} />
                  <Route path="projects/:id"   element={<ProjectDetails />} />
                  <Route path="scanner"        element={<SourceCodeScanner />} />
                  <Route path="reports"        element={<Reports />} />
                  <Route path="reports/:id"    element={<ReportDetail />} />
                  <Route path="history"        element={<ScanHistory />} />
                  <Route path="settings"       element={<Settings />} />
                  <Route path="profile"        element={<Profile />} />

                  {/* Admin Routes */}
                  <Route path="admin/dashboard" element={<AdminDashboard />} />
                  <Route path="admin/users" element={<AdminUsers />} />
                  <Route path="admin/projects" element={<AdminProjects />} />
                  <Route path="admin/audit-logs" element={<AdminAuditLogs />} />
                  <Route path="admin/settings" element={<AdminSettings />} />
                </Route>

                {/* AI Assistant — owns its full-screen layout with embedded Topnav */}
                <Route path="/chat" element={<SecurityChat />} />

                {/* Fallback */}
                <Route path="*" element={<Navigate to="/" replace />} />
              </Routes>
            </Suspense>
          </ToastProvider>
        </AuthProvider>
      </BrowserRouter>
    </ThemeProvider>
  )
}

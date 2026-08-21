import React, { Suspense, lazy, useState, useEffect, useRef } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './hooks/useAuth'
import { ToastProvider } from './hooks/useToast'
import { ThemeProvider } from './hooks/useTheme'
import MainLayout from './components/layout/MainLayout'
import LoadingSpinner from './components/ui/LoadingSpinner'

// Lazy load pages for performance
const LandingPage    = lazy(() => import('./pages/LandingPage'))
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

// Minimum display time for splash (ms) — long enough to actually see it
const SPLASH_MIN_MS = 3000

/**
 * AppSplash — shows branded splash for at least SPLASH_MIN_MS,
 * then fades out smoothly before unmounting.
 */
function AppSplash() {
  const [fading, setFading] = useState(false)
  const [gone,   setGone]   = useState(false)

  useEffect(() => {
    const t1 = setTimeout(() => setFading(true), SPLASH_MIN_MS)
    const t2 = setTimeout(() => setGone(true),   SPLASH_MIN_MS + 440)
    return () => { clearTimeout(t1); clearTimeout(t2) }
  }, [])

  if (gone) return null

  return (
    <div style={{
      position: 'fixed', inset: 0, zIndex: 9999,
      background: '#f9f9ff',
      display: 'flex', flexDirection: 'column',
      alignItems: 'center', justifyContent: 'center',
      fontFamily: "'Plus Jakarta Sans', 'Inter', sans-serif",
      opacity: fading ? 0 : 1,
      transition: 'opacity 0.42s cubic-bezier(0.4,0,0.2,1)',
      pointerEvents: fading ? 'none' : 'all',
    }}>
      <style>{`
        @keyframes cs-spin    { to { transform: rotate(360deg); } }
        @keyframes cs-shimmer { 0% { transform:translateX(-100%); } 100% { transform:translateX(400%); } }
        @keyframes cs-fadein  { from { opacity:0; transform:translateY(10px); } to { opacity:1; transform:translateY(0); } }
      `}</style>

      {/* Spinning ring + logo */}
      <div style={{ position:'relative', width:88, height:88, marginBottom:28 }}>
        <svg width="88" height="88" viewBox="0 0 88 88"
          style={{ position:'absolute', inset:0, animation:'cs-spin 1.4s linear infinite' }}>
          <defs>
            <linearGradient id="csG" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%"   stopColor="#0058be" stopOpacity="1" />
              <stop offset="60%"  stopColor="#2170e4" stopOpacity="0.6" />
              <stop offset="100%" stopColor="#2170e4" stopOpacity="0" />
            </linearGradient>
          </defs>
          <circle cx="44" cy="44" r="38" fill="none" stroke="url(#csG)"
            strokeWidth="4" strokeLinecap="round" strokeDasharray="180 60" />
        </svg>
        <div style={{ position:'absolute', inset:0, display:'flex', alignItems:'center', justifyContent:'center' }}>
          <img src="/logo.png" alt="CodeSentry" style={{ width:46, height:46, objectFit:'contain' }} />
        </div>
      </div>

      {/* Name */}
      <p style={{ margin:'0 0 6px', fontSize:20, fontWeight:700, color:'#111c2d', letterSpacing:'-0.02em',
                  animation:'cs-fadein 0.5s 0.15s ease both' }}>
        CodeSentry
      </p>
      {/* Tagline */}
      <p style={{ margin:'0 0 32px', fontSize:13, color:'#64748b', fontWeight:500,
                  animation:'cs-fadein 0.5s 0.3s ease both' }}>
        AI Security Analysis Platform
      </p>
      {/* Shimmer bar */}
      <div style={{ width:160, height:3, borderRadius:4, background:'#e2e8f0', overflow:'hidden',
                    animation:'cs-fadein 0.5s 0.4s ease both' }}>
        <div style={{ width:40, height:'100%',
                      background:'linear-gradient(90deg, transparent, #0058be, transparent)',
                      animation:'cs-shimmer 1.4s ease-in-out infinite' }} />
      </div>
    </div>
  )
}

// Lightweight inline loader — used inside already-rendered layouts
const PageLoader = () => (
  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '60vh' }}>
    <LoadingSpinner size="lg" />
  </div>
)

export default function App() {
  return (
    <ThemeProvider>
      <BrowserRouter>
        <AuthProvider>
          <ToastProvider>
            <Suspense fallback={<AppSplash />}>
              <Routes>
                {/* Public Routes */}
                <Route path="/"                element={<LandingPage />} />
                <Route path="/login"           element={<Login />} />
                <Route path="/register"        element={<Register />} />
                <Route path="/forgot-password" element={<ForgotPassword />} />
                <Route path="/reset-password"  element={<ResetPassword />} />

                {/* Protected Routes */}
                <Route path="/" element={<MainLayout />}>
                  <Route path="dashboard" element={<Dashboard />} />

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

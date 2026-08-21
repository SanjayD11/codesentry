import React, { Suspense, lazy, useState, useEffect } from 'react'
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

// ─── Splash Screen ─────────────────────────────────────────────────────────────
// Lives OUTSIDE <Suspense> so React cannot prematurely unmount it when lazy
// chunks finish loading. The 3.2 s timer always runs to completion.
const SPLASH_MS = 3200

function SplashOverlay() {
  const [fading, setFading] = useState(false)
  const [gone,   setGone]   = useState(false)

  useEffect(() => {
    const t1 = setTimeout(() => setFading(true), SPLASH_MS)
    const t2 = setTimeout(() => setGone(true),   SPLASH_MS + 520)
    return () => { clearTimeout(t1); clearTimeout(t2) }
  }, [])

  if (gone) return null

  return (
    <div style={{
      position:'fixed', inset:0, zIndex:9999,
      background:'#f8faff',
      display:'flex', flexDirection:'column',
      alignItems:'center', justifyContent:'center',
      fontFamily:"'Plus Jakarta Sans','Inter',sans-serif",
      opacity: fading ? 0 : 1,
      transition:'opacity 0.52s cubic-bezier(0.4,0,0.2,1)',
      pointerEvents: fading ? 'none' : 'all',
    }}>
      <style>{`
        @keyframes sp-spin    { to { transform:rotate(360deg); } }
        @keyframes sp-shimmer { 0%{transform:translateX(-100%);} 100%{transform:translateX(400%);} }
        @keyframes sp-fadein  { from{opacity:0;transform:translateY(12px);} to{opacity:1;transform:translateY(0);} }
        @keyframes sp-pulse   { 0%,100%{opacity:1;} 50%{opacity:0.5;} }
      `}</style>

      {/* Ring + logo */}
      <div style={{ position:'relative', width:100, height:100, marginBottom:32 }}>
        <div style={{ position:'absolute', inset:'-10px', borderRadius:'50%',
          background:'radial-gradient(circle, rgba(37,99,235,0.11) 0%, transparent 70%)' }} />
        <svg width="100" height="100" viewBox="0 0 100 100"
          style={{ position:'absolute', inset:0, animation:'sp-spin 1.6s linear infinite' }}>
          <defs>
            <linearGradient id="spRing" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%"   stopColor="#2563eb" stopOpacity="1" />
              <stop offset="55%"  stopColor="#60a5fa" stopOpacity="0.65" />
              <stop offset="100%" stopColor="#60a5fa" stopOpacity="0" />
            </linearGradient>
          </defs>
          <circle cx="50" cy="50" r="44" fill="none" stroke="url(#spRing)"
            strokeWidth="3.5" strokeLinecap="round" strokeDasharray="210 70" />
        </svg>
        <div style={{ position:'absolute', inset:0, display:'flex', alignItems:'center', justifyContent:'center' }}>
          <img src="/logo.png" alt="CodeSentry"
            style={{ width:52, height:52, objectFit:'contain', animation:'sp-pulse 2.2s ease-in-out infinite' }} />
        </div>
      </div>

      <p style={{ margin:'0 0 6px', fontSize:21, fontWeight:700, color:'#0f172a',
                  letterSpacing:'-0.025em', animation:'sp-fadein 0.5s 0.2s ease both' }}>
        CodeSentry
      </p>
      <p style={{ margin:'0 0 36px', fontSize:13, color:'#64748b', fontWeight:500,
                  animation:'sp-fadein 0.5s 0.36s ease both' }}>
        AI Security Analysis Platform
      </p>
      <div style={{ width:176, height:3, borderRadius:4, background:'#e2e8f0', overflow:'hidden',
                    animation:'sp-fadein 0.5s 0.48s ease both' }}>
        <div style={{ width:44, height:'100%',
          background:'linear-gradient(90deg,transparent,#2563eb,transparent)',
          animation:'sp-shimmer 1.5s ease-in-out infinite' }} />
      </div>
    </div>
  )
}

// Silent transparent fallback — splash already covers the screen
const SuspenseFallback = () => (
  <div style={{ position:'fixed', inset:0, background:'#f8faff', zIndex:9998 }} />
)

// Lightweight inline loader for already-rendered layouts
const PageLoader = () => (
  <div style={{ display:'flex', alignItems:'center', justifyContent:'center', minHeight:'60vh' }}>
    <LoadingSpinner size="lg" />
  </div>
)

export default function App() {
  return (
    <ThemeProvider>
      {/* Splash is OUTSIDE BrowserRouter & Suspense — React cannot prematurely unmount it */}
      <SplashOverlay />

      <BrowserRouter>
        <AuthProvider>
          <ToastProvider>
            <Suspense fallback={<SuspenseFallback />}>
              <Routes>
                {/* Public */}
                <Route path="/"                element={<LandingPage />} />
                <Route path="/login"           element={<Login />} />
                <Route path="/register"        element={<Register />} />
                <Route path="/forgot-password" element={<ForgotPassword />} />
                <Route path="/reset-password"  element={<ResetPassword />} />

                {/* Protected (inside MainLayout) */}
                <Route path="/" element={<MainLayout />}>
                  <Route path="dashboard"    element={<Dashboard />} />
                  <Route path="projects"     element={<Projects />} />
                  <Route path="projects/:id" element={<ProjectDetails />} />
                  <Route path="scanner"      element={<SourceCodeScanner />} />
                  <Route path="reports"      element={<Reports />} />
                  <Route path="reports/:id"  element={<ReportDetail />} />
                  <Route path="history"      element={<ScanHistory />} />
                  <Route path="settings"     element={<Settings />} />
                  <Route path="profile"      element={<Profile />} />
                  <Route path="admin/dashboard"  element={<AdminDashboard />} />
                  <Route path="admin/users"      element={<AdminUsers />} />
                  <Route path="admin/projects"   element={<AdminProjects />} />
                  <Route path="admin/audit-logs" element={<AdminAuditLogs />} />
                  <Route path="admin/settings"   element={<AdminSettings />} />
                </Route>

                {/* AI Assistant — full-screen layout */}
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

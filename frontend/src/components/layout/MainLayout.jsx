import { useEffect, Suspense } from 'react'
import { Outlet, Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'
import { useToast } from '../../hooks/useToast'
import Topnav from './Topnav'
import PageContainer from './PageContainer'

export default function MainLayout() {
  const { isAuthenticated } = useAuth()
  const { pathname } = useLocation()
  const addToast = useToast()

  useEffect(() => {
    const handleSessionExpired = () => {
      addToast('Your session has expired. Please log in again.', 'warning', 3000)
    }
    window.addEventListener('session-expired', handleSessionExpired)
    return () => window.removeEventListener('session-expired', handleSessionExpired)
  }, [addToast])

  if (!isAuthenticated) {
    return <Navigate to="/" replace />
  }

  return (
    <div style={{ minHeight: '100dvh', background: 'var(--snt-surface-2)', display: 'flex', flexDirection: 'column' }}>
      <Topnav />
      <main style={{ flex: 1, width: '100%', minWidth: 0 }}>
        <Suspense fallback={
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '400px' }}>
            <div className="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin"></div>
          </div>
        }>
          <div key={pathname} className="section-enter">
            <PageContainer>
              <Outlet />
            </PageContainer>
          </div>
        </Suspense>
      </main>
    </div>
  )
}

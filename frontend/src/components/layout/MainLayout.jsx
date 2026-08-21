import { useEffect } from 'react'
import { Outlet, Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'
import { useToast } from '../../hooks/useToast'
import Topnav from './Topnav'
import BottomNav from './BottomNav'
import PageContainer from './PageContainer'
import LoadingSpinner from '../ui/LoadingSpinner'

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
    return <Navigate to="/login" replace />
  }

  return (
    <div style={{ minHeight: '100dvh', background: 'var(--snt-surface-2, #f9f9ff)', display: 'flex', flexDirection: 'column' }}>
      <Topnav />
      <main style={{ flex: 1, width: '100%', minWidth: 0 }}>
        {/* On mobile/tablet, add bottom padding so content clears the bottom nav bar */}
        <div style={{ paddingBottom: 84 }} className="main-content-pad">
          <style>{`
            @media (min-width: 1024px) {
              .main-content-pad { padding-bottom: 0 !important; }
            }
          `}</style>
          <div key={pathname} className="section-enter">
            <PageContainer>
              <Outlet />
            </PageContainer>
          </div>
        </div>
      </main>
      <BottomNav />
    </div>
  )
}

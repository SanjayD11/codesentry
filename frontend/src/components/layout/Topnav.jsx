import { useState, useEffect } from 'react'
import { createPortal } from 'react-dom'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'

export default function Topnav() {
  const { user, logout } = useAuth()
  const { pathname } = useLocation()
  const navigate = useNavigate()
  const [profileOpen, setProfileOpen] = useState(false)
  const [mobileDrawerOpen, setMobileDrawerOpen] = useState(false)
  const [dropdownRef, setDropdownRef] = useState(null)

  let NAV_ITEMS = []
  if (user?.role === 'ADMIN') {
    NAV_ITEMS = [
      { name: 'Dashboard',   path: '/admin/dashboard',   icon: 'dashboard' },
      { name: 'Users',       path: '/admin/users',       icon: 'group' },
      { name: 'Projects',    path: '/admin/projects',    icon: 'folder_open' },
      { name: 'Audit Logs',  path: '/admin/audit-logs',  icon: 'policy' },
      { name: 'Settings',    path: '/admin/settings',    icon: 'tune' },
    ]
  } else {
    NAV_ITEMS = [
      { name: 'Dashboard',           path: '/',         icon: 'dashboard' },
      { name: 'Projects',            path: '/projects', icon: 'folder' },
      { name: 'Source Code Scanner', path: '/scanner',  icon: 'code' },
      { name: 'Security Reports',    path: '/reports',  icon: 'folder_special' },
      { name: 'Scan History',        path: '/history',  icon: 'manage_history' },
      { name: 'AI Assistant', path: '/chat', icon: 'ai-bot' },
      { name: 'Settings',            path: '/settings', icon: 'settings' },
    ]
  }

  const isActive = (path) => path === '/' ? pathname === '/' : pathname.startsWith(path)
  const displayName = user ? `${user.firstName ?? ''} ${user.lastName ?? ''}`.trim() || user.email : ''
  const initials = user?.firstName ? `${user.firstName[0]}${user.lastName?.[0] ?? ''}`.toUpperCase() : '?'

  useEffect(() => {
    const handler = (e) => {
      if (dropdownRef && !dropdownRef.contains(e.target)) setProfileOpen(false)
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [dropdownRef])

  // Close mobile drawer on route change
  useEffect(() => {
    setMobileDrawerOpen(false)
  }, [pathname])

  const handleLogout = () => { setProfileOpen(false); setMobileDrawerOpen(false); logout() }

  return (
    <header style={{
      position: 'sticky', top: 0, zIndex: 50,
      background: '#ffffff',
      borderBottom: '1px solid #e2e8f0',
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      padding: '0 16px', height: '56px',
      fontFamily: "'Manrope', sans-serif",
    }}>
      <style>{`
        @media (min-width: 1024px) {
          .tn-desktop-nav { display: flex !important; }
          .tn-mobile-btn  { display: none !important; }
        }
        @media (min-width: 768px) and (max-width: 1023px) {
          .tn-desktop-nav { display: none !important; }
          .tn-mobile-btn  { display: flex !important; }
        }
        @media (max-width: 767px) {
          /* Bottom nav handles mobile navigation — hide hamburger */
          .tn-desktop-nav { display: none !important; }
          .tn-mobile-btn  { display: none !important; }
        }
      `}</style>

      {/* Brand & Mobile Hamburger Toggle */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <button
          className="tn-mobile-btn"
          onClick={() => setMobileDrawerOpen(true)}
          aria-label="Open Navigation Menu"
          style={{
            background: 'none', border: '1px solid #cbd5e1', borderRadius: 8,
            padding: '6px 8px', cursor: 'pointer', color: '#0f172a',
            display: 'flex', alignItems: 'center', justifyContent: 'center'
          }}
        >
          <span className="material-symbols-outlined" style={{ fontSize: 20 }}>menu</span>
        </button>

        <a href="/landing.html" style={{ display: 'flex', alignItems: 'center', textDecoration: 'none', flexShrink: 0, cursor: 'pointer' }}>
          <img src="/logo.png" alt="CodeSentry" style={{ height: '34px', width: 'auto' }} />
        </a>
      </div>

      {/* Nav Items (Desktop) */}
      <nav className="tn-desktop-nav" style={{ alignItems: 'center', gap: 4 }}>
        {NAV_ITEMS.map(item => {
          const active = isActive(item.path)
          return (
            <Link
              key={item.path}
              to={item.path}
              style={{
                display: 'flex', alignItems: 'center', gap: 6,
                padding: '0 12px', height: 34, borderRadius: 8,
                fontSize: 13.5, fontWeight: active ? 600 : 500,
                color: active ? '#0058be' : '#424754',
                background: active ? '#f0f3ff' : 'transparent',
                border: active ? '1px solid #c2c6d6' : '1px solid transparent',
                textDecoration: 'none',
                whiteSpace: 'nowrap',
                cursor: 'pointer',
                transition: 'all 0.15s cubic-bezier(0.4, 0, 0.2, 1)',
              }}
              onMouseEnter={e => {
                if (!active) {
                  e.currentTarget.style.color = '#111c2d'
                  e.currentTarget.style.background = '#f8fafc'
                  e.currentTarget.style.border = '1px solid #e2e8f0'
                }
              }}
              onMouseLeave={e => {
                if (!active) {
                  e.currentTarget.style.color = '#424754'
                  e.currentTarget.style.background = 'transparent'
                  e.currentTarget.style.border = '1px solid transparent'
                }
              }}
            >
              {item.icon === 'ai-bot' ? (
                <img src="/ai-bot.png" alt="AI" style={{ width: 16, height: 16, borderRadius: 4, objectFit: 'cover' }} />
              ) : (
                <span className="material-symbols-outlined" style={{
                  fontSize: 16,
                  fontVariationSettings: active ? "'FILL' 1" : "'FILL' 0",
                  transition: 'font-variation-settings 0.18s ease',
                }}>{item.icon}</span>
              )}
              {item.name}
            </Link>
          )
        })}
      </nav>

      {/* Profile */}
      <div ref={setDropdownRef} style={{ position: 'relative', flexShrink: 0 }}>
        <button
          onClick={() => setProfileOpen(p => !p)}
          style={{
            display: 'flex', alignItems: 'center', gap: 8,
            background: 'none', border: 'none', cursor: 'pointer',
            padding: '4px 6px', borderRadius: 8,
            transition: 'background 0.15s ease',
          }}
          onMouseEnter={e => e.currentTarget.style.background = '#f0f3ff'}
          onMouseLeave={e => e.currentTarget.style.background = 'none'}
        >
          <div style={{
            width: 30, height: 30, borderRadius: '50%',
            background: '#d0e1fb', color: '#0058be',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontSize: 12.5, fontWeight: 700, fontFamily: "'Plus Jakarta Sans', 'Manrope', sans-serif",
            border: '1.5px solid #c2c6d6',
          }}>
            {initials}
          </div>
          <span style={{ fontSize: 13.5, fontWeight: 600, color: '#111c2d', display: 'none sm:inline' }}>{displayName}</span>
          <span className="material-symbols-outlined" style={{
            fontSize: 16, color: '#424754',
            transform: profileOpen ? 'rotate(180deg)' : 'rotate(0deg)',
            transition: 'transform 0.18s ease',
          }}>expand_more</span>
        </button>

        {/* Dropdown */}
        {profileOpen && (
          <div style={{
            position: 'absolute', top: 'calc(100% + 8px)', right: 0,
            background: '#ffffff',
            border: '1px solid rgba(226,232,240,0.9)',
            borderRadius: 12,
            boxShadow: '0 10px 25px -5px rgba(0,0,0,0.08)',
            minWidth: 200, overflow: 'hidden', zIndex: 100,
            animation: 'dropdownIn 0.15s cubic-bezier(0.16,1,0.3,1)',
          }}>
            <style>{`@keyframes dropdownIn { from { opacity: 0; transform: translateY(-6px) scale(0.98); } to { opacity: 1; transform: translateY(0) scale(1); } }`}</style>
            <div style={{ padding: '12px 16px', borderBottom: '1px solid #f0f3ff' }}>
              <p style={{ margin: 0, fontSize: 13, fontWeight: 600, color: '#111c2d' }}>{displayName}</p>
              <p style={{ margin: '2px 0 0', fontSize: 12, color: '#424754' }}>{user?.email}</p>
            </div>
            <div style={{ padding: '6px 0' }}>
              <DropdownItem icon="person"  label="Profile" onClick={() => { navigate('/profile'); setProfileOpen(false) }} />
              <DropdownItem icon="settings" label="Settings" onClick={() => { navigate('/settings'); setProfileOpen(false) }} />
              {user?.role === 'ADMIN' && (
                <DropdownItem icon="admin_panel_settings" label="Admin Dashboard" onClick={() => { navigate('/admin/dashboard'); setProfileOpen(false) }} />
              )}
              <div style={{ borderTop: '1px solid #f0f3ff', margin: '4px 0' }} />
              <DropdownItem icon="logout" label="Sign out" onClick={handleLogout} danger />
            </div>
          </div>
        )}
      </div>

      {/* Mobile Slide-Over Navigation Drawer (Mounted via React Portal) */}
      {mobileDrawerOpen && createPortal(
        <div style={{ position: 'fixed', inset: 0, zIndex: 9999, display: 'flex' }}>
          {/* Backdrop Overlay */}
          <div
            style={{ position: 'absolute', inset: 0, background: 'rgba(15,23,42,0.45)', animation: 'fadeIn 0.2s ease' }}
            onClick={() => setMobileDrawerOpen(false)}
          />

          {/* Drawer Panel */}
          <div style={{
            position: 'relative', width: 280, maxWidth: '85vw', background: '#ffffff', height: '100%',
            display: 'flex', flexDirection: 'column', boxShadow: '4px 0 24px rgba(15,23,42,0.18)',
            zIndex: 10000, animation: 'slideInLeft 0.22s cubic-bezier(0.16,1,0.3,1)'
          }}>
            <style>{`
              @keyframes slideInLeft { from { transform: translateX(-100%); } to { transform: translateX(0); } }
              @keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
            `}</style>

            {/* Header */}
            <div style={{ padding: '16px 20px', borderBottom: '1px solid #e2e8f0', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <img src="/logo.png" alt="CodeSentry" style={{ height: '32px', width: 'auto' }} />
              <button
                onClick={() => setMobileDrawerOpen(false)}
                style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#64748b', display: 'flex', padding: 4 }}
              >
                <span className="material-symbols-outlined" style={{ fontSize: 20 }}>close</span>
              </button>
            </div>

            {/* Navigation Items List */}
            <nav style={{ flex: 1, overflowY: 'auto', padding: '16px 12px', display: 'flex', flexDirection: 'column', gap: 6 }}>
              {NAV_ITEMS.map(item => {
                const active = isActive(item.path)
                return (
                  <Link
                    key={item.path}
                    to={item.path}
                    onClick={() => setMobileDrawerOpen(false)}
                    style={{
                      display: 'flex', alignItems: 'center', gap: 12,
                      padding: '10px 16px', borderRadius: 10,
                      fontSize: 14, fontWeight: active ? 600 : 500,
                      color: active ? '#0058be' : '#334155',
                      background: active ? '#eff6ff' : 'transparent',
                      border: active ? '1px solid #bfdbfe' : '1px solid transparent',
                      textDecoration: 'none',
                    }}
                  >
                    <span className="material-symbols-outlined" style={{ fontSize: 18, color: active ? '#0058be' : '#64748b' }}>{item.icon}</span>
                    {item.name}
                  </Link>
                )
              })}
            </nav>

            {/* User Profile Footer */}
            <div style={{ padding: '16px 20px', borderTop: '1px solid #e2e8f0', background: '#f8fafc' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 12 }}>
                <div style={{
                  width: 34, height: 34, borderRadius: '50%', background: '#d0e1fb', color: '#0058be',
                  display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 13, fontWeight: 700
                }}>
                  {initials}
                </div>
                <div style={{ minWidth: 0, flex: 1 }}>
                  <p style={{ margin: 0, fontSize: 13.5, fontWeight: 600, color: '#0f172a', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{displayName}</p>
                  <p style={{ margin: 0, fontSize: 11.5, color: '#64748b', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{user?.email}</p>
                </div>
              </div>
              <button
                onClick={handleLogout}
                style={{
                  width: '100%', height: 36, background: '#fef2f2', border: '1px solid #fecaca',
                  borderRadius: 8, color: '#dc2626', fontSize: 13, fontWeight: 600,
                  cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6
                }}
              >
                <span className="material-symbols-outlined" style={{ fontSize: 16 }}>logout</span>
                Sign out
              </button>
            </div>
          </div>
        </div>,
        document.body
      )}
    </header>
  )
}

function DropdownItem({ icon, label, onClick, danger }) {
  const [hovered, setHovered] = useState(false)
  return (
    <button
      onClick={onClick}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      style={{
        width: '100%', display: 'flex', alignItems: 'center', gap: 10,
        padding: '8px 16px',
        background: hovered ? (danger ? '#ffdad6' : '#f0f3ff') : 'transparent',
        border: 'none', cursor: 'pointer', textAlign: 'left',
        color: danger ? '#ba1a1a' : '#111c2d',
        fontSize: 13.5, fontWeight: 500,
        transition: 'background 0.12s ease',
        fontFamily: "'Manrope', sans-serif",
      }}
    >
      <span className="material-symbols-outlined" style={{ fontSize: 16, opacity: 0.8 }}>{icon}</span>
      {label}
    </button>
  )
}

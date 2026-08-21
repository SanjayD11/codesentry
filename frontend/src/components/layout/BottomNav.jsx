import { useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'
import { createPortal } from 'react-dom'

const USER_NAV = [
  { name: 'Dashboard', path: '/dashboard', icon: 'home' },
  { name: 'Scans',     path: '/scanner',   icon: 'search' },
  { name: 'Reports',   path: '/reports',   icon: 'description' },
  { name: 'Settings',  path: '/settings',  icon: 'settings' },
]

const ADMIN_NAV = [
  { name: 'Dashboard', path: '/admin/dashboard',   icon: 'home' },
  { name: 'Users',     path: '/admin/users',       icon: 'group' },
  { name: 'Logs',      path: '/admin/audit-logs',  icon: 'policy' },
  { name: 'Settings',  path: '/admin/settings',    icon: 'tune' },
]

export default function BottomNav() {
  const { user } = useAuth()
  const { pathname } = useLocation()
  const [scanOpen, setScanOpen] = useState(false)

  const isAdmin = user?.role === 'ADMIN'
  const items = isAdmin ? ADMIN_NAV : USER_NAV
  const showFab = !isAdmin

  const leftItems  = items.slice(0, 2)
  const rightItems = items.slice(2)

  const isActive = (path) =>
    path === '/dashboard'       ? pathname === '/dashboard'
    : path === '/admin/dashboard' ? pathname === '/admin/dashboard'
    : pathname.startsWith(path)

  return (
    <>
      <style>{`
        .bottom-nav-bar { display: none !important; }
        @media (max-width: 1023px) {
          .bottom-nav-bar { display: flex !important; }
        }
        .bnav-item {
          flex: 1; display: flex; flex-direction: column; align-items: center;
          justify-content: center; gap: 3px; padding: 6px 0 8px;
          background: none; border: none; cursor: pointer; text-decoration: none;
          transition: color 0.15s ease; -webkit-tap-highlight-color: transparent;
        }
        .bnav-icon {
          width: 42px; height: 28px; border-radius: 14px;
          display: flex; align-items: center; justify-content: center;
          transition: background 0.2s ease;
        }
        .bnav-item.active .bnav-icon { background: #dde8ff; }
        .bnav-label {
          font-size: 10px; font-weight: 600; letter-spacing: 0.01em;
          font-family: 'Manrope', sans-serif;
          transition: color 0.15s ease;
        }
        .bnav-item.active .bnav-label { color: #0058be; }
        .bnav-item:not(.active) .bnav-label { color: #64748b; }
        .bnav-fab-wrapper { flex: 0 0 64px; display: flex; align-items: center; justify-content: center; }
        .bnav-fab {
          width: 52px; height: 52px; border-radius: 50%;
          background: linear-gradient(135deg, #0058be 0%, #2170e4 100%);
          border: none; cursor: pointer;
          display: flex; align-items: center; justify-content: center;
          box-shadow: 0 4px 16px rgba(0,88,190,0.35);
          transition: transform 0.18s cubic-bezier(0.34,1.56,0.64,1), box-shadow 0.18s ease;
          margin-bottom: 14px; -webkit-tap-highlight-color: transparent;
        }
        .bnav-fab:hover, .bnav-fab:active { transform: scale(1.1); box-shadow: 0 6px 22px rgba(0,88,190,0.45); }
        .bnav-fab .fab-icon { color: #fff; font-size: 24px; transition: transform 0.22s cubic-bezier(0.34,1.56,0.64,1); display: block; }
        .bnav-fab.open .fab-icon { transform: rotate(45deg); }
        .fab-overlay { position: fixed; inset: 0; z-index: 9998; background: rgba(15,23,42,0.4); animation: fo 0.15s ease; }
        @keyframes fo { from { opacity: 0; } to { opacity: 1; } }
        .fab-panel {
          position: fixed; bottom: 76px; left: 50%; transform: translateX(-50%);
          z-index: 9999; display: flex; flex-direction: column; gap: 10px; align-items: stretch;
          width: calc(100vw - 48px); max-width: 320px;
          animation: fp 0.22s cubic-bezier(0.16,1,0.3,1);
        }
        @keyframes fp { from { opacity: 0; transform: translateX(-50%) translateY(20px); } to { opacity: 1; transform: translateX(-50%) translateY(0); } }
        .fab-item {
          display: flex; align-items: center; gap: 14px; padding: 13px 16px;
          background: #fff; border-radius: 14px; text-decoration: none;
          box-shadow: 0 4px 20px rgba(0,0,0,0.12);
          border: 1px solid rgba(194,198,214,0.4);
          color: #111c2d; font-family: 'Manrope', sans-serif; font-size: 14px; font-weight: 600;
          transition: background 0.12s ease; -webkit-tap-highlight-color: transparent;
        }
        .fab-item:active { background: #f0f3ff; }
        .fab-item-icon {
          width: 38px; height: 38px; border-radius: 10px;
          display: flex; align-items: center; justify-content: center; flex-shrink: 0;
        }
      `}</style>

      {/* FAB Speed-dial */}
      {scanOpen && createPortal(
        <>
          <div className="fab-overlay" onClick={() => setScanOpen(false)} />
          <div className="fab-panel">
            {[
              { label: 'New Project',      path: '/projects', icon: 'folder_open', bg: '#e0f2fe', ic: '#0284c7', isImg: false },
              { label: 'Source Code Scan', path: '/scanner',  icon: 'code',        bg: '#eff6ff', ic: '#0058be', isImg: false },
              { label: 'Security Chat',    path: '/chat',     icon: '/ai-bot.png', bg: '#eff6ff', ic: '#0058be', isImg: true  },
            ].map(item => (
              <Link key={item.path} to={item.path} className="fab-item" onClick={() => setScanOpen(false)}>
                <div className="fab-item-icon" style={{ background: item.bg }}>
                  {item.isImg
                    ? <img src={item.icon} alt="AI" style={{ width: 22, height: 22, borderRadius: 5, objectFit: 'cover' }} />
                    : <span className="material-symbols-outlined" style={{ fontSize: 19, color: item.ic }}>{item.icon}</span>
                  }
                </div>
                {item.label}
              </Link>
            ))}
          </div>
        </>,
        document.body
      )}

      {/* Bottom Bar */}
      <nav className="bottom-nav-bar" style={{
        position: 'fixed', bottom: 0, left: 0, right: 0, zIndex: 1000,
        background: 'rgba(255,255,255,0.96)',
        backdropFilter: 'blur(20px)', WebkitBackdropFilter: 'blur(20px)',
        borderTop: '1px solid rgba(194,198,214,0.5)',
        paddingBottom: 'env(safe-area-inset-bottom, 0px)',
        height: 64, alignItems: 'center', justifyContent: showFab ? 'space-around' : 'stretch',
        boxShadow: '0 -4px 24px rgba(15,23,42,0.07)',
      }}>
        {showFab ? (
          <>
            {leftItems.map(item => {
              const active = isActive(item.path)
              return (
                <Link key={item.path} to={item.path} className={`bnav-item${active ? ' active' : ''}`}>
                  <div className="bnav-icon">
                    <span className="material-symbols-outlined" style={{
                      fontSize: 22, color: active ? '#0058be' : '#94a3b8',
                      fontVariationSettings: active ? "'FILL' 1" : "'FILL' 0",
                      transition: 'all 0.15s ease',
                    }}>{item.icon}</span>
                  </div>
                  <span className="bnav-label">{item.name}</span>
                </Link>
              )
            })}

            <div className="bnav-fab-wrapper">
              <button className={`bnav-fab${scanOpen ? ' open' : ''}`} onClick={() => setScanOpen(o => !o)} aria-label="Quick actions">
                <span className="material-symbols-outlined fab-icon">add</span>
              </button>
            </div>

            {rightItems.map(item => {
              const active = isActive(item.path)
              return (
                <Link key={item.path} to={item.path} className={`bnav-item${active ? ' active' : ''}`}>
                  <div className="bnav-icon">
                    <span className="material-symbols-outlined" style={{
                      fontSize: 22, color: active ? '#0058be' : '#94a3b8',
                      fontVariationSettings: active ? "'FILL' 1" : "'FILL' 0",
                      transition: 'all 0.15s ease',
                    }}>{item.icon}</span>
                  </div>
                  <span className="bnav-label">{item.name}</span>
                </Link>
              )
            })}
          </>
        ) : (
          items.map(item => {
            const active = isActive(item.path)
            return (
              <Link key={item.path} to={item.path} className={`bnav-item${active ? ' active' : ''}`} style={{ flex: 1 }}>
                <div className="bnav-icon">
                  <span className="material-symbols-outlined" style={{
                    fontSize: 22, color: active ? '#0058be' : '#94a3b8',
                    fontVariationSettings: active ? "'FILL' 1" : "'FILL' 0",
                    transition: 'all 0.15s ease',
                  }}>{item.icon}</span>
                </div>
                <span className="bnav-label">{item.name}</span>
              </Link>
            )
          })
        )}
      </nav>
    </>
  )
}


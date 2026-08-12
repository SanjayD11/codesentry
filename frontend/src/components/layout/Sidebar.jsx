import { Link, useLocation } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'

export default function Sidebar() {
  const { pathname } = useLocation()
  const { user, logout } = useAuth()

  let navItems = []
  if (user?.role === 'ADMIN') {
    navItems = [
      { name: 'Admin Dashboard', path: '/admin/dashboard', icon: 'admin_panel_settings' },
      { name: 'Project Management', path: '/admin/projects', icon: 'account_tree' },
      { name: 'Activity Logs', path: '/admin/audit-logs', icon: 'list_alt' }
    ]
  } else {
    navItems = [
      { name: 'Dashboard',           path: '/',          icon: 'dashboard' },
      { name: 'Source Code Scanner', path: '/scanner',   icon: 'code' },
      { name: 'Analysis Reports',    path: '/reports',   icon: 'description' },
      { name: 'Scan History',        path: '/history',   icon: 'history' },
      { name: 'AI Assistant',        path: '/chat',      icon: 'smart_toy' },
    ]
  }

  const isActive = (path) => {
    if (path === '/') return pathname === '/'
    return pathname.startsWith(path)
  }

  return (
    <aside className="fixed left-0 top-0 h-full w-64 bg-surface-container-lowest border-r border-outline-variant/50 shadow-sm flex flex-col p-md z-40 hidden md:flex">
      {/* Brand Header */}
      <a href="/landing.html" className="flex flex-col gap-1 mb-xl pl-sm cursor-pointer">
        <img src="/logo.png" alt="CodeSentry" style={{ height: '44px', width: 'auto', objectFit: 'contain', objectPosition: 'left' }} />
        <p className="font-label-md text-[12px] text-on-surface-variant font-medium mt-1">AI Security Platform</p>
      </a>

      {/* Navigation */}
      <nav className="flex-1 space-y-sm overflow-y-auto">
        {navItems.map((item) => {
          const active = isActive(item.path)
          return (
            <Link
              key={item.path}
              to={item.path}
              className={`flex items-center gap-md px-md py-2 rounded-lg transition-all duration-180 ease-in-out cursor-pointer ${
                active 
                  ? 'bg-primary-fixed/50 text-primary font-semibold border-l-4 border-primary pl-[12px] shadow-xs' 
                  : 'text-on-surface-variant hover:bg-surface-container-high hover:text-on-surface transition-colors font-medium'
              }`}
            >
              <span className={`material-symbols-outlined text-[20px] ${active ? 'text-primary' : 'font-light text-secondary'}`}>{item.icon}</span>
              <span className="font-body-md text-body-md">{item.name}</span>
            </Link>
          )
        })}
      </nav>

      {/* Bottom Actions */}
      <div className="mt-auto space-y-sm pt-md border-t border-outline-variant/50">
        <Link
          to="/settings"
          className="flex items-center gap-md px-md py-2 text-on-surface-variant hover:bg-surface-container-high hover:text-on-surface transition-colors rounded-lg duration-180 ease-in-out cursor-pointer"
        >
          <span className="material-symbols-outlined text-[20px] font-light text-secondary">settings</span>
          <span className="font-body-md text-body-md">Settings</span>
        </Link>
        <button
          onClick={logout}
          className="w-full flex items-center gap-md px-md py-2 text-on-surface-variant hover:bg-error-container/60 hover:text-error transition-colors rounded-lg duration-180 ease-in-out cursor-pointer"
        >
          <span className="material-symbols-outlined text-[20px] font-light">logout</span>
          <span className="font-body-md text-body-md">Sign Out</span>
        </button>
      </div>
    </aside>
  )
}


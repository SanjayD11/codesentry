import { Link, useLocation } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'

const PAGE_TITLES = {
  '/':         'Dashboard',
  '/scanner':  'Source Code Scanner',
  '/reports':  'Analysis Reports',
  '/history':  'Scan History',
  '/chat':     'AI Assistant',
  '/settings': 'Settings',
  '/profile':  'Profile',
  '/admin/dashboard': 'Enterprise Admin Dashboard',
}

export default function Topbar() {
  const { user } = useAuth()
  const { pathname } = useLocation()

  const pageTitle = Object.entries(PAGE_TITLES).find(([path]) =>
    path === '/' ? pathname === '/' : pathname.startsWith(path)
  )?.[1] ?? 'Aegis Nexus'

  const displayName = user
    ? `${user.firstName ?? ''} ${user.lastName ?? ''}`.trim() || user.email
    : 'Security Admin'

  const initials = user?.firstName
    ? `${user.firstName[0]}${user.lastName?.[0] ?? ''}`.toUpperCase()
    : 'SA'

  return (
    <header className="sticky top-0 z-30 bg-surface/80 backdrop-blur-md border-b border-outline-variant/50 flex items-center justify-between px-margin-mobile md:px-margin-desktop py-md">
      <div className="flex items-center gap-md">
        <h2 className="font-headline-lg-mobile md:font-headline-lg text-[24px] md:text-[32px] text-on-surface font-extrabold tracking-tight">
          {pageTitle}
        </h2>
      </div>

      <div className="flex items-center gap-lg">
        {/* Actions */}
        <div className="hidden md:flex items-center gap-sm">
          <button className="w-10 h-10 rounded-full flex items-center justify-center text-on-surface-variant hover:bg-surface-container transition-colors relative" aria-label="Notifications">
            <span className="material-symbols-outlined font-light text-[24px]">notifications</span>
            <span className="absolute top-2 right-2 w-2 h-2 bg-error rounded-full"></span>
          </button>
        </div>

        {/* Profile Dropdown Trigger */}
        <Link to="/profile" className="flex items-center gap-sm hover-lift cursor-pointer">
          <div className="text-right hidden sm:block">
            <p className="font-label-md text-[14px] text-on-surface font-semibold">{displayName}</p>
            <p className="font-body-md text-[12px] text-on-surface-variant">{user?.email ?? ''}</p>
          </div>
          <div className="w-10 h-10 rounded-full bg-primary-container text-on-primary-container flex items-center justify-center font-title-lg text-[16px] border border-outline-variant/30 shadow-sm">
            {initials}
          </div>
        </Link>
      </div>
    </header>
  )
}

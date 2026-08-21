/**
 * LoadingSpinner — used for Suspense fallbacks inside already-rendered layouts.
 * For the full-page initial app load, see AppSplash (App.jsx Suspense fallback).
 */
export default function LoadingSpinner({ size = 'md', className = '', fullScreen = false }) {
  if (fullScreen) {
    return (
      <div style={{
        position: 'fixed', inset: 0, zIndex: 9999,
        background: '#f9f9ff',
        display: 'flex', flexDirection: 'column',
        alignItems: 'center', justifyContent: 'center',
        fontFamily: "'Manrope', sans-serif",
      }}>
        <style>{`
          @keyframes cs-spin  { to { transform: rotate(360deg); } }
          @keyframes cs-pulse { 0%,100% { opacity: 1; } 50% { opacity: 0.4; } }
          @keyframes cs-shimmer {
            0%   { transform: translateX(-100%); }
            100% { transform: translateX(400%); }
          }
          @keyframes cs-fadein { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }
        `}</style>

        {/* Logo + ring */}
        <div style={{ position: 'relative', width: 88, height: 88, marginBottom: 24 }}>
          {/* Outer gradient ring */}
          <svg width="88" height="88" viewBox="0 0 88 88" style={{
            position: 'absolute', inset: 0,
            animation: 'cs-spin 1.4s linear infinite',
          }}>
            <defs>
              <linearGradient id="csGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%"   stopColor="#0058be" stopOpacity="1" />
                <stop offset="60%"  stopColor="#2170e4" stopOpacity="0.6" />
                <stop offset="100%" stopColor="#2170e4" stopOpacity="0" />
              </linearGradient>
            </defs>
            <circle cx="44" cy="44" r="38"
              fill="none" stroke="url(#csGrad)" strokeWidth="4"
              strokeLinecap="round" strokeDasharray="180 60"
            />
          </svg>
          {/* Logo image centred */}
          <div style={{
            position: 'absolute', inset: 0,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>
            <img src="/logo.png" alt="CodeSentry" style={{ width: 46, height: 46, objectFit: 'contain' }} />
          </div>
        </div>

        {/* App name */}
        <p style={{
          margin: '0 0 8px', fontSize: 20, fontWeight: 700, color: '#111c2d',
          letterSpacing: '-0.02em',
          animation: 'cs-fadein 0.4s ease both',
          fontFamily: "'Plus Jakarta Sans', 'Manrope', sans-serif",
        }}>
          CodeSentry
        </p>
        <p style={{
          margin: '0 0 28px', fontSize: 13, color: '#64748b', fontWeight: 500,
          animation: 'cs-fadein 0.5s 0.1s ease both',
        }}>
          AI Security Analysis Platform
        </p>

        {/* Progress shimmer bar */}
        <div style={{
          width: 160, height: 3, borderRadius: 4,
          background: '#e2e8f0', overflow: 'hidden',
          animation: 'cs-fadein 0.5s 0.15s ease both',
        }}>
          <div style={{
            width: 40, height: '100%',
            background: 'linear-gradient(90deg, transparent, #0058be, transparent)',
            animation: 'cs-shimmer 1.4s ease-in-out infinite',
          }} />
        </div>
      </div>
    )
  }

  const sizeMap = { sm: 16, md: 28, lg: 40 }
  const s = sizeMap[size] || 28
  const stroke = size === 'sm' ? 2 : 3

  return (
    <div className={`flex justify-center items-center ${className}`} role="status" aria-label="Loading">
      <svg width={s} height={s} viewBox={`0 0 ${s} ${s}`} style={{ animation: 'cs-spin 1.2s linear infinite' }}>
        <style>{'@keyframes cs-spin { to { transform: rotate(360deg); } }'}</style>
        <defs>
          <linearGradient id={`sp${size}`} x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%"   stopColor="#0058be" />
            <stop offset="100%" stopColor="#2170e4" stopOpacity="0.2" />
          </linearGradient>
        </defs>
        <circle
          cx={s/2} cy={s/2} r={s/2 - stroke}
          fill="none" stroke={`url(#sp${size})`} strokeWidth={stroke}
          strokeLinecap="round" strokeDasharray={`${(s/2 - stroke) * 2 * Math.PI * 0.75} ${(s/2 - stroke) * 2 * Math.PI * 0.25}`}
        />
      </svg>
      <span className="sr-only">Loading...</span>
    </div>
  )
}

/* eslint-disable react/only-export-components */
import { createContext, useContext, useState, useCallback } from 'react'

const ToastContext = createContext(null)

const TOAST_CONFIG = {
  success: {
    accent: '#16a34a',
    icon: (
      <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
        <path d="M2.5 7.5L5.5 10.5L11.5 3.5" stroke="#16a34a" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
      </svg>
    ),
  },
  error: {
    accent: '#dc2626',
    icon: (
      <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
        <path d="M3.5 3.5L10.5 10.5M10.5 3.5L3.5 10.5" stroke="#dc2626" strokeWidth="2" strokeLinecap="round"/>
      </svg>
    ),
  },
  warning: {
    accent: '#d97706',
    icon: (
      <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
        <path d="M7 4.5V8M7 10.5h.01" stroke="#d97706" strokeWidth="2" strokeLinecap="round"/>
      </svg>
    ),
  },
  info: {
    accent: '#2563eb',
    icon: (
      <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
        <path d="M7 4.5h.01M7 7v3.5" stroke="#2563eb" strokeWidth="2" strokeLinecap="round"/>
      </svg>
    ),
  },
}

function ToastItem({ t, onRemove }) {
  const c = TOAST_CONFIG[t.type] || TOAST_CONFIG.info
  return (
    <div style={{
      position: 'relative',
      display: 'flex',
      alignItems: 'center',
      gap: 11,
      minWidth: 280,
      maxWidth: 360,
      background: '#ffffff',
      border: '1px solid #e2e8f0',
      borderLeft: `3px solid ${c.accent}`,
      borderRadius: '10px',
      padding: '12px 14px',
      boxShadow: '0 4px 24px rgba(15,23,42,0.09), 0 1px 3px rgba(15,23,42,0.05)',
      pointerEvents: 'auto',
      animation: 'toastSlideIn 200ms cubic-bezier(0.16, 1, 0.3, 1)',
    }}>
      {/* Icon */}
      <div style={{
        width: 22, height: 22,
        display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
      }}>
        {c.icon}
      </div>

      {/* Message */}
      <p style={{
        margin: 0, flex: 1,
        fontSize: '13px',
        fontWeight: 500,
        color: '#1e293b',
        lineHeight: 1.5,
        fontFamily: "'Manrope', sans-serif",
        letterSpacing: '-0.005em',
      }}>
        {t.message}
      </p>

      {/* Dismiss */}
      <button
        onClick={() => onRemove(t.id)}
        aria-label="Dismiss"
        style={{
          background: 'none', border: 'none', cursor: 'pointer',
          padding: '3px', borderRadius: '5px',
          color: '#94a3b8', display: 'flex', alignItems: 'center',
          justifyContent: 'center', flexShrink: 0,
          transition: 'color 100ms ease, background 100ms ease',
        }}
        onMouseEnter={e => {
          e.currentTarget.style.color = '#475569'
          e.currentTarget.style.background = '#f1f5f9'
        }}
        onMouseLeave={e => {
          e.currentTarget.style.color = '#94a3b8'
          e.currentTarget.style.background = 'none'
        }}
      >
        <svg width="11" height="11" viewBox="0 0 11 11" fill="none">
          <path d="M1 1l9 9M10 1L1 10" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round"/>
        </svg>
      </button>
    </div>
  )
}

function ToastContainer({ toasts, onRemove }) {
  return (
    <div style={{
      position: 'fixed',
      bottom: '24px',
      right: '20px',
      zIndex: 9999,
      display: 'flex',
      flexDirection: 'column',
      gap: '8px',
      pointerEvents: 'none',
      alignItems: 'flex-end',
    }}>
      <style>{`
        @keyframes toastSlideIn {
          from { opacity: 0; transform: translateX(12px); }
          to   { opacity: 1; transform: translateX(0); }
        }
      `}</style>
      {toasts.map((t) => (
        <ToastItem key={t.id} t={t} onRemove={onRemove} />
      ))}
    </div>
  )
}

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([])

  const addToast = useCallback((message, type = 'info', duration = 4000) => {
    const id = Date.now() + Math.random()
    setToasts((prev) => [...prev, { id, message, type }])
    setTimeout(() => setToasts((prev) => prev.filter((t) => t.id !== id)), duration)
  }, [])

  const removeToast = useCallback(
    (id) => setToasts((prev) => prev.filter((t) => t.id !== id)),
    []
  )

  return (
    <ToastContext.Provider value={{ addToast }}>
      {children}
      <ToastContainer toasts={toasts} onRemove={removeToast} />
    </ToastContext.Provider>
  )
}

export const useToast = () => {
  const ctx = useContext(ToastContext)
  if (!ctx) throw new Error('useToast must be inside ToastProvider')
  return ctx.addToast
}

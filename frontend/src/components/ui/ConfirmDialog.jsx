import { useEffect, useRef } from 'react'
import { createPortal } from 'react-dom'

/**
 * Professional confirm dialog — replaces window.confirm()
 *
 * Props:
 *   open         {boolean}
 *   title        {string}
 *   message      {string}
 *   variant      {'danger'|'warning'|'info'}
 *   confirmLabel {string}
 *   cancelLabel  {string}
 *   loading      {boolean}
 *   onConfirm    {()=>void}
 *   onCancel     {()=>void}
 */
export default function ConfirmDialog({
  open,
  title = 'Are you sure?',
  message,
  variant = 'danger',
  confirmLabel = 'Confirm',
  cancelLabel  = 'Cancel',
  loading = false,
  onConfirm,
  onCancel,
}) {
  const confirmRef = useRef(null)

  useEffect(() => {
    if (open) setTimeout(() => confirmRef.current?.focus(), 50)
  }, [open])

  useEffect(() => {
    if (!open) return
    const handler = (e) => { if (e.key === 'Escape') onCancel?.() }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [open, onCancel])

  if (!open) return null

  const VARIANT = {
    danger:  { bg: '#dc2626', hover: '#b91c1c', icon: 'delete_forever', iconBg: '#fef2f2', iconColor: '#dc2626' },
    warning: { bg: '#d97706', hover: '#b45309', icon: 'warning',         iconBg: '#fffbeb', iconColor: '#d97706' },
    info:    { bg: '#2563eb', hover: '#1d4ed8', icon: 'info',            iconBg: '#eff6ff', iconColor: '#2563eb' },
  }
  const v = VARIANT[variant] || VARIANT.danger

  return createPortal(
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="cdlg-title"
      style={{
        position: 'fixed', inset: 0, zIndex: 9999,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        padding: 24,
        background: 'rgba(15, 23, 42, 0.45)',
        animation: 'cdlgFadeIn 120ms cubic-bezier(0.16,1,0.3,1)',
      }}
      onClick={onCancel}
    >
      <style>{`
        @keyframes cdlgFadeIn  { from { opacity: 0; } to { opacity: 1; } }
        @keyframes spin         { to   { transform: rotate(360deg); } }
        .cdlg-confirm-btn { transition: background 120ms ease, box-shadow 120ms ease; }
        .cdlg-confirm-btn:hover { filter: brightness(0.9); }
        .cdlg-cancel-btn  { transition: background 120ms ease, border-color 120ms ease; }
        .cdlg-cancel-btn:hover { background: #f8fafc !important; border-color: #cbd5e1 !important; }
      `}</style>

      <div
        onClick={e => e.stopPropagation()}
        style={{
          background: '#ffffff',
          borderRadius: 18,
          width: '100%',
          maxWidth: 420,
          boxShadow: '0 24px 48px -8px rgba(15,23,42,0.2), 0 8px 16px -4px rgba(15,23,42,0.08)',
          border: '1px solid rgba(226,232,240,0.9)',
          overflow: 'hidden',
          animation: 'cdlgSlideUp 160ms cubic-bezier(0.16,1,0.3,1)',
        }}
      >
        {/* Header */}
        <div style={{ padding: '24px 24px 0', display: 'flex', alignItems: 'flex-start', gap: 16 }}>
          <div style={{
            width: 44, height: 44, borderRadius: 12, flexShrink: 0,
            background: v.iconBg, display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>
            <span className="material-symbols-outlined" style={{ fontSize: 22, color: v.iconColor, fontVariationSettings: "'FILL' 1" }}>
              {v.icon}
            </span>
          </div>

          <div style={{ flex: 1, minWidth: 0 }}>
            <p id="cdlg-title" style={{ margin: '0 0 6px', fontSize: 16, fontWeight: 700, color: '#0f172a', letterSpacing: '-0.01em', fontFamily: "'Plus Jakarta Sans', 'Manrope', sans-serif" }}>
              {title}
            </p>
            {message && (
              <p style={{ margin: 0, fontSize: 13.5, color: '#64748b', lineHeight: 1.55, fontFamily: "'Manrope', sans-serif" }}>
                {message}
              </p>
            )}
          </div>
        </div>

        {/* Divider */}
        <div style={{ margin: '20px 0 0', height: 1, background: '#f1f5f9' }} />

        {/* Actions */}
        <div style={{ padding: '16px 24px 20px', display: 'flex', justifyContent: 'flex-end', gap: 10 }}>
          <button
            className="cdlg-cancel-btn"
            onClick={onCancel}
            disabled={loading}
            style={{
              height: 38, padding: '0 20px',
              background: '#ffffff', color: '#374151',
              border: '1px solid #cbd5e1', borderRadius: 10,
              fontSize: 13.5, fontWeight: 600, cursor: 'pointer',
              fontFamily: "'Manrope', sans-serif",
              display: 'flex', alignItems: 'center', gap: 6,
              boxShadow: '0 1px 2px rgba(15,23,42,0.04)',
            }}
          >
            {cancelLabel}
          </button>

          <button
            ref={confirmRef}
            className="cdlg-confirm-btn"
            onClick={onConfirm}
            disabled={loading}
            style={{
              height: 38, padding: '0 22px',
              background: loading ? '#94a3b8' : v.bg, color: '#ffffff',
              border: 'none', borderRadius: 10,
              fontSize: 13.5, fontWeight: 600, cursor: loading ? 'not-allowed' : 'pointer',
              fontFamily: "'Manrope', sans-serif",
              display: 'flex', alignItems: 'center', gap: 8,
              boxShadow: '0 1px 4px rgba(0,0,0,0.16)',
            }}
          >
            {loading && (
              <svg width="14" height="14" viewBox="0 0 14 14" style={{ animation: 'spin 0.7s linear infinite' }}>
                <circle cx="7" cy="7" r="5.5" fill="none" stroke="rgba(255,255,255,0.35)" strokeWidth="2" />
                <path d="M7 1.5a5.5 5.5 0 0 1 5.5 5.5" fill="none" stroke="white" strokeWidth="2" strokeLinecap="round" />
              </svg>
            )}
            {loading ? 'Processing…' : confirmLabel}
          </button>
        </div>
      </div>
    </div>,
    document.body
  )
}

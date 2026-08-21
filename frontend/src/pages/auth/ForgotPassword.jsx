import { useState, useRef } from 'react'
import { Link } from 'react-router-dom'
import { forgotPassword } from '../../api/authApi'
import { useToast } from '../../hooks/useToast'
import { LuFileSearch, LuMessageSquare, LuClipboardCheck } from 'react-icons/lu'

// ── Helpers ────────────────────────────────────────────────────────────────
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function validateEmail(email) {
  if (!email.trim()) return 'Email address is required.'
  if (!EMAIL_REGEX.test(email.trim())) return 'Please enter a valid email address.'
  return null
}

/**
 * Maps Firebase Auth error codes to user-friendly messages.
 * Never reveal whether an account exists (anti-enumeration for unknown email).
 */
function getFirebaseErrorMessage(errorCode) {
  const map = {
    'auth/invalid-email':           'That email address is not valid.',
    'auth/network-request-failed':  'Network error. Please check your internet connection and try again.',
    'auth/too-many-requests':       'Too many attempts. Please wait a few minutes before trying again.',
    'auth/user-disabled':           'This account has been disabled. Please contact support.',
    // user-not-found → intentionally treated as success (anti-enumeration)
  }
  return map[errorCode] || 'Something went wrong. Please try again.'
}

// ── Component ──────────────────────────────────────────────────────────────
export default function ForgotPassword() {
  const [email, setEmail]           = useState('')
  const [emailError, setEmailError] = useState('')
  const [loading, setLoading]       = useState(false)
  const [sent, setSent]             = useState(false)

  const addToast        = useToast()
  const requestInFlight = useRef(false)

  const handleEmailChange = (e) => {
    setEmail(e.target.value)
    if (emailError) setEmailError('')
  }

  const handleSubmit = async (e) => {
    e.preventDefault()

    const validationError = validateEmail(email)
    if (validationError) { setEmailError(validationError); return }
    if (requestInFlight.current) return

    requestInFlight.current = true
    setLoading(true)

    try {
      await forgotPassword(email.trim().toLowerCase())
      // Always show success — even if the email doesn't exist Backend returns success
      setSent(true)
    } catch (err) {
      const msg = err.response?.data?.message || 'Network error. Please try again later.'
      addToast(msg, 'error')
      console.error('[ForgotPassword] Error:', err.message)
    } finally {
      setLoading(false)
      requestInFlight.current = false
    }
  }

  return (
    <div className="h-screen overflow-hidden grid lg:grid-cols-[58%_42%] bg-surface-container-lowest font-['Manrope',sans-serif]">

      {/* ── LEFT BRAND PANEL ─────────────────────────────────────────── */}
      <div className="relative hidden lg:flex flex-col justify-between p-6 lg:p-10 overflow-hidden bg-[#fafcff] border-r border-outline-variant/30 h-full">
        <div className="absolute inset-0 pointer-events-none opacity-[0.03]"
          style={{ backgroundImage: `linear-gradient(to right,#0058be 1px,transparent 1px),linear-gradient(to bottom,#0058be 1px,transparent 1px)`, backgroundSize:'40px 40px' }} />
        <div className="absolute inset-0 bg-gradient-to-br from-transparent via-[#fafcff]/80 to-[#fafcff] pointer-events-none" />

        <Link to="/" className="relative z-10 flex items-center gap-3 w-fit">
          <div className="w-9 h-9 rounded-xl bg-primary flex items-center justify-center text-white shadow-sm">
            <span className="material-symbols-outlined text-[20px]" style={{ fontVariationSettings:"'FILL' 1" }}>security</span>
          </div>
          <span className="font-headline-md text-[18px] font-extrabold tracking-tight text-on-surface">CodeSentry</span>
        </Link>

        <div className="relative z-10 flex-1 flex flex-col justify-center max-w-[520px] mx-auto w-full py-6">
          <h2 className="font-headline-lg text-[34px] leading-[1.15] font-extrabold tracking-tight text-on-surface mb-4">
            Secure software starts with <span className="text-primary">trusted intelligence.</span>
          </h2>
          <p className="font-body-md text-[14px] text-on-surface-variant leading-relaxed mb-6">
            Scan source code, detect vulnerabilities, generate professional security reports, and receive AI-powered remediation guidance—all from one unified security platform.
          </p>
          <div className="space-y-3">
            {[
              { Icon: LuFileSearch,     title: 'Hybrid Static Analysis',  desc: 'Rule-based vulnerability detection combined with AI-powered explanations.' },
              { Icon: LuMessageSquare,  title: 'AI Security Assistant',   desc: 'Understand findings instantly with contextual remediation guidance.' },
              { Icon: LuClipboardCheck, title: 'Professional Reports',    desc: 'Generate executive-ready PDF security assessments with confidence.' },
            ].map(({ Icon, title, desc }) => (
              <div key={title} className="group flex items-start gap-4 p-3 rounded-2xl bg-white/70 backdrop-blur-md border border-outline-variant/40 hover:shadow-[0_8px_32px_-8px_rgba(0,0,0,0.06)] hover:-translate-y-[1px] hover:border-outline-variant/60 transition-all duration-200 cursor-default">
                <div className="w-[40px] h-[40px] rounded-[12px] bg-primary/5 border border-primary/10 flex items-center justify-center shrink-0 group-hover:border-primary/20 transition-colors shadow-sm">
                  <Icon className="text-[20px] text-primary" strokeWidth={1.5} />
                </div>
                <div className="pt-0.5">
                  <h3 className="font-headline-md text-[15px] font-bold text-on-surface mb-1">{title}</h3>
                  <p className="font-body-md text-[14px] text-on-surface-variant leading-relaxed">{desc}</p>
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className="relative z-10 flex items-center gap-3">
          <div className="flex -space-x-2">
            {['S','D','E'].map(l => (
              <div key={l} className="w-8 h-8 rounded-full bg-surface-container-high border-2 border-[#fafcff] flex items-center justify-center text-[10px] font-bold text-on-surface shadow-sm">{l}</div>
            ))}
          </div>
          <p className="font-label-md text-[13px] text-on-surface-variant">Designed for secure software development from classroom projects to enterprise workflows.</p>
        </div>
      </div>

      {/* ── RIGHT FORM PANEL ─────────────────────────────────────────── */}
      <div className="flex flex-col items-center justify-center p-4 md:p-6 relative bg-[#f9f9ff] h-full overflow-y-auto">
        <div className="absolute inset-0 lg:hidden" style={{ backgroundColor:'#f9f9ff', backgroundImage:'radial-gradient(at 100% 0%,rgba(33,112,228,0.08) 0px,transparent 50%)' }} />

        <div className="w-full max-w-[380px] relative z-10 py-4">
          {/* Mobile logo */}
          <div className="flex lg:hidden items-center gap-sm mb-2xl">
            <Link to="/" className="flex items-center gap-sm">
              <div className="w-10 h-10 rounded-lg bg-primary-container flex items-center justify-center text-on-primary-container shadow-sm">
                <span className="material-symbols-outlined" style={{ fontVariationSettings:"'FILL' 1" }}>security</span>
              </div>
              <span className="font-headline-md text-[18px] font-extrabold tracking-tight text-primary">CodeSentry</span>
            </Link>
          </div>

          {/* ── Success state ── */}
          {sent ? (
            <div className="text-center fade-in" role="alert" aria-live="polite">
              <div className="flex justify-center mb-5">
                <div className="w-14 h-14 bg-[#00855b]/10 rounded-full flex items-center justify-center text-[#006947]">
                  <span className="material-symbols-outlined text-[32px]">mark_email_read</span>
                </div>
              </div>
              <h1 className="font-headline-lg text-[26px] text-on-surface font-extrabold tracking-tight mb-2">Check Your Inbox</h1>
              <p className="font-body-md text-[14px] text-on-surface-variant leading-relaxed mb-2">
                If <span className="text-on-surface font-semibold">{email}</span> is registered, a password reset email has been sent.
              </p>
              <p className="font-body-md text-[13px] text-on-surface-variant leading-relaxed mb-6">
                Click the link in the email to choose a new password. Don't forget to check your spam folder.
              </p>
              <div className="flex flex-col gap-3">
                <button
                  onClick={() => { setSent(false); setEmail(''); setEmailError('') }}
                  className="cta-magnetic w-full border border-outline-variant bg-surface-container-lowest text-on-surface-variant py-2.5 rounded-lg font-label-md text-[13px] font-semibold hover:bg-surface-container-low transition-colors"
                >
                  Try a different email
                </button>
                <Link to="/login" className="cta-magnetic w-full bg-primary text-on-primary py-2.5 rounded-lg font-label-md text-[13px] font-semibold tracking-[0.005em] shadow-[inset_0_1px_0_rgba(255,255,255,0.2),0_1px_2px_rgba(0,0,0,0.1)] hover:bg-[#004395] flex items-center justify-center">
                  Back to Sign in
                </Link>
              </div>
            </div>

          /* ── Form state ── */
          ) : (
            <div className="fade-in">
              <h1 className="font-headline-lg text-[26px] text-on-surface font-extrabold tracking-tight text-center md:text-left">Reset Password</h1>
              <p className="font-body-md text-[13.5px] text-on-surface-variant mt-1 text-center md:text-left mb-6">
                Enter your account email and we'll send you a secure password reset link.
              </p>

              <form onSubmit={handleSubmit} noValidate aria-label="Password reset form">
                <div className="field-group mb-1">
                  <input
                    type="email"
                    id="reset-email"
                    name="email"
                    placeholder=" "
                    className={`field-input${emailError ? ' border-error' : ''}`}
                    autoComplete="email"
                    value={email}
                    onChange={handleEmailChange}
                    required
                    aria-required="true"
                    aria-describedby={emailError ? 'email-error' : undefined}
                    aria-invalid={!!emailError}
                    disabled={loading}
                  />
                  <label htmlFor="reset-email" className="field-label">Email address</label>
                </div>

                {emailError && (
                  <p id="email-error" className="text-[12px] text-[#ba1a1a] mt-1 mb-3 flex items-center gap-1" role="alert" aria-live="polite">
                    <span className="material-symbols-outlined text-[14px]">error</span>
                    {emailError}
                  </p>
                )}

                <div className={emailError ? 'mt-3' : 'mt-5'}>
                  <button
                    type="submit"
                    id="reset-submit-btn"
                    disabled={loading}
                    aria-disabled={loading}
                    aria-busy={loading}
                    className="cta-magnetic w-full bg-primary text-on-primary py-2.5 rounded-lg font-label-md text-[13px] font-semibold tracking-[0.005em] shadow-[inset_0_1px_0_rgba(255,255,255,0.2),0_1px_2px_rgba(0,0,0,0.1)] hover:bg-[#004395] flex items-center justify-center gap-sm disabled:opacity-60 disabled:cursor-not-allowed"
                  >
                    {loading ? (
                      <><span className="spinner" aria-hidden="true" /><span>Sending reset link…</span></>
                    ) : 'Send Reset Link'}
                  </button>
                </div>
              </form>

              <div className="flex items-center gap-4 my-6">
                <div className="h-px bg-outline-variant flex-1" />
                <span className="font-label-md text-[12px] text-on-surface-variant">or</span>
                <div className="h-px bg-outline-variant flex-1" />
              </div>

              <div className="text-center">
                <Link to="/login" className="font-label-md text-[13px] text-on-surface-variant font-semibold hover:text-primary transition-colors flex items-center justify-center gap-1.5">
                  <span className="material-symbols-outlined text-[16px]" aria-hidden="true">arrow_back</span>
                  Back to Sign in
                </Link>
              </div>
            </div>
          )}

          <p className="flex items-center justify-center gap-1.5 font-label-md text-[11.5px] text-on-surface-variant mt-8" aria-hidden="true">
            <span className="material-symbols-outlined text-[14px] text-tertiary-container">lock</span>
            Secure password recovery with encrypted verification
          </p>
        </div>
      </div>
    </div>
  )
}

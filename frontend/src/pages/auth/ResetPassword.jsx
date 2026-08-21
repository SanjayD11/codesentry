import { useState, useEffect, useRef } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { resetPassword } from '../../api/authApi'
import { useToast } from '../../hooks/useToast'
import { LuFileSearch, LuMessageSquare, LuClipboardCheck, LuEye, LuEyeOff } from 'react-icons/lu'

// ── Helpers ────────────────────────────────────────────────────────────────
const REDIRECT_DELAY_SECONDS = 5

function getPasswordScore(password) {
  let score = 0
  if (password.length > 7)                                   score++
  if (/[A-Z]/.test(password) && /[a-z]/.test(password))    score++
  if (/\d/.test(password))                                   score++
  if (/[^A-Za-z0-9]/.test(password))                        score++
  return score
}

// ── Component ──────────────────────────────────────────────────────────────
export default function ResetPassword() {
  const [searchParams]   = useSearchParams()
  const navigate         = useNavigate()
  const addToast         = useToast()
  const requestInFlight  = useRef(false)

  // URL param: ?token=XXX
  const token = searchParams.get('token')

  // Form state
  const [newPassword, setNewPassword]       = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [showNew, setShowNew]               = useState(false)
  const [showConfirm, setShowConfirm]       = useState(false)
  const [loading, setLoading]               = useState(false)

  // Outcome state
  const [success, setSuccess]               = useState(false)
  const [countdown, setCountdown]           = useState(REDIRECT_DELAY_SECONDS)

  const passwordScore = getPasswordScore(newPassword)
  const strengthColors = ['#f43f5e', '#f59e0b', '#10b981', '#059669']
  const strengthText   = ['Weak', 'Fair', 'Good', 'Strong']

  // Auto-redirect after success
  useEffect(() => {
    if (!success) return
    const interval = setInterval(() => {
      setCountdown(c => {
        if (c <= 1) { clearInterval(interval); navigate('/login'); return 0 }
        return c - 1
      })
    }, 1000)
    return () => clearInterval(interval)
  }, [success, navigate])

  const handleSubmit = async (e) => {
    e.preventDefault()

    if (newPassword.length < 8)
      return addToast('Password must be at least 8 characters.', 'error')
    if (passwordScore < 1)
      return addToast('Please choose a stronger password.', 'error')
    if (newPassword !== confirmPassword)
      return addToast('Passwords do not match.', 'error')
    if (requestInFlight.current) return

    requestInFlight.current = true
    setLoading(true)

    try {
      await resetPassword(token, newPassword)
      setSuccess(true)
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to update password. Please try again.'
      addToast(msg, 'error')
    } finally {
      setLoading(false)
      requestInFlight.current = false
    }
  }

  // If no token is present, show error immediately
  if (!token) {
    return (
      <div className="h-screen flex items-center justify-center bg-[#f9f9ff]">
        <div className="bg-white p-8 rounded-2xl shadow-sm text-center max-w-sm w-full mx-4 border border-outline-variant/40">
          <div className="w-14 h-14 bg-error-container text-on-error-container rounded-full flex items-center justify-center mx-auto mb-5 shadow-sm">
            <span className="material-symbols-outlined text-[28px]" style={{ fontVariationSettings:"'FILL' 0" }}>error</span>
          </div>
          <h1 className="font-headline-md text-[20px] font-bold text-on-surface mb-3">Invalid Link</h1>
          <p className="font-body-md text-[14px] text-on-surface-variant mb-6 leading-relaxed">
            This reset link is invalid or missing the reset token. Please request a new password reset link.
          </p>
          <Link to="/forgot-password"
            className="w-full flex items-center justify-center bg-primary hover:bg-primary/90 text-white rounded-xl h-[42px] font-label-lg font-bold transition-colors">
            Request New Link
          </Link>
        </div>
      </div>
    )
  }

  const BrandPanel = () => (
    <div className="relative hidden lg:flex flex-col justify-between p-6 lg:p-10 overflow-hidden bg-[#fafcff] border-r border-outline-variant/30 h-full">
      <div className="absolute inset-0 pointer-events-none opacity-[0.03]"
        style={{ backgroundImage:`linear-gradient(to right,#0058be 1px,transparent 1px),linear-gradient(to bottom,#0058be 1px,transparent 1px)`, backgroundSize:'40px 40px' }} />
      <div className="absolute inset-0 bg-gradient-to-br from-transparent via-[#fafcff]/80 to-[#fafcff] pointer-events-none" />

      <Link to="/" className="relative z-10 flex items-center gap-3 w-fit">
        <img src="/logo.png" alt="CodeSentry" className="h-[44px] w-auto object-contain" />
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
  )

  return (
    <div className="h-screen overflow-hidden grid lg:grid-cols-[58%_42%] bg-surface-container-lowest font-['Manrope',sans-serif]">
      <BrandPanel />

      <div className="flex flex-col items-center justify-center p-4 md:p-6 relative bg-[#f9f9ff] h-full overflow-y-auto">
        <div className="absolute inset-0 lg:hidden" style={{ backgroundColor:'#f9f9ff', backgroundImage:'radial-gradient(at 100% 0%,rgba(33,112,228,0.08) 0px,transparent 50%)' }} />

        <div className="w-full max-w-[380px] relative z-10 py-4">
          <div className="flex lg:hidden items-center gap-sm mb-6">
            <Link to="/" className="flex items-center gap-sm">
              <img src="/logo.png" alt="CodeSentry" className="h-[40px] w-auto object-contain" />
            </Link>
          </div>

          {success ? (
            <div className="text-center fade-in bg-white p-8 rounded-2xl shadow-[0_8px_30px_rgb(0,0,0,0.04)] border border-outline-variant/30" role="status" aria-live="polite">
              <div className="flex justify-center mb-5">
                <div className="w-16 h-16 bg-primary/10 rounded-full flex items-center justify-center relative shadow-inner">
                  <div className="absolute inset-0 bg-primary/20 rounded-full animate-ping opacity-20" />
                  <span className="material-symbols-outlined text-[32px] text-primary" style={{ fontVariationSettings:"'FILL' 1" }}>check_circle</span>
                </div>
              </div>
              <h1 className="font-headline-lg text-[22px] text-on-surface font-extrabold tracking-tight mb-3">Password Reset!</h1>
              <p className="font-body-md text-[14px] text-on-surface-variant leading-relaxed mb-6">
                Your password has been successfully updated. You can now log in to your account with your new credentials.
              </p>
              
              <div className="flex flex-col gap-3">
                <Link to="/login"
                  className="w-full flex items-center justify-center bg-primary hover:bg-primary/90 text-white rounded-xl h-[44px] font-label-lg font-bold transition-all shadow-sm hover:shadow active:scale-[0.98]">
                  Proceed to Login
                </Link>
                <p className="text-[13px] text-on-surface-variant font-medium">
                  Redirecting automatically in <span className="text-primary font-bold">{countdown}</span>s...
                </p>
              </div>
            </div>
          ) : (
            <div className="fade-in bg-white/70 lg:bg-transparent p-6 lg:p-0 rounded-2xl lg:rounded-none shadow-sm lg:shadow-none border border-outline-variant/30 lg:border-none backdrop-blur-sm lg:backdrop-blur-none">
              <div className="mb-7">
                <h1 className="font-headline-lg text-[26px] text-on-surface font-extrabold tracking-tight mb-2">Create new password</h1>
                <p className="font-body-md text-[14px] text-on-surface-variant leading-relaxed">
                  Please choose a strong password that you haven't used before on this platform.
                </p>
              </div>

              <form onSubmit={handleSubmit} className="flex flex-col gap-5" noValidate>
                <div>
                  <label className="block font-label-md text-[13px] font-bold text-on-surface mb-1.5 ml-1">New Password</label>
                  <div className="relative group">
                    <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-on-surface-variant group-focus-within:text-primary transition-colors">
                      <span className="material-symbols-outlined text-[20px]" style={{ fontVariationSettings:"'FILL' 0" }}>lock_reset</span>
                    </div>
                    <input
                      type={showNew ? "text" : "password"}
                      value={newPassword}
                      onChange={(e) => setNewPassword(e.target.value)}
                      required
                      placeholder="Enter new password"
                      className="w-full h-[46px] pl-10 pr-12 rounded-xl border border-outline-variant/60 bg-white/50 text-[14px] text-on-surface font-medium placeholder:text-on-surface-variant/60 focus:border-primary focus:ring-2 focus:ring-primary/10 transition-all shadow-sm hover:border-outline-variant outline-none"
                    />
                    <button
                      type="button"
                      onClick={() => setShowNew(!showNew)}
                      className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-on-surface-variant hover:text-primary transition-colors outline-none"
                      aria-label={showNew ? "Hide password" : "Show password"}
                    >
                      {showNew ? <LuEyeOff size={18} /> : <LuEye size={18} />}
                    </button>
                  </div>
                  
                  {newPassword && (
                    <div className="mt-2.5 px-1 fade-in">
                      <div className="flex gap-1 h-1 mb-1.5 w-full bg-surface-container-high rounded-full overflow-hidden">
                        {[0,1,2,3].map(i => (
                          <div 
                            key={i} 
                            className="h-full flex-1 transition-all duration-300"
                            style={{ 
                              backgroundColor: i < passwordScore ? strengthColors[passwordScore - 1] : 'transparent' 
                            }}
                          />
                        ))}
                      </div>
                      <div className="flex justify-between items-center text-[11px] font-medium">
                        <span style={{ color: passwordScore > 0 ? strengthColors[passwordScore - 1] : '#6b7280' }}>
                          {passwordScore > 0 ? strengthText[passwordScore - 1] : 'Start typing...'}
                        </span>
                        <span className="text-on-surface-variant">Min. 8 characters</span>
                      </div>
                    </div>
                  )}
                </div>

                <div>
                  <label className="block font-label-md text-[13px] font-bold text-on-surface mb-1.5 ml-1">Confirm New Password</label>
                  <div className="relative group">
                    <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-on-surface-variant group-focus-within:text-primary transition-colors">
                      <span className="material-symbols-outlined text-[20px]" style={{ fontVariationSettings:"'FILL' 0" }}>password</span>
                    </div>
                    <input
                      type={showConfirm ? "text" : "password"}
                      value={confirmPassword}
                      onChange={(e) => setConfirmPassword(e.target.value)}
                      required
                      placeholder="Confirm new password"
                      className="w-full h-[46px] pl-10 pr-12 rounded-xl border border-outline-variant/60 bg-white/50 text-[14px] text-on-surface font-medium placeholder:text-on-surface-variant/60 focus:border-primary focus:ring-2 focus:ring-primary/10 transition-all shadow-sm hover:border-outline-variant outline-none"
                    />
                    <button
                      type="button"
                      onClick={() => setShowConfirm(!showConfirm)}
                      className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-on-surface-variant hover:text-primary transition-colors outline-none"
                      aria-label={showConfirm ? "Hide password" : "Show password"}
                    >
                      {showConfirm ? <LuEyeOff size={18} /> : <LuEye size={18} />}
                    </button>
                  </div>
                  {confirmPassword && newPassword !== confirmPassword && (
                    <p className="mt-1.5 ml-1 text-[12px] font-medium text-error flex items-center gap-1 fade-in">
                      <span className="material-symbols-outlined text-[14px]">error</span>
                      Passwords do not match
                    </p>
                  )}
                  {confirmPassword && newPassword === confirmPassword && (
                    <p className="mt-1.5 ml-1 text-[12px] font-medium text-success flex items-center gap-1 fade-in">
                      <span className="material-symbols-outlined text-[14px]">check_circle</span>
                      Passwords match
                    </p>
                  )}
                </div>

                <button
                  type="submit"
                  disabled={loading}
                  className="w-full mt-2 h-[46px] flex items-center justify-center bg-primary hover:bg-primary/90 disabled:bg-surface-container-highest text-white disabled:text-on-surface/40 rounded-xl font-label-lg font-bold transition-all shadow-sm hover:shadow active:scale-[0.98] disabled:active:scale-100 disabled:shadow-none relative overflow-hidden group"
                >
                  <span className={`transition-opacity duration-200 flex items-center gap-2 ${loading ? 'opacity-0' : 'opacity-100'}`}>
                    Reset Password
                    <span className="material-symbols-outlined text-[18px] group-hover:translate-x-0.5 transition-transform">arrow_forward</span>
                  </span>
                  {loading && (
                    <div className="absolute inset-0 flex items-center justify-center">
                      <span className="spinner border-white border-2 text-transparent" style={{ width: 22, height: 22 }} />
                    </div>
                  )}
                </button>
              </form>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

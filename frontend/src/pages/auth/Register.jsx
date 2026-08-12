import { useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { createUserWithEmailAndPassword } from 'firebase/auth'
import { auth } from '../../config/firebase'
import { useAuth } from '../../hooks/useAuth'
import { useToast } from '../../hooks/useToast'
import { 
  LuFileSearch, LuMessageSquare, LuClipboardCheck, 
  LuEye, LuEyeOff 
} from 'react-icons/lu'
import { FcGoogle } from 'react-icons/fc'
import { FaGithub } from 'react-icons/fa'

export default function Register() {
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [agreeTerms, setAgreeTerms] = useState(false)
  const [loading, setLoading] = useState(false)
  const [passwordScore, setPasswordScore] = useState(0)
  
  const { register, loginWithGoogle, loginWithGithub } = useAuth()
  const addToast = useToast()
  const navigate = useNavigate()

  useEffect(() => {
    let score = 0
    if (password.length > 7) score += 1
    if (/[A-Z]/.test(password) && /[a-z]/.test(password)) score += 1
    if (/\d/.test(password)) score += 1
    if (/[^A-Za-z0-9]/.test(password)) score += 1
    setPasswordScore(score)
  }, [password])

  useEffect(() => {
    // Entrance reveal animation
    const elements = document.querySelectorAll('.reveal')
    requestAnimationFrame(() => {
      elements.forEach(el => el.classList.add('in-view'))
    })
  }, [])

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!fullName.trim() || fullName.trim().split(' ')[0].length < 2) {
      return addToast('Please enter a valid first name (min 2 chars)', 'error')
    }
    if (!email.trim()) return addToast('Please enter your email', 'error')
    if (passwordScore < 1 || password.length < 8) return addToast('Please use a stronger password (min 8 chars)', 'error')
    if (!agreeTerms) return addToast('You must agree to the terms', 'error')
    
    // Split full name to match backend RegisterRequest DTO
    const nameParts = fullName.trim().split(' ')
    const firstName = nameParts[0]
    const lastName = nameParts.length > 1 ? nameParts.slice(1).join(' ') : 'User'
    
    setLoading(true)
    try {
      // 1. Create user in Firebase Auth (required for Firebase password resets to work)
      try {
        await createUserWithEmailAndPassword(auth, email.trim().toLowerCase(), password)
      } catch (firebaseErr) {
        if (firebaseErr.code === 'auth/email-already-in-use') {
          // If already in Firebase but not in our DB, we can proceed.
          // If in our DB too, the backend call below will catch it.
        } else {
          throw firebaseErr
        }
      }

      // 2. Create user in Spring Boot/MySQL (our source of truth)
      await register({
        firstName,
        lastName: lastName.length < 2 ? 'User' : lastName,
        email: email.trim().toLowerCase(),
        password
      })
      // useAuth.register now stores the JWT — navigate directly to dashboard
      navigate('/')
    } catch (err) {
      console.error('Registration failed:', err)
      addToast('Registration failed. Email might be in use.', 'error')
    } finally {
      setLoading(false)
    }
  }

  const strengthColors = ['#f43f5e', '#f59e0b', '#10b981', '#059669']
  const strengthText = ['Weak', 'Fair', 'Good', 'Strong']

  return (
    <div className="h-screen overflow-hidden grid lg:grid-cols-[58%_42%] bg-surface-container-lowest font-['Manrope',sans-serif]">
      
      {/* LEFT: BRAND / EDITORIAL PANEL (Matches Login Layout) */}
      <div className="relative hidden lg:flex flex-col justify-between p-6 lg:p-10 overflow-hidden bg-[#fafcff] border-r border-outline-variant/30 h-full">
        
        {/* Subtle geometric background */}
        <div 
          className="absolute inset-0 pointer-events-none opacity-[0.03]"
          style={{
            backgroundImage: `linear-gradient(to right, #0058be 1px, transparent 1px), linear-gradient(to bottom, #0058be 1px, transparent 1px)`,
            backgroundSize: '40px 40px'
          }}
        ></div>
        <div className="absolute inset-0 bg-gradient-to-br from-transparent via-[#fafcff]/80 to-[#fafcff] pointer-events-none"></div>

        <Link to="/" className="relative z-10 flex items-center w-fit">
          <img src="/logo.png" alt="CodeSentry" className="h-[44px] w-auto" />
        </Link>

        {/* Editorial centerpiece */}
        <div className="relative z-10 flex-1 flex flex-col justify-center max-w-[520px] mx-auto w-full py-6">
          
          <h2 className="font-headline-lg text-[34px] leading-[1.15] font-extrabold tracking-tight text-on-surface mb-4">
            Secure software starts with <span className="text-primary">trusted intelligence.</span>
          </h2>
          
          <p className="font-body-md text-[14px] text-on-surface-variant leading-relaxed mb-6">
            Scan source code, detect vulnerabilities, generate professional security reports, and receive AI-powered remediation guidance—all from one unified security platform.
          </p>

          <div className="space-y-3">
            <div className="group flex items-start gap-4 p-3 rounded-2xl bg-white/70 backdrop-blur-md border border-outline-variant/40 hover:border-primary/25 hover:bg-white/90 transition-all duration-200 cursor-default">
              <div className="w-[40px] h-[40px] rounded-[12px] bg-primary/5 border border-primary/10 flex items-center justify-center shrink-0 group-hover:bg-primary/8 group-hover:border-primary/20 transition-colors">
                <LuFileSearch className="text-[20px] text-primary" strokeWidth={1.5} />
              </div>
              <div className="pt-0.5">
                <h3 className="font-headline-md text-[15px] font-bold text-on-surface mb-1">Hybrid Static Analysis</h3>
                <p className="font-body-md text-[14px] text-on-surface-variant leading-relaxed">Rule-based vulnerability detection combined with AI-powered explanations.</p>
              </div>
            </div>
            
            <div className="group flex items-start gap-4 p-3 rounded-2xl bg-white/70 backdrop-blur-md border border-outline-variant/40 hover:border-primary/25 hover:bg-white/90 transition-all duration-200 cursor-default">
              <div className="w-[40px] h-[40px] rounded-[12px] bg-primary/5 border border-primary/10 flex items-center justify-center shrink-0 group-hover:bg-primary/8 group-hover:border-primary/20 transition-colors">
                <LuMessageSquare className="text-[20px] text-primary" strokeWidth={1.5} />
              </div>
              <div className="pt-0.5">
                <h3 className="font-headline-md text-[15px] font-bold text-on-surface mb-1">AI Security Assistant</h3>
                <p className="font-body-md text-[14px] text-on-surface-variant leading-relaxed">Understand findings instantly with contextual remediation guidance.</p>
              </div>
            </div>

            <div className="group flex items-start gap-4 p-3 rounded-2xl bg-white/70 backdrop-blur-md border border-outline-variant/40 hover:border-primary/25 hover:bg-white/90 transition-all duration-200 cursor-default">
              <div className="w-[40px] h-[40px] rounded-[12px] bg-primary/5 border border-primary/10 flex items-center justify-center shrink-0 group-hover:bg-primary/8 group-hover:border-primary/20 transition-colors">
                <LuClipboardCheck className="text-[20px] text-primary" strokeWidth={1.5} />
              </div>
              <div className="pt-0.5">
                <h3 className="font-headline-md text-[15px] font-bold text-on-surface mb-1">Professional Reports</h3>
                <p className="font-body-md text-[14px] text-on-surface-variant leading-relaxed">Generate executive-ready PDF security assessments with confidence.</p>
              </div>
            </div>
          </div>
        </div>

        <div className="relative z-10 flex items-center gap-3">
          <div className="flex -space-x-2">
            <div className="w-8 h-8 rounded-full bg-surface-container-high border-2 border-[#fafcff] flex items-center justify-center text-[10px] font-bold text-on-surface shadow-sm">S</div>
            <div className="w-8 h-8 rounded-full bg-surface-container-high border-2 border-[#fafcff] flex items-center justify-center text-[10px] font-bold text-on-surface shadow-sm">D</div>
            <div className="w-8 h-8 rounded-full bg-surface-container-high border-2 border-[#fafcff] flex items-center justify-center text-[10px] font-bold text-on-surface shadow-sm">E</div>
          </div>
          <p className="font-label-md text-[13px] text-on-surface-variant">
            Designed for secure software development from classroom projects to enterprise workflows.
          </p>
        </div>
      </div>

      {/* RIGHT: REGISTER FORM */}
      <div className="flex flex-col items-center justify-center p-4 md:p-6 relative bg-[#f9f9ff] h-full overflow-y-auto">
        <div className="absolute inset-0 lg:hidden" style={{ backgroundColor: '#f9f9ff', backgroundImage: 'radial-gradient(at 100% 0%, rgba(33, 112, 228, 0.08) 0px, transparent 50%)' }}></div>

        <div className="w-full max-w-[380px] relative z-10 py-4">
          {/* Mobile-only logo */}
          <div className="flex lg:hidden items-center gap-sm mb-6">
            <Link to="/" className="flex items-center">
              <img src="/logo.png" alt="CodeSentry" className="h-[44px] w-auto" />
            </Link>
          </div>

          <h1 className="font-headline-lg text-[26px] text-on-surface font-extrabold tracking-tight text-center md:text-left">Create your account</h1>
          <p className="font-body-md text-[13.5px] text-on-surface-variant mt-1 text-center md:text-left mb-4">Start scanning files, links, and code in minutes — free.</p>

          {/* Social sign-up */}
          <div className="grid grid-cols-2 gap-3 mt-0">
            <button
              type="button"
              onClick={async () => {
                setLoading(true);
                try { await loginWithGoogle(); navigate('/'); }
                catch { addToast('Google registration failed', 'error'); }
                finally { setLoading(false); }
              }}
              className="flex items-center justify-center gap-sm border border-outline-variant bg-surface-container-lowest rounded-lg py-sm font-label-md text-[13px] font-semibold text-on-surface-variant hover:bg-surface-container-low hover:border-outline transition-colors"
            >
              <FcGoogle className="text-[16px]" /> Google
            </button>
            <button
              type="button"
              onClick={async () => {
                setLoading(true);
                try { await loginWithGithub(); navigate('/'); }
                catch { addToast('GitHub registration failed', 'error'); }
                finally { setLoading(false); }
              }}
              className="flex items-center justify-center gap-sm border border-outline-variant bg-surface-container-lowest rounded-lg py-sm font-label-md text-[13px] font-semibold text-on-surface-variant hover:bg-surface-container-low hover:border-outline transition-colors"
            >
              <FaGithub className="text-[16px]" /> GitHub
            </button>
          </div>

          <div className="flex items-center gap-4 my-4">
            <div className="h-px bg-outline-variant flex-1"></div>
            <span className="font-label-md text-[12px] text-on-surface-variant">or continue with email</span>
            <div className="h-px bg-outline-variant flex-1"></div>
          </div>

          <form onSubmit={handleSubmit} noValidate>
            <div className="field-group mb-2.5">
              <input
                type="text"
                id="full-name"
                name="full-name"
                placeholder=" "
                className="field-input"
                autoComplete="name"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                required
              />
              <label htmlFor="full-name" className="field-label">Full name</label>
            </div>

            <div className="field-group mb-2.5">
              <input
                type="email"
                id="email"
                name="email"
                placeholder=" "
                className="field-input"
                autoComplete="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
              <label htmlFor="email" className="field-label">Work email</label>
            </div>

            <div className="field-group mb-2 relative">
              <input
                type={showPassword ? 'text' : 'password'}
                id="password"
                name="password"
                placeholder=" "
                className="field-input has-toggle"
                autoComplete="new-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
              <label htmlFor="password" className="field-label">Password</label>
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-[14px] top-[22px] -translate-y-1/2 text-on-surface-variant hover:text-on-surface transition-colors"
                aria-label={showPassword ? "Hide password" : "Show password"}
              >
                {showPassword ? <LuEyeOff className="text-[18px]" strokeWidth={2} /> : <LuEye className="text-[18px]" strokeWidth={2} />}
              </button>
            </div>

            {/* Strength meter */}
            <div className="grid grid-cols-4 gap-[6px] h-1 mb-1">
              {[1, 2, 3, 4].map(n => (
                <div key={n} className="bg-[#e2e6f0] rounded-full overflow-hidden h-full">
                  <div 
                    className="h-full rounded-full transition-all duration-300"
                    style={{ 
                      width: passwordScore >= 1 && n <= passwordScore ? '100%' : '0%',
                      backgroundColor: strengthColors[Math.min(passwordScore, 4) - 1]
                    }}
                  ></div>
                </div>
              ))}
            </div>
            
            <div className="flex items-center justify-between mb-2.5">
              <p 
                className="font-label-md text-[12px]" 
                style={{ color: passwordScore <= 0 ? '#424754' : strengthColors[Math.min(passwordScore, 4) - 1] }}
              >
                {passwordScore <= 0 ? 'Use 8+ characters with a number and symbol' : strengthText[Math.min(passwordScore, 4) - 1]}
              </p>
            </div>

            <label className="flex items-start gap-sm mb-4 cursor-pointer w-fit select-none">
              <input 
                type="checkbox" 
                className="w-[18px] h-[18px] mt-[1px] rounded border-[#c2c6d6] text-[#0058be] focus:ring-[#0058be]" 
                checked={agreeTerms}
                onChange={(e) => setAgreeTerms(e.target.checked)}
              />
              <span className="font-body-md text-[14px] text-on-surface-variant leading-snug">
                I agree to the <a href="#" className="text-primary font-semibold hover:underline">Terms of Service</a> and <a href="#" className="text-primary font-semibold hover:underline">Privacy Policy</a>
              </span>
            </label>

            <button
              type="submit"
              disabled={loading}
              className="cta-magnetic w-full bg-primary text-on-primary py-2.5 rounded-lg font-label-md text-[13px] font-semibold tracking-[0.005em] shadow-[inset_0_1px_0_rgba(255,255,255,0.2),0_1px_2px_rgba(0,0,0,0.1)] hover:bg-[#004395] flex items-center justify-center gap-sm"
            >
              {loading ? <span className="spinner"></span> : 'Create account'}
            </button>
          </form>

          <p className="font-body-md text-[13.5px] text-on-surface-variant text-center mt-4">
            Already have an account? <Link to="/login" className="text-primary font-semibold hover:underline">Sign in</Link>
          </p>

          <p className="flex items-center justify-center gap-1.5 font-label-md text-[11.5px] text-on-surface-variant mt-4">
            <span className="material-symbols-outlined text-[14px] text-tertiary-container">lock</span>
            Your credentials are encrypted end-to-end
          </p>
        </div>
      </div>
    </div>
  )
}

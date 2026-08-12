import { useState } from 'react'
import { useAuth } from '../../hooks/useAuth'
import { useToast } from '../../hooks/useToast'
import { useTheme } from '../../hooks/useTheme'
import api from '../../api/axiosConfig'
import LoadingSpinner from '../../components/ui/LoadingSpinner'
import ConfirmDialog from '../../components/ui/ConfirmDialog'

export default function Settings() {
  const { user, logout, updateUser } = useAuth()
  const addToast = useToast()
  const { isDark, toggle } = useTheme()

  // ── Account info ────────────────────────────────────────────────────────────
  const [profileForm, setProfileForm] = useState({
    firstName: user?.firstName ?? '',
    lastName:  user?.lastName  ?? '',
  })
  const [savingProfile, setSavingProfile] = useState(false)

  // ── Password ────────────────────────────────────────────────────────────────
  const [pwdForm, setPwdForm] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' })
  const [showPwd, setShowPwd] = useState({ current: false, new: false, confirm: false })
  const [savingPwd, setSavingPwd] = useState(false)

  // ── Handlers ────────────────────────────────────────────────────────────────
  const handleProfileSave = async (e) => {
    e.preventDefault()
    setSavingProfile(true)
    try {
      await api.put('/auth/profile', {
        firstName: profileForm.firstName.trim(),
        lastName:  profileForm.lastName.trim(),
      })
      addToast('Profile updated successfully', 'success')
      updateUser({ firstName: profileForm.firstName.trim(), lastName: profileForm.lastName.trim() })
    } catch {
      addToast(err.response?.data?.message || 'Failed to update profile', 'error')
    } finally {
      setSavingProfile(false)
    }
  }

  const handlePasswordChange = async (e) => {
    e.preventDefault()
    if (pwdForm.newPassword !== pwdForm.confirmPassword) {
      return addToast('New passwords do not match', 'error')
    }
    if (pwdForm.newPassword.length < 8) {
      return addToast('Password must be at least 8 characters', 'error')
    }
    setSavingPwd(true)
    try {
      await api.put('/auth/change-password', {
        currentPassword: pwdForm.currentPassword,
        newPassword:     pwdForm.newPassword,
      })
      addToast('Password changed successfully', 'success')
      setPwdForm({ currentPassword: '', newPassword: '', confirmPassword: '' })
    } catch {
      addToast(err.response?.data?.message || 'Failed to change password. Check current password.', 'error')
    } finally {
      setSavingPwd(false)
    }
  }

  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)

  const handleDeleteAccount = () => {
    setShowDeleteConfirm(true)
  }

  const handleDeleteAccountConfirm = () => {
    setShowDeleteConfirm(false)
    addToast('Account deletion is not yet enabled via API.', 'warning')
  }

  const PwdToggle = ({ field }) => (
    <button
      type="button"
      onClick={() => setShowPwd(p => ({ ...p, [field]: !p[field] }))}
      className="absolute right-3 top-1/2 -translate-y-1/2 text-outline hover:text-on-surface transition-colors"
    >
      <span className="material-symbols-outlined text-[18px]">
        {showPwd[field] ? 'visibility_off' : 'visibility'}
      </span>
    </button>
  )

  return (
    <div className="flex-1 max-w-5xl mx-auto w-full flex flex-col gap-lg">
      <div className="mb-md">
        <h2 className="font-headline-lg-mobile md:font-headline-lg text-[24px] md:text-[32px] text-on-surface mb-xs font-black tracking-tight">Settings</h2>
        <p className="font-body-lg text-[16px] text-on-surface-variant">Manage your account and application preferences.</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-lg">
        {/* Main Settings Column */}
        <div className="lg:col-span-2 flex flex-col gap-lg">
          
          {/* Account Information Card */}
          <div className="bg-surface-container-lowest border border-outline-variant/50 rounded-xl p-lg shadow-sm hover:shadow-md transition-shadow duration-300">
            <div className="flex items-center justify-between mb-xl border-b border-outline-variant/50 pb-sm">
              <h3 className="font-title-lg text-[18px] text-on-surface font-bold">Account Information</h3>
            </div>
            
            <form onSubmit={handleProfileSave} className="space-y-md">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-md">
                <div>
                  <label className="block font-label-md text-[13px] text-on-surface-variant mb-xs">First Name</label>
                  <input 
                    className="w-full h-[40px] px-sm bg-surface-bright border border-outline-variant/50 rounded-DEFAULT font-body-md text-[14px] text-on-surface focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary transition-colors" 
                    type="text" 
                    required
                    value={profileForm.firstName}
                    onChange={e => setProfileForm(p => ({ ...p, firstName: e.target.value }))}
                  />
                </div>
                <div>
                  <label className="block font-label-md text-[13px] text-on-surface-variant mb-xs">Last Name</label>
                  <input 
                    className="w-full h-[40px] px-sm bg-surface-bright border border-outline-variant/50 rounded-DEFAULT font-body-md text-[14px] text-on-surface focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary transition-colors" 
                    type="text" 
                    required
                    value={profileForm.lastName}
                    onChange={e => setProfileForm(p => ({ ...p, lastName: e.target.value }))}
                  />
                </div>
              </div>
              
              <div>
                <label className="block font-label-md text-[13px] text-on-surface-variant mb-xs">Email Address</label>
                <input 
                  className="w-full h-[40px] px-sm bg-surface-container-low border border-outline-variant/50 rounded-DEFAULT font-body-md text-[14px] text-on-surface-variant cursor-not-allowed" 
                  disabled 
                  type="email" 
                  value={user?.email || ''}
                />
                <p className="mt-1 font-label-md text-[11px] text-outline">Email cannot be changed from settings.</p>
              </div>

              <div className="mt-xl flex justify-end">
                <button 
                  type="submit" 
                  disabled={savingProfile} 
                  className="bg-primary text-on-primary px-lg py-2 rounded-DEFAULT font-label-md text-[13px] font-semibold hover:bg-surface-tint transition-colors shadow-[inset_0_1px_0_rgba(255,255,255,0.2),0_1px_2px_rgba(0,0,0,0.1)] flex items-center justify-center disabled:opacity-50"
                >
                  {savingProfile ? <LoadingSpinner size="sm" /> : 'Save Changes'}
                </button>
              </div>
            </form>
          </div>

          {/* Change Password Card */}
          <div className="bg-surface-container-lowest border border-outline-variant/50 rounded-xl p-lg shadow-sm hover:shadow-md transition-shadow duration-300">
            <div className="mb-xl border-b border-outline-variant/50 pb-sm">
              <h3 className="font-title-lg text-[18px] text-on-surface font-bold">Change Password</h3>
            </div>
            
            <form onSubmit={handlePasswordChange} className="space-y-md">
              <div>
                <label className="block font-label-md text-[13px] text-on-surface-variant mb-xs">Current Password</label>
                <div className="relative">
                  <input 
                    className="w-full h-[40px] px-sm bg-surface-bright border border-outline-variant/50 rounded-DEFAULT font-body-md text-[14px] text-on-surface focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary transition-colors pr-10" 
                    placeholder="••••••••" 
                    type={showPwd.current ? 'text' : 'password'}
                    required
                    value={pwdForm.currentPassword}
                    onChange={e => setPwdForm(p => ({ ...p, currentPassword: e.target.value }))}
                  />
                  <PwdToggle field="current" />
                </div>
              </div>
              
              <div className="grid grid-cols-1 md:grid-cols-2 gap-md">
                <div>
                  <label className="block font-label-md text-[13px] text-on-surface-variant mb-xs">New Password</label>
                  <div className="relative">
                    <input 
                      className="w-full h-[40px] px-sm bg-surface-bright border border-outline-variant/50 rounded-DEFAULT font-body-md text-[14px] text-on-surface focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary transition-colors pr-10" 
                      type={showPwd.new ? 'text' : 'password'}
                      required
                      minLength={8}
                      placeholder="Min. 8 characters"
                      value={pwdForm.newPassword}
                      onChange={e => setPwdForm(p => ({ ...p, newPassword: e.target.value }))}
                    />
                    <PwdToggle field="new" />
                  </div>
                </div>
                <div>
                  <label className="block font-label-md text-[13px] text-on-surface-variant mb-xs">Confirm Password</label>
                  <div className="relative">
                    <input 
                      className="w-full h-[40px] px-sm bg-surface-bright border border-outline-variant/50 rounded-DEFAULT font-body-md text-[14px] text-on-surface focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary transition-colors pr-10" 
                      type={showPwd.confirm ? 'text' : 'password'}
                      required
                      placeholder="••••••••"
                      value={pwdForm.confirmPassword}
                      onChange={e => setPwdForm(p => ({ ...p, confirmPassword: e.target.value }))}
                    />
                    <PwdToggle field="confirm" />
                  </div>
                </div>
              </div>
              
              <div className="mt-xl flex justify-end">
                <button 
                  type="submit" 
                  disabled={savingPwd} 
                  className="bg-surface-container-lowest text-on-surface border border-outline-variant px-lg py-2 rounded-DEFAULT font-label-md text-[13px] font-semibold hover:bg-surface-container-low transition-colors shadow-sm disabled:opacity-50"
                >
                  {savingPwd ? <LoadingSpinner size="sm" /> : 'Update Password'}
                </button>
              </div>
            </form>
          </div>
        </div>

        {/* Side Settings Column */}
        <div className="flex flex-col gap-lg">
          
          {/* Appearance Card */}
          <div className="bg-surface-container-lowest border border-outline-variant/50 rounded-xl p-lg shadow-sm hover:shadow-md transition-shadow duration-300">
            <div className="mb-md border-b border-outline-variant/50 pb-sm">
              <h3 className="font-title-lg text-[18px] text-on-surface font-bold">Appearance</h3>
            </div>
            <div className="grid grid-cols-1 gap-md">
              <div className="border-2 border-primary bg-primary-container/20 flex items-center justify-between p-md rounded-lg">
                <div className="flex items-center gap-sm">
                  <span className="material-symbols-outlined text-[22px] text-primary" style={{ fontVariationSettings: "'FILL' 1" }}>light_mode</span>
                  <span className="font-label-md text-[13.5px] text-primary font-semibold">Light Mode</span>
                </div>
                <span className="text-[10px] text-primary font-bold uppercase tracking-wide bg-primary/10 px-2 py-0.5 rounded-full">Active</span>
              </div>
            </div>
          </div>

          {/* Preferences Card */}
          <div className="bg-surface-container-lowest border border-outline-variant/50 rounded-xl p-lg shadow-sm hover:shadow-md transition-shadow duration-300">
            <div className="mb-md border-b border-outline-variant/50 pb-sm">
              <h3 className="font-title-lg text-[18px] text-on-surface font-bold">Preferences</h3>
            </div>
            <div className="space-y-md">
              <label className="flex items-center justify-between cursor-pointer group">
                <span className="font-body-md text-[14px] text-on-surface group-hover:text-primary transition-colors">Remember Me</span>
                <div className="relative">
                  <input defaultChecked className="sr-only" type="checkbox"/>
                  <div className="w-10 h-6 bg-primary rounded-full shadow-[inset_0_1px_3px_rgba(0,0,0,0.2)] transition-colors"></div>
                  <div className="absolute left-1 top-1 w-4 h-4 bg-white rounded-full transition-transform translate-x-4 shadow-sm"></div>
                </div>
              </label>
              <label className="flex items-center justify-between cursor-pointer group">
                <span className="font-body-md text-[14px] text-on-surface group-hover:text-primary transition-colors">Auto Save Preferences</span>
                <div className="relative">
                  <input className="sr-only" type="checkbox"/>
                  <div className="w-10 h-6 bg-outline-variant rounded-full shadow-[inset_0_1px_3px_rgba(0,0,0,0.2)] transition-colors"></div>
                  <div className="absolute left-1 top-1 w-4 h-4 bg-white rounded-full transition-transform shadow-sm"></div>
                </div>
              </label>
            </div>
          </div>

          {/* Account Actions Card */}
          <div className="bg-surface-container-lowest border border-outline-variant/50 rounded-xl p-lg shadow-sm hover:shadow-md transition-shadow duration-300">
            <div className="mb-md border-b border-outline-variant/50 pb-sm">
              <h3 className="font-title-lg text-[18px] text-on-surface font-bold">Account Actions</h3>
            </div>
            <div className="flex flex-col gap-sm">
              <button onClick={logout} className="w-full flex justify-center items-center gap-xs bg-surface-container-lowest text-on-surface border border-outline-variant px-md py-2 rounded-DEFAULT font-label-md text-[13px] font-semibold hover:bg-surface-container-low transition-colors">
                <span className="material-symbols-outlined text-[18px]">logout</span> Logout
              </button>
              <button onClick={handleDeleteAccount} className="w-full flex justify-center items-center gap-xs bg-error-container text-on-error-container border border-error/20 px-md py-2 rounded-DEFAULT font-label-md text-[13px] font-semibold hover:bg-error-container/80 transition-colors">
                <span className="material-symbols-outlined text-[18px]">delete_forever</span> Delete Account
              </button>
            </div>
          </div>

        </div>
      </div>
      <ConfirmDialog
        open={showDeleteConfirm}
        title="Delete Account"
        message="Your account and all associated data will be permanently deleted. This action cannot be undone."
        confirmLabel="Delete Account"
        variant="danger"
        onConfirm={handleDeleteAccountConfirm}
        onCancel={() => setShowDeleteConfirm(false)}
      />
    </div>
  )
}

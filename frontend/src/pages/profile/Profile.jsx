import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'
import { useToast } from '../../hooks/useToast'
import api from '../../api/axiosConfig'
import LoadingSpinner from '../../components/ui/LoadingSpinner'

export default function Profile() {
  const { user, updateUser } = useAuth()
  const addToast = useToast()

  const [saving, setSaving] = useState(false)
  const [form, setForm] = useState({
    firstName: user?.firstName ?? '',
    lastName:  user?.lastName  ?? '',
  })
  
  const displayName = `${user?.firstName ?? ''} ${user?.lastName ?? ''}`.trim() || user?.email || 'User'
  const initials = user?.firstName
    ? `${user.firstName[0]}${user.lastName?.[0] ?? ''}`.toUpperCase()
    : 'U'

  const handleSave = async () => {
    setSaving(true)
    try {
      await api.put('/auth/profile', {
        firstName: form.firstName.trim(),
        lastName:  form.lastName.trim(),
      })
      addToast('Profile updated!', 'success')
      // Note: We use the existing state update to prevent window.reload as requested
      updateUser({ firstName: form.firstName.trim(), lastName: form.lastName.trim() })
    } catch {
      addToast(err.response?.data?.message || 'Failed to update profile', 'error')
    } finally {
      setSaving(false)
    }
  }

  const isFormChanged = form.firstName !== (user?.firstName || '') || form.lastName !== (user?.lastName || '')

  return (
    <div className="flex flex-col gap-xl">
      {/* Page Header */}
      <div className="mb-md">
        <h2 className="font-headline-lg-mobile md:font-headline-lg text-[24px] md:text-[32px] text-on-surface mb-xs font-black tracking-tight">
          My Profile
        </h2>
        <p className="font-body-lg text-[16px] text-on-surface-variant">View and update your account information.</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-lg">
        {/* Left Column: Profile Card & Status */}
        <div className="lg:col-span-1 flex flex-col gap-lg">
          {/* Profile Summary Card */}
          <div className="bg-surface-container-lowest rounded-xl border border-outline-variant/50 p-lg shadow-sm flex flex-col items-center text-center">
            <div className="w-24 h-24 rounded-full border-4 border-surface-container-highest flex items-center justify-center bg-primary-container text-on-primary-container mb-md shadow-sm font-title-lg text-[36px]">
              {initials}
            </div>
            <h3 className="font-title-lg text-[18px] text-on-surface mb-xs font-bold">{displayName}</h3>
            <p className="font-body-md text-[14px] text-on-surface-variant mb-md">{user?.email}</p>
            
            <div className="w-full border-t border-outline-variant/50 pt-md flex justify-between items-center">
              <div className="text-left">
                <span className="block font-label-md text-[13px] text-outline">Role</span>
                <span className="font-body-md text-[14px] text-on-surface font-medium capitalize">{user?.role?.toLowerCase() ?? 'User'}</span>
              </div>
              <div className="text-right">
                <span className="block font-label-md text-[13px] text-outline">Status</span>
                <span className="font-body-md text-[14px] text-on-surface font-medium">
                  {user?.active ? 'Active' : 'Inactive'}
                </span>
              </div>
            </div>
          </div>

          {/* Account Info Card (Read-only) */}
          <div className="bg-surface-container-lowest rounded-xl border border-outline-variant/50 p-lg shadow-sm">
            <h4 className="font-title-lg text-[18px] text-on-surface mb-md pb-xs border-b border-outline-variant/50 font-bold">Account Details</h4>
            
            <div className="flex justify-between items-center mb-md">
              <span className="font-label-md text-[13px] text-on-surface-variant">User ID</span>
              <span className="font-code text-[13px] text-on-surface bg-surface-container-low px-2 py-1 rounded-DEFAULT border border-outline-variant/50">
                {user?.id ? `AN-${user.id}` : 'AN-00000'}
              </span>
            </div>
            
            <div className="flex justify-between items-center">
              <span className="font-label-md text-[13px] text-on-surface-variant">Account Status</span>
              <span className={`inline-flex items-center gap-xs px-2 py-1 rounded-full font-label-md text-[12px] border ${
                user?.active 
                  ? 'bg-tertiary-container/10 text-tertiary-container border-tertiary-container/20' 
                  : 'bg-error-container/10 text-error border-error-container/20'
              }`}>
                <span className={`w-2 h-2 rounded-full ${user?.active ? 'bg-tertiary-container' : 'bg-error'}`}></span>
                {user?.active ? 'Active' : 'Inactive'}
              </span>
            </div>
          </div>
        </div>

        {/* Right Column: Editable Forms & Actions */}
        <div className="lg:col-span-2 flex flex-col gap-lg">
          {/* Edit Information Form */}
          <div className="bg-surface-container-lowest rounded-xl border border-outline-variant/50 p-lg shadow-sm flex-1 flex flex-col">
            <div className="flex justify-between items-center mb-xl pb-sm border-b border-outline-variant/50">
              <h4 className="font-title-lg text-[18px] text-on-surface font-bold">Personal Information</h4>
            </div>
            
            <div className="space-y-lg flex-1">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-md">
                <div>
                  <label className="block font-label-md text-[13px] text-on-surface-variant mb-xs" htmlFor="firstName">First Name</label>
                  <input 
                    className="w-full h-[40px] px-sm bg-surface-bright border border-outline-variant/50 rounded-DEFAULT font-body-md text-[14px] text-on-surface focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary transition-all" 
                    id="firstName" 
                    type="text" 
                    value={form.firstName}
                    onChange={(e) => setForm({ ...form, firstName: e.target.value })}
                  />
                </div>
                <div>
                  <label className="block font-label-md text-[13px] text-on-surface-variant mb-xs" htmlFor="lastName">Last Name</label>
                  <input 
                    className="w-full h-[40px] px-sm bg-surface-bright border border-outline-variant/50 rounded-DEFAULT font-body-md text-[14px] text-on-surface focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary transition-all" 
                    id="lastName" 
                    type="text" 
                    value={form.lastName}
                    onChange={(e) => setForm({ ...form, lastName: e.target.value })}
                  />
                </div>
              </div>
              
              <div>
                <label className="block font-label-md text-[13px] text-on-surface-variant mb-xs" htmlFor="email">Email Address</label>
                <input 
                  className="w-full h-[40px] px-sm bg-surface-container-low border border-outline-variant/50 rounded-DEFAULT font-body-md text-[14px] text-on-surface-variant cursor-not-allowed" 
                  disabled 
                  id="email" 
                  type="email" 
                  value={user?.email || ''}
                />
                <p className="mt-1 font-label-md text-[11px] text-outline">Contact IT support to change your primary email address.</p>
              </div>
              
              <div className="mt-auto pt-lg border-t border-outline-variant/50 flex flex-col sm:flex-row gap-md justify-between items-center">
                <Link to="/settings" className="w-full sm:w-auto px-lg py-2 bg-surface-container-lowest border border-outline-variant text-on-surface rounded-DEFAULT font-label-md text-[13px] hover:bg-surface-container-low transition-colors shadow-sm flex items-center justify-center gap-xs font-semibold">
                  <span className="material-symbols-outlined text-[18px]">lock_reset</span>
                  Change Password
                </Link>
                <div className="flex gap-sm w-full sm:w-auto">
                  <button 
                    onClick={() => setForm({ firstName: user?.firstName ?? '', lastName: user?.lastName ?? '' })}
                    disabled={!isFormChanged || saving}
                    className="flex-1 sm:flex-none px-lg py-2 bg-surface-container-lowest border border-outline-variant text-on-surface rounded-DEFAULT font-label-md text-[13px] hover:bg-surface-container-low transition-colors shadow-sm font-semibold disabled:opacity-50" 
                    type="button"
                  >
                    Cancel
                  </button>
                  <button 
                    onClick={handleSave}
                    disabled={!isFormChanged || saving}
                    className="flex-1 sm:flex-none flex items-center justify-center gap-2 px-lg py-2 bg-primary text-on-primary rounded-DEFAULT font-label-md text-[13px] hover:bg-surface-tint transition-colors shadow-[inset_0_1px_0_rgba(255,255,255,0.2),0_1px_2px_rgba(0,0,0,0.1)] font-semibold disabled:opacity-50 disabled:cursor-not-allowed" 
                    type="button"
                  >
                    {saving ? <LoadingSpinner size="sm" /> : 'Save Changes'}
                  </button>
                </div>
              </div>
            </div>
          </div>

          {/* Quick Navigation */}
          <div className="bg-primary-container/5 rounded-xl border border-primary-container/20 p-lg flex flex-col sm:flex-row items-center justify-between gap-md">
            <div>
              <h5 className="font-title-lg text-[18px] text-on-surface mb-xs font-bold">Ready to scan?</h5>
              <p className="font-body-md text-[14px] text-on-surface-variant">Return to the dashboard to initiate a new security analysis.</p>
            </div>
            <Link to="/" className="whitespace-nowrap px-lg py-2 bg-primary-container text-on-primary-container rounded-DEFAULT font-label-md text-[13px] font-semibold hover:bg-surface-tint transition-colors shadow-[inset_0_1px_0_rgba(255,255,255,0.2),0_1px_2px_rgba(0,0,0,0.1)] flex items-center gap-xs">
              Go to Dashboard
              <span className="material-symbols-outlined text-[18px]">arrow_forward</span>
            </Link>
          </div>
        </div>
      </div>
    </div>
  )
}

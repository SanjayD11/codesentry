import api from './axiosConfig'

export const login          = (data)              => api.post('/auth/login', data)
export const register       = (data)              => api.post('/auth/register', data)
export const getMe          = ()                  => api.get('/auth/me')
export const firebaseLogin  = (token)             => api.post('/auth/firebase-login', { token })
export const forgotPassword = (email)             => api.post('/auth/forgot-password', { email })

/**
 * Reset password.
 *
 * @param {string} token       - the native reset token from the URL
 * @param {string} newPassword - the user's chosen new password
 */
export const resetPassword = (token, newPassword) =>
  api.post('/auth/reset-password', { token, newPassword })


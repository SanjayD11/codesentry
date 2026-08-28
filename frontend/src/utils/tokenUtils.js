/**
 * Utility functions for safely inspecting and validating JWT tokens on the client side.
 */

/**
 * Decodes the payload portion of a JWT string safely.
 * Handles standard Base64 as well as Base64URL encoding with UTF-8 support.
 *
 * @param {string} token - The raw JWT token string
 * @returns {object|null} The parsed payload object, or null if invalid
 */
export function parseJwt(token) {
  if (!token || typeof token !== 'string') return null
  const parts = token.split('.')
  if (parts.length !== 3) return null

  try {
    const base64Url = parts[1]
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    )
    return JSON.parse(jsonPayload)
  } catch {
    try {
      // Fallback for simple ASCII payloads if UTF-8 decoder throws
      const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
      return JSON.parse(atob(base64))
    } catch {
      return null
    }
  }
}

/**
 * Checks if a JWT token is missing, malformed, or past its expiration time.
 * Includes a 5-second buffer to account for minor clock skew.
 *
 * @param {string} token - The JWT string
 * @returns {boolean} True if expired or invalid; false if still valid
 */
export function isTokenExpired(token) {
  if (!token) return true
  const payload = parseJwt(token)
  if (!payload || typeof payload.exp !== 'number') return true

  // exp is in epoch seconds; compare against current epoch milliseconds
  const bufferMs = 5000 // 5-second safety buffer
  return Date.now() >= (payload.exp * 1000 - bufferMs)
}

/**
 * Clears authentication tokens and cached user data from localStorage.
 */
export function clearAuthStorage() {
  try {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  } catch (e) {
    console.error('Failed to clear auth storage:', e)
  }
}

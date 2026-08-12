/* eslint-disable react/only-export-components */
import { createContext, useContext, useState, useCallback } from 'react'
import { login as loginApi, register as registerApi, firebaseLogin } from '../api/authApi'
import { auth, googleProvider, githubProvider } from '../config/firebase'
import { signInWithPopup } from 'firebase/auth'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try { return JSON.parse(localStorage.getItem('user')) } catch { return null }
  })

  const login = useCallback(async (email, password) => {
    const res  = await loginApi({ email, password })
    const data = res.data.data                          // ApiResponse<AuthenticationResponse>
    // Backend returns: { accessToken, tokenType, expiresIn, user: { ... } }
    const token    = data.accessToken
    const userData = data.user  // { id, firstName, lastName, email, role, ... }
    localStorage.setItem('token', token)
    localStorage.setItem('user', JSON.stringify(userData))
    setUser(userData)
    return userData
  }, [])

  const register = useCallback(async (formData) => {
    const res = await registerApi(formData)
    const data = res.data.data   // ApiResponse<AuthenticationResponse>
    if (data?.accessToken) {
      // Registration returns a JWT — store it so the user is immediately logged in
      localStorage.setItem('token', data.accessToken)
      localStorage.setItem('user', JSON.stringify(data.user))
      setUser(data.user)
    }
    return res.data
  }, [])

  const handleFirebaseSignIn = useCallback(async (provider) => {
    try {
      const result = await signInWithPopup(auth, provider);
      const idToken = await result.user.getIdToken();
      
      // Send the Firebase ID token to our Spring Boot backend
      const res = await firebaseLogin(idToken);
      const data = res.data.data;
      
      const token = data.accessToken;
      const userData = data.user;
      
      localStorage.setItem('token', token);
      localStorage.setItem('user', JSON.stringify(userData));
      setUser(userData);
      return userData;
    } catch (error) {
      console.error("Firebase sign-in error:", error);
      throw error;
    }
  }, []);

  const loginWithGoogle = useCallback(() => handleFirebaseSignIn(googleProvider), [handleFirebaseSignIn]);
  const loginWithGithub = useCallback(() => handleFirebaseSignIn(githubProvider), [handleFirebaseSignIn]);

  const updateUser = useCallback((newUserData) => {
    setUser((prevUser) => {
      const updated = { ...prevUser, ...newUserData }
      localStorage.setItem('user', JSON.stringify(updated))
      return updated
    })
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    setUser(null)
    window.location.href = '/login'
  }, [])

  return (
    <AuthContext.Provider value={{ user, login, register, logout, updateUser, loginWithGoogle, loginWithGithub, isAuthenticated: !!user }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be inside AuthProvider')
  return ctx
}

/* eslint-disable react/only-export-components */
import { createContext, useContext, useEffect, useState } from 'react'

const ThemeContext = createContext(null)

export function ThemeProvider({ children }) {
  const [theme] = useState('light')

  useEffect(() => {
    const root = document.documentElement
    root.setAttribute('data-theme', 'light')
    root.removeAttribute('data-theme')
    root.style.colorScheme = 'light'
    localStorage.removeItem('snt-theme')
  }, [])

  const toggle = () => {}
  const isDark = false

  return (
    <ThemeContext.Provider value={{ theme: 'light', toggle, isDark: false }}>
      {children}
    </ThemeContext.Provider>
  )
}

export const useTheme = () => {
  const ctx = useContext(ThemeContext)
  if (!ctx) throw new Error('useTheme must be inside ThemeProvider')
  return ctx
}

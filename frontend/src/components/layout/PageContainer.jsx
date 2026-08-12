import React from 'react'

/**
 * Shared Responsive Page Container Utility
 * Provides standardized max-width, responsive horizontal/vertical padding,
 * and minWidth containment across all pages.
 */
export default function PageContainer({ children, className = '', style = {} }) {
  return (
    <div
      className={`page-container ${className}`}
      style={{
        width: '100%',
        maxWidth: 1280,
        margin: '0 auto',
        padding: '20px 16px 48px',
        boxSizing: 'border-box',
        minWidth: 0,
        ...style
      }}
    >
      {children}
    </div>
  )
}

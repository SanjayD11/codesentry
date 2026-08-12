export default function LoadingSpinner({ size = 'md', className = '' }) {
  const sizeClasses = {
    sm: 'w-4 h-4 border-2',
    md: 'w-8 h-8 border-3',
    lg: 'w-12 h-12 border-4',
  }

  return (
    <div className={`flex justify-center items-center ${className}`} role="status" aria-label="Loading">
      <div
        className={`${sizeClasses[size]} rounded-full border-slate-700 border-t-blue-500 animate-spin`}
      ></div>
      <span className="sr-only">Loading...</span>
    </div>
  )
}

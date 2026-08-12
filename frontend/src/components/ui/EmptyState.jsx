import React from 'react';

/**
 * A reusable empty state component for when there is no data to display.
 * 
 * @param {string} icon - Material symbols icon name (e.g., 'folder_open')
 * @param {string} title - The main heading
 * @param {string} description - A helpful subtitle explaining what to do next
 * @param {string} [actionLabel] - Optional button text
 * @param {function} [onAction] - Optional button click handler
 */
export default function EmptyState({ icon, title, description, actionLabel, onAction }) {
  return (
    <div className="flex flex-col items-center justify-center w-full min-h-[300px] p-8 text-center bg-surface-container-lowest border border-outline-variant/30 rounded-xl shadow-sm">
      <div className="w-16 h-16 rounded-full bg-secondary-container/20 flex items-center justify-center mb-4">
        <span className="material-symbols-outlined text-[32px] text-primary opacity-80">
          {icon || 'inbox'}
        </span>
      </div>
      
      <h3 className="text-title-lg font-title-lg text-on-surface mb-2 font-bold tracking-tight">
        {title}
      </h3>
      
      <p className="text-body-md text-on-surface-variant max-w-[400px] mb-6 leading-relaxed">
        {description}
      </p>
      
      {actionLabel && onAction && (
        <button
          onClick={onAction}
          className="flex items-center gap-2 px-6 py-2.5 bg-primary text-on-primary rounded-lg hover:bg-primary/90 transition-colors font-label-lg shadow-sm font-bold"
        >
          <span className="material-symbols-outlined text-[18px]">add</span>
          {actionLabel}
        </button>
      )}
    </div>
  );
}

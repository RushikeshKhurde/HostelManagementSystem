import React, { useState } from 'react';
import { RotateCw } from 'lucide-react';

export const FlipCard = ({
  frontIcon: icon,
  frontTitle,
  frontSubtitle,
  frontBadge,
  backContent,
  backActionText,
  onBackAction,
  className = '',
}) => {
  const [isFlipped, setIsFlipped] = useState(false);

  const renderIcon = () => {
    if (!icon) return null;
    if (React.isValidElement(icon)) return icon;
    if (typeof icon === 'function') {
      const IconComponent = icon;
      return <IconComponent size={24} />;
    }
    return null;
  };

  return (
    <div
      className={`flip-card-container ${isFlipped ? 'is-flipped' : ''} ${className}`}
      onClick={() => setIsFlipped(!isFlipped)}
    >
      <div className="flip-card-inner">
        {/* FRONT */}
        <div className="flip-card-front">
          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
              {icon && (
                <div
                  style={{
                    padding: '0.75rem',
                    borderRadius: 'var(--radius-lg)',
                    backgroundColor: 'var(--primary-light)',
                    color: 'var(--primary)',
                    display: 'inline-flex',
                    marginBottom: '1rem',
                  }}
                >
                  {renderIcon()}
                </div>
              )}
              {frontBadge && <span className="badge badge-info">{frontBadge}</span>}
            </div>
            <h3 style={{ fontSize: '1.125rem', marginBottom: '0.5rem' }}>{frontTitle}</h3>
            <p style={{ fontSize: '0.875rem' }}>{frontSubtitle}</p>
          </div>
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '0.375rem',
              fontSize: '0.75rem',
              fontWeight: 600,
              color: 'var(--primary)',
            }}
          >
            <RotateCw size={12} /> Hover or click to explore details
          </div>
        </div>

        {/* BACK */}
        <div className="flip-card-back">
          <div>
            <h4 style={{ fontSize: '1rem', color: 'var(--primary)', marginBottom: '0.75rem' }}>
              {frontTitle} Overview
            </h4>
            <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', lineHeight: 1.6 }}>
              {backContent}
            </div>
          </div>
          {backActionText && (
            <button
              className="btn btn-primary btn-sm"
              onClick={(e) => {
                e.stopPropagation();
                if (onBackAction) onBackAction();
              }}
              style={{ width: '100%' }}
            >
              {backActionText}
            </button>
          )}
        </div>
      </div>
    </div>
  );
};
import React from 'react';
import { AlertOctagon, RefreshCw } from 'lucide-react';
import { Button } from '../common/Button';

export const ErrorState = ({
  title = 'Failed to load content',
  message = 'An error occurred while communicating with the server.',
  onRetry,
}) => {
  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '3.5rem 1.5rem',
        textAlign: 'center',
        background: 'var(--bg-card)',
        borderRadius: 'var(--radius-lg)',
        border: '1px solid var(--danger-border)',
      }}
    >
      <div
        style={{
          padding: '1rem',
          borderRadius: 'var(--radius-full)',
          backgroundColor: 'var(--danger-light)',
          color: 'var(--danger)',
          marginBottom: '1rem',
        }}
      >
        <AlertOctagon size={36} />
      </div>
      <h3 style={{ fontSize: '1.125rem', marginBottom: '0.375rem', color: 'var(--danger)' }}>{title}</h3>
      <p style={{ fontSize: '0.875rem', maxWidth: '450px', marginBottom: onRetry ? '1.25rem' : 0 }}>
        {message}
      </p>
      {onRetry && (
        <Button variant="secondary" size="sm" icon={RefreshCw} onClick={onRetry}>
          Try Again
        </Button>
      )}
    </div>
  );
};
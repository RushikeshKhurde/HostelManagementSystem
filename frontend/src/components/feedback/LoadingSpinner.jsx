import React from 'react';
import { Loader2 } from 'lucide-react';

export const LoadingSpinner = ({ text = 'Loading data...', size = 32 }) => {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: '3rem 1rem', gap: '1rem' }}>
      <Loader2 size={size} color="var(--primary)" style={{ animation: 'spin 1s linear infinite' }} />
      <span style={{ fontSize: '0.875rem', color: 'var(--text-muted)', fontWeight: 500 }}>{text}</span>
    </div>
  );
};
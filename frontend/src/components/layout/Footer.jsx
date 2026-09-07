import React from 'react';
import { Building2, Shield, Heart } from 'lucide-react';

export const Footer = () => {
  return (
    <footer
      style={{
        backgroundColor: 'var(--bg-surface)',
        borderTop: '1px solid var(--border-color)',
        padding: '2rem 1.5rem',
        marginTop: 'auto',
      }}
    >
      <div
        style={{
          maxWidth: '1400px',
          margin: '0 auto',
          display: 'flex',
          flexDirection: 'column',
          gap: '1rem',
          alignItems: 'center',
          textAlign: 'center',
          color: 'var(--text-muted)',
          fontSize: '0.875rem',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: 'var(--text-primary)', fontWeight: 700 }}>
          <Building2 size={18} color="var(--primary)" /> SmartHostel Management System
        </div>
        <p style={{ margin: 0 }}>
          Streamlined hostel management for rooms, admissions, digital fee collection, and student welfare.
        </p>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.25rem', fontSize: '0.8125rem' }}>
          <span>© {new Date().getFullYear()} SmartHostel. All rights reserved.</span>
        </div>
      </div>
    </footer>
  );
};
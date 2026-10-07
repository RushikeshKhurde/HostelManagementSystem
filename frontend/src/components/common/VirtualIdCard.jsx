import React, { useRef, useState } from 'react';
import { Building2, ShieldCheck, Mail, Phone, Calendar, User, Printer } from 'lucide-react';
import { Button } from './Button';

export const VirtualIdCard = ({ user }) => {
  const cardRef = useRef(null);
  const [imgError, setImgError] = useState(false);

  if (!user) return null;

  const currentYear = new Date().getFullYear();
  const validityPeriod = `${currentYear} - ${currentYear + 1}`;
  const studentId = user.studentId || 'STU-' + currentYear + '-00001';

  const handlePrint = () => {
    window.print();
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '1rem', width: '100%' }}>
      {/* ID Card Wrapper */}
      <div
        ref={cardRef}
        className="virtual-id-card"
        style={{
          width: '100%',
          maxWidth: '420px',
          background: 'linear-gradient(145deg, #1e293b 0%, #0f172a 100%)',
          color: '#ffffff',
          borderRadius: '16px',
          overflow: 'hidden',
          boxShadow: '0 12px 30px -5px rgba(0, 0, 0, 0.35), 0 0 0 1px rgba(255, 255, 255, 0.1)',
          position: 'relative',
          fontFamily: 'inherit',
        }}
      >
        {/* Top Header Bar */}
        <div
          style={{
            background: 'linear-gradient(90deg, #2563eb 0%, #3b82f6 50%, #1d4ed8 100%)',
            padding: '1rem 1.25rem',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            borderBottom: '2px solid rgba(255, 255, 255, 0.15)',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <div
              style={{
                width: '36px',
                height: '36px',
                borderRadius: '8px',
                backgroundColor: 'rgba(255, 255, 255, 0.2)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                backdropFilter: 'blur(4px)',
              }}
            >
              <Building2 size={20} color="#ffffff" />
            </div>
            <div>
              <div style={{ fontSize: '0.9rem', fontWeight: 800, letterSpacing: '0.05em', textTransform: 'uppercase' }}>
                Smart Hostel
              </div>
              <div style={{ fontSize: '0.65rem', color: '#bfdbfe', letterSpacing: '0.08em', textTransform: 'uppercase' }}>
                Student Identity Card
              </div>
            </div>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.25rem', color: '#93c5fd' }}>
            <ShieldCheck size={18} />
          </div>
        </div>

        {/* Card Body */}
        <div style={{ padding: '1.25rem' }}>
          <div style={{ display: 'flex', gap: '1.25rem', alignItems: 'center', marginBottom: '1.25rem' }}>
            {/* Student Photo */}
            <div
              style={{
                width: '88px',
                height: '88px',
                borderRadius: '12px',
                overflow: 'hidden',
                border: '3px solid #3b82f6',
                boxShadow: '0 4px 10px rgba(0,0,0,0.3)',
                flexShrink: 0,
                backgroundColor: '#334155',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              {user.profilePhoto && !imgError ? (
                <img
                  src={user.profilePhoto}
                  alt={user.fullName}
                  style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                  onError={() => setImgError(true)}
                />
              ) : (
                <div
                  style={{
                    fontSize: '2rem',
                    fontWeight: 800,
                    color: '#93c5fd',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    width: '100%',
                    height: '100%',
                    background: 'linear-gradient(135deg, #1e40af 0%, #3b82f6 100%)',
                  }}
                >
                  {user.fullName?.[0]?.toUpperCase() || 'S'}
                </div>
              )}
            </div>

            {/* Student Name & ID Badge */}
            <div style={{ flex: 1, minWidth: 0 }}>
              <div
                style={{
                  fontSize: '1.15rem',
                  fontWeight: 800,
                  color: '#ffffff',
                  whiteSpace: 'nowrap',
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                  marginBottom: '0.25rem',
                }}
              >
                {user.fullName}
              </div>
              <div
                style={{
                  display: 'inline-block',
                  backgroundColor: 'rgba(59, 130, 246, 0.25)',
                  border: '1px solid #3b82f6',
                  color: '#60a5fa',
                  fontFamily: 'monospace',
                  fontWeight: 700,
                  fontSize: '0.85rem',
                  padding: '0.2rem 0.6rem',
                  borderRadius: '6px',
                  letterSpacing: '0.05em',
                  marginBottom: '0.35rem',
                }}
              >
                {studentId}
              </div>
              <div style={{ fontSize: '0.75rem', color: '#94a3b8' }}>
                Role: <span style={{ color: '#38bdf8', fontWeight: 600 }}>Resident Student</span>
              </div>
            </div>
          </div>

          {/* Details Table */}
          <div
            style={{
              backgroundColor: 'rgba(255, 255, 255, 0.04)',
              borderRadius: '10px',
              padding: '0.75rem 1rem',
              display: 'flex',
              flexDirection: 'column',
              gap: '0.45rem',
              fontSize: '0.8rem',
              border: '1px solid rgba(255, 255, 255, 0.06)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <span style={{ color: '#94a3b8', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                <Mail size={13} /> Email
              </span>
              <span style={{ fontWeight: 600, color: '#f1f5f9', maxWidth: '200px', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                {user.email}
              </span>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <span style={{ color: '#94a3b8', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                <Phone size={13} /> Mobile
              </span>
              <span style={{ fontWeight: 600, color: '#f1f5f9' }}>
                {user.mobileNumber || 'N/A'}
              </span>
            </div>

            {user.gender && (
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <span style={{ color: '#94a3b8', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                  <User size={13} /> Gender
                </span>
                <span style={{ fontWeight: 600, color: '#f1f5f9' }}>
                  {user.gender}
                </span>
              </div>
            )}

            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <span style={{ color: '#94a3b8', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                <Calendar size={13} /> Academic Year
              </span>
              <span style={{ fontWeight: 600, color: '#38bdf8' }}>
                {validityPeriod}
              </span>
            </div>
          </div>
        </div>

        {/* Footer Bar with Decorative Barcode */}
        <div
          style={{
            backgroundColor: '#090d16',
            padding: '0.75rem 1.25rem',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            borderTop: '1px solid rgba(255, 255, 255, 0.08)',
          }}
        >
          {/* Simulated Barcode */}
          <div style={{ display: 'flex', gap: '2px', alignItems: 'center', height: '22px' }}>
            {[3, 1, 4, 1, 5, 2, 1, 3, 2, 4, 1, 3, 1, 2, 4, 2, 1, 3, 2, 1, 4, 2].map((w, idx) => (
              <div
                key={idx}
                style={{
                  width: `${w}px`,
                  height: '100%',
                  backgroundColor: idx % 2 === 0 ? '#64748b' : '#334155',
                }}
              />
            ))}
          </div>

          <div style={{ textAlign: 'right' }}>
            <span
              style={{
                fontSize: '0.65rem',
                fontWeight: 700,
                color: user.status === 'ACTIVE' ? '#4ade80' : '#f87171',
                textTransform: 'uppercase',
                letterSpacing: '0.08em',
              }}
            >
              ● {user.status || 'ACTIVE'}
            </span>
          </div>
        </div>
      </div>

      {/* Card Action */}
      <Button variant="outline" icon={Printer} onClick={handlePrint} style={{ fontSize: '0.85rem' }}>
        Print / Save ID Card
      </Button>
    </div>
  );
};

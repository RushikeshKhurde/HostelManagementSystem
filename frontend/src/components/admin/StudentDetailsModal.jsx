import React from 'react';
import { Modal } from '../common/Modal';
import { Badge } from '../common/Badge';
import { Button } from '../common/Button';
import {
  User,
  Mail,
  Phone,
  Calendar,
  MapPin,
  GraduationCap,
  Home,
  Bed,
  CreditCard,
  CheckCircle2,
  AlertCircle,
  FileText,
  Clock
} from 'lucide-react';

export const StudentDetailsModal = ({ isOpen, onClose, student }) => {
  if (!student) return null;

  const formatDate = (dateStr) => {
    if (!dateStr) return 'Not specified';
    try {
      const d = new Date(dateStr);
      return d.toLocaleDateString('en-IN', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
      });
    } catch {
      return dateStr;
    }
  };

  const formatCurrency = (val) => {
    if (val === undefined || val === null) return '₹0';
    return `₹${Number(val).toLocaleString('en-IN')}`;
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Resident Student Profile"
      maxWidth="680px"
      footer={
        <div style={{ display: 'flex', justifyContent: 'flex-end', width: '100%' }}>
          <Button variant="primary" onClick={onClose}>
            Close
          </Button>
        </div>
      }
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
        {/* Profile Header */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '1.25rem',
            padding: '1.25rem',
            backgroundColor: 'var(--bg-subtle)',
            borderRadius: 'var(--radius-lg)',
            border: '1px solid var(--border-color)',
            flexWrap: 'wrap',
          }}
        >
          <div
            style={{
              width: '64px',
              height: '64px',
              borderRadius: 'var(--radius-full)',
              backgroundColor: 'var(--primary)',
              color: '#ffffff',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontWeight: 800,
              fontSize: '1.75rem',
              boxShadow: 'var(--shadow-md)',
              flexShrink: 0,
            }}
          >
            {student.studentName ? student.studentName[0].toUpperCase() : 'S'}
          </div>

          <div style={{ flex: 1, minWidth: '200px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap', marginBottom: '0.25rem' }}>
              <h3 style={{ fontSize: '1.25rem', fontWeight: 800, margin: 0, color: 'var(--text-primary)' }}>
                {student.studentName}
              </h3>
              <Badge status={student.bookingStatus || 'APPROVED'} />
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap', fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
              <span>ID: <strong>{student.studentCode || `STU${String(student.studentId).padStart(3, '0')}`}</strong></span>
              {student.username && <span>· @{student.username}</span>}
              {student.bedNumber && <span>· <strong>{student.bedNumber}</strong> (Room {student.roomNumber})</span>}
            </div>
          </div>
        </div>

        {/* Section 1: Personal Information */}
        <div className="card" style={{ padding: '1.25rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.5rem' }}>
            <User size={18} style={{ color: 'var(--primary)' }} />
            <h4 style={{ fontSize: '0.9375rem', fontWeight: 700, margin: 0 }}>Personal Information</h4>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem', fontSize: '0.875rem' }}>
            <div>
              <span style={{ color: 'var(--text-muted)', display: 'block', fontSize: '0.75rem', marginBottom: '2px' }}>Full Name</span>
              <strong style={{ color: 'var(--text-primary)' }}>{student.studentName || 'Not specified'}</strong>
            </div>

            <div>
              <span style={{ color: 'var(--text-muted)', display: 'block', fontSize: '0.75rem', marginBottom: '2px' }}>Student ID / Code</span>
              <strong style={{ color: 'var(--text-primary)' }}>{student.studentCode || `STU${String(student.studentId).padStart(3, '0')}`}</strong>
            </div>

            <div>
              <span style={{ color: 'var(--text-muted)', display: 'block', fontSize: '0.75rem', marginBottom: '2px' }}>Email Address</span>
              <a href={`mailto:${student.email}`} style={{ color: 'var(--primary)', textDecoration: 'none', fontWeight: 600 }}>
                {student.email || 'Not specified'}
              </a>
            </div>

            <div>
              <span style={{ color: 'var(--text-muted)', display: 'block', fontSize: '0.75rem', marginBottom: '2px' }}>Mobile Phone</span>
              <a href={`tel:${student.phone}`} style={{ color: 'var(--text-primary)', textDecoration: 'none', fontWeight: 600 }}>
                {student.phone || 'Not specified'}
              </a>
            </div>

            <div>
              <span style={{ color: 'var(--text-muted)', display: 'block', fontSize: '0.75rem', marginBottom: '2px' }}>Gender</span>
              <span style={{ color: 'var(--text-primary)', fontWeight: 500 }}>{student.gender || 'Not specified'}</span>
            </div>

            <div>
              <span style={{ color: 'var(--text-muted)', display: 'block', fontSize: '0.75rem', marginBottom: '2px' }}>Date of Birth</span>
              <span style={{ color: 'var(--text-primary)', fontWeight: 500 }}>{formatDate(student.dateOfBirth)}</span>
            </div>

            <div style={{ gridColumn: '1 / -1' }}>
              <span style={{ color: 'var(--text-muted)', display: 'block', fontSize: '0.75rem', marginBottom: '2px' }}>Permanent Address</span>
              <span style={{ color: 'var(--text-primary)', fontWeight: 500 }}>{student.address || 'Not specified'}</span>
            </div>
          </div>
        </div>

        {/* Section 2: Academic Information */}
        <div className="card" style={{ padding: '1.25rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.5rem' }}>
            <GraduationCap size={18} style={{ color: 'var(--primary)' }} />
            <h4 style={{ fontSize: '0.9375rem', fontWeight: 700, margin: 0 }}>Academic Details</h4>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem', fontSize: '0.875rem' }}>
            <div>
              <span style={{ color: 'var(--text-muted)', display: 'block', fontSize: '0.75rem', marginBottom: '2px' }}>Course / Stream</span>
              <strong style={{ color: 'var(--text-primary)' }}>{student.course || 'Not specified'}</strong>
            </div>

            <div>
              <span style={{ color: 'var(--text-muted)', display: 'block', fontSize: '0.75rem', marginBottom: '2px' }}>Department</span>
              <strong style={{ color: 'var(--text-primary)' }}>{student.department || 'Not specified'}</strong>
            </div>

            <div>
              <span style={{ color: 'var(--text-muted)', display: 'block', fontSize: '0.75rem', marginBottom: '2px' }}>Year / Semester</span>
              <span style={{ color: 'var(--text-primary)', fontWeight: 500 }}>{student.yearSemester || 'Not specified'}</span>
            </div>

            <div>
              <span style={{ color: 'var(--text-muted)', display: 'block', fontSize: '0.75rem', marginBottom: '2px' }}>College / Roll No.</span>
              <span style={{ color: 'var(--text-primary)', fontWeight: 500 }}>{student.rollNumber || student.studentCode || `STU${String(student.studentId).padStart(3, '0')}`}</span>
            </div>
          </div>
        </div>

        {/* Section 3: Hostel & Bed Information */}
        <div className="card" style={{ padding: '1.25rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.5rem' }}>
            <Home size={18} style={{ color: 'var(--primary)' }} />
            <h4 style={{ fontSize: '0.9375rem', fontWeight: 700, margin: 0 }}>Hostel & Bed Allocation</h4>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem', fontSize: '0.875rem' }}>
            <div>
              <span style={{ color: 'var(--text-muted)', display: 'block', fontSize: '0.75rem', marginBottom: '2px' }}>Hostel Facility</span>
              <strong style={{ color: 'var(--text-primary)' }}>{student.hostelName || 'SmartHostel Campus'}</strong>
            </div>

            <div>
              <span style={{ color: 'var(--text-muted)', display: 'block', fontSize: '0.75rem', marginBottom: '2px' }}>Room Number & Type</span>
              <strong style={{ color: 'var(--text-primary)' }}>Room {student.roomNumber} ({student.roomType || 'Standard'})</strong>
            </div>

            <div>
              <span style={{ color: 'var(--text-muted)', display: 'block', fontSize: '0.75rem', marginBottom: '2px' }}>Assigned Bed</span>
              <span style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '0.375rem',
                backgroundColor: 'var(--primary-light)',
                color: 'var(--primary)',
                padding: '2px 8px',
                borderRadius: 'var(--radius-sm)',
                fontWeight: 700,
              }}>
                <Bed size={14} /> {student.bedNumber || 'Bed 1'}
              </span>
            </div>

            <div>
              <span style={{ color: 'var(--text-muted)', display: 'block', fontSize: '0.75rem', marginBottom: '2px' }}>Check-in Date</span>
              <strong style={{ color: 'var(--text-primary)' }}>{formatDate(student.checkInDate)}</strong>
            </div>

            <div>
              <span style={{ color: 'var(--text-muted)', display: 'block', fontSize: '0.75rem', marginBottom: '2px' }}>Booking Allocation Status</span>
              <Badge status={student.bookingStatus || 'APPROVED'} />
            </div>

            {student.checkOutDate && (
              <div>
                <span style={{ color: 'var(--text-muted)', display: 'block', fontSize: '0.75rem', marginBottom: '2px' }}>Check-out Date</span>
                <span style={{ color: 'var(--text-primary)', fontWeight: 500 }}>{formatDate(student.checkOutDate)}</span>
              </div>
            )}
          </div>
        </div>

        {/* Section 4: Fee & Payment Summary */}
        <div className="card" style={{ padding: '1.25rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1rem', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.5rem', flexWrap: 'wrap', gap: '0.5rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <CreditCard size={18} style={{ color: 'var(--primary)' }} />
              <h4 style={{ fontSize: '0.9375rem', fontWeight: 700, margin: 0 }}>Hostel Fee & Payment Status</h4>
            </div>
            <Badge status={student.paymentStatus === 'PAID' ? 'SUCCESS' : (student.paymentStatus === 'PARTIAL' ? 'IN_PROGRESS' : 'PENDING')}>
              {student.paymentStatus || 'PENDING'}
            </Badge>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '1rem', fontSize: '0.875rem' }}>
            <div style={{ padding: '0.75rem', backgroundColor: 'var(--bg-subtle)', borderRadius: 'var(--radius-md)' }}>
              <span style={{ color: 'var(--text-muted)', display: 'block', fontSize: '0.75rem', textTransform: 'uppercase', fontWeight: 700 }}>Total Fee</span>
              <div style={{ fontSize: '1.25rem', fontWeight: 800, marginTop: '2px', color: 'var(--text-primary)' }}>
                {formatCurrency(student.totalFee)}
              </div>
            </div>

            <div style={{ padding: '0.75rem', backgroundColor: 'var(--success-light)', borderRadius: 'var(--radius-md)' }}>
              <span style={{ color: 'var(--success-text)', display: 'block', fontSize: '0.75rem', textTransform: 'uppercase', fontWeight: 700 }}>Paid Amount</span>
              <div style={{ fontSize: '1.25rem', fontWeight: 800, marginTop: '2px', color: 'var(--success)' }}>
                {formatCurrency(student.paidAmount)}
              </div>
            </div>

            <div style={{ padding: '0.75rem', backgroundColor: student.pendingAmount > 0 ? 'var(--warning-light)' : 'var(--bg-subtle)', borderRadius: 'var(--radius-md)' }}>
              <span style={{ color: student.pendingAmount > 0 ? 'var(--warning-text)' : 'var(--text-muted)', display: 'block', fontSize: '0.75rem', textTransform: 'uppercase', fontWeight: 700 }}>Pending Amount</span>
              <div style={{ fontSize: '1.25rem', fontWeight: 800, marginTop: '2px', color: student.pendingAmount > 0 ? 'var(--warning)' : 'var(--text-muted)' }}>
                {formatCurrency(student.pendingAmount)}
              </div>
            </div>
          </div>
        </div>
      </div>
    </Modal>
  );
};

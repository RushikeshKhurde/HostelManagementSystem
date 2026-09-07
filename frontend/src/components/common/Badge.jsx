import React from 'react';

export const Badge = ({ children, status = 'neutral', className = '' }) => {
  const statusMap = {
    // Rooms
    AVAILABLE: 'badge-success',
    'PARTIALLY OCCUPIED': 'badge-info',
    FULL: 'badge-warning',
    MAINTENANCE: 'badge-danger',
    // Bookings / Payments
    PENDING: 'badge-warning',
    APPROVED: 'badge-success',
    REJECTED: 'badge-danger',
    COMPLETED: 'badge-info',
    CANCELLED: 'badge-neutral',
    SUCCESS: 'badge-success',
    FAILED: 'badge-danger',
    // Roles
    ADMIN: 'badge-info',
    USER: 'badge-neutral',
    // Status
    ACTIVE: 'badge-success',
    INACTIVE: 'badge-danger',
    // Priority
    LOW: 'badge-info',
    MEDIUM: 'badge-warning',
    HIGH: 'badge-danger',
    URGENT: 'badge-danger',
    IMPORTANT: 'badge-warning',
    NORMAL: 'badge-neutral',
    // Grievances
    IN_PROGRESS: 'badge-info',
    RESOLVED: 'badge-success',
  };

  const badgeClass = statusMap[status] || `badge-${status}` || 'badge-neutral';

  return <span className={`badge ${badgeClass} ${className}`}>{children || status}</span>;
};
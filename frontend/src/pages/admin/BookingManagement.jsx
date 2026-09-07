import React, { useState, useEffect } from 'react';
import api from '../../services/api';
import { useToast } from '../../context/ToastContext';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { ConfirmDialog } from '../../components/common/ConfirmDialog';
import { LoadingSpinner } from '../../components/feedback/LoadingSpinner';
import { EmptyState } from '../../components/feedback/EmptyState';
import { Check, X, CheckCircle, Clock, Search, BookmarkCheck } from 'lucide-react';

export const BookingManagement = () => {
  const { success: toastSuccess, error: toastError } = useToast();
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('ALL');
  const [search, setSearch] = useState('');
  const [actionTarget, setActionTarget] = useState(null); // { booking, status }
  const [actionLoading, setActionLoading] = useState(false);

  const fetchBookings = async () => {
    setLoading(true);
    try {
      const res = await api.get('/bookings');
      setBookings(res.data);
    } catch (err) {
      toastError(err.message || 'Failed to fetch bookings');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBookings();
  }, []);

  const handleStatusChange = async () => {
    if (!actionTarget) return;
    setActionLoading(true);
    try {
      await api.put(`/bookings/${actionTarget.booking.id}/status`, { status: actionTarget.status });
      toastSuccess(`Booking marked as ${actionTarget.status}`);
      setActionTarget(null);
      fetchBookings();
    } catch (err) {
      toastError(err.message || 'Failed to update booking');
    } finally {
      setActionLoading(false);
    }
  };

  const filteredBookings = bookings.filter((b) => {
    const studentName = b.student?.fullName || '';
    const studentEmail = b.student?.email || '';
    const roomNum = b.room?.roomNumber || '';
    const matchesSearch =
      studentName.toLowerCase().includes(search.toLowerCase()) ||
      studentEmail.toLowerCase().includes(search.toLowerCase()) ||
      roomNum.toLowerCase().includes(search.toLowerCase());
    const matchesFilter = filter === 'ALL' || b.status === filter;
    return matchesSearch && matchesFilter;
  });

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      <div>
        <h2 style={{ fontSize: '1.5rem', fontWeight: 800 }}>Student Booking Requests</h2>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>
          Review, approve, or reject student room allocations.
        </p>
      </div>

      {/* Filter Bar */}
      <div
        className="card"
        style={{
          padding: '1rem 1.25rem',
          display: 'flex',
          gap: '1rem',
          alignItems: 'center',
          flexWrap: 'wrap',
          justifyContent: 'space-between',
        }}
      >
        <div style={{ position: 'relative', minWidth: '240px', flex: 1 }}>
          <Search size={16} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
          <input
            type="text"
            placeholder="Search by student name, email, or room number..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="form-input"
            style={{ paddingLeft: '2.25rem' }}
          />
        </div>

        <select
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
          className="form-select"
          style={{ width: 'auto', minWidth: '160px' }}
        >
          <option value="ALL">All Statuses</option>
          <option value="PENDING">Pending Approval</option>
          <option value="APPROVED">Approved</option>
          <option value="REJECTED">Rejected</option>
          <option value="COMPLETED">Completed</option>
          <option value="CANCELLED">Cancelled</option>
        </select>
      </div>

      {loading ? (
        <LoadingSpinner text="Loading booking requests..." />
      ) : filteredBookings.length === 0 ? (
        <EmptyState
          icon={BookmarkCheck}
          title="No booking requests found"
          description="There are currently no room requests matching your filter."
        />
      ) : (
        <div className="table-container">
          <table className="table">
            <thead>
              <tr>
                <th>Student Info</th>
                <th>Requested Room</th>
                <th>Check-In Date</th>
                <th>Monthly Rent</th>
                <th>Status</th>
                <th style={{ textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredBookings.map((b) => (
                <tr key={b.id}>
                  <td>
                    <div style={{ fontWeight: 700 }}>{b.student?.fullName}</div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                      {b.student?.email} · {b.student?.mobileNumber}
                    </div>
                  </td>
                  <td>
                    <div style={{ fontWeight: 600 }}>Room {b.room?.roomNumber}</div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{b.room?.roomType}</div>
                  </td>
                  <td>{b.checkInDate}</td>
                  <td style={{ fontWeight: 600 }}>₹{b.room?.pricePerMonth?.toLocaleString('en-IN')}</td>
                  <td><Badge status={b.status} /></td>
                  <td style={{ textAlign: 'right' }}>
                    {b.status === 'PENDING' ? (
                      <div style={{ display: 'inline-flex', gap: '0.5rem' }}>
                        <Button
                          variant="primary"
                          size="sm"
                          icon={Check}
                          onClick={() => setActionTarget({ booking: b, status: 'APPROVED' })}
                        >
                          Approve
                        </Button>
                        <Button
                          variant="outlineDanger"
                          size="sm"
                          icon={X}
                          onClick={() => setActionTarget({ booking: b, status: 'REJECTED' })}
                        >
                          Reject
                        </Button>
                      </div>
                    ) : b.status === 'APPROVED' ? (
                      <div style={{ display: 'inline-flex', gap: '0.5rem' }}>
                        <Button
                          variant="secondary"
                          size="sm"
                          onClick={() => setActionTarget({ booking: b, status: 'COMPLETED' })}
                        >
                          Mark Done
                        </Button>
                        <Button
                          variant="outlineDanger"
                          size="sm"
                          onClick={() => setActionTarget({ booking: b, status: 'CANCELLED' })}
                        >
                          Cancel
                        </Button>
                      </div>
                    ) : (
                      <span style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>No actions</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Confirmation Dialog */}
      <ConfirmDialog
        isOpen={!!actionTarget}
        onClose={() => setActionTarget(null)}
        onConfirm={handleStatusChange}
        title={`${actionTarget?.status} Booking Request`}
        message={`Are you sure you want to change this booking for ${actionTarget?.booking?.student?.fullName} (Room ${actionTarget?.booking?.room?.roomNumber}) to ${actionTarget?.status}?`}
        confirmText={`Set as ${actionTarget?.status}`}
        danger={actionTarget?.status === 'REJECTED' || actionTarget?.status === 'CANCELLED'}
        loading={actionLoading}
      />
    </div>
  );
};
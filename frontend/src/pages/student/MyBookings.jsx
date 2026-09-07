import React, { useState, useEffect } from 'react';
import api from '../../services/api';
import { useToast } from '../../context/ToastContext';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { Modal } from '../../components/common/Modal';
import { LoadingSpinner } from '../../components/feedback/LoadingSpinner';
import { EmptyState } from '../../components/feedback/EmptyState';
import { Bed, Plus, Calendar, CheckCircle2, ArrowRight } from 'lucide-react';
import { Link } from 'react-router-dom';

export const MyBookings = () => {
  const { success: toastSuccess, error: toastError } = useToast();
  const [bookings, setBookings] = useState([]);
  const [rooms, setRooms] = useState([]);
  const [loading, setLoading] = useState(true);

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedRoomId, setSelectedRoomId] = useState('');
  const [checkInDate, setCheckInDate] = useState(new Date().toISOString().split('T')[0]);
  const [submitting, setSubmitting] = useState(false);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [bookingsRes, roomsRes] = await Promise.all([
        api.get('/bookings/my'),
        api.get('/rooms'),
      ]);
      setBookings(bookingsRes.data);
      setRooms(roomsRes.data.filter((r) => r.status === 'AVAILABLE'));
      if (roomsRes.data.filter((r) => r.status === 'AVAILABLE').length > 0) {
        setSelectedRoomId(roomsRes.data.filter((r) => r.status === 'AVAILABLE')[0].id);
      }
    } catch (err) {
      toastError(err.message || 'Failed to fetch bookings');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleBookingSubmit = async (e) => {
    e.preventDefault();
    if (!selectedRoomId) {
      toastError('Please select a room');
      return;
    }
    setSubmitting(true);
    try {
      await api.post('/bookings', {
        roomId: parseInt(selectedRoomId, 10),
        checkInDate,
      });
      toastSuccess('Booking request submitted successfully! Awaiting admin approval.');
      setIsModalOpen(false);
      fetchData();
    } catch (err) {
      toastError(err.message || 'Failed to submit booking');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 800 }}>My Room Bookings</h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>
            Submit room allocation requests and track approval lifecycle.
          </p>
        </div>
        <Button variant="primary" icon={Plus} onClick={() => setIsModalOpen(true)}>
          New Room Booking
        </Button>
      </div>

      {loading ? (
        <LoadingSpinner text="Loading your bookings..." />
      ) : bookings.length === 0 ? (
        <EmptyState
          icon={Bed}
          title="No bookings yet"
          description="You have not submitted any room booking requests."
          actionText="Request a Room"
          onAction={() => setIsModalOpen(true)}
        />
      ) : (
        <div className="table-container">
          <table className="table">
            <thead>
              <tr>
                <th>Room Number</th>
                <th>Room Type</th>
                <th>Check-In Date</th>
                <th>Monthly Fee</th>
                <th>Status</th>
                <th style={{ textAlign: 'right' }}>Action</th>
              </tr>
            </thead>
            <tbody>
              {bookings.map((b) => (
                <tr key={b.id}>
                  <td style={{ fontWeight: 700 }}>Room {b.room?.roomNumber}</td>
                  <td>{b.room?.roomType} ({b.room?.capacity} Beds)</td>
                  <td>{b.checkInDate}</td>
                  <td style={{ fontWeight: 700, color: 'var(--primary)' }}>₹{b.room?.pricePerMonth?.toLocaleString('en-IN')}</td>
                  <td><Badge status={b.status} /></td>
                  <td style={{ textAlign: 'right' }}>
                    {b.status === 'APPROVED' ? (
                      <Link to="/student/payments" className="btn btn-primary btn-sm">
                        Pay Rent <ArrowRight size={14} />
                      </Link>
                    ) : (
                      <span style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
                        {b.status === 'PENDING' ? 'Under Review' : 'Closed'}
                      </span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* New Booking Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Apply for Room Booking"
        maxWidth="500px"
      >
        <form onSubmit={handleBookingSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          <div>
            <label className="form-label">Select Available Room</label>
            {rooms.length === 0 ? (
              <p style={{ fontSize: '0.875rem', color: 'var(--danger)' }}>
                No rooms are currently marked as available. Please check back later.
              </p>
            ) : (
              <select
                className="form-select"
                value={selectedRoomId}
                onChange={(e) => setSelectedRoomId(e.target.value)}
                required
              >
                {rooms.map((r) => (
                  <option key={r.id} value={r.id}>
                    Room {r.roomNumber} - {r.roomType} (₹{r.pricePerMonth?.toLocaleString('en-IN')}/mo) [{r.occupied}/{r.capacity} Occupied]
                  </option>
                ))}
              </select>
            )}
          </div>

          <div>
            <label className="form-label">Proposed Check-In Date</label>
            <input
              type="date"
              className="form-input"
              value={checkInDate}
              onChange={(e) => setCheckInDate(e.target.value)}
              required
            />
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
            <Button type="button" variant="secondary" onClick={() => setIsModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" variant="primary" loading={submitting} disabled={rooms.length === 0}>
              Submit Request
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
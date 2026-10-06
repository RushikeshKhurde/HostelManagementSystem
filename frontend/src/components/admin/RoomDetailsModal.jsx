import React, { useState, useEffect } from 'react';
import api from '../../services/api';
import { Modal } from '../common/Modal';
import { Badge } from '../common/Badge';
import { Button } from '../common/Button';
import { LoadingSpinner } from '../feedback/LoadingSpinner';
import { EmptyState } from '../feedback/EmptyState';
import {
  Bed,
  Users,
  CheckCircle2,
  AlertTriangle,
  Wrench,
  Eye,
  Calendar,
  Phone,
  Mail,
  GraduationCap,
  RefreshCw,
  Clock,
  ArrowRight,
  ShieldCheck
} from 'lucide-react';

export const RoomDetailsModal = ({
  isOpen,
  onClose,
  room,
  onViewStudent,
  isWarden = false,
}) => {
  const [occupants, setOccupants] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchOccupants = async () => {
    if (!room?.id) return;
    setLoading(true);
    setError(null);
    try {
      const res = await api.get(`/rooms/${room.id}/students`);
      setOccupants(res.data || []);
    } catch (err) {
      console.error('Failed to load room occupants:', err);
      setError('Unable to load room occupants. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (isOpen && room?.id) {
      fetchOccupants();
    } else {
      setOccupants([]);
      setError(null);
    }
  }, [isOpen, room?.id]);

  if (!room) return null;

  const occupiedCount = occupants.length;
  const capacity = room.capacity || 1;
  const availableBeds = Math.max(0, capacity - occupiedCount);
  const occupancyPercentage = Math.min(100, Math.round((occupiedCount / capacity) * 100));

  const floorNumber = room.roomNumber?.includes('-')
    ? room.roomNumber.split('-')[1]?.[0] || '1'
    : '1';

  const formatDate = (dateStr) => {
    if (!dateStr) return 'N/A';
    try {
      const d = new Date(dateStr);
      return d.toLocaleDateString('en-IN', {
        day: '2-digit',
        month: 'short',
        year: 'numeric',
      });
    } catch {
      return dateStr;
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={`Room ${room.roomNumber} Details & Occupancy`}
      maxWidth="840px"
      footer={
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', width: '100%' }}>
          <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
            Room ID: #{room.id} · Floor {floorNumber} · {room.roomType}
          </div>
          <Button variant="primary" onClick={onClose}>
            Close
          </Button>
        </div>
      }
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
        {/* Top Header Card */}
        <div
          className="card"
          style={{
            padding: '1.25rem',
            backgroundColor: 'var(--bg-subtle)',
            border: '1px solid var(--border-color)',
          }}
        >
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'flex-start',
              flexWrap: 'wrap',
              gap: '1rem',
              marginBottom: '1rem',
            }}
          >
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
                <h3 style={{ fontSize: '1.5rem', fontWeight: 800, margin: 0, color: 'var(--text-primary)' }}>
                  Room {room.roomNumber}
                </h3>
                <Badge status={room.status} />
              </div>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', margin: '4px 0 0 0' }}>
                <strong>{room.roomType} ROOM</strong> · Floor {floorNumber} · Block{' '}
                {room.roomNumber?.split('-')[0] || 'A'}
              </p>
            </div>

            <div style={{ textAlign: 'right' }}>
              <div style={{ fontSize: '1.5rem', fontWeight: 800, color: 'var(--primary)' }}>
                ₹{room.pricePerMonth?.toLocaleString('en-IN')}{' '}
                <span style={{ fontSize: '0.8125rem', fontWeight: 400, color: 'var(--text-muted)' }}>/ month</span>
              </div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Per Student Resident</div>
            </div>
          </div>

          {/* Under Maintenance Notice */}
          {room.status === 'MAINTENANCE' && (
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '0.75rem',
                padding: '0.75rem 1rem',
                backgroundColor: 'var(--danger-light)',
                border: '1px solid var(--danger-border)',
                borderRadius: 'var(--radius-md)',
                color: 'var(--danger-text)',
                fontSize: '0.875rem',
                marginBottom: '1rem',
              }}
            >
              <Wrench size={18} style={{ flexShrink: 0, color: 'var(--danger)' }} />
              <div>
                <strong>Under Maintenance:</strong> This room is currently unavailable for new student bookings.
                Active resident allocations remain intact.
              </div>
            </div>
          )}

          {/* Statistics Grid */}
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(130px, 1fr))',
              gap: '0.75rem',
              marginBottom: '1rem',
            }}
          >
            <div
              style={{
                padding: '0.75rem',
                backgroundColor: 'var(--bg-surface)',
                borderRadius: 'var(--radius-md)',
                border: '1px solid var(--border-color)',
                textAlign: 'center',
              }}
            >
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 700 }}>
                Total Capacity
              </div>
              <div style={{ fontSize: '1.25rem', fontWeight: 800, marginTop: '2px', color: 'var(--text-primary)' }}>
                {capacity} {capacity === 1 ? 'Bed' : 'Beds'}
              </div>
            </div>

            <div
              style={{
                padding: '0.75rem',
                backgroundColor: 'var(--bg-surface)',
                borderRadius: 'var(--radius-md)',
                border: '1px solid var(--border-color)',
                textAlign: 'center',
              }}
            >
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 700 }}>
                Occupied Beds
              </div>
              <div
                style={{
                  fontSize: '1.25rem',
                  fontWeight: 800,
                  marginTop: '2px',
                  color: occupiedCount >= capacity ? 'var(--warning)' : 'var(--text-primary)',
                }}
              >
                {occupiedCount} {occupiedCount === 1 ? 'Bed' : 'Beds'}
              </div>
            </div>

            <div
              style={{
                padding: '0.75rem',
                backgroundColor: 'var(--bg-surface)',
                borderRadius: 'var(--radius-md)',
                border: '1px solid var(--border-color)',
                textAlign: 'center',
              }}
            >
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 700 }}>
                Available Beds
              </div>
              <div
                style={{
                  fontSize: '1.25rem',
                  fontWeight: 800,
                  marginTop: '2px',
                  color: availableBeds > 0 ? 'var(--success)' : 'var(--danger)',
                }}
              >
                {availableBeds} {availableBeds === 1 ? 'Bed' : 'Beds'}
              </div>
            </div>

            <div
              style={{
                padding: '0.75rem',
                backgroundColor: 'var(--bg-surface)',
                borderRadius: 'var(--radius-md)',
                border: '1px solid var(--border-color)',
                textAlign: 'center',
              }}
            >
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 700 }}>
                Current Status
              </div>
              <div style={{ marginTop: '4px' }}>
                <Badge status={room.status} />
              </div>
            </div>
          </div>

          {/* Occupancy Progress */}
          <div>
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                fontSize: '0.8125rem',
                color: 'var(--text-muted)',
                marginBottom: '0.375rem',
              }}
            >
              <span>Occupancy Level ({occupiedCount} of {capacity} Beds Allocated)</span>
              <strong>{occupancyPercentage}%</strong>
            </div>
            <div
              style={{
                height: '8px',
                backgroundColor: 'var(--bg-hover)',
                borderRadius: 'var(--radius-full)',
                overflow: 'hidden',
              }}
            >
              <div
                style={{
                  width: `${occupancyPercentage}%`,
                  height: '100%',
                  backgroundColor:
                    room.status === 'MAINTENANCE'
                      ? 'var(--danger)'
                      : occupiedCount >= capacity
                      ? 'var(--warning)'
                      : 'var(--primary)',
                  transition: 'width 300ms ease',
                }}
              />
            </div>
          </div>
        </div>

        {/* Bed-Level Allocation Grid */}
        <div className="card" style={{ padding: '1.25rem' }}>
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              marginBottom: '1rem',
              borderBottom: '1px solid var(--border-color)',
              paddingBottom: '0.5rem',
              flexWrap: 'wrap',
              gap: '0.5rem',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Bed size={18} style={{ color: 'var(--primary)' }} />
              <h4 style={{ fontSize: '0.9375rem', fontWeight: 700, margin: 0 }}>
                Individual Bed Allocation Layout
              </h4>
            </div>
            <span style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
              {capacity} {capacity === 1 ? 'Slot' : 'Slots'} Configured
            </span>
          </div>

          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
              gap: '0.875rem',
            }}
          >
            {Array.from({ length: capacity }, (_, i) => {
              const bedNum = `Bed ${i + 1}`;
              const occupant = occupants[i];
              const isOccupied = !!occupant;

              return (
                <div
                  key={bedNum}
                  style={{
                    padding: '1rem',
                    borderRadius: 'var(--radius-md)',
                    border: `1px solid ${isOccupied ? 'var(--primary-border)' : 'var(--border-color)'}`,
                    backgroundColor: isOccupied ? 'var(--primary-light)' : 'var(--bg-subtle)',
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'space-between',
                    gap: '0.75rem',
                    transition: 'all var(--transition-fast)',
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                      <Bed size={18} style={{ color: isOccupied ? 'var(--primary)' : 'var(--text-muted)' }} />
                      <span style={{ fontWeight: 800, fontSize: '0.9375rem', color: 'var(--text-primary)' }}>
                        {bedNum}
                      </span>
                    </div>
                    <Badge status={isOccupied ? 'FULL' : (room.status === 'MAINTENANCE' ? 'MAINTENANCE' : 'AVAILABLE')}>
                      {isOccupied ? 'OCCUPIED' : (room.status === 'MAINTENANCE' ? 'UNAVAILABLE' : 'AVAILABLE')}
                    </Badge>
                  </div>

                  {isOccupied ? (
                    <div>
                      <div style={{ fontWeight: 700, fontSize: '0.875rem', color: 'var(--text-primary)' }}>
                        {occupant.studentName}
                      </div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '2px' }}>
                        ID: {occupant.studentCode || `STU${String(occupant.studentId).padStart(3, '0')}`}
                      </div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '2px' }}>
                        Since: {formatDate(occupant.checkInDate)}
                      </div>
                      <div style={{ marginTop: '0.75rem' }}>
                        <Button
                          variant="outline"
                          size="sm"
                          icon={Eye}
                          onClick={() => onViewStudent(occupant)}
                          style={{ width: '100%', fontSize: '0.75rem', padding: '4px 8px' }}
                        >
                          View Student
                        </Button>
                      </div>
                    </div>
                  ) : (
                    <div>
                      <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', margin: 0 }}>
                        {room.status === 'MAINTENANCE'
                          ? 'Slot under maintenance'
                          : 'Vacant Bed · Ready for student allocation'}
                      </p>
                      <div style={{ height: '31px', marginTop: '0.75rem' }} />
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </div>

        {/* Current Occupants Detailed List */}
        <div className="card" style={{ padding: '1.25rem' }}>
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              marginBottom: '1rem',
              borderBottom: '1px solid var(--border-color)',
              paddingBottom: '0.5rem',
              flexWrap: 'wrap',
              gap: '0.5rem',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Users size={18} style={{ color: 'var(--primary)' }} />
              <h4 style={{ fontSize: '0.9375rem', fontWeight: 700, margin: 0 }}>
                Students in this Room ({occupiedCount})
              </h4>
            </div>

            <Button
              variant="ghost"
              size="sm"
              icon={RefreshCw}
              onClick={fetchOccupants}
              loading={loading}
              title="Refresh occupant records"
            >
              Refresh
            </Button>
          </div>

          {loading ? (
            <div style={{ padding: '2rem 0' }}>
              <LoadingSpinner text="Loading student details..." />
            </div>
          ) : error ? (
            <div
              style={{
                padding: '1.5rem',
                textAlign: 'center',
                backgroundColor: 'var(--danger-light)',
                borderRadius: 'var(--radius-md)',
                color: 'var(--danger-text)',
              }}
            >
              <AlertTriangle size={32} style={{ margin: '0 auto 0.5rem', color: 'var(--danger)' }} />
              <p style={{ fontWeight: 600, margin: '0 0 0.75rem 0' }}>{error}</p>
              <Button variant="secondary" size="sm" icon={RefreshCw} onClick={fetchOccupants}>
                Try Again
              </Button>
            </div>
          ) : occupants.length === 0 ? (
            <div style={{ padding: '1rem 0' }}>
              <EmptyState
                icon={Bed}
                title="No students are currently assigned to this room."
                description="Approved resident bookings for this room will automatically populate here."
              />
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.875rem' }}>
              {occupants.map((student, idx) => (
                <div
                  key={student.studentId || idx}
                  style={{
                    padding: '1rem 1.25rem',
                    backgroundColor: 'var(--bg-subtle)',
                    borderRadius: 'var(--radius-md)',
                    border: '1px solid var(--border-color)',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    flexWrap: 'wrap',
                    gap: '1rem',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', minWidth: '240px' }}>
                    <div
                      style={{
                        width: '44px',
                        height: '44px',
                        borderRadius: 'var(--radius-full)',
                        backgroundColor: 'var(--primary)',
                        color: '#ffffff',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        fontWeight: 800,
                        fontSize: '1.125rem',
                        flexShrink: 0,
                      }}
                    >
                      {student.studentName ? student.studentName[0].toUpperCase() : 'S'}
                    </div>

                    <div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap' }}>
                        <strong style={{ fontSize: '1rem', color: 'var(--text-primary)' }}>
                          {student.studentName}
                        </strong>
                        <span
                          style={{
                            fontSize: '0.75rem',
                            fontWeight: 700,
                            backgroundColor: 'var(--primary-light)',
                            color: 'var(--primary)',
                            padding: '1px 6px',
                            borderRadius: 'var(--radius-xs)',
                          }}
                        >
                          {student.bedNumber || `Bed ${idx + 1}`}
                        </span>
                        <Badge status={student.bookingStatus || 'APPROVED'} />
                      </div>

                      <div
                        style={{
                          fontSize: '0.8125rem',
                          color: 'var(--text-muted)',
                          marginTop: '2px',
                          display: 'flex',
                          alignItems: 'center',
                          gap: '0.75rem',
                          flexWrap: 'wrap',
                        }}
                      >
                        <span>ID: <strong>{student.studentCode || `STU${String(student.studentId).padStart(3, '0')}`}</strong></span>
                        <span>· {student.course || 'Information Technology'}</span>
                      </div>
                    </div>
                  </div>

                  <div
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '1.5rem',
                      flexWrap: 'wrap',
                      fontSize: '0.8125rem',
                    }}
                  >
                    <div>
                      <div style={{ color: 'var(--text-muted)', fontSize: '0.75rem' }}>Contact</div>
                      <div style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{student.phone}</div>
                      <div style={{ color: 'var(--text-muted)', fontSize: '0.75rem' }}>{student.email}</div>
                    </div>

                    <div>
                      <div style={{ color: 'var(--text-muted)', fontSize: '0.75rem' }}>Check-in Date</div>
                      <div style={{ fontWeight: 600, color: 'var(--text-primary)' }}>
                        {formatDate(student.checkInDate)}
                      </div>
                    </div>

                    <div>
                      <div style={{ color: 'var(--text-muted)', fontSize: '0.75rem' }}>Fee Status</div>
                      <Badge
                        status={
                          student.paymentStatus === 'PAID'
                            ? 'SUCCESS'
                            : student.paymentStatus === 'PARTIAL'
                            ? 'IN_PROGRESS'
                            : 'PENDING'
                        }
                      >
                        {student.paymentStatus || 'PENDING'}
                      </Badge>
                    </div>

                    <div style={{ display: 'flex', gap: '0.5rem' }}>
                      <Button
                        variant="primary"
                        size="sm"
                        icon={Eye}
                        onClick={() => onViewStudent(student)}
                      >
                        View Student
                      </Button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </Modal>
  );
};

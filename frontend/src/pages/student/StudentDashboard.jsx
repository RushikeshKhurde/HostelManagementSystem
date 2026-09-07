import React, { useState, useEffect } from 'react';
import api from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { StatCard } from '../../components/common/StatCard';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { LoadingSpinner } from '../../components/feedback/LoadingSpinner';
import { ErrorState } from '../../components/feedback/ErrorState';
import {
  Bed,
  CreditCard,
  MessageSquareWarning,
  CalendarDays,
  Users,
  Plus,
  ArrowRight,
  Sparkles,
  CheckCircle2,
  AlertCircle,
  Bell,
  User,
} from 'lucide-react';
import { Link } from 'react-router-dom';

export const StudentDashboard = () => {
  const { user } = useAuth();
  const { error: toastError } = useToast();
  const [stats, setStats] = useState(null);
  const [notices, setNotices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchStudentData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [statsRes, noticesRes] = await Promise.all([
        api.get('/dashboard/student-stats'),
        api.get('/notices'),
      ]);
      setStats(statsRes.data);
      setNotices(noticesRes.data.slice(0, 3));
    } catch (err) {
      setError(err.message);
      toastError(err.message || 'Failed to fetch student data');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchStudentData();
  }, []);

  if (loading) return <LoadingSpinner text="Loading student dashboard..." />;
  if (error) return <ErrorState message={error} onRetry={fetchStudentData} />;

  const activeBooking = stats?.activeBooking;
  const room = activeBooking?.room;
  const isApproved = activeBooking?.status === 'APPROVED';

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
      {/* Welcome Banner */}
      <div
        className="card"
        style={{
          background: 'linear-gradient(135deg, var(--bg-card) 0%, var(--bg-subtle) 100%)',
          border: '1px solid var(--border-color)',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '1rem',
          padding: '1.75rem 2rem',
        }}
      >
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.25rem' }}>
            <span className="badge badge-info">Student Portal</span>
          </div>
          <h2 style={{ fontSize: '1.75rem', fontWeight: 800 }}>Welcome back, {user?.fullName}!</h2>
          <p style={{ color: 'var(--text-secondary)', marginTop: '0.25rem', fontSize: '0.9375rem' }}>
            Access your room details, pay rent securely, lodge maintenance requests, or apply for leaves.
          </p>
        </div>
        <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
          <Link to="/profile" className="btn btn-secondary btn-sm" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.5rem' }}>
            <User size={16} /> My Profile
          </Link>
          {!activeBooking ? (
            <Link to="/student/rooms" className="btn btn-primary btn-sm">
              <Plus size={16} /> Explore & Book Room
            </Link>
          ) : isApproved ? (
            <Link to="/student/payments" className="btn btn-primary btn-sm">
              <CreditCard size={16} /> Pay Monthly Rent
            </Link>
          ) : (
            <span className="badge badge-warning" style={{ padding: '0.5rem 1rem' }}>
              Booking Request Pending Approval
            </span>
          )}
        </div>
      </div>

      {/* KPI Stats */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
          gap: '1.25rem',
        }}
      >
        <StatCard
          title="My Allocated Room"
          value={room ? `Room ${room.roomNumber}` : 'Not Allocated'}
          subtitle={room ? `${room.roomType} (${room.capacity} Beds)` : 'Apply for a room'}
          icon={Bed}
          color={room ? 'primary' : 'warning'}
        />
        <StatCard
          title="Total Rent Paid"
          value={`₹${(stats?.totalPaid || 0).toLocaleString('en-IN')}`}
          subtitle="Verified payment receipts"
          icon={CreditCard}
          color="success"
        />
        <StatCard
          title="Active Grievances"
          value={stats?.pendingComplaints || 0}
          subtitle={`${stats?.totalComplaints || 0} total lodged`}
          icon={MessageSquareWarning}
          color={stats?.pendingComplaints > 0 ? 'warning' : 'info'}
        />
        <StatCard
          title="Pending Leaves"
          value={stats?.pendingLeaves || 0}
          subtitle={`${stats?.totalLeaves || 0} total requests`}
          icon={CalendarDays}
          color={stats?.pendingLeaves > 0 ? 'warning' : 'info'}
        />
      </div>

      {/* Room & Roommates Card + Quick Actions */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(340px, 1fr))', gap: '1.5rem' }}>
        {/* Allocated Room Card */}
        <div className="card">
          <h3 style={{ fontSize: '1.125rem', marginBottom: '1.25rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span>My Room & Accommodation</span>
            {activeBooking && <Badge status={activeBooking.status} />}
          </h3>

          {room && isApproved ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.75rem', backgroundColor: 'var(--bg-subtle)', borderRadius: 'var(--radius-md)' }}>
                <div>
                  <div style={{ fontSize: '0.75rem', textTransform: 'uppercase', color: 'var(--text-muted)', fontWeight: 700 }}>Wing & Room</div>
                  <div style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--primary)' }}>Room {room.roomNumber}</div>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <div style={{ fontSize: '0.75rem', textTransform: 'uppercase', color: 'var(--text-muted)', fontWeight: 700 }}>Monthly Rent</div>
                  <div style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--success)' }}>₹{room.pricePerMonth?.toLocaleString('en-IN')}</div>
                </div>
              </div>

              <div>
                <div style={{ fontSize: '0.8125rem', fontWeight: 700, textTransform: 'uppercase', color: 'var(--text-muted)', marginBottom: '0.5rem' }}>
                  Roommates in Room {room.roomNumber} ({stats?.roommates?.length || 0})
                </div>
                {stats?.roommates?.length === 0 ? (
                  <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>No other roommates currently allocated.</p>
                ) : (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                    {stats.roommates.map((rm, idx) => (
                      <div key={idx} style={{ display: 'flex', justifyContent: 'space-between', padding: '0.5rem 0.75rem', border: '1px solid var(--border-color)', borderRadius: 'var(--radius-sm)', fontSize: '0.875rem' }}>
                        <span style={{ fontWeight: 600 }}>{rm.name}</span>
                        <span style={{ color: 'var(--text-muted)' }}>{rm.mobile}</span>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          ) : activeBooking ? (
            <div style={{ textAlign: 'center', padding: '2rem 1rem' }}>
              <AlertCircle size={36} color="var(--warning)" style={{ margin: '0 auto 0.75rem' }} />
              <h4 style={{ fontSize: '1rem', fontWeight: 700 }}>Request Submitted for Room {activeBooking.room?.roomNumber}</h4>
              <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
                Your booking request is being reviewed by the hostel administrator.
              </p>
            </div>
          ) : (
            <div style={{ textAlign: 'center', padding: '2rem 1rem' }}>
              <Bed size={36} color="var(--primary)" style={{ margin: '0 auto 0.75rem' }} />
              <h4 style={{ fontSize: '1rem', fontWeight: 700 }}>No Room Allocated Yet</h4>
              <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', marginTop: '0.25rem', marginBottom: '1rem' }}>
                Browse available single, double, triple, and dormitory rooms.
              </p>
              <Link to="/student/rooms" className="btn btn-primary btn-sm">
                Explore Available Rooms <ArrowRight size={14} />
              </Link>
            </div>
          )}
        </div>

        {/* Recent Hostel Circulars */}
        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
            <h3 style={{ fontSize: '1.125rem' }}>Hostel Circulars & Notices</h3>
            <Link to="/student/notices" className="btn btn-ghost btn-sm">
              View All <ArrowRight size={14} />
            </Link>
          </div>

          {notices.length === 0 ? (
            <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', textAlign: 'center', padding: '2rem 0' }}>
              No recent announcements.
            </p>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
              {notices.map((n) => (
                <div key={n.id} style={{ padding: '0.75rem', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)', backgroundColor: 'var(--bg-subtle)' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.25rem' }}>
                    <span style={{ fontWeight: 700, fontSize: '0.875rem' }}>{n.title}</span>
                    <Badge status={n.priority} />
                  </div>
                  <p style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
                    {n.content}
                  </p>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
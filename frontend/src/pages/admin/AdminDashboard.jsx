import React, { useState, useEffect } from 'react';
import api from '../../services/api';
import { useToast } from '../../context/ToastContext';
import { StatCard } from '../../components/common/StatCard';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { LoadingSpinner } from '../../components/feedback/LoadingSpinner';
import { ErrorState } from '../../components/feedback/ErrorState';
import {
  Users,
  Building2,
  Bed,
  CreditCard,
  MessageSquareWarning,
  CalendarDays,
  TrendingUp,
  Plus,
  ArrowRight,
  ShieldCheck,
  CheckCircle,
  User,
} from 'lucide-react';
import { Link } from 'react-router-dom';

export const AdminDashboard = () => {
  const { error: toastError } = useToast();
  const [stats, setStats] = useState(null);
  const [recentBookings, setRecentBookings] = useState([]);
  const [recentPayments, setRecentPayments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchDashboardData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [statsRes, bookingsRes, paymentsRes] = await Promise.all([
        api.get('/dashboard/stats'),
        api.get('/bookings'),
        api.get('/payments'),
      ]);
      setStats(statsRes.data);
      setRecentBookings(bookingsRes.data.slice(0, 5));
      setRecentPayments(paymentsRes.data.slice(0, 5));
    } catch (err) {
      setError(err.message);
      toastError(err.message || 'Failed to fetch dashboard data');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboardData();
  }, []);

  if (loading) return <LoadingSpinner text="Loading admin intelligence dashboard..." />;
  if (error) return <ErrorState message={error} onRetry={fetchDashboardData} />;

  // Calculate occupancy percentage
  const totalCapacity = stats?.totalCapacity || 1;
  const occupiedBeds = stats?.occupiedBeds || 0;
  const occupancyPercent = Math.round((occupiedBeds / totalCapacity) * 100);

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
            <span className="badge badge-info">Administrator Control Panel</span>
          </div>
          <h2 style={{ fontSize: '1.75rem', fontWeight: 800 }}>Welcome to SmartHostel Operations</h2>
          <p style={{ color: 'var(--text-secondary)', marginTop: '0.25rem', fontSize: '0.9375rem' }}>
            Real-time occupancy metrics, fee collections, and student service status.
          </p>
        </div>
        <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
          <Link to="/profile" className="btn btn-secondary btn-sm" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.5rem' }}>
            <User size={16} /> My Profile
          </Link>
          <Link to="/admin/rooms" className="btn btn-primary btn-sm">
            <Plus size={16} /> Manage Rooms
          </Link>
          <Link to="/admin/bookings" className="btn btn-secondary btn-sm">
            Review Bookings ({stats?.pendingBookings || 0})
          </Link>
        </div>
      </div>

      {/* KPI Cards Grid */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
          gap: '1.25rem',
        }}
      >
        <StatCard
          title="Total Students"
          value={stats?.totalStudents || 0}
          subtitle="Registered user accounts"
          icon={Users}
          color="primary"
        />
        <StatCard
          title="Total Rooms"
          value={stats?.totalRooms || 0}
          subtitle={`${stats?.occupiedRooms || 0} occupied rooms`}
          icon={Bed}
          color="info"
        />
        <StatCard
          title="Available Beds"
          value={stats?.availableBeds || 0}
          subtitle={`Out of ${stats?.totalCapacity || 0} total beds`}
          icon={Building2}
          color="success"
        />
        <StatCard
          title="Revenue Collected"
          value={`₹${(stats?.totalRevenue || 0).toLocaleString('en-IN')}`}
          subtitle="Total verified payments"
          icon={CreditCard}
          color="success"
        />
        <StatCard
          title="Pending Grievances"
          value={stats?.pendingComplaints || 0}
          subtitle="Awaiting resolution"
          icon={MessageSquareWarning}
          color={stats?.pendingComplaints > 0 ? 'warning' : 'info'}
        />
        <StatCard
          title="Pending Leaves"
          value={stats?.pendingLeaves || 0}
          subtitle="Awaiting warden review"
          icon={CalendarDays}
          color={stats?.pendingLeaves > 0 ? 'warning' : 'info'}
        />
      </div>

      {/* Visual Charts & Occupancy Insights */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(340px, 1fr))', gap: '1.5rem' }}>
        {/* Occupancy Progress Visual */}
        <div className="card">
          <h3 style={{ fontSize: '1.125rem', marginBottom: '1rem', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <span>Room Bed Occupancy</span>
            <span className="badge badge-success">{occupancyPercent}% Occupied</span>
          </h3>

          <div style={{ margin: '1.5rem 0' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.875rem', marginBottom: '0.5rem', fontWeight: 600 }}>
              <span>Occupied: {occupiedBeds} Beds</span>
              <span style={{ color: 'var(--text-muted)' }}>Capacity: {totalCapacity} Beds</span>
            </div>
            <div style={{ height: '12px', backgroundColor: 'var(--bg-subtle)', borderRadius: '6px', overflow: 'hidden' }}>
              <div
                style={{
                  width: `${occupancyPercent}%`,
                  height: '100%',
                  background: 'linear-gradient(90deg, var(--primary) 0%, var(--success) 100%)',
                  borderRadius: '6px',
                  transition: 'width 0.8s ease',
                }}
              />
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', paddingTop: '1rem', borderTop: '1px solid var(--border-color)' }}>
            <div>
              <div style={{ fontSize: '0.75rem', textTransform: 'uppercase', color: 'var(--text-muted)', fontWeight: 700 }}>
                Available Bed Capacity
              </div>
              <div style={{ fontSize: '1.5rem', fontWeight: 800, color: 'var(--success)', marginTop: '0.25rem' }}>
                {stats?.availableBeds || 0}
              </div>
            </div>
            <div>
              <div style={{ fontSize: '0.75rem', textTransform: 'uppercase', color: 'var(--text-muted)', fontWeight: 700 }}>
                Hostel Blocks
              </div>
              <div style={{ fontSize: '1.5rem', fontWeight: 800, color: 'var(--primary)', marginTop: '0.25rem' }}>
                {stats?.totalHostels || 3} Wings
              </div>
            </div>
          </div>
        </div>

        {/* Room Type Distribution */}
        <div className="card">
          <h3 style={{ fontSize: '1.125rem', marginBottom: '1rem' }}>Room Inventory Breakdown</h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            {stats?.roomTypeDistribution && Object.entries(stats.roomTypeDistribution).map(([type, count]) => (
              <div key={type}>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.875rem', marginBottom: '0.25rem' }}>
                  <span style={{ fontWeight: 600 }}>{type} ROOMS</span>
                  <span style={{ color: 'var(--text-muted)' }}>{count} Rooms</span>
                </div>
                <div style={{ height: '8px', backgroundColor: 'var(--bg-subtle)', borderRadius: '4px', overflow: 'hidden' }}>
                  <div
                    style={{
                      width: `${(count / (stats?.totalRooms || 1)) * 100}%`,
                      height: '100%',
                      backgroundColor: 'var(--primary)',
                      borderRadius: '4px',
                    }}
                  />
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Recent Activity Section */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(400px, 1fr))', gap: '1.5rem' }}>
        {/* Recent Booking Requests */}
        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
            <h3 style={{ fontSize: '1.125rem' }}>Recent Booking Requests</h3>
            <Link to="/admin/bookings" className="btn btn-ghost btn-sm">
              View All <ArrowRight size={14} />
            </Link>
          </div>

          {recentBookings.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '2rem 1rem', color: 'var(--text-muted)', fontSize: '0.875rem' }}>
              No booking requests recorded yet.
            </div>
          ) : (
            <div className="table-container">
              <table className="table">
                <thead>
                  <tr>
                    <th>Student</th>
                    <th>Room</th>
                    <th>Check-In</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {recentBookings.map((b) => (
                    <tr key={b.id}>
                      <td>
                        <div style={{ fontWeight: 600 }}>{b.student?.fullName}</div>
                        <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{b.student?.email}</div>
                      </td>
                      <td>Room {b.room?.roomNumber}</td>
                      <td>{b.checkInDate}</td>
                      <td><Badge status={b.status} /></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* Recent Payments Stream */}
        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
            <h3 style={{ fontSize: '1.125rem' }}>Recent Fee Collections</h3>
            <Link to="/admin/fees" className="btn btn-ghost btn-sm">
              View All <ArrowRight size={14} />
            </Link>
          </div>

          {recentPayments.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '2rem 1rem', color: 'var(--text-muted)', fontSize: '0.875rem' }}>
              No rent payments recorded yet.
            </div>
          ) : (
            <div className="table-container">
              <table className="table">
                <thead>
                  <tr>
                    <th>Ref ID</th>
                    <th>Student</th>
                    <th>Amount</th>
                    <th>Method</th>
                  </tr>
                </thead>
                <tbody>
                  {recentPayments.map((p) => (
                    <tr key={p.id}>
                      <td style={{ fontFamily: 'monospace', fontSize: '0.8125rem' }}>{p.transactionRef}</td>
                      <td>{p.booking?.student?.fullName}</td>
                      <td style={{ fontWeight: 700, color: 'var(--success-text)' }}>₹{p.amount?.toLocaleString('en-IN')}</td>
                      <td><span className="badge badge-neutral">{p.method}</span></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
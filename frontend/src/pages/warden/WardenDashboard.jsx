import React, { useState, useEffect } from 'react';
import api from '../../services/api';
import { useToast } from '../../context/ToastContext';
import { StatCard } from '../../components/common/StatCard';
import { Badge } from '../../components/common/Badge';
import { LoadingSpinner } from '../../components/feedback/LoadingSpinner';
import { ErrorState } from '../../components/feedback/ErrorState';
import {
  Users,
  Bed,
  CalendarDays,
  MessageSquareWarning,
  Bell,
  CheckCircle,
  BookmarkCheck,
  ArrowRight,
  Shield,
  User,
} from 'lucide-react';
import { Link } from 'react-router-dom';

export const WardenDashboard = () => {
  const { error: toastError } = useToast();
  const [stats, setStats] = useState(null);
  const [recentLeaves, setRecentLeaves] = useState([]);
  const [recentComplaints, setRecentComplaints] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchDashboardData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [statsRes, leavesRes, complaintsRes] = await Promise.all([
        api.get('/dashboard/warden-stats'),
        api.get('/leaves'),
        api.get('/complaints'),
      ]);
      setStats(statsRes.data);
      setRecentLeaves(leavesRes.data.slice(0, 5));
      setRecentComplaints(complaintsRes.data.slice(0, 5));
    } catch (err) {
      setError(err.message);
      toastError(err.message || 'Failed to fetch warden dashboard data');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboardData();
  }, []);

  if (loading) return <LoadingSpinner text="Loading warden operations dashboard..." />;
  if (error) return <ErrorState message={error} onRetry={fetchDashboardData} />;

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
            <span className="badge badge-accent" style={{ backgroundColor: '#7c3aed', color: '#ffffff' }}>Hostel Warden Operations</span>
          </div>
          <h2 style={{ fontSize: '1.75rem', fontWeight: 800 }}>Hostel Warden Command Center</h2>
          <p style={{ color: 'var(--text-secondary)', marginTop: '0.25rem', fontSize: '0.9375rem' }}>
            Real-time hostel occupancy, resident welfare, grievance redressal, and outpass authorizations.
          </p>
        </div>
        <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
          <Link to="/profile" className="btn btn-secondary btn-sm" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.5rem' }}>
            <User size={16} /> My Profile
          </Link>
          <Link to="/warden/leaves" className="btn btn-primary btn-sm">
            Review Leaves ({stats?.pendingLeaves || 0})
          </Link>
          <Link to="/warden/complaints" className="btn btn-secondary btn-sm">
            Complaints ({stats?.pendingComplaints || 0})
          </Link>
        </div>
      </div>

      {/* Operational KPI Grid */}
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
          subtitle="Registered Active Residents"
          icon={Users}
          color="primary"
        />
        <StatCard
          title="Total Rooms"
          value={stats?.totalRooms || 0}
          subtitle={`${stats?.occupiedRooms || 0} Rooms Occupied`}
          icon={Bed}
          color="info"
        />
        <StatCard
          title="Bed Occupancy"
          value={`${occupancyPercent}%`}
          subtitle={`${stats?.occupiedBeds || 0} / ${stats?.totalCapacity || 0} Beds (${stats?.availableBeds || 0} Available)`}
          icon={Bed}
          color="success"
        />
        <StatCard
          title="Pending Leaves"
          value={stats?.pendingLeaves || 0}
          subtitle="Awaiting Warden Action"
          icon={CalendarDays}
          color="warning"
        />
        <StatCard
          title="Pending Complaints"
          value={stats?.pendingComplaints || 0}
          subtitle="Needs Grievance Action"
          icon={MessageSquareWarning}
          color="danger"
        />
        <StatCard
          title="Active Notices"
          value={stats?.activeNotices || 0}
          subtitle="Campus Announcements"
          icon={Bell}
          color="primary"
        />
      </div>

      {/* Two Column Layout: Recent Leaves & Recent Complaints */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))',
          gap: '1.5rem',
        }}
      >
        {/* Recent Leave Requests */}
        <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <h3 style={{ fontSize: '1.125rem', fontWeight: 700 }}>Recent Leave & Outpass Requests</h3>
              <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>Latest student leave applications</p>
            </div>
            <Link to="/warden/leaves" className="btn btn-ghost btn-sm" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.25rem' }}>
              View All <ArrowRight size={14} />
            </Link>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            {recentLeaves.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '2rem 1rem', color: 'var(--text-muted)' }}>
                No recent leave requests
              </div>
            ) : (
              recentLeaves.map((l) => (
                <div
                  key={l.id}
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    padding: '0.875rem 1rem',
                    borderRadius: 'var(--radius-md)',
                    backgroundColor: 'var(--bg-subtle)',
                    border: '1px solid var(--border-color)',
                  }}
                >
                  <div>
                    <div style={{ fontWeight: 600, fontSize: '0.875rem' }}>{l.student?.fullName || 'Student'}</div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '2px' }}>
                      {l.startDate} to {l.endDate} · {l.reason}
                    </div>
                  </div>
                  <Badge status={l.status} />
                </div>
              ))
            )}
          </div>
        </div>

        {/* Recent Complaints */}
        <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <h3 style={{ fontSize: '1.125rem', fontWeight: 700 }}>Recent Student Complaints</h3>
              <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>Maintenance & student grievance tickets</p>
            </div>
            <Link to="/warden/complaints" className="btn btn-ghost btn-sm" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.25rem' }}>
              View All <ArrowRight size={14} />
            </Link>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            {recentComplaints.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '2rem 1rem', color: 'var(--text-muted)' }}>
                No recent complaints
              </div>
            ) : (
              recentComplaints.map((c) => (
                <div
                  key={c.id}
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    padding: '0.875rem 1rem',
                    borderRadius: 'var(--radius-md)',
                    backgroundColor: 'var(--bg-subtle)',
                    border: '1px solid var(--border-color)',
                  }}
                >
                  <div>
                    <div style={{ fontWeight: 600, fontSize: '0.875rem' }}>{c.title}</div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '2px' }}>
                      By {c.student?.fullName || 'Student'} · Category: {c.category}
                    </div>
                  </div>
                  <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                    <Badge status={c.priority} />
                    <Badge status={c.status} />
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
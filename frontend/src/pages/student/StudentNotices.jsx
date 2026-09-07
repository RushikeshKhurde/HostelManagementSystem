import React, { useState, useEffect } from 'react';
import api from '../../services/api';
import { useToast } from '../../context/ToastContext';
import { Badge } from '../../components/common/Badge';
import { LoadingSpinner } from '../../components/feedback/LoadingSpinner';
import { EmptyState } from '../../components/feedback/EmptyState';
import { Bell, Megaphone } from 'lucide-react';

export const StudentNotices = () => {
  const { error: toastError } = useToast();
  const [notices, setNotices] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchNotices = async () => {
    setLoading(true);
    try {
      const res = await api.get('/notices');
      setNotices(res.data);
    } catch (err) {
      toastError(err.message || 'Failed to fetch notices');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchNotices();
  }, []);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      <div>
        <h2 style={{ fontSize: '1.5rem', fontWeight: 800 }}>Hostel Notice & Circular Board</h2>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>
          Official updates from the warden and hostel administration.
        </p>
      </div>

      {loading ? (
        <LoadingSpinner text="Loading notices..." />
      ) : notices.length === 0 ? (
        <EmptyState
          icon={Bell}
          title="No circulars posted"
          description="There are currently no active announcements from the administration."
        />
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          {notices.map((n) => (
            <div key={n.id} className="card card-hover" style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '0.5rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                  <div style={{ padding: '0.5rem', borderRadius: 'var(--radius-md)', backgroundColor: n.priority === 'URGENT' ? 'var(--danger-light)' : 'var(--primary-light)', color: n.priority === 'URGENT' ? 'var(--danger)' : 'var(--primary)' }}>
                    <Megaphone size={20} />
                  </div>
                  <div>
                    <h3 style={{ fontSize: '1.125rem' }}>{n.title}</h3>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                      Published by {n.postedBy || 'Administration'} · {n.createdAt ? new Date(n.createdAt).toLocaleDateString() : 'Recent'}
                    </div>
                  </div>
                </div>

                <Badge status={n.priority} />
              </div>

              <p style={{ fontSize: '0.9375rem', color: 'var(--text-secondary)', lineHeight: 1.6 }}>
                {n.content}
              </p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
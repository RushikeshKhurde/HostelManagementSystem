import React, { useState, useEffect } from 'react';
import api from '../../services/api';
import { useToast } from '../../context/ToastContext';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { Modal } from '../../components/common/Modal';
import { ConfirmDialog } from '../../components/common/ConfirmDialog';
import { LoadingSpinner } from '../../components/feedback/LoadingSpinner';
import { EmptyState } from '../../components/feedback/EmptyState';
import { Bell, Plus, Trash2, Megaphone } from 'lucide-react';

export const NoticeManagement = () => {
  const { success: toastSuccess, error: toastError } = useToast();
  const [notices, setNotices] = useState([]);
  const [loading, setLoading] = useState(true);

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [formData, setFormData] = useState({
    title: '',
    content: '',
    category: 'GENERAL',
    priority: 'NORMAL',
    targetAudience: 'ALL',
  });
  const [submitting, setSubmitting] = useState(false);

  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);

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

  const handleCreate = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      await api.post('/notices', {
        ...formData,
        postedBy: 'Chief Warden',
      });
      toastSuccess('Announcement published successfully.');
      setIsModalOpen(false);
      setFormData({ title: '', content: '', category: 'GENERAL', priority: 'NORMAL', targetAudience: 'ALL' });
      fetchNotices();
    } catch (err) {
      toastError(err.message || 'Failed to create notice');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await api.delete(`/notices/${deleteTarget.id}`);
      toastSuccess('Notice removed.');
      setDeleteTarget(null);
      fetchNotices();
    } catch (err) {
      toastError(err.message || 'Failed to delete notice');
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 800 }}>Hostel Notice Board</h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>
            Publish official announcements, maintenance alerts, and hostel circulars.
          </p>
        </div>
        <Button variant="primary" icon={Plus} onClick={() => setIsModalOpen(true)}>
          Publish Announcement
        </Button>
      </div>

      {loading ? (
        <LoadingSpinner text="Loading notices..." />
      ) : notices.length === 0 ? (
        <EmptyState
          icon={Bell}
          title="No notices published"
          description="There are currently no active announcements."
          actionText="Publish First Notice"
          onAction={() => setIsModalOpen(true)}
        />
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          {notices.map((n) => (
            <div key={n.id} className="card card-hover" style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                  <div style={{ padding: '0.5rem', borderRadius: 'var(--radius-md)', backgroundColor: n.priority === 'URGENT' ? 'var(--danger-light)' : 'var(--primary-light)', color: n.priority === 'URGENT' ? 'var(--danger)' : 'var(--primary)' }}>
                    <Megaphone size={20} />
                  </div>
                  <div>
                    <h3 style={{ fontSize: '1.125rem' }}>{n.title}</h3>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                      Audience: {n.targetAudience} · Posted on {n.createdAt ? new Date(n.createdAt).toLocaleDateString() : 'Recent'}
                    </div>
                  </div>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <Badge status={n.priority} />
                  <Button variant="ghost" size="sm" style={{ color: 'var(--danger)' }} icon={Trash2} onClick={() => setDeleteTarget(n)} />
                </div>
              </div>

              <p style={{ fontSize: '0.9375rem', color: 'var(--text-secondary)', lineHeight: 1.6 }}>
                {n.content}
              </p>
            </div>
          ))}
        </div>
      )}

      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Publish New Announcement"
        maxWidth="540px"
      >
        <form onSubmit={handleCreate} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          <div>
            <label className="form-label">Notice Title</label>
            <input
              className="form-input"
              required
              placeholder="e.g. Scheduled Water Tank Cleaning & Power Outage"
              value={formData.title}
              onChange={(e) => setFormData({ ...formData, title: e.target.value })}
            />
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
            <div>
              <label className="form-label">Priority</label>
              <select
                className="form-select"
                value={formData.priority}
                onChange={(e) => setFormData({ ...formData, priority: e.target.value })}
              >
                <option value="NORMAL">Normal Notice</option>
                <option value="IMPORTANT">Important</option>
                <option value="URGENT">Urgent / Emergency</option>
              </select>
            </div>

            <div>
              <label className="form-label">Target Wing</label>
              <select
                className="form-select"
                value={formData.targetAudience}
                onChange={(e) => setFormData({ ...formData, targetAudience: e.target.value })}
              >
                <option value="ALL">All Blocks (A, B, C)</option>
                <option value="BLOCK_A">Block A Only</option>
                <option value="BLOCK_B">Block B Only</option>
                <option value="BLOCK_C">Block C Only</option>
              </select>
            </div>
          </div>

          <div>
            <label className="form-label">Notice Content</label>
            <textarea
              className="form-textarea"
              rows={4}
              required
              placeholder="Write the full circular description here..."
              value={formData.content}
              onChange={(e) => setFormData({ ...formData, content: e.target.value })}
            />
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
            <Button type="button" variant="secondary" onClick={() => setIsModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" variant="primary" loading={submitting}>
              Publish Announcement
            </Button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        isOpen={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
        title="Delete Announcement"
        message={`Are you sure you want to remove "${deleteTarget?.title}"?`}
        confirmText="Delete"
        danger
        loading={deleting}
      />
    </div>
  );
};
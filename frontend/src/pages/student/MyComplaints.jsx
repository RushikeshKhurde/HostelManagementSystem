import React, { useState, useEffect } from 'react';
import api from '../../services/api';
import { useToast } from '../../context/ToastContext';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { Modal } from '../../components/common/Modal';
import { LoadingSpinner } from '../../components/feedback/LoadingSpinner';
import { EmptyState } from '../../components/feedback/EmptyState';
import { MessageSquareWarning, Plus, CheckCircle2 } from 'lucide-react';

export const MyComplaints = () => {
  const { success: toastSuccess, error: toastError } = useToast();
  const [complaints, setComplaints] = useState([]);
  const [loading, setLoading] = useState(true);

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [formData, setFormData] = useState({
    title: '',
    category: 'ELECTRICITY',
    description: '',
    roomNumber: '',
    priority: 'MEDIUM',
  });
  const [submitting, setSubmitting] = useState(false);

  const fetchComplaints = async () => {
    setLoading(true);
    try {
      const res = await api.get('/complaints/my');
      setComplaints(res.data);
    } catch (err) {
      toastError(err.message || 'Failed to fetch complaints');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchComplaints();
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      await api.post('/complaints', formData);
      toastSuccess('Grievance submitted successfully. Maintenance team has been notified.');
      setIsModalOpen(false);
      setFormData({ title: '', category: 'ELECTRICITY', description: '', roomNumber: '', priority: 'MEDIUM' });
      fetchComplaints();
    } catch (err) {
      toastError(err.message || 'Failed to submit complaint');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 800 }}>My Grievance Tickets</h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>
            Report room maintenance, electrical, plumbing, or hygiene issues.
          </p>
        </div>
        <Button variant="primary" icon={Plus} onClick={() => setIsModalOpen(true)}>
          Lodge New Complaint
        </Button>
      </div>

      {loading ? (
        <LoadingSpinner text="Loading your grievances..." />
      ) : complaints.length === 0 ? (
        <EmptyState
          icon={MessageSquareWarning}
          title="No complaints filed"
          description="You have not filed any maintenance tickets or complaints."
          actionText="Lodge a Complaint"
          onAction={() => setIsModalOpen(true)}
        />
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          {complaints.map((c) => (
            <div key={c.id} className="card card-hover" style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '0.5rem' }}>
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <h3 style={{ fontSize: '1.125rem' }}>{c.title}</h3>
                    <span className="badge badge-neutral">{c.category}</span>
                  </div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
                    {c.roomNumber ? `Room: ${c.roomNumber} · ` : ''}Filed on {c.createdAt ? new Date(c.createdAt).toLocaleDateString() : 'Recent'}
                  </div>
                </div>

                <div style={{ display: 'flex', gap: '0.5rem' }}>
                  <Badge status={c.priority} />
                  <Badge status={c.status} />
                </div>
              </div>

              <p style={{ fontSize: '0.9375rem', color: 'var(--text-secondary)' }}>{c.description}</p>

              {c.adminComment && (
                <div style={{ padding: '0.75rem', backgroundColor: 'var(--bg-subtle)', borderRadius: 'var(--radius-md)', fontSize: '0.875rem', borderLeft: '3px solid var(--primary)' }}>
                  <strong>Admin Response:</strong> {c.adminComment}
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {/* Lodge Complaint Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Lodge Grievance / Maintenance Ticket"
        maxWidth="520px"
      >
        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          <div>
            <label className="form-label">Subject / Issue Title</label>
            <input
              className="form-input"
              required
              placeholder="e.g. Ceiling fan not working in Room A-102"
              value={formData.title}
              onChange={(e) => setFormData({ ...formData, title: e.target.value })}
            />
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
            <div>
              <label className="form-label">Category</label>
              <select
                className="form-select"
                value={formData.category}
                onChange={(e) => setFormData({ ...formData, category: e.target.value })}
              >
                <option value="ELECTRICITY">Electricity / Power</option>
                <option value="PLUMBING">Plumbing / Water</option>
                <option value="CLEANLINESS">Cleanliness / Hygiene</option>
                <option value="INTERNET">Wi-Fi & Internet</option>
                <option value="FURNITURE">Carpentry / Furniture</option>
                <option value="OTHER">Other Issue</option>
              </select>
            </div>

            <div>
              <label className="form-label">Priority</label>
              <select
                className="form-select"
                value={formData.priority}
                onChange={(e) => setFormData({ ...formData, priority: e.target.value })}
              >
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High / Urgent</option>
              </select>
            </div>
          </div>

          <div>
            <label className="form-label">Room / Location</label>
            <input
              className="form-input"
              placeholder="e.g. Room A-102 or 2nd Floor Study Lounge"
              value={formData.roomNumber}
              onChange={(e) => setFormData({ ...formData, roomNumber: e.target.value })}
            />
          </div>

          <div>
            <label className="form-label">Detailed Description</label>
            <textarea
              className="form-textarea"
              rows={3}
              required
              placeholder="Please describe the issue in detail so maintenance can assist..."
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
            />
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
            <Button type="button" variant="secondary" onClick={() => setIsModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" variant="primary" loading={submitting}>
              Submit Ticket
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
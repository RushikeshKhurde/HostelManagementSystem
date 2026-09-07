import React, { useState, useEffect } from 'react';
import api from '../../services/api';
import { useToast } from '../../context/ToastContext';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { Modal } from '../../components/common/Modal';
import { LoadingSpinner } from '../../components/feedback/LoadingSpinner';
import { EmptyState } from '../../components/feedback/EmptyState';
import { CalendarDays, Plus } from 'lucide-react';

export const MyLeave = () => {
  const { success: toastSuccess, error: toastError } = useToast();
  const [leaves, setLeaves] = useState([]);
  const [loading, setLoading] = useState(true);

  // Apply Modal
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [formData, setFormData] = useState({
    leaveType: 'HOME_VISIT',
    startDate: '',
    endDate: '',
    reason: '',
    emergencyContact: '',
  });
  const [submitting, setSubmitting] = useState(false);

  const fetchLeaves = async () => {
    setLoading(true);
    try {
      const res = await api.get('/leaves/my');
      setLeaves(res.data);
    } catch (err) {
      toastError(err.message || 'Failed to fetch leave requests');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLeaves();
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (new Date(formData.endDate) < new Date(formData.startDate)) {
      toastError('End date cannot be before start date.');
      return;
    }
    setSubmitting(true);
    try {
      await api.post('/leaves', formData);
      toastSuccess('Leave request submitted. Warden has been notified.');
      setIsModalOpen(false);
      setFormData({ leaveType: 'HOME_VISIT', startDate: '', endDate: '', reason: '', emergencyContact: '' });
      fetchLeaves();
    } catch (err) {
      toastError(err.message || 'Failed to apply for leave');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 800 }}>My Leave & Outpass Applications</h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>
            Apply for home visits, weekend night-outs, or medical leave.
          </p>
        </div>
        <Button variant="primary" icon={Plus} onClick={() => setIsModalOpen(true)}>
          Apply for Leave
        </Button>
      </div>

      {loading ? (
        <LoadingSpinner text="Loading leave applications..." />
      ) : leaves.length === 0 ? (
        <EmptyState
          icon={CalendarDays}
          title="No leave requests filed"
          description="You have not submitted any leave applications."
          actionText="Apply for Leave"
          onAction={() => setIsModalOpen(true)}
        />
      ) : (
        <div className="table-container">
          <table className="table">
            <thead>
              <tr>
                <th>Leave Type</th>
                <th>Dates (Start to End)</th>
                <th>Reason</th>
                <th>Emergency Phone</th>
                <th>Status</th>
                <th>Warden Remarks</th>
              </tr>
            </thead>
            <tbody>
              {leaves.map((l) => (
                <tr key={l.id}>
                  <td><span className="badge badge-neutral">{l.leaveType}</span></td>
                  <td><strong>{l.startDate}</strong> → <strong>{l.endDate}</strong></td>
                  <td style={{ maxWidth: '240px' }}>{l.reason}</td>
                  <td>{l.emergencyContact || 'N/A'}</td>
                  <td><Badge status={l.status} /></td>
                  <td style={{ color: 'var(--text-muted)', fontSize: '0.8125rem' }}>
                    {l.adminRemarks || 'None'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Apply Leave Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Apply for Hostel Outpass / Leave"
        maxWidth="500px"
      >
        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          <div>
            <label className="form-label">Leave Category</label>
            <select
              className="form-select"
              value={formData.leaveType}
              onChange={(e) => setFormData({ ...formData, leaveType: e.target.value })}
            >
              <option value="HOME_VISIT">Home Visit</option>
              <option value="WEEKEND_OUT">Weekend Night Out</option>
              <option value="MEDICAL">Medical Emergency</option>
              <option value="ACADEMIC_EVENT">Academic / Internship Trip</option>
              <option value="OTHER">Other</option>
            </select>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
            <div>
              <label className="form-label">Start Date</label>
              <input
                type="date"
                className="form-input"
                required
                value={formData.startDate}
                onChange={(e) => setFormData({ ...formData, startDate: e.target.value })}
              />
            </div>

            <div>
              <label className="form-label">End Date</label>
              <input
                type="date"
                className="form-input"
                required
                value={formData.endDate}
                onChange={(e) => setFormData({ ...formData, endDate: e.target.value })}
              />
            </div>
          </div>

          <div>
            <label className="form-label">Guardian / Emergency Contact Number</label>
            <input
              type="tel"
              className="form-input"
              required
              placeholder="10-digit mobile number"
              value={formData.emergencyContact}
              onChange={(e) => setFormData({ ...formData, emergencyContact: e.target.value })}
            />
          </div>

          <div>
            <label className="form-label">Reason for Leave</label>
            <textarea
              className="form-textarea"
              rows={3}
              required
              placeholder="State clear purpose of travel/leave..."
              value={formData.reason}
              onChange={(e) => setFormData({ ...formData, reason: e.target.value })}
            />
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
            <Button type="button" variant="secondary" onClick={() => setIsModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" variant="primary" loading={submitting}>
              Submit Application
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
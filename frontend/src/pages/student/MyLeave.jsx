import React, { useState, useEffect } from 'react';
import api from '../../services/api';
import { useToast } from '../../context/ToastContext';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { Modal } from '../../components/common/Modal';
import { LoadingSpinner } from '../../components/feedback/LoadingSpinner';
import { EmptyState } from '../../components/feedback/EmptyState';
import { CalendarDays, Plus, AlertCircle, Info } from 'lucide-react';

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

  const approvedLeaves = leaves.filter((l) => l.status === 'APPROVED');

  const getConflictingApprovedLeave = (start, end) => {
    if (!start || !end) return null;
    const s = new Date(start);
    const e = new Date(end);
    if (isNaN(s.getTime()) || isNaN(e.getTime()) || s > e) return null;

    return approvedLeaves.find((al) => {
      const existingStart = new Date(al.startDate);
      const existingEnd = new Date(al.endDate);
      return s <= existingEnd && e >= existingStart;
    });
  };

  const getNextAvailableDate = (endDateStr) => {
    if (!endDateStr) return '';
    const d = new Date(endDateStr);
    d.setDate(d.getDate() + 1);
    return d.toISOString().split('T')[0];
  };

  const formatDateDisplay = (dateStr) => {
    if (!dateStr) return '';
    try {
      const parts = dateStr.split('-');
      if (parts.length === 3) {
        const d = new Date(parts[0], parts[1] - 1, parts[2]);
        return d.toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' });
      }
      return new Date(dateStr).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' });
    } catch {
      return dateStr;
    }
  };

  const selectedConflict = getConflictingApprovedLeave(formData.startDate, formData.endDate);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (new Date(formData.endDate) < new Date(formData.startDate)) {
      toastError('End date cannot be before start date.');
      return;
    }

    if (selectedConflict) {
      toastError(
        `Overlaps with your approved leave from ${formatDateDisplay(selectedConflict.startDate)} to ${formatDateDisplay(selectedConflict.endDate)}.`
      );
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
      const msg = err.response?.data?.message || err.message || 'Failed to apply for leave';
      toastError(msg);
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
          {approvedLeaves.length > 0 && (
            <div style={{ padding: '0.65rem 0.85rem', backgroundColor: 'var(--bg-subtle)', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)', fontSize: '0.8125rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', fontWeight: 600, color: 'var(--text-primary)', marginBottom: '0.25rem' }}>
                <Info size={14} color="var(--primary)" />
                <span>Active Approved Leaves:</span>
              </div>
              <ul style={{ margin: 0, paddingLeft: '1.2rem', color: 'var(--text-secondary)' }}>
                {approvedLeaves.map((al) => (
                  <li key={al.id} style={{ marginTop: '0.15rem' }}>
                    <strong>{formatDateDisplay(al.startDate)}</strong> to <strong>{formatDateDisplay(al.endDate)}</strong> ({al.leaveType})
                    {' — Available from: '}
                    <span style={{ color: 'var(--primary)', fontWeight: 600 }}>{formatDateDisplay(getNextAvailableDate(al.endDate))}</span>
                  </li>
                ))}
              </ul>
            </div>
          )}

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

          {selectedConflict && (
            <div style={{ display: 'flex', alignItems: 'flex-start', gap: '0.4rem', color: '#b91c1c', fontSize: '0.8125rem', padding: '0.5rem 0.75rem', backgroundColor: '#fef2f2', border: '1px solid #fecaca', borderRadius: 'var(--radius-md)' }}>
              <AlertCircle size={16} style={{ flexShrink: 0, marginTop: '2px' }} />
              <span>
                Your approved leave from <strong>{formatDateDisplay(selectedConflict.startDate)}</strong> to <strong>{formatDateDisplay(selectedConflict.endDate)}</strong> prevents you from applying for leave on overlapping dates. You can apply for leave starting <strong>{formatDateDisplay(getNextAvailableDate(selectedConflict.endDate))}</strong>.
              </span>
            </div>
          )}

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

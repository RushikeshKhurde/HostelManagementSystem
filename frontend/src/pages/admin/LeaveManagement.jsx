import React, { useState, useEffect } from 'react';
import api from '../../services/api';
import { useToast } from '../../context/ToastContext';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { Modal } from '../../components/common/Modal';
import { LoadingSpinner } from '../../components/feedback/LoadingSpinner';
import { EmptyState } from '../../components/feedback/EmptyState';
import { CalendarDays, Search, Check, X } from 'lucide-react';

export const LeaveManagement = () => {
  const { success: toastSuccess, error: toastError } = useToast();
  const [leaves, setLeaves] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');

  const [selectedLeave, setSelectedLeave] = useState(null);
  const [actionType, setActionType] = useState('APPROVED');
  const [remarks, setRemarks] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const fetchLeaves = async () => {
    setLoading(true);
    try {
      const res = await api.get('/leaves');
      setLeaves(res.data);
    } catch (err) {
      toastError(err.message || 'Failed to fetch leaves');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLeaves();
  }, []);

  const handleOpenAction = (leave, action) => {
    setSelectedLeave(leave);
    setActionType(action);
    setRemarks('');
  };

  const handleConfirmAction = async (e) => {
    e.preventDefault();
    if (!selectedLeave) return;
    setSubmitting(true);
    try {
      await api.put(`/leaves/${selectedLeave.id}/status`, {
        status: actionType,
        adminRemarks: remarks.trim(),
      });
      toastSuccess(`Leave application marked as ${actionType}.`);
      setSelectedLeave(null);
      fetchLeaves();
    } catch (err) {
      toastError(err.message || 'Failed to update leave request');
    } finally {
      setSubmitting(false);
    }
  };

  const filtered = leaves.filter((l) => {
    const student = l.student?.fullName || '';
    const reason = l.reason || '';
    const matchesSearch =
      student.toLowerCase().includes(search.toLowerCase()) ||
      reason.toLowerCase().includes(search.toLowerCase());
    const matchesStatus = statusFilter === 'ALL' || l.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      <div>
        <h2 style={{ fontSize: '1.5rem', fontWeight: 800 }}>Student Leave & Outpass Approvals</h2>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>
          Review and approve student night outs, home visits, and medical leave requests.
        </p>
      </div>

      <div
        className="card"
        style={{
          padding: '1rem 1.25rem',
          display: 'flex',
          gap: '1rem',
          alignItems: 'center',
          flexWrap: 'wrap',
          justifyContent: 'space-between',
        }}
      >
        <div style={{ position: 'relative', minWidth: '240px', flex: 1 }}>
          <Search size={16} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
          <input
            type="text"
            placeholder="Search leave requests by student or reason..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="form-input"
            style={{ paddingLeft: '2.25rem' }}
          />
        </div>

        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          className="form-select"
          style={{ width: 'auto', minWidth: '160px' }}
        >
          <option value="ALL">All Statuses</option>
          <option value="PENDING">Pending Review</option>
          <option value="APPROVED">Approved</option>
          <option value="REJECTED">Rejected</option>
        </select>
      </div>

      {loading ? (
        <LoadingSpinner text="Loading leave requests..." />
      ) : filtered.length === 0 ? (
        <EmptyState
          icon={CalendarDays}
          title="No leave requests found"
          description="There are currently no leave applications matching your criteria."
        />
      ) : (
        <div className="table-container">
          <table className="table">
            <thead>
              <tr>
                <th>Student</th>
                <th>Leave Type</th>
                <th>Duration (Start - End)</th>
                <th>Reason</th>
                <th>Emergency Contact</th>
                <th>Status</th>
                <th style={{ textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((l) => (
                <tr key={l.id}>
                  <td>
                    <div style={{ fontWeight: 700 }}>{l.student?.fullName}</div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{l.student?.email}</div>
                  </td>
                  <td><span className="badge badge-neutral">{l.leaveType}</span></td>
                  <td>{l.startDate} → {l.endDate}</td>
                  <td style={{ maxWidth: '220px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {l.reason}
                  </td>
                  <td>{l.emergencyContact || 'N/A'}</td>
                  <td><Badge status={l.status} /></td>
                  <td style={{ textAlign: 'right' }}>
                    {l.status === 'PENDING' ? (
                      <div style={{ display: 'inline-flex', gap: '0.5rem' }}>
                        <Button
                          variant="primary"
                          size="sm"
                          icon={Check}
                          onClick={() => handleOpenAction(l, 'APPROVED')}
                        >
                          Approve
                        </Button>
                        <Button
                          variant="outlineDanger"
                          size="sm"
                          icon={X}
                          onClick={() => handleOpenAction(l, 'REJECTED')}
                        >
                          Reject
                        </Button>
                      </div>
                    ) : (
                      <span style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
                        {l.adminRemarks ? `Remark: ${l.adminRemarks}` : 'Reviewed'}
                      </span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <Modal
        isOpen={!!selectedLeave}
        onClose={() => setSelectedLeave(null)}
        title={`${actionType === 'APPROVED' ? 'Approve' : 'Reject'} Leave Application`}
        maxWidth="460px"
      >
        {selectedLeave && (
          <form onSubmit={handleConfirmAction} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <p style={{ fontSize: '0.9375rem', color: 'var(--text-secondary)' }}>
              Confirm setting leave application for <strong>{selectedLeave.student?.fullName}</strong> ({selectedLeave.startDate} to {selectedLeave.endDate}) to <strong>{actionType}</strong>.
            </p>
            <div>
              <label className="form-label">Warden Remarks (Optional)</label>
              <textarea
                className="form-textarea"
                rows={2}
                placeholder="e.g. Verified with guardian."
                value={remarks}
                onChange={(e) => setRemarks(e.target.value)}
              />
            </div>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem' }}>
              <Button type="button" variant="secondary" onClick={() => setSelectedLeave(null)}>
                Cancel
              </Button>
              <Button type="submit" variant={actionType === 'APPROVED' ? 'primary' : 'danger'} loading={submitting}>
                Confirm {actionType}
              </Button>
            </div>
          </form>
        )}
      </Modal>
    </div>
  );
};
import React, { useState, useEffect } from 'react';
import api from '../../services/api';
import { useToast } from '../../context/ToastContext';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { Modal } from '../../components/common/Modal';
import { LoadingSpinner } from '../../components/feedback/LoadingSpinner';
import { EmptyState } from '../../components/feedback/EmptyState';
import { ComplaintImage } from '../../components/complaint/ComplaintImage';
import { MessageSquareWarning, Search, Edit3, Image as ImageIcon, User, MapPin } from 'lucide-react';

export const ComplaintManagement = () => {
  const { success: toastSuccess, error: toastError } = useToast();
  const [complaints, setComplaints] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');

  const [selectedComplaint, setSelectedComplaint] = useState(null);
  const [newStatus, setNewStatus] = useState('IN_PROGRESS');
  const [adminComment, setAdminComment] = useState('');
  const [updating, setUpdating] = useState(false);

  const fetchComplaints = async () => {
    setLoading(true);
    try {
      const res = await api.get('/complaints');
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

  const handleOpenUpdate = (complaint) => {
    setSelectedComplaint(complaint);
    setNewStatus(complaint.status);
    setAdminComment(complaint.adminComment || '');
  };

  const handleSaveStatus = async (e) => {
    e.preventDefault();
    if (!selectedComplaint) return;
    setUpdating(true);
    try {
      await api.put(`/complaints/${selectedComplaint.id}/status`, {
        status: newStatus,
        adminComment: adminComment.trim(),
      });
      toastSuccess('Complaint updated successfully. Student has been notified.');
      setSelectedComplaint(null);
      fetchComplaints();
    } catch (err) {
      toastError(err.message || 'Failed to update complaint');
    } finally {
      setUpdating(false);
    }
  };

  const filtered = complaints.filter((c) => {
    const student = c.student?.fullName || '';
    const email = c.student?.email || '';
    const title = c.title || '';
    const feedback = c.feedback || '';
    const desc = c.description || '';
    const visibility = c.visibility || '';
    const query = search.toLowerCase();

    const matchesSearch =
      student.toLowerCase().includes(query) ||
      email.toLowerCase().includes(query) ||
      title.toLowerCase().includes(query) ||
      feedback.toLowerCase().includes(query) ||
      desc.toLowerCase().includes(query) ||
      visibility.toLowerCase().includes(query);

    const matchesStatus = statusFilter === 'ALL' || c.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      <div>
        <h2 style={{ fontSize: '1.5rem', fontWeight: 800 }}>Student Grievance Redressal</h2>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>
          Monitor, assign, and resolve maintenance and student complaints.
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
            placeholder="Search complaints by student, title, or message..."
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
          <option value="PENDING">Pending</option>
          <option value="IN_PROGRESS">In Progress</option>
          <option value="RESOLVED">Resolved</option>
          <option value="REJECTED">Rejected</option>
        </select>
      </div>

      {loading ? (
        <LoadingSpinner text="Loading grievances..." />
      ) : filtered.length === 0 ? (
        <EmptyState
          icon={MessageSquareWarning}
          title="No complaints found"
          description="There are currently no complaints matching your criteria."
        />
      ) : (
        <div className="table-container">
          <table className="table">
            <thead>
              <tr>
                <th>Student</th>
                <th>Category & Grievance</th>
                <th>Audience</th>
                <th>Attachment</th>
                <th>Room</th>
                <th>Priority</th>
                <th>Status</th>
                <th>Submitted</th>
                <th style={{ textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((c) => (
                <tr key={c.id}>
                  <td>
                    <div style={{ fontWeight: 700 }}>{c.student?.fullName || 'Student'}</div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{c.student?.email || c.student?.mobileNumber}</div>
                  </td>
                  <td>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                      <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--primary)' }}>#{c.id}</span>
                      <span style={{ fontWeight: 600 }}>{c.title}</span>
                    </div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{c.category}</div>
                  </td>
                  <td>
                    {(() => {
                      const v = c.visibility;
                      if (v === 'WARDEN_ONLY') {
                        return (
                          <span
                            style={{
                              display: 'inline-block',
                              fontSize: '0.75rem',
                              fontWeight: 600,
                              padding: '0.2rem 0.5rem',
                              borderRadius: '9999px',
                              backgroundColor: '#fef3c7',
                              color: '#92400e',
                              border: '1px solid #fde68a',
                            }}
                          >
                            Warden Only
                          </span>
                        );
                      }
                      if (v === 'ADMIN_ONLY') {
                        return (
                          <span
                            style={{
                              display: 'inline-block',
                              fontSize: '0.75rem',
                              fontWeight: 600,
                              padding: '0.2rem 0.5rem',
                              borderRadius: '9999px',
                              backgroundColor: '#e0f2fe',
                              color: '#075985',
                              border: '1px solid #bae6fd',
                            }}
                          >
                            Admin Only
                          </span>
                        );
                      }
                      return (
                        <span
                          style={{
                            display: 'inline-block',
                            fontSize: '0.75rem',
                            fontWeight: 600,
                            padding: '0.2rem 0.5rem',
                            borderRadius: '9999px',
                            backgroundColor: 'var(--bg-subtle)',
                            color: 'var(--text-secondary)',
                            border: '1px solid var(--border-color)',
                          }}
                        >
                          Admin &amp; Warden
                        </span>
                      );
                    })()}
                  </td>
                  <td>
                    {c.hasImage ? (
                      <ComplaintImage complaintId={c.id} imageName={c.imageName} thumbnail={true} />
                    ) : (
                      <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>None</span>
                    )}
                  </td>
                  <td>{c.roomNumber || 'General'}</td>
                  <td><Badge status={c.priority} /></td>
                  <td><Badge status={c.status} /></td>
                  <td>{c.createdAt ? new Date(c.createdAt).toLocaleDateString() : 'N/A'}</td>
                  <td style={{ textAlign: 'right' }}>
                    <Button variant="secondary" size="sm" icon={Edit3} onClick={() => handleOpenUpdate(c)}>
                      Review / Update
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Review & Status Update Modal */}
      <Modal
        isOpen={!!selectedComplaint}
        onClose={() => setSelectedComplaint(null)}
        title="Review & Update Grievance Ticket"
        maxWidth="560px"
      >
        {selectedComplaint && (
          <form onSubmit={handleSaveStatus} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            {/* Student & Ticket Details Card */}
            <div style={{ padding: '0.85rem', backgroundColor: 'var(--bg-subtle)', borderRadius: 'var(--radius-md)', fontSize: '0.875rem', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontWeight: 700 }}>
                  <User size={15} color="var(--primary)" />
                  <span>{selectedComplaint.student?.fullName || 'Student'}</span>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 400 }}>
                    ({selectedComplaint.student?.email || selectedComplaint.student?.mobileNumber})
                  </span>
                </div>
                <div style={{ display: 'flex', gap: '0.35rem' }}>
                  <Badge status={selectedComplaint.priority} />
                  <Badge status={selectedComplaint.status} />
                </div>
              </div>

              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.75rem', color: 'var(--text-muted)', flexWrap: 'wrap' }}>
                <span><strong>Category:</strong> {selectedComplaint.category}</span>
                <span>· <strong>Audience:</strong> {
                  selectedComplaint.visibility === 'WARDEN_ONLY' ? 'Warden Only' :
                  selectedComplaint.visibility === 'ADMIN_ONLY' ? 'Admin Only' : 'Admin & Warden'
                }</span>
                {selectedComplaint.roomNumber && (
                  <span>· <strong>Location:</strong> {selectedComplaint.roomNumber}</span>
                )}
                <span>· <strong>Date:</strong> {selectedComplaint.createdAt ? new Date(selectedComplaint.createdAt).toLocaleString() : 'Recent'}</span>
              </div>

              {/* Main Grievance / Feedback */}
              <div style={{ marginTop: '0.35rem', paddingTop: '0.5rem', borderTop: '1px solid var(--border-color)' }}>
                <div style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-muted)', marginBottom: '0.2rem' }}>
                  GRIEVANCE / FEEDBACK:
                </div>
                <div style={{ color: 'var(--text-primary)', whiteSpace: 'pre-wrap', lineHeight: '1.4' }}>
                  {selectedComplaint.feedback || selectedComplaint.description}
                </div>
              </div>

              {/* Additional Description if present */}
              {selectedComplaint.description && selectedComplaint.feedback && selectedComplaint.description !== selectedComplaint.feedback && (
                <div style={{ marginTop: '0.35rem' }}>
                  <div style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-muted)', marginBottom: '0.2rem' }}>
                    ADDITIONAL DETAILS:
                  </div>
                  <div style={{ color: 'var(--text-secondary)', whiteSpace: 'pre-wrap', fontSize: '0.8125rem' }}>
                    {selectedComplaint.description}
                  </div>
                </div>
              )}

              {/* Attached Image if present */}
              {selectedComplaint.hasImage && (
                <div style={{ marginTop: '0.4rem' }}>
                  <div style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-muted)', marginBottom: '0.3rem' }}>
                    ATTACHED PHOTO:
                  </div>
                  <ComplaintImage complaintId={selectedComplaint.id} imageName={selectedComplaint.imageName} thumbnail={true} />
                </div>
              )}
            </div>

            <div>
              <label className="form-label">Update Status</label>
              <select
                className="form-select"
                value={newStatus}
                onChange={(e) => setNewStatus(e.target.value)}
              >
                <option value="PENDING">Pending</option>
                <option value="IN_PROGRESS">In Progress / Assigned</option>
                <option value="RESOLVED">Resolved</option>
                <option value="REJECTED">Rejected</option>
              </select>
            </div>

            <div>
              <label className="form-label">Hostel Staff Resolution Comment</label>
              <textarea
                className="form-textarea"
                rows={3}
                placeholder="e.g. Electrician visited and repaired switchboard. Work verified."
                value={adminComment}
                onChange={(e) => setAdminComment(e.target.value)}
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
              <Button type="button" variant="secondary" onClick={() => setSelectedComplaint(null)}>
                Cancel
              </Button>
              <Button type="submit" variant="primary" loading={updating}>
                Save Changes
              </Button>
            </div>
          </form>
        )}
      </Modal>
    </div>
  );
};

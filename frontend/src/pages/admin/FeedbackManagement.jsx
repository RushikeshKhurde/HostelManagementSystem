import React, { useState, useEffect } from 'react';
import api from '../../services/api';
import { useToast } from '../../context/ToastContext';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { Modal } from '../../components/common/Modal';
import { ConfirmDialog } from '../../components/common/ConfirmDialog';
import { LoadingSpinner } from '../../components/feedback/LoadingSpinner';
import { EmptyState } from '../../components/feedback/EmptyState';
import {
  MessageSquare,
  Search,
  Eye,
  Trash2,
  Calendar,
  User as UserIcon,
  CheckCircle2,
  Shield,
  FileText,
} from 'lucide-react';

export const FeedbackManagement = ({ isWarden = false }) => {
  const { success: toastSuccess, error: toastError } = useToast();

  const [submissions, setSubmissions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [selectedSubmission, setSelectedSubmission] = useState(null);

  // Admin delete states
  const [submissionToDelete, setSubmissionToDelete] = useState(null);
  const [deleting, setDeleting] = useState(false);

  const fetchFeedback = async () => {
    setLoading(true);
    try {
      const res = await api.get('/feedback');
      setSubmissions(res.data || []);
    } catch (err) {
      toastError(err.message || 'Failed to fetch student feedback submissions');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchFeedback();
  }, []);

  const handleDelete = async () => {
    if (isWarden || !submissionToDelete) return;
    setDeleting(true);
    try {
      await api.delete(`/feedback/admin/${submissionToDelete.id}`);
      toastSuccess(`Feedback from ${submissionToDelete.studentName} deleted successfully.`);
      setSubmissionToDelete(null);
      if (selectedSubmission?.id === submissionToDelete.id) {
        setSelectedSubmission(null);
      }
      fetchFeedback();
    } catch (err) {
      toastError(err.message || 'Failed to delete student feedback');
    } finally {
      setDeleting(false);
    }
  };

  const filteredSubmissions = submissions.filter((sub) => {
    const term = search.toLowerCase();
    const name = (sub.studentName || '').toLowerCase();
    const uname = (sub.studentUsername || '').toLowerCase();
    const email = (sub.studentEmail || '').toLowerCase();
    return name.includes(term) || uname.includes(term) || email.includes(term);
  });

  const formatDateTime = (dateStr) => {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    return d.toLocaleString('en-IN', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Page Header */}
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
            <span
              className="badge badge-accent"
              style={{ backgroundColor: isWarden ? '#7c3aed' : 'var(--primary)', color: '#ffffff' }}
            >
              {isWarden ? 'Hostel Warden Review' : 'System Administration'}
            </span>
            {isWarden && (
              <span className="badge badge-neutral" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.25rem' }}>
                <Shield size={12} /> Read-Only
              </span>
            )}
          </div>
          <h2 style={{ fontSize: '1.75rem', fontWeight: 800, color: 'var(--text-primary)' }}>
            {isWarden ? 'Student Feedback Directory' : 'Student Feedback Management'}
          </h2>
          <p style={{ color: 'var(--text-secondary)', marginTop: '0.25rem', fontSize: '0.9375rem' }}>
            {isWarden
              ? 'Warden inspection of resident questionnaires covering room cleanliness, mess, water, Wi-Fi, and security.'
              : 'Monitor resident satisfaction ratings, inspect questionnaire answers, and maintain feedback integrity.'}
          </p>
        </div>

        {/* Counter Pill */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '0.75rem',
            padding: '0.75rem 1.25rem',
            backgroundColor: 'var(--bg-surface)',
            border: '1px solid var(--border-color)',
            borderRadius: 'var(--radius-lg)',
          }}
        >
          <div style={{ textAlign: 'right' }}>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 700 }}>
              Total Responses
            </div>
            <div style={{ fontSize: '1.5rem', fontWeight: 800, color: 'var(--text-primary)' }}>
              {submissions.length}
            </div>
          </div>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div
        className="card"
        style={{
          padding: '1rem 1.25rem',
          display: 'flex',
          gap: '1rem',
          alignItems: 'center',
          flexWrap: 'wrap',
        }}
      >
        <div style={{ position: 'relative', flex: 1, minWidth: '240px' }}>
          <Search
            size={18}
            style={{
              position: 'absolute',
              left: '0.75rem',
              top: '50%',
              transform: 'translateY(-50%)',
              color: 'var(--text-muted)',
            }}
          />
          <input
            type="text"
            placeholder="Search by student name, username, or email..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="input"
            style={{ paddingLeft: '2.5rem', width: '100%' }}
          />
        </div>
      </div>

      {/* Submissions List */}
      {loading ? (
        <div style={{ padding: '3rem 1rem', textAlign: 'center' }}>
          <LoadingSpinner text="Loading feedback submissions..." />
        </div>
      ) : filteredSubmissions.length === 0 ? (
        <EmptyState
          icon={MessageSquare}
          title={search ? 'No matching submissions found' : 'No feedback submissions yet'}
          message={
            search
              ? 'Try modifying your search filter.'
              : 'Students have not submitted any feedback questionnaires yet.'
          }
        />
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(360px, 1fr))', gap: '1.25rem' }}>
          {filteredSubmissions.map((sub) => (
            <div
              key={sub.id}
              className="card"
              style={{
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
                padding: '1.5rem',
                border: '1px solid var(--border-color)',
                gap: '1.25rem',
              }}
            >
              {/* Card Header */}
              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '0.5rem' }}>
                  <div>
                    <h3 style={{ fontSize: '1.125rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                      {sub.studentName}
                    </h3>
                    <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', marginTop: '2px' }}>
                      @{sub.studentUsername} • {sub.studentEmail}
                    </div>
                  </div>
                  <span className="badge badge-success" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.25rem', fontSize: '0.75rem' }}>
                    <CheckCircle2 size={12} /> {sub.answers?.length || 0} Answers
                  </span>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginTop: '0.75rem', fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>
                  <Calendar size={14} />
                  <span>Submitted: {formatDateTime(sub.submittedAt)}</span>
                </div>
              </div>

              {/* Sample Answers Preview (First 3) */}
              <div
                style={{
                  backgroundColor: 'var(--bg-subtle)',
                  borderRadius: 'var(--radius-md)',
                  padding: '0.75rem 1rem',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '0.375rem',
                  fontSize: '0.8125rem',
                }}
              >
                {sub.answers?.slice(0, 3).map((ans, idx) => (
                  <div key={idx} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span style={{ color: 'var(--text-muted)', maxWidth: '200px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {ans.question}
                    </span>
                    <strong style={{ color: 'var(--primary)' }}>{ans.selectedOption}</strong>
                  </div>
                ))}
                {sub.answers?.length > 3 && (
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textAlign: 'center', marginTop: '0.25rem' }}>
                    + {sub.answers.length - 3} more questions answered
                  </div>
                )}
              </div>

              {/* Card Action Buttons */}
              <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end', borderTop: '1px solid var(--border-color)', paddingTop: '1rem' }}>
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={() => setSelectedSubmission(sub)}
                  style={{ display: 'inline-flex', alignItems: 'center', gap: '0.375rem' }}
                >
                  <Eye size={15} /> View Full Responses
                </Button>

                {!isWarden && (
                  <Button
                    variant="danger"
                    size="sm"
                    onClick={() => setSubmissionToDelete(sub)}
                    style={{ display: 'inline-flex', alignItems: 'center', gap: '0.375rem' }}
                  >
                    <Trash2 size={15} /> Delete
                  </Button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Full Questionnaire Details Modal */}
      {selectedSubmission && (
        <Modal
          isOpen={Boolean(selectedSubmission)}
          onClose={() => setSelectedSubmission(null)}
          title={`Feedback Responses: ${selectedSubmission.studentName}`}
          maxWidth="760px"
          footer={
            <div style={{ display: 'flex', justifyContent: 'space-between', width: '100%', alignItems: 'center' }}>
              {!isWarden && (
                <Button
                  variant="danger"
                  onClick={() => setSubmissionToDelete(selectedSubmission)}
                  style={{ display: 'inline-flex', alignItems: 'center', gap: '0.375rem' }}
                >
                  <Trash2 size={16} /> Delete Submission
                </Button>
              )}
              <div style={{ marginLeft: 'auto' }}>
                <Button variant="secondary" onClick={() => setSelectedSubmission(null)}>
                  Close
                </Button>
              </div>
            </div>
          }
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem', maxHeight: '65vh', overflowY: 'auto', paddingRight: '0.5rem' }}>
            {/* Student Metadata Header in Modal */}
            <div
              style={{
                padding: '1rem',
                borderRadius: 'var(--radius-md)',
                backgroundColor: 'var(--bg-subtle)',
                border: '1px solid var(--border-color)',
                display: 'flex',
                justifyContent: 'space-between',
                flexWrap: 'wrap',
                gap: '0.75rem',
              }}
            >
              <div>
                <div style={{ fontWeight: 700, fontSize: '1rem', color: 'var(--text-primary)' }}>
                  {selectedSubmission.studentName} (@{selectedSubmission.studentUsername})
                </div>
                <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
                  {selectedSubmission.studentEmail}
                </div>
              </div>
              <div style={{ textAlign: 'right' }}>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                  Submission Date:
                </div>
                <div style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--text-secondary)' }}>
                  {formatDateTime(selectedSubmission.submittedAt)}
                </div>
              </div>
            </div>

            {/* Questions & Answers breakdown */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.875rem' }}>
              {selectedSubmission.answers?.map((ans, idx) => (
                <div
                  key={idx}
                  style={{
                    padding: '0.875rem 1rem',
                    borderRadius: 'var(--radius-md)',
                    border: '1px solid var(--border-color)',
                    backgroundColor: 'var(--bg-surface)',
                    display: 'flex',
                    flexDirection: 'column',
                    gap: '0.5rem',
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '0.5rem' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                      <span
                        style={{
                          width: '22px',
                          height: '22px',
                          borderRadius: 'var(--radius-full)',
                          backgroundColor: 'var(--primary-light)',
                          color: 'var(--primary)',
                          fontSize: '0.75rem',
                          fontWeight: 700,
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                        }}
                      >
                        {ans.displayOrder || idx + 1}
                      </span>
                      <span
                        style={{
                          fontSize: '0.6875rem',
                          fontWeight: 700,
                          textTransform: 'uppercase',
                          letterSpacing: '0.06em',
                          color: 'var(--text-muted)',
                        }}
                      >
                        {ans.category}
                      </span>
                    </div>

                    <span
                      className="badge badge-success"
                      style={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: '0.375rem',
                        fontWeight: 600,
                        fontSize: '0.8125rem',
                      }}
                    >
                      <CheckCircle2 size={12} /> {ans.selectedOption}
                    </span>
                  </div>

                  <div style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--text-primary)', paddingLeft: '0.25rem' }}>
                    {ans.question}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </Modal>
      )}

      {/* Admin Delete Confirmation Dialog */}
      {!isWarden && (
        <ConfirmDialog
          isOpen={Boolean(submissionToDelete)}
          onClose={() => setSubmissionToDelete(null)}
          onConfirm={handleDelete}
          title="Delete Student Feedback?"
          message={`Are you sure you want to permanently delete the feedback submission from "${submissionToDelete?.studentName}"? This action cannot be undone.`}
          confirmText="Yes, Delete Feedback"
          cancelText="Cancel"
          danger={true}
          loading={deleting}
        />
      )}
    </div>
  );
};

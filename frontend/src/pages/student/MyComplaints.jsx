import React, { useState, useEffect, useRef } from 'react';
import api from '../../services/api';
import { useToast } from '../../context/ToastContext';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { Modal } from '../../components/common/Modal';
import { LoadingSpinner } from '../../components/feedback/LoadingSpinner';
import { EmptyState } from '../../components/feedback/EmptyState';
import { ComplaintImage } from '../../components/complaint/ComplaintImage';
import {
  MessageSquareWarning,
  Plus,
  Image as ImageIcon,
  Trash2,
  UploadCloud,
  Info,
  AlertCircle,
  Eye
} from 'lucide-react';

export const MyComplaints = () => {
  const { success: toastSuccess, error: toastError } = useToast();
  const [complaints, setComplaints] = useState([]);
  const [loading, setLoading] = useState(true);

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [formData, setFormData] = useState({
    feedback: '',
    description: '',
    category: 'ELECTRICITY',
    roomNumber: '',
    priority: 'MEDIUM',
    visibility: '',
  });
  const [imageFile, setImageFile] = useState(null);
  const [imagePreviewUrl, setImagePreviewUrl] = useState(null);
  const [fileError, setFileError] = useState('');
  const [feedbackError, setFeedbackError] = useState('');
  const [visibilityError, setVisibilityError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const fileInputRef = useRef(null);

  const MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5 MB
  const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'];

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

  // Cleanup object URL on unmount or when image changes
  useEffect(() => {
    return () => {
      if (imagePreviewUrl) {
        URL.revokeObjectURL(imagePreviewUrl);
      }
    };
  }, [imagePreviewUrl]);

  const resetForm = () => {
    setFormData({
      feedback: '',
      description: '',
      category: 'ELECTRICITY',
      roomNumber: '',
      priority: 'MEDIUM',
      visibility: '',
    });
    if (imagePreviewUrl) {
      URL.revokeObjectURL(imagePreviewUrl);
    }
    setImageFile(null);
    setImagePreviewUrl(null);
    setFileError('');
    setFeedbackError('');
    setVisibilityError('');
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const handleOpenModal = () => {
    resetForm();
    setIsModalOpen(true);
  };

  const handleCloseModal = () => {
    if (!submitting) {
      setIsModalOpen(false);
      resetForm();
    }
  };

  const handleImageChange = (e) => {
    setFileError('');
    const file = e.target.files?.[0];
    if (!file) return;

    if (!ALLOWED_IMAGE_TYPES.includes(file.type.toLowerCase())) {
      setFileError('Unsupported file format. Please upload JPG, JPEG, PNG, or WebP.');
      if (fileInputRef.current) fileInputRef.current.value = '';
      return;
    }

    if (file.size > MAX_IMAGE_SIZE) {
      setFileError('Image file size exceeds the 5MB limit. Please choose a smaller photo.');
      if (fileInputRef.current) fileInputRef.current.value = '';
      return;
    }

    if (imagePreviewUrl) {
      URL.revokeObjectURL(imagePreviewUrl);
    }

    const preview = URL.createObjectURL(file);
    setImageFile(file);
    setImagePreviewUrl(preview);
  };

  const handleRemoveImage = () => {
    if (imagePreviewUrl) {
      URL.revokeObjectURL(imagePreviewUrl);
    }
    setImageFile(null);
    setImagePreviewUrl(null);
    setFileError('');
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const formatFileSize = (bytes) => {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setFeedbackError('');
    setVisibilityError('');

    const trimmedFeedback = formData.feedback.trim();
    if (!trimmedFeedback) {
      setFeedbackError('Feedback / complaint message is mandatory.');
      return;
    }

    if (trimmedFeedback.length > 2000) {
      setFeedbackError('Feedback cannot exceed 2000 characters.');
      return;
    }

    if (!formData.visibility) {
      setVisibilityError('Please select who can view this complaint.');
      return;
    }

    if (formData.description && formData.description.length > 5000) {
      toastError('Description cannot exceed 5000 characters.');
      return;
    }

    setSubmitting(true);
    try {
      const data = new FormData();
      data.append('feedback', trimmedFeedback);
      data.append('visibility', formData.visibility);

      if (formData.description && formData.description.trim()) {
        data.append('description', formData.description.trim());
      }

      data.append('category', formData.category);
      data.append('priority', formData.priority);

      if (formData.roomNumber && formData.roomNumber.trim()) {
        data.append('roomNumber', formData.roomNumber.trim());
      }

      if (imageFile) {
        data.append('image', imageFile);
      }

      await api.post('/complaints', data, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });

      toastSuccess('Grievance ticket submitted successfully. Designated hostel authority notified.');
      setIsModalOpen(false);
      resetForm();
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
            Report room maintenance, electrical, plumbing, hygiene, or connectivity issues.
          </p>
        </div>
        <Button variant="primary" icon={Plus} onClick={handleOpenModal}>
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
          onAction={handleOpenModal}
        />
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          {complaints.map((c) => (
            <div key={c.id} className="card card-hover" style={{ display: 'flex', flexDirection: 'column', gap: '0.85rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '0.5rem' }}>
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap' }}>
                    <span style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--primary)' }}>#{c.id}</span>
                    <h3 style={{ fontSize: '1.125rem', margin: 0 }}>{c.title}</h3>
                    <span className="badge badge-neutral">{c.category}</span>
                    <span
                      style={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: '0.3rem',
                        fontSize: '0.75rem',
                        padding: '0.15rem 0.5rem',
                        borderRadius: '9999px',
                        fontWeight: 600,
                        backgroundColor: 'var(--bg-subtle)',
                        border: '1px solid var(--border-color)',
                        color: 'var(--text-secondary)',
                      }}
                    >
                      <Eye size={12} color="var(--primary)" />
                      {c.visibility === 'WARDEN_ONLY'
                        ? 'Visible to Warden only'
                        : c.visibility === 'ADMIN_ONLY'
                        ? 'Visible to Admin only'
                        : 'Visible to Admin and Warden'}
                    </span>
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

              {/* Mandatory Feedback / Main Grievance text */}
              <div style={{ fontSize: '0.9375rem', color: 'var(--text-primary)', whiteSpace: 'pre-wrap', lineHeight: '1.5' }}>
                {c.feedback || c.description}
              </div>

              {/* Additional Description if provided separately */}
              {c.description && c.feedback && c.description !== c.feedback && (
                <div style={{ padding: '0.75rem', backgroundColor: 'var(--bg-subtle)', borderRadius: 'var(--radius-md)', fontSize: '0.875rem' }}>
                  <div style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-muted)', marginBottom: '0.25rem' }}>
                    ADDITIONAL DETAILS:
                  </div>
                  <div style={{ color: 'var(--text-secondary)', whiteSpace: 'pre-wrap' }}>
                    {c.description}
                  </div>
                </div>
              )}

              {/* Attached Photo Thumbnail & Viewer */}
              {c.hasImage && (
                <div style={{ marginTop: '0.25rem' }}>
                  <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '0.35rem' }}>
                    ATTACHMENT:
                  </div>
                  <ComplaintImage complaintId={c.id} imageName={c.imageName} thumbnail={true} />
                </div>
              )}

              {/* Admin / Warden Response Comment */}
              {c.adminComment && (
                <div style={{ padding: '0.75rem', backgroundColor: 'var(--bg-subtle)', borderRadius: 'var(--radius-md)', fontSize: '0.875rem', borderLeft: '3px solid var(--primary)' }}>
                  <strong>Hostel Staff Response:</strong> {c.adminComment}
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {/* Lodge Complaint Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={handleCloseModal}
        title="Lodge Grievance / Maintenance Ticket"
        maxWidth="560px"
      >
        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          {/* Note Banner */}
          <div style={{ display: 'flex', gap: '0.5rem', padding: '0.65rem 0.85rem', backgroundColor: 'var(--bg-subtle)', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)', fontSize: '0.8125rem', color: 'var(--text-secondary)', alignItems: 'center' }}>
            <Info size={16} color="var(--primary)" style={{ flexShrink: 0 }} />
            <span>
              <strong>Note:</strong> Feedback message and viewer visibility are mandatory. Additional description and photo attachment are completely optional.
            </span>
          </div>

          {/* Mandatory Feedback Field */}
          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.25rem' }}>
              <label className="form-label" style={{ marginBottom: 0 }}>
                Feedback / Complaint Message <span style={{ color: '#ef4444' }}>*</span>
              </label>
              <span style={{ fontSize: '0.75rem', color: formData.feedback.length > 1900 ? '#ef4444' : 'var(--text-muted)' }}>
                {formData.feedback.length} / 2000
              </span>
            </div>
            <textarea
              className="form-textarea"
              rows={4}
              maxLength={2000}
              placeholder="State the core issue or maintenance request clearly..."
              value={formData.feedback}
              onChange={(e) => {
                setFormData({ ...formData, feedback: e.target.value });
                if (feedbackError) setFeedbackError('');
              }}
              style={feedbackError ? { borderColor: '#ef4444' } : {}}
            />
            {feedbackError && (
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.25rem', color: '#ef4444', fontSize: '0.75rem', marginTop: '0.25rem' }}>
                <AlertCircle size={13} />
                <span>{feedbackError}</span>
              </div>
            )}
          </div>

          {/* Optional Additional Description */}
          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.25rem' }}>
              <label className="form-label" style={{ marginBottom: 0 }}>
                Additional Description <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 400 }}>(Optional)</span>
              </label>
              <span style={{ fontSize: '0.75rem', color: formData.description.length > 4800 ? '#ef4444' : 'var(--text-muted)' }}>
                {formData.description.length} / 5000
              </span>
            </div>
            <textarea
              className="form-textarea"
              rows={2}
              maxLength={5000}
              placeholder="Additional background, timing, or troubleshooting attempts (optional)..."
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
            />
          </div>

          {/* Category & Priority */}
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

          {/* Room / Location */}
          <div>
            <label className="form-label">
              Room / Specific Location <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 400 }}>(Optional)</span>
            </label>
            <input
              className="form-input"
              maxLength={20}
              placeholder="e.g. Room A-102, 2nd Floor Washroom"
              value={formData.roomNumber}
              onChange={(e) => setFormData({ ...formData, roomNumber: e.target.value })}
            />
          </div>

          {/* Who can view this complaint? (Mandatory) */}
          <div>
            <label className="form-label" style={{ marginBottom: '0.4rem' }}>
              Who can view this complaint? <span style={{ color: '#ef4444' }}>*</span>
            </label>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
              {[
                {
                  value: 'WARDEN_ONLY',
                  label: 'Only Warden',
                  desc: 'Only authorized Warden users can view and manage this complaint.',
                },
                {
                  value: 'ADMIN_ONLY',
                  label: 'Only Admin',
                  desc: 'Only authorized Admin users can view and manage this complaint.',
                },
                {
                  value: 'WARDEN_AND_ADMIN',
                  label: 'Both Warden and Admin',
                  desc: 'Authorized Warden and Admin users can view and manage this complaint.',
                },
              ].map((opt) => {
                const isSelected = formData.visibility === opt.value;
                return (
                  <label
                    key={opt.value}
                    style={{
                      display: 'flex',
                      alignItems: 'flex-start',
                      gap: '0.75rem',
                      padding: '0.65rem 0.85rem',
                      borderRadius: 'var(--radius-md)',
                      border: `1.5px solid ${isSelected ? 'var(--primary)' : visibilityError ? '#ef4444' : 'var(--border-color)'}`,
                      backgroundColor: isSelected ? 'var(--bg-subtle)' : 'transparent',
                      cursor: 'pointer',
                      transition: 'all 0.15s ease',
                    }}
                  >
                    <input
                      type="radio"
                      name="complaintVisibility"
                      value={opt.value}
                      checked={isSelected}
                      onChange={(e) => {
                        setFormData({ ...formData, visibility: e.target.value });
                        if (visibilityError) setVisibilityError('');
                      }}
                      style={{ marginTop: '0.2rem', accentColor: 'var(--primary)', cursor: 'pointer' }}
                    />
                    <div style={{ display: 'flex', flexDirection: 'column' }}>
                      <span style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                        {opt.label}
                      </span>
                      <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '2px' }}>
                        {opt.desc}
                      </span>
                    </div>
                  </label>
                );
              })}
            </div>
            {visibilityError && (
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.25rem', color: '#ef4444', fontSize: '0.75rem', marginTop: '0.35rem' }}>
                <AlertCircle size={13} />
                <span>{visibilityError}</span>
              </div>
            )}
          </div>

          {/* Optional Image Attachment */}
          <div>
            <label className="form-label" style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span>
                Complaint Image Attachment <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 400 }}>(Optional, max 1)</span>
              </span>
              <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 400 }}>JPG, PNG, WebP (Max 5MB)</span>
            </label>

            {/* Hidden native input */}
            <input
              type="file"
              ref={fileInputRef}
              accept=".jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp"
              onChange={handleImageChange}
              style={{ display: 'none' }}
            />

            {!imageFile ? (
              <div
                onClick={() => fileInputRef.current?.click()}
                style={{
                  border: '2px dashed var(--border-color)',
                  borderRadius: 'var(--radius-md)',
                  padding: '1.25rem 1rem',
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '0.5rem',
                  cursor: 'pointer',
                  backgroundColor: 'var(--bg-subtle)',
                  transition: 'border-color 0.2s',
                }}
                className="upload-dropzone"
              >
                <UploadCloud size={28} color="var(--primary)" />
                <div style={{ textAlign: 'center' }}>
                  <span style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                    Click to browse or upload a photo
                  </span>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '2px' }}>
                    E.g. broken furniture, water leakage, damaged switchboard
                  </div>
                </div>
              </div>
            ) : (
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  padding: '0.6rem 0.85rem',
                  backgroundColor: 'var(--bg-subtle)',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid var(--border-color)',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', minWidth: 0 }}>
                  {imagePreviewUrl ? (
                    <img
                      src={imagePreviewUrl}
                      alt="Selected attachment preview"
                      style={{ width: '44px', height: '44px', objectFit: 'cover', borderRadius: '4px', border: '1px solid var(--border-color)' }}
                    />
                  ) : (
                    <ImageIcon size={24} color="var(--primary)" />
                  )}
                  <div style={{ display: 'flex', flexDirection: 'column', minWidth: 0 }}>
                    <span style={{ fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-primary)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', maxWidth: '240px' }}>
                      {imageFile.name}
                    </span>
                    <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                      {formatFileSize(imageFile.size)}
                    </span>
                  </div>
                </div>

                <div style={{ display: 'flex', gap: '0.5rem' }}>
                  <button
                    type="button"
                    className="btn btn-ghost btn-sm"
                    onClick={() => fileInputRef.current?.click()}
                    style={{ fontSize: '0.75rem' }}
                  >
                    Replace
                  </button>
                  <button
                    type="button"
                    className="btn btn-ghost btn-sm"
                    onClick={handleRemoveImage}
                    style={{ color: '#ef4444', padding: '4px' }}
                    title="Remove image"
                  >
                    <Trash2 size={16} />
                  </button>
                </div>
              </div>
            )}

            {fileError && (
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.25rem', color: '#ef4444', fontSize: '0.75rem', marginTop: '0.35rem' }}>
                <AlertCircle size={13} />
                <span>{fileError}</span>
              </div>
            )}
          </div>

          {/* Form Actions */}
          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
            <Button type="button" variant="secondary" onClick={handleCloseModal} disabled={submitting}>
              Cancel
            </Button>
            <Button type="submit" variant="primary" loading={submitting}>
              Submit Grievance
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};

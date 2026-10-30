import React, { useState, useEffect, useRef } from 'react';
import api from '../../services/api';
import { useToast } from '../../context/ToastContext';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { ConfirmDialog } from '../../components/common/ConfirmDialog';
import { LoadingSpinner } from '../../components/feedback/LoadingSpinner';
import { EmptyState } from '../../components/feedback/EmptyState';
import { FeedbackImage } from '../../components/feedback/FeedbackImage';
import {
  MessageSquare,
  CheckCircle2,
  Trash2,
  Calendar,
  AlertCircle,
  ShieldCheck,
  Send,
  UploadCloud,
  Image as ImageIcon,
  Info,
  FileText,
} from 'lucide-react';

export const StudentFeedback = () => {
  const { success: toastSuccess, error: toastError } = useToast();

  const [loading, setLoading] = useState(true);
  const [questions, setQuestions] = useState([]);
  const [myFeedback, setMyFeedback] = useState(null);
  const [selectedAnswers, setSelectedAnswers] = useState({});
  const [description, setDescription] = useState('');
  const [imageFile, setImageFile] = useState(null);
  const [imagePreviewUrl, setImagePreviewUrl] = useState(null);
  const [fileError, setFileError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [validationError, setValidationError] = useState('');

  const fileInputRef = useRef(null);

  const MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5 MB
  const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'];

  const fetchData = async () => {
    setLoading(true);
    setValidationError('');
    try {
      // 1. Fetch active questions
      const qRes = await api.get('/feedback/questions');
      setQuestions(qRes.data || []);

      // 2. Fetch authenticated student's existing feedback
      const myRes = await api.get('/feedback/my');
      setMyFeedback(myRes.data || null);
    } catch (err) {
      toastError(err.message || 'Failed to load feedback questionnaire');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  // Cleanup object URL on unmount or when image changes
  useEffect(() => {
    return () => {
      if (imagePreviewUrl) {
        URL.revokeObjectURL(imagePreviewUrl);
      }
    };
  }, [imagePreviewUrl]);

  const handleSelectOption = (questionId, optionId) => {
    setSelectedAnswers((prev) => ({
      ...prev,
      [questionId]: optionId,
    }));
    if (validationError) {
      setValidationError('');
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
    setValidationError('');

    // Client-side validation: all active questions must be answered
    const unanswered = questions.filter((q) => !selectedAnswers[q.id]);
    if (unanswered.length > 0) {
      const msg = `Please answer all ${questions.length} questions before submitting. (${unanswered.length} remaining)`;
      setValidationError(msg);
      toastError(msg);

      // Scroll to the first unanswered question
      const firstUnansweredEl = document.getElementById(`question-card-${unanswered[0].id}`);
      if (firstUnansweredEl) {
        firstUnansweredEl.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }
      return;
    }

    if (description && description.length > 5000) {
      setValidationError('Additional description cannot exceed 5000 characters.');
      toastError('Additional description cannot exceed 5000 characters.');
      return;
    }

    setSubmitting(true);
    try {
      const answersPayload = questions.map((q) => ({
        questionId: q.id,
        optionId: selectedAnswers[q.id],
      }));

      const formData = new FormData();
      formData.append('answers', JSON.stringify(answersPayload));

      if (description && description.trim()) {
        formData.append('description', description.trim());
      }

      if (imageFile) {
        formData.append('image', imageFile);
      }

      const res = await api.post('/feedback', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });

      toastSuccess('Thank you! Your feedback has been submitted successfully.');
      setMyFeedback(res.data);
      setSelectedAnswers({});
      setDescription('');
      if (imagePreviewUrl) {
        URL.revokeObjectURL(imagePreviewUrl);
      }
      setImageFile(null);
      setImagePreviewUrl(null);
      setFileError('');
      if (fileInputRef.current) fileInputRef.current.value = '';
    } catch (err) {
      toastError(err.message || 'Failed to submit feedback');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async () => {
    if (!myFeedback) return;
    setDeleting(true);
    try {
      await api.delete(`/feedback/${myFeedback.id}`);
      toastSuccess('Feedback deleted successfully. You can now submit a new questionnaire.');
      setMyFeedback(null);
      setSelectedAnswers({});
      setDescription('');
      if (imagePreviewUrl) {
        URL.revokeObjectURL(imagePreviewUrl);
      }
      setImageFile(null);
      setImagePreviewUrl(null);
      setDeleteConfirmOpen(false);
    } catch (err) {
      toastError(err.message || 'Failed to delete feedback');
    } finally {
      setDeleting(false);
    }
  };

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

  if (loading) {
    return (
      <div style={{ padding: '3rem 1rem', textAlign: 'center' }}>
        <LoadingSpinner text="Loading feedback questionnaire..." />
      </div>
    );
  }

  // -------------------------------------------------------------
  // VIEW 1: IMMUTABLE SUBMITTED FEEDBACK VIEW
  // -------------------------------------------------------------
  if (myFeedback) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: '1.75rem', maxWidth: '840px', margin: '0 auto' }}>
        {/* Status Banner */}
        <div
          className="card"
          style={{
            background: 'linear-gradient(135deg, var(--bg-card) 0%, var(--bg-subtle) 100%)',
            border: '1px solid var(--border-color)',
            padding: '1.75rem 2rem',
            display: 'flex',
            flexDirection: 'column',
            gap: '1rem',
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '1rem' }}>
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.375rem' }}>
                <span className="badge badge-success" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.375rem' }}>
                  <CheckCircle2 size={13} /> Feedback Recorded
                </span>
                <span className="badge badge-neutral" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.375rem' }}>
                  <ShieldCheck size={13} /> Immutable
                </span>
              </div>
              <h2 style={{ fontSize: '1.625rem', fontWeight: 800, color: 'var(--text-primary)' }}>
                My Submitted Feedback
              </h2>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginTop: '0.375rem', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
                <Calendar size={15} />
                <span>Submitted on: <strong>{formatDateTime(myFeedback.submittedAt)}</strong></span>
              </div>
            </div>

            <Button
              variant="danger"
              onClick={() => setDeleteConfirmOpen(true)}
              style={{ display: 'inline-flex', alignItems: 'center', gap: '0.5rem' }}
            >
              <Trash2 size={16} /> Delete Feedback
            </Button>
          </div>

          <div
            style={{
              padding: '0.875rem 1rem',
              borderRadius: 'var(--radius-md)',
              backgroundColor: 'var(--bg-subtle)',
              border: '1px solid var(--border-color)',
              fontSize: '0.875rem',
              color: 'var(--text-secondary)',
              lineHeight: 1.5,
            }}
          >
            <strong>Note:</strong> Submitted feedback is permanent and cannot be modified directly. If your experience or rating has changed, you may delete your submission and complete a fresh questionnaire.
          </div>
        </div>

        {/* Optional Additional Description Card (if submitted) */}
        {myFeedback.description && (
          <div
            className="card"
            style={{
              padding: '1.25rem 1.5rem',
              border: '1px solid var(--border-color)',
              display: 'flex',
              flexDirection: 'column',
              gap: '0.5rem',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.8125rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
              <FileText size={15} color="var(--primary)" />
              <span>Additional Comments / Description</span>
            </div>
            <div style={{ color: 'var(--text-primary)', whiteSpace: 'pre-wrap', fontSize: '0.9375rem', lineHeight: 1.5 }}>
              {myFeedback.description}
            </div>
          </div>
        )}

        {/* Optional Attached Photo (if submitted) */}
        {myFeedback.hasImage && (
          <div
            className="card"
            style={{
              padding: '1.25rem 1.5rem',
              border: '1px solid var(--border-color)',
              display: 'flex',
              flexDirection: 'column',
              gap: '0.5rem',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.8125rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
              <ImageIcon size={15} color="var(--primary)" />
              <span>Attached Photo</span>
            </div>
            <FeedbackImage feedbackId={myFeedback.id} imageName={myFeedback.imageName} thumbnail={true} />
          </div>
        )}

        {/* Answers List */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0 0.25rem' }}>
            <h3 style={{ fontSize: '1.125rem', fontWeight: 700, color: 'var(--text-primary)' }}>
              Submitted Responses ({myFeedback.answers?.length || 0} Questions)
            </h3>
          </div>

          {myFeedback.answers?.map((ans, idx) => (
            <div
              key={ans.questionId || idx}
              className="card"
              style={{
                padding: '1.25rem 1.5rem',
                border: '1px solid var(--border-color)',
                display: 'flex',
                flexDirection: 'column',
                gap: '0.75rem',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '0.75rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <span
                    style={{
                      width: '24px',
                      height: '24px',
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
                    padding: '0.375rem 0.75rem',
                    fontSize: '0.8125rem',
                    fontWeight: 600,
                  }}
                >
                  <CheckCircle2 size={13} /> {ans.selectedOption}
                </span>
              </div>

              <div style={{ fontSize: '0.9375rem', fontWeight: 600, color: 'var(--text-primary)', paddingLeft: '0.25rem' }}>
                {ans.question}
              </div>
            </div>
          ))}
        </div>

        {/* Delete Confirmation Dialog */}
        <ConfirmDialog
          isOpen={deleteConfirmOpen}
          onClose={() => setDeleteConfirmOpen(false)}
          onConfirm={handleDelete}
          title="Delete Your Feedback?"
          message="Are you sure you want to delete your feedback? This will permanently remove your submitted answers. You will be able to fill out and submit a fresh questionnaire afterward."
          confirmText="Yes, Delete Feedback"
          cancelText="Keep Feedback"
          danger={true}
          loading={deleting}
        />
      </div>
    );
  }

  // -------------------------------------------------------------
  // VIEW 2: EMPTY QUESTIONS STATE
  // -------------------------------------------------------------
  if (questions.length === 0) {
    return (
      <div style={{ maxWidth: '720px', margin: '2rem auto' }}>
        <EmptyState
          icon={MessageSquare}
          title="Questionnaire Unavailable"
          message="The hostel feedback questionnaire is currently not active or being updated by administrators. Please check back later."
        />
      </div>
    );
  }

  // -------------------------------------------------------------
  // VIEW 3: INTERACTIVE QUESTIONNAIRE FORM (BEFORE SUBMISSION)
  // -------------------------------------------------------------
  const answeredCount = Object.keys(selectedAnswers).length;
  const progressPercent = Math.round((answeredCount / questions.length) * 100);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.75rem', maxWidth: '840px', margin: '0 auto' }}>
      {/* Header Banner */}
      <div
        className="card"
        style={{
          background: 'linear-gradient(135deg, var(--bg-card) 0%, var(--bg-subtle) 100%)',
          border: '1px solid var(--border-color)',
          padding: '1.75rem 2rem',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.375rem' }}>
          <span className="badge badge-accent" style={{ backgroundColor: 'var(--primary)', color: '#ffffff' }}>
            Resident Survey
          </span>
          <span style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
            One submission per student
          </span>
        </div>
        <h2 style={{ fontSize: '1.75rem', fontWeight: 800, color: 'var(--text-primary)' }}>
          Hostel Feedback Questionnaire
        </h2>
        <p style={{ color: 'var(--text-secondary)', marginTop: '0.375rem', fontSize: '0.9375rem' }}>
          Your feedback directly influences hostel maintenance, mess cleanliness, internet reliability, and security services.
          Please answer all 15 questions below, and optionally attach an explanation or photograph.
        </p>

        {/* Progress bar */}
        <div style={{ marginTop: '1.25rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8125rem', marginBottom: '0.375rem' }}>
            <span style={{ color: 'var(--text-secondary)', fontWeight: 500 }}>
              Progress: <strong>{answeredCount} of {questions.length} answered</strong>
            </span>
            <span style={{ color: 'var(--primary)', fontWeight: 700 }}>
              {progressPercent}%
            </span>
          </div>
          <div
            style={{
              width: '100%',
              height: '8px',
              backgroundColor: 'var(--bg-subtle)',
              borderRadius: '999px',
              overflow: 'hidden',
              border: '1px solid var(--border-color)',
            }}
          >
            <div
              style={{
                width: `${progressPercent}%`,
                height: '100%',
                backgroundColor: progressPercent === 100 ? 'var(--success)' : 'var(--primary)',
                transition: 'width 0.3s ease, background-color 0.3s ease',
              }}
            />
          </div>
        </div>
      </div>

      {/* Validation alert banner */}
      {validationError && (
        <div
          style={{
            padding: '1rem 1.25rem',
            borderRadius: 'var(--radius-md)',
            backgroundColor: 'var(--danger-light)',
            border: '1px solid var(--danger)',
            color: 'var(--danger)',
            display: 'flex',
            alignItems: 'center',
            gap: '0.75rem',
            fontSize: '0.875rem',
            fontWeight: 600,
          }}
        >
          <AlertCircle size={20} flexShrink={0} />
          <span>{validationError}</span>
        </div>
      )}

      {/* Questions Section */}
      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
        {questions.map((q) => {
          const isAnswered = Boolean(selectedAnswers[q.id]);
          return (
            <div
              key={q.id}
              id={`question-card-${q.id}`}
              className="card"
              style={{
                padding: '1.5rem',
                border: '1px solid var(--border-color)',
                display: 'flex',
                flexDirection: 'column',
                gap: '1rem',
                transition: 'border-color 0.2s ease',
              }}
            >
              {/* Question Header */}
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '0.5rem', flexWrap: 'wrap' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem' }}>
                  <span
                    style={{
                      width: '28px',
                      height: '28px',
                      borderRadius: 'var(--radius-full)',
                      backgroundColor: isAnswered ? 'var(--primary)' : 'var(--bg-subtle)',
                      color: isAnswered ? '#ffffff' : 'var(--text-secondary)',
                      fontSize: '0.8125rem',
                      fontWeight: 700,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      transition: 'all 0.2s ease',
                    }}
                  >
                    {q.displayOrder}
                  </span>
                  <span
                    style={{
                      fontSize: '0.75rem',
                      fontWeight: 700,
                      textTransform: 'uppercase',
                      letterSpacing: '0.06em',
                      color: 'var(--text-muted)',
                    }}
                  >
                    {q.category}
                  </span>
                </div>

                {isAnswered ? (
                  <span style={{ fontSize: '0.75rem', color: 'var(--success)', fontWeight: 600, display: 'inline-flex', alignItems: 'center', gap: '0.25rem' }}>
                    <CheckCircle2 size={13} /> Answered
                  </span>
                ) : (
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 500 }}>
                    Required *
                  </span>
                )}
              </div>

              {/* Question Text */}
              <div style={{ fontSize: '1.0625rem', fontWeight: 600, color: 'var(--text-primary)', lineHeight: 1.4 }}>
                {q.questionText}
              </div>

              {/* Multiple-Choice Options */}
              <div
                role="radiogroup"
                aria-label={q.questionText}
                style={{
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '0.5rem',
                  marginTop: '0.25rem',
                }}
              >
                {q.options?.map((opt) => {
                  const isSelected = selectedAnswers[q.id] === opt.id;
                  const radioId = `q-${q.id}-opt-${opt.id}`;
                  return (
                    <label
                      key={opt.id}
                      htmlFor={radioId}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: '0.875rem',
                        padding: '0.75rem 1rem',
                        borderRadius: 'var(--radius-md)',
                        border: isSelected ? '1.5px solid var(--primary)' : '1px solid var(--border-color)',
                        backgroundColor: isSelected ? 'var(--primary-light)' : 'var(--bg-surface)',
                        cursor: 'pointer',
                        transition: 'all 0.15s ease',
                        userSelect: 'none',
                      }}
                    >
                      <input
                        type="radio"
                        id={radioId}
                        name={`question-${q.id}`}
                        value={opt.id}
                        checked={isSelected}
                        onChange={() => handleSelectOption(q.id, opt.id)}
                        style={{
                          accentColor: 'var(--primary)',
                          width: '18px',
                          height: '18px',
                          cursor: 'pointer',
                        }}
                      />
                      <span
                        style={{
                          fontSize: '0.9375rem',
                          fontWeight: isSelected ? 600 : 500,
                          color: isSelected ? 'var(--primary)' : 'var(--text-primary)',
                        }}
                      >
                        {opt.displayText}
                      </span>
                    </label>
                  );
                })}
              </div>
            </div>
          );
        })}

        {/* ----------------------------------------------------------------- */}
        {/* OPTIONAL FIELDS: DESCRIPTION & PHOTO ATTACHMENT                  */}
        {/* ----------------------------------------------------------------- */}
        <div
          className="card"
          style={{
            padding: '1.5rem 2rem',
            border: '1px solid var(--border-color)',
            display: 'flex',
            flexDirection: 'column',
            gap: '1.25rem',
            backgroundColor: 'var(--bg-card)',
          }}
        >
          {/* Informational Guidance Note */}
          <div
            style={{
              display: 'flex',
              gap: '0.5rem',
              padding: '0.75rem 1rem',
              backgroundColor: 'var(--bg-subtle)',
              borderRadius: 'var(--radius-md)',
              border: '1px solid var(--border-color)',
              fontSize: '0.8125rem',
              color: 'var(--text-secondary)',
              alignItems: 'center',
            }}
          >
            <Info size={16} color="var(--primary)" style={{ flexShrink: 0 }} />
            <span>
              <strong>Note:</strong> All 15 questions above are required. The additional description and photograph attachment below are <strong>completely optional</strong>.
            </span>
          </div>

          {/* Additional Description (Optional) */}
          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.375rem' }}>
              <label className="form-label" style={{ marginBottom: 0, fontWeight: 700 }}>
                Additional Description <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 400 }}>(Optional)</span>
              </label>
              <span style={{ fontSize: '0.75rem', color: description.length > 4800 ? '#ef4444' : 'var(--text-muted)' }}>
                {description.length} / 5000
              </span>
            </div>
            <textarea
              className="form-textarea"
              rows={3}
              maxLength={5000}
              placeholder="Provide any further context, suggestions, or specific observations to explain your feedback..."
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>

          {/* Attach a Photo (Optional) */}
          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.375rem' }}>
              <label className="form-label" style={{ marginBottom: 0, fontWeight: 700 }}>
                Attach a Photo <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 400 }}>(Optional, max 1)</span>
              </label>
              <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 400 }}>
                JPG, JPEG, PNG, WebP (Max 5MB)
              </span>
            </div>

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
                    Click to browse or upload an illustrative photo
                  </span>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '2px' }}>
                    E.g. washroom hygiene, mess food quality, damaged furniture, or water facility
                  </div>
                </div>
              </div>
            ) : (
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  padding: '0.65rem 0.85rem',
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
                    title="Remove photo"
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
        </div>

        {/* Submit Actions */}
        <div
          className="card"
          style={{
            padding: '1.5rem 2rem',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            flexWrap: 'wrap',
            gap: '1rem',
            backgroundColor: 'var(--bg-card)',
            border: '1px solid var(--border-color)',
            marginTop: '0.5rem',
          }}
        >
          <div>
            <div style={{ fontWeight: 700, fontSize: '0.9375rem', color: 'var(--text-primary)' }}>
              Ready to submit?
            </div>
            <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', marginTop: '2px' }}>
              {answeredCount === questions.length
                ? 'All 15 questions answered. Once submitted, your feedback is recorded and becomes immutable.'
                : `${questions.length - answeredCount} questions remaining to be answered.`}
            </div>
          </div>

          <Button
            type="submit"
            variant="primary"
            loading={submitting}
            disabled={submitting}
            style={{
              padding: '0.75rem 2rem',
              fontWeight: 700,
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.5rem',
            }}
          >
            <Send size={16} /> Submit Feedback
          </Button>
        </div>
      </form>
    </div>
  );
};

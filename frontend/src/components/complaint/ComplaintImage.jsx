import React, { useState, useEffect } from 'react';
import api from '../../services/api';
import { LoadingSpinner } from '../feedback/LoadingSpinner';
import { Image as ImageIcon, ImageOff, ZoomIn, X } from 'lucide-react';

export const ComplaintImage = ({ complaintId, imageName, thumbnail = true }) => {
  const [imgUrl, setImgUrl] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [isModalOpen, setIsModalOpen] = useState(false);

  useEffect(() => {
    let isMounted = true;
    let url = null;

    const loadImage = async () => {
      try {
        setLoading(true);
        setError(false);
        const response = await api.get(`/complaints/${complaintId}/image`, {
          responseType: 'blob',
        });
        if (isMounted) {
          url = URL.createObjectURL(response.data);
          setImgUrl(url);
        }
      } catch (err) {
        if (isMounted) {
          setError(true);
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    };

    if (complaintId) {
      loadImage();
    }

    return () => {
      isMounted = false;
      if (url) {
        URL.revokeObjectURL(url);
      }
    };
  }, [complaintId]);

  if (loading) {
    return (
      <div style={{ display: 'inline-flex', alignItems: 'center', gap: '0.5rem', color: 'var(--text-muted)', fontSize: '0.8rem', padding: '0.4rem 0.6rem', backgroundColor: 'var(--bg-subtle)', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)' }}>
        <LoadingSpinner size="sm" />
        <span>Loading attachment...</span>
      </div>
    );
  }

  if (error || !imgUrl) {
    return (
      <div style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem', color: 'var(--text-muted)', fontSize: '0.8rem', padding: '0.3rem 0.5rem', backgroundColor: 'var(--bg-subtle)', borderRadius: 'var(--radius-md)' }}>
        <ImageOff size={15} />
        <span>Photo unavailable</span>
      </div>
    );
  }

  return (
    <>
      {thumbnail ? (
        <button
          type="button"
          onClick={() => setIsModalOpen(true)}
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.6rem',
            padding: '0.35rem 0.65rem',
            backgroundColor: 'var(--bg-subtle)',
            border: '1px solid var(--border-color)',
            borderRadius: 'var(--radius-md)',
            cursor: 'pointer',
            textAlign: 'left',
            transition: 'border-color 0.2s, background-color 0.2s',
          }}
          className="btn-ghost"
          title="Click to view full photo"
        >
          <img
            src={imgUrl}
            alt={imageName || 'Complaint photo'}
            style={{ width: '36px', height: '36px', objectFit: 'cover', borderRadius: '4px', flexShrink: 0 }}
          />
          <div style={{ display: 'flex', flexDirection: 'column', minWidth: 0 }}>
            <span style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-primary)', maxWidth: '160px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
              {imageName || 'Attached Photo'}
            </span>
            <span style={{ fontSize: '0.7rem', color: 'var(--primary)', display: 'inline-flex', alignItems: 'center', gap: '3px' }}>
              <ZoomIn size={12} /> View full photo
            </span>
          </div>
        </button>
      ) : (
        <div style={{ position: 'relative' }}>
          <img
            src={imgUrl}
            alt={imageName || 'Complaint photo'}
            style={{ maxWidth: '100%', maxHeight: '420px', borderRadius: 'var(--radius-md)', objectFit: 'contain', border: '1px solid var(--border-color)', display: 'block' }}
          />
        </div>
      )}

      {/* Enlarged Photo Preview Modal */}
      {isModalOpen && (
        <div
          className="modal-overlay"
          onClick={() => setIsModalOpen(false)}
          style={{ zIndex: 1100 }}
        >
          <div
            className="modal-content"
            style={{ maxWidth: '720px', width: '90%', padding: '1rem', backgroundColor: 'var(--bg-card)' }}
            onClick={(e) => e.stopPropagation()}
            role="dialog"
            aria-modal="true"
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontWeight: 700, fontSize: '0.95rem' }}>
                <ImageIcon size={18} color="var(--primary)" />
                <span style={{ maxWidth: '400px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  {imageName || 'Complaint Photo Attachment'}
                </span>
              </div>
              <button
                type="button"
                onClick={() => setIsModalOpen(false)}
                className="btn btn-ghost btn-sm"
                style={{ padding: '4px', borderRadius: 'var(--radius-full)' }}
                aria-label="Close"
              >
                <X size={20} />
              </button>
            </div>

            <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', backgroundColor: 'var(--bg-subtle)', borderRadius: 'var(--radius-md)', overflow: 'hidden', padding: '0.5rem' }}>
              <img
                src={imgUrl}
                alt={imageName || 'Full photo preview'}
                style={{ maxWidth: '100%', maxHeight: '65vh', objectFit: 'contain', borderRadius: '4px' }}
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '0.75rem' }}>
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                onClick={() => setIsModalOpen(false)}
              >
                Close Preview
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
};

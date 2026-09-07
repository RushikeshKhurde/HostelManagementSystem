import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import api from '../services/api';
import { Button } from '../components/common/Button';
import { Input } from '../components/common/Input';
import { Select } from '../components/common/Select';
import { Badge } from '../components/common/Badge';
import { LoadingSpinner } from '../components/feedback/LoadingSpinner';
import {
  User,
  Mail,
  Phone,
  Calendar,
  MapPin,
  Shield,
  Edit3,
  CheckCircle2,
  Lock,
  KeyRound,
  X,
  Save,
  AlertCircle
} from 'lucide-react';

export const Profile = () => {
  const { user, updateUser } = useAuth();
  const { success: toastSuccess, error: toastError } = useToast();

  const [isEditing, setIsEditing] = useState(false);
  const [initialLoading, setInitialLoading] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  // Form State
  const [formData, setFormData] = useState({
    fullName: user?.fullName || '',
    email: user?.email || '',
    mobileNumber: user?.mobileNumber || '',
    gender: user?.gender || 'Male',
    dateOfBirth: user?.dateOfBirth || '',
    address: user?.address || '',
  });

  // Password Update State
  const [changePassword, setChangePassword] = useState(false);
  const [passwordData, setPasswordData] = useState({
    currentPassword: '',
    newPassword: '',
    confirmNewPassword: '',
  });

  useEffect(() => {
    const fetchFreshProfile = async () => {
      try {
        setInitialLoading(true);
        const res = await api.get('/profile/me');
        if (res.data) {
          updateUser(res.data);
          setFormData({
            fullName: res.data.fullName || '',
            email: res.data.email || '',
            mobileNumber: res.data.mobileNumber || '',
            gender: res.data.gender || 'Male',
            dateOfBirth: res.data.dateOfBirth || '',
            address: res.data.address || '',
          });
        }
      } catch (err) {
        console.error('Error fetching fresh profile:', err);
      } finally {
        setInitialLoading(false);
      }
    };

    fetchFreshProfile();
  }, []);

  const handleStartEdit = () => {
    setFormData({
      fullName: user?.fullName || '',
      email: user?.email || '',
      mobileNumber: user?.mobileNumber || '',
      gender: user?.gender || 'Male',
      dateOfBirth: user?.dateOfBirth || '',
      address: user?.address || '',
    });
    setChangePassword(false);
    setPasswordData({
      currentPassword: '',
      newPassword: '',
      confirmNewPassword: '',
    });
    setErrorMessage('');
    setIsEditing(true);
  };

  const handleCancelEdit = () => {
    setIsEditing(false);
    setErrorMessage('');
  };

  const handleInputChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  const handlePasswordChange = (field, value) => {
    setPasswordData((prev) => ({ ...prev, [field]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');

    // Client-side validations
    if (!formData.fullName.trim()) {
      setErrorMessage('Full name is required.');
      return;
    }
    if (!formData.email.trim()) {
      setErrorMessage('Email address is required.');
      return;
    }
    if (!formData.mobileNumber.trim() || !/^[0-9]{10}$/.test(formData.mobileNumber.trim())) {
      setErrorMessage('Please enter a valid 10-digit mobile number.');
      return;
    }

    if (changePassword) {
      if (!passwordData.currentPassword) {
        setErrorMessage('Current password is required to change your password.');
        return;
      }
      if (passwordData.newPassword.length < 6) {
        setErrorMessage('New password must be at least 6 characters long.');
        return;
      }
      if (passwordData.newPassword !== passwordData.confirmNewPassword) {
        setErrorMessage('New passwords do not match.');
        return;
      }
    }

    setLoading(true);

    try {
      const payload = {
        fullName: formData.fullName.trim(),
        email: formData.email.trim().toLowerCase(),
        mobileNumber: formData.mobileNumber.trim(),
        gender: formData.gender,
        dateOfBirth: formData.dateOfBirth || null,
        address: formData.address.trim() || null,
      };

      if (changePassword && passwordData.newPassword) {
        payload.currentPassword = passwordData.currentPassword;
        payload.newPassword = passwordData.newPassword;
      }

      const res = await api.put('/profile', payload);
      updateUser(res.data);
      toastSuccess('Profile updated successfully!');
      setIsEditing(false);
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to update profile. Please check your inputs.';
      setErrorMessage(msg);
      toastError(msg);
    } finally {
      setLoading(false);
    }
  };

  if (initialLoading && !user) {
    return <LoadingSpinner text="Loading profile details..." />;
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem', maxWidth: '850px', margin: '0 auto', paddingBottom: '3rem' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h2 style={{ fontSize: '1.75rem', fontWeight: 800 }}>Account Profile</h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>
            Manage and update your personal details and security preferences.
          </p>
        </div>
        {!isEditing && (
          <Button variant="primary" icon={Edit3} onClick={handleStartEdit}>
            Edit Profile
          </Button>
        )}
      </div>

      {errorMessage && (
        <div
          style={{
            padding: '1rem',
            backgroundColor: 'rgba(239, 68, 68, 0.1)',
            border: '1px solid var(--danger)',
            borderRadius: 'var(--radius-md)',
            color: 'var(--danger)',
            display: 'flex',
            alignItems: 'center',
            gap: '0.75rem',
            fontSize: '0.875rem',
          }}
        >
          <AlertCircle size={18} style={{ flexShrink: 0 }} />
          <span>{errorMessage}</span>
        </div>
      )}

      {/* Main Profile Container */}
      <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: '1.75rem' }}>
        {/* User Identity Banner */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: '1.25rem',
            paddingBottom: '1.5rem',
            borderBottom: '1px solid var(--border-color)',
            flexWrap: 'wrap',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '1.25rem' }}>
            <div
              style={{
                width: '68px',
                height: '68px',
                borderRadius: 'var(--radius-full)',
                backgroundColor: user?.role === 'ADMIN' ? 'var(--primary)' : 'var(--accent)',
                color: '#ffffff',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontWeight: 800,
                fontSize: '1.75rem',
                boxShadow: 'var(--shadow-md)',
              }}
            >
              {user?.fullName?.[0]?.toUpperCase() || 'U'}
            </div>
            <div>
              <h3 style={{ fontSize: '1.375rem', fontWeight: 700 }}>{user?.fullName}</h3>
              <div style={{ color: 'var(--text-muted)', fontSize: '0.875rem', marginTop: '0.125rem' }}>
                @{user?.username} · {user?.email}
              </div>
              <div style={{ display: 'flex', gap: '0.5rem', marginTop: '0.5rem', alignItems: 'center' }}>
                <Badge status={user?.role}>{user?.role === 'ADMIN' ? 'Administrator' : 'Student (USER)'}</Badge>
                <span style={{ display: 'flex', alignItems: 'center', gap: '0.25rem', fontSize: '0.75rem', color: 'var(--success)', fontWeight: 600 }}>
                  <CheckCircle2 size={12} /> {user?.status || 'ACTIVE'}
                </span>
              </div>
            </div>
          </div>
        </div>

        {!isEditing ? (
          /* ================= VIEW MODE ================= */
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '1.25rem' }}>
              <div className="card" style={{ backgroundColor: 'var(--bg-subtle)' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: 'var(--text-muted)', fontSize: '0.8125rem', fontWeight: 600, textTransform: 'uppercase', marginBottom: '0.375rem' }}>
                  <Mail size={14} /> Email Address
                </div>
                <div style={{ fontSize: '1rem', fontWeight: 700, wordBreak: 'break-all' }}>{user?.email}</div>
              </div>

              <div className="card" style={{ backgroundColor: 'var(--bg-subtle)' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: 'var(--text-muted)', fontSize: '0.8125rem', fontWeight: 600, textTransform: 'uppercase', marginBottom: '0.375rem' }}>
                  <Phone size={14} /> Mobile Phone
                </div>
                <div style={{ fontSize: '1rem', fontWeight: 700 }}>{user?.mobileNumber || 'Not provided'}</div>
              </div>

              <div className="card" style={{ backgroundColor: 'var(--bg-subtle)' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: 'var(--text-muted)', fontSize: '0.8125rem', fontWeight: 600, textTransform: 'uppercase', marginBottom: '0.375rem' }}>
                  <User size={14} /> Gender
                </div>
                <div style={{ fontSize: '1rem', fontWeight: 700 }}>{user?.gender || 'Not specified'}</div>
              </div>

              <div className="card" style={{ backgroundColor: 'var(--bg-subtle)' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: 'var(--text-muted)', fontSize: '0.8125rem', fontWeight: 600, textTransform: 'uppercase', marginBottom: '0.375rem' }}>
                  <Calendar size={14} /> Date of Birth
                </div>
                <div style={{ fontSize: '1rem', fontWeight: 700 }}>{user?.dateOfBirth || 'Not provided'}</div>
              </div>

              <div className="card" style={{ backgroundColor: 'var(--bg-subtle)' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: 'var(--text-muted)', fontSize: '0.8125rem', fontWeight: 600, textTransform: 'uppercase', marginBottom: '0.375rem' }}>
                  <Shield size={14} /> Role Permissions
                </div>
                <div style={{ fontSize: '1rem', fontWeight: 700 }}>
                  {user?.role === 'ADMIN' ? 'Full Administrator Access' : 'Standard Student Portal'}
                </div>
              </div>

              <div className="card" style={{ backgroundColor: 'var(--bg-subtle)' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: 'var(--text-muted)', fontSize: '0.8125rem', fontWeight: 600, textTransform: 'uppercase', marginBottom: '0.375rem' }}>
                  <Calendar size={14} /> Member Since
                </div>
                <div style={{ fontSize: '1rem', fontWeight: 700 }}>
                  {user?.createdAt ? new Date(user.createdAt).toLocaleDateString() : 'N/A'}
                </div>
              </div>
            </div>

            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: 'var(--text-muted)', fontSize: '0.8125rem', fontWeight: 600, textTransform: 'uppercase', marginBottom: '0.375rem' }}>
                <MapPin size={14} /> Permanent Address
              </div>
              <div style={{ padding: '1rem', backgroundColor: 'var(--bg-subtle)', borderRadius: 'var(--radius-md)', fontSize: '0.9375rem', lineHeight: 1.6 }}>
                {user?.address || 'No permanent address recorded on file.'}
              </div>
            </div>
          </div>
        ) : (
          /* ================= EDIT MODE ================= */
          <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.75rem' }}>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '1.25rem' }}>
              <Input
                label="Full Name *"
                value={formData.fullName}
                onChange={(e) => handleInputChange('fullName', e.target.value)}
                placeholder="Enter your full name"
                icon={User}
                required
              />

              <Input
                label="Username (Read-Only)"
                value={user?.username || ''}
                disabled
                helper="Username cannot be altered for security auditing."
                icon={Shield}
              />

              <Input
                label="Email Address *"
                type="email"
                value={formData.email}
                onChange={(e) => handleInputChange('email', e.target.value)}
                placeholder="Enter email address"
                icon={Mail}
                required
              />

              <Input
                label="Mobile Phone Number *"
                value={formData.mobileNumber}
                onChange={(e) => handleInputChange('mobileNumber', e.target.value)}
                placeholder="10-digit phone number"
                icon={Phone}
                required
              />

              <Select
                label="Gender"
                value={formData.gender}
                onChange={(e) => handleInputChange('gender', e.target.value)}
                options={[
                  { value: 'Male', label: 'Male' },
                  { value: 'Female', label: 'Female' },
                  { value: 'Other', label: 'Other' },
                ]}
              />

              <Input
                label="Date of Birth"
                type="date"
                value={formData.dateOfBirth}
                onChange={(e) => handleInputChange('dateOfBirth', e.target.value)}
                icon={Calendar}
              />
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 600, color: 'var(--text-main)', marginBottom: '0.375rem' }}>
                Permanent Address
              </label>
              <textarea
                className="form-input"
                style={{ width: '100%', minHeight: '80px', resize: 'vertical' }}
                value={formData.address}
                onChange={(e) => handleInputChange('address', e.target.value)}
                placeholder="Street address, city, state, postal code"
              />
            </div>

            {/* Password Security Section */}
            <div
              style={{
                padding: '1.25rem',
                backgroundColor: 'var(--bg-subtle)',
                borderRadius: 'var(--radius-lg)',
                border: '1px solid var(--border-color)',
                display: 'flex',
                flexDirection: 'column',
                gap: '1rem',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontWeight: 700, fontSize: '0.9375rem' }}>
                  <KeyRound size={18} style={{ color: 'var(--primary)' }} />
                  <span>Security & Password</span>
                </div>
                <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer', fontSize: '0.875rem', fontWeight: 600 }}>
                  <input
                    type="checkbox"
                    checked={changePassword}
                    onChange={(e) => setChangePassword(e.target.checked)}
                    style={{ accentColor: 'var(--primary)', cursor: 'pointer', width: '16px', height: '16px' }}
                  />
                  <span>Change Password</span>
                </label>
              </div>

              {changePassword && (
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '1rem', marginTop: '0.5rem' }}>
                  <Input
                    label="Current Password *"
                    type="password"
                    value={passwordData.currentPassword}
                    onChange={(e) => handlePasswordChange('currentPassword', e.target.value)}
                    placeholder="Enter current password"
                    icon={Lock}
                    required
                  />

                  <Input
                    label="New Password *"
                    type="password"
                    value={passwordData.newPassword}
                    onChange={(e) => handlePasswordChange('newPassword', e.target.value)}
                    placeholder="At least 6 characters"
                    icon={Lock}
                    required
                  />

                  <Input
                    label="Confirm New Password *"
                    type="password"
                    value={passwordData.confirmNewPassword}
                    onChange={(e) => handlePasswordChange('confirmNewPassword', e.target.value)}
                    placeholder="Re-type new password"
                    icon={Lock}
                    required
                  />
                </div>
              )}
            </div>

            {/* Action Buttons */}
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '1rem', marginTop: '0.5rem' }}>
              <Button type="button" variant="outline" icon={X} onClick={handleCancelEdit} disabled={loading}>
                Cancel
              </Button>
              <Button type="submit" variant="primary" icon={Save} loading={loading}>
                Save Changes
              </Button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
};
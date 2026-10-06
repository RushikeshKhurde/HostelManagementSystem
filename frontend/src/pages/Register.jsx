import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { Input } from '../components/common/Input';
import { Select } from '../components/common/Select';
import { Button } from '../components/common/Button';
import { Building2, User, Mail, Phone, Lock, Calendar, MapPin, ArrowRight } from 'lucide-react';

export const Register = () => {
  const { register, user } = useAuth();
  const { error: toastError, success: toastSuccess } = useToast();
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    fullName: '',
    username: '',
    email: '',
    mobileNumber: '',
    password: '',
    confirmPassword: '',
    gender: 'Male',
    dateOfBirth: '',
    address: '',
  });

  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);

  React.useEffect(() => {
    if (user) {
      navigate(user.role === 'ADMIN' ? '/admin/dashboard' : '/student/dashboard', { replace: true });
    }
  }, [user, navigate]);

  const handleChange = (e) => {
    const { id, value } = e.target;
    setFormData((prev) => ({ ...prev, [id]: value }));
    if (errors[id]) {
      setErrors((prev) => ({ ...prev, [id]: null }));
    }
  };

  const getPasswordStrength = (pw) => {
    if (!pw) return { score: 0, text: '', color: '' };
    let score = 0;
    if (pw.length >= 8) score++;
    if (/[A-Z]/.test(pw)) score++;
    if (/[a-z]/.test(pw)) score++;
    if (/\d/.test(pw)) score++;
    if (/[@#$%^&+=!]/.test(pw)) score++;

    if (score <= 2) return { score: 1, text: 'Weak', color: 'var(--danger)' };
    if (score <= 4) return { score: 2, text: 'Medium', color: 'var(--warning)' };
    return { score: 3, text: 'Strong', color: 'var(--success)' };
  };

  const strength = getPasswordStrength(formData.password);

  const validate = () => {
    const newErrors = {};
    if (!formData.fullName.trim() || formData.fullName.trim().length < 3) {
      newErrors.fullName = 'Full name must be at least 3 characters.';
    }

    const usernameRegex = /^[a-zA-Z0-9_.]+$/;
    if (!formData.username.trim() || formData.username.trim().length < 3) {
      newErrors.username = 'Username must be at least 3 characters.';
    } else if (!usernameRegex.test(formData.username.trim())) {
      newErrors.username = 'Only letters, numbers, dots, and underscores allowed.';
    }

    const email = formData.email;
    const emailRegex = /^[a-z0-9._%+-]+@([a-z0-9-]+\.)+[a-z]{2,}$/;
    if (!email || !email.trim()) {
      newErrors.email = 'Please enter a valid email address (e.g., username@domain.com).';
    } else if (/[A-Z]/.test(email)) {
      newErrors.email = 'Email must be in lowercase.';
    } else if (!emailRegex.test(email)) {
      newErrors.email = 'Please enter a valid email address (e.g., username@domain.com).';
    }

    const mobileRegex = /^[6-9]\d{9}$/;
    if (!formData.mobileNumber.trim() || !mobileRegex.test(formData.mobileNumber.trim())) {
      newErrors.mobileNumber = 'Enter a valid 10-digit mobile number starting with 6-9.';
    }

    const pwRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@#$%^&+=!]).{8,20}$/;
    if (!formData.password) {
      newErrors.password = 'Password is required.';
    } else if (!pwRegex.test(formData.password)) {
      newErrors.password = '8-20 chars with uppercase, lowercase, digit, and special char (@#$%^&+=!).';
    }

    if (formData.password !== formData.confirmPassword) {
      newErrors.confirmPassword = 'Passwords do not match.';
    }

    if (formData.dateOfBirth) {
      const dob = new Date(formData.dateOfBirth);
      const today = new Date();
      if (dob >= today) {
        newErrors.dateOfBirth = 'Date of birth must be a past date.';
      }
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validate()) return;

    setLoading(true);
    try {
      const payload = {
        fullName: formData.fullName.trim(),
        username: formData.username.trim(),
        email: formData.email,
        mobileNumber: formData.mobileNumber.trim(),
        password: formData.password,
        confirmPassword: formData.confirmPassword,
        gender: formData.gender,
        dateOfBirth: formData.dateOfBirth || null,
        address: formData.address.trim() || null,
      };

      const data = await register(payload);
      toastSuccess('Registration successful! Welcome to SmartHostel.');
      navigate('/student/dashboard', { replace: true });
    } catch (err) {
      const msg = err.message || 'Registration failed.';
      if (msg.toLowerCase().includes('email') && msg.toLowerCase().includes('already')) {
        setErrors((prev) => ({ ...prev, email: 'Email address is already registered.' }));
      }
      toastError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: 'var(--bg-app)',
        padding: '2rem 1.5rem',
      }}
    >
      <div
        style={{
          width: '100%',
          maxWidth: '720px',
          backgroundColor: 'var(--bg-surface)',
          border: '1px solid var(--border-color)',
          borderRadius: 'var(--radius-xl)',
          boxShadow: 'var(--shadow-xl)',
          padding: '2.5rem',
        }}
      >
        <div style={{ textAlign: 'center', marginBottom: '2rem' }}>
          <div
            style={{
              width: '48px',
              height: '48px',
              borderRadius: 'var(--radius-lg)',
              background: 'linear-gradient(135deg, var(--primary) 0%, var(--accent) 100%)',
              color: '#ffffff',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              margin: '0 auto 1rem',
              boxShadow: '0 4px 12px rgba(37, 99, 235, 0.3)',
            }}
          >
            <Building2 size={26} />
          </div>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 800, letterSpacing: '-0.02em' }}>
            Create Student Account
          </h2>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
            Register to explore hostel rooms, apply for bookings, and manage payments
          </p>
        </div>

        <form onSubmit={handleSubmit} noValidate>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '1rem' }}>
            <Input
              label="Full Name"
              id="fullName"
              icon={User}
              value={formData.fullName}
              onChange={handleChange}
              placeholder="e.g. Rahul Sharma"
              error={errors.fullName}
              required
            />

            <Input
              label="Username"
              id="username"
              icon={User}
              value={formData.username}
              onChange={handleChange}
              placeholder="e.g. rahul_sharma"
              error={errors.username}
              required
            />

            <Input
              label="Email Address"
              id="email"
              type="email"
              pattern="^[a-z0-9._%+-]+@([a-z0-9-]+\.)+[a-z]{2,}$"
              icon={Mail}
              value={formData.email}
              onChange={handleChange}
              placeholder="rahul@example.com"
              error={errors.email}
              required
            />

            <Input
              label="Mobile Number"
              id="mobileNumber"
              type="tel"
              icon={Phone}
              value={formData.mobileNumber}
              onChange={handleChange}
              placeholder="10-digit number (e.g. 9876543210)"
              error={errors.mobileNumber}
              maxLength={10}
              required
            />

            <div>
              <Input
                label="Password"
                id="password"
                type="password"
                icon={Lock}
                value={formData.password}
                onChange={handleChange}
                placeholder="Create a strong password"
                error={errors.password}
                required
              />
              {formData.password && (
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginTop: '-0.5rem', marginBottom: '0.75rem' }}>
                  <div style={{ flex: 1, height: '4px', backgroundColor: 'var(--bg-subtle)', borderRadius: '2px', overflow: 'hidden' }}>
                    <div
                      style={{
                        width: `${(strength.score / 3) * 100}%`,
                        height: '100%',
                        backgroundColor: strength.color,
                        transition: 'width var(--transition-fast)',
                      }}
                    />
                  </div>
                  <span style={{ fontSize: '0.75rem', fontWeight: 600, color: strength.color }}>
                    {strength.text}
                  </span>
                </div>
              )}
            </div>

            <Input
              label="Confirm Password"
              id="confirmPassword"
              type="password"
              icon={Lock}
              value={formData.confirmPassword}
              onChange={handleChange}
              placeholder="Re-enter your password"
              error={errors.confirmPassword}
              required
            />

            <Select
              label="Gender"
              id="gender"
              value={formData.gender}
              onChange={handleChange}
              options={[
                { value: 'Male', label: 'Male' },
                { value: 'Female', label: 'Female' },
                { value: 'Other', label: 'Other' },
              ]}
            />

            <Input
              label="Date of Birth"
              id="dateOfBirth"
              type="date"
              icon={Calendar}
              value={formData.dateOfBirth}
              onChange={handleChange}
              error={errors.dateOfBirth}
            />
          </div>

          <div style={{ marginTop: '0.25rem' }}>
            <Input
              label="Permanent Address"
              id="address"
              icon={MapPin}
              value={formData.address}
              onChange={handleChange}
              placeholder="Flat/House No., Street, City, State, PIN"
              error={errors.address}
            />
          </div>

          <Button
            type="submit"
            variant="primary"
            loading={loading}
            style={{ width: '100%', marginTop: '1rem', padding: '0.75rem' }}
          >
            Create Account <ArrowRight size={16} />
          </Button>
        </form>

        <div
          style={{
            marginTop: '2rem',
            textAlign: 'center',
            fontSize: '0.875rem',
            color: 'var(--text-secondary)',
            borderTop: '1px solid var(--border-color)',
            paddingTop: '1.25rem',
          }}
        >
          Already have an account?{' '}
          <Link to="/login" style={{ fontWeight: 600, color: 'var(--primary)' }}>
            Sign In
          </Link>
        </div>
      </div>
    </div>
  );
};
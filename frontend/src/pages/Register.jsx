import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import api from '../services/api';
import { Input } from '../components/common/Input';
import { Select } from '../components/common/Select';
import { Button } from '../components/common/Button';
import {
  Building2,
  User,
  Mail,
  Phone,
  Lock,
  Calendar,
  MapPin,
  ArrowRight,
  CheckCircle2,
  ShieldCheck,
  RefreshCw,
  Edit2,
  KeyRound,
  Clock,
} from 'lucide-react';

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

  // Email OTP verification states
  const [isEmailVerified, setIsEmailVerified] = useState(false);
  const [registrationToken, setRegistrationToken] = useState(null);
  const [otpSent, setOtpSent] = useState(false);
  const [otpCode, setOtpCode] = useState('');
  const [otpSending, setOtpSending] = useState(false);
  const [otpVerifying, setOtpVerifying] = useState(false);
  const [resendCooldown, setResendCooldown] = useState(0);
  const [otpExpirySeconds, setOtpExpirySeconds] = useState(0);
  const [maskedEmail, setMaskedEmail] = useState('');

  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);

  // Cooldown countdown timer
  useEffect(() => {
    let timer;
    if (resendCooldown > 0) {
      timer = setInterval(() => {
        setResendCooldown((prev) => (prev > 0 ? prev - 1 : 0));
      }, 1000);
    }
    return () => clearInterval(timer);
  }, [resendCooldown]);

  // OTP expiration countdown timer
  useEffect(() => {
    let timer;
    if (otpExpirySeconds > 0 && !isEmailVerified) {
      timer = setInterval(() => {
        setOtpExpirySeconds((prev) => {
          if (prev <= 1) {
            return 0;
          }
          return prev - 1;
        });
      }, 1000);
    }
    return () => clearInterval(timer);
  }, [otpExpirySeconds, isEmailVerified]);

  const formatTimer = (secs) => {
    const mins = Math.floor(secs / 60);
    const remainder = secs % 60;
    return `${String(mins).padStart(2, '0')}:${String(remainder).padStart(2, '0')}`;
  };

  const handleChange = (e) => {
    const { id, value } = e.target;
    setFormData((prev) => ({ ...prev, [id]: value }));

    if (id === 'email') {
      if (otpSent || isEmailVerified) {
        setIsEmailVerified(false);
        setRegistrationToken(null);
        setOtpSent(false);
        setOtpCode('');
        setResendCooldown(0);
        setOtpExpirySeconds(0);
      }
    }

    if (errors[id]) {
      setErrors((prev) => ({ ...prev, [id]: null }));
    }
  };

  const handleSendOtp = async (e) => {
    if (e) e.preventDefault();
    const email = formData.email ? formData.email.trim().toLowerCase() : '';
    const emailRegex = /^[a-z0-9._%+-]+@([a-z0-9-]+\.)+[a-z]{2,}$/;

    if (!email) {
      setErrors((prev) => ({ ...prev, email: 'Email is required before requesting verification code.' }));
      return;
    }
    if (/[A-Z]/.test(formData.email)) {
      setErrors((prev) => ({ ...prev, email: 'Email must be in lowercase.' }));
      return;
    }
    if (!emailRegex.test(email)) {
      setErrors((prev) => ({ ...prev, email: 'Please enter a valid email address (e.g., username@domain.com).' }));
      return;
    }

    setOtpSending(true);
    try {
      const res = await api.post('/auth/email-verification/send-otp', { email });
      setOtpSent(true);
      setMaskedEmail(res.data.destinationMasked || email);
      setResendCooldown(res.data.resendCooldownSeconds || 60);
      setOtpExpirySeconds(res.data.expiresInSeconds || 600);
      setOtpCode('');
      setErrors((prev) => ({ ...prev, email: null, otp: null }));
      toastSuccess(res.data.message || 'Verification code sent to your email!');
    } catch (err) {
      const msg = err.message || 'Failed to send verification code.';
      setErrors((prev) => ({ ...prev, email: msg }));
      toastError(msg);
    } finally {
      setOtpSending(false);
    }
  };

  const handleVerifyOtp = async (e) => {
    if (e) e.preventDefault();
    const trimmedOtp = otpCode ? otpCode.trim() : '';

    if (!trimmedOtp || trimmedOtp.length !== 6) {
      setErrors((prev) => ({ ...prev, otp: 'Please enter the complete 6-digit verification code.' }));
      return;
    }

    setOtpVerifying(true);
    try {
      const email = formData.email.trim().toLowerCase();
      const res = await api.post('/auth/email-verification/verify-otp', {
        email,
        otp: trimmedOtp,
      });
      setIsEmailVerified(true);
      setRegistrationToken(res.data.registrationToken);
      setErrors((prev) => ({ ...prev, otp: null, email: null }));
      toastSuccess(res.data.message || 'Email verified successfully!');
    } catch (err) {
      const msg = err.message || 'Invalid or expired verification code.';
      setErrors((prev) => ({ ...prev, otp: msg }));
      toastError(msg);
    } finally {
      setOtpVerifying(false);
    }
  };

  const handleResetVerification = () => {
    setIsEmailVerified(false);
    setRegistrationToken(null);
    setOtpSent(false);
    setOtpCode('');
    setResendCooldown(0);
    setOtpExpirySeconds(0);
    setErrors((prev) => ({ ...prev, email: null, otp: null }));
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
    } else if (!isEmailVerified || !registrationToken) {
      newErrors.email = 'Mandatory: Please verify your email ownership with OTP before registering.';
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

  useEffect(() => {
    if (user) {
      navigate(user.role === 'ADMIN' ? '/admin/dashboard' : '/student/dashboard', { replace: true });
    }
  }, [user, navigate]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validate()) {
      if (!isEmailVerified || !registrationToken) {
        toastError('Please verify your email address with OTP before creating your account.');
      }
      return;
    }

    setLoading(true);
    try {
      const payload = {
        fullName: formData.fullName.trim(),
        username: formData.username.trim(),
        email: formData.email.trim().toLowerCase(),
        mobileNumber: formData.mobileNumber.trim(),
        password: formData.password,
        confirmPassword: formData.confirmPassword,
        gender: formData.gender,
        dateOfBirth: formData.dateOfBirth || null,
        address: formData.address.trim() || null,
        registrationToken: registrationToken,
      };

      const data = await register(payload);
      toastSuccess('Registration successful! Welcome to SmartHostel.');
      navigate('/student/dashboard', { replace: true });
    } catch (err) {
      const msg = err.message || 'Registration failed.';
      if (msg.toLowerCase().includes('email') && msg.toLowerCase().includes('already')) {
        setErrors((prev) => ({ ...prev, email: 'Email address is already registered.' }));
      } else if (msg.toLowerCase().includes('verification token') || msg.toLowerCase().includes('expired')) {
        setErrors((prev) => ({ ...prev, email: msg }));
        handleResetVerification();
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
          maxWidth: '740px',
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

            {/* Email Address & Verification Section */}
            <div style={{ gridColumn: '1 / -1' }}>
              <div style={{ display: 'flex', alignItems: 'flex-end', gap: '0.75rem' }}>
                <div style={{ flex: 1 }}>
                  <Input
                    label="Email Address (Verification Required)"
                    id="email"
                    type="email"
                    pattern="^[a-z0-9._%+-]+@([a-z0-9-]+\.)+[a-z]{2,}$"
                    icon={Mail}
                    value={formData.email}
                    onChange={handleChange}
                    placeholder="rahul@example.com"
                    error={errors.email}
                    disabled={isEmailVerified}
                    required
                  />
                </div>

                {!isEmailVerified && !otpSent && (
                  <Button
                    type="button"
                    variant="secondary"
                    onClick={handleSendOtp}
                    loading={otpSending}
                    style={{ marginBottom: '1.25rem', whiteSpace: 'nowrap', height: '42px', padding: '0 1.25rem' }}
                  >
                    <ShieldCheck size={16} /> Send Verification OTP
                  </Button>
                )}

                {isEmailVerified && (
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1.25rem' }}>
                    <div
                      style={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: '0.35rem',
                        padding: '0.5rem 0.85rem',
                        backgroundColor: '#ecfdf5',
                        color: '#059669',
                        border: '1px solid #a7f3d0',
                        borderRadius: 'var(--radius-md)',
                        fontSize: '0.85rem',
                        fontWeight: 600,
                      }}
                    >
                      <CheckCircle2 size={16} /> Verified
                    </div>
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      onClick={handleResetVerification}
                      title="Change email"
                      style={{ padding: '0.5rem' }}
                    >
                      <Edit2 size={14} /> Change
                    </Button>
                  </div>
                )}
              </div>

              {/* OTP Verification Prompt Card */}
              {otpSent && !isEmailVerified && (
                <div
                  style={{
                    backgroundColor: 'var(--bg-subtle, #f8fafc)',
                    border: '1px solid var(--border-color)',
                    borderRadius: 'var(--radius-lg)',
                    padding: '1.25rem',
                    marginTop: '-0.5rem',
                    marginBottom: '1rem',
                  }}
                >
                  <div
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      marginBottom: '0.75rem',
                    }}
                  >
                    <span style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                      Enter 6-digit code sent to <strong style={{ color: 'var(--primary)' }}>{maskedEmail || formData.email}</strong>
                    </span>
                    {otpExpirySeconds > 0 ? (
                      <span
                        style={{
                          fontSize: '0.8rem',
                          color: otpExpirySeconds < 120 ? 'var(--danger)' : 'var(--text-muted)',
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: '0.25rem',
                          fontWeight: 500,
                        }}
                      >
                        <Clock size={13} /> Expires in {formatTimer(otpExpirySeconds)}
                      </span>
                    ) : (
                      <span style={{ fontSize: '0.8rem', color: 'var(--danger)', fontWeight: 600 }}>
                        Code Expired
                      </span>
                    )}
                  </div>

                  <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'flex-start' }}>
                    <div style={{ flex: 1 }}>
                      <Input
                        id="otpCode"
                        type="text"
                        maxLength={6}
                        icon={KeyRound}
                        value={otpCode}
                        onChange={(e) => {
                          const val = e.target.value.replace(/\D/g, '').slice(0, 6);
                          setOtpCode(val);
                          if (errors.otp) setErrors((prev) => ({ ...prev, otp: null }));
                        }}
                        placeholder="6-digit OTP (e.g. 123456)"
                        error={errors.otp}
                        autoFocus
                      />
                    </div>

                    <Button
                      type="button"
                      variant="primary"
                      onClick={handleVerifyOtp}
                      loading={otpVerifying}
                      disabled={otpCode.length !== 6 || otpExpirySeconds <= 0}
                      style={{ height: '42px', padding: '0 1.25rem', whiteSpace: 'nowrap' }}
                    >
                      Verify Code
                    </Button>

                    <Button
                      type="button"
                      variant="outline"
                      onClick={handleSendOtp}
                      loading={otpSending}
                      disabled={resendCooldown > 0}
                      style={{ height: '42px', padding: '0 1rem', whiteSpace: 'nowrap' }}
                      title={resendCooldown > 0 ? `Wait ${resendCooldown}s to resend` : 'Resend OTP'}
                    >
                      <RefreshCw size={14} /> {resendCooldown > 0 ? `${resendCooldown}s` : 'Resend'}
                    </Button>
                  </div>

                  <div style={{ marginTop: '0.5rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                      Did not receive the code? Check spam or click Resend.
                    </span>
                    <button
                      type="button"
                      onClick={handleResetVerification}
                      style={{
                        background: 'none',
                        border: 'none',
                        color: 'var(--text-muted)',
                        fontSize: '0.75rem',
                        cursor: 'pointer',
                        textDecoration: 'underline',
                      }}
                    >
                      Change Email Address
                    </button>
                  </div>
                </div>
              )}
            </div>

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
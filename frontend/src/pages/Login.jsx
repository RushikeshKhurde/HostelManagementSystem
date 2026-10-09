import React, { useState, useEffect, useRef } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import api from '../services/api';
import { Input } from '../components/common/Input';
import { Button } from '../components/common/Button';
import {
  Building2,
  Lock,
  User as UserIcon,
  ArrowRight,
  ArrowLeft,
  Mail,
  Phone,
  KeyRound,
  CheckCircle,
  RefreshCw,
  Clock,
  ShieldCheck,
  AlertCircle
} from 'lucide-react';

export const Login = () => {
  const { login, user } = useAuth();
  const { error: toastError, success: toastSuccess } = useToast();
  const navigate = useNavigate();

  // Login form state
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  // Forgot password flow state: 'LOGIN' | 'CHOOSE_METHOD' | 'ENTER_IDENTIFIER' | 'VERIFY_OTP' | 'NEW_PASSWORD' | 'SUCCESS'
  const [view, setView] = useState('LOGIN');
  const [recoveryMethod, setRecoveryMethod] = useState('EMAIL'); // 'EMAIL' | 'MOBILE'
  const [identifier, setIdentifier] = useState('');
  const [requestId, setRequestId] = useState('');
  const [destinationMasked, setDestinationMasked] = useState('');
  const [otp, setOtp] = useState('');
  const [resetToken, setResetToken] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  // Timers
  const [otpTimeLeft, setOtpTimeLeft] = useState(300); // 5 mins in seconds
  const [resendCooldown, setResendCooldown] = useState(60); // 60s cooldown

  useEffect(() => {
    if (user) {
      const target = user.role === 'ADMIN' ? '/admin/dashboard' : user.role === 'WARDEN' ? '/warden/dashboard' : '/student/dashboard';
      navigate(target, { replace: true });
    }
  }, [user, navigate]);

  // OTP expiration timer
  useEffect(() => {
    let timer;
    if (view === 'VERIFY_OTP' && otpTimeLeft > 0) {
      timer = setInterval(() => {
        setOtpTimeLeft((prev) => (prev > 0 ? prev - 1 : 0));
      }, 1000);
    }
    return () => clearInterval(timer);
  }, [view, otpTimeLeft]);

  // Resend cooldown timer
  useEffect(() => {
    let timer;
    if (view === 'VERIFY_OTP' && resendCooldown > 0) {
      timer = setInterval(() => {
        setResendCooldown((prev) => (prev > 0 ? prev - 1 : 0));
      }, 1000);
    }
    return () => clearInterval(timer);
  }, [view, resendCooldown]);

  const formatTimer = (seconds) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  // --- Handlers ---

  // Standard Login
  const handleLoginSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');

    if (!username.trim()) {
      setErrorMessage('Username or email is required.');
      return;
    }
    if (!password) {
      setErrorMessage('Password is required.');
      return;
    }

    setLoading(true);
    try {
      const data = await login(username.trim(), password);
      toastSuccess(`Welcome back, ${data.fullName}!`);
      if (data.role === 'ADMIN') {
        navigate('/admin/dashboard', { replace: true });
      } else if (data.role === 'WARDEN') {
        navigate('/warden/dashboard', { replace: true });
      } else {
        navigate('/student/dashboard', { replace: true });
      }
    } catch (err) {
      setPassword('');
      setErrorMessage(err.message || 'Invalid username or password.');
      toastError(err.message || 'Login failed.');
    } finally {
      setLoading(false);
    }
  };

  // Step 2: Request OTP
  const handleRequestOtp = async (e) => {
    e.preventDefault();
    setErrorMessage('');

    if (!identifier.trim()) {
      setErrorMessage(recoveryMethod === 'EMAIL' ? 'Please enter your registered email address.' : 'Please enter your registered mobile number.');
      return;
    }

    if (recoveryMethod === 'EMAIL' && !identifier.includes('@')) {
      setErrorMessage('Please enter a valid email address.');
      return;
    }

    if (recoveryMethod === 'MOBILE' && !/^[6-9]\d{9}$/.test(identifier.trim())) {
      setErrorMessage('Please enter a valid 10-digit mobile number starting with 6-9.');
      return;
    }

    setLoading(true);
    try {
      const res = await api.post('/auth/forgot-password/request', {
        method: recoveryMethod,
        identifier: identifier.trim(),
      });

      const data = res.data;
      setRequestId(data.requestId || '');
      setDestinationMasked(data.destinationMasked || identifier.trim());
      setOtpTimeLeft(data.expiresInSeconds || 300);
      setResendCooldown(data.resendCooldownSeconds || 60);
      setOtp('');

      toastSuccess(data.message || 'Verification code sent.');
      setView('VERIFY_OTP');
    } catch (err) {
      setErrorMessage(err.message || 'Failed to send verification code. Please try again.');
      toastError(err.message || 'Failed to send OTP.');
    } finally {
      setLoading(false);
    }
  };

  // Step 3: Resend OTP
  const handleResendOtp = async () => {
    if (resendCooldown > 0 || loading) return;
    setErrorMessage('');
    setLoading(true);
    try {
      const res = await api.post('/auth/forgot-password/resend', {
        requestId,
      });

      const data = res.data;
      setOtpTimeLeft(data.expiresInSeconds || 300);
      setResendCooldown(data.resendCooldownSeconds || 60);
      setOtp('');
      toastSuccess(data.message || 'A new OTP has been sent.');
    } catch (err) {
      setErrorMessage(err.message || 'Failed to resend OTP.');
      toastError(err.message || 'Resend failed.');
    } finally {
      setLoading(false);
    }
  };

  // Step 3: Verify OTP
  const handleVerifyOtp = async (e) => {
    e.preventDefault();
    setErrorMessage('');

    if (!otp || otp.trim().length !== 6) {
      setErrorMessage('Please enter the 6-digit OTP code.');
      return;
    }

    if (otpTimeLeft <= 0) {
      setErrorMessage('The OTP has expired. Please request a new OTP.');
      return;
    }

    setLoading(true);
    try {
      const res = await api.post('/auth/forgot-password/verify-otp', {
        requestId,
        otp: otp.trim(),
      });

      const data = res.data;
      if (data.verified && data.resetToken) {
        setResetToken(data.resetToken);
        toastSuccess('OTP verified successfully!');
        setView('NEW_PASSWORD');
      } else {
        setErrorMessage('Verification failed. Please try again.');
      }
    } catch (err) {
      setErrorMessage(err.message || 'Invalid OTP code.');
      toastError(err.message || 'Verification failed.');
    } finally {
      setLoading(false);
    }
  };

  // Step 4: Reset Password
  const handleResetPassword = async (e) => {
    e.preventDefault();
    setErrorMessage('');

    if (!newPassword) {
      setErrorMessage('New password is required.');
      return;
    }

    if (newPassword.length < 8 || newPassword.length > 20) {
      setErrorMessage('Password must be 8-20 characters long.');
      return;
    }

    const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@#$%^&+=!]).{8,20}$/;
    if (!passwordRegex.test(newPassword)) {
      setErrorMessage('Password must include uppercase, lowercase, digit, and special symbol (@#$%^&+=!).');
      return;
    }

    if (newPassword !== confirmPassword) {
      setErrorMessage('Passwords do not match.');
      return;
    }

    setLoading(true);
    try {
      const res = await api.post('/auth/forgot-password/reset', {
        resetToken,
        newPassword,
        confirmPassword,
      });

      // Clear recovery state
      setNewPassword('');
      setConfirmPassword('');
      setResetToken('');
      setOtp('');
      setRequestId('');
      toastSuccess(res.data.message || 'Password reset successful!');
      setView('SUCCESS');
    } catch (err) {
      setErrorMessage(err.message || 'Failed to reset password.');
      toastError(err.message || 'Password reset failed.');
    } finally {
      setLoading(false);
    }
  };

  const resetToLogin = () => {
    setView('LOGIN');
    setErrorMessage('');
    setIdentifier('');
    setOtp('');
    setResetToken('');
    setNewPassword('');
    setConfirmPassword('');
  };

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: 'var(--bg-app)',
        padding: '1.5rem',
      }}
    >
      <div
        style={{
          width: '100%',
          maxWidth: '460px',
          backgroundColor: 'var(--bg-surface)',
          border: '1px solid var(--border-color)',
          borderRadius: 'var(--radius-xl)',
          boxShadow: 'var(--shadow-xl)',
          padding: '2.5rem 2rem',
        }}
      >
        {/* Header Icon & Title */}
        <div style={{ textAlign: 'center', marginBottom: '1.75rem' }}>
          <div
            style={{
              width: '52px',
              height: '52px',
              borderRadius: 'var(--radius-lg)',
              background:
                view === 'SUCCESS'
                  ? 'linear-gradient(135deg, #10b981 0%, #059669 100%)'
                  : view !== 'LOGIN'
                  ? 'linear-gradient(135deg, var(--primary) 0%, #6366f1 100%)'
                  : 'linear-gradient(135deg, var(--primary) 0%, var(--accent) 100%)',
              color: '#ffffff',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              margin: '0 auto 1rem',
              boxShadow: '0 4px 12px rgba(37, 99, 235, 0.25)',
            }}
          >
            {view === 'SUCCESS' ? (
              <CheckCircle size={28} />
            ) : view === 'NEW_PASSWORD' ? (
              <KeyRound size={26} />
            ) : view === 'VERIFY_OTP' ? (
              <ShieldCheck size={26} />
            ) : view === 'CHOOSE_METHOD' || view === 'ENTER_IDENTIFIER' ? (
              <Lock size={26} />
            ) : (
              <Building2 size={26} />
            )}
          </div>

          <h2 style={{ fontSize: '1.5rem', fontWeight: 800, letterSpacing: '-0.02em', color: 'var(--text-primary)' }}>
            {view === 'LOGIN' && 'Welcome Back'}
            {view === 'CHOOSE_METHOD' && 'Forgot Password'}
            {view === 'ENTER_IDENTIFIER' && (recoveryMethod === 'EMAIL' ? 'Email Recovery' : 'Mobile Recovery')}
            {view === 'VERIFY_OTP' && 'Verify OTP Code'}
            {view === 'NEW_PASSWORD' && 'Create New Password'}
            {view === 'SUCCESS' && 'Password Reset Successful'}
          </h2>

          <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', marginTop: '0.35rem', lineHeight: '1.4' }}>
            {view === 'LOGIN' && 'Sign in to access your hostel account'}
            {view === 'CHOOSE_METHOD' && 'Select how you want to receive your security verification code'}
            {view === 'ENTER_IDENTIFIER' &&
              (recoveryMethod === 'EMAIL'
                ? 'Enter your registered email address to receive an OTP'
                : 'Enter your registered 10-digit mobile number to receive an OTP')}
            {view === 'VERIFY_OTP' && `A 6-digit OTP code was sent to ${destinationMasked || 'your contact channel'}`}
            {view === 'NEW_PASSWORD' && 'Enter and confirm your new secure account password'}
            {view === 'SUCCESS' && 'Your password has been changed. You can now log in with your new password.'}
          </p>
        </div>

        {/* Global Error Banner */}
        {errorMessage && (
          <div
            style={{
              padding: '0.75rem 1rem',
              borderRadius: 'var(--radius-md)',
              backgroundColor: 'rgba(239, 68, 68, 0.1)',
              border: '1px solid rgba(239, 68, 68, 0.3)',
              color: 'var(--danger)',
              fontSize: '0.875rem',
              fontWeight: 500,
              marginBottom: '1.25rem',
              display: 'flex',
              alignItems: 'center',
              gap: '0.5rem',
            }}
          >
            <AlertCircle size={18} style={{ flexShrink: 0 }} />
            <span>{errorMessage}</span>
          </div>
        )}

        {/* ================= VIEW 1: LOGIN ================= */}
        {view === 'LOGIN' && (
          <form onSubmit={handleLoginSubmit} noValidate>
            <Input
              label="Username, Email, or Mobile"
              id="username"
              name="username"
              type="text"
              icon={UserIcon}
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="Enter your username, email, or mobile"
              autoComplete="username"
              required
            />

            <div style={{ marginBottom: '1rem' }}>
              <Input
                label="Password"
                id="password"
                name="password"
                type="password"
                icon={Lock}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Enter your password"
                autoComplete="current-password"
                required
              />

              <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '-0.25rem' }}>
                <button
                  type="button"
                  onClick={() => {
                    setView('CHOOSE_METHOD');
                    setErrorMessage('');
                  }}
                  style={{
                    background: 'none',
                    border: 'none',
                    color: 'var(--primary)',
                    fontSize: '0.8125rem',
                    fontWeight: 600,
                    cursor: 'pointer',
                    padding: '0.25rem 0',
                    transition: 'color 0.15s ease',
                  }}
                >
                  Forgot Password?
                </button>
              </div>
            </div>

            <Button
              type="submit"
              variant="primary"
              loading={loading}
              style={{ width: '100%', marginTop: '0.5rem', padding: '0.75rem', fontSize: '0.9375rem' }}
            >
              Sign In <ArrowRight size={16} />
            </Button>

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
              Don't have an account?{' '}
              <Link to="/register" style={{ fontWeight: 600, color: 'var(--primary)' }}>
                Register as Student
              </Link>
            </div>
          </form>
        )}

        {/* ================= VIEW 2: CHOOSE VERIFICATION METHOD ================= */}
        {view === 'CHOOSE_METHOD' && (
          <div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.875rem', marginBottom: '1.5rem' }}>
              {/* Option 1: Email */}
              <div
                onClick={() => setRecoveryMethod('EMAIL')}
                style={{
                  padding: '1rem 1.125rem',
                  borderRadius: 'var(--radius-lg)',
                  border: `2px solid ${recoveryMethod === 'EMAIL' ? 'var(--primary)' : 'var(--border-color)'}`,
                  backgroundColor: recoveryMethod === 'EMAIL' ? 'rgba(37, 99, 235, 0.04)' : 'var(--bg-card)',
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '1rem',
                  transition: 'all 0.15s ease',
                }}
              >
                <div
                  style={{
                    width: '40px',
                    height: '40px',
                    borderRadius: 'var(--radius-md)',
                    backgroundColor: recoveryMethod === 'EMAIL' ? 'var(--primary)' : 'var(--border-color)',
                    color: '#ffffff',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    flexShrink: 0,
                  }}
                >
                  <Mail size={20} />
                </div>
                <div style={{ flex: 1 }}>
                  <div style={{ fontWeight: 700, fontSize: '0.9375rem', color: 'var(--text-primary)' }}>
                    Verify with Email
                  </div>
                  <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', marginTop: '0.125rem' }}>
                    Send a 6-digit OTP code to your registered email
                  </div>
                </div>
                <div
                  style={{
                    width: '18px',
                    height: '18px',
                    borderRadius: '50%',
                    border: `2px solid ${recoveryMethod === 'EMAIL' ? 'var(--primary)' : 'var(--text-muted)'}`,
                    backgroundColor: recoveryMethod === 'EMAIL' ? 'var(--primary)' : 'transparent',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                  }}
                >
                  {recoveryMethod === 'EMAIL' && (
                    <div style={{ width: '6px', height: '6px', borderRadius: '50%', backgroundColor: '#ffffff' }} />
                  )}
                </div>
              </div>

              {/* Option 2: Mobile Number */}
              <div
                onClick={() => setRecoveryMethod('MOBILE')}
                style={{
                  padding: '1rem 1.125rem',
                  borderRadius: 'var(--radius-lg)',
                  border: `2px solid ${recoveryMethod === 'MOBILE' ? 'var(--primary)' : 'var(--border-color)'}`,
                  backgroundColor: recoveryMethod === 'MOBILE' ? 'rgba(37, 99, 235, 0.04)' : 'var(--bg-card)',
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '1rem',
                  transition: 'all 0.15s ease',
                }}
              >
                <div
                  style={{
                    width: '40px',
                    height: '40px',
                    borderRadius: 'var(--radius-md)',
                    backgroundColor: recoveryMethod === 'MOBILE' ? 'var(--primary)' : 'var(--border-color)',
                    color: '#ffffff',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    flexShrink: 0,
                  }}
                >
                  <Phone size={20} />
                </div>
                <div style={{ flex: 1 }}>
                  <div style={{ fontWeight: 700, fontSize: '0.9375rem', color: 'var(--text-primary)' }}>
                    Verify with Mobile Number
                  </div>
                  <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', marginTop: '0.125rem' }}>
                    Send a 6-digit OTP code via SMS to your registered phone
                  </div>
                </div>
                <div
                  style={{
                    width: '18px',
                    height: '18px',
                    borderRadius: '50%',
                    border: `2px solid ${recoveryMethod === 'MOBILE' ? 'var(--primary)' : 'var(--text-muted)'}`,
                    backgroundColor: recoveryMethod === 'MOBILE' ? 'var(--primary)' : 'transparent',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                  }}
                >
                  {recoveryMethod === 'MOBILE' && (
                    <div style={{ width: '6px', height: '6px', borderRadius: '50%', backgroundColor: '#ffffff' }} />
                  )}
                </div>
              </div>
            </div>

            <Button
              type="button"
              variant="primary"
              onClick={() => {
                setErrorMessage('');
                setView('ENTER_IDENTIFIER');
              }}
              style={{ width: '100%', padding: '0.75rem', fontSize: '0.9375rem' }}
            >
              Continue <ArrowRight size={16} />
            </Button>

            <Button
              type="button"
              variant="outline"
              onClick={resetToLogin}
              style={{ width: '100%', marginTop: '0.75rem', padding: '0.75rem' }}
            >
              <ArrowLeft size={16} /> Back to Sign In
            </Button>
          </div>
        )}

        {/* ================= VIEW 3: ENTER REGISTERED IDENTIFIER ================= */}
        {view === 'ENTER_IDENTIFIER' && (
          <form onSubmit={handleRequestOtp} noValidate>
            {recoveryMethod === 'EMAIL' ? (
              <Input
                label="Registered Email Address"
                id="identifier-email"
                type="email"
                icon={Mail}
                value={identifier}
                onChange={(e) => setIdentifier(e.target.value)}
                placeholder="e.g. student@hostel.com"
                required
                autoFocus
              />
            ) : (
              <Input
                label="Registered 10-Digit Mobile Number"
                id="identifier-mobile"
                type="tel"
                icon={Phone}
                value={identifier}
                onChange={(e) => setIdentifier(e.target.value.replace(/\D/g, '').slice(0, 10))}
                placeholder="e.g. 9876543210"
                maxLength={10}
                required
                autoFocus
              />
            )}

            <Button
              type="submit"
              variant="primary"
              loading={loading}
              style={{ width: '100%', marginTop: '0.5rem', padding: '0.75rem', fontSize: '0.9375rem' }}
            >
              Send OTP Code <ArrowRight size={16} />
            </Button>

            <Button
              type="button"
              variant="outline"
              onClick={() => {
                setErrorMessage('');
                setView('CHOOSE_METHOD');
              }}
              style={{ width: '100%', marginTop: '0.75rem', padding: '0.75rem' }}
            >
              <ArrowLeft size={16} /> Back
            </Button>
          </form>
        )}

        {/* ================= VIEW 4: VERIFY OTP ================= */}
        {view === 'VERIFY_OTP' && (
          <form onSubmit={handleVerifyOtp} noValidate>
            <div style={{ marginBottom: '1.25rem' }}>
              <label
                htmlFor="otp-input"
                style={{ display: 'block', fontSize: '0.875rem', fontWeight: 600, color: 'var(--text-primary)', marginBottom: '0.5rem' }}
              >
                6-Digit Verification Code
              </label>
              <input
                id="otp-input"
                type="text"
                inputMode="numeric"
                autoComplete="one-time-code"
                maxLength={6}
                value={otp}
                onChange={(e) => setOtp(e.target.value.replace(/\D/g, '').slice(0, 6))}
                placeholder="• • • • • •"
                style={{
                  width: '100%',
                  padding: '0.75rem 1rem',
                  fontSize: '1.625rem',
                  fontWeight: 800,
                  letterSpacing: '0.65rem',
                  textAlign: 'center',
                  borderRadius: 'var(--radius-lg)',
                  border: '2px solid var(--border-color)',
                  backgroundColor: 'var(--bg-card)',
                  color: 'var(--text-primary)',
                  fontFamily: 'monospace',
                  outline: 'none',
                }}
                autoFocus
              />
            </div>

            {/* Timer & Expiration Display */}
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '0.625rem 0.875rem',
                borderRadius: 'var(--radius-md)',
                backgroundColor: otpTimeLeft > 0 ? 'rgba(37, 99, 235, 0.05)' : 'rgba(239, 68, 68, 0.08)',
                color: otpTimeLeft > 0 ? 'var(--primary)' : 'var(--danger)',
                fontSize: '0.8125rem',
                fontWeight: 600,
                marginBottom: '1.25rem',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                <Clock size={16} />
                <span>{otpTimeLeft > 0 ? `Expires in ${formatTimer(otpTimeLeft)}` : 'OTP expired'}</span>
              </div>

              {/* Resend button / countdown */}
              <button
                type="button"
                onClick={handleResendOtp}
                disabled={resendCooldown > 0 || loading}
                style={{
                  background: 'none',
                  border: 'none',
                  color: resendCooldown > 0 ? 'var(--text-muted)' : 'var(--primary)',
                  cursor: resendCooldown > 0 ? 'not-allowed' : 'pointer',
                  fontWeight: 700,
                  fontSize: '0.8125rem',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.25rem',
                  padding: 0,
                }}
              >
                <RefreshCw size={13} className={loading ? 'animate-spin' : ''} />
                {resendCooldown > 0 ? `Resend in ${resendCooldown}s` : 'Resend OTP'}
              </button>
            </div>

            <Button
              type="submit"
              variant="primary"
              loading={loading}
              disabled={otp.length !== 6 || otpTimeLeft <= 0}
              style={{ width: '100%', padding: '0.75rem', fontSize: '0.9375rem' }}
            >
              Verify OTP <ArrowRight size={16} />
            </Button>

            <Button
              type="button"
              variant="outline"
              onClick={() => {
                setErrorMessage('');
                setView('ENTER_IDENTIFIER');
              }}
              style={{ width: '100%', marginTop: '0.75rem', padding: '0.75rem' }}
            >
              <ArrowLeft size={16} /> Back
            </Button>
          </form>
        )}

        {/* ================= VIEW 5: CREATE NEW PASSWORD ================= */}
        {view === 'NEW_PASSWORD' && (
          <form onSubmit={handleResetPassword} noValidate>
            <Input
              label="New Password"
              id="new-password"
              type="password"
              icon={Lock}
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              placeholder="Enter your new password"
              required
              autoFocus
            />

            <Input
              label="Confirm New Password"
              id="confirm-new-password"
              type="password"
              icon={Lock}
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              placeholder="Confirm your new password"
              required
            />

            <div
              style={{
                fontSize: '0.75rem',
                color: 'var(--text-muted)',
                backgroundColor: 'rgba(0, 0, 0, 0.02)',
                border: '1px solid var(--border-color)',
                borderRadius: 'var(--radius-md)',
                padding: '0.625rem 0.75rem',
                marginBottom: '1.25rem',
                lineHeight: '1.4',
              }}
            >
              <strong>Password Requirements:</strong> 8-20 characters, at least one uppercase letter, one lowercase
              letter, one digit, and one special character (@#$%^&+=!).
            </div>

            <Button
              type="submit"
              variant="primary"
              loading={loading}
              style={{ width: '100%', padding: '0.75rem', fontSize: '0.9375rem' }}
            >
              Reset Password <ShieldCheck size={16} />
            </Button>
          </form>
        )}

        {/* ================= VIEW 6: SUCCESS ================= */}
        {view === 'SUCCESS' && (
          <div style={{ textAlign: 'center' }}>
            <div
              style={{
                padding: '1.25rem 1rem',
                borderRadius: 'var(--radius-lg)',
                backgroundColor: 'rgba(16, 185, 129, 0.08)',
                border: '1px solid rgba(16, 185, 129, 0.25)',
                color: '#065f46',
                fontSize: '0.9375rem',
                fontWeight: 600,
                marginBottom: '1.5rem',
                lineHeight: '1.5',
              }}
            >
              Your password has been reset successfully! You can now log in using your new password.
            </div>

            <Button
              type="button"
              variant="primary"
              onClick={resetToLogin}
              style={{ width: '100%', padding: '0.75rem', fontSize: '0.9375rem' }}
            >
              Proceed to Sign In <ArrowRight size={16} />
            </Button>
          </div>
        )}
      </div>
    </div>
  );
};
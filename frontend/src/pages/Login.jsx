import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { Input } from '../components/common/Input';
import { Button } from '../components/common/Button';
import { Building2, Lock, User as UserIcon, ArrowRight } from 'lucide-react';

export const Login = () => {
  const { login, user } = useAuth();
  const { error: toastError, success: toastSuccess } = useToast();
  const navigate = useNavigate();

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [isReadOnly, setIsReadOnly] = useState(true);

  React.useEffect(() => {
    setUsername('');
    setPassword('');
    const timer = setTimeout(() => {
      setIsReadOnly(false);
    }, 200);
    return () => clearTimeout(timer);
  }, []);

  React.useEffect(() => {
    if (user) {
      const target = user.role === 'ADMIN' ? '/admin/dashboard' : user.role === 'WARDEN' ? '/warden/dashboard' : '/student/dashboard';
      navigate(target, { replace: true });
    }
  }, [user, navigate]);

  const handleSubmit = async (e) => {
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
          maxWidth: '440px',
          backgroundColor: 'var(--bg-surface)',
          border: '1px solid var(--border-color)',
          borderRadius: 'var(--radius-xl)',
          boxShadow: 'var(--shadow-xl)',
          padding: '2.5rem 2rem',
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
            Welcome Back
          </h2>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
            Sign in to access your hostel account
          </p>
        </div>

        {errorMessage && (
          <div
            style={{
              padding: '0.75rem 1rem',
              borderRadius: 'var(--radius-md)',
              backgroundColor: 'var(--danger-light)',
              border: '1px solid var(--danger-border)',
              color: 'var(--danger-text)',
              fontSize: '0.875rem',
              fontWeight: 500,
              marginBottom: '1.25rem',
            }}
          >
            {errorMessage}
          </div>
        )}

        <form onSubmit={handleSubmit} autoComplete="off" noValidate>
          {/* Hidden trap inputs to prevent aggressive browser autofill */}
          <input
            type="text"
            name="prevent_autofill_username"
            style={{ display: 'none' }}
            tabIndex={-1}
            aria-hidden="true"
            autoComplete="off"
          />
          <input
            type="password"
            name="prevent_autofill_password"
            style={{ display: 'none' }}
            tabIndex={-1}
            aria-hidden="true"
            autoComplete="off"
          />

          <Input
            label="Username or Email"
            id="username"
            name="username"
            type="text"
            icon={UserIcon}
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            onFocus={() => setIsReadOnly(false)}
            onClick={() => setIsReadOnly(false)}
            readOnly={isReadOnly}
            placeholder="Enter your username or email"
            autoComplete="off"
            autoCorrect="off"
            autoCapitalize="none"
            spellCheck="false"
            required
          />

          <Input
            label="Password"
            id="password"
            name="password"
            type="password"
            icon={Lock}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            onFocus={() => setIsReadOnly(false)}
            onClick={() => setIsReadOnly(false)}
            readOnly={isReadOnly}
            placeholder="Enter your password"
            autoComplete="new-password"
            required
          />

          <Button
            type="submit"
            variant="primary"
            loading={loading}
            style={{ width: '100%', marginTop: '0.5rem', padding: '0.75rem' }}
          >
            Sign In <ArrowRight size={16} />
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
          Don't have an account?{' '}
          <Link to="/register" style={{ fontWeight: 600, color: 'var(--primary)' }}>
            Register as Student
          </Link>
        </div>
      </div>
    </div>
  );
};
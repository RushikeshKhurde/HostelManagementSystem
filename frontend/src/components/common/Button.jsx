import React from 'react';
import { Loader2 } from 'lucide-react';

export const Button = ({
  children,
  variant = 'primary',
  size = 'md',
  loading = false,
  disabled = false,
  icon,
  className = '',
  ...props
}) => {
  const variantClass = {
    primary: 'btn-primary',
    secondary: 'btn-secondary',
    danger: 'btn-danger',
    outlineDanger: 'btn-outline-danger',
    outline: 'btn-secondary',
    ghost: 'btn-ghost',
  }[variant] || 'btn-primary';

  const sizeClass = {
    sm: 'btn-sm',
    md: '',
    lg: 'btn-lg',
  }[size] || '';

  const iconSize = size === 'sm' ? 14 : size === 'lg' ? 20 : 18;

  const renderIcon = () => {
    if (loading) {
      return <Loader2 className="animate-spin" size={iconSize} style={{ animation: 'spin 1s linear infinite' }} />;
    }
    if (!icon) return null;
    if (React.isValidElement(icon)) return icon;
    if (typeof icon === 'function') {
      const IconComponent = icon;
      return <IconComponent size={iconSize} />;
    }
    return null;
  };

  return (
    <button
      className={`btn ${variantClass} ${sizeClass} ${className}`}
      disabled={disabled || loading}
      {...props}
    >
      {renderIcon()}
      {children}
    </button>
  );
};
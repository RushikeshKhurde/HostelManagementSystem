import React, { useState, useEffect } from 'react';
import api from '../../services/api';
import { useToast } from '../../context/ToastContext';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { Modal } from '../../components/common/Modal';
import { LoadingSpinner } from '../../components/feedback/LoadingSpinner';
import { EmptyState } from '../../components/feedback/EmptyState';
import { Users, Search, Eye, BellRing, Send, CheckCircle2, AlertCircle } from 'lucide-react';

export const UserManagement = ({ isWarden = false }) => {
  const { success: toastSuccess, error: toastError } = useToast();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [roleFilter, setRoleFilter] = useState('ALL');
  const [selectedUser, setSelectedUser] = useState(null);
  const [sendingReminderId, setSendingReminderId] = useState(null);
  const [bulkLoading, setBulkLoading] = useState(false);
  const [bulkResult, setBulkResult] = useState(null);
  const [showBulkConfirm, setShowBulkConfirm] = useState(false);

  const fetchUsers = async () => {
    setLoading(true);
    try {
      const res = await api.get('/users');
      setUsers(res.data);
    } catch (err) {
      toastError(err.message || 'Failed to fetch users');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  const handleToggleStatus = async (user) => {
    if (isWarden) return;
    const nextStatus = user.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    try {
      await api.put(`/users/${user.id}/status`, { status: nextStatus });
      toastSuccess(`User ${user.fullName} is now ${nextStatus}.`);
      fetchUsers();
    } catch (err) {
      toastError(err.message || 'Failed to update user status');
    }
  };

  const handleSendFeeReminder = async (user) => {
    setSendingReminderId(user.id);
    try {
      const res = await api.post(`/notifications/fee-reminder/${user.id}`);
      toastSuccess(res.data.message || `Fee reminder sent to ${user.fullName}`);
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Failed to send fee reminder';
      toastError(msg);
    } finally {
      setSendingReminderId(null);
    }
  };

  const handleBulkFeeReminder = async () => {
    setShowBulkConfirm(false);
    setBulkLoading(true);
    try {
      const res = await api.post('/notifications/fee-reminder/all');
      setBulkResult(res.data);
      toastSuccess(res.data.message || 'Fee reminders processed successfully');
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Failed to process bulk fee reminders';
      toastError(msg);
    } finally {
      setBulkLoading(false);
    }
  };

  const filteredUsers = users.filter((u) => {
    const name = u.fullName || '';
    const email = u.email || '';
    const username = u.username || '';
    const mobile = u.mobileNumber || '';
    const matchesSearch =
      name.toLowerCase().includes(search.toLowerCase()) ||
      email.toLowerCase().includes(search.toLowerCase()) ||
      username.toLowerCase().includes(search.toLowerCase()) ||
      mobile.includes(search);
    const matchesRole = roleFilter === 'ALL' || u.role === roleFilter;
    return matchesSearch && matchesRole;
  });

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 800 }}>{isWarden ? 'Student & Resident Directory' : 'User & Student Directory'}</h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>
            {isWarden ? 'Inspect student resident profiles, emergency contact details, and account statuses.' : 'Inspect user accounts, student contact profiles, roles, and authorization status.'}
          </p>
        </div>
        <Button
          variant="primary"
          icon={Send}
          onClick={() => setShowBulkConfirm(true)}
          loading={bulkLoading}
        >
          Notify All Students With Pending Fees
        </Button>
      </div>

      <div
        className="card"
        style={{
          padding: '1rem 1.25rem',
          display: 'flex',
          gap: '1rem',
          alignItems: 'center',
          flexWrap: 'wrap',
          justifyContent: 'space-between',
        }}
      >
        <div style={{ position: 'relative', minWidth: '240px', flex: 1 }}>
          <Search size={16} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
          <input
            type="text"
            placeholder="Search by name, username, email, or mobile..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="form-input"
            style={{ paddingLeft: '2.25rem' }}
          />
        </div>

        <select
          value={roleFilter}
          onChange={(e) => setRoleFilter(e.target.value)}
          className="form-select"
          style={{ width: 'auto', minWidth: '150px' }}
        >
          <option value="ALL">All Roles</option>
          <option value="ADMIN">Administrators</option>
          <option value="WARDEN">Hostel Warden</option>
          <option value="USER">Students (USER)</option>
        </select>
      </div>

      {loading ? (
        <LoadingSpinner text="Loading user directory..." />
      ) : filteredUsers.length === 0 ? (
        <EmptyState icon={Users} title="No users found" description="No user accounts matched your search criteria." />
      ) : (
        <div className="table-container">
          <table className="table">
            <thead>
              <tr>
                <th>User Details</th>
                <th>Username</th>
                <th>Role</th>
                <th>Mobile Number</th>
                <th>Account Status</th>
                <th>Registered Date</th>
                <th style={{ textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredUsers.map((u) => (
                <tr key={u.id}>
                  <td>
                    <div style={{ fontWeight: 700 }}>{u.fullName}</div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{u.email}</div>
                  </td>
                  <td style={{ fontFamily: 'monospace', fontWeight: 600 }}>@{u.username}</td>
                  <td><Badge status={u.role}>{u.role === 'ADMIN' ? 'ADMINISTRATOR' : u.role === 'WARDEN' ? 'WARDEN' : 'STUDENT'}</Badge></td>
                  <td>{u.mobileNumber}</td>
                  <td><Badge status={u.status || 'ACTIVE'} /></td>
                  <td>{u.createdAt ? new Date(u.createdAt).toLocaleDateString() : 'N/A'}</td>
                  <td style={{ textAlign: 'right' }}>
                    <div style={{ display: 'inline-flex', gap: '0.375rem', alignItems: 'center' }}>
                      <Button variant="ghost" size="sm" icon={Eye} onClick={() => setSelectedUser(u)}>
                        View
                      </Button>
                      {u.role === 'USER' && (
                        <Button
                          variant="outline"
                          size="sm"
                          icon={BellRing}
                          onClick={() => handleSendFeeReminder(u)}
                          loading={sendingReminderId === u.id}
                          title="Send Fee Reminder Notification"
                        >
                          Remind Fee
                        </Button>
                      )}
                      {!isWarden && u.role !== 'ADMIN' && u.role !== 'WARDEN' && (
                        <Button
                          variant={u.status === 'ACTIVE' ? 'outlineDanger' : 'secondary'}
                          size="sm"
                          onClick={() => handleToggleStatus(u)}
                        >
                          {u.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}
                        </Button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <Modal
        isOpen={!!selectedUser}
        onClose={() => setSelectedUser(null)}
        title="Student Profile Overview"
        maxWidth="500px"
        footer={
          <div style={{ display: 'flex', gap: '0.5rem', width: '100%', justifyContent: 'flex-end' }}>
            {selectedUser && selectedUser.role === 'USER' && (
              <Button
                variant="outline"
                icon={BellRing}
                onClick={() => handleSendFeeReminder(selectedUser)}
                loading={sendingReminderId === selectedUser.id}
              >
                Send Fee Reminder
              </Button>
            )}
            <Button variant="primary" onClick={() => setSelectedUser(null)}>
              Close
            </Button>
          </div>
        }
      >
        {selectedUser && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', fontSize: '0.9375rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', paddingBottom: '1rem', borderBottom: '1px solid var(--border-color)' }}>
              <div
                style={{
                  width: '48px',
                  height: '48px',
                  borderRadius: 'var(--radius-full)',
                  backgroundColor: selectedUser.role === 'ADMIN' ? 'var(--primary)' : 'var(--accent)',
                  color: '#ffffff',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontWeight: 800,
                  fontSize: '1.25rem',
                }}
              >
                {selectedUser.fullName[0]?.toUpperCase()}
              </div>
              <div>
                <h3 style={{ fontSize: '1.125rem' }}>{selectedUser.fullName}</h3>
                <div style={{ color: 'var(--text-muted)', fontSize: '0.8125rem' }}>@{selectedUser.username} · {selectedUser.email}</div>
              </div>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.625rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: 'var(--text-muted)' }}>Role:</span>
                <strong>{selectedUser.role}</strong>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: 'var(--text-muted)' }}>Mobile Phone:</span>
                <span>{selectedUser.mobileNumber}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: 'var(--text-muted)' }}>Gender:</span>
                <span>{selectedUser.gender || 'Not specified'}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: 'var(--text-muted)' }}>Date of Birth:</span>
                <span>{selectedUser.dateOfBirth || 'Not provided'}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: 'var(--text-muted)' }}>Address:</span>
                <span style={{ textAlign: 'right', maxWidth: '60%' }}>{selectedUser.address || 'Not specified'}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: 'var(--text-muted)' }}>Account Status:</span>
                <Badge status={selectedUser.status || 'ACTIVE'} />
              </div>
            </div>
          </div>
        )}
      </Modal>

      {/* Bulk Fee Reminder Confirmation Modal */}
      <Modal
        isOpen={showBulkConfirm}
        onClose={() => setShowBulkConfirm(false)}
        title="Send Bulk Fee Reminders"
        maxWidth="480px"
        footer={
          <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end', width: '100%' }}>
            <Button variant="ghost" onClick={() => setShowBulkConfirm(false)}>
              Cancel
            </Button>
            <Button variant="primary" icon={Send} onClick={handleBulkFeeReminder} loading={bulkLoading}>
              Confirm & Send Reminders
            </Button>
          </div>
        }
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'flex-start' }}>
            <AlertCircle size={24} style={{ color: 'var(--primary)', flexShrink: 0, marginTop: '2px' }} />
            <div>
              <p style={{ fontWeight: 600, margin: 0, fontSize: '0.9375rem' }}>
                Are you sure you want to notify all students with pending fees?
              </p>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.8125rem', marginTop: '0.5rem', lineHeight: 1.5 }}>
                The system will automatically calculate remaining hostel fees for each student with an approved room booking. Students with unpaid balances will receive a personalized fee reminder notification. Students who were already notified of the exact pending amount will be safely skipped.
              </p>
            </div>
          </div>
        </div>
      </Modal>

      {/* Bulk Fee Reminder Result Modal */}
      <Modal
        isOpen={!!bulkResult}
        onClose={() => setBulkResult(null)}
        title="Fee Reminder Summary"
        maxWidth="480px"
        footer={
          <Button variant="primary" onClick={() => setBulkResult(null)}>
            Done
          </Button>
        }
      >
        {bulkResult && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
            <div style={{ textAlign: 'center', padding: '1rem', backgroundColor: 'var(--bg-subtle)', borderRadius: 'var(--radius-md)' }}>
              <CheckCircle2 size={36} color="var(--success)" style={{ margin: '0 auto 0.5rem' }} />
              <div style={{ fontSize: '1.125rem', fontWeight: 800 }}>Fee Reminders Dispatched</div>
              <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', marginTop: '2px' }}>
                {bulkResult.message || 'Operation completed successfully'}
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
              <div style={{ padding: '0.75rem', border: '1px solid var(--border-color)', borderRadius: 'var(--radius-md)', textAlign: 'center' }}>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 700 }}>Total Checked</div>
                <div style={{ fontSize: '1.25rem', fontWeight: 800, marginTop: '0.25rem' }}>{bulkResult.totalStudentsChecked ?? 0}</div>
              </div>
              <div style={{ padding: '0.75rem', border: '1px solid var(--border-color)', borderRadius: 'var(--radius-md)', textAlign: 'center' }}>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 700 }}>Pending Fees</div>
                <div style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--warning)', marginTop: '0.25rem' }}>{bulkResult.studentsWithPendingFees ?? 0}</div>
              </div>
              <div style={{ padding: '0.75rem', border: '1px solid var(--border-color)', borderRadius: 'var(--radius-md)', textAlign: 'center', backgroundColor: 'rgba(34, 197, 94, 0.05)' }}>
                <div style={{ fontSize: '0.75rem', color: 'var(--success)', textTransform: 'uppercase', fontWeight: 700 }}>Notifications Sent</div>
                <div style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--success)', marginTop: '0.25rem' }}>{bulkResult.notificationsCreated ?? 0}</div>
              </div>
              <div style={{ padding: '0.75rem', border: '1px solid var(--border-color)', borderRadius: 'var(--radius-md)', textAlign: 'center' }}>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 700 }}>Skipped (Duplicate)</div>
                <div style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--text-muted)', marginTop: '0.25rem' }}>{bulkResult.notificationsSkipped ?? 0}</div>
              </div>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
};
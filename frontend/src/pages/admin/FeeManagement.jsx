import React, { useState, useEffect } from 'react';
import api from '../../services/api';
import { useToast } from '../../context/ToastContext';
import { Badge } from '../../components/common/Badge';
import { Modal } from '../../components/common/Modal';
import { Button } from '../../components/common/Button';
import { LoadingSpinner } from '../../components/feedback/LoadingSpinner';
import { EmptyState } from '../../components/feedback/EmptyState';
import { CreditCard, Search, FileText, CheckCircle2, Send, AlertCircle } from 'lucide-react';

export const FeeManagement = () => {
  const { success: toastSuccess, error: toastError } = useToast();
  const [payments, setPayments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [selectedPayment, setSelectedPayment] = useState(null);
  const [bulkLoading, setBulkLoading] = useState(false);
  const [bulkResult, setBulkResult] = useState(null);
  const [showBulkConfirm, setShowBulkConfirm] = useState(false);

  const fetchPayments = async () => {
    setLoading(true);
    try {
      const res = await api.get('/payments');
      setPayments(res.data);
    } catch (err) {
      toastError(err.message || 'Failed to fetch payments');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPayments();
  }, []);

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

  const totalRevenue = payments.reduce((acc, p) => (p.status === 'SUCCESS' ? acc + p.amount : acc), 0);

  const filteredPayments = payments.filter((p) => {
    const studentName = p.booking?.student?.fullName || '';
    const txnRef = p.transactionRef || '';
    const roomNum = p.booking?.room?.roomNumber || '';
    return (
      studentName.toLowerCase().includes(search.toLowerCase()) ||
      txnRef.toLowerCase().includes(search.toLowerCase()) ||
      roomNum.toLowerCase().includes(search.toLowerCase())
    );
  });

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 800 }}>Fee & Payment Ledger</h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>
            Comprehensive records of student rent collections and transaction receipts.
          </p>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', flexWrap: 'wrap' }}>
          <Button
            variant="primary"
            icon={Send}
            onClick={() => setShowBulkConfirm(true)}
            loading={bulkLoading}
          >
            Notify All Students With Pending Fees
          </Button>
          <div className="card" style={{ padding: '0.75rem 1.25rem', backgroundColor: 'var(--success-light)', borderColor: 'var(--success-border)' }}>
            <div style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--success-text)', textTransform: 'uppercase' }}>Total Collections</div>
            <div style={{ fontSize: '1.5rem', fontWeight: 800, color: 'var(--success)' }}>₹{totalRevenue.toLocaleString('en-IN')}</div>
          </div>
        </div>
      </div>

      {/* Search Bar */}
      <div className="card" style={{ padding: '1rem 1.25rem' }}>
        <div style={{ position: 'relative' }}>
          <Search size={16} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
          <input
            type="text"
            placeholder="Search by student name, transaction reference, or room number..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="form-input"
            style={{ paddingLeft: '2.25rem' }}
          />
        </div>
      </div>

      {loading ? (
        <LoadingSpinner text="Loading payment ledger..." />
      ) : filteredPayments.length === 0 ? (
        <EmptyState
          icon={CreditCard}
          title="No payment records found"
          description="No student rent payments matching your search were found."
        />
      ) : (
        <div className="table-container">
          <table className="table">
            <thead>
              <tr>
                <th>Transaction Ref</th>
                <th>Student</th>
                <th>Allocated Room</th>
                <th>Amount Paid</th>
                <th>Payment Mode</th>
                <th>Date & Time</th>
                <th>Status</th>
                <th style={{ textAlign: 'right' }}>Receipt</th>
              </tr>
            </thead>
            <tbody>
              {filteredPayments.map((p) => (
                <tr key={p.id}>
                  <td style={{ fontFamily: 'monospace', fontWeight: 700 }}>{p.transactionRef}</td>
                  <td>
                    <div style={{ fontWeight: 600 }}>{p.booking?.student?.fullName}</div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{p.booking?.student?.email}</div>
                  </td>
                  <td>Room {p.booking?.room?.roomNumber}</td>
                  <td style={{ fontWeight: 700, color: 'var(--success)' }}>₹{p.amount?.toLocaleString('en-IN')}</td>
                  <td><span className="badge badge-neutral">{p.method}</span></td>
                  <td>{p.paidAt ? new Date(p.paidAt).toLocaleString() : 'N/A'}</td>
                  <td><Badge status={p.status} /></td>
                  <td style={{ textAlign: 'right' }}>
                    <Button variant="ghost" size="sm" icon={FileText} onClick={() => setSelectedPayment(p)}>
                      Receipt
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Payment Receipt Modal */}
      <Modal
        isOpen={!!selectedPayment}
        onClose={() => setSelectedPayment(null)}
        title="Payment Receipt Details"
        maxWidth="480px"
        footer={
          <Button variant="primary" onClick={() => setSelectedPayment(null)}>
            Close
          </Button>
        }
      >
        {selectedPayment && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', fontSize: '0.9375rem' }}>
            <div style={{ textAlign: 'center', padding: '1rem', backgroundColor: 'var(--bg-subtle)', borderRadius: 'var(--radius-md)' }}>
              <CheckCircle2 size={36} color="var(--success)" style={{ margin: '0 auto 0.5rem' }} />
              <div style={{ fontSize: '1.25rem', fontWeight: 800 }}>₹{selectedPayment.amount?.toLocaleString('en-IN')}</div>
              <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>Payment Successful</div>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.5rem' }}>
                <span style={{ color: 'var(--text-muted)' }}>Transaction Ref</span>
                <span style={{ fontFamily: 'monospace', fontWeight: 600 }}>{selectedPayment.transactionRef}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.5rem' }}>
                <span style={{ color: 'var(--text-muted)' }}>Student</span>
                <span style={{ fontWeight: 600 }}>{selectedPayment.booking?.student?.fullName}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.5rem' }}>
                <span style={{ color: 'var(--text-muted)' }}>Allocated Room</span>
                <span style={{ fontWeight: 600 }}>Room {selectedPayment.booking?.room?.roomNumber}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.5rem' }}>
                <span style={{ color: 'var(--text-muted)' }}>Payment Method</span>
                <span style={{ fontWeight: 600 }}>{selectedPayment.method}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: 'var(--text-muted)' }}>Timestamp</span>
                <span>{selectedPayment.paidAt ? new Date(selectedPayment.paidAt).toLocaleString() : 'N/A'}</span>
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
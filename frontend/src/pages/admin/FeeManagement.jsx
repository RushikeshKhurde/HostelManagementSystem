import React, { useState, useEffect } from 'react';
import api from '../../services/api';
import { useToast } from '../../context/ToastContext';
import { Badge } from '../../components/common/Badge';
import { Modal } from '../../components/common/Modal';
import { Button } from '../../components/common/Button';
import { LoadingSpinner } from '../../components/feedback/LoadingSpinner';
import { EmptyState } from '../../components/feedback/EmptyState';
import { CreditCard, Search, FileText, CheckCircle2 } from 'lucide-react';

export const FeeManagement = () => {
  const { error: toastError } = useToast();
  const [payments, setPayments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [selectedPayment, setSelectedPayment] = useState(null);

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
        <div className="card" style={{ padding: '0.75rem 1.25rem', backgroundColor: 'var(--success-light)', borderColor: 'var(--success-border)' }}>
          <div style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--success-text)', textTransform: 'uppercase' }}>Total Collections</div>
          <div style={{ fontSize: '1.5rem', fontWeight: 800, color: 'var(--success)' }}>₹{totalRevenue.toLocaleString('en-IN')}</div>
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
    </div>
  );
};
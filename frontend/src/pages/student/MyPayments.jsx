import React, { useState, useEffect } from 'react';
import api from '../../services/api';
import { useToast } from '../../context/ToastContext';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { Modal } from '../../components/common/Modal';
import { LoadingSpinner } from '../../components/feedback/LoadingSpinner';
import { EmptyState } from '../../components/feedback/EmptyState';
import { CreditCard, Plus, CheckCircle2, FileText, Download } from 'lucide-react';

export const MyPayments = () => {
  const { success: toastSuccess, error: toastError } = useToast();
  const [payments, setPayments] = useState([]);
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);

  // Pay Modal
  const [isPayModalOpen, setIsPayModalOpen] = useState(false);
  const [selectedBookingId, setSelectedBookingId] = useState('');
  const [paymentMethod, setPaymentMethod] = useState('UPI');
  const [paying, setPaying] = useState(false);

  // Receipt Modal
  const [selectedReceipt, setSelectedReceipt] = useState(null);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [paymentsRes, bookingsRes] = await Promise.all([
        api.get('/payments/my'),
        api.get('/bookings/my'),
      ]);
      setPayments(paymentsRes.data);
      const approvedBookings = bookingsRes.data.filter((b) => b.status === 'APPROVED');
      setBookings(approvedBookings);
      if (approvedBookings.length > 0) {
        setSelectedBookingId(approvedBookings[0].id);
      }
    } catch (err) {
      toastError(err.message || 'Failed to fetch payments');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleMakePayment = async (e) => {
    e.preventDefault();
    if (!selectedBookingId) {
      toastError('No approved booking selected.');
      return;
    }
    setPaying(true);
    try {
      const res = await api.post('/payments', {
        bookingId: parseInt(selectedBookingId, 10),
        method: paymentMethod,
      });
      toastSuccess('Payment successful! Digital receipt generated.');
      setIsPayModalOpen(false);
      setSelectedReceipt(res.data);
      fetchData();
    } catch (err) {
      toastError(err.message || 'Payment failed');
    } finally {
      setPaying(false);
    }
  };

  const selectedBooking = bookings.find((b) => b.id === parseInt(selectedBookingId, 10));

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 800 }}>Fee Payments & Receipts</h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>
            Pay your monthly hostel rent and view digital receipts.
          </p>
        </div>
        {bookings.length > 0 && (
          <Button variant="primary" icon={CreditCard} onClick={() => setIsPayModalOpen(true)}>
            Pay Monthly Rent
          </Button>
        )}
      </div>

      {loading ? (
        <LoadingSpinner text="Loading payment records..." />
      ) : payments.length === 0 ? (
        <EmptyState
          icon={CreditCard}
          title="No payments made yet"
          description="You have not made any rent payments yet."
          actionText={bookings.length > 0 ? 'Pay Rent Now' : undefined}
          onAction={() => setIsPayModalOpen(true)}
        />
      ) : (
        <div className="table-container">
          <table className="table">
            <thead>
              <tr>
                <th>Transaction Ref</th>
                <th>Room</th>
                <th>Amount Paid</th>
                <th>Payment Mode</th>
                <th>Date & Time</th>
                <th>Status</th>
                <th style={{ textAlign: 'right' }}>Receipt</th>
              </tr>
            </thead>
            <tbody>
              {payments.map((p) => (
                <tr key={p.id}>
                  <td style={{ fontFamily: 'monospace', fontWeight: 700 }}>{p.transactionRef}</td>
                  <td>Room {p.booking?.room?.roomNumber}</td>
                  <td style={{ fontWeight: 700, color: 'var(--success)' }}>₹{p.amount?.toLocaleString('en-IN')}</td>
                  <td><span className="badge badge-neutral">{p.method}</span></td>
                  <td>{p.paidAt ? new Date(p.paidAt).toLocaleString() : 'N/A'}</td>
                  <td><Badge status={p.status} /></td>
                  <td style={{ textAlign: 'right' }}>
                    <Button variant="ghost" size="sm" icon={FileText} onClick={() => setSelectedReceipt(p)}>
                      View Receipt
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Pay Rent Modal */}
      <Modal
        isOpen={isPayModalOpen}
        onClose={() => setIsPayModalOpen(false)}
        title="Pay Hostel Monthly Rent"
        maxWidth="480px"
      >
        <form onSubmit={handleMakePayment} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          <div>
            <label className="form-label">Select Allocated Room</label>
            <select
              className="form-select"
              value={selectedBookingId}
              onChange={(e) => setSelectedBookingId(e.target.value)}
              required
            >
              {bookings.map((b) => (
                <option key={b.id} value={b.id}>
                  Room {b.room?.roomNumber} ({b.room?.roomType}) - ₹{b.room?.pricePerMonth?.toLocaleString('en-IN')}
                </option>
              ))}
            </select>
          </div>

          {selectedBooking && (
            <div style={{ padding: '1rem', backgroundColor: 'var(--bg-subtle)', borderRadius: 'var(--radius-md)' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.25rem' }}>
                <span style={{ color: 'var(--text-muted)' }}>Room Number:</span>
                <strong>Room {selectedBooking.room?.roomNumber}</strong>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: 'var(--text-muted)' }}>Amount Payable:</span>
                <strong style={{ fontSize: '1.25rem', color: 'var(--primary)' }}>
                  ₹{selectedBooking.room?.pricePerMonth?.toLocaleString('en-IN')}
                </strong>
              </div>
            </div>
          )}

          <div>
            <label className="form-label">Payment Gateway / Method</label>
            <select
              className="form-select"
              value={paymentMethod}
              onChange={(e) => setPaymentMethod(e.target.value)}
            >
              <option value="UPI">UPI (GooglePay / PhonePe / Paytm / BHIM)</option>
              <option value="CARD">Credit / Debit Card (Visa, MasterCard, RuPay)</option>
              <option value="NETBANKING">Net Banking (All Indian Banks)</option>
              <option value="CASH">Cash Deposit at Warden Office</option>
            </select>
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
            <Button type="button" variant="secondary" onClick={() => setIsPayModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" variant="primary" loading={paying}>
              Complete Payment
            </Button>
          </div>
        </form>
      </Modal>

      {/* Receipt Modal */}
      <Modal
        isOpen={!!selectedReceipt}
        onClose={() => setSelectedReceipt(null)}
        title="Payment Confirmation Receipt"
        maxWidth="460px"
        footer={
          <Button variant="primary" onClick={() => setSelectedReceipt(null)}>
            Done
          </Button>
        }
      >
        {selectedReceipt && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <div style={{ textAlign: 'center', padding: '1.25rem', backgroundColor: 'var(--success-light)', borderRadius: 'var(--radius-md)', border: '1px solid var(--success-border)' }}>
              <CheckCircle2 size={40} color="var(--success)" style={{ margin: '0 auto 0.5rem' }} />
              <div style={{ fontSize: '1.5rem', fontWeight: 800, color: 'var(--success)' }}>
                ₹{selectedReceipt.amount?.toLocaleString('en-IN')}
              </div>
              <div style={{ fontSize: '0.875rem', color: 'var(--success-text)', fontWeight: 600 }}>
                Payment Processed Successfully
              </div>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', fontSize: '0.875rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.5rem' }}>
                <span style={{ color: 'var(--text-muted)' }}>Transaction Reference:</span>
                <strong style={{ fontFamily: 'monospace' }}>{selectedReceipt.transactionRef}</strong>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.5rem' }}>
                <span style={{ color: 'var(--text-muted)' }}>Hostel Room:</span>
                <strong>Room {selectedReceipt.booking?.room?.roomNumber}</strong>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.5rem' }}>
                <span style={{ color: 'var(--text-muted)' }}>Payment Mode:</span>
                <strong>{selectedReceipt.method}</strong>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: 'var(--text-muted)' }}>Date & Time:</span>
                <span>{selectedReceipt.paidAt ? new Date(selectedReceipt.paidAt).toLocaleString() : 'Recent'}</span>
              </div>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
};
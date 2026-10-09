import React, { useState, useEffect } from 'react';
import api from '../../services/api';
import { useToast } from '../../context/ToastContext';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { Modal } from '../../components/common/Modal';
import { LoadingSpinner } from '../../components/feedback/LoadingSpinner';
import { EmptyState } from '../../components/feedback/EmptyState';
import {
  CreditCard,
  Plus,
  CheckCircle2,
  FileText,
  Printer,
  ShieldCheck,
  AlertCircle,
  Clock,
  Building,
  User,
  ArrowRight,
  Banknote,
  ExternalLink,
} from 'lucide-react';

// Helper to dynamically load Razorpay checkout script
const loadRazorpayScript = () => {
  return new Promise((resolve) => {
    if (window.Razorpay) {
      resolve(true);
      return;
    }
    const script = document.createElement('script');
    script.src = 'https://checkout.razorpay.com/v1/checkout.js';
    script.async = true;
    script.onload = () => resolve(true);
    script.onerror = () => resolve(false);
    document.body.appendChild(script);
  });
};

export const MyPayments = () => {
  const { success: toastSuccess, error: toastError, info: toastInfo } = useToast();
  const [payments, setPayments] = useState([]);
  const [pendingFees, setPendingFees] = useState([]);
  const [loading, setLoading] = useState(true);

  // Razorpay Payment in-flight state
  const [payingBookingId, setPayingBookingId] = useState(null);
  const [verifying, setVerifying] = useState(false);

  // Cash Payment Modal state
  const [isCashModalOpen, setIsCashModalOpen] = useState(false);
  const [selectedCashBookingId, setSelectedCashBookingId] = useState('');
  const [cashSubmitting, setCashSubmitting] = useState(false);

  // Receipt Modal state
  const [selectedReceipt, setSelectedReceipt] = useState(null);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [paymentsRes, pendingFeesRes] = await Promise.all([
        api.get('/payments/my'),
        api.get('/payments/pending-fees'),
      ]);
      setPayments(paymentsRes.data || []);
      setPendingFees(pendingFeesRes.data || []);

      const unpaidList = (pendingFeesRes.data || []).filter((f) => !f.paid);
      if (unpaidList.length > 0) {
        setSelectedCashBookingId(unpaidList[0].bookingId);
      }
    } catch (err) {
      toastError(err.message || 'Failed to fetch payments data');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  // REALISTIC & SECURE PAYMENT GATEWAY FLOW (RAZORPAY)
  const handlePayWithRazorpay = async (bookingId) => {
    setPayingBookingId(bookingId);
    try {
      // Step 1: Request backend to create Razorpay Order
      // Client sends ONLY bookingId. Amount is extracted strictly from DB by backend.
      const orderRes = await api.post('/payments/create-order', {
        bookingId: bookingId,
      });
      const orderData = orderRes.data;

      // Step 2: Ensure Razorpay Checkout script is loaded
      const isLoaded = await loadRazorpayScript();
      if (!isLoaded) {
        toastError('Unable to connect to Razorpay payment gateway. Please check your internet connection.');
        setPayingBookingId(null);
        return;
      }

      // Step 3: Configure Razorpay Checkout options
      const options = {
        key: orderData.keyId,
        amount: orderData.amountInPaise,
        currency: orderData.currency || 'INR',
        name: orderData.companyName || 'Hostel Management System',
        description: orderData.description || `Hostel Rent - Room ${orderData.roomNumber}`,
        order_id: orderData.orderId,
        image: '/vite.svg',
        prefill: {
          name: orderData.studentName || '',
          email: orderData.studentEmail || '',
          contact: orderData.studentMobile || '',
        },
        notes: {
          bookingId: String(orderData.bookingId),
          roomNumber: String(orderData.roomNumber),
        },
        theme: {
          color: '#2563eb', // Royal Blue
        },
        modal: {
          ondismiss: () => {
            setPayingBookingId(null);
            toastInfo('Payment checkout was dismissed.');
          },
        },
        // Step 4: Razorpay handles actual payment authentication (UPI PIN, 3D Secure OTP, Card, NetBanking)
        // Then returns razorpay_order_id, razorpay_payment_id, razorpay_signature to this handler:
        handler: async (response) => {
          setVerifying(true);
          try {
            // Step 5: Frontend sends signature verification payload to backend
            const verifyRes = await api.post('/payments/verify-order', {
              razorpayOrderId: response.razorpay_order_id,
              razorpayPaymentId: response.razorpay_payment_id,
              razorpaySignature: response.razorpay_signature,
              bookingId: orderData.bookingId,
              paymentMethod: 'RAZORPAY',
            });

            toastSuccess('Payment successful! Cryptographically verified via Razorpay.');
            // Step 6: Show digital payment receipt
            setSelectedReceipt(verifyRes.data);
            await fetchData();
          } catch (err) {
            toastError(err.message || 'Payment verification failed');
            await fetchData();
          } finally {
            setVerifying(false);
            setPayingBookingId(null);
          }
        },
      };

      const rzp = new window.Razorpay(options);
      rzp.on('payment.failed', (response) => {
        const errorDesc = response.error?.description || response.error?.reason || 'Payment failed';
        toastError(`Payment Failed: ${errorDesc}`);
        setPayingBookingId(null);
      });
      rzp.open();
    } catch (err) {
      toastError(err.response?.data?.message || err.message || 'Failed to initiate Razorpay order');
      setPayingBookingId(null);
    }
  };

  // Cash deposit request fallback
  const handleCashPaymentSubmit = async (e) => {
    e.preventDefault();
    if (!selectedCashBookingId) {
      toastError('Please select a booking.');
      return;
    }
    setCashSubmitting(true);
    try {
      const res = await api.post('/payments', {
        bookingId: parseInt(selectedCashBookingId, 10),
        method: 'CASH',
      });
      toastSuccess('Cash payment recorded as PENDING. Please deposit cash at the warden office for confirmation.');
      setIsCashModalOpen(false);
      setSelectedReceipt(res.data);
      fetchData();
    } catch (err) {
      toastError(err.response?.data?.message || err.message || 'Cash payment request failed');
    } finally {
      setCashSubmitting(false);
    }
  };

  const unpaidFees = pendingFees.filter((f) => !f.paid);
  const totalDueAmount = unpaidFees.reduce((acc, f) => acc + (Number(f.remainingAmount) || 0), 0);
  const totalPaidAmount = payments
    .filter((p) => p.status === 'SUCCESS')
    .reduce((acc, p) => acc + (Number(p.amount) || 0), 0);

  const handlePrintReceipt = () => {
    window.print();
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.75rem' }}>
      {/* Top Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.25rem' }}>
            <span className="badge badge-primary" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.35rem' }}>
              <ShieldCheck size={14} /> Razorpay Secured Gateway
            </span>
            <span className="badge badge-neutral" style={{ fontSize: '0.75rem' }}>
              Test Mode
            </span>
          </div>
          <h2 style={{ fontSize: '1.625rem', fontWeight: 800 }}>Fee Payments & Receipts</h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>
            Review unpaid hostel dues, make secure online payments via Razorpay, and download authentic digital receipts.
          </p>
        </div>

        {unpaidFees.length > 0 && (
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <Button
              variant="secondary"
              icon={Banknote}
              onClick={() => setIsCashModalOpen(true)}
            >
              Pay via Cash Deposit
            </Button>
          </div>
        )}
      </div>

      {/* Summary Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem' }}>
        <div className="card" style={{ padding: '1.25rem' }}>
          <div style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
            Total Outstanding Dues
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 800, color: totalDueAmount > 0 ? 'var(--danger)' : 'var(--success)', marginTop: '0.25rem' }}>
            ₹{totalDueAmount.toLocaleString('en-IN')}
          </div>
          <div style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', marginTop: '0.25rem' }}>
            {unpaidFees.length} pending fee bill(s)
          </div>
        </div>

        <div className="card" style={{ padding: '1.25rem' }}>
          <div style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
            Total Rent Settled
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 800, color: 'var(--success)', marginTop: '0.25rem' }}>
            ₹{totalPaidAmount.toLocaleString('en-IN')}
          </div>
          <div style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', marginTop: '0.25rem' }}>
            Lifetime paid rent transactions
          </div>
        </div>

        <div className="card" style={{ padding: '1.25rem' }}>
          <div style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
            Payment Gateway Status
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginTop: '0.5rem' }}>
            <span style={{ width: '10px', height: '10px', borderRadius: '50%', backgroundColor: '#22c55e', display: 'inline-block' }} />
            <strong style={{ fontSize: '1.125rem' }}>Razorpay Test Gateway</strong>
          </div>
          <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
            UPI • Cards • NetBanking active
          </div>
        </div>
      </div>

      {/* SECTION 1: UNPAID FEES / ACTION REQUIRED */}
      <div className="card" style={{ padding: '1.5rem', border: '1px solid var(--border-color)' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem', flexWrap: 'wrap', gap: '0.5rem' }}>
          <div>
            <h3 style={{ fontSize: '1.125rem', fontWeight: 800, margin: 0, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <AlertCircle size={20} color={unpaidFees.length > 0 ? 'var(--warning)' : 'var(--success)'} />
              Pending Hostel Dues & Actions
            </h3>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.8125rem', margin: '0.25rem 0 0 0' }}>
              Select an unpaid fee below to pay securely through Razorpay payment authentication.
            </p>
          </div>
        </div>

        {loading ? (
          <LoadingSpinner text="Checking pending fees..." />
        ) : unpaidFees.length === 0 ? (
          <div
            style={{
              padding: '2rem',
              textAlign: 'center',
              backgroundColor: 'rgba(34, 197, 94, 0.05)',
              borderRadius: 'var(--radius-md)',
              border: '1px solid rgba(34, 197, 94, 0.2)',
            }}
          >
            <CheckCircle2 size={40} color="var(--success)" style={{ margin: '0 auto 0.75rem' }} />
            <h4 style={{ fontWeight: 800, color: 'var(--success)', margin: 0, fontSize: '1.125rem' }}>
              All Hostel Fees Are Fully Paid!
            </h4>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginTop: '0.25rem', maxWidth: '480px', margin: '0.5rem auto 0' }}>
              You have no outstanding dues for your approved room allocations. Receipts for past payments are listed below.
            </p>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            {unpaidFees.map((fee) => (
              <div
                key={fee.bookingId}
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  padding: '1.25rem',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid var(--border-color)',
                  backgroundColor: 'var(--bg-subtle)',
                  flexWrap: 'wrap',
                  gap: '1rem',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                  <div
                    style={{
                      width: '48px',
                      height: '48px',
                      borderRadius: 'var(--radius-md)',
                      backgroundColor: 'var(--primary-light)',
                      color: 'var(--primary)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      flexShrink: 0,
                    }}
                  >
                    <Building size={24} />
                  </div>
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                      <span style={{ fontWeight: 800, fontSize: '1.0625rem', color: 'var(--text-primary)' }}>
                        Room {fee.roomNumber}
                      </span>
                      <span className="badge badge-info">{fee.roomType}</span>
                      <span className="badge badge-warning">UNPAID</span>
                    </div>
                    <div style={{ color: 'var(--text-muted)', fontSize: '0.8125rem', marginTop: '0.25rem' }}>
                      Check-in Date: {fee.checkInDate || 'Active'} • Monthly Rent: ₹{fee.monthlyRent?.toLocaleString('en-IN')}
                    </div>
                  </div>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem', flexWrap: 'wrap' }}>
                  <div style={{ textAlign: 'right' }}>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 700 }}>
                      Payable Amount
                    </div>
                    <div style={{ fontSize: '1.375rem', fontWeight: 800, color: 'var(--primary)' }}>
                      ₹{fee.remainingAmount?.toLocaleString('en-IN')}
                    </div>
                  </div>

                  <div style={{ display: 'flex', gap: '0.5rem' }}>
                    <Button
                      variant="primary"
                      icon={CreditCard}
                      loading={payingBookingId === fee.bookingId}
                      disabled={payingBookingId !== null || verifying}
                      onClick={() => handlePayWithRazorpay(fee.bookingId)}
                    >
                      {payingBookingId === fee.bookingId ? 'Opening Razorpay...' : 'Pay Now (Razorpay)'}
                    </Button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* SECTION 2: PAYMENT HISTORY LEDGER */}
      <div className="card" style={{ padding: '1.5rem', border: '1px solid var(--border-color)' }}>
        <div style={{ marginBottom: '1.25rem' }}>
          <h3 style={{ fontSize: '1.125rem', fontWeight: 800, margin: 0 }}>Payment History & Digital Receipts</h3>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.8125rem', margin: '0.25rem 0 0 0' }}>
            Complete ledger of your online and offline hostel payments.
          </p>
        </div>

        {loading ? (
          <LoadingSpinner text="Loading payment records..." />
        ) : payments.length === 0 ? (
          <EmptyState
            icon={CreditCard}
            title="No payments made yet"
            description="You have not made any rent payments yet."
          />
        ) : (
          <div className="table-container">
            <table className="table">
              <thead>
                <tr>
                  <th>Transaction Ref / Gateway ID</th>
                  <th>Room</th>
                  <th>Amount Paid</th>
                  <th>Payment Gateway / Mode</th>
                  <th>Date & Time</th>
                  <th>Status</th>
                  <th style={{ textAlign: 'right' }}>Action</th>
                </tr>
              </thead>
              <tbody>
                {payments.map((p) => (
                  <tr key={p.id}>
                    <td>
                      <div style={{ fontFamily: 'monospace', fontWeight: 700, color: 'var(--text-primary)' }}>
                        {p.transactionRef}
                      </div>
                      {p.razorpayOrderId && (
                        <div style={{ fontSize: '0.6875rem', color: 'var(--text-muted)', fontFamily: 'monospace' }}>
                          Order: {p.razorpayOrderId}
                        </div>
                      )}
                    </td>
                    <td>Room {p.booking?.room?.roomNumber}</td>
                    <td style={{ fontWeight: 700, color: 'var(--success)' }}>
                      ₹{p.amount?.toLocaleString('en-IN')}
                    </td>
                    <td>
                      <span className={`badge ${p.method === 'RAZORPAY' ? 'badge-primary' : 'badge-neutral'}`}>
                        {p.method}
                      </span>
                    </td>
                    <td>{p.paidAt ? new Date(p.paidAt).toLocaleString() : 'Pending confirmation'}</td>
                    <td>
                      <Badge status={p.status} />
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      <Button
                        variant="ghost"
                        size="sm"
                        icon={FileText}
                        onClick={() => setSelectedReceipt(p)}
                      >
                        Receipt
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* MODAL 1: CASH DEPOSIT REQUEST */}
      <Modal
        isOpen={isCashModalOpen}
        onClose={() => setIsCashModalOpen(false)}
        title="Deposit Cash at Warden Office"
        maxWidth="480px"
      >
        <form onSubmit={handleCashPaymentSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          <div style={{ padding: '0.875rem', backgroundColor: 'var(--bg-subtle)', borderRadius: 'var(--radius-md)', fontSize: '0.8125rem', color: 'var(--text-secondary)', lineHeight: 1.5 }}>
            <strong style={{ color: 'var(--text-primary)' }}>Cash Deposit Policy:</strong>
            <br />
            Submitting this request marks the payment as <strong>PENDING</strong>. Please visit the hostel warden office to deposit physical cash. The warden will confirm receipt in the system, converting your status to <strong>SUCCESS</strong>.
          </div>

          <div>
            <label className="form-label">Select Allocated Room</label>
            <select
              className="form-select"
              value={selectedCashBookingId}
              onChange={(e) => setSelectedCashBookingId(e.target.value)}
              required
            >
              {unpaidFees.map((f) => (
                <option key={f.bookingId} value={f.bookingId}>
                  Room {f.roomNumber} ({f.roomType}) - ₹{f.remainingAmount?.toLocaleString('en-IN')}
                </option>
              ))}
            </select>
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
            <Button type="button" variant="secondary" onClick={() => setIsCashModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" variant="primary" loading={cashSubmitting}>
              Submit Cash Request
            </Button>
          </div>
        </form>
      </Modal>

      {/* MODAL 2: OFFICIAL DIGITAL PAYMENT RECEIPT */}
      <Modal
        isOpen={!!selectedReceipt}
        onClose={() => setSelectedReceipt(null)}
        title="Official Payment Receipt"
        maxWidth="500px"
        footer={
          <div style={{ display: 'flex', justifyContent: 'space-between', width: '100%' }}>
            <Button variant="secondary" icon={Printer} onClick={handlePrintReceipt}>
              Print Receipt
            </Button>
            <Button variant="primary" onClick={() => setSelectedReceipt(null)}>
              Close
            </Button>
          </div>
        }
      >
        {selectedReceipt && (
          <div id="printable-receipt" style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
            {/* Header with Stamp */}
            <div
              style={{
                textAlign: 'center',
                padding: '1.25rem',
                backgroundColor: 'rgba(34, 197, 94, 0.08)',
                borderRadius: 'var(--radius-md)',
                border: '1px solid rgba(34, 197, 94, 0.25)',
              }}
            >
              <CheckCircle2 size={42} color="var(--success)" style={{ margin: '0 auto 0.5rem' }} />
              <div style={{ fontSize: '1.625rem', fontWeight: 800, color: 'var(--success)' }}>
                ₹{selectedReceipt.amount?.toLocaleString('en-IN')}
              </div>
              <div style={{ fontSize: '0.875rem', color: 'var(--text-primary)', fontWeight: 700, marginTop: '2px' }}>
                Payment {selectedReceipt.status === 'SUCCESS' ? 'Completed Successfully' : selectedReceipt.status}
              </div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '2px' }}>
                Hostel Rent Digital Invoice
              </div>
            </div>

            {/* Receipt Details Grid */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.625rem', fontSize: '0.875rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.5rem' }}>
                <span style={{ color: 'var(--text-muted)' }}>Transaction Reference</span>
                <span style={{ fontFamily: 'monospace', fontWeight: 700 }}>{selectedReceipt.transactionRef}</span>
              </div>

              {selectedReceipt.razorpayOrderId && (
                <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.5rem' }}>
                  <span style={{ color: 'var(--text-muted)' }}>Razorpay Order ID</span>
                  <span style={{ fontFamily: 'monospace', fontSize: '0.8125rem' }}>{selectedReceipt.razorpayOrderId}</span>
                </div>
              )}

              {selectedReceipt.razorpayPaymentId && (
                <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.5rem' }}>
                  <span style={{ color: 'var(--text-muted)' }}>Razorpay Payment ID</span>
                  <span style={{ fontFamily: 'monospace', fontSize: '0.8125rem' }}>{selectedReceipt.razorpayPaymentId}</span>
                </div>
              )}

              <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.5rem' }}>
                <span style={{ color: 'var(--text-muted)' }}>Allocated Room</span>
                <strong>
                  Room {selectedReceipt.booking?.room?.roomNumber}{' '}
                  {selectedReceipt.booking?.room?.roomType ? `(${selectedReceipt.booking?.room?.roomType})` : ''}
                </strong>
              </div>

              {selectedReceipt.booking?.student?.fullName && (
                <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.5rem' }}>
                  <span style={{ color: 'var(--text-muted)' }}>Student Name</span>
                  <strong>{selectedReceipt.booking?.student?.fullName}</strong>
                </div>
              )}

              <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.5rem' }}>
                <span style={{ color: 'var(--text-muted)' }}>Payment Mode</span>
                <span className="badge badge-primary">{selectedReceipt.method}</span>
              </div>

              <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.5rem' }}>
                <span style={{ color: 'var(--text-muted)' }}>Date & Time</span>
                <span>{selectedReceipt.paidAt ? new Date(selectedReceipt.paidAt).toLocaleString() : 'N/A'}</span>
              </div>

              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: 'var(--text-muted)' }}>Security Verification</span>
                <span style={{ color: 'var(--success)', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                  <ShieldCheck size={14} /> HMAC SHA-256 Verified
                </span>
              </div>
            </div>

            <div
              style={{
                fontSize: '0.75rem',
                color: 'var(--text-muted)',
                textAlign: 'center',
                borderTop: '1px dashed var(--border-color)',
                paddingTop: '0.75rem',
              }}
            >
              This is a computer-generated digital receipt issued by Hostel Management System.
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
};
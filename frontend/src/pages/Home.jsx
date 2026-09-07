import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Building2,
  Shield,
  Wifi,
  Utensils,
  Dumbbell,
  BookOpen,
  Sparkles,
  CheckCircle2,
  ArrowRight,
  UserCheck,
  CalendarCheck,
  CreditCard,
  MessageSquare,
  MapPin,
  Phone,
  Mail,
  User as UserIcon,
  LogOut,
} from 'lucide-react';
import { FlipCard } from '../components/common/FlipCard';
import { Modal } from '../components/common/Modal';
import { Button } from '../components/common/Button';
import { Footer } from '../components/layout/Footer';
import { useAuth } from '../context/AuthContext';

export const Home = () => {
  const { user, logout } = useAuth();
  const [rulesOpen, setRulesOpen] = useState(false);
  const [contactSent, setContactSent] = useState(false);

  const amenities = [
    {
      icon: Wifi,
      title: 'Gigabit Wi-Fi',
      badge: '24/7 Access',
      subtitle: 'High-speed fiber connectivity in all rooms and study lounges.',
      backContent: 'Dual-band 5GHz coverage across all floors. Dedicated study zone bandwidth prioritization and zero dead zones.',
      actionText: 'View Network Info',
    },
    {
      icon: Utensils,
      title: 'Hygienic Dining',
      badge: '4 Meals/Day',
      subtitle: 'Nutritious chef-prepared breakfast, lunch, snacks, and dinner.',
      backContent: 'FSSAI certified kitchen, seasonal rotating menu, purified RO drinking water, and special dietary options on request.',
      actionText: 'Dining Policy',
    },
    {
      icon: Shield,
      title: 'Multi-Tier Security',
      badge: 'Guarded 24/7',
      subtitle: 'Biometric access control, CCTV monitoring, and full-time wardens.',
      backContent: 'Continuous surveillance of entries & exits, electronic visitor logs, emergency response protocols, and fenced perimeter.',
      actionText: 'Security Protocols',
    },
    {
      icon: Dumbbell,
      title: 'Fitness Center',
      badge: 'Equipped Gym',
      subtitle: 'Modern strength and cardio equipment for student health.',
      backContent: 'Treadmills, free weights, resistance machines, and open yoga spaces open daily 6:00 AM – 10:00 PM.',
      actionText: 'Gym Timings',
    },
    {
      icon: BookOpen,
      title: 'Quiet Study Halls',
      badge: 'Air-Conditioned',
      subtitle: 'Sound-isolated study rooms with individual power ports.',
      backContent: 'Ergonomic seating, reference library, whiteboard walls, and quiet policy enforced 24/7 for exam preparation.',
      actionText: 'Study Facilities',
    },
    {
      icon: Sparkles,
      title: 'Housekeeping & Laundry',
      badge: 'Included',
      subtitle: 'Daily room sanitization, trash collection, and laundry machines.',
      backContent: 'Automated commercial washing machines, steam press stations, and dedicated professional cleaning staff daily.',
      actionText: 'Cleaning Schedule',
    },
  ];

  const roomTypes = [
    {
      name: 'Single Premium',
      capacity: '1 Student',
      price: '₹8,000',
      description: 'Private sanctuary with attached bath, study desk, wardrobe, and balcony.',
      features: ['Attached Bathroom', 'Private Balcony', 'Dedicated Study Desk', 'High-Speed LAN Port'],
    },
    {
      name: 'Double Deluxe',
      capacity: '2 Students',
      price: '₹5,500',
      description: 'Spacious shared room with dual study stations and individual wardrobes.',
      features: ['Dual Wardrobes', 'Twin Study Stations', 'Attached Washroom', 'Air Cooler / AC Option'],
    },
    {
      name: 'Triple Economy',
      capacity: '3 Students',
      price: '₹4,200',
      description: 'Affordable community living with ample personal storage and shared balcony.',
      features: ['Individual Lockers', 'Study Nook', 'Natural Ventilation', 'Regular Housekeeping'],
    },
  ];

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <header
        style={{
          height: '72px',
          backgroundColor: 'var(--bg-surface)',
          borderBottom: '1px solid var(--border-color)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '0 2rem',
          position: 'sticky',
          top: 0,
          zIndex: 100,
          backdropFilter: 'blur(8px)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <div
            style={{
              width: '38px',
              height: '38px',
              borderRadius: 'var(--radius-md)',
              background: 'linear-gradient(135deg, var(--primary) 0%, var(--accent) 100%)',
              color: '#ffffff',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: '0 2px 8px rgba(37, 99, 235, 0.3)',
            }}
          >
            <Building2 size={22} />
          </div>
          <div>
            <span style={{ fontWeight: 800, fontSize: '1.25rem', color: 'var(--text-primary)', letterSpacing: '-0.02em' }}>
              SmartHostel
            </span>
          </div>
        </div>

        <nav style={{ display: 'flex', alignItems: 'center', gap: '1.5rem' }}>
          <a href="#features" style={{ color: 'var(--text-secondary)', fontWeight: 500, fontSize: '0.9375rem' }}>Features</a>
          <a href="#amenities" style={{ color: 'var(--text-secondary)', fontWeight: 500, fontSize: '0.9375rem' }}>Amenities</a>
          <a href="#rooms" style={{ color: 'var(--text-secondary)', fontWeight: 500, fontSize: '0.9375rem' }}>Rooms & Fees</a>
          <button
            onClick={() => setRulesOpen(true)}
            style={{ background: 'none', border: 'none', color: 'var(--text-secondary)', fontWeight: 500, fontSize: '0.9375rem', cursor: 'pointer' }}
          >
            Hostel Rules
          </button>
          <a href="#contact" style={{ color: 'var(--text-secondary)', fontWeight: 500, fontSize: '0.9375rem' }}>Contact</a>
        </nav>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          {user ? (
            <>
              <Link
                to="/profile"
                className="btn btn-ghost btn-sm"
                style={{ display: 'flex', alignItems: 'center', gap: '0.375rem' }}
              >
                <UserIcon size={16} /> My Profile ({user.fullName?.split(' ')[0]})
              </Link>
              <Link
                to={user.role === 'ADMIN' ? '/admin/dashboard' : '/student/dashboard'}
                className="btn btn-primary btn-sm"
              >
                Dashboard <ArrowRight size={16} />
              </Link>
              <button
                onClick={() => logout()}
                className="btn btn-outline btn-sm"
                style={{ color: 'var(--danger)', borderColor: 'var(--danger)', padding: '6px 10px' }}
                title="Sign Out"
              >
                <LogOut size={16} />
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className="btn btn-ghost btn-sm">Sign In</Link>
              <Link to="/register" className="btn btn-primary btn-sm">Register <ArrowRight size={16} /></Link>
            </>
          )}
        </div>
      </header>

      <section
        style={{
          padding: '5rem 1.5rem 4rem',
          background: 'radial-gradient(ellipse at 50% -20%, rgba(37, 99, 235, 0.15), transparent 70%)',
          textAlign: 'center',
        }}
      >
        <div style={{ maxWidth: '840px', margin: '0 auto' }}>
          <div
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.5rem',
              padding: '0.375rem 0.875rem',
              borderRadius: 'var(--radius-full)',
              backgroundColor: 'var(--primary-light)',
              color: 'var(--primary)',
              fontSize: '0.8125rem',
              fontWeight: 700,
              marginBottom: '1.5rem',
            }}
          >
            <Sparkles size={14} /> Next-Generation Hostel Management Platform
          </div>
          <h1
            style={{
              fontSize: 'clamp(2.25rem, 5vw, 3.5rem)',
              fontWeight: 800,
              lineHeight: 1.15,
              marginBottom: '1.25rem',
              letterSpacing: '-0.03em',
            }}
          >
            Smart, Seamless & Modern <br />
            <span style={{ color: 'var(--primary)' }}>Hostel Living & Operations</span>
          </h1>
          <p
            style={{
              fontSize: 'clamp(1rem, 2vw, 1.25rem)',
              color: 'var(--text-secondary)',
              lineHeight: 1.6,
              maxWidth: '680px',
              margin: '0 auto 2.5rem',
            }}
          >
            Manage room allocations, instant digital fee receipts, 24/7 grievance redressal, and
            student welfare all in one unified, real-time platform.
          </p>

          <div style={{ display: 'flex', gap: '1rem', justifyContent: 'center', flexWrap: 'wrap' }}>
            {user ? (
              <>
                <Link
                  to={user.role === 'ADMIN' ? '/admin/dashboard' : '/student/dashboard'}
                  className="btn btn-primary btn-lg"
                >
                  Go to {user.role === 'ADMIN' ? 'Admin Dashboard' : 'Student Dashboard'} <ArrowRight size={18} />
                </Link>
                <Link
                  to="/profile"
                  className="btn btn-secondary btn-lg"
                  style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}
                >
                  <UserIcon size={18} /> View & Edit Profile
                </Link>
              </>
            ) : (
              <>
                <Link to="/register" className="btn btn-primary btn-lg">
                  Student Registration <ArrowRight size={18} />
                </Link>
                <Link to="/login" className="btn btn-secondary btn-lg">
                  Sign In to Account
                </Link>
              </>
            )}
          </div>
        </div>

        <div
          style={{
            maxWidth: '1000px',
            margin: '4rem auto 0',
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
            gap: '1.5rem',
          }}
        >
          <div className="card" style={{ padding: '1.25rem', textAlign: 'center' }}>
            <div style={{ fontSize: '2rem', fontWeight: 800, color: 'var(--primary)' }}>3 Blocks</div>
            <div style={{ fontSize: '0.875rem', color: 'var(--text-muted)', fontWeight: 600 }}>Hostel Wings A, B, C</div>
          </div>
          <div className="card" style={{ padding: '1.25rem', textAlign: 'center' }}>
            <div style={{ fontSize: '2rem', fontWeight: 800, color: 'var(--success)' }}>100%</div>
            <div style={{ fontSize: '0.875rem', color: 'var(--text-muted)', fontWeight: 600 }}>Digital Fee Receipts</div>
          </div>
          <div className="card" style={{ padding: '1.25rem', textAlign: 'center' }}>
            <div style={{ fontSize: '2rem', fontWeight: 800, color: 'var(--accent)' }}>24/7</div>
            <div style={{ fontSize: '0.875rem', color: 'var(--text-muted)', fontWeight: 600 }}>Grievance Redressal</div>
          </div>
          <div className="card" style={{ padding: '1.25rem', textAlign: 'center' }}>
            <div style={{ fontSize: '2rem', fontWeight: 800, color: 'var(--info)' }}>Instant</div>
            <div style={{ fontSize: '0.875rem', color: 'var(--text-muted)', fontWeight: 600 }}>Leave Approvals</div>
          </div>
        </div>
      </section>

      <section id="amenities" style={{ padding: '5rem 1.5rem', backgroundColor: 'var(--bg-app)' }}>
        <div style={{ maxWidth: '1200px', margin: '0 auto' }}>
          <div style={{ textAlign: 'center', marginBottom: '3.5rem' }}>
            <div style={{ fontSize: '0.8125rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.08em', color: 'var(--primary)', marginBottom: '0.5rem' }}>
              World-Class Facilities
            </div>
            <h2 style={{ fontSize: '2.25rem', fontWeight: 800, letterSpacing: '-0.02em' }}>
              Comfortable, Connected & Secure Living
            </h2>
            <p style={{ color: 'var(--text-muted)', marginTop: '0.5rem', fontSize: '1rem' }}>
              Hover or click on any facility card below to inspect details.
            </p>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: '1.5rem' }}>
            {amenities.map((item, idx) => (
              <FlipCard
                key={idx}
                frontIcon={item.icon}
                frontTitle={item.title}
                frontSubtitle={item.subtitle}
                frontBadge={item.badge}
                backContent={item.backContent}
                backActionText={item.actionText}
                onBackAction={() => setRulesOpen(true)}
              />
            ))}
          </div>
        </div>
      </section>

      <section id="rooms" style={{ padding: '5rem 1.5rem', backgroundColor: 'var(--bg-surface)' }}>
        <div style={{ maxWidth: '1200px', margin: '0 auto' }}>
          <div style={{ textAlign: 'center', marginBottom: '3.5rem' }}>
            <div style={{ fontSize: '0.8125rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.08em', color: 'var(--primary)', marginBottom: '0.5rem' }}>
              Accommodation Plans
            </div>
            <h2 style={{ fontSize: '2.25rem', fontWeight: 800, letterSpacing: '-0.02em' }}>
              Choose Your Room Type
            </h2>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(340px, 1fr))', gap: '2rem' }}>
            {roomTypes.map((room, idx) => (
              <div key={idx} className="card card-hover" style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
                <div style={{ borderBottom: '1px solid var(--border-color)', paddingBottom: '1.25rem', marginBottom: '1.25rem' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                    <h3 style={{ fontSize: '1.25rem' }}>{room.name}</h3>
                    <span className="badge badge-info">{room.capacity}</span>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'baseline', gap: '0.25rem' }}>
                    <span style={{ fontSize: '2rem', fontWeight: 800, color: 'var(--primary)' }}>{room.price}</span>
                    <span style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>/ month</span>
                  </div>
                  <p style={{ fontSize: '0.875rem', marginTop: '0.5rem' }}>{room.description}</p>
                </div>

                <div style={{ flex: 1 }}>
                  <div style={{ fontSize: '0.8125rem', fontWeight: 700, textTransform: 'uppercase', color: 'var(--text-muted)', marginBottom: '0.75rem' }}>
                    Included Features:
                  </div>
                  <ul style={{ listStyle: 'none', display: 'flex', flexDirection: 'column', gap: '0.625rem' }}>
                    {room.features.map((feat, fIdx) => (
                      <li key={fIdx} style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
                        <CheckCircle2 size={16} color="var(--success)" /> {feat}
                      </li>
                    ))}
                  </ul>
                </div>

                <div style={{ marginTop: '1.5rem', paddingTop: '1.25rem', borderTop: '1px solid var(--border-color)' }}>
                  <Link to="/register" className="btn btn-primary" style={{ width: '100%' }}>
                    Register to Book <ArrowRight size={16} />
                  </Link>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section id="features" style={{ padding: '5rem 1.5rem', backgroundColor: 'var(--bg-app)' }}>
        <div style={{ maxWidth: '1200px', margin: '0 auto' }}>
          <div style={{ textAlign: 'center', marginBottom: '3.5rem' }}>
            <div style={{ fontSize: '0.8125rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.08em', color: 'var(--primary)', marginBottom: '0.5rem' }}>
              Management System Highlights
            </div>
            <h2 style={{ fontSize: '2.25rem', fontWeight: 800, letterSpacing: '-0.02em' }}>
              Built for Modern Campus Administration
            </h2>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: '1.5rem' }}>
            <div className="card">
              <div style={{ color: 'var(--primary)', marginBottom: '0.75rem' }}><UserCheck size={28} /></div>
              <h3 style={{ fontSize: '1.125rem', marginBottom: '0.5rem' }}>Seamless Student Onboarding</h3>
              <p style={{ fontSize: '0.875rem' }}>Quick account registration, automatic role verification, and centralized records.</p>
            </div>
            <div className="card">
              <div style={{ color: 'var(--success)', marginBottom: '0.75rem' }}><CreditCard size={28} /></div>
              <h3 style={{ fontSize: '1.125rem', marginBottom: '0.5rem' }}>Transparent Rent & Payments</h3>
              <p style={{ fontSize: '0.875rem' }}>Digital receipts with unique transaction IDs and real-time payment history tracking.</p>
            </div>
            <div className="card">
              <div style={{ color: 'var(--warning)', marginBottom: '0.75rem' }}><MessageSquare size={28} /></div>
              <h3 style={{ fontSize: '1.125rem', marginBottom: '0.5rem' }}>Grievance Ticketing System</h3>
              <p style={{ fontSize: '0.875rem' }}>Categorized complaint submission with status workflow from Pending to Resolved.</p>
            </div>
            <div className="card">
              <div style={{ color: 'var(--accent)', marginBottom: '0.75rem' }}><CalendarCheck size={28} /></div>
              <h3 style={{ fontSize: '1.125rem', marginBottom: '0.5rem' }}>Digital Leave Applications</h3>
              <p style={{ fontSize: '0.875rem' }}>Students apply online; wardens review and approve with date validations and remarks.</p>
            </div>
          </div>
        </div>
      </section>

      <section id="contact" style={{ padding: '5rem 1.5rem', backgroundColor: 'var(--bg-surface)' }}>
        <div style={{ maxWidth: '1000px', margin: '0 auto' }}>
          <div style={{ textAlign: 'center', marginBottom: '3rem' }}>
            <h2 style={{ fontSize: '2rem', fontWeight: 800 }}>Get In Touch with Hostel Office</h2>
            <p style={{ color: 'var(--text-muted)', marginTop: '0.5rem' }}>
              Have questions regarding admissions or facilities? Contact our administration.
            </p>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '2rem' }}>
            <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                <div style={{ padding: '0.75rem', borderRadius: 'var(--radius-md)', backgroundColor: 'var(--primary-light)', color: 'var(--primary)' }}>
                  <MapPin size={22} />
                </div>
                <div>
                  <div style={{ fontWeight: 700, fontSize: '0.9375rem' }}>Campus Address</div>
                  <div style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>Hostel Block A, Main Campus Avenue</div>
                </div>
              </div>

              <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                <div style={{ padding: '0.75rem', borderRadius: 'var(--radius-md)', backgroundColor: 'var(--success-light)', color: 'var(--success)' }}>
                  <Phone size={22} />
                </div>
                <div>
                  <div style={{ fontWeight: 700, fontSize: '0.9375rem' }}>Administration Phone</div>
                  <div style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>+91 99999 99999 / +91 98765 43210</div>
                </div>
              </div>

              <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                <div style={{ padding: '0.75rem', borderRadius: 'var(--radius-md)', backgroundColor: 'var(--info-light)', color: 'var(--info)' }}>
                  <Mail size={22} />
                </div>
                <div>
                  <div style={{ fontWeight: 700, fontSize: '0.9375rem' }}>Hostel Email</div>
                  <div style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>admin@hostel.com</div>
                </div>
              </div>
            </div>

            <div className="card">
              {contactSent ? (
                <div style={{ textAlign: 'center', padding: '2rem 1rem' }}>
                  <CheckCircle2 size={48} color="var(--success)" style={{ margin: '0 auto 1rem' }} />
                  <h3 style={{ fontSize: '1.125rem' }}>Inquiry Submitted!</h3>
                  <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', marginTop: '0.5rem' }}>
                    Thank you. Our administration desk will review your message promptly.
                  </p>
                </div>
              ) : (
                <form
                  onSubmit={(e) => {
                    e.preventDefault();
                    setContactSent(true);
                  }}
                  style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}
                >
                  <div>
                    <label className="form-label">Your Name</label>
                    <input className="form-input" required placeholder="e.g. Rahul Sharma" />
                  </div>
                  <div>
                    <label className="form-label">Email or Mobile</label>
                    <input className="form-input" required placeholder="e.g. rahul@example.com" />
                  </div>
                  <div>
                    <label className="form-label">Message / Inquiry</label>
                    <textarea className="form-textarea" rows={3} required placeholder="Ask about room availability, fees, or visit timings..." />
                  </div>
                  <Button type="submit" variant="primary">
                    Send Inquiry
                  </Button>
                </form>
              )}
            </div>
          </div>
        </div>
      </section>

      <Modal
        isOpen={rulesOpen}
        onClose={() => setRulesOpen(false)}
        title="Hostel Rules & Code of Conduct"
        maxWidth="600px"
        footer={
          <Button variant="primary" onClick={() => setRulesOpen(false)}>
            I Understand
          </Button>
        }
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', fontSize: '0.9375rem', lineHeight: 1.6 }}>
          <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'flex-start' }}>
            <CheckCircle2 size={18} color="var(--primary)" style={{ flexShrink: 0, marginTop: '3px' }} />
            <div><strong>Entry & Exit Timings:</strong> Gates close at 10:00 PM. Night outs require approved leave requests.</div>
          </div>
          <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'flex-start' }}>
            <CheckCircle2 size={18} color="var(--primary)" style={{ flexShrink: 0, marginTop: '3px' }} />
            <div><strong>Silence Hours:</strong> Quiet hours between 11:00 PM and 6:00 AM daily.</div>
          </div>
          <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'flex-start' }}>
            <CheckCircle2 size={18} color="var(--primary)" style={{ flexShrink: 0, marginTop: '3px' }} />
            <div><strong>Cleanliness:</strong> Rooms must be kept clean. Maintenance tickets can be raised anytime.</div>
          </div>
        </div>
      </Modal>

      <Footer />
    </div>
  );
};
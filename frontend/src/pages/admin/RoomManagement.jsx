import React, { useState, useEffect } from 'react';
import api from '../../services/api';
import { useToast } from '../../context/ToastContext';
import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import { Select } from '../../components/common/Select';
import { Modal } from '../../components/common/Modal';
import { Badge } from '../../components/common/Badge';
import { ConfirmDialog } from '../../components/common/ConfirmDialog';
import { LoadingSpinner } from '../../components/feedback/LoadingSpinner';
import { EmptyState } from '../../components/feedback/EmptyState';
import { RoomDetailsModal } from '../../components/admin/RoomDetailsModal';
import { StudentDetailsModal } from '../../components/admin/StudentDetailsModal';
import {
  Plus,
  Search,
  Filter,
  Trash2,
  Edit,
  Bed,
  LayoutGrid,
  List,
  Eye,
  Users,
  CheckCircle2,
  Wrench,
  Layers,
  ArrowRight,
  AlertTriangle
} from 'lucide-react';

export const RoomManagement = ({ isWarden = false }) => {
  const { success: toastSuccess, error: toastError } = useToast();
  const [rooms, setRooms] = useState([]);
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [filterType, setFilterType] = useState('ALL');
  const [filterOccupancy, setFilterOccupancy] = useState('ALL');
  const [filterFloor, setFilterFloor] = useState('ALL');
  const [viewMode, setViewMode] = useState('grid'); // 'grid' or 'table'

  // Modal State for Add / Edit
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingRoom, setEditingRoom] = useState(null);
  const [formData, setFormData] = useState({
    roomNumber: '',
    roomType: 'SINGLE',
    capacity: 1,
    pricePerMonth: 6000,
    status: 'AVAILABLE',
  });
  const [modalLoading, setModalLoading] = useState(false);

  // Delete Confirm State
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleteLoading, setDeleteLoading] = useState(false);

  // Room Details & Student Details Modals
  const [selectedRoomDetails, setSelectedRoomDetails] = useState(null);
  const [selectedStudentDetails, setSelectedStudentDetails] = useState(null);

  const fetchRooms = async () => {
    setLoading(true);
    try {
      const [roomsRes, bookingsRes] = await Promise.allSettled([
        api.get('/rooms'),
        api.get('/bookings'),
      ]);

      if (roomsRes.status === 'fulfilled') {
        setRooms(roomsRes.value.data || []);
      } else {
        toastError(roomsRes.reason?.message || 'Failed to fetch rooms');
      }

      if (bookingsRes.status === 'fulfilled') {
        setBookings(bookingsRes.value.data || []);
      }
    } catch (err) {
      toastError(err.message || 'Failed to fetch room catalog');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRooms();
  }, []);

  const handleOpenAdd = () => {
    if (isWarden) return;
    setEditingRoom(null);
    setFormData({
      roomNumber: '',
      roomType: 'SINGLE',
      capacity: 1,
      pricePerMonth: 6000,
      status: 'AVAILABLE',
    });
    setIsModalOpen(true);
  };

  const handleOpenEdit = (room, e) => {
    if (e) e.stopPropagation();
    if (isWarden) return;
    setEditingRoom(room);
    setFormData({
      roomNumber: room.roomNumber,
      roomType: room.roomType,
      capacity: room.capacity,
      pricePerMonth: room.pricePerMonth,
      status: room.status,
    });
    setIsModalOpen(true);
  };

  const handleOpenDelete = (room, e) => {
    if (e) e.stopPropagation();
    if (isWarden) return;
    setDeleteTarget(room);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (isWarden) return;
    setModalLoading(true);
    try {
      if (editingRoom) {
        await api.put(`/rooms/${editingRoom.id}`, formData);
        toastSuccess(`Room ${formData.roomNumber} updated successfully.`);
      } else {
        await api.post('/rooms', formData);
        toastSuccess(`Room ${formData.roomNumber} created successfully.`);
      }
      setIsModalOpen(false);
      fetchRooms();
    } catch (err) {
      toastError(err.response?.data?.message || err.message || 'Operation failed');
    } finally {
      setModalLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget || isWarden) return;

    if (deleteTarget.occupied > 0) {
      toastError(
        `Cannot delete Room ${deleteTarget.roomNumber} because ${deleteTarget.occupied} ${
          deleteTarget.occupied === 1 ? 'student is' : 'students are'
        } currently assigned to this room.`
      );
      setDeleteTarget(null);
      return;
    }

    setDeleteLoading(true);
    try {
      await api.delete(`/rooms/${deleteTarget.id}`);
      toastSuccess(`Room ${deleteTarget.roomNumber} removed.`);
      setDeleteTarget(null);
      fetchRooms();
    } catch (err) {
      toastError(err.response?.data?.message || err.message || 'Failed to delete room');
    } finally {
      setDeleteLoading(false);
    }
  };

  // Check if room has active occupants matching the search term
  const roomHasMatchingOccupant = (roomId, term) => {
    if (!term) return false;
    const lower = term.toLowerCase();
    return bookings.some(
      (b) =>
        b.room?.id === roomId &&
        b.status === 'APPROVED' &&
        ((b.student?.fullName || '').toLowerCase().includes(lower) ||
          (b.student?.username || '').toLowerCase().includes(lower) ||
          (b.student?.email || '').toLowerCase().includes(lower) ||
          String(b.student?.id || '').includes(lower) ||
          `stu${String(b.student?.id || '').padStart(3, '0')}`.includes(lower))
    );
  };

  // Filtered rooms logic
  const filteredRooms = rooms.filter((r) => {
    const num = r.roomNumber || '';
    const matchesSearch =
      num.toLowerCase().includes(search.toLowerCase()) ||
      roomHasMatchingOccupant(r.id, search);

    const matchesType = filterType === 'ALL' || r.roomType === filterType;

    // Floor matching: A-101 -> Floor 1, B-201 -> Floor 2
    const floor = num.includes('-') ? num.split('-')[1]?.[0] || '1' : '1';
    const matchesFloor = filterFloor === 'ALL' || floor === filterFloor;

    // Occupancy status matching
    let matchesOccupancy = true;
    const occ = r.occupied || 0;
    const cap = r.capacity || 1;

    if (filterOccupancy === 'AVAILABLE') {
      matchesOccupancy = r.status !== 'MAINTENANCE' && occ < cap;
    } else if (filterOccupancy === 'PARTIALLY_OCCUPIED') {
      matchesOccupancy = r.status !== 'MAINTENANCE' && occ > 0 && occ < cap;
    } else if (filterOccupancy === 'FULL') {
      matchesOccupancy = r.status !== 'MAINTENANCE' && occ >= cap;
    } else if (filterOccupancy === 'MAINTENANCE') {
      matchesOccupancy = r.status === 'MAINTENANCE';
    }

    return matchesSearch && matchesType && matchesFloor && matchesOccupancy;
  });

  // Calculate Summary Statistics from actual room data
  const totalRooms = rooms.length;
  const maintenanceRooms = rooms.filter((r) => r.status === 'MAINTENANCE').length;
  const fullRooms = rooms.filter((r) => r.status !== 'MAINTENANCE' && (r.occupied || 0) >= r.capacity).length;
  const occupiedRooms = rooms.filter((r) => r.status !== 'MAINTENANCE' && (r.occupied || 0) > 0).length;
  const availableRooms = rooms.filter((r) => r.status !== 'MAINTENANCE' && (r.occupied || 0) < r.capacity).length;

  const totalBeds = rooms.reduce((acc, r) => acc + (r.capacity || 0), 0);
  const occupiedBeds = rooms.reduce((acc, r) => acc + (r.occupied || 0), 0);
  const availableBeds = Math.max(0, totalBeds - occupiedBeds);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Page Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 800, margin: 0 }}>
            {isWarden ? 'Hostel Room Inventory & Occupancy' : 'Rooms & Occupancy Management'}
          </h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', marginTop: '4px' }}>
            {isWarden
              ? 'Inspect real-time room capacity, student bed allocations, and availability.'
              : 'Click any room to view assigned resident students, bed allocation, and details.'}
          </p>
        </div>
        {!isWarden && (
          <Button variant="primary" icon={Plus} onClick={handleOpenAdd}>
            Add New Room
          </Button>
        )}
      </div>

      {/* Summary Statistics Cards */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))',
          gap: '0.875rem',
        }}
      >
        <div className="card" style={{ padding: '0.875rem 1rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
              Total Rooms
            </span>
            <Layers size={16} style={{ color: 'var(--primary)' }} />
          </div>
          <div style={{ fontSize: '1.5rem', fontWeight: 800, marginTop: '4px', color: 'var(--text-primary)' }}>
            {totalRooms}
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '2px' }}>Inventory Count</div>
        </div>

        <div className="card" style={{ padding: '0.875rem 1rem', borderLeft: '3px solid var(--success)' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--success-text)', textTransform: 'uppercase' }}>
              Available
            </span>
            <CheckCircle2 size={16} style={{ color: 'var(--success)' }} />
          </div>
          <div style={{ fontSize: '1.5rem', fontWeight: 800, marginTop: '4px', color: 'var(--success)' }}>
            {availableRooms}
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '2px' }}>Ready for Booking</div>
        </div>

        <div className="card" style={{ padding: '0.875rem 1rem', borderLeft: '3px solid var(--warning)' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--warning-text)', textTransform: 'uppercase' }}>
              Occupied
            </span>
            <Users size={16} style={{ color: 'var(--warning)' }} />
          </div>
          <div style={{ fontSize: '1.5rem', fontWeight: 800, marginTop: '4px', color: 'var(--warning)' }}>
            {occupiedRooms}
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '2px' }}>Active Residents</div>
        </div>

        <div className="card" style={{ padding: '0.875rem 1rem', borderLeft: '3px solid var(--danger)' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--danger-text)', textTransform: 'uppercase' }}>
              Maintenance
            </span>
            <Wrench size={16} style={{ color: 'var(--danger)' }} />
          </div>
          <div style={{ fontSize: '1.5rem', fontWeight: 800, marginTop: '4px', color: 'var(--danger)' }}>
            {maintenanceRooms}
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '2px' }}>Unavailable</div>
        </div>

        <div className="card" style={{ padding: '0.875rem 1rem', backgroundColor: 'var(--bg-subtle)' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
              Total Beds
            </span>
            <Bed size={16} style={{ color: 'var(--text-muted)' }} />
          </div>
          <div style={{ fontSize: '1.5rem', fontWeight: 800, marginTop: '4px', color: 'var(--text-primary)' }}>
            {totalBeds}
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '2px' }}>Total Capacity</div>
        </div>

        <div className="card" style={{ padding: '0.875rem 1rem', backgroundColor: 'var(--primary-light)', borderColor: 'var(--primary-border)' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--primary)', textTransform: 'uppercase' }}>
              Occupied Beds
            </span>
            <Bed size={16} style={{ color: 'var(--primary)' }} />
          </div>
          <div style={{ fontSize: '1.5rem', fontWeight: 800, marginTop: '4px', color: 'var(--primary)' }}>
            {occupiedBeds}
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--primary)', marginTop: '2px' }}>Assigned Students</div>
        </div>

        <div className="card" style={{ padding: '0.875rem 1rem', backgroundColor: 'var(--success-light)', borderColor: 'var(--success-border)' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--success-text)', textTransform: 'uppercase' }}>
              Available Beds
            </span>
            <CheckCircle2 size={16} style={{ color: 'var(--success)' }} />
          </div>
          <div style={{ fontSize: '1.5rem', fontWeight: 800, marginTop: '4px', color: 'var(--success)' }}>
            {availableBeds}
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--success-text)', marginTop: '2px' }}>Vacant Beds</div>
        </div>
      </div>

      {/* Filter & Search Controls */}
      <div
        className="card"
        style={{
          padding: '1rem 1.25rem',
          display: 'flex',
          gap: '0.75rem',
          alignItems: 'center',
          flexWrap: 'wrap',
          justifyContent: 'space-between',
        }}
      >
        <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap', flex: 1, minWidth: '280px' }}>
          {/* Search by room number, student name, student ID */}
          <div style={{ position: 'relative', minWidth: '220px', flex: 1.5 }}>
            <Search
              size={16}
              style={{
                position: 'absolute',
                left: '12px',
                top: '50%',
                transform: 'translateY(-50%)',
                color: 'var(--text-muted)',
              }}
            />
            <input
              type="text"
              placeholder="Search room (A-101), student name, or ID..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="form-input"
              style={{ paddingLeft: '2.25rem' }}
            />
          </div>

          {/* Room Type Filter */}
          <select
            value={filterType}
            onChange={(e) => setFilterType(e.target.value)}
            className="form-select"
            style={{ width: 'auto', minWidth: '130px' }}
          >
            <option value="ALL">All Room Types</option>
            <option value="SINGLE">Single</option>
            <option value="DOUBLE">Double</option>
            <option value="TRIPLE">Triple</option>
            <option value="DORMITORY">Dormitory</option>
          </select>

          {/* Occupancy Status Filter */}
          <select
            value={filterOccupancy}
            onChange={(e) => setFilterOccupancy(e.target.value)}
            className="form-select"
            style={{ width: 'auto', minWidth: '160px' }}
          >
            <option value="ALL">All Occupancy</option>
            <option value="AVAILABLE">Available</option>
            <option value="PARTIALLY_OCCUPIED">Partially Occupied</option>
            <option value="FULL">Full</option>
            <option value="MAINTENANCE">Under Maintenance</option>
          </select>

          {/* Floor Filter */}
          <select
            value={filterFloor}
            onChange={(e) => setFilterFloor(e.target.value)}
            className="form-select"
            style={{ width: 'auto', minWidth: '120px' }}
          >
            <option value="ALL">All Floors</option>
            <option value="1">Floor 1</option>
            <option value="2">Floor 2</option>
            <option value="3">Floor 3</option>
            <option value="4">Floor 4</option>
          </select>
        </div>

        {/* View Mode Toggle */}
        <div
          style={{
            display: 'flex',
            gap: '0.25rem',
            backgroundColor: 'var(--bg-subtle)',
            padding: '3px',
            borderRadius: 'var(--radius-md)',
          }}
        >
          <button
            className={`btn btn-sm ${viewMode === 'grid' ? 'btn-primary' : 'btn-ghost'}`}
            style={{ padding: '6px 10px' }}
            onClick={() => setViewMode('grid')}
            title="Grid View"
          >
            <LayoutGrid size={16} />
          </button>
          <button
            className={`btn btn-sm ${viewMode === 'table' ? 'btn-primary' : 'btn-ghost'}`}
            style={{ padding: '6px 10px' }}
            onClick={() => setViewMode('table')}
            title="Table View"
          >
            <List size={16} />
          </button>
        </div>
      </div>

      {/* Rooms Content */}
      {loading ? (
        <LoadingSpinner text="Loading room catalog and bed occupancy..." />
      ) : filteredRooms.length === 0 ? (
        <EmptyState
          icon={Bed}
          title="No rooms match your filter"
          description="Try adjusting your search query, floor, or occupancy filters."
          actionText={!isWarden ? 'Add Room' : undefined}
          onAction={!isWarden ? handleOpenAdd : undefined}
        />
      ) : viewMode === 'grid' ? (
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill, minmax(290px, 1fr))',
            gap: '1.25rem',
          }}
        >
          {filteredRooms.map((room) => {
            const occ = room.occupied || 0;
            const cap = room.capacity || 1;
            const percentage = Math.min(100, Math.round((occ / cap) * 100));
            const floor = room.roomNumber.split('-')[1]?.[0] || '1';

            return (
              <div
                key={room.id}
                className="card card-hover"
                style={{
                  display: 'flex',
                  flexDirection: 'column',
                  justifyContent: 'space-between',
                  cursor: 'pointer',
                  border: '1px solid var(--border-color)',
                  position: 'relative',
                  transition: 'all 200ms ease',
                }}
                onClick={() => setSelectedRoomDetails(room)}
              >
                <div>
                  {/* Card Header */}
                  <div
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      marginBottom: '0.75rem',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                      <span
                        style={{
                          fontSize: '1.25rem',
                          fontWeight: 800,
                          color: 'var(--text-primary)',
                        }}
                      >
                        Room {room.roomNumber}
                      </span>
                    </div>
                    <Badge status={room.status} />
                  </div>

                  {/* Subtitle */}
                  <div
                    style={{
                      fontSize: '0.875rem',
                      color: 'var(--text-secondary)',
                      marginBottom: '0.875rem',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                    }}
                  >
                    <span>
                      <strong>{room.roomType}</strong> · Floor {floor}
                    </span>
                    <span
                      style={{
                        fontSize: '0.75rem',
                        color: 'var(--primary)',
                        fontWeight: 600,
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: '2px',
                      }}
                    >
                      View Details <ArrowRight size={12} />
                    </span>
                  </div>

                  {/* Occupancy Progress Bar */}
                  <div style={{ margin: '0.875rem 0' }}>
                    <div
                      style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        fontSize: '0.8125rem',
                        color: 'var(--text-muted)',
                        marginBottom: '0.375rem',
                      }}
                    >
                      <span>
                        Occupancy: <strong>{occ}/{cap}</strong> Beds
                      </span>
                      <span>{percentage}%</span>
                    </div>
                    <div
                      style={{
                        height: '6px',
                        backgroundColor: 'var(--bg-subtle)',
                        borderRadius: '3px',
                        overflow: 'hidden',
                      }}
                    >
                      <div
                        style={{
                          width: `${percentage}%`,
                          height: '100%',
                          backgroundColor:
                            room.status === 'MAINTENANCE'
                              ? 'var(--danger)'
                              : occ >= cap
                              ? 'var(--warning)'
                              : 'var(--primary)',
                          transition: 'width 250ms ease',
                        }}
                      />
                    </div>
                  </div>

                  {/* Rent */}
                  <div
                    style={{
                      fontSize: '1.25rem',
                      fontWeight: 800,
                      color: 'var(--primary)',
                      marginBottom: '0.75rem',
                    }}
                  >
                    ₹{room.pricePerMonth?.toLocaleString('en-IN')}{' '}
                    <span style={{ fontSize: '0.8125rem', fontWeight: 400, color: 'var(--text-muted)' }}>
                      / month
                    </span>
                  </div>
                </div>

                {/* Actions Row */}
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '0.5rem',
                    paddingTop: '0.75rem',
                    borderTop: '1px solid var(--border-color)',
                  }}
                  onClick={(e) => e.stopPropagation()}
                >
                  <Button
                    variant="outline"
                    size="sm"
                    icon={Eye}
                    onClick={() => setSelectedRoomDetails(room)}
                    style={{ flex: 1 }}
                  >
                    Details
                  </Button>
                  {!isWarden && (
                    <>
                      <Button
                        variant="secondary"
                        size="sm"
                        icon={Edit}
                        onClick={(e) => handleOpenEdit(room, e)}
                        title="Edit Room Configuration"
                      >
                        Edit
                      </Button>
                      <Button
                        variant="outlineDanger"
                        size="sm"
                        icon={Trash2}
                        onClick={(e) => handleOpenDelete(room, e)}
                        title="Delete Room"
                      >
                        Delete
                      </Button>
                    </>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      ) : (
        <div className="table-container">
          <table className="table">
            <thead>
              <tr>
                <th>Room</th>
                <th>Type</th>
                <th>Floor</th>
                <th>Capacity</th>
                <th>Occupied</th>
                <th>Available</th>
                <th>Monthly Rent</th>
                <th>Status</th>
                <th style={{ textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredRooms.map((room) => {
                const occ = room.occupied || 0;
                const cap = room.capacity || 1;
                const available = Math.max(0, cap - occ);
                const floor = room.roomNumber.split('-')[1]?.[0] || '1';

                return (
                  <tr
                    key={room.id}
                    style={{ cursor: 'pointer' }}
                    onClick={() => setSelectedRoomDetails(room)}
                  >
                    <td style={{ fontWeight: 700, color: 'var(--primary)' }}>
                      Room {room.roomNumber}
                    </td>
                    <td>{room.roomType}</td>
                    <td>Floor {floor}</td>
                    <td>{cap} Beds</td>
                    <td>
                      <span
                        style={{
                          fontWeight: 600,
                          color: occ >= cap ? 'var(--warning)' : 'var(--text-primary)',
                        }}
                      >
                        {occ} Beds
                      </span>
                    </td>
                    <td>
                      <span
                        style={{
                          fontWeight: 600,
                          color: available > 0 ? 'var(--success)' : 'var(--danger)',
                        }}
                      >
                        {available} Beds
                      </span>
                    </td>
                    <td style={{ fontWeight: 600 }}>₹{room.pricePerMonth?.toLocaleString('en-IN')}</td>
                    <td>
                      <Badge status={room.status} />
                    </td>
                    <td style={{ textAlign: 'right' }} onClick={(e) => e.stopPropagation()}>
                      <div style={{ display: 'inline-flex', gap: '0.375rem' }}>
                        <button
                          className="btn btn-ghost btn-sm"
                          onClick={() => setSelectedRoomDetails(room)}
                          title="View Room & Occupants Details"
                        >
                          <Eye size={16} />
                        </button>
                        {!isWarden && (
                          <>
                            <button
                              className="btn btn-ghost btn-sm"
                              onClick={(e) => handleOpenEdit(room, e)}
                              title="Edit Room"
                            >
                              <Edit size={16} />
                            </button>
                            <button
                              className="btn btn-ghost btn-sm"
                              style={{ color: 'var(--danger)' }}
                              onClick={(e) => handleOpenDelete(room, e)}
                              title="Delete Room"
                            >
                              <Trash2 size={16} />
                            </button>
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {/* Room Details Modal */}
      <RoomDetailsModal
        isOpen={!!selectedRoomDetails}
        onClose={() => setSelectedRoomDetails(null)}
        room={selectedRoomDetails}
        isWarden={isWarden}
        onViewStudent={(student) => {
          setSelectedStudentDetails(student);
        }}
      />

      {/* Student Details Modal */}
      <StudentDetailsModal
        isOpen={!!selectedStudentDetails}
        onClose={() => setSelectedStudentDetails(null)}
        student={selectedStudentDetails}
      />

      {/* Add / Edit Room Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={editingRoom ? `Edit Room ${editingRoom.roomNumber}` : 'Add New Room'}
      >
        <form onSubmit={handleSubmit}>
          <Input
            label="Room Number"
            id="roomNumber"
            value={formData.roomNumber}
            onChange={(e) => setFormData({ ...formData, roomNumber: e.target.value.toUpperCase() })}
            placeholder="e.g. A-101, B-204"
            disabled={!!editingRoom}
            required
          />

          <Select
            label="Room Type"
            id="roomType"
            value={formData.roomType}
            onChange={(e) => setFormData({ ...formData, roomType: e.target.value })}
            options={[
              { value: 'SINGLE', label: 'Single (1 Bed)' },
              { value: 'DOUBLE', label: 'Double (2 Beds)' },
              { value: 'TRIPLE', label: 'Triple (3 Beds)' },
              { value: 'DORMITORY', label: 'Dormitory (4+ Beds)' },
            ]}
          />

          <Input
            label="Total Bed Capacity"
            id="capacity"
            type="number"
            min={1}
            max={10}
            value={formData.capacity}
            onChange={(e) => setFormData({ ...formData, capacity: parseInt(e.target.value, 10) || 1 })}
            required
          />

          <Input
            label="Monthly Rent (₹)"
            id="pricePerMonth"
            type="number"
            min={0}
            step={100}
            value={formData.pricePerMonth}
            onChange={(e) => setFormData({ ...formData, pricePerMonth: parseFloat(e.target.value) || 0 })}
            required
          />

          <Select
            label="Room Status"
            id="status"
            value={formData.status}
            onChange={(e) => setFormData({ ...formData, status: e.target.value })}
            options={[
              { value: 'AVAILABLE', label: 'Available' },
              { value: 'FULL', label: 'Full / Occupied' },
              { value: 'MAINTENANCE', label: 'Under Maintenance' },
            ]}
          />

          <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end', marginTop: '1.5rem' }}>
            <Button type="button" variant="secondary" onClick={() => setIsModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" variant="primary" loading={modalLoading}>
              {editingRoom ? 'Update Room' : 'Create Room'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Delete Confirmation Dialog */}
      <ConfirmDialog
        isOpen={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
        title="Delete Room"
        message={
          deleteTarget?.occupied > 0
            ? `Cannot delete Room ${deleteTarget?.roomNumber} because ${deleteTarget?.occupied} student${
                deleteTarget?.occupied === 1 ? ' is' : 's are'
              } currently assigned to this room. Please reassign or check out all resident students before deleting.`
            : `Are you sure you want to delete Room ${deleteTarget?.roomNumber}? This cannot be undone.`
        }
        confirmText={deleteTarget?.occupied > 0 ? 'Understood' : 'Delete Room'}
        danger={!(deleteTarget?.occupied > 0)}
        loading={deleteLoading}
      />
    </div>
  );
};
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
import { Plus, Search, Filter, Trash2, Edit, Bed, LayoutGrid, List } from 'lucide-react';

export const RoomManagement = ({ isWarden = false }) => {
  const { success: toastSuccess, error: toastError } = useToast();
  const [rooms, setRooms] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [filterType, setFilterType] = useState('ALL');
  const [filterStatus, setFilterStatus] = useState('ALL');
  const [viewMode, setViewMode] = useState('grid'); // 'grid' or 'table'

  // Modal State
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

  const fetchRooms = async () => {
    setLoading(true);
    try {
      const res = await api.get('/rooms');
      setRooms(res.data);
    } catch (err) {
      toastError(err.message || 'Failed to fetch rooms');
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

  const handleOpenEdit = (room) => {
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
      toastError(err.message || 'Operation failed');
    } finally {
      setModalLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget || isWarden) return;
    setDeleteLoading(true);
    try {
      await api.delete(`/rooms/${deleteTarget.id}`);
      toastSuccess(`Room ${deleteTarget.roomNumber} removed.`);
      setDeleteTarget(null);
      fetchRooms();
    } catch (err) {
      toastError(err.message || 'Failed to delete room');
    } finally {
      setDeleteLoading(false);
    }
  };

  const filteredRooms = rooms.filter((r) => {
    const num = r.roomNumber || '';
    const matchesSearch = num.toLowerCase().includes(search.toLowerCase());
    const matchesType = filterType === 'ALL' || r.roomType === filterType;
    const matchesStatus = filterStatus === 'ALL' || r.status === filterStatus;
    return matchesSearch && matchesType && matchesStatus;
  });

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 800 }}>{isWarden ? 'Hostel Room Inventory & Occupancy' : 'Rooms & Occupancy Management'}</h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>
            {isWarden ? 'Monitor real-time room capacity, bed allocations, and availability.' : 'Configure room inventory, set monthly rents, and track occupancy levels.'}
          </p>
        </div>
        {!isWarden && (
          <Button variant="primary" icon={Plus} onClick={handleOpenAdd}>
            Add New Room
          </Button>
        )}
      </div>

      {/* Filter & Search Bar */}
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
        <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap', flex: 1, minWidth: '280px' }}>
          <div style={{ position: 'relative', minWidth: '220px', flex: 1 }}>
            <Search size={16} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
            <input
              type="text"
              placeholder="Search by room number (e.g. A-101)..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="form-input"
              style={{ paddingLeft: '2.25rem' }}
            />
          </div>

          <select
            value={filterType}
            onChange={(e) => setFilterType(e.target.value)}
            className="form-select"
            style={{ width: 'auto', minWidth: '140px' }}
          >
            <option value="ALL">All Types</option>
            <option value="SINGLE">Single</option>
            <option value="DOUBLE">Double</option>
            <option value="TRIPLE">Triple</option>
            <option value="DORMITORY">Dormitory</option>
          </select>

          <select
            value={filterStatus}
            onChange={(e) => setFilterStatus(e.target.value)}
            className="form-select"
            style={{ width: 'auto', minWidth: '150px' }}
          >
            <option value="ALL">All Statuses</option>
            <option value="AVAILABLE">Available</option>
            <option value="FULL">Fully Occupied</option>
            <option value="MAINTENANCE">Under Maintenance</option>
          </select>
        </div>

        <div style={{ display: 'flex', gap: '0.25rem', backgroundColor: 'var(--bg-subtle)', padding: '3px', borderRadius: 'var(--radius-md)' }}>
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

      {/* Content */}
      {loading ? (
        <LoadingSpinner text="Loading room catalog..." />
      ) : filteredRooms.length === 0 ? (
        <EmptyState
          icon={Bed}
          title="No rooms match your filter"
          description="Try adjusting your search criteria or create a new room."
          actionText="Add Room"
          onAction={handleOpenAdd}
        />
      ) : viewMode === 'grid' ? (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: '1.25rem' }}>
          {filteredRooms.map((room) => (
            <div key={room.id} className="card card-hover" style={{ display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
                  <span style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--text-primary)' }}>
                    Room {room.roomNumber}
                  </span>
                  <Badge status={room.status} />
                </div>
                <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', marginBottom: '0.75rem' }}>
                  <strong>{room.roomType}</strong> · Floor {room.roomNumber.split('-')[1]?.[0] || '1'}
                </div>

                <div style={{ margin: '1rem 0' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8125rem', color: 'var(--text-muted)', marginBottom: '0.375rem' }}>
                    <span>Occupancy: {room.occupied}/{room.capacity} Beds</span>
                    <span>{Math.round(((room.occupied || 0) / room.capacity) * 100)}%</span>
                  </div>
                  <div style={{ height: '6px', backgroundColor: 'var(--bg-subtle)', borderRadius: '3px', overflow: 'hidden' }}>
                    <div
                      style={{
                        width: `${((room.occupied || 0) / room.capacity) * 100}%`,
                        height: '100%',
                        backgroundColor: room.occupied >= room.capacity ? 'var(--warning)' : 'var(--primary)',
                      }}
                    />
                  </div>
                </div>

                <div style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--primary)', marginBottom: '1rem' }}>
                  ₹{room.pricePerMonth?.toLocaleString('en-IN')}{' '}
                  <span style={{ fontSize: '0.8125rem', fontWeight: 400, color: 'var(--text-muted)' }}>/ month</span>
                </div>
              </div>

              {!isWarden && (
                <div style={{ display: 'flex', gap: '0.5rem', paddingTop: '0.75rem', borderTop: '1px solid var(--border-color)' }}>
                  <Button variant="secondary" size="sm" icon={Edit} onClick={() => handleOpenEdit(room)} style={{ flex: 1 }}>
                    Edit
                  </Button>
                  <Button variant="outlineDanger" size="sm" icon={Trash2} onClick={() => setDeleteTarget(room)}>
                    Delete
                  </Button>
                </div>
              )}
            </div>
          ))}
        </div>
      ) : (
        <div className="table-container">
          <table className="table">
            <thead>
              <tr>
                <th>Room Number</th>
                <th>Type</th>
                <th>Capacity</th>
                <th>Occupied</th>
                <th>Monthly Rent</th>
                <th>Status</th>
                {!isWarden && <th style={{ textAlign: 'right' }}>Actions</th>}
              </tr>
            </thead>
            <tbody>
              {filteredRooms.map((room) => (
                <tr key={room.id}>
                  <td style={{ fontWeight: 700 }}>Room {room.roomNumber}</td>
                  <td>{room.roomType}</td>
                  <td>{room.capacity} Beds</td>
                  <td>{room.occupied || 0} Beds</td>
                  <td style={{ fontWeight: 600 }}>₹{room.pricePerMonth?.toLocaleString('en-IN')}</td>
                  <td><Badge status={room.status} /></td>
                  {!isWarden && (
                    <td style={{ textAlign: 'right' }}>
                      <div style={{ display: 'inline-flex', gap: '0.5rem' }}>
                        <button className="btn btn-ghost btn-sm" onClick={() => handleOpenEdit(room)}>
                          <Edit size={16} />
                        </button>
                        <button className="btn btn-ghost btn-sm" style={{ color: 'var(--danger)' }} onClick={() => setDeleteTarget(room)}>
                          <Trash2 size={16} />
                        </button>
                      </div>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

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

      {/* Delete Confirmation */}
      <ConfirmDialog
        isOpen={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
        title="Delete Room"
        message={`Are you sure you want to delete Room ${deleteTarget?.roomNumber}? This cannot be undone.`}
        confirmText="Delete Room"
        danger
        loading={deleteLoading}
      />
    </div>
  );
};
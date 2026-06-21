const express = require('express');
const router = express.Router();

// GET /api/v1/alerts/:groupId
router.get('/:groupId', async (req, res) => {
  try {
    const { groupId } = req.params;
    const { limit = 50, offset = 0 } = req.query;
    const pool = req.app.locals.pool;

    const result = await pool.query(
      `SELECT sa.*, d.device_name 
       FROM sos_alerts sa
       JOIN devices d ON sa.device_id = d.device_id
       WHERE d.group_id = $1
       ORDER BY sa.created_at DESC
       LIMIT $2 OFFSET $3`,
      [groupId, limit, offset]
    );

    res.json({
      success: true,
      data: result.rows,
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to get alerts',
    });
  }
});

// PUT /api/v1/alerts/:alertId/acknowledge
router.put('/:alertId/acknowledge', async (req, res) => {
  try {
    const { alertId } = req.params;
    const { acknowledgedBy } = req.body;
    const pool = req.app.locals.pool;

    await pool.query(
      `UPDATE sos_alerts 
       SET status = 'ACKNOWLEDGED', acknowledged_by = $1, acknowledged_at = NOW()
       WHERE alert_id = $2`,
      [acknowledgedBy, alertId]
    );

    res.json({
      success: true,
      message: 'Alert acknowledged',
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to acknowledge alert',
    });
  }
});

// PUT /api/v1/alerts/:alertId/cancel
router.put('/:alertId/cancel', async (req, res) => {
  try {
    const { alertId } = req.params;
    const pool = req.app.locals.pool;

    await pool.query(
      `UPDATE sos_alerts SET status = 'CANCELLED' WHERE alert_id = $1`,
      [alertId]
    );

    res.json({
      success: true,
      message: 'Alert cancelled',
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to cancel alert',
    });
  }
});

// POST /api/v1/alerts/safe-zones
router.post('/safe-zones', async (req, res) => {
  try {
    const { groupId, name, centerLatitude, centerLongitude, radiusMeters, notifyOnEntry, notifyOnExit } = req.body;
    const pool = req.app.locals.pool;
    const { v4: uuidv4 } = require('uuid');

    const zoneId = uuidv4();

    const result = await pool.query(
      `INSERT INTO safe_zones (zone_id, group_id, name, center_latitude, center_longitude, radius_meters, is_active, notify_on_entry, notify_on_exit)
       VALUES ($1, $2, $3, $4, $5, $6, true, $7, $8)
       RETURNING *`,
      [zoneId, groupId, name, centerLatitude, centerLongitude, radiusMeters, notifyOnEntry, notifyOnExit]
    );

    res.status(201).json({
      success: true,
      message: 'Safe zone created',
      data: result.rows[0],
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to create safe zone',
    });
  }
});

// GET /api/v1/alerts/safe-zones/:groupId
router.get('/safe-zones/:groupId', async (req, res) => {
  try {
    const { groupId } = req.params;
    const pool = req.app.locals.pool;

    const result = await pool.query(
      `SELECT * FROM safe_zones WHERE group_id = $1 AND is_active = true`,
      [groupId]
    );

    res.json({
      success: true,
      data: result.rows,
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to get safe zones',
    });
  }
});

module.exports = router;

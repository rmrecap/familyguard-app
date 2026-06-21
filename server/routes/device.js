const express = require('express');
const router = express.Router();
const { v4: uuidv4 } = require('uuid');

// POST /api/v1/device/register
router.post('/register', async (req, res) => {
  try {
    const { deviceName, deviceModel, osVersion, fcmToken, role } = req.body;
    const pool = req.app.locals.pool;
    const logger = req.app.locals.logger;

    const deviceId = uuidv4();
    const now = new Date();

    // Insert device
    const result = await pool.query(
      `INSERT INTO devices (device_id, device_name, device_model, os_version, fcm_token, role, registered_at, last_seen_at)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
       RETURNING *`,
      [deviceId, deviceName, deviceModel, osVersion, fcmToken, role, now, now]
    );

    logger.info(`Device registered: ${deviceId} (${role})`);

    res.status(201).json({
      success: true,
      message: 'Device registered successfully',
      data: result.rows[0],
    });
  } catch (error) {
    req.app.locals.logger.error('Device registration error:', error);
    res.status(500).json({
      success: false,
      message: 'Failed to register device',
    });
  }
});

// POST /api/v1/device/heartbeat/:deviceId
router.post('/heartbeat/:deviceId', async (req, res) => {
  try {
    const { deviceId } = req.params;
    const pool = req.app.locals.pool;

    await pool.query(
      `UPDATE devices SET last_seen_at = NOW() WHERE device_id = $1`,
      [deviceId]
    );

    res.json({
      success: true,
      message: 'Heartbeat received',
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to update heartbeat',
    });
  }
});

// GET /api/v1/device/:deviceId
router.get('/:deviceId', async (req, res) => {
  try {
    const { deviceId } = req.params;
    const pool = req.app.locals.pool;

    const result = await pool.query(
      `SELECT * FROM devices WHERE device_id = $1`,
      [deviceId]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'Device not found',
      });
    }

    res.json({
      success: true,
      data: result.rows[0],
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to get device',
    });
  }
});

module.exports = router;

const express = require('express');
const router = express.Router();
const { v4: uuidv4 } = require('uuid');

// POST /api/v1/family/create
router.post('/create', async (req, res) => {
  try {
    const { deviceName, deviceModel, osVersion, fcmToken } = req.body;
    const pool = req.app.locals.pool;
    const logger = req.app.locals.logger;

    const groupId = uuidv4();
    const inviteCode = Math.random().toString(36).substring(2, 8).toUpperCase();
    const now = new Date();

    // Create family group
    const groupResult = await pool.query(
      `INSERT INTO family_groups (group_id, invite_code, created_at)
       VALUES ($1, $2, $3)
       RETURNING *`,
      [groupId, inviteCode, now]
    );

    // Register parent device
    const deviceId = uuidv4();
    await pool.query(
      `INSERT INTO devices (device_id, device_name, device_model, os_version, fcm_token, role, group_id, registered_at, last_seen_at)
       VALUES ($1, $2, $3, $4, $5, 'PARENT', $6, $7, $8)`,
      [deviceId, deviceName, deviceModel, osVersion, fcmToken, groupId, now, now]
    );

    logger.info(`Family group created: ${groupId} with code ${inviteCode}`);

    res.status(201).json({
      success: true,
      message: 'Family group created',
      data: {
        groupId,
        deviceId,
        inviteCode,
      },
    });
  } catch (error) {
    req.app.locals.logger.error('Family creation error:', error);
    res.status(500).json({
      success: false,
      message: 'Failed to create family group',
    });
  }
});

// POST /api/v1/family/join
router.post('/join', async (req, res) => {
  try {
    const { inviteCode, deviceId } = req.body;
    const pool = req.app.locals.pool;
    const logger = req.app.locals.logger;

    // Find group by invite code
    const groupResult = await pool.query(
      `SELECT * FROM family_groups WHERE invite_code = $1`,
      [inviteCode]
    );

    if (groupResult.rows.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'Invalid invite code',
      });
    }

    const group = groupResult.rows[0];

    // Add device to group as CHILD
    await pool.query(
      `UPDATE devices SET group_id = $1, role = 'CHILD' WHERE device_id = $2`,
      [group.group_id, deviceId]
    );

    logger.info(`Device ${deviceId} joined family group ${group.group_id}`);

    res.json({
      success: true,
      message: 'Joined family group',
      data: {
        groupId: group.group_id,
      },
    });
  } catch (error) {
    req.app.locals.logger.error('Family join error:', error);
    res.status(500).json({
      success: false,
      message: 'Failed to join family group',
    });
  }
});

// GET /api/v1/family/members/:groupId
router.get('/members/:groupId', async (req, res) => {
  try {
    const { groupId } = req.params;
    const pool = req.app.locals.pool;

    const result = await pool.query(
      `SELECT device_id, device_name, role, last_seen_at 
       FROM devices WHERE group_id = $1`,
      [groupId]
    );

    res.json({
      success: true,
      data: result.rows,
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to get family members',
    });
  }
});

module.exports = router;

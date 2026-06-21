const express = require('express');
const router = express.Router();

// POST /api/v1/sync/location
router.post('/location', async (req, res) => {
  try {
    const { deviceId, payload, timestamp, signature } = req.body;
    const pool = req.app.locals.pool;
    const logger = req.app.locals.logger;

    // Store location update
    await pool.query(
      `INSERT INTO location_updates (device_id, payload, timestamp, signature, synced_at)
       VALUES ($1, $2, $3, $4, NOW())`,
      [deviceId, payload, timestamp, signature]
    );

    // Update device last seen
    await pool.query(
      `UPDATE devices SET last_seen_at = NOW() WHERE device_id = $1`,
      [deviceId]
    );

    // Check if we need to trigger alerts (geofence, etc.)
    await checkGeofenceAlerts(pool, deviceId, payload, logger);

    res.json({
      success: true,
      message: 'Location synced',
    });
  } catch (error) {
    req.app.locals.logger.error('Location sync error:', error);
    res.status(500).json({
      success: false,
      message: 'Failed to sync location',
    });
  }
});

// POST /api/v1/sync/sos
router.post('/sos', async (req, res) => {
  try {
    const { deviceId, payload, timestamp, signature } = req.body;
    const pool = req.app.locals.pool;
    const logger = req.app.locals.logger;

    // Store SOS alert
    const alertId = require('uuid').v4();
    await pool.query(
      `INSERT INTO sos_alerts (alert_id, device_id, payload, timestamp, signature, status, created_at)
       VALUES ($1, $2, $3, $4, $5, 'TRIGGERED', NOW())`,
      [alertId, deviceId, payload, timestamp, signature]
    );

    // Get device's family group
    const deviceResult = await pool.query(
      `SELECT group_id FROM devices WHERE device_id = $1`,
      [deviceId]
    );

    if (deviceResult.rows.length > 0 && deviceResult.rows[0].group_id) {
      const groupId = deviceResult.rows[0].group_id;

      // Get parent devices in the group
      const parentsResult = await pool.query(
        `SELECT device_id, fcm_token FROM devices WHERE group_id = $1 AND role = 'PARENT'`,
        [groupId]
      );

      // Send push notifications to parents
      const admin = req.app.locals.admin;
      for (const parent of parentsResult.rows) {
        if (parent.fcm_token) {
          try {
            await admin.messaging().send({
              token: parent.fcm_token,
              notification: {
                title: '🚨 Emergency SOS Alert!',
                body: `Your child has triggered an emergency alert!`,
              },
              data: {
                type: 'SOS_ALERT',
                alertId,
                childDeviceId: deviceId,
              },
            });
          } catch (pushError) {
            logger.error(`Failed to send push to ${parent.device_id}:`, pushError);
          }
        }
      }
    }

    logger.info(`SOS alert created: ${alertId} from device ${deviceId}`);

    res.status(201).json({
      success: true,
      message: 'SOS alert received',
      data: { alertId },
    });
  } catch (error) {
    req.app.locals.logger.error('SOS sync error:', error);
    res.status(500).json({
      success: false,
      message: 'Failed to sync SOS alert',
    });
  }
});

// POST /api/v1/sync/geofence
router.post('/geofence', async (req, res) => {
  try {
    const { deviceId, payload, timestamp, signature } = req.body;
    const pool = req.app.locals.pool;

    await pool.query(
      `INSERT INTO geofence_events (device_id, payload, timestamp, signature, created_at)
       VALUES ($1, $2, $3, $4, NOW())`,
      [deviceId, payload, timestamp, signature]
    );

    res.json({
      success: true,
      message: 'Geofence status synced',
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to sync geofence status',
    });
  }
});

// GET /api/v1/sync/pull/:deviceId
router.get('/pull/:deviceId', async (req, res) => {
  try {
    const { deviceId } = req.params;
    const pool = req.app.locals.pool;

    // Get pending data for this device
    const alertsResult = await pool.query(
      `SELECT * FROM sos_alerts 
       WHERE device_id = $1 AND status IN ('TRIGGERED', 'DELIVERED')
       ORDER BY created_at DESC LIMIT 10`,
      [deviceId]
    );

    res.json({
      success: true,
      data: {
        pendingAlerts: alertsResult.rows,
      },
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to pull pending data',
    });
  }
});

// Helper function to check geofence alerts
async function checkGeofenceAlerts(pool, deviceId, payload, logger) {
  try {
    // Get device's group
    const deviceResult = await pool.query(
      `SELECT group_id FROM devices WHERE device_id = $1`,
      [deviceId]
    );

    if (deviceResult.rows.length === 0 || !deviceResult.rows[0].group_id) {
      return;
    }

    const groupId = deviceResult.rows[0].group_id;

    // Get safe zones for the group
    const zonesResult = await pool.query(
      `SELECT * FROM safe_zones WHERE group_id = $1 AND is_active = true`,
      [groupId]
    );

    // Parse location from payload
    const location = JSON.parse(payload);
    const { latitude, longitude } = location;

    for (const zone of zonesResult.rows) {
      const distance = haversineDistance(
        latitude, longitude,
        zone.center_latitude, zone.center_longitude
      );

      const wasInZone = location.isInSafeZone || false;
      const isNowInZone = distance <= zone.radius_meters;

      // Detect entry/exit
      if (!wasInZone && isNowInZone && zone.notify_on_entry) {
        logger.info(`Device ${deviceId} entered safe zone ${zone.name}`);
        // Send notification
      } else if (wasInZone && !isNowInZone && zone.notify_on_exit) {
        logger.info(`Device ${deviceId} left safe zone ${zone.name}`);
        // Send notification
      }
    }
  } catch (error) {
    logger.error('Geofence check error:', error);
  }
}

// Haversine distance calculation
function haversineDistance(lat1, lon1, lat2, lon2) {
  const R = 6371e3; // Earth's radius in meters
  const φ1 = (lat1 * Math.PI) / 180;
  const φ2 = (lat2 * Math.PI) / 180;
  const Δφ = ((lat2 - lat1) * Math.PI) / 180;
  const Δλ = ((lon2 - lon1) * Math.PI) / 180;

  const a =
    Math.sin(Δφ / 2) * Math.sin(Δφ / 2) +
    Math.cos(φ1) * Math.cos(φ2) * Math.sin(Δλ / 2) * Math.sin(Δλ / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

  return R * c;
}

module.exports = router;

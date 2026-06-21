const express = require('express');
const router = express.Router();

// GET /api/v1/health
router.get('/', async (req, res) => {
  try {
    const pool = req.app.locals.pool;
    
    // Check database connection
    await pool.query('SELECT 1');
    
    res.json({
      success: true,
      status: 'healthy',
      timestamp: new Date().toISOString(),
      version: process.env.npm_package_version || '1.0.0',
    });
  } catch (error) {
    res.status(503).json({
      success: false,
      status: 'unhealthy',
      error: error.message,
    });
  }
});

// GET /api/v1/health/stats
router.get('/stats', async (req, res) => {
  try {
    const pool = req.app.locals.pool;
    
    const devices = await pool.query('SELECT COUNT(*) FROM devices');
    const groups = await pool.query('SELECT COUNT(*) FROM family_groups');
    const alerts = await pool.query('SELECT COUNT(*) FROM sos_alerts');
    
    res.json({
      success: true,
      data: {
        totalDevices: parseInt(devices.rows[0].count),
        totalGroups: parseInt(groups.rows[0].count),
        totalAlerts: parseInt(alerts.rows[0].count),
      },
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Failed to get stats',
    });
  }
});

module.exports = router;

const express = require('express');
const router = express.Router();

/**
 * POST /api/v1/contextual/report
 *
 * Receives an E2E-encrypted contextual awareness report from a child device and
 * writes the opaque envelope to Firestore. The server never decrypts
 * `encryptedData`; it is stored verbatim for the parent device to decrypt.
 *
 * Body shape (matches the app's ContextualSyncRequest):
 *   {
 *     childDeviceId: string,   // non-empty
 *     timestamp:     number,   // epoch millis
 *     encryptedData: string    // Base64(iv + ciphertext), opaque to the server
 *   }
 *
 * Writes:
 *   - contextual_reports / {childDeviceId}            (latest, overwritten)
 *   - contextual_history  / {childDeviceId}_{timestamp} (time-series)
 */
router.post('/report', async (req, res) => {
  const logger = req.app.locals.logger;
  try {
    const { childDeviceId, timestamp, encryptedData } = req.body;

    // Validate the DTO. encryptedData is opaque; we only assert it is a non-empty
    // string. The child device's DataValidator already filtered PII before encrypt.
    if (!childDeviceId || typeof childDeviceId !== 'string' || childDeviceId.trim() === '') {
      return res.status(400).json({ success: false, message: 'childDeviceId is required' });
    }
    if (timestamp === undefined || typeof timestamp !== 'number' || !Number.isFinite(timestamp)) {
      return res.status(400).json({ success: false, message: 'timestamp must be a finite number' });
    }
    if (!encryptedData || typeof encryptedData !== 'string' || encryptedData.trim() === '') {
      return res.status(400).json({ success: false, message: 'encryptedData is required' });
    }

    const admin = req.app.locals.admin;
    if (!admin || typeof admin.firestore !== 'function') {
      logger.error('Firestore (firebase-admin) is not initialized on the server');
      return res.status(503).json({ success: false, message: 'Firestore unavailable on server' });
    }
    const db = admin.firestore();

    const envelope = { childDeviceId, timestamp, encryptedData };

    // Latest report (overwrites previous).
    await db.collection('contextual_reports')
      .doc(childDeviceId)
      .set(envelope);

    // Time-series history entry.
    await db.collection('contextual_history')
      .doc(`${childDeviceId}_${timestamp}`)
      .set(envelope);

    // Update device last-seen (best-effort; device may not be registered yet).
    const pool = req.app.locals.pool;
    if (pool) {
      try {
        await pool.query(
          `UPDATE devices SET last_seen_at = NOW() WHERE device_id = $1`,
          [childDeviceId]
        );
      } catch (dbErr) {
        logger.warn(`Could not update last_seen_at for ${childDeviceId}: ${dbErr.message}`);
      }
    }

    logger.info(`Contextual report stored for ${childDeviceId} @ ${timestamp}`);

    res.status(201).json({
      success: true,
      message: 'Contextual report received',
    });
  } catch (error) {
    logger.error('Contextual report sync error:', error);
    res.status(500).json({ success: false, message: 'Failed to sync contextual report' });
  }
});

module.exports = router;

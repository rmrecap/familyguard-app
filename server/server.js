require('dotenv').config();
const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const rateLimit = require('express-rate-limit');
const admin = require('firebase-admin');
const { Pool } = require('pg');
const winston = require('winston');
const path = require('path');
const fs = require('fs');

// Initialize Firebase Admin - try service account file first, then env vars
let firebaseCredential;
const serviceAccountPath = path.join(__dirname, 'config', 'serviceAccountKey.json');

if (fs.existsSync(serviceAccountPath)) {
  const serviceAccount = require(serviceAccountPath);
  firebaseCredential = admin.credential.cert(serviceAccount);
  console.log('Firebase initialized with service account file');
} else if (process.env.FIREBASE_PROJECT_ID && process.env.FIREBASE_PRIVATE_KEY && process.env.FIREBASE_CLIENT_EMAIL) {
  firebaseCredential = admin.credential.cert({
    projectId: process.env.FIREBASE_PROJECT_ID,
    privateKey: process.env.FIREBASE_PRIVATE_KEY?.replace(/\\n/g, '\n'),
    clientEmail: process.env.FIREBASE_CLIENT_EMAIL,
  });
  console.log('Firebase initialized with environment variables');
} else {
  console.warn('Firebase credentials not found - push notifications will not work');
  firebaseCredential = null;
}

if (firebaseCredential) {
  admin.initializeApp({ credential: firebaseCredential });
}

// Database connection
const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: process.env.NODE_ENV === 'production' ? { rejectUnauthorized: false } : false,
});

// Logger
const logger = winston.createLogger({
  level: 'info',
  format: winston.format.combine(
    winston.format.timestamp(),
    winston.format.json()
  ),
  transports: [
    new winston.transports.Console(),
    new winston.transports.File({ filename: 'logs/error.log', level: 'error' }),
    new winston.transports.File({ filename: 'logs/combined.log' }),
  ],
});

// Express app
const app = express();

// Security middleware
app.use(helmet());
app.use(cors({
  origin: process.env.CORS_ORIGINS?.split(',') || '*',
  credentials: true,
}));

// Rate limiting
const limiter = rateLimit({
  windowMs: parseInt(process.env.RATE_LIMIT_WINDOW_MS) || 15 * 60 * 1000,
  max: parseInt(process.env.RATE_LIMIT_MAX) || 100,
  message: 'Too many requests from this IP, please try again later.',
});
app.use('/api/', limiter);

// Body parsing
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true }));

// Make pool available to routes
app.locals.pool = pool;
app.locals.logger = logger;
app.locals.admin = admin;

// Routes
app.use('/api/v1/device', require('./routes/device'));
app.use('/api/v1/family', require('./routes/family'));
app.use('/api/v1/sync', require('./routes/sync'));
app.use('/api/v1/alerts', require('./routes/alerts'));
app.use('/api/v1/health', require('./routes/health'));

// Error handling middleware
app.use((err, req, res, next) => {
  logger.error(err.stack);
  res.status(err.status || 500).json({
    success: false,
    message: err.message || 'Internal Server Error',
  });
});

// Start server
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  logger.info(`FamilyGuard API running on port ${PORT}`);
  console.log(`FamilyGuard API running on port ${PORT}`);
});

module.exports = app;

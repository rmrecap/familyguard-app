# FamilyGuard - Privacy-First Family Safety App

A legitimate, ethical child safety Android application with robust security, privacy-first architecture, and full legal compliance.

## Features

- **Location Sharing**: Share location with parents for safety (list-based view, no Google Maps billing)
- **Emergency SOS**: Send emergency alerts with location to family members
- **Safe Zones**: Geofencing alerts when entering/leaving designated areas
- **Family Management**: Create family groups and invite members
- **Consent Management**: Feature-level consent with audit trail
- **Kill Switch**: Emergency stop all monitoring with single tap

## Architecture

### Android Client
- **Kotlin + Jetpack Compose**: Modern Android UI
- **MVVM/MVI Pattern**: Clean architecture
- **Hilt DI**: Dependency injection
- **Room DB**: Local encrypted database
- **Firebase Integration**: Real-time sync and push notifications
- **E2E Encryption**: AES-256-GCM encryption for all data

### Backend (Render.com + Firebase)
- **Node.js/Express API**: RESTful API endpoints
- **PostgreSQL Database**: Persistent storage for audit logs
- **Firebase Firestore**: Real-time data sync
- **Firebase Cloud Messaging**: Push notifications
- **Docker Deployment**: Containerized for Render.com

## Security Features

- **No SMS/Contact Reading**: Metadata-only approach
- **Encrypted Storage**: All sensitive data encrypted at rest
- **Audit Trail**: Tamper-proof logging of all actions
- **Kill Switch**: Emergency stop functionality
- **Transparent Notifications**: Always visible when active

## Legal Compliance

- **GDPR Compliant**: Data minimization, right to deletion
- **CCPA Compliant**: User rights and data protection
- **COPPA Compliant**: Child protection measures
- **Google Play Store Ready**: Meets all policy requirements

## Setup

### Android App
1. Open project in Android Studio
2. Add `google-services.json` from Firebase Console
3. Build and run on device/emulator

### Backend API
1. Navigate to `server/` directory
2. Run `npm install`
3. Copy `.env.example` to `.env` and configure
4. Run `npm run migrate` to set up database
5. Run `npm start` to start server

### Render.com Deployment
1. Connect GitHub repository to Render.com
2. Use `render.yaml` for automatic configuration
3. Set environment variables in Render dashboard
4. Deploy!

## Environment Variables

### Server (.env)
```
PORT=3000
NODE_ENV=production
DATABASE_URL=postgresql://...
FIREBASE_PROJECT_ID=your-project-id
FIREBASE_PRIVATE_KEY=your-private-key
FIREBASE_CLIENT_EMAIL=your-client-email
JWT_SECRET=your-jwt-secret
ENCRYPTION_KEY=your-encryption-key
```

### Android App
- Add `google-services.json` to `app/` directory
- Update API base URL in `AppModule.kt` for production

## API Endpoints

- `POST /api/v1/device/register` - Register device
- `POST /api/v1/device/heartbeat/:deviceId` - Device heartbeat
- `POST /api/v1/family/create` - Create family group
- `POST /api/v1/family/join` - Join family group
- `GET /api/v1/family/members/:groupId` - Get family members
- `POST /api/v1/sync/location` - Sync location
- `POST /api/v1/sync/sos` - Sync SOS alert
- `GET /api/v1/alerts/:groupId` - Get alerts
- `GET /api/v1/health` - Health check

## License

MIT License - See LICENSE file for details

## Support

For issues or questions, please open an issue on GitHub.

# PaySentry

PaySentry is a real-time digital-banking fraud-monitoring system for a browser dashboard and Android app. It ingests **simulated** banking events, correlates them with SMS, URL, and QR fraud signals, then delivers saved risk explanations to both clients through the shared Spring Boot API and WebSocket, with optional FCM notifications.

> PaySentry monitors and alerts on lien changes only. It never removes, bypasses, releases, or modifies a bank lien.

## Architecture

```text
Mock Banking API -> Spring Boot monitoring backend -> Unified Risk Engine -> MySQL
                                                              |              |
                                                           WebSocket         Alerts/history
                                                        /             \
                                             React web dashboard    Flutter Android app

High / critical alerts -> Firebase Cloud Messaging -> registered phone
```

## Project structure

```text
PaySentry/
|-- backend/                 Spring Boot API, JWT, WebSocket, FCM adapter, risk engine
|-- mock-bank-api/           Separate local-only banking-event simulator
|-- mobile-app/              Flutter/Dart mobile client source
|-- database/                MySQL schema and seed data
|-- legacy-web-dashboard/    React browser dashboard for the shared backend
`-- README.md
```

## Implemented capabilities

- JWT registration, login, server-side token revocation on logout, BCrypt password hashing, and admin authorization.
- Live account balance, available balance, lien amount, security score, recent transactions, and alerts.
- Dedicated lien monitoring for unexpected creation, large holds, sharp increases, repeated modifications, and missing references.
- Rule-based transaction, new-device, and new-location risk detection.
- A central risk engine that correlates recent banking, SMS, URL, and QR signals using `LOW` (0–30), `MEDIUM` (31–70), `HIGH` (71–90), and `CRITICAL` (91–100) levels.
- WebSocket alert broadcasts on `/topic/accounts/{accountId}` and FCM delivery for high/critical alerts when configured.
- Opt-in incoming-SMS monitoring with automatic checks for links found in messages, phishing URL checks, live QR scanning, and QR image uploads in the Flutter app. QR scanning remains user-triggered.
- Consent-based Gmail read-only OAuth integration, periodic inbox synchronization, encrypted provider tokens, message-body minimization, and per-account disconnect.
- Full fraud-event and alert history plus an admin monitoring summary at `GET /api/admin/monitoring/summary`.
- A separate mock-bank service that provides account, transactions, lien APIs, and safe development event generators.

These are development-project capabilities, not a claim of bank-grade or independently verified fraud detection. A LOW score does not prove that content is safe.

## Cloud deployment

For a zero-cost internet deployment, follow [DEPLOYMENT.md](./DEPLOYMENT.md): Render Free for the Spring Boot API and static React dashboard, plus a TiDB Cloud Starter Free MySQL-compatible database. Render's API sleeps when idle and has ephemeral local storage, so persistent accounts belong in TiDB, not on the web service. The free database has monthly storage/request quotas and must be configured with TLS. No hosted URL exists until the provider accounts and services have been created and verified.

## Before running

The current workstation has Java 11, Maven 3.9.9, MySQL 8, and Node installed. Install Flutter, Dart, Android SDK/ADB, and the VS Code Dart + Flutter extensions before running `mobile-app`.

For each terminal session, set Java 11 as `JAVA_HOME` (the JDK root, without `bin`) if it is not already selected:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-11.0.30.7-hotspot'
```

## Run locally with H2

The development profile has no built-in signing or mock-bank secrets. For a convenient local run, use the startup script. It creates temporary random values in memory and passes the shared mock-bank key to both services; no secret file is written:

```powershell
Set-Location 'C:\Mini project\PaySentry'
.\scripts\start-local.ps1
```

The script opens separate PowerShell windows for the H2 development backend, mock-bank simulator, and React dashboard, waits for the backend and frontend health checks, then opens the dashboard. The H2 database is in-memory and is reset when the backend stops; use MySQL when local persistence between restarts is needed.

The React dashboard and Flutter app both use the authenticated `/api/mobile/dashboard` and `/api/mobile/alerts` APIs for account activity and saved unified alerts. The React alert view reconnects to the authenticated WebSocket after interruptions and refreshes persisted history when an alert arrives. The configured `PAYSENTRY_CORS_ALLOWED_ORIGINS` is an exact comma-separated allowlist shared by REST and WebSocket handshakes; it must contain the dashboard origin, never `*`. For production, set it to the actual HTTPS dashboard origin(s), and set the matching `VITE_API_BASE_URL` and `VITE_WEBSOCKET_URL` to HTTPS/WSS endpoints.

To start each component manually instead, configure the same per-run credentials in the terminal that launches the processes:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-11.0.30.7-hotspot'
$env:SPRING_PROFILES_ACTIVE = 'dev'
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$jwtBytes = New-Object byte[] 32
$rng.GetBytes($jwtBytes)
$env:PAYSENTRY_JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
$env:PAYSENTRY_MOCK_BANK_KEY = [Guid]::NewGuid().ToString('N')
```

In one terminal, start the backend:

```powershell
Set-Location 'C:\Mini project\PaySentry\backend'
& 'C:\Mini project\apache-maven-3.9.9\bin\mvn.cmd' spring-boot:run
```

In a second terminal, set the same key used by the backend and start the mock bank. The JWT key is only required by the backend:

```powershell
$env:PAYSENTRY_MOCK_BANK_KEY = '<same random key configured for backend>'
Set-Location 'C:\Mini project\PaySentry\mock-bank-api'
& 'C:\Mini project\apache-maven-3.9.9\bin\mvn.cmd' spring-boot:run
```

In another terminal, start the browser dashboard:

```powershell
Set-Location 'C:\Mini project\PaySentry\legacy-web-dashboard'
$env:VITE_API_BASE_URL = 'http://127.0.0.1:8080/api'
$env:VITE_WEBSOCKET_URL = 'ws://127.0.0.1:8080/ws'
npm run dev
```

Open `http://127.0.0.1:5173` and register an account. Admin seeding is disabled unless explicitly configured with `PAYSENTRY_SEED_ADMIN_ENABLED=true`, `PAYSENTRY_ADMIN_EMAIL`, and `PAYSENTRY_ADMIN_PASSWORD`; there is no default admin password.

Generate a synthetic event for the account ID returned at login (for example, `PS-10001`):

```powershell
Invoke-RestMethod -Method Post 'http://127.0.0.1:8081/api/development/events/<ACCOUNT_ID>/LARGE_TRANSACTION'
```

Available scenarios are `NORMAL_ACTIVITY`, `LARGE_TRANSACTION`, `NEW_DEVICE_TRANSACTION`, `NEW_LOCATION_TRANSACTION`, `LIEN_CREATE`, `LIEN_INCREASE`, `DEVICE_CHANGE`, and `LOCATION_CHANGE`. Events are synthetic and never contact a real bank.

## Use MySQL

Create the database and optionally seed the admin account:

```powershell
mysql -u root -p < 'C:\Mini project\PaySentry\database\schema.sql'
mysql -u root -p paysentry < 'C:\Mini project\PaySentry\database\seed.sql'
```

In PowerShell, if native `<` input redirection is unavailable, pipe the SQL file:

```powershell
Get-Content -Raw 'C:\Mini project\PaySentry\database\schema.sql' | mysql -u root -p
Get-Content -Raw 'C:\Mini project\PaySentry\database\seed.sql' | mysql -u root -p paysentry
```

Start the installed MySQL Windows service if it is stopped (the service name can differ by installer):

```powershell
Get-Service *mysql*
Start-Service -Name 'MySQL80'
```

Set `PAYSENTRY_DB_URL`, `PAYSENTRY_DB_USERNAME`, and `PAYSENTRY_DB_PASSWORD` in the local environment. The `mysql` profile runs Flyway migrations and keeps Hibernate in `validate` mode. Empty databases are initialized from the versioned baseline; existing databases are baselined without dropping tables and receive the additive V2 migration. Do not replay `database/schema.sql` against a production database. The older manual `database/migrations/V001__add_user_token_version.sql` applies only to historical schemas that lack `token_version`; the current schema already has that column.

Start the backend with:

```powershell
$env:PAYSENTRY_DB_URL = 'jdbc:mysql://localhost:3306/paysentry?useSSL=true&requireSSL=true&verifyServerCertificate=true&serverTimezone=UTC'
$env:SPRING_PROFILES_ACTIVE = 'mysql'
& 'C:\Mini project\apache-maven-3.9.9\bin\mvn.cmd' spring-boot:run
```

## Run the Flutter app

The Android project is already included. Resolve dependencies and run on an Android emulator. For the emulator, `localhost` is the emulator itself; use `10.0.2.2` to reach the laptop-hosted backend:

```powershell
Set-Location 'C:\Mini project\PaySentry\mobile-app'
flutter pub get
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080/api --dart-define=WEBSOCKET_URL=ws://10.0.2.2:8080/ws
```

`10.0.2.2` is the Android Emulator address for the host computer. For a physical phone, put the phone and computer on the same trusted Wi-Fi, use the computer's LAN IPv4 address in both `--dart-define` values, and allow ports 8080 and 8081 through the local firewall. HTTP and WS are permitted only in the Android debug manifest; use HTTPS and WSS with valid certificates for release builds. The main manifest blocks cleartext traffic.

To create and install a debug APK for a phone during local development, set the reachable backend URLs before building, then install over USB with Android platform-tools:

```powershell
flutter build apk --debug --dart-define=API_BASE_URL=http://192.168.1.25:8080/api --dart-define=WEBSOCKET_URL=ws://192.168.1.25:8080/ws
adb install -r .\build\app\outputs\flutter-apk\app-debug.apk
```

Replace the example address with the laptop's Wi-Fi IPv4 from `ipconfig`. `localhost` only reaches the same device, while `10.0.2.2` is emulator-only; a real phone must use the laptop's reachable LAN address. Start services before the app. For Windows Firewall, allow inbound TCP 8080 on Private networks only (run PowerShell as Administrator if a rule is needed):

For release builds, do not use a laptop address. Use the cloud procedure in [DEPLOYMENT.md](./DEPLOYMENT.md) and provide production HTTPS/WSS URLs through `--dart-define-from-file`.

```powershell
New-NetFirewallRule -DisplayName 'PaySentry local API (Private)' -Direction Inbound -Action Allow -Protocol TCP -LocalPort 8080 -Profile Private
```

Check connectivity from the phone browser at `http://<LAPTOP_WIFI_IP>:8080/api/health`. If it fails, confirm both devices are on the same non-guest Wi-Fi, the backend is running, Windows is using the Private network profile, and the firewall rule is active. Port 8081 is only needed for local simulator endpoints; the browser and phone dashboards receive persisted event/alert data from port 8080. Start PaySentry, register/sign in, then use Profile to connect Gmail or enable incoming SMS. SMS monitoring is opt-in and depends on Android permission and process lifetime; it is not guaranteed background monitoring. Android/Google Play restricts SMS permissions, so distribution eligibility must be confirmed before publishing. QR scanning and image upload are user-triggered and never initiate a payment or automatically open scanned destinations.

## Gmail OAuth setup

Gmail integration is disabled until explicitly configured. In Google Cloud, enable the Gmail API, configure the OAuth consent screen and add the Gmail read-only scope `https://www.googleapis.com/auth/gmail.readonly`, then create a Web application OAuth client. Register the backend callback URI exactly as `https://YOUR_BACKEND_HOST/api/gmail/callback` (use an HTTPS tunnel for local provider testing and register that exact URI in Google Cloud).

Provide the following only to the backend process using a secret manager or private environment configuration:

- `PAYSENTRY_GMAIL_ENABLED=true`
- `PAYSENTRY_GMAIL_CLIENT_ID` and `PAYSENTRY_GMAIL_CLIENT_SECRET`
- `PAYSENTRY_GMAIL_REDIRECT_URI` (must exactly match the Google OAuth client)
- `PAYSENTRY_CREDENTIAL_ENCRYPTION_KEY` (base64-encoded AES key: 16, 24, or 32 random bytes)
- Optional `PAYSENTRY_GMAIL_SYNC_INTERVAL_MS` (default 900000) and `PAYSENTRY_GMAIL_MAX_MESSAGES_PER_SYNC` (default 25)

The authenticated user must select **Connect Gmail** and approve Google consent. PaySentry requests read-only mailbox access, stores OAuth tokens encrypted, and analyzes message content in memory rather than persisting bodies. Periodic sync requires the backend to remain running. The current sync reads only the provider's first page (up to the configured maximum), so large inbox backfills are incomplete. Disconnect removes stored connection data and attempts token revocation. Google's OAuth consent-screen verification and restricted-scope review may be required outside test users.

## Enable FCM

1. Create a Firebase project and register the Android application ID shown in `mobile-app/android/app/build.gradle.kts`.
2. Configure Firebase for Flutter and add the generated platform configuration without committing private service-account credentials.
3. Place the Firebase Admin service-account JSON outside source control.
4. Set `PAYSENTRY_FCM_ENABLED=true` and `PAYSENTRY_FCM_SERVICE_ACCOUNT_PATH` in the backend environment.

Without Firebase configuration, local WebSocket alerts and in-app alert history remain fully available.

## Tests and builds

Run the available checks from PowerShell:

```powershell
Set-Location 'C:\Mini project\PaySentry\backend'
..\..\apache-maven-3.9.9\bin\mvn.cmd test

Set-Location 'C:\Mini project\PaySentry\mock-bank-api'
..\..\apache-maven-3.9.9\bin\mvn.cmd test

Set-Location 'C:\Mini project\PaySentry\mobile-app'
flutter analyze
flutter test
flutter build apk --debug
```

Backend tests cover the shared risk engine, authentication security, QR analysis, and Spring application startup; mock-bank tests cover event submission behavior. The React dashboard has a production build but no test script, and mobile coverage is limited. Real-device SMS, camera, background, FCM, and OAuth consent flows must be validated on configured devices/accounts.

## Development APIs

- `GET /api/accounts/{accountId}` — mock account snapshot
- `GET /api/accounts/{accountId}/transactions` — mock transaction history
- `GET /api/accounts/{accountId}/lien` — mock lien state
- `POST /api/development/events/{accountId}/{scenario}` — safe local event generator
- `GET /api/mobile/dashboard` — authenticated mobile dashboard
- `GET /api/mobile/alerts` — authenticated fraud history
- `POST /api/detections/sms`, `/url`, `/qr` — authenticated unified content checks

The mock service's internal call to the backend uses `X-Mock-Bank-Key`; configure the same strong, private `PAYSENTRY_MOCK_BANK_KEY` for both services. The backend requires a 256-bit JWT signing key and refuses to start if it is missing or too short. Do not expose H2, use development HTTP/WS, or rely on the in-process rate limiter in production. MySQL production deployments should use encrypted private networking or database TLS, managed schema migrations, tested backups, retention controls, and a gateway-level distributed rate limiter. Password-reset delivery is intentionally disabled until an email delivery provider is configured; the public endpoint does not return reset tokens.

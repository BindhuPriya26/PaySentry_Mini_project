# PaySentry

PaySentry is a mobile-first, real-time digital-banking fraud-monitoring system. It ingests **simulated** banking events, correlates them with SMS, URL, and QR fraud signals, then delivers risk explanations to the Flutter app through REST, WebSocket, and optional FCM notifications.

> PaySentry monitors and alerts on lien changes only. It never removes, bypasses, releases, or modifies a bank lien.

## Architecture

```text
Mock Banking API -> Spring Boot monitoring backend -> Unified Risk Engine -> MySQL
                                                              |              |
                                                           WebSocket         Alerts/history
                                                              |
                                                        Flutter mobile app

High / critical alerts -> Firebase Cloud Messaging -> registered phone
```

## Project structure

```text
PaySentry/
|-- backend/                 Spring Boot API, JWT, WebSocket, FCM adapter, risk engine
|-- mock-bank-api/           Separate local-only banking-event simulator
|-- mobile-app/              Flutter/Dart mobile client source
|-- database/                MySQL schema and seed data
|-- legacy-web-dashboard/    Preserved React prototype; not part of mobile workflow
`-- README.md
```

## Implemented capabilities

- JWT registration, login, client-side logout, BCrypt password hashing, and admin authorization.
- Live account balance, available balance, lien amount, security score, recent transactions, and alerts.
- Dedicated lien monitoring for unexpected creation, large holds, sharp increases, repeated modifications, and missing references.
- Rule-based transaction, new-device, and new-location risk detection.
- A central risk engine that correlates recent banking, SMS, URL, and QR signals using `LOW` (0–30), `MEDIUM` (31–70), `HIGH` (71–90), and `CRITICAL` (91–100) levels.
- WebSocket alert broadcasts on `/topic/accounts/{accountId}` and FCM delivery for high/critical alerts when configured.
- SMS scam, phishing URL, live QR scanner, and QR image-upload workflows in the Flutter app.
- Full fraud-event and alert history plus an admin monitoring summary at `GET /api/admin/monitoring/summary`.
- A separate mock-bank service that provides account, transactions, lien APIs, and safe development event generators.

## Before running

The current workstation has Java 11, Maven 3.9.9, MySQL 8, and Node installed. Install Flutter, Dart, Android SDK/ADB, and the VS Code Dart + Flutter extensions before running `mobile-app`.

For each terminal session, set a valid Java home (without `bin`):

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-11.0.30.7-hotspot'
```

## Run locally with H2

1. Start the monitoring backend:

```powershell
Set-Location 'C:\Mini project\PaySentry\backend'
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-11.0.30.7-hotspot'
..\..\apache-maven-3.9.9\bin\mvn.cmd spring-boot:run
```

2. In a second terminal, start the mock-bank service:

```powershell
Set-Location 'C:\Mini project\PaySentry\mock-bank-api'
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-11.0.30.7-hotspot'
..\..\apache-maven-3.9.9\bin\mvn.cmd spring-boot:run
```

3. Sign in from the app with the seeded local account:

```text
Email: admin@paysentry.local
Password: Admin@123
```

4. Generate a safe test event after signing in:

```powershell
Invoke-RestMethod -Method Post 'http://127.0.0.1:8081/api/development/events/PS-10001/LARGE_TRANSACTION'
```

Available scenarios are `NORMAL_ACTIVITY`, `LARGE_TRANSACTION`, `NEW_DEVICE_TRANSACTION`, `NEW_LOCATION_TRANSACTION`, `LIEN_CREATE`, `LIEN_INCREASE`, `DEVICE_CHANGE`, and `LOCATION_CHANGE`.

## Use MySQL

Create the database and optionally seed the admin account:

```powershell
mysql -u root -p < 'C:\Mini project\PaySentry\database\schema.sql'
mysql -u root -p paysentry < 'C:\Mini project\PaySentry\database\seed.sql'
```

Set a real local MySQL password in `backend/src/main/resources/application-mysql.properties`, then start the backend with:

```powershell
..\..\apache-maven-3.9.9\bin\mvn.cmd spring-boot:run -Dspring-boot.run.profiles=mysql
```

## Run the Flutter app

After Flutter is installed, generate the Android platform wrapper once without replacing the source in `lib/`:

```powershell
Set-Location 'C:\Mini project\PaySentry\mobile-app'
flutter create . --platforms=android
flutter pub get
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080/api --dart-define=WEBSOCKET_URL=ws://10.0.2.2:8080/ws
```

`10.0.2.2` is the Android Emulator address for the host computer. For a physical device, replace it with the computer’s LAN IPv4 address and ensure both services accept the connection.

## Enable FCM

1. Create a Firebase project and register the Android app after `flutter create`.
2. Add `google-services.json` to `mobile-app/android/app/` and run `flutterfire configure` if using FlutterFire CLI.
3. Place the Firebase Admin service-account JSON outside source control, for example `backend/firebase-service-account.json`.
4. Set `paysentry.fcm.enabled=true` and `paysentry.fcm.service-account-path=...` in a private Spring configuration file or environment-specific profile.

Without Firebase configuration, local WebSocket alerts and in-app alert history remain fully available.

## Development APIs

- `GET /api/accounts/{accountId}` — mock account snapshot
- `GET /api/accounts/{accountId}/transactions` — mock transaction history
- `GET /api/accounts/{accountId}/lien` — mock lien state
- `POST /api/development/events/{accountId}/{scenario}` — safe local event generator
- `GET /api/mobile/dashboard` — authenticated mobile dashboard
- `GET /api/mobile/alerts` — authenticated fraud history
- `POST /api/detections/sms`, `/url`, `/qr` — authenticated unified content checks

The mock service’s internal call to the backend uses `X-Mock-Bank-Key`; change the shared development key before any non-local deployment.

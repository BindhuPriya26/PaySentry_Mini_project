# PaySentry

PaySentry is a unified full-stack cybersecurity platform for detecting and preventing multi-channel digital banking fraud from a single dashboard.

## Project Structure

```text
PaySentry/
|-- backend/    Spring Boot REST API
|-- database/   MySQL schema and seed scripts
|-- frontend/   React + Vite dashboard
`-- README.md
```

## Included Features

- JWT authentication with registration, login, forgot password, and reset password
- Dashboard analytics with risk metrics, activity feed, and charts
- Scam SMS detection
- Fraudulent QR code analysis
- Phishing URL detection
- Transaction risk scoring
- Saved reports with PDF export
- Admin users, reports, and system statistics panel

## Local Run

### Single Website Mode

You can now run PaySentry as one website from the Spring Boot backend.

1. Open a terminal in `PaySentry/backend`
2. Set Java 11:

```powershell
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-11.0.30.7-hotspot"
```

3. Start Spring Boot:

```powershell
..\..\apache-maven-3.9.9\bin\mvn.cmd spring-boot:run
```

4. Open:

```text
http://127.0.0.1:8080
```

The backend now serves the bundled React frontend and the API together as a single website.

### Backend

The backend now defaults to a local H2 development profile so you can run it immediately without configuring MySQL first.

1. Open a terminal in `PaySentry/backend`
2. Set Java 11:

```powershell
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-11.0.30.7-hotspot"
```

3. Run Spring Boot:

```powershell
..\..\apache-maven-3.9.9\bin\mvn.cmd spring-boot:run
```

Default dev admin credentials:

- Email: `admin@paysentry.local`
- Password: `Admin@123`

### Frontend

1. Open a second terminal in `PaySentry/frontend`
2. Install packages:

```powershell
npm.cmd install
```

3. Start the Vite development server:

```powershell
npm.cmd run dev
```

The frontend runs on `http://127.0.0.1:5173` and talks directly to the backend on `http://127.0.0.1:8080/api` during local development.
Production builds are written to `frontend/web-build`.

### MySQL Mode

If you want to use MySQL instead of H2:

1. Create the database using [schema.sql](/C:/Mini project/PaySentry/database/schema.sql:1)
2. Optionally import [seed.sql](/C:/Mini project/PaySentry/database/seed.sql:1)
3. Update `backend/src/main/resources/application-mysql.properties`
4. Start the backend with:

```powershell
..\..\apache-maven-3.9.9\bin\mvn.cmd spring-boot:run -Dspring-boot.run.profiles=mysql
```

# PaySentry zero-cost deployment

This deployment plan uses free tiers only:

- **Spring Boot API:** Render Free web service, built from `backend/Dockerfile`.
- **Persistent SQL:** TiDB Cloud Starter Free instance, accessed with the existing MySQL Connector/J driver and JDBC URL. TiDB implements the MySQL protocol and the MySQL syntax used by the PaySentry schema and migrations; it is not Oracle MySQL.
- **React dashboard:** Render Free static site. Static sites do not sleep like Render Free web services.
- **Android:** Flutter release APK configured with the API's actual public HTTPS and WSS URLs after the API is deployed and verified.

No service, payment method, or paid plan is created by this repository configuration. Do not choose a Render paid instance, a TiDB Scalable instance, or add a payment method. Provider quotas and plan details can change; check the linked official pages during setup.

## Free-tier limitations and persistence

### Render Free API

Render documents that a Free web service spins down after **15 minutes without inbound traffic** (including WebSocket traffic). Its next HTTP request or WebSocket connection wakes it, typically in about a minute. The first login or API call after idle can therefore be slow or time out on a short client timeout. A service's local filesystem is ephemeral and is cleared on restart, spin-down, and redeploy; **do not store user accounts or uploaded persistent data there**.

Render includes **750 Free instance hours per workspace per calendar month**. Spun-down time does not consume those hours. If the workspace uses all included hours, free web services are suspended until the next month. Free services can restart at any time. Render also meters outbound bandwidth and build-pipeline minutes; without a payment method, reaching included limits suspends service or prevents new builds rather than charging for overages. Keep the workspace payment-method-free and monitor usage. Render warns that unusually high service-initiated public traffic, including external database calls, can cause a Free service to be suspended.

The API uses Render's HTTPS endpoint. Railway is not required. The API's `PORT` is supplied by Render, and Spring Boot binds to it.

### TiDB Cloud Starter Free database

TiDB Cloud Starter is a managed, MySQL-protocol-compatible database. The first five Starter instances in an organization receive free usage quotas; usage beyond free quotas can be consumption-billed. To guarantee a zero-spend ceiling, create a **Starter** instance, set its **Monthly Spending Limit to $0.00**, and do not add a payment method or switch to a paid plan. TiDB documents that a $0 limit restricts usage to the free quota and throttles the instance when that quota is exhausted. The current console showed 5 GiB row-based storage and 50 million request units per month for this instance; monitor the console for its current quota and usage.

The data is in the database service, not Render's ephemeral filesystem, so API restarts and redeployments do not delete accounts. TiDB's free-quota page does not state a 30-day expiry; still, this is a free-tier service, not a contractual production SLA. Keep independent backups of data you cannot afford to lose, and periodically export and verify them. Check the provider's current plan terms before storing important data.

Connections use TiDB's public endpoint and **TLS 1.2+** (`sslMode=VERIFY_IDENTITY`). Render Free does not provide dedicated outbound IPs. After deploying the API, copy the outbound IP ranges for its Render region from the service's **Connect > Outbound** panel and allow those ranges in TiDB's IP firewall. Render documents these ranges as shared by services in that region, not exclusive IPs. If you cannot apply the ranges or connections fail, do not weaken TLS; resolve the firewall setup before using the app.

TiDB supports common MySQL 5.7/8.0 syntax, including the `AUTO_INCREMENT`, indexes, foreign keys, `ALTER TABLE`, and transactions used here. The application continues to use MySQL Connector/J; no PostgreSQL driver, SQL rewrite, or PostgreSQL migration is introduced. TiDB is not identical to MySQL, and provider-side execution of Flyway and the row-lock refresh-token flow must be verified after creating the account. Do not move production users to a different database without a verified backup and restore plan.

### Free static dashboard

Render Static Sites provide HTTPS via `onrender.com` and are free. They have build-pipeline and outbound-bandwidth quotas. They have no service container to spin down. Production builds require an explicit HTTPS API URL and WSS URL.

## Setup sequence

You must create and authorize provider accounts in their dashboards. No cloud credentials are present in this repository/workspace, so deployment cannot be created or verified locally. Use only the **Free** options described below.

### 1. Create the TiDB Cloud Starter Free database

1. Sign up at [TiDB Cloud](https://tidbcloud.com/); select **TiDB Cloud Starter** and the **Free** instance option. The documented free option does not require a credit card.
2. Create the instance, choose a nearby region, and save the generated database password in a password manager. Do not commit it or send it in chat.
3. In **Connect**, select a MySQL client connection and note the host, port (normally `4000`), database, and the instance-prefixed username. Enable TLS as shown by the provider.
4. In TiDB's SQL editor, create a dedicated application database and a least-privilege user. Replace the placeholders; choose a strong password and save it securely. Use the TiDB instance prefix in the SQL username:

   ```sql
   CREATE DATABASE paysentry;
   CREATE USER '<instance-prefix>.paysentry_app'@'%' IDENTIFIED BY '<unique-database-password>';
   GRANT ALL PRIVILEGES ON paysentry.* TO '<instance-prefix>.paysentry_app'@'%';
   ```

   Use `<instance-prefix>.paysentry_app` as `PAYSENTRY_DB_USERNAME`, that user's password as `PAYSENTRY_DB_PASSWORD`, and `paysentry` as the JDBC database name. The application user needs schema DDL rights for Flyway migrations but no global grants.
5. Keep the instance on Starter with the monthly spending limit at **$0.00**. Do not create a paid plan, attach a payment method, or raise the spending limit.
6. Create the API service in the next step. Then add that Render service's regional outbound CIDR ranges to TiDB's public endpoint firewall. For initial migration only, you may also allow your current IP temporarily; remove it after the migration test.

### 2. Deploy the API on Render Free

1. The deployment configuration must be present on the GitHub branch Render will build. Connect your GitHub repository in Render and create a **Blueprint** from `render.yaml`, or create a Web Service manually. The Blueprint generates the JWT and mock-service keys server-side; its `sync: false` prompts are for the database credentials, CORS origin, and dashboard build URLs. Enter those values only in the Render dashboard, never in Git.
2. If manual: select the Docker runtime, choose **Free**, set root directory to `backend`, Dockerfile path to `./Dockerfile`, health check path to `/api/health`, and branch to the branch containing these changes.
3. Set `SPRING_PROFILES_ACTIVE=prod`, `PAYSENTRY_DB_URL`, `PAYSENTRY_DB_USERNAME`, and `PAYSENTRY_DB_PASSWORD` in Render's environment settings. The Blueprint generates `PAYSENTRY_JWT_SECRET` and `PAYSENTRY_MOCK_BANK_KEY` for you; for a manual service, generate independent random values for them. Set the JDBC URL in this form, replacing every placeholder with the values shown by TiDB:

   ```text
   jdbc:mysql://<tidb-host>:4000/<database>?sslMode=VERIFY_IDENTITY&serverTimezone=UTC
   ```

   The TiDB username includes its instance prefix. For a manual service, generate at least 32 bytes of key material for `PAYSENTRY_JWT_SECRET` and a separate random `PAYSENTRY_MOCK_BANK_KEY`; for example, use Node's `crypto.randomBytes(32).toString('base64')`. Keep them stable in Render across deploys. Never put them in Flutter, React build variables, or Git.
4. Set `PAYSENTRY_DB_POOL_SIZE=2`, `PAYSENTRY_DB_MIN_IDLE=0`, `PAYSENTRY_JWT_EXPIRATION_MS=900000`, and `PAYSENTRY_REFRESH_TOKEN_EXPIRATION_DAYS=30`. Keep admin seeding, Gmail, and FCM disabled unless separately configured.
5. Add a public Render `onrender.com` domain for the API and wait for the first deployment. Copy the real domain; do not guess it. Update the TiDB firewall with the API service's outbound ranges from Render's service Connect panel. TiDB may initially have an allow-all public rule; replace it with the Render ranges after obtaining them.
6. Set `PAYSENTRY_CORS_ALLOWED_ORIGINS` to the exact dashboard origin (for example `https://<actual-dashboard>.onrender.com`, without a path or trailing slash). Production requires this value; redeploy the API after setting it. If you use `render.yaml` before the domains are assigned, enter temporary HTTPS/WSS-shaped dashboard build URLs and a temporary HTTPS CORS origin to create the services, then replace all three with the actual URLs before browser sign-in.

The Render Free instance has 512 MiB RAM. The API Docker entrypoint caps the JVM heap to leave headroom for the runtime. Do not deploy optional compute services on a paid plan. The simulated-bank generator is not required to provide the API's authentication, saved account data, analysis endpoints, or shared risk engine; it can be run locally for development only. Any event source exposed to the hosted API must be reviewed and protected with the shared mock key.

### 3. Deploy the React dashboard as a Render Free Static Site

1. In Render, create a **Static Site** from the same repository and select **Free**.
2. Set root directory `legacy-web-dashboard`, build command `npm ci && npm run build`, and publish directory `web-build`.
3. Set the build environment variables after the API domain exists:

   ```text
   VITE_API_BASE_URL=https://<actual-api-domain>.onrender.com/api
   VITE_WEBSOCKET_URL=wss://<actual-api-domain>.onrender.com/ws
   ```

4. Add the site's public `onrender.com` domain. If you change the dashboard origin later, update `PAYSENTRY_CORS_ALLOWED_ORIGINS` on the API to match and redeploy.

### 4. Verify the hosted services before building the production APK

1. From a phone using mobile data, open `https://<actual-api-domain>.onrender.com/api/health`. It must return HTTP 2xx and PaySentry's health JSON. The first request may take about a minute while the free API wakes.
2. Verify dashboard sign-in, browser CORS, and WSS notifications with a test account. Registering on the cloud creates the account in TiDB, not in a local H2 database.
3. Register once, log out/in, restart or redeploy the API, and verify login still works. Inspect TiDB to verify that Flyway created `flyway_schema_history`, `users.normalized_email`, and `refresh_tokens`. Run a case-insensitive duplicate-registration test and confirm another account cannot access the first account's data.
4. Sign in on Android, force-stop/reopen the app, and verify its secure refresh-token restore. Use mobile data or a different Wi-Fi network and verify the HTTPS API and WSS endpoint independently.
5. Review Render and TiDB usage so their free quotas are not exceeded.

Do not treat a successful local H2 test as a database persistence or deployment test. If Flyway fails on TiDB, stop and inspect the database before retrying; never drop existing user tables to make a deployment pass.

## Android production build

Only create `mobile-app/config/production.json` after the hosted API health check succeeds. Copy [the example](./mobile-app/config/production.example.json), replace its placeholders with the actual deployed API and WebSocket URLs, and keep that local file ignored by Git. Then run:

```powershell
Set-Location 'C:\Mini project\PaySentry\mobile-app'
flutter pub get
flutter build apk --release --dart-define-from-file=config/production.json
adb install -r .\build\app\outputs\flutter-apk\app-release.apk
```

The expected release APK path is `mobile-app/build/app/outputs/flutter-apk/app-release.apk`. Do not label a debug APK built with emulator/local defaults as a cloud-ready release.

## Database migration safety

Production uses Flyway and Hibernate `validate`. An empty database receives `V1__initial_schema.sql` followed by `V2__normalized_emails_and_refresh_sessions.sql`. An existing PaySentry database is baselined at version 1 and receives V2 without dropping tables. The baseline must already have all V1 tables and `users.token_version`. V2 normalizes existing emails and adds a unique index; case/whitespace duplicates can make that migration fail safely. Back up and reconcile conflicts manually before migrating; never reset production data.

The old manual `database/migrations/V001__add_user_token_version.sql` is only for historical schemas that lack that field. Do not run it against a schema that already has `token_version`. Keep development seed data separate from the cloud database.

## Environment variable reference

Configure secret values only in Render's environment settings. [`.env.example`](./.env.example) contains placeholders and is not a deployable secret file. Do not publish a TiDB password, JWT key, mock key, OAuth client secret, or Firebase service-account credential.

Optional Google OAuth/Gmail analysis requires a configured Google OAuth consent screen, read-only Gmail scope, exact HTTPS callback URL `https://<actual-api-domain>.onrender.com/api/gmail/callback`, client credentials, and the server-side encryption key. FCM requires Firebase project/service-account setup. Both remain disabled until configured and tested. SMS monitoring is opt-in and depends on Android permission approval. The banking simulator is synthetic and must not be described as a real-bank integration.

## Provider references

- [Render Free instance limits and spin-down](https://render.com/docs/free)
- [Render Docker services](https://render.com/docs/docker)
- [Render outbound IP ranges](https://render.com/docs/outbound-ip-addresses)
- [Render health checks](https://render.com/docs/health-checks)
- [Render static sites](https://render.com/docs/static-sites)
- [TiDB Cloud Starter plan and free quota](https://docs.pingcap.com/tidbcloud/select-cluster-tier/)
- [TiDB Cloud Starter price/quota details](https://www.pingcap.com/tidb-cloud-starter-pricing-details/)
- [TiDB MySQL compatibility](https://docs.pingcap.com/tidbcloud/mysql-compatibility/)
- [TiDB TLS connection requirements](https://docs.pingcap.com/tidbcloud/secure-connections-to-serverless-clusters/)
- [TiDB public endpoint firewall](https://docs.pingcap.com/tidbcloud/configure-serverless-firewall-rules-for-public-endpoints/)

USE paysentry;

INSERT INTO users (
    full_name,
    email,
    password,
    role,
    active,
    trusted_devices,
    trusted_locations,
    created_at,
    updated_at
)
VALUES (
    'System Admin',
    'admin@paysentry.local',
    '$2a$10$czEh3IIYvFj9jM7Yg/5VOeLG7ba9zYz7eew3JjAq56m2He92Q2bVC',
    'ADMIN',
    TRUE,
    'Admin Laptop',
    'Mumbai',
    NOW(),
    NOW()
)
ON DUPLICATE KEY UPDATE email = email;

INSERT INTO bank_accounts (
    user_id,
    account_id,
    total_balance,
    available_balance,
    lien_amount,
    currency,
    created_at,
    updated_at
)
SELECT
    id,
    CONCAT('PS-', LPAD(10000 + id, 5, '0')),
    250000.00,
    250000.00,
    0.00,
    'INR',
    NOW(),
    NOW()
FROM users
WHERE email = 'admin@paysentry.local'
  AND NOT EXISTS (
    SELECT 1 FROM bank_accounts WHERE bank_accounts.user_id = users.id
  );

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

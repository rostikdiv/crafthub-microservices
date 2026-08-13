-- Flyway Migration V2: Seed Default Admin User

INSERT INTO users (
    id,
    email,
    password,
    first_name,
    last_name,
    phone_number,
    role,
    is_verified,
    created_at,
    updated_at
) VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'admin@milhub.ua',
    '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd0D1RPH65W1yPw6',
    'System',
    'Admin',
    '+380000000000',
    'ADMIN',
    TRUE,
    NOW(),
    NOW()
) ON CONFLICT (email) DO NOTHING;

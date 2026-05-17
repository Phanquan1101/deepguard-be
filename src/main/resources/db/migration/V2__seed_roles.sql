INSERT INTO roles (id, name)
VALUES
    (1, 'USER'),
    (2, 'ADMIN')
ON CONFLICT (id) DO NOTHING;

INSERT INTO pricing_plans (id, name, price, credits, description)
VALUES
('BASIC', 'Basic', 10000.00, 10, 'Gói test 10 credits'),
('PRO', 'Pro', 50000.00, 70, 'Gói test 70 credits'),
('PREMIUM', 'Premium', 100000.00, 150, 'Gói test 150 credits')
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name,
    price = EXCLUDED.price,
    credits = EXCLUDED.credits,
    description = EXCLUDED.description;

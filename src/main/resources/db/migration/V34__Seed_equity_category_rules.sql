-- V34__Seed_equity_category_rules.sql
-- Groceries
INSERT INTO category_rules (id, keyword, category_id, is_expense) VALUES
(gen_random_uuid(), 'Naivas', '00000000-0000-4000-a000-000000000013', true),
(gen_random_uuid(), 'Carrefour', '00000000-0000-4000-a000-000000000013', true),
(gen_random_uuid(), 'Quickmart', '00000000-0000-4000-a000-000000000013', true),
(gen_random_uuid(), 'Chandarana', '00000000-0000-4000-a000-000000000013', true)
ON CONFLICT (keyword) DO NOTHING;

-- Transport
INSERT INTO category_rules (id, keyword, category_id, is_expense) VALUES
(gen_random_uuid(), 'Uber', '00000000-0000-4000-a000-000000000002', true),
(gen_random_uuid(), 'Bolt', '00000000-0000-4000-a000-000000000002', true),
(gen_random_uuid(), 'Shell', '00000000-0000-4000-a000-000000000002', true),
(gen_random_uuid(), 'TotalEnergies', '00000000-0000-4000-a000-000000000002', true),
(gen_random_uuid(), 'Rubis', '00000000-0000-4000-a000-000000000002', true)
ON CONFLICT (keyword) DO NOTHING;

-- Subscriptions
INSERT INTO category_rules (id, keyword, category_id, is_expense) VALUES
(gen_random_uuid(), 'Netflix', '00000000-0000-4000-a000-000000000011', true),
(gen_random_uuid(), 'Spotify', '00000000-0000-4000-a000-000000000011', true),
(gen_random_uuid(), 'YouTube', '00000000-0000-4000-a000-000000000011', true),
(gen_random_uuid(), 'Prime', '00000000-0000-4000-a000-000000000011', true),
(gen_random_uuid(), 'OpenAI', '00000000-0000-4000-a000-000000000011', true),
(gen_random_uuid(), 'ChatGPT', '00000000-0000-4000-a000-000000000011', true)
ON CONFLICT (keyword) DO NOTHING;

-- Health
INSERT INTO category_rules (id, keyword, category_id, is_expense) VALUES
(gen_random_uuid(), 'Pharmacy', '00000000-0000-4000-a000-000000000004', true),
(gen_random_uuid(), 'Chemist', '00000000-0000-4000-a000-000000000004', true),
(gen_random_uuid(), 'Hospital', '00000000-0000-4000-a000-000000000004', true),
(gen_random_uuid(), 'Clinic', '00000000-0000-4000-a000-000000000004', true)
ON CONFLICT (keyword) DO NOTHING;

-- Utilities
INSERT INTO category_rules (id, keyword, category_id, is_expense) VALUES
(gen_random_uuid(), 'NCWSC', '00000000-0000-4000-a000-000000000016', true),
(gen_random_uuid(), 'Nairobi Water', '00000000-0000-4000-a000-000000000016', true),
(gen_random_uuid(), 'M-Gas', '00000000-0000-4000-a000-000000000016', true)
ON CONFLICT (keyword) DO NOTHING;

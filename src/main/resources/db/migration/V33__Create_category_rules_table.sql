-- V33__Create_category_rules_table.sql
CREATE TABLE category_rules (
    id UUID PRIMARY KEY,
    keyword VARCHAR(255) NOT NULL UNIQUE,
    category_id UUID NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    is_expense BOOLEAN NOT NULL
);

-- Initial Seeding from MpesaParser.kt
-- Utilities
INSERT INTO category_rules (id, keyword, category_id, is_expense) VALUES
(gen_random_uuid(), 'kplc', '00000000-0000-4000-a000-000000000016', true),
(gen_random_uuid(), 'tokens', '00000000-0000-4000-a000-000000000016', true),
(gen_random_uuid(), 'power', '00000000-0000-4000-a000-000000000016', true),
(gen_random_uuid(), 'jajemelo', '00000000-0000-4000-a000-000000000016', true),
(gen_random_uuid(), 'water', '00000000-0000-4000-a000-000000000016', true),
(gen_random_uuid(), 'sewerage', '00000000-0000-4000-a000-000000000016', true),
(gen_random_uuid(), 'ncwsc', '00000000-0000-4000-a000-000000000016', true),
(gen_random_uuid(), 'kiwasco', '00000000-0000-4000-a000-000000000016', true),
(gen_random_uuid(), 'mawasco', '00000000-0000-4000-a000-000000000016', true),
(gen_random_uuid(), 'nyewasco', '00000000-0000-4000-a000-000000000016', true),
(gen_random_uuid(), 'eldowas', '00000000-0000-4000-a000-000000000016', true),
(gen_random_uuid(), 'mowasco', '00000000-0000-4000-a000-000000000016', true),
(gen_random_uuid(), 'gas', '00000000-0000-4000-a000-000000000016', true),
(gen_random_uuid(), 'm-gas', '00000000-0000-4000-a000-000000000016', true),
(gen_random_uuid(), 'mgas', '00000000-0000-4000-a000-000000000016', true),
(gen_random_uuid(), 'afrigas', '00000000-0000-4000-a000-000000000016', true),
(gen_random_uuid(), 'garbage', '00000000-0000-4000-a000-000000000016', true),
(gen_random_uuid(), 'waste', '00000000-0000-4000-a000-000000000016', true),
(gen_random_uuid(), 'trash', '00000000-0000-4000-a000-000000000016', true),
(gen_random_uuid(), 'm-kopa', '00000000-0000-4000-a000-000000000016', true),
(gen_random_uuid(), 'mkopa', '00000000-0000-4000-a000-000000000016', true),
(gen_random_uuid(), 'd.light', '00000000-0000-4000-a000-000000000016', true),
(gen_random_uuid(), 'sunking', '00000000-0000-4000-a000-000000000016', true),
(gen_random_uuid(), 'bboxx', '00000000-0000-4000-a000-000000000016', true);

-- Internet
INSERT INTO category_rules (id, keyword, category_id, is_expense) VALUES
(gen_random_uuid(), 'zuku', '00000000-0000-4000-a000-000000000017', true),
(gen_random_uuid(), 'safaricom home', '00000000-0000-4000-a000-000000000017', true),
(gen_random_uuid(), 'poa internet', '00000000-0000-4000-a000-000000000017', true),
(gen_random_uuid(), 'vilcom', '00000000-0000-4000-a000-000000000017', true),
(gen_random_uuid(), 'faiba', '00000000-0000-4000-a000-000000000017', true),
(gen_random_uuid(), 'jtl', '00000000-0000-4000-a000-000000000017', true),
(gen_random_uuid(), 'jtlk', '00000000-0000-4000-a000-000000000017', true),
(gen_random_uuid(), 'wananchi', '00000000-0000-4000-a000-000000000017', true),
(gen_random_uuid(), 'mawingu', '00000000-0000-4000-a000-000000000017', true),
(gen_random_uuid(), 'starlink', '00000000-0000-4000-a000-000000000017', true),
(gen_random_uuid(), 'konnect', '00000000-0000-4000-a000-000000000017', true),
(gen_random_uuid(), 'fibre connect', '00000000-0000-4000-a000-000000000017', true),
(gen_random_uuid(), 'fiber connect', '00000000-0000-4000-a000-000000000017', true),
(gen_random_uuid(), 'airtel fibre', '00000000-0000-4000-a000-000000000017', true),
(gen_random_uuid(), 'telkom home', '00000000-0000-4000-a000-000000000017', true),
(gen_random_uuid(), 'liquid home', '00000000-0000-4000-a000-000000000017', true),
(gen_random_uuid(), 'liquid telecom', '00000000-0000-4000-a000-000000000017', true),
(gen_random_uuid(), 'data bundles', '00000000-0000-4000-a000-000000000017', true),
(gen_random_uuid(), 'data bundle', '00000000-0000-4000-a000-000000000017', true),
(gen_random_uuid(), 'offers', '00000000-0000-4000-a000-000000000017', true),
(gen_random_uuid(), 'tunukiwa', '00000000-0000-4000-a000-000000000017', true),
(gen_random_uuid(), 'internet', '00000000-0000-4000-a000-000000000017', true),
(gen_random_uuid(), 'bundles', '00000000-0000-4000-a000-000000000017', true);

-- Airtime
INSERT INTO category_rules (id, keyword, category_id, is_expense) VALUES
(gen_random_uuid(), 'airtime', '00000000-0000-4000-a000-000000000018', true),
(gen_random_uuid(), 'tingg', '00000000-0000-4000-a000-000000000018', true),
(gen_random_uuid(), 'top up', '00000000-0000-4000-a000-000000000018', true);

-- Groceries
INSERT INTO category_rules (id, keyword, category_id, is_expense) VALUES
(gen_random_uuid(), 'supermarket', '00000000-0000-4000-a000-000000000013', true),
(gen_random_uuid(), 'naivas', '00000000-0000-4000-a000-000000000013', true),
(gen_random_uuid(), 'carrefour', '00000000-0000-4000-a000-000000000013', true),
(gen_random_uuid(), 'quickmart', '00000000-0000-4000-a000-000000000013', true),
(gen_random_uuid(), 'butchery', '00000000-0000-4000-a000-000000000013', true),
(gen_random_uuid(), 'quick mart', '00000000-0000-4000-a000-000000000013', true),
(gen_random_uuid(), 'friendly 5', '00000000-0000-4000-a000-000000000013', true),
(gen_random_uuid(), 'slice city', '00000000-0000-4000-a000-000000000013', true),
(gen_random_uuid(), 'memento butchery', '00000000-0000-4000-a000-000000000013', true);

-- Dining Out
INSERT INTO category_rules (id, keyword, category_id, is_expense) VALUES
(gen_random_uuid(), 'restaurant', '00000000-0000-4000-a000-000000000015', true),
(gen_random_uuid(), 'cafe', '00000000-0000-4000-a000-000000000015', true),
(gen_random_uuid(), 'kfc', '00000000-0000-4000-a000-000000000015', true),
(gen_random_uuid(), 'java', '00000000-0000-4000-a000-000000000015', true),
(gen_random_uuid(), 'lounge', '00000000-0000-4000-a000-000000000015', true),
(gen_random_uuid(), 'chicken inn', '00000000-0000-4000-a000-000000000015', true),
(gen_random_uuid(), 'pizza inn', '00000000-0000-4000-a000-000000000015', true),
(gen_random_uuid(), 'creamy inn', '00000000-0000-4000-a000-000000000015', true),
(gen_random_uuid(), 'choma place', '00000000-0000-4000-a000-000000000015', true),
(gen_random_uuid(), 'nas n001', '00000000-0000-4000-a000-000000000015', true),
(gen_random_uuid(), 'caterers', '00000000-0000-4000-a000-000000000015', true),
(gen_random_uuid(), 'dishes', '00000000-0000-4000-a000-000000000015', true);

-- Bank
INSERT INTO category_rules (id, keyword, category_id, is_expense) VALUES
(gen_random_uuid(), 'equity', '00000000-0000-4000-a000-000000000019', true),
(gen_random_uuid(), 'co-operative', '00000000-0000-4000-a000-000000000019', true),
(gen_random_uuid(), 'bank', '00000000-0000-4000-a000-000000000019', true),
(gen_random_uuid(), 'i&m', '00000000-0000-4000-a000-000000000019', true),
(gen_random_uuid(), 'ncba', '00000000-0000-4000-a000-000000000019', true),
(gen_random_uuid(), 'boa', '00000000-0000-4000-a000-000000000019', true),
(gen_random_uuid(), 'family bank', '00000000-0000-4000-a000-000000000019', true),
(gen_random_uuid(), 'stanbic', '00000000-0000-4000-a000-000000000019', true),
(gen_random_uuid(), 'loop', '00000000-0000-4000-a000-000000000019', true),
(gen_random_uuid(), 'sidian', '00000000-0000-4000-a000-000000000019', true);

-- Loans
INSERT INTO category_rules (id, keyword, category_id, is_expense) VALUES
(gen_random_uuid(), 'loan repayment', '00000000-0000-4000-a000-000000000020', true),
(gen_random_uuid(), 'loan', '00000000-0000-4000-a000-000000000020', true),
(gen_random_uuid(), 'fuliza', '00000000-0000-4000-a000-000000000020', true),
(gen_random_uuid(), 'tala', '00000000-0000-4000-a000-000000000020', true),
(gen_random_uuid(), 'branch', '00000000-0000-4000-a000-000000000020', true);

-- Savings
INSERT INTO category_rules (id, keyword, category_id, is_expense) VALUES
(gen_random_uuid(), 'm-shwari saving', '00000000-0000-4000-a000-000000000023', true),
(gen_random_uuid(), 'mshwari saving', '00000000-0000-4000-a000-000000000023', true),
(gen_random_uuid(), 'm-shwari', '00000000-0000-4000-a000-000000000023', true),
(gen_random_uuid(), 'mshwari', '00000000-0000-4000-a000-000000000023', true),
(gen_random_uuid(), 'kcb', '00000000-0000-4000-a000-000000000023', true),
(gen_random_uuid(), 'sacco', '00000000-0000-4000-a000-000000000023', true),
(gen_random_uuid(), 'chama', '00000000-0000-4000-a000-000000000023', true),
(gen_random_uuid(), 'orokise', '00000000-0000-4000-a000-000000000023', true),
(gen_random_uuid(), 'zimele', '00000000-0000-4000-a000-000000000023', true),
(gen_random_uuid(), 'etica', '00000000-0000-4000-a000-000000000023', true),
(gen_random_uuid(), 'gulfcap', '00000000-0000-4000-a000-000000000023', true),
(gen_random_uuid(), 'cytonn', '00000000-0000-4000-a000-000000000023', true),
(gen_random_uuid(), 'arvocap', '00000000-0000-4000-a000-000000000023', true),
(gen_random_uuid(), 'lofty', '00000000-0000-4000-a000-000000000023', true),
(gen_random_uuid(), 'kuza', '00000000-0000-4000-a000-000000000023', true),
(gen_random_uuid(), 'mali', '00000000-0000-4000-a000-000000000023', true),
(gen_random_uuid(), 'ziidi', '00000000-0000-4000-a000-000000000023', true),
(gen_random_uuid(), 'kasha', '00000000-0000-4000-a000-000000000023', true),
(gen_random_uuid(), 'genghis', '00000000-0000-4000-a000-000000000023', true),
(gen_random_uuid(), 'hela imara', '00000000-0000-4000-a000-000000000023', true),
(gen_random_uuid(), 'nabo capital', '00000000-0000-4000-a000-000000000023', true),
(gen_random_uuid(), 'stima sacco', '00000000-0000-4000-a000-000000000023', true),
(gen_random_uuid(), 'police sacco', '00000000-0000-4000-a000-000000000023', true),
(gen_random_uuid(), 'unaitas', '00000000-0000-4000-a000-000000000023', true),
(gen_random_uuid(), 'mwalimu', '00000000-0000-4000-a000-000000000023', true),
(gen_random_uuid(), 'harambee', '00000000-0000-4000-a000-000000000023', true),
(gen_random_uuid(), 'kimisitu', '00000000-0000-4000-a000-000000000023', true),
(gen_random_uuid(), 'hazina sacco', '00000000-0000-4000-a000-000000000023', true),
(gen_random_uuid(), 'imarisha', '00000000-0000-4000-a000-000000000023', true),
(gen_random_uuid(), 'tower sacco', '00000000-0000-4000-a000-000000000023', true),
(gen_random_uuid(), 'waumini', '00000000-0000-4000-a000-000000000023', true),
(gen_random_uuid(), 'dry associates', '00000000-0000-4000-a000-000000000023', true),
(gen_random_uuid(), 'm-pesa saving', '00000000-0000-4000-a000-000000000023', true),
(gen_random_uuid(), 'money market', '00000000-0000-4000-a000-000000000023', true),
(gen_random_uuid(), 'fund', '00000000-0000-4000-a000-000000000023', true),
(gen_random_uuid(), 'asset', '00000000-0000-4000-a000-000000000023', true),
(gen_random_uuid(), 'mmf', '00000000-0000-4000-a000-000000000023', true);

-- Charity
INSERT INTO category_rules (id, keyword, category_id, is_expense) VALUES
(gen_random_uuid(), 'tithe', '00000000-0000-4000-a000-000000000021', true),
(gen_random_uuid(), 'offering', '00000000-0000-4000-a000-000000000021', true),
(gen_random_uuid(), 'citam', '00000000-0000-4000-a000-000000000021', true),
(gen_random_uuid(), 'church', '00000000-0000-4000-a000-000000000021', true),
(gen_random_uuid(), 'charity', '00000000-0000-4000-a000-000000000021', true),
(gen_random_uuid(), 'mosque', '00000000-0000-4000-a000-000000000021', true),
(gen_random_uuid(), 'prayer mountain', '00000000-0000-4000-a000-000000000021', true);

-- Transport
INSERT INTO category_rules (id, keyword, category_id, is_expense) VALUES
(gen_random_uuid(), 'parking', '00000000-0000-4000-a000-000000000002', true),
(gen_random_uuid(), 'kaps', '00000000-0000-4000-a000-000000000002', true),
(gen_random_uuid(), 'bolt', '00000000-0000-4000-a000-000000000002', true),
(gen_random_uuid(), 'uber', '00000000-0000-4000-a000-000000000002', true),
(gen_random_uuid(), 'taxi', '00000000-0000-4000-a000-000000000002', true),
(gen_random_uuid(), 'rubis', '00000000-0000-4000-a000-000000000002', true),
(gen_random_uuid(), 'totalenergies', '00000000-0000-4000-a000-000000000002', true),
(gen_random_uuid(), 'shell', '00000000-0000-4000-a000-000000000002', true);

-- Health
INSERT INTO category_rules (id, keyword, category_id, is_expense) VALUES
(gen_random_uuid(), 'chemist', '00000000-0000-4000-a000-000000000004', true),
(gen_random_uuid(), 'pharmacy', '00000000-0000-4000-a000-000000000004', true),
(gen_random_uuid(), 'hospital', '00000000-0000-4000-a000-000000000004', true),
(gen_random_uuid(), 'health', '00000000-0000-4000-a000-000000000004', true),
(gen_random_uuid(), 'clinic', '00000000-0000-4000-a000-000000000004', true),
(gen_random_uuid(), 'meds', '00000000-0000-4000-a000-000000000004', true),
(gen_random_uuid(), 'dental', '00000000-0000-4000-a000-000000000004', true),
(gen_random_uuid(), 'hopemed', '00000000-0000-4000-a000-000000000004', true),
(gen_random_uuid(), 'medical', '00000000-0000-4000-a000-000000000004', true);

-- Subscriptions
INSERT INTO category_rules (id, keyword, category_id, is_expense) VALUES
(gen_random_uuid(), 'netflix', '00000000-0000-4000-a000-000000000011', true),
(gen_random_uuid(), 'spotify', '00000000-0000-4000-a000-000000000011', true),
(gen_random_uuid(), 'showmax', '00000000-0000-4000-a000-000000000011', true),
(gen_random_uuid(), 'youtube', '00000000-0000-4000-a000-000000000011', true);

-- Shopping
INSERT INTO category_rules (id, keyword, category_id, is_expense) VALUES
(gen_random_uuid(), 'jumia', '00000000-0000-4000-a000-000000000003', true),
(gen_random_uuid(), 'leather', '00000000-0000-4000-a000-000000000003', true),
(gen_random_uuid(), 'watches', '00000000-0000-4000-a000-000000000003', true),
(gen_random_uuid(), 'perfume', '00000000-0000-4000-a000-000000000003', true),
(gen_random_uuid(), 'clothes', '00000000-0000-4000-a000-000000000003', true),
(gen_random_uuid(), 'fashion', '00000000-0000-4000-a000-000000000003', true),
(gen_random_uuid(), 'mrp', '00000000-0000-4000-a000-000000000003', true),
(gen_random_uuid(), 'miniso', '00000000-0000-4000-a000-000000000003', true),
(gen_random_uuid(), 'woolworths', '00000000-0000-4000-a000-000000000003', true),
(gen_random_uuid(), 'tushop', '00000000-0000-4000-a000-000000000003', true),
(gen_random_uuid(), 'm-pesa card', '00000000-0000-4000-a000-000000000003', true),
(gen_random_uuid(), 'canva', '00000000-0000-4000-a000-000000000003', true),
(gen_random_uuid(), 'pdfaid', '00000000-0000-4000-a000-000000000003', true);

-- Personal Care
INSERT INTO category_rules (id, keyword, category_id, is_expense) VALUES
(gen_random_uuid(), 'salon', '00000000-0000-4000-a000-000000000010', true),
(gen_random_uuid(), 'barber', '00000000-0000-4000-a000-000000000010', true),
(gen_random_uuid(), 'beauty', '00000000-0000-4000-a000-000000000010', true),
(gen_random_uuid(), 'nail bar', '00000000-0000-4000-a000-000000000010', true);

-- Maintenance
INSERT INTO category_rules (id, keyword, category_id, is_expense) VALUES
(gen_random_uuid(), 'hardware', '00000000-0000-4000-a000-000000000027', true),
(gen_random_uuid(), 'timber', '00000000-0000-4000-a000-000000000027', true),
(gen_random_uuid(), 'maintenance', '00000000-0000-4000-a000-000000000027', true),
(gen_random_uuid(), 'repair', '00000000-0000-4000-a000-000000000027', true);

-- Government
INSERT INTO category_rules (id, keyword, category_id, is_expense) VALUES
(gen_random_uuid(), 'e-citizen', '00000000-0000-4000-a000-000000000022', true),
(gen_random_uuid(), 'kra', '00000000-0000-4000-a000-000000000022', true),
(gen_random_uuid(), 'county', '00000000-0000-4000-a000-000000000022', true);

-- Insurance
INSERT INTO category_rules (id, keyword, category_id, is_expense) VALUES
(gen_random_uuid(), 'britam', '00000000-0000-4000-a000-000000000014', true),
(gen_random_uuid(), 'nhif', '00000000-0000-4000-a000-000000000014', true),
(gen_random_uuid(), 'shif', '00000000-0000-4000-a000-000000000014', true),
(gen_random_uuid(), 'insurance', '00000000-0000-4000-a000-000000000014', true),
(gen_random_uuid(), 'apa', '00000000-0000-4000-a000-000000000014', true),
(gen_random_uuid(), 'jubilee', '00000000-0000-4000-a000-000000000014', true),
(gen_random_uuid(), 'sanlam', '00000000-0000-4000-a000-000000000014', true),
(gen_random_uuid(), 'cic', '00000000-0000-4000-a000-000000000014', true),
(gen_random_uuid(), 'old mutual', '00000000-0000-4000-a000-000000000014', true),
(gen_random_uuid(), 'icea lion', '00000000-0000-4000-a000-000000000014', true),
(gen_random_uuid(), 'madison', '00000000-0000-4000-a000-000000000014', true),
(gen_random_uuid(), 'apollo', '00000000-0000-4000-a000-000000000014', true),
(gen_random_uuid(), 'ga insurance', '00000000-0000-4000-a000-000000000014', true),
(gen_random_uuid(), 'heritage', '00000000-0000-4000-a000-000000000014', true),
(gen_random_uuid(), 'geminia', '00000000-0000-4000-a000-000000000014', true),
(gen_random_uuid(), 'pioneer', '00000000-0000-4000-a000-000000000014', true),
(gen_random_uuid(), 'kenindia', '00000000-0000-4000-a000-000000000014', true),
(gen_random_uuid(), 'uap', '00000000-0000-4000-a000-000000000014', true);

-- Income categories
-- Salary
INSERT INTO category_rules (id, keyword, category_id, is_expense) VALUES
(gen_random_uuid(), 'salary', 'aaaaaaaa-aaaa-4aaa-baaa-000000000001', false);

-- Bonus
INSERT INTO category_rules (id, keyword, category_id, is_expense) VALUES
(gen_random_uuid(), 'bonus', 'aaaaaaaa-aaaa-4aaa-baaa-000000000005', false);

-- Interest
INSERT INTO category_rules (id, keyword, category_id, is_expense) VALUES
(gen_random_uuid(), 'interest', 'aaaaaaaa-aaaa-4aaa-baaa-000000000008', false);

-- Other Income
INSERT INTO category_rules (id, keyword, category_id, is_expense) VALUES
(gen_random_uuid(), 'commission', 'bbbbbbbb-bbbb-4bbb-bbbb-bbbbbbbbbbbb', false),
(gen_random_uuid(), 'income', 'bbbbbbbb-bbbb-4bbb-bbbb-bbbbbbbbbbbb', false);

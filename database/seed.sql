-- =====================================================================
--  GroceryIQ demo data
--  Expiry and sale dates are relative to CURDATE(), so the dashboard
--  always shows a realistic mix of alerts no matter when you load it.
--
--  Demo logins:  admin / Admin@123   (ADMIN)
--                staff / Staff@123   (STAFF)
-- =====================================================================
USE grocery_inventory;

INSERT INTO users (username, password_hash, full_name, role) VALUES
('admin', 'pbkdf2_sha256$600000$v+NUaU/C6JWMMHh/6c+xig==$UT98W8rvCJ1ZQNWBzHmbfd0BsbvIEzViRAFZSXFDLXc=', 'Store Administrator', 'ADMIN'),
('staff', 'pbkdf2_sha256$600000$4yxI/dIQUSaX3H4NPctZxw==$4m5H9XqerTPfZO5ea/zsW9+rQwIMjxrG7i1ftSD53Jc=', 'Sam Rivera',          'STAFF');

INSERT INTO categories (id, name, description) VALUES
(1,  'Dairy & Eggs',            'Milk, cheese, yogurt, butter and eggs'),
(2,  'Fruits & Vegetables',     'Fresh produce'),
(3,  'Meat & Seafood',          'Fresh and packaged meat and fish'),
(4,  'Bakery',                  'Bread, buns and baked goods'),
(5,  'Beverages',               'Juice, soft drinks, water, coffee and tea'),
(6,  'Snacks & Confectionery',  'Chips, crackers, chocolate and candy'),
(7,  'Canned & Packaged',       'Canned goods, pasta, rice and cereal'),
(8,  'Frozen Foods',            'Frozen meals, vegetables and desserts'),
(9,  'Household & Cleaning',    'Cleaning supplies and paper products'),
(10, 'Personal Care',           'Toiletries and personal hygiene');

INSERT INTO products (id, name, category_id, quantity, price, min_stock, expiry_date) VALUES
-- Dairy & Eggs
(1,  'Whole Milk 2L',               1,  42,  5.49, 15, DATE_ADD(CURDATE(), INTERVAL 5  DAY)),
(2,  'Large Eggs (12)',             1,   8,  4.99, 12, DATE_ADD(CURDATE(), INTERVAL 18 DAY)),
(3,  'Greek Yogurt 750g',           1,  20,  6.29, 10, DATE_SUB(CURDATE(), INTERVAL 2  DAY)),
(4,  'Cheddar Cheese 400g',         1,  35,  7.99, 10, DATE_ADD(CURDATE(), INTERVAL 45 DAY)),
(5,  'Salted Butter 454g',          1,   0,  6.49,  8, DATE_ADD(CURDATE(), INTERVAL 60 DAY)),
-- Fruits & Vegetables
(6,  'Bananas (per lb)',            2,  60,  0.79, 20, DATE_ADD(CURDATE(), INTERVAL 3  DAY)),
(7,  'Gala Apples 3lb Bag',         2,  25,  5.99, 10, DATE_ADD(CURDATE(), INTERVAL 21 DAY)),
(8,  'Baby Spinach 312g',           2,  14,  4.49,  8, DATE_ADD(CURDATE(), INTERVAL 1  DAY)),
(9,  'Roma Tomatoes (per lb)',      2,  30,  1.99, 15, DATE_SUB(CURDATE(), INTERVAL 1  DAY)),
(10, 'Yellow Onions 3lb Bag',       2,  40,  3.49, 10, DATE_ADD(CURDATE(), INTERVAL 30 DAY)),
-- Meat & Seafood
(11, 'Chicken Breast 1kg',          3,  18, 14.99,  8, DATE_ADD(CURDATE(), INTERVAL 2  DAY)),
(12, 'Lean Ground Beef 454g',       3,   6,  7.49,  8, DATE_ADD(CURDATE(), INTERVAL 4  DAY)),
(13, 'Atlantic Salmon Fillet',      3,   9, 12.99,  5, DATE_SUB(CURDATE(), INTERVAL 3  DAY)),
-- Bakery
(14, 'Whole Wheat Bread',           4,  22,  3.79, 10, DATE_ADD(CURDATE(), INTERVAL 6  DAY)),
(15, 'Croissants (6)',              4,   5,  5.99,  6, DATE_ADD(CURDATE(), INTERVAL 2  DAY)),
(16, 'Bagels Plain (6)',            4,  16,  4.29,  6, DATE_ADD(CURDATE(), INTERVAL 9  DAY)),
-- Beverages
(17, 'Orange Juice 1.75L',          5,  28,  5.29, 10, DATE_ADD(CURDATE(), INTERVAL 12 DAY)),
(18, 'Sparkling Water 12-pack',     5,  50,  6.99, 15, DATE_ADD(CURDATE(), INTERVAL 300 DAY)),
(19, 'Ground Coffee 925g',          5,  12, 15.99,  5, DATE_ADD(CURDATE(), INTERVAL 180 DAY)),
-- Snacks & Confectionery
(20, 'Potato Chips 235g',           6,  45,  3.99, 15, DATE_ADD(CURDATE(), INTERVAL 90 DAY)),
(21, 'Dark Chocolate Bar 100g',     6,   4,  2.99, 10, DATE_ADD(CURDATE(), INTERVAL 200 DAY)),
(22, 'Granola Bars (8)',            6,  33,  4.79, 10, DATE_ADD(CURDATE(), INTERVAL 120 DAY)),
-- Canned & Packaged
(23, 'Spaghetti 900g',              7,  70,  2.49, 20, DATE_ADD(CURDATE(), INTERVAL 400 DAY)),
(24, 'Basmati Rice 2kg',            7,  26,  8.99, 10, DATE_ADD(CURDATE(), INTERVAL 500 DAY)),
(25, 'Diced Tomatoes 796ml',        7,  55,  1.79, 20, DATE_ADD(CURDATE(), INTERVAL 600 DAY)),
(26, 'Oat Cereal 525g',             7,   0,  5.49, 10, DATE_ADD(CURDATE(), INTERVAL 150 DAY)),
-- Frozen Foods
(27, 'Frozen Mixed Vegetables 1kg', 8,  24,  4.99, 10, DATE_ADD(CURDATE(), INTERVAL 240 DAY)),
(28, 'Pepperoni Pizza',             8,  11,  7.99,  6, DATE_ADD(CURDATE(), INTERVAL 150 DAY)),
-- Household & Cleaning (non-perishable)
(29, 'Paper Towels 6-roll',         9,  38,  9.99, 10, NULL),
(30, 'Dish Soap 709ml',             9,   7,  3.99, 10, NULL),
-- Personal Care (non-perishable)
(31, 'Toothpaste 120ml',           10,  29,  3.49, 10, NULL),
(32, 'Shampoo 650ml',              10,  15,  6.99,  5, NULL);

-- Sales over the last two weeks (unit_price is the price at the time of sale)
INSERT INTO sales (product_id, quantity_sold, unit_price, sale_date, recorded_by) VALUES
(1,  6,  5.49, CURDATE(), 2),
(6, 12,  0.79, CURDATE(), 2),
(14, 4,  3.79, CURDATE(), 2),
(20, 3,  3.99, CURDATE(), 1),
(17, 2,  5.29, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 2),
(11, 3, 14.99, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 2),
(23, 8,  2.49, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 1),
(4,  2,  7.99, DATE_SUB(CURDATE(), INTERVAL 2 DAY), 2),
(18, 5,  6.99, DATE_SUB(CURDATE(), INTERVAL 2 DAY), 2),
(2,  6,  4.99, DATE_SUB(CURDATE(), INTERVAL 3 DAY), 2),
(7,  4,  5.99, DATE_SUB(CURDATE(), INTERVAL 3 DAY), 1),
(19, 2, 15.99, DATE_SUB(CURDATE(), INTERVAL 3 DAY), 2),
(1,  9,  5.49, DATE_SUB(CURDATE(), INTERVAL 4 DAY), 2),
(12, 4,  7.49, DATE_SUB(CURDATE(), INTERVAL 4 DAY), 2),
(28, 3,  7.99, DATE_SUB(CURDATE(), INTERVAL 5 DAY), 1),
(24, 2,  8.99, DATE_SUB(CURDATE(), INTERVAL 5 DAY), 2),
(14, 6,  3.79, DATE_SUB(CURDATE(), INTERVAL 5 DAY), 2),
(6, 20,  0.79, DATE_SUB(CURDATE(), INTERVAL 6 DAY), 2),
(21, 6,  2.99, DATE_SUB(CURDATE(), INTERVAL 6 DAY), 2),
(31, 4,  3.49, DATE_SUB(CURDATE(), INTERVAL 7 DAY), 1),
(29, 3,  9.99, DATE_SUB(CURDATE(), INTERVAL 8 DAY), 2),
(5,  8,  6.49, DATE_SUB(CURDATE(), INTERVAL 9 DAY), 2),
(26, 10, 5.49, DATE_SUB(CURDATE(), INTERVAL 10 DAY), 2),
(16, 5,  4.29, DATE_SUB(CURDATE(), INTERVAL 11 DAY), 1),
(22, 7,  4.79, DATE_SUB(CURDATE(), INTERVAL 12 DAY), 2),
(10, 5,  3.49, DATE_SUB(CURDATE(), INTERVAL 13 DAY), 2);

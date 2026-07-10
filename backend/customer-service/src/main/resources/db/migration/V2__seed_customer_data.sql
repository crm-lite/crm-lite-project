INSERT INTO roles (id, code, name) VALUES (1, 'CUSTOMER', 'Customer');

-- Sample 1: Ali Yilmaz
INSERT INTO parties (id, status, created_at) VALUES (1, 'ACTIVE', now());
INSERT INTO individuals (id, party_id, first_name, middle_name, last_name, father_name, mother_name, birth_date, gender, nationality_id)
VALUES (1, 1, 'Ali', NULL, 'Yilmaz', 'Mehmet', 'Ayse', '1990-05-10', 'MALE', '10000000001');
INSERT INTO party_roles (id, party_id, role_id, status) VALUES (1, 1, 1, 'ACTIVE');
INSERT INTO customers (id, party_role_id, status, created_at) VALUES (1, 1, 'ACTIVE', now());

-- Sample 2: Ayse Demir
INSERT INTO parties (id, status, created_at) VALUES (2, 'ACTIVE', now());
INSERT INTO individuals (id, party_id, first_name, middle_name, last_name, father_name, mother_name, birth_date, gender, nationality_id)
VALUES (2, 2, 'Ayse', NULL, 'Demir', 'Ahmet', 'Fatma', '1985-08-20', 'FEMALE', '10000000002');
INSERT INTO party_roles (id, party_id, role_id, status) VALUES (2, 2, 1, 'ACTIVE');
INSERT INTO customers (id, party_role_id, status, created_at) VALUES (2, 2, 'ACTIVE', now());

-- Sample 3: Ali Can Kaya (shares first name "Ali" with sample 1, for partial-match search testing)
INSERT INTO parties (id, status, created_at) VALUES (3, 'ACTIVE', now());
INSERT INTO individuals (id, party_id, first_name, middle_name, last_name, father_name, mother_name, birth_date, gender, nationality_id)
VALUES (3, 3, 'Ali', 'Can', 'Kaya', 'Hasan', 'Zeynep', '1995-12-01', 'MALE', '10000000003');
INSERT INTO party_roles (id, party_id, role_id, status) VALUES (3, 3, 1, 'ACTIVE');
INSERT INTO customers (id, party_role_id, status, created_at) VALUES (3, 3, 'ACTIVE', now());

-- IDs above were inserted explicitly; advance the identity sequences so the next
-- JPA-generated insert doesn't collide with these seeded rows.
SELECT setval(pg_get_serial_sequence('parties', 'id'), (SELECT MAX(id) FROM parties));
SELECT setval(pg_get_serial_sequence('individuals', 'id'), (SELECT MAX(id) FROM individuals));
SELECT setval(pg_get_serial_sequence('party_roles', 'id'), (SELECT MAX(id) FROM party_roles));
SELECT setval(pg_get_serial_sequence('customers', 'id'), (SELECT MAX(id) FROM customers));

CREATE TABLE roles (
    id   BIGINT PRIMARY KEY,
    code VARCHAR(50)  NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE parties (
    id         BIGSERIAL PRIMARY KEY,
    status     VARCHAR(20) NOT NULL,
    created_at TIMESTAMP   NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE individuals (
    id              BIGSERIAL PRIMARY KEY,
    party_id        BIGINT      NOT NULL UNIQUE REFERENCES parties (id),
    first_name      VARCHAR(100) NOT NULL,
    middle_name     VARCHAR(100),
    last_name       VARCHAR(100) NOT NULL,
    father_name     VARCHAR(100),
    mother_name     VARCHAR(100),
    birth_date      DATE        NOT NULL,
    gender          VARCHAR(20) NOT NULL,
    -- Deliberately NOT globally UNIQUE: status (ACTIVE/PASSIVE) lives on customers/
    -- party_roles/parties, not on individuals, so a DB-level unique constraint here
    -- cannot express "unique among ACTIVE customers only" without a cross-table
    -- partial index. The application layer enforces that narrower rule instead
    -- (CustomerBusinessRules.checkNationalityIdIsUniqueForCreate/ForUpdate), so a
    -- nationalityId freed up by a soft-deleted (PASSIVE) customer can be reused.
    -- See docs/customer-service.md.
    nationality_id  VARCHAR(11) NOT NULL
);

CREATE TABLE party_roles (
    id       BIGSERIAL PRIMARY KEY,
    party_id BIGINT      NOT NULL REFERENCES parties (id),
    role_id  BIGINT      NOT NULL REFERENCES roles (id),
    status   VARCHAR(20) NOT NULL
);

CREATE TABLE customers (
    id            BIGSERIAL PRIMARY KEY,
    party_role_id BIGINT      NOT NULL UNIQUE REFERENCES party_roles (id),
    status        VARCHAR(20) NOT NULL,
    created_at    TIMESTAMP   NOT NULL,
    updated_at    TIMESTAMP
);

-- Speeds up the case-insensitive partial name search required by FR-CUST-01.
-- NOTE: a plain functional index on lower(...) helps prefix/equality lookups but
-- does not make '%term%' substring LIKE queries index-friendly; a trigram index
-- (pg_trgm extension) would be needed for that at real data volume.
CREATE INDEX idx_individuals_first_last_name ON individuals (lower(first_name), lower(last_name));

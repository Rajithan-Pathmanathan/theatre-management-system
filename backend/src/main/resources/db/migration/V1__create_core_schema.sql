-- Phase 1 core schema: movie catalogue, show scheduling, hybrid seat
-- allocation, bookings (online + counter), payments, and check-in.
-- Mirrors the JPA entities in com.vijaya.theatre.{movie,show,booking,payment,validation,auth}
-- exactly — ddl-auto is "validate", so this migration is the one source of truth for the schema.

-- No pgcrypto needed: primary keys are UUIDs generated application-side by
-- Hibernate (GenerationType.UUID), not by a database-side default.

-- =====================================================================
-- movies (FR-01–FR-03)
-- =====================================================================
create table movies (
    id                uuid primary key,
    title             varchar(255) not null,
    language          varchar(255) not null,
    genre             varchar(255) not null,
    duration_minutes  integer not null,
    poster_url        varchar(255),
    synopsis          text,
    status            varchar(20) not null,
    created_at        timestamptz not null,
    updated_at        timestamptz not null
);

-- =====================================================================
-- seats (FR-09) — fixed ~300-seat ODC hall layout, independent of any show
-- =====================================================================
create table seats (
    id            uuid primary key,
    seat_number   varchar(10) not null unique,
    row_label     varchar(5) not null,
    seat_in_row   integer not null,
    zone          varchar(10) not null
);

-- =====================================================================
-- shows (FR-04–FR-08, BR-01)
-- =====================================================================
create table shows (
    id                          uuid primary key,
    movie_id                    uuid not null references movies(id),
    show_date                   date not null,
    start_time                  time not null,
    status                      varchar(20) not null,
    adult_price                 numeric(10, 2) not null check (adult_price >= 0),
    child_price                 numeric(10, 2) not null check (child_price >= 0),
    guaranteed                  boolean not null default false,
    online_booking_enabled      boolean not null default false,
    online_allocation_percent   integer not null default 0 check (online_allocation_percent between 0 and 100),
    has_interval                boolean not null default true,
    created_at                  timestamptz not null,
    updated_at                  timestamptz not null
);

create index idx_shows_movie_id on shows(movie_id);
create index idx_shows_show_date on shows(show_date);

-- =====================================================================
-- show_seat_allocations (FR-09–FR-12, FR-19, BR-03–BR-06)
-- the single most concurrency-sensitive table in the system
-- =====================================================================
create table show_seat_allocations (
    id             uuid primary key,
    show_id        uuid not null references shows(id),
    seat_id        uuid not null references seats(id),
    channel        varchar(10) not null,
    status         varchar(10) not null default 'AVAILABLE',
    locked_until   timestamptz,
    lock_token     uuid,
    version        bigint not null default 0,
    constraint uk_show_seat unique (show_id, seat_id)
);

create index idx_show_seat_allocations_show_id on show_seat_allocations(show_id);
-- speeds up the expired-lock sweep job (BR-03)
create index idx_show_seat_allocations_locked_until on show_seat_allocations(locked_until) where status = 'LOCKED';

-- =====================================================================
-- staff_accounts (UC-01–UC-03) — staff/admin only, customers never get one
-- =====================================================================
create table staff_accounts (
    id             uuid primary key,
    username       varchar(255) not null unique,
    password_hash  varchar(255) not null,
    full_name      varchar(255) not null,
    email          varchar(255),
    phone_number   varchar(50),
    role           varchar(20) not null,
    active         boolean not null default true,
    created_at     timestamptz not null,
    updated_at     timestamptz not null
);

-- =====================================================================
-- bookings (FR-13–FR-27, BR-02, BR-04, BR-07, BR-11, BR-12, BR-17)
-- online and counter sales share this table so occupancy/reporting stay unified
-- =====================================================================
create table bookings (
    id                        uuid primary key,
    booking_reference         varchar(30) not null unique,
    show_id                   uuid not null references shows(id),
    channel                   varchar(10) not null,
    status                    varchar(20) not null,
    customer_name             varchar(255) not null,
    customer_phone            varchar(50) not null,
    customer_email            varchar(255),
    adult_count               integer not null check (adult_count >= 0),
    child_count               integer not null check (child_count >= 0),
    total_amount              numeric(10, 2) not null check (total_amount >= 0),
    qr_token                  uuid unique,
    tickets_used              integer not null default 0 check (tickets_used >= 0),
    physical_payment_method   varchar(10),
    sold_by_staff_id          uuid references staff_accounts(id),
    expires_at                timestamptz,
    confirmed_at              timestamptz,
    created_at                timestamptz not null,
    updated_at                timestamptz not null
);

create index idx_bookings_show_id on bookings(show_id);
create index idx_bookings_customer_phone on bookings(customer_phone);
-- speeds up the PENDING_PAYMENT lock-expiry sweep (BR-03)
create index idx_bookings_expires_at on bookings(expires_at) where status = 'PENDING_PAYMENT';

-- =====================================================================
-- booking_seats (FR-16, FR-26) — join table, Booking <-> ShowSeatAllocation
-- =====================================================================
create table booking_seats (
    id                         uuid primary key,
    booking_id                 uuid not null references bookings(id),
    show_seat_allocation_id    uuid not null references show_seat_allocations(id),
    constraint uk_booking_seat_allocation unique (booking_id, show_seat_allocation_id)
);

create index idx_booking_seats_booking_id on booking_seats(booking_id);

-- =====================================================================
-- payment_transactions (FR-21–FR-23, NFR-07) — one per online booking
-- =====================================================================
create table payment_transactions (
    id                       uuid primary key,
    booking_id               uuid not null unique references bookings(id),
    gateway                  varchar(10) not null,
    gateway_transaction_id   varchar(255),
    amount                   numeric(10, 2) not null,
    currency                 varchar(3) not null default 'LKR',
    status                   varchar(10) not null default 'INITIATED',
    raw_response             text,
    completed_at             timestamptz,
    created_at               timestamptz not null,
    updated_at               timestamptz not null
);

-- =====================================================================
-- booking_validations (FR-30–FR-32, BR-12, NFR-21) — check-in audit trail,
-- one row per entry event, supporting partial entry for group bookings
-- =====================================================================
create table booking_validations (
    id                      uuid primary key,
    booking_id              uuid not null references bookings(id),
    validated_by_staff_id   uuid not null references staff_accounts(id),
    tickets_validated       integer not null check (tickets_validated > 0),
    validated_at            timestamptz not null
);

create index idx_booking_validations_booking_id on booking_validations(booking_id);

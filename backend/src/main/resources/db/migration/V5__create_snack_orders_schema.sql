-- Phase 3: snack ordering (UC-18; FR-50–FR-52, FR-58–FR-63; BR-08, BR-13–BR-15).
-- order_number is sequential and customer-facing (BR-15), scoped per calendar
-- day via (order_date, order_number) rather than globally unique — a cinema
-- canteen's ticket counter naturally resets each day; a literal lifetime cap
-- of 300 orders ever would make no sense.

create table snack_orders (
    id                 uuid primary key,
    order_date         date not null,
    order_number       integer not null check (order_number between 1 and 300),
    show_id            uuid not null references shows(id),
    linked_booking_id  uuid references bookings(id),
    customer_name      varchar(255) not null,
    customer_phone     varchar(50) not null,
    pickup_timing      varchar(20) not null,
    status             varchar(20) not null default 'PLACED',
    total_amount       numeric(10, 2) not null check (total_amount >= 0),
    order_token        uuid not null unique,
    created_at         timestamptz not null,
    updated_at         timestamptz not null,
    constraint uk_snack_order_date_number unique (order_date, order_number)
);

create index idx_snack_orders_show_id on snack_orders(show_id);
create index idx_snack_orders_order_date_status on snack_orders(order_date, status);

-- =====================================================================
-- snack_order_items — item_name/unit_price are snapshotted at order time,
-- same reasoning as booking_seats not re-deriving prices from the live show.
-- =====================================================================
create table snack_order_items (
    id               uuid primary key,
    snack_order_id   uuid not null references snack_orders(id),
    snack_item_id    uuid not null references snack_items(id),
    item_name        varchar(255) not null,
    unit_price       numeric(10, 2) not null check (unit_price >= 0),
    quantity         integer not null check (quantity > 0),
    unavailable      boolean not null default false,
    constraint uk_snack_order_item unique (snack_order_id, snack_item_id)
);

create index idx_snack_order_items_order_id on snack_order_items(snack_order_id);

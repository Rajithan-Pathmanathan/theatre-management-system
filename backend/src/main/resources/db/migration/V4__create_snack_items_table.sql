-- Phase 3: snack menu management (UC-17, FR-49). Items are never deleted,
-- only toggled unavailable — orders already placed still reference them.

create table snack_items (
    id          uuid primary key,
    name        varchar(255) not null,
    price       numeric(10, 2) not null check (price >= 0),
    available   boolean not null default true,
    created_at  timestamptz not null,
    updated_at  timestamptz not null
);

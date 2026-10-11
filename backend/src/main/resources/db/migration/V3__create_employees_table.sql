-- Phase 2: basic employee record management (UC-16, FR-46–FR-48, BR-09).
-- Deliberately minimal per BR-09 — no attendance, shifts, or payroll.

create table employees (
    id                        uuid primary key,
    full_name                 varchar(255) not null,
    role                      varchar(255) not null,
    phone_number              varchar(50),
    email                     varchar(255),
    active                    boolean not null default true,
    linked_staff_account_id   uuid unique references staff_accounts(id),
    created_at                timestamptz not null,
    updated_at                timestamptz not null
);

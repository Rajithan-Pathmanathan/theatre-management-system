-- FR-09. Seeds the theatre's fixed ~300-seat ODC layout as data, not as an
-- admin CRUD workflow — this is physical hall geometry for a single-screen
-- theatre, not something that changes at runtime. 15 rows (A-O) x 20 seats.
--
-- Zones (BR-06's "front, middle, corner/side, and other areas"):
--   SIDE   seat 1-2 and 19-20 of every row (aisle seats)              — 60 seats
--   FRONT  rows A-D, interior seats                                   — 64 seats
--   MIDDLE rows E-K, interior seats                                   — 112 seats
--   BACK   rows L-O, interior seats                                   — 64 seats
-- gen_random_uuid() is a PostgreSQL 13+ builtin (pg_catalog) — no pgcrypto needed.

insert into seats (id, seat_number, row_label, seat_in_row, zone)
select
    gen_random_uuid(),
    layout.row_label || layout.seat_in_row::text,
    layout.row_label,
    layout.seat_in_row,
    case
        when layout.seat_in_row in (1, 2, 19, 20) then 'SIDE'
        when layout.row_label in ('A', 'B', 'C', 'D') then 'FRONT'
        when layout.row_label in ('L', 'M', 'N', 'O') then 'BACK'
        else 'MIDDLE'
    end
from (
    select chr(64 + r) as row_label, s as seat_in_row
    from generate_series(1, 15) as r, generate_series(1, 20) as s
) layout
order by layout.row_label, layout.seat_in_row;

-- BR-13: snacks ordered during online booking are charged with the tickets.
-- prepaid_amount is what was charged online for this order (null = pay at collection),
-- kept separately from total_amount so an item that later runs out shows as a refund.
alter table snack_orders add column prepaid_amount numeric(10, 2) check (prepaid_amount >= 0);

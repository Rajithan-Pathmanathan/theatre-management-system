package com.vijaya.theatre.auth;

/**
 * The SRS treats Theatre Manager and Admin as the same actor (Manager/Admin)
 * throughout the use cases, so there is one ADMIN role rather than separate
 * manager/admin roles.
 */
public enum StaffRole {
    ADMIN,
    COUNTER_STAFF,
    CANTEEN_STAFF
}

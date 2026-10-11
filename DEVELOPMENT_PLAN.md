# Development Plan — Integrated Cinema Theatre Ticketing and Operations Management System

**Client:** Vijaya Theatre, Batticaloa
**Project:** IM/2022/136 SDP (solo developer)
**Status:** Draft for client/supervisor verification before Phase 1 starts

This plan turns the locked requirement set into three buildable phases. It does not
re-derive requirements — every item below is traced to an FR/BR/UC/US id from the
source documents so nothing here is invented.

## 0. Source documents (locked)

All requirements come from `Client Requirement/Locked Client Requirement Documents/`:

| Document | What it supplies |
|---|---|
| `IM_2022_136 - SDP Phase I Document.pdf` | Master SRS: FR-01–FR-68, NFR-01–NFR-25, BR-01–BR-17, UC-01–UC-19, US-01–US-31, AC-01–AC-22, data entities, traceability matrix |
| `Updated_Cinema_Theatre_SRS - V2.0 - draft.pdf` | Earlier SRS baseline (superseded by the Phase I doc where they differ) |
| `Finalized_Decisions_Problem_Value_UserStories_MoSCoW.pdf` | 20 finalized business decisions (7-min→10-min seat lock superseded, hybrid %, partial entry, canteen independence, etc.) — folded into the Phase I doc's BR/FR tables |
| `User_Stories_Cinema_System.pdf` | Original 75-story catalog + MVP backlog |
| `MoSCoW_Prioritization_MVP.pdf` | Must/Should/Could/Won't feature list + the phased rollout this plan follows |
| `Problem_Statement.pdf`, `Value_Proposition_Statement.pdf` | Business framing (context only, no new requirements) |

**Note on the seat lock duration:** the original SRS/MoSCoW docs say 7 minutes; the
Finalized Decisions addendum and the Phase I SRS (BR-03, FR-19) both lock it at
**10 minutes**. The Phase I doc is the latest and most authoritative — this plan uses
10 minutes.

## 1. Phasing rationale

The MoSCoW document (§6.1 Phased Rollout) already specifies the build order the client
agreed to:

1. **Must Have** — the complete booking workflow (movie → show → seat → pay → QR →
   validate → report), delivered first.
2. **Should Have** — the analytics/decision-support layer and employee records, added
   once the core is stable.
3. **Could Have** — the canteen module, built last and independent of the first two.

This plan's three phases are exactly that rollout. The canteen module's *internal*
rules (independent ordering, QR access, pickup timing, sequential numbering — BR-08,
BR-13–BR-16) are themselves "Must Have" once you're building canteen at all, per the
Finalized Decisions addendum — but the module as a whole stays Phase 3, same as the
client's own MoSCoW ordering, because it must not block or complicate the core
booking system (BR-08).

## 2. Technology stack

No stack is specified in any locked document. Decided stack: **decoupled Spring Boot
REST API + React SPA**, chosen deliberately over a simpler server-rendered option
because this project doubles as the learning vehicle for OOP, Spring Boot, and
full-stack hosting — not just the fastest way to ship the SRS.

### Backend

| Concern | Choice | Why |
|---|---|---|
| Language/runtime | **Java 21 (LTS) + Spring Boot 3.x** | Current LTS, matches Spring Boot 3's baseline |
| Web layer | **Spring Web (REST controllers)** | Stateless JSON API; no server-rendered views — frontend is a separate SPA |
| Architecture layering | Controller → Service → Repository → Entity, with separate **DTOs** (request/response records, not entities) mapped via MapStruct or manual mappers | Standard, defensible layering; maps onto the Boundary/Controller/Entity diagrams already produced (Boundary = DTOs/API contract, Controller = REST controllers, Entity = JPA entities) |
| Persistence | **Spring Data JPA + Hibernate** over **PostgreSQL** | Relational, transactional — required for seat-lock/double-booking prevention (FR-12, NFR-02, NFR-10) |
| Migrations | **Flyway** | Versioned schema, standard practice, avoids `ddl-auto` in anything beyond local dev |
| Seat locking | `@Lock(LockModeType.PESSIMISTIC_WRITE)` on the seat/lock query inside `@Transactional`, plus a `lockedUntil` timestamp column and a `@Scheduled` sweep to release expired locks | Satisfies BR-03/FR-19/NFR-02 without a separate job queue |
| Auth (staff/admin) | **Spring Security + stateless JWT** (customers remain unauthenticated — guest checkout per UC-08) | Matches UC-01/UC-02/UC-03 role-based staff accounts |
| Validation | **Jakarta Bean Validation** (`@Valid` + constraint annotations on DTOs) | Standard input validation at the API boundary |
| API docs | **springdoc-openapi** (Swagger UI) | Self-documenting API, useful while building the React side against it |
| Payment gateway | **PayHere** (Sri Lankan gateway, sandbox mode for dev) | Matches the LK context (BR-02, FR-21); Stripe test mode is an acceptable drop-in if PayHere sandbox access is unavailable |
| QR codes | **ZXing** (`com.google.zxing`), booking reference encoded as the payload, returned as a base64 PNG or a dedicated image endpoint | FR-25, BR-11 |
| Report export | **Apache POI** (CSV/Excel) or **OpenPDF**/iText for FR-38, FR-48 | Pick CSV first (less code), add PDF only if the client specifically wants a printable municipal-style report |
| Testing | **JUnit 5 + Mockito** (unit) + **Testcontainers** (integration, real Postgres) | Standard, catches the seat-lock concurrency behavior for real rather than against an in-memory DB |

### Frontend

| Concern | Choice | Why |
|---|---|---|
| Framework | **React + TypeScript + Vite** | Standard SPA pairing for a separate backend API; no SSR needed since this isn't a public content site |
| Data fetching | **TanStack Query** (React Query) | Standard pattern for REST API consumption, caching, and the seat-lock countdown/refresh UX |
| Routing | **React Router** | Standard client-side routing for customer / staff / admin / canteen areas |
| Styling | Tailwind CSS (+ a component set such as shadcn/ui) | Fast to build an interactive seat map and dashboards with, widely used |
| Auth | JWT stored and attached via an API client interceptor | Pairs with the backend's stateless JWT auth |

### Infrastructure

| Concern | Choice | Why |
|---|---|---|
| Database hosting | **Neon or Supabase (managed PostgreSQL)**, or Railway's Postgres | Free tier, works with any JDBC-based Spring Boot deploy |
| Backend hosting | **Docker image → Render or Railway** (or a VPS later) | Matches the scope item "cloud deployment with automated build/release"; teaches containerizing a JVM app |
| Frontend hosting | **Vercel or Netlify** | Standard static/SPA hosting, separate from the backend deploy |
| CI/CD | **GitHub Actions** — build + test (backend and frontend) on every push, deploy on merge to `main` | Worth having from early on given the timeline; standard practice |

## 3. Repository structure (proposed)

Two deployable codebases in one repo (or two repos, as preferred — a monorepo with
`backend/` and `frontend/` is simplest for a solo project with one CI pipeline each).

```
theatre-management-system/
  backend/
    src/main/java/com/vijaya/theatre/
      movie/            # UC-04 — entity, repository, service, controller, dto
      show/              # UC-05, UC-06 — Show, Seat, ShowSeatAllocation
      booking/            # UC-08, UC-09, UC-12, UC-13 — Booking, BookingSeat, SeatLock
      payment/            # UC-09 — PaymentTransaction, PayHere client
      counter/            # UC-10 — physical ticket sales
      validation/         # UC-11 — BookingValidation / check-in
      reporting/           # UC-14 — Phase 1
      analytics/          # UC-15 — Phase 2
      employee/           # UC-16 — Phase 2
      canteen/            # UC-17, UC-18, UC-19 — Phase 3
      auth/               # UC-01, UC-02, UC-03 — Spring Security, JWT, StaffAccount
      common/             # shared config, exception handling, scheduled jobs
    src/main/resources/
      db/migration/       # Flyway SQL migrations
      application.yml
    src/test/java/...     # JUnit/Mockito unit tests + Testcontainers integration tests
    build.gradle (or pom.xml)
  frontend/
    src/
      pages/customer/      # UC-07, UC-08, UC-09, UC-12, UC-18 (Phase 3)
      pages/counter/        # UC-10, UC-11
      pages/canteen/         # UC-19 (Phase 3)
      pages/admin/            # UC-04, UC-05, UC-06, UC-13, UC-14, UC-15, UC-16, UC-17
      api/                    # typed API client (fetch/axios + TanStack Query hooks)
      components/
    package.json
  DEVELOPMENT_PLAN.md
```

Package-by-feature (as above), not package-by-layer (`controllers/`, `services/`,
`repositories/` as top-level folders) — this is the more standard, scalable layout
for a Spring Boot codebase of this size and reads better against the use-case list.

## 4. Core data model (introduced across phases, not all at once)

| Entity | Introduced in | Key FRs |
|---|---|---|
| Movie | Phase 1 | FR-01–FR-03 |
| Show, ShowSeatAllocation | Phase 1 | FR-04–FR-08, FR-10–FR-11 |
| Seat, SeatLock | Phase 1 | FR-09, FR-12, FR-19 |
| Booking, BookingSeat | Phase 1 | FR-13–FR-20, FR-24–FR-27 |
| PaymentTransaction | Phase 1 | FR-21–FR-23 |
| BookingValidation (check-in record) | Phase 1 | FR-30–FR-32, BR-12 |
| StaffAccount (role: admin/manager, counter, canteen) | Phase 1 | UC-01–UC-03 |
| DailyReportSnapshot (or computed view) | Phase 1 | FR-33–FR-39 |
| Employee | Phase 2 | FR-46–FR-48 |
| AnalyticsAggregate (or just query views over Booking) | Phase 2 | FR-40–FR-45 |
| SnackItem | Phase 3 | FR-49 |
| SnackOrder, SnackOrderItem | Phase 3 | FR-50–FR-65 |

---

## Phase 1 — Core Booking Workflow (Must Have)

**Goal:** a customer can browse a guaranteed show, pick exact seats, pay, and get a
QR-coded booking; a counter staff member can sell a physical ticket from the same seat
pool; theatre staff can validate a booking at the door, with partial entry support;
a manager can run the daily report. This alone is a demonstrable, deployable product.

### In scope

| Module | Use cases | Key FRs | Key BRs |
|---|---|---|---|
| Accounts & auth | UC-01 Register, UC-02 Login, UC-03 Manage Profile | — (governed by NFR-04–NFR-08) | — |
| Movie management | UC-04 | FR-01–FR-03 | — |
| Show scheduling | UC-05 | FR-04–FR-08 | BR-01 |
| Hybrid seat allocation | UC-06 | FR-09–FR-12 | BR-05, BR-06 |
| Customer browsing | UC-07 | FR-13–FR-14, FR-18 | BR-01 |
| Online booking + seat lock | UC-08 | FR-15–FR-20, FR-22 (ticket portion only) | BR-03, BR-04 |
| Payment + QR confirmation | UC-09 | FR-21, FR-23–FR-27 | BR-02, BR-11 |
| Counter ticket sales | UC-10 | FR-66–FR-68 | BR-07, BR-17 |
| Theatre-side validation | UC-11 | FR-28–FR-32 | BR-11, BR-12 |
| Booking status lookup | UC-12 | FR-29 | — |
| Booking/order management (admin) | UC-13 | — | — |
| Core reporting | UC-14 (daily/show-wise only) | FR-33–FR-39 | — |

### Explicitly deferred (do not build yet)

- Analytics dashboard (UC-15) — Phase 2
- Employee records (UC-16) — Phase 2
- Report export (FR-48 export path) — Phase 2, keep on-screen reports only
- Everything canteen-related (UC-17–UC-19, FR-49–FR-65) — Phase 3
- Anything in the SRS's "Future Enhancements" list (loyalty, coupons, multi-screen,
  predictive AI scheduling, payroll) — never in this project's scope

### Business rules to enforce from day one

- **BR-01** — online booking only for shows flagged `guaranteed`.
- **BR-02** — booking status flips to `Confirmed` only after a successful payment
  callback; never on a "pending"/"pay later" basis.
- **BR-03** — seat selection locks for exactly 10 minutes; expired locks release
  automatically (a scheduled sweep or a lazy check-on-read is enough — don't build a
  job queue for this).
- **BR-04** — seat count must equal adult + child ticket count before a lock is
  attempted.
- **BR-05/BR-06** — online seat % is configurable per show and must be distributed as
  mixed clusters across hall zones, not one block.
- **BR-07** — every physical sale is written to the same `Booking` table as online
  sales (channel = `counter`), so occupancy/reporting is unified.
- **BR-11** — one `Booking` → one reference → one QR, regardless of seat count.
- **BR-12** — a booking can be checked in partially; track `ticketsUsed` /
  `ticketsRemaining` per booking, not just a boolean "checked in" flag.
- **BR-17** — physical sales record the payment method used (cash/card/other).

### Definition of done (Phase 1)

All of AC-01 through AC-16 (Phase I doc, §9.2) pass, plus:

- [ ] Admin can create/update/deactivate a movie and a show, mark it guaranteed.
- [ ] Admin can set an online-seat % for a show and see it distributed across zones.
- [ ] Customer can browse, pick exact seats matching ticket counts, get a 10-minute
      lock, pay via the gateway's sandbox, and receive one booking reference + QR.
- [ ] A second customer cannot select a seat already locked/booked (concurrency test —
      see §7).
- [ ] Counter staff can sell a ticket against the physical allocation and it shows up
      in the same booking list as online sales.
- [ ] Staff can validate a booking by QR, reference, or phone number; a fully
      checked-in booking cannot be validated again; a partially-checked-in booking can
      admit the remaining tickets later.
- [ ] Manager can view a show-wise and daily report with ticket counts and collections
      matching the underlying bookings, split online vs. physical.
- [ ] Staff/admin auth is required for every non-public screen; passwords are hashed.

---

## Phase 2 — Analytics, Employee Records & Reporting Extensions (Should Have)

**Goal:** give the manager the decision-support and operational-record features the
original problem statement calls out, without touching the booking core.

### In scope

| Module | Use cases | Key FRs |
|---|---|---|
| Analytics & decision-support | UC-15 | FR-40–FR-45 |
| Employee records | UC-16 | FR-46–FR-48 |
| Report export | (extends UC-14) | FR-48 export path |
| Extended audit trail | (extends UC-02/UC-11/UC-13) | NFR-20, NFR-21 |

### Business rules / constraints

- **BR-09** — employee management stays basic (name, role, contact, status). No
  attendance, shifts, or payroll — those are explicitly out of scope (SRS §1.5).
- Analytics is **historical decision-support, not forecasting** — if a stakeholder or
  reviewer asks for predictive scheduling, that's a Future Enhancement, not this
  phase (see AC-17's note and UC-15 alternative flow A4).

### Definition of done (Phase 2)

- [ ] Manager dashboard shows occupancy, attendance, and channel-performance by
      showtime/date/movie, falling back to a pre-aggregated daily summary when a
      query would be slow (no need to build real-time aggregation from scratch).
- [ ] Manager can add/update/deactivate an employee record; deactivation disables any
      linked staff account.
- [ ] Daily/show reports from Phase 1 can be exported (CSV/PDF — pick whichever is
      less code; CSV is the lazy-correct choice unless the client specifically wants a
      printable PDF for the municipal workflow).
- [ ] Booking validation/check-in events remain traceable (who validated what, when).

---

## Phase 3 — Canteen Module (Could Have, independent service)

**Goal:** an optional, decoupled snack-ordering service that works with or without a
movie booking — built last so it cannot delay or complicate the booking core (BR-08).

### In scope

| Module | Use cases | Key FRs |
|---|---|---|
| Snack menu management | UC-17 | FR-49 |
| Snack ordering (booking-linked + standalone) | UC-18 | FR-50–FR-52, FR-58–FR-63 |
| Canteen order fulfilment | UC-19 | FR-53–FR-57, FR-64–FR-65 |

### Business rules

- **BR-08** — canteen must not require or assume a prior movie booking.
- **BR-13** — standalone orders don't require online payment (pay at collection);
  booking-linked snack items *do* ride the same online payment as the tickets.
- **BR-14** — pickup timing is Before Movie / During Interval / both; Interval is
  disabled for shows with no interval.
- **BR-15** — sequential customer-facing order numbers in the 1–300 range, separate
  from the internal DB id.
- **BR-16** — no automatic expiry for uncollected orders; staff close them manually.

### Definition of done (Phase 3)

- [ ] Admin can manage the snack menu (item, price, availability).
- [ ] Customer can order snacks during online booking (paid with tickets) **or**
      independently via a website link / theatre QR (no booking required, payment
      optional).
- [ ] Order gets a sequential 1–300 customer-facing number distinct from its DB id.
- [ ] Interval pickup option is hidden for shows configured with no interval.
- [ ] Canteen staff queue separates Before-Movie vs. Interval orders, can mark
      ready/collected/unavailable, and verify pickup via reference/QR.
- [ ] An unavailable item notifies the customer and adjusts the order total; an
      uncollected order sits active until a staff member manually closes it.
- [ ] Canteen sales report (FR-57) is available to the manager.

---

## 5. Cross-phase non-functional requirements (apply from Phase 1 onward)

- **Security** (NFR-04–NFR-08): staff/admin auth required everywhere; hashed
  passwords; no payment data beyond a transaction reference ever touches the DB.
- **Reliability** (NFR-09–NFR-11): a confirmed booking must never silently disappear;
  seat state must never go inconsistent under concurrent requests — this is the one
  area worth a real transaction/lock test, not just a happy-path check.
- **Usability** (NFR-12–NFR-14, NFR-22–NFR-25): responsive web only (no native app),
  simple seat-map UI, Tamil/English presentable copy where the client serves both.
- **Maintainability** (NFR-17–NFR-18): keep booking, reporting, and canteen as
  separable modules so Phase 3 can be skipped or delayed without touching Phase 1/2
  code — this is also why canteen is a separate route group in §3.

## 6. Testing approach

Since this project is explicitly also the vehicle for learning proper Spring Boot
practice, testing is treated as a real layer of the architecture, not an afterthought
— but still targeted at what's actually risky, not padded for coverage numbers:

- **Unit tests (JUnit 5 + Mockito)** — every service class with real branching logic
  (pricing calculation, seat-count validation, booking state transitions, allocation
  %-to-seat-count math). Mock the repository layer.
- **Integration tests (Testcontainers + real PostgreSQL)** — anything that touches the
  database transactionally, especially:
  - **Phase 1:** a concurrency test that fires two simultaneous booking attempts at
    the same seat and asserts only one wins (covers BR-03/FR-12/NFR-02 — the single
    highest-risk piece of this whole project). A lock-expiry test (seat frees after
    10 minutes). A payment-webhook idempotency test (duplicate callback doesn't
    double-book). Repository-level tests for the hybrid allocation split.
  - **Phase 2:** a report-totals test (sum of bookings == report total, online+
    physical split correct).
  - **Phase 3:** a pickup-timing test (interval option hidden when show has no
    interval) and a standalone-order-no-payment-required test.
- **Controller/API tests (`@WebMvcTest` or `MockMvc` + Spring Security test support)**
  — auth/role enforcement on protected endpoints (e.g. a counter-staff token cannot
  hit an admin-only endpoint), and request validation (bad payloads get a 400, not a
  500).
- **Frontend** — component/unit tests (Vitest + React Testing Library) for the seat
  map and booking summary logic specifically, since that's where UI state bugs would
  actually cost a sale; broad snapshot testing of every screen isn't worth the time.
- Wire these into the GitHub Actions CI pipeline (§2) so tests run on every push —
  this is itself part of the "how to build like a real team" learning goal.

Full manual click-through of each phase's Definition of Done checklist happens on top
of this, as final sign-off before moving to the next phase.

## 7. Deployment

- **Backend:** Dockerize the Spring Boot app; deploy the image to Render or Railway
  against a managed PostgreSQL instance (Neon/Supabase/Railway Postgres); configure
  env vars (DB URL, JWT secret, PayHere keys) per environment rather than hardcoding.
- **Frontend:** deploy the React build to Vercel or Netlify, pointed at the backend's
  deployed API URL; configure CORS on the backend accordingly.
- **CI/CD:** GitHub Actions runs backend tests (incl. Testcontainers) and frontend
  tests/build on every push; deploy jobs trigger on merge to `main` once Phase 1 is
  stable enough to have something worth auto-deploying.
- Deploy early and often (end of each meaningful slice, not just end of phase) so
  there's always a working, demoable version — useful both for client feedback and
  for having deployment experience to talk about in an interview.
- Use PayHere/Stripe **sandbox** keys throughout development; swap to live keys only
  once the client is ready to go live.

---

## Next step

This plan is awaiting your verification. Once confirmed, Phase 1 implementation
starts with: project scaffolding (backend + frontend skeletons, CI pipeline) → JPA
entity model + Flyway migration → auth (Spring Security/JWT) → movie/show CRUD → seat
layout/allocation → booking + seat lock → payment + QR → counter sales → validation →
reporting, in that order, each slice deployed and checked against its Definition of
Done before moving to the next.

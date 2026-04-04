# FinPulse — The Lending Echosystem
## Complete End-to-End Interview Preparation Guide

---

## PART 1: PROJECT OVERVIEW

**What is it?** A full-stack Loan Management System (LMS) covering the complete loan lifecycle — from customer onboarding and loan application to disbursement, EMI collection, NPA management, and loan closure.

**Purpose:** Demo project built to learn fintech domain and land a role at Pennant Technologies.

---

## PART 2: TECH STACK

### Backend

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Java | 21 |
| Framework | Spring Boot | 3.5.10 |
| Database | MySQL | 8.0 |
| ORM | Spring Data JPA + Hibernate | - |
| Security | Spring Security + JWT (JJWT) | 0.12.6 |
| Build Tool | Maven | - |
| Utilities | Lombok | - |
| File Storage | Cloudinary | - |
| Scheduler | Spring `@Scheduled` | - |
| Async | Spring `@Async` | - |

### Frontend

| Layer | Technology | Version |
|-------|-----------|---------|
| Framework | React | 19.2.0 |
| Routing | React Router | v7.13.1 |
| UI Library | Ant Design | v6.3.1 |
| HTTP Client | Axios | v1.13.6 |
| State Management | Zustand | v5.0.11 |
| Charts | Recharts | v3.7.0 |
| Build Tool | Vite | v7.3.1 |
| Date Handling | dayjs | v1.11.19 |
| Forms | Ant Form + React Hook Form | v7.71.2 |

---

## PART 3: DATABASE DESIGN

### Database
- **Name:** `money_moment`
- **Engine:** MySQL 8.0
- **DDL:** `spring.jpa.hibernate.ddl-auto=update` (auto-creates/updates schema on startup)

---

### All Entities and Key Fields

#### `customers` table (CustomerEntity)

| Field | Type | Notes |
|-------|------|-------|
| id | BIGINT PK | Auto-increment |
| customer_number | VARCHAR UNIQUE | Generated (CUST + UUID) |
| name, email, phone | VARCHAR | |
| pan, aadhar | VARCHAR | KYC fields |
| employment_type | ENUM | SALARIED, SELF_EMPLOYED, etc. |
| monthly_salary | DECIMAL | For DTI/FOIR calculation |
| credit_score | DECIMAL | Fixed 758.5 (demo) |
| is_active | BOOLEAN | Soft delete flag |
| deactivated_at | DATETIME | Set on soft delete |
| relationship_manager_id | BIGINT FK | → users |
| created_at, updated_at, created_by, updated_by | DATETIME/VARCHAR | Audit via @PrePersist/@PreUpdate |

#### `loans` table (LoanEntity)

| Field | Type | Notes |
|-------|------|-------|
| id | BIGINT PK | |
| loan_number | VARCHAR UNIQUE | Generated |
| customer_id | BIGINT FK | → customers |
| loan_type_id, loan_purpose_id, loan_status_id | FK | → master tables |
| loan_amount, interest_rate, tenure_months | DECIMAL/INT | Core loan parameters |
| emi_amount | DECIMAL | Calculated via reducing balance |
| total_interest, total_amount, outstanding_amount | DECIMAL | |
| processing_fee | DECIMAL | Calculated via ProcessingFeeConfig |
| number_of_paid_emis, number_of_overdue_emis | INT | Real-time counters |
| current_dpd, highest_dpd | INT | Days Past Due |
| total_overdue_amount, total_penalty_amount | DECIMAL | |
| last_payment_date, next_due_date | DATE | |
| applied_date, approved_date, disbursed_date, closed_date | DATE | Lifecycle dates |
| disbursement_account_number, disbursement_ifsc | VARCHAR | |
| accrued_interest, provision_amount, provision_rate | DECIMAL | EOD fields |
| npa_recovery_payment_count | INT | 3 consecutive payments → downgrade from NPA |

#### `emi_schedules` table (EmiScheduleEntity)

| Field | Type | Notes |
|-------|------|-------|
| loan_id, customer_id | FK | |
| emi_number | INT | 1 to N |
| due_date | DATE | |
| principal_amount, interest_amount, emi_amount | DECIMAL | Reducing balance breakdown |
| outstanding_principal | DECIMAL | Balance after this EMI |
| status | ENUM | PENDING, PAID, PARTIALLY_PAID, OVERDUE |
| paid_date, amount_paid, shortfall_amount | | |
| days_past_due | INT | Updated by EOD daily |

#### `emi_payments` table (EmiPaymentEntity)

| Field | Type | Notes |
|-------|------|-------|
| payment_number | VARCHAR UNIQUE | Generated |
| loan_id, customer_id, emi_schedule_id | FK | |
| payment_date, payment_amount | DATE/DECIMAL | |
| payment_mode | ENUM | NACH, UPI, NEFT, RTGS, CASH, CHEQUE |
| payment_type | ENUM | FULL, PARTIAL, EXCESS, ADVANCE |
| payment_status | ENUM | SUCCESS, FAILED, PENDING, BOUNCED |
| transaction_id, reference_number | VARCHAR | |
| excess_amount | DECIMAL | Applied to next EMI |

#### `credit_assessments` table

| Field | Type | Notes |
|-------|------|-------|
| assessment_number | VARCHAR UNIQUE | |
| credit_score, monthly_income | DECIMAL | |
| existing_emi_obligations, proposed_emi | DECIMAL | |
| dti_ratio, foir | DECIMAL | Calculated ratios |
| risk_category | ENUM | LOW, MEDIUM, HIGH |
| is_eligible | BOOLEAN | |
| recommendation | ENUM | APPROVE, REJECT, MANUAL_REVIEW |
| recommended_loan_amount | DECIMAL | |

#### `loan_approvals` table

| Field | Type | Notes |
|-------|------|-------|
| approved_by_user_id | FK | → users |
| approved_by_employee_id, approved_by_name | VARCHAR | Denormalized for audit trail |
| approval_level | INT | 1=L1, 2=L2, 3=L3, 4=L4 |
| role_code | VARCHAR | Approver's role |
| action | ENUM | APPROVE, REJECT |
| remarks | TEXT | |
| loan_amount | DECIMAL | Snapshot at approval time |

#### `disbursements` table

| Field | Type | Notes |
|-------|------|-------|
| disbursement_number | VARCHAR UNIQUE | |
| disbursement_amount, processing_fee, net_disbursement | DECIMAL | |
| beneficiary_account_number, ifsc, name | VARCHAR | |
| disbursement_mode | ENUM | |
| transaction_id, utr_number | VARCHAR | |
| status | ENUM | INITIATED, COMPLETED, FAILED |
| initiated_at, completed_at | DATETIME | |

#### `collateral_details` table

| Field | Type | Notes |
|-------|------|-------|
| collateral_number | VARCHAR UNIQUE | |
| collateral_type | ENUM | PROPERTY, VEHICLE, GOLD, FD |
| valuation_amount | DECIMAL | |
| ltv_percentage | DECIMAL | Loan-to-Value ratio |
| collateral_status | ENUM | PLEDGED, RELEASED |
| pledge_date, release_date | DATE | |

#### `loan_penalties` table

| Field | Type | Notes |
|-------|------|-------|
| penalty_code, penalty_name | VARCHAR | |
| penalty_amount, base_amount | DECIMAL | |
| days_overdue | INT | |
| is_waived | BOOLEAN | |
| waived_amount, waived_at, waiver_reason | | |
| is_paid, paid_amount, paid_date | | |
| applied_by | VARCHAR | 'SYSTEM' or employee ID |

#### `users` table

| Field | Type | Notes |
|-------|------|-------|
| user_number, employee_id | VARCHAR UNIQUE | |
| username, email | VARCHAR UNIQUE | |
| full_name, phone, department, designation | VARCHAR | |
| branch_code, region_code | VARCHAR | |
| manager_id | FK self | Org hierarchy |
| is_active | BOOLEAN | |
| roles | M2M | → user_roles junction → roles |

#### `roles` table

| Field | Type | Notes |
|-------|------|-------|
| role_code | VARCHAR UNIQUE | |
| role_name, description | VARCHAR | |
| max_approval_amount | DECIMAL | Authority limit |
| can_approve, can_recommend, can_veto | BOOLEAN | |
| approval_level | INT | 1–4 hierarchy |

#### Master Tables

| Table | Purpose |
|-------|---------|
| `loan_types` | Home Loan, Personal Loan, Car Loan, etc. + `collateral_required` flag |
| `loan_purposes` | Home Purchase, Education, Medical, etc. |
| `loan_statuses` | Status codes + descriptions |
| `tenure_masters` | Available tenures per loan type |
| `processing_fee_configs` | FLAT or PERCENTAGE fees with min/max bounds |
| `interest_rate_configs` | Rate brackets by credit score + loan amount + tenure |
| `penalty_configs` | Penalty codes, amounts, charge type |
| `document_types` | Required document types |
| `disbursement_modes` | NEFT, RTGS, IMPS, Cheque |

#### EOD Logging Tables

| Table | Purpose |
|-------|---------|
| `eod_logs` | Summary per job run — duration, phase results, metrics |
| `eod_phase_logs` | One row per phase per run — metrics stored as JSON |

---

### Key Database Design Patterns

- **Soft Delete** — `is_active` + `deactivated_at` (never hard delete financial records)
- **Audit Fields** — Every entity has `created_at`, `updated_at`, `created_by`, `updated_by` via `BaseEntity`
- **Denormalization for Audit** — Approval/disbursement tables store name + employee ID inline (audit trail survives user deletion)
- **JSON in DB** — EOD phase metrics stored as JSON string (flexible schema without extra columns)
- **JPA Specifications** — Dynamic WHERE clause building for filtered list APIs
- **Batch Queries** — `JOIN FETCH` to avoid N+1; `saveAll()` for batch writes

---

## PART 4: BACKEND ARCHITECTURE

### Package Structure

```
com.moneymoment.lending/
├── controllers/          # 16 REST controllers
├── services/             # 24 business logic services
├── repos/                # 17 repositories (JpaRepository + JpaSpecificationExecutor)
├── entities/             # 13 core JPA entities
├── dtos/                 # 90+ Request/Response DTOs
├── security/             # JwtFilter, JwtUtil, SecurityConfig
├── common/
│   ├── enums/            # LoanStatusEnums, EmiStatusEnums, PaymentStatusEnums
│   ├── exception/        # GlobalExceptionHandler + 5 custom exceptions
│   ├── response/         # ApiResponse<T>, PagedResponse<T>
│   ├── spec/             # LoanSpecification, CustomerSpecification
│   ├── utils/            # EmiCalculator, ProcessingChargeUtils, NumberGenerator, DateUtils
│   └── validation/       # AadhaarValidator, PanValidator, PhoneValidator
├── master/               # Master data entities + repos
├── crons/                # EodScheduler
└── seed/                 # DataSeederService (demo data)
```

---

### All REST API Endpoints

#### Authentication
```
POST  /api/auth/login                               Returns JWT token (24hr)
```

#### Customers
```
POST   /api/customers                               Create customer
GET    /api/customers                               List (paginated + filtered)
GET    /api/customers/{id}                          Get by ID
PUT    /api/customers/{id}                          Update
DELETE /api/customers/{id}                          Soft delete (isActive=false)
```

#### Loans
```
POST  /api/loans                                    Create loan
GET   /api/loans                                    List (filters: status, customerId, loanTypeCode)
GET   /api/loans/{id}                               Get by ID
GET   /api/loans/loan-number/{loanNumber}           Get by loan number
GET   /api/loans/customer/{customerId}              Get customer's loans
GET   /api/loans/{loanNumber}/emi-schedule          Get EMI amortization schedule
GET   /api/loans/{loanNumber}/timeline              Get full audit trail
```

#### Loan Lifecycle
```
POST  /api/credit-assessment                        Perform credit assessment
GET   /api/credit-assessment/{id}
GET   /api/credit-assessment/by-loan/{loanNumber}

POST  /api/loan-approval/approve                    Approve or reject loan
GET   /api/loan-approval/history/{loanNumber}

POST  /api/documents/upload                         Multipart file upload (Cloudinary)
POST  /api/documents/{id}/verify
GET   /api/documents/by-customer/{customerId}
GET   /api/documents/by-loan/{loanId}

POST  /api/disbursements/process                    Process disbursement (mock gateway)
POST  /api/disbursements/schedule-emis/{loanNumber} Generate EMI schedule
GET   /api/disbursements/by-loan/{loanNumber}

POST  /api/collateral/register
GET   /api/collateral/by-loan/{loanNumber}
PUT   /api/collateral/{id}/release
```

#### Repayment & Collections
```
GET   /api/emi-payments                             List payments (paginated + date filters)
POST  /api/emi-payments                             Process payment

POST  /api/penalties/apply
GET   /api/penalties/by-loan/{loanNumber}
GET   /api/penalties/by-emi/{emiScheduleId}
PUT   /api/penalties/{id}/waive
```

#### Users & Admin
```
POST/GET/PUT/DELETE  /api/users                     CRUD (no auth required)
POST  /api/users/bulk-create
POST  /api/users/{id}/assign-roles
DELETE /api/users/{id}/remove-role/{roleId}
GET   /api/users/by-role/{roleCode}
GET   /api/users/by-branch/{branchCode}
```

#### EOD & Reports
```
POST  /api/eod/run-now                              Manual trigger (ADMIN only)
GET   /api/eod/status                               Live job status
GET   /api/eod/history                              All past runs (paginated)
GET   /api/eod/history/{jobId}                      Detailed run with phase breakdown

GET   /api/reports/loan-book
GET   /api/reports/disbursements
GET   /api/reports/collections
GET   /api/reports/npa-loans
GET   /api/reports/dpd-buckets

GET   /api/masters/loan-types
GET   /api/masters/interest-rates
GET   /api/masters/processing-fees
GET   /api/masters/penalty-configs
```

#### Utility
```
GET   /api/health
POST  /api/seed/demo-data                           Seed 30 customers + 300 loans + 9,270 EMIs
GET   /api/seed/status
```

---

### Service Layer Summary

| Service | Key Responsibility |
|---------|-------------------|
| `LoanService` | Create loan, fetch with JPA Specifications |
| `CustomerService` | CRUD + soft delete + validation |
| `CreditAssessmentService` | DTI/FOIR calc, risk category, eligibility check |
| `ApprovalService` | Multi-level authority validation, approve/reject |
| `EMIScheduleGenerationService` | Reducing balance amortization, last EMI rounding |
| `DisbursementService` | Mock payment gateway, collateral validation |
| `EmiPaymentService` | 5-rule payment allocation algorithm |
| `DpdService` | DPD calculation, loan status transitions, batch optimization |
| `PenaltyService` | FLAT/PERCENTAGE penalty calc, deduplication, waiver |
| `EodService` | Orchestrates 11 EOD phases, async execution, phase logging |
| `CollateralService` | LTV calculation, pledge/release workflow |
| `LoanTimelineService` | Builds full lifecycle audit trail |
| `ReportService` | Portfolio analytics, DPD buckets, disbursement/collection reports |
| `UserService` | Login (JWT generation), user CRUD, role management |
| `DocumentsService` | Cloudinary upload, verification workflow |

---

## PART 5: KEY BUSINESS LOGIC & ALGORITHMS

### 1. EMI Calculation — Reducing Balance Method

```
EMI = [P × R × (1+R)^N] / [(1+R)^N - 1]

Where:
  P = Principal (loan amount)
  R = Monthly interest rate = Annual Rate / 12 / 100
  N = Tenure in months

Total Interest = (EMI × N) − P
Total Payable  = EMI × N
```

**Example:** ₹5,00,000 at 12.5% for 60 months
- Monthly Rate = 12.5 / 12 / 100 = 0.010417
- EMI ≈ ₹9,967
- Total Interest ≈ ₹98,000
- Total Payable ≈ ₹5,98,000

**Why reducing balance?** Each month interest is charged on the *remaining* principal, not the original amount. So month 1 interest is higher than month 60 interest.

**Last EMI Adjustment:** Rounding errors over 60 EMIs are corrected in the last EMI to ensure exact payoff.

---

### 2. Processing Fee Calculation

```
If FLAT:
  fee = fixed amount (e.g., ₹500)

If PERCENTAGE:
  fee = (loanAmount × feeValue) / 100
  fee = max(minFee, min(fee, maxFee))

Example:
  Loan ₹10,00,000 × 2% = ₹20,000
  maxFee = ₹25,000 → ₹20,000 (within bounds)
```

---

### 3. Credit Assessment — DTI / FOIR

```
DTI  = (Existing EMI + Proposed EMI) / Monthly Income × 100
FOIR = same formula (Fixed Obligation to Income Ratio)

Eligibility Rules:
  creditScore ≥ 650   AND
  DTI ≤ 50%           AND
  Monthly Income ≥ ₹15,000

Risk Category + Recommendation:
  creditScore ≥ 750  → LOW     → APPROVE
  creditScore ≥ 650  → MEDIUM  → MANUAL_REVIEW
  creditScore < 650  → HIGH    → REJECT
```

---

### 4. Multi-Level Approval Authority

```
Level   Role                    Max Loan Amount
L1      Credit Manager          ≤ ₹5 Lakhs
L2      Branch Manager          ≤ ₹20 Lakhs
L3      Regional Manager        ≤ ₹1 Crore
L4      Chief Credit Officer    > ₹1 Crore
```

Validation: Approver's `maxApprovalAmount` must be ≥ loan amount. Same user cannot approve twice.

---

### 5. DPD & Loan Status Logic

```
DPD (Days Past Due) = today − EMI due date

Loan Status Rules:
  No overdue EMIs, paid EMIs > 0    → ACTIVE
  DPD > 0 and DPD < 90             → OVERDUE
  DPD ≥ 90                         → NPA (Non-Performing Asset)
  CLOSED status is final, never changes

NPA Recovery:
  After NPA, customer needs 3 CONSECUTIVE FULL payments with DPD = 0
  → Loan downgrades to ACTIVE
  Any miss during recovery resets counter to 0
```

---

### 6. Payment Allocation Algorithm (5 Critical Rules)

The most complex business logic in the system, implementing real NBFC payment processing rules:

**Rule 1 — Oldest-Overdue-First:**
Cannot pay EMI #N if EMI #(N-1) is OVERDUE or PARTIALLY_PAID.

**Rule 2 — Penalty-First:**
Settle ALL unpaid, non-waived penalties before applying to EMI principal/interest.

**Rule 3 — Excess Handling:**
If payment > remaining EMI due → classify as EXCESS, apply surplus to next EMI.

**Rule 4 — NPA Recovery:**
Track `npaRecoveryPaymentCount`; needs 3 consecutive full payments to exit NPA.

**Rule 5 — Partial Classification:**
FULL/PARTIAL/EXCESS determined by *remaining* due amount (not original EMI amount).

**Example Payment Flow:**
```
Customer pays ₹15,000 on EMI #5
→ Check: EMI #4 is OVERDUE (30 DPD)
→ Route payment to EMI #4 first

On EMI #4:
  Penalties outstanding: ₹500 + ₹1,000 = ₹1,500
  Remaining: ₹15,000 − ₹1,500 = ₹13,500

  EMI #4 due: ₹10,000 (already paid ₹2,000 partially)
  Remaining due: ₹8,000

  ₹13,500 > ₹8,000 → EXCESS payment
  Apply ₹8,000 → EMI #4 = PAID
  Excess ₹5,500 → Applied to EMI #5 as advance
```

---

### 7. EOD Scheduler — 11 Phases (11:59 PM IST daily)

```
Phase  1  — Pre-EOD Checks         Health checks, transaction cutoff
Phase  2  — Loan Processing        DPD calc, overdue marking, penalty application
Phase  3  — Interest Accrual       Daily interest on outstanding principal
Phase  4  — NACH Processing        Payment collection, bounce handling
Phase  5  — Collections & Alerts   SMS/email alerts by DPD bucket
Phase  6  — Provisioning & GL      RBI NPA provisioning, General Ledger entries
Phase  7  — Regulatory Reporting   RBI NPA upload, CIBIL bureau report
Phase  8  — MIS Reports            Portfolio analytics, branch performance
Phase  9  — Archival               Closed loan cleanup, purge temp data
Phase 10  — Next Day Prep          Tomorrow's schedule, NACH mandates
Phase 11  — Verification           Reconciliation + EOD sign-off
```

**Architecture:**
- `EodScheduler` (@Scheduled) → `EodAsyncExecutor` (@Async) → `EodService.processEodPhases()`
- In-memory `AtomicReference<EodJobStatus>` prevents duplicate runs
- Each phase persists `EodPhaseLogEntity` with metrics as JSON
- Manual trigger via `POST /api/eod/run-now`

---

### 8. The 19,000 → 4 DB Call Optimization (EOD Phase 2)

**Problem (before optimization):**
```
300 loans × 30 EMIs each = 9,000 EMIs
  9,000 findById  (EMI lookup)
+ 9,000 save      (EMI update)
+ 271  findById   (loan lookup)
+ 813  save       (loan update)
= ~19,000 DB round-trips per EOD
```

**Solution (after optimization):**
```java
// Call 1: Load all active loans + status (JOIN FETCH)
@Query("SELECT l FROM LoanEntity l JOIN FETCH l.loanStatus WHERE l.loanStatus.code IN :codes")
List<LoanEntity> findByLoanStatusCodes(@Param("codes") List<String> codes);

// Call 2: Load ALL their EMIs (JOIN FETCH)
@Query("SELECT e FROM EmiScheduleEntity e JOIN FETCH e.loan l JOIN FETCH l.loanStatus WHERE ...")
List<EmiScheduleEntity> findEmisByLoanStatusCodes(@Param("codes") List<String> codes);

// Process in memory using streams
Map<Long, List<EmiScheduleEntity>> emisByLoan = emis.stream()
    .collect(Collectors.groupingBy(e -> e.getLoan().getId()));

// Call 3: Batch save all EMIs
emiScheduleRepository.saveAll(allEmis);

// Call 4: Batch save all loans
loanRepo.saveAll(allLoans);

// Result: 4 DB calls total
```

---

## PART 6: SECURITY IMPLEMENTATION

### JWT Token Structure

```json
{
  "sub": "username",
  "employeeId": "EMP001",
  "roles": "LOAN_OFFICER,CREDIT_MANAGER",
  "iat": 1609459200,
  "exp": 1609545600
}
```

- **Algorithm:** HMAC SHA256
- **Expiry:** 24 hours (86,400,000 ms)
- **Secret:** Injected via env var `JWT_SECRET`

### Request Filter Chain

```
Request
  → JwtFilter (OncePerRequestFilter)
  → Extract "Authorization: Bearer <token>"
  → JwtUtil.isTokenValid()
  → Extract username + roles
  → UsernamePasswordAuthenticationToken
  → SecurityContextHolder.setAuthentication()
  → Controller executes
```

### Security Config

- **Session:** STATELESS (no cookies, pure JWT)
- **CSRF:** Disabled
- **CORS:** All origins permitted (credentials=true)
- **Password:** BCryptPasswordEncoder

**Permit All (no auth needed):**
```
/api/auth/**
/api/health
/api/users/**
/api/seed/**
```

**All other routes:** Require `isAuthenticated()` or role-specific `@PreAuthorize`

### Role-Based Authorization

```
@PreAuthorize("hasAnyAuthority('LOAN_OFFICER', 'ADMIN')")  // Create loan
@PreAuthorize("hasAnyAuthority('ADMIN')")                   // EOD manual trigger
```

---

## PART 7: GLOBAL EXCEPTION HANDLING

All exceptions handled centrally in `GlobalExceptionHandler` (`@RestControllerAdvice`):

| Exception | HTTP Status |
|-----------|------------|
| `ResourceNotFoundException` | 404 |
| `ValidationException` | 400 |
| `BusinessLogicException` | 400 |
| `DuplicateRecordException` | 409 |
| All others | 500 |

All responses wrapped in `ApiResponse<T>`:

```json
{
  "success": false,
  "message": "Loan not found with number: LN001",
  "data": null,
  "timestamp": "2026-03-27T10:00:00"
}
```

---

## PART 8: FRONTEND ARCHITECTURE

### Folder Structure (74 React files)

```
src/
├── pages/           # 36 page components (route-level)
├── api/             # 17 Axios API service modules
├── components/      # 10 reusable components
├── store/           # Zustand auth store
├── routes/          # RoleGuard + route metadata config
├── layouts/         # AppLayout (sider+header) + AuthLayout
├── theme/           # Ant Design tokens + brand colors
└── utils/           # formatters, errorHandler, csvExport, constants
```

---

### All 36 Pages

| Module | Page | Route |
|--------|------|-------|
| Auth | Login | `/login` |
| Dashboard | Dashboard | `/dashboard` |
| Customers | Customer List | `/customers` |
| | New Customer | `/customers/new` |
| | Customer Detail | `/customers/:id` |
| LOS | Applications | `/los/applications` |
| | New Loan | `/los/applications/new` |
| | Loan Detail (7 tabs) | `/los/applications/:loanNumber` |
| | Credit Assessments | `/los/credit-assessments` |
| | Approvals | `/los/approvals` |
| | Documents | `/los/documents` |
| | Collaterals | `/los/collaterals` |
| LMS | Active Loans | `/lms/active-loans` |
| | EMI Schedule | `/lms/emi-schedule` |
| | Payments | `/lms/payments` |
| | Loan Closure | `/lms/closure` |
| Collections | Overdue Loans | `/collections/overdue` |
| | DPD Buckets | `/collections/dpd-buckets` |
| | Penalties | `/collections/penalties` |
| | NPA Accounts | `/collections/npa` |
| Disbursements | Disbursements | `/disbursements` |
| Advices | Receivables | `/advices/receivables` |
| | Payables | `/advices/payables` |
| Fees | Fees & Charges | `/fees` |
| EOD | EOD Scheduler | `/eod` |
| Reports | Disbursement | `/reports/disbursement` |
| | Collection | `/reports/collection` |
| | Outstanding | `/reports/outstanding` |
| | DPD / NPA | `/reports/dpd-npa` |
| | MIS | `/reports/mis` |
| Admin | Users | `/admin/users` |
| | Roles | `/admin/roles` |
| | Masters | `/admin/masters` |
| Profile | Profile | `/profile` |

---

### Loan Detail — 7 Tabs

| Tab | Content |
|-----|---------|
| Overview | Financial summary cards, loan details, customer info |
| Credit Assessment | Risk score, DTI, FOIR, recommendation, approver authority |
| Approvals | Approval history table + approve/reject form |
| Disbursement | Disbursal date, mode, UTR number, amount |
| EMI Schedule | Full amortization table (principal, interest, balance, DPD per row) |
| Documents | Upload, verify, reject documents (Cloudinary) |
| Collateral | Registered collateral details + release form |

---

### API Integration Pattern

**Axios Instance** (`src/api/axios.js`):
```javascript
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'https://money-moment-lending.onrender.com',
  headers: { 'Content-Type': 'application/json' }
})

// Request interceptor — attach JWT
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// Response interceptor — handle 401/403
api.interceptors.response.use(
  (res) => res,
  (error) => {
    if ([401, 403].includes(error.response?.status)) {
      localStorage.clear()
      // Show session modal → redirect to login
    }
    return Promise.reject(error)
  }
)
```

**API Module Pattern** (one file per domain):
```javascript
export const loanApi = {
  getAll:          (params) => api.get('/api/loans', { params }),
  getById:         (id)     => api.get(`/api/loans/${id}`),
  getByLoanNumber: (num)    => api.get(`/api/loans/loan-number/${num}`),
  create:          (data)   => api.post('/api/loans', data),
  getTimeline:     (num)    => api.get(`/api/loans/${num}/timeline`),
}
```

**Response Parsing:**
```javascript
res.data.data             // Single record
res.data?.data?.content   // Paginated list
res.data?.data?.totalElements
```

---

### Authentication Flow

```
1. POST /api/auth/login → { token, data: { username, roles, employeeId } }
2. authStore.login(token, user) → saves to Zustand + localStorage
3. ProtectedRoute checks token → redirect to /login if missing
4. RoleGuard checks allowedRoles → Ant Design 403 Result if unauthorized
5. Axios interceptor auto-attaches token to every request
6. 401/403 response → clear localStorage → session modal → /login
```

---

### State Management

**Global (Zustand):** Only auth state
```javascript
const useAuthStore = create((set) => ({
  token: localStorage.getItem('token') || null,
  user: JSON.parse(localStorage.getItem('user') || 'null'),
  login:   (token, user) => { localStorage.setItem(...); set({ token, user }) },
  logout:  () => { localStorage.clear(); set({ token: null, user: null }) },
  hasRole: (roleCode) => user?.roles?.some(r => r.roleCode === roleCode)
}))
```

**Local state:** All page data (loans, customers, filters, pagination, loading)

**Data fetch pattern:**
```javascript
useEffect(() => {
  const load = async () => {
    setLoading(true)
    try {
      const res = await loanApi.getAll({ page, size, ...filters })
      setLoans(res.data?.data?.content || [])
      setPagination({ total: res.data?.data?.totalElements })
    } catch (err) {
      showError(err)
    } finally {
      setLoading(false)
    }
  }
  load()
}, [page, size, filters])
```

---

### Dashboard — Full Breakdown

**Row 1 — Pipeline KPIs (6 cards):**
Total Customers, Total Loans, In Pipeline, Approved (pending disbursal), Disbursed (awaiting activation), Closed

**Row 2 — Portfolio Health (6 cards):**
Active Loans, Overdue Loans, NPA Accounts, Total AUM, Total Overdue Amount, PAR Ratio %

**Row 3 — Today's Collection (1 wide card, 6 metrics):**
EMIs Due Today, Collected Today, Still Pending, Amount Due, Amount Collected, Collection Efficiency %
(Green if ≥ 80%, Orange if < 80%)

**Row 4 — Charts (2 columns):**
- Loan Status Distribution (donut pie chart, Recharts)
- DPD Bucket Analysis (bar chart, 5 buckets, Recharts)

**Row 5 — Recent Loans:**
Last 5 applications — Loan No., Customer, Amount, Outstanding, Status, Applied On

**Data loading:**
```javascript
// All loaded in parallel on mount
Promise.all([
  loanApi.getAll({ page: 0, size: 500 }),
  customerApi.getAll({ page: 0, size: 1 }),
  emiScheduleApi.getAll({ dueDateFrom: today, dueDateTo: today }),
  emiScheduleApi.getAll({ dueDateFrom: today, dueDateTo: today, status: 'PAID' })
])
```

---

### Key Utility Functions

```javascript
// Currency
formatCurrency(1500000)       → ₹15,00,000.00   (Intl.NumberFormat en-IN)
formatCurrencyShort(1500000)  → ₹15L

// Dates
formatDate('2026-03-27')              → 27 Mar 2026
formatDateTime('2026-03-27T14:30:00') → 27 Mar 2026, 02:30 PM

// Timezone fix — LocalDateTime from Java has no timezone info
// Append 'Z' so browser treats it as UTC → converts to local
const toLocal = (d) => (!d.endsWith('Z') && !d.includes('+')) ? d + 'Z' : d

// Privacy masking
maskPan('ABCDE1234F')        → XXXXXX34F
maskAadhaar('123456789012')  → XXXX XXXX 9012

// CSV Export (no library needed)
exportToCsv(data, 'loan-report')   // Blob + URL.createObjectURL → browser download
```

---

### Color System

**Brand:** `#1B3A6B` (Dark Blue)

**Loan Status Colors:**
```
INITIATED       → Grey bg
UNDER_ASSESSMENT → Orange bg
APPROVED         → Blue bg
DISBURSED        → Purple bg
ACTIVE           → Green bg
OVERDUE          → Red bg
NPA              → Dark red bg, white text
CLOSED           → Light grey bg
```

**DPD Bucket Colors:**
```
0 DPD     (Current) → #52c41a  Green
1-30 DPD  (SMA-0)   → #faad14  Amber
31-60 DPD (SMA-1)   → #fa8c16  Orange
61-90 DPD (SMA-2)   → #f5222d  Red
90+ DPD   (NPA)     → #820014  Dark Red
```

---

### Reusable Components (10)

| Component | Purpose |
|-----------|---------|
| `ProtectedRoute` | Redirects to `/login` if no JWT token |
| `RoleGuard` | Shows Ant 403 Result if user lacks required role |
| `PageHeader` | Page title + breadcrumbs + action buttons |
| `DataTable` | Ant Table wrapper with pagination + sorting |
| `KPICard` | Metric card with icon + value + trend |
| `StatusBadge` | Colored status tag (maps status code → color) |
| `ConfirmModal` | Ant Modal confirmation dialog |
| `ErrorBoundary` | React class component error boundary |
| `EmptyState` | No-data placeholder with illustration |
| `ComingSoon` | Feature placeholder for future pages |

---

## PART 9: FULL LOAN LIFECYCLE (End to End)

```
Status          Trigger                              What Happens
─────────────────────────────────────────────────────────────────────────────
INITIATED     → Customer applies                   EMI calculated, loan number generated
               ↓
UNDER_ASSESSMENT → Documents uploaded & verified   Credit assessment checks DTI, FOIR, risk
               ↓
APPROVED      → Approver reviews assessment        L1/L2/L3/L4 authority check, approve/reject
               ↓
DISBURSED     → Collateral registered (if needed)  Mock payment gateway (90% success)
                 Disbursement processed             EMI schedule generated (first EMI: +30 days)
               ↓
ACTIVE        → First EMI paid, DPD = 0
               ↓
OVERDUE       → Missed EMI, DPD > 0 (< 90)        Penalties applied by EOD, alerts sent
               ↓
NPA           → DPD ≥ 90                           RBI provisioning, CIBIL report updated
               ↓ (after 3 consecutive full payments)
ACTIVE        → NPA recovery                       Loan rehabilitated
               ↓
CLOSED        → Full outstanding settled           Collateral released, loan archived
```

---

## PART 10: DEPLOYMENT

### Local Development

```bash
# Backend (port 1992)
./mvnw spring-boot:run

# Frontend
npm run dev
# Set VITE_API_BASE_URL=http://localhost:1992 in .env.local
```

**Backend config:**
- DB: `localhost:3306/money_moment`, user: `root`
- DDL: `update` (auto schema)
- SQL logging: enabled

### Production (Render + Docker)

**Backend environment variables:**
```
DB_URL=jdbc:mysql://<host>/money_moment
DB_USERNAME=...
DB_PASSWORD=...
JWT_SECRET=...
CLOUDINARY_CLOUD_NAME=...
CLOUDINARY_API_KEY=...
CLOUDINARY_API_SECRET=...
```

**Frontend environment variable:**
```
VITE_API_BASE_URL=https://money-moment-lending.onrender.com
```

---

## PART 11: DEMO / SEED DATA

```
POST /api/seed/demo-data   (no auth required, idempotent)

Creates:
  30  customers
  300 loans
        180 ACTIVE
         60 OVERDUE
         30 NPA
         30 CLOSED
  9,270 EMI records (7-10 EMIs due per day in current month)

Skips if 20+ customers already exist.
```

---

## PART 12: TOP INTERVIEW TALKING POINTS

### Fintech Domain Concepts to Explain Clearly

| Concept | One-Line Explanation |
|---------|---------------------|
| **DTI / FOIR** | Monthly debt obligations as % of income; max 50% for eligibility |
| **Reducing Balance** | Interest charged on remaining principal, not original amount |
| **DPD** | Days Past Due — number of days an EMI is overdue |
| **NPA** | Non-Performing Asset — loan with DPD ≥ 90 days (RBI standard) |
| **PAR Ratio** | Portfolio at Risk — % of total AUM that is overdue |
| **LTV** | Loan-to-Value — loan amount as % of collateral value |
| **NACH** | National Automated Clearing House — auto-debit mandate for EMI |
| **CIBIL** | India's credit bureau — NPA loans reported here |
| **Provisioning** | RBI requires banks to set aside funds against NPA loans |
| **SMA** | Special Mention Account — early warning stage before NPA |

### Technical Decisions to Defend

**Q: Why Zustand over Redux?**
Auth is the only global state. Zustand is 50x less boilerplate, no reducers/actions needed.

**Q: How do you handle JWT expiry?**
Axios response interceptor catches 401/403, clears localStorage, shows session modal, redirects to login.

**Q: Why REQUIRES_NEW transaction in penalties?**
EOD processes 300 loans. If one penalty fails, isolating it prevents rolling back the entire EOD job.

**Q: How did you reduce EOD from 19,000 to 4 DB calls?**
JOIN FETCH to batch-load all loans + EMIs in 2 queries, compute everything in Java memory, then batch save with saveAll().

**Q: Why soft delete for customers?**
Financial regulations require audit trails. Deleting a customer would orphan loan/payment history.

**Q: Why denormalize approver name in loan_approvals?**
If the approver's user account is deactivated, approval history must still be readable.

**Q: How does NPA recovery work?**
3 consecutive full EMI payments with DPD = 0 are required. Any missed payment resets the counter. This mirrors RBI's NPA upgrade norms.

**Q: How do you prevent overpaying a penalized EMI?**
Payment allocation settles penalties first, then applies remainder to EMI. If remainder > remaining EMI due, the excess goes to the next EMI.

**Q: How do you format Indian currency in the frontend?**
`Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' })` + custom short format helper for ₹15L, ₹1.2Cr.

**Q: How do you handle Java LocalDateTime timezone in React?**
Spring sends bare `LocalDateTime` without timezone info. The formatter appends 'Z' so the browser treats it as UTC and converts to local time.

---

## PART 13: SYSTEM STATS

| Metric | Count |
|--------|-------|
| Total Java files | ~133 |
| REST controllers | 16 |
| Service classes | 24 |
| Repositories | 17 |
| JPA entities | 13 core + 8 master + 2 EOD |
| DTOs | 90+ |
| React pages | 36 |
| API modules (frontend) | 17 |
| Reusable components | 10 |
| API endpoints | ~60+ |
| EOD phases | 11 |
| Demo data | 30 customers, 300 loans, 9,270 EMIs |

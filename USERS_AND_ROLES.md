# FinPulse — Users & Roles Reference

> **Bank:** FinPulse Lending | **Total Users:** 16 | **Branches:** Mumbai, Delhi, Bangalore

---

## Role Definitions — What They Do in a Real Bank

### Approval Roles (Sanctioning Authority)

| Role | Approval Level | Max Sanction Limit | Can Approve | Can Recommend | Can Veto |
|------|---------------|-------------------|-------------|---------------|----------|
| Credit Manager | L1 | ₹5,00,000 | Yes | No | No |
| Branch Manager | L2 | ₹20,00,000 | Yes | No | No |
| Regional Manager | L3 | ₹1,00,00,000 | Yes | No | No |
| Chief Credit Officer | L4 | ₹5,00,00,000 | Yes | No | No |

### Operational Roles (Processing & Support)

| Role | Can Approve | Can Recommend | Can Veto |
|------|-------------|---------------|----------|
| Loan Officer | No | Yes | No |
| Credit Analyst | No | Yes | No |
| Risk Manager | No | No | Yes |
| Compliance Officer | No | No | Yes |
| Operations Manager | No | No | No |
| System Administrator | No | No | No |

---

## What Each Role Does in Real Life

### Loan Officer
- First point of contact for the customer
- Collects loan application, income proof, KYC documents
- Does initial eligibility check and explains loan products
- Creates the loan record in the system and initiates processing
- Cannot approve — can only recommend and forward

### Credit Analyst
- Studies the customer's financial profile: salary, existing EMIs, liabilities
- Calculates DTI (Debt-to-Income) and FOIR (Fixed Obligation to Income Ratio)
- Prepares the credit assessment report
- Recommends approval or rejection with detailed justification
- Cannot approve — recommendation goes to Credit Manager / Branch Manager

### Risk Manager
- Assesses the overall risk of lending to the customer
- Checks collateral valuation and LTV (Loan-to-Value ratio)
- Flags high-risk accounts or suspicious applications
- Has **veto power** — can block a loan even if credit analyst recommends it
- Monitors portfolio-level risk (NPA trends, overdue patterns)

### Compliance Officer
- Ensures the loan process follows RBI guidelines and internal policies
- Checks KYC compliance, anti-money laundering (AML) rules
- Reviews documentation completeness before disbursement
- Has **veto power** — can block disbursement if compliance is not met
- Files regulatory reports (SMA, NPA classification)

### Credit Manager _(L1 Approver)_
- Reviews credit assessment submitted by the Credit Analyst
- Sanctions loans up to **₹5 Lakh** independently
- For loans above ₹5L, prepares recommendation and escalates to Branch Manager
- Day-to-day loan approval authority for small retail loans (personal, consumer)

### Branch Manager _(L2 Approver)_
- Sanctions loans up to **₹20 Lakh** independently
- Final authority for all branch-level loan decisions within their limit
- Oversees branch operations, customer escalations, and staff performance
- Escalates loans above ₹20L to Regional Manager

### Regional Manager _(L3 Approver)_
- Sanctions loans up to **₹1 Crore**
- Oversees multiple branches in a geographic region
- Reviews escalated cases from multiple Branch Managers
- Monitors regional NPA levels and collection efficiency

### Chief Credit Officer _(L4 Approver)_
- Highest credit authority — sanctions loans above ₹1 Crore up to ₹5 Crore
- Sets credit policy for the entire organization
- Final escalation point for large corporate or high-value loans
- Reviews portfolio health, approves policy exceptions

### Operations Manager
- Handles disbursement processing after loan approval
- Verifies bank account details, NACH mandate setup
- Coordinates with finance team for fund transfer
- Manages EMI collection operations and payment posting
- No approval or veto authority — purely execution role

### System Administrator
- Manages user accounts, role assignments, and access control
- Configures master data: loan types, interest rates, processing fees, tenure options
- Monitors system health and can trigger manual EOD
- No lending authority — back-office IT/admin function

---

## Users by Branch

### Mumbai Branch — `BR_MUM_001`

| Emp ID | Name | Role | What They Do |
|--------|------|------|--------------|
| HDFC_EMP001 | Manu | Branch Manager + System Admin | Approves loans up to ₹20L. Also manages system config and user access. |
| HDFC_EMP002 | Rajesh Kumar | Branch Manager | Approves loans up to ₹20L. Oversees all Mumbai branch lending operations. |
| HDFC_EMP005 | Amit Desai | Loan Officer | Handles customer walk-ins, collects applications and documents, initiates loans. |
| HDFC_EMP006 | Neha Patil | Credit Analyst | Analyzes customer financials, prepares credit assessment, recommends to manager. |
| HDFC_EMP007 | Vikram Mehta | Risk Manager | Reviews collateral, flags high-risk cases, can veto any loan on risk grounds. |
| HDFC_EMP008 | Pooja Joshi | Operations Manager | Processes disbursements, manages EMI collections, coordinates fund transfers. |

### Delhi Branch — `BR_DEL_001`

| Emp ID | Name | Role | What They Do |
|--------|------|------|--------------|
| HDFC_EMP003 | Priya Sharma | Branch Manager | Approves loans up to ₹20L. Heads the Delhi branch lending team. |
| HDFC_EMP009 | Rohit Gupta | Loan Officer | Handles customer applications and document collection for Delhi customers. |
| HDFC_EMP010 | Anjali Singh | Credit Analyst | Studies creditworthiness of Delhi applicants, prepares assessment reports. |
| HDFC_EMP011 | Karan Verma | Credit Manager | Sanctions loans up to ₹5L independently. First approver in the credit chain. |
| HDFC_EMP012 | Divya Chopra | Compliance Officer | Checks RBI/KYC compliance, can veto disbursement if documentation is incomplete. |

### Bangalore Branch — `BR_BLR_001`

| Emp ID | Name | Role | What They Do |
|--------|------|------|--------------|
| HDFC_EMP004 | Suresh Nair | Branch Manager | Approves loans up to ₹20L. Heads the Bangalore branch. |
| HDFC_EMP013 | Arjun Rao | Loan Officer | Handles customer onboarding and loan applications in Bangalore. |
| HDFC_EMP014 | Deepa Krishna | Credit Analyst | Prepares credit assessment reports for Bangalore loan applicants. |
| HDFC_EMP015 | Manoj Pillai | Risk Manager | Assesses loan risk in Bangalore, monitors collateral quality, holds veto power. |
| HDFC_EMP016 | Kavya Reddy | Operations Manager | Manages disbursements and EMI operations for the Bangalore branch. |

---

## Loan Approval Flow (Real Bank Process)

```
Customer Applies
      |
      v
Loan Officer (EMP005 / EMP009 / EMP013)
  - Creates loan application
  - Collects KYC + income documents
      |
      v
Credit Analyst (EMP006 / EMP010 / EMP014)
  - Reviews financials
  - Calculates DTI, FOIR, credit score
  - Recommends: Approve / Reject
      |
      v
Risk Manager / Compliance Officer  [optional veto]
  - Risk check on collateral and applicant profile
  - Compliance check on KYC and AML
      |
      v
Sanctioning Authority (based on loan amount):
  ≤ ₹5L   → Credit Manager (Karan Verma, EMP011)
  ≤ ₹20L  → Branch Manager (Rajesh/Priya/Suresh, EMP002/003/004)
  ≤ ₹1Cr  → Regional Manager [not yet created]
  > ₹1Cr  → Chief Credit Officer [not yet created]
      |
      v
Operations Manager (EMP008 / EMP016)
  - Verifies disbursement details
  - Processes fund transfer
  - Sets up EMI schedule + NACH mandate
      |
      v
Loan ACTIVE — EMI collection begins
```

---

## Gaps in Current User Setup

| Missing Role | Why Needed |
|-------------|------------|
| Regional Manager | No one can currently approve loans between ₹20L–₹1Cr |
| Chief Credit Officer | No one can approve loans above ₹1Cr |
| Loan Officer in Mumbai | Amit Desai handles Mumbai alone — single point of failure |

---

_Last updated: 2026-03-09_

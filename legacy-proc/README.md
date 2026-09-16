# Legacy Pro\*C module

This directory contains an extract of the legacy billing chain, written in
Oracle Pro\*C (C with embedded SQL, precompiled by `proc` before `cc`).

## Read only

There is **no Pro\*C build environment in this sandbox**. These files are here
for comprehension, documentation and impact analysis only. You are not expected
to compile them, and the agent must not be allowed to modify them.

Lab 1 walks through setting up a guardrail (`permissions.yaml`) that blocks
writes to `*.pc` files, precisely so this stays true.

## What the two programs do

| File | Role |
| --- | --- |
| `billing_batch.pc` | Nightly batch: recomputes tier, discount, VAT and total for pending invoices, writes the SOX audit trail |
| `customer_load.pc` | Daily load of customer reference data from the CRM export, refreshes the rolling 12-month revenue |

## Why it matters for the modern stack

`billing_batch.pc` is the historical reference implementation of the billing
rules. The Java service in `../java-service` reimplements the same rules. The
two must agree, line for line, on:

- the commercial tier thresholds
- the order of operations (discount first, then VAT)
- the rounding applied to each monetary amount

Any divergence produces invoices that reconcile differently between the batch
and the API. Finding such a divergence is one of the Lab 1 exercises.

## Constraints to respect

- **Bind variable names** (`:v_invoice_no`, `:v_cust_id`, ...) are referenced by
  the operations monitoring scripts. Renaming one silently breaks reconciliation.
- **The audit log insert** in `billing_batch.pc` is mandatory for SOX compliance.
  It must stay inside the transaction, before the `COMMIT`, and unconditional.

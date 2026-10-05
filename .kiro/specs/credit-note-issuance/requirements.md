# Requirements Document

## Introduction

This feature adds credit note issuance to the EMG billing system. A credit note is a document that partially or fully reverses an existing invoice. It must reference the original invoice, apply the same calculation rules (discount, VAT, rounding), and produce negative amounts that offset the original charges. The feature extends the existing Java/Spring Boot REST API and must stay aligned with the legacy Pro*C batch calculation order defined in `docs/billing-rules.md`.

## Glossary

- **Credit_Note**: A billing document that negates part or all of an existing invoice. Amounts are expressed as negative values.
- **Original_Invoice**: The invoice being reversed, identified by its invoice number.
- **CreditNoteCalculator**: The service component that computes credit note amounts following the same rules as `InvoiceCalculator`.
- **CreditNoteController**: The REST controller that exposes credit note endpoints.
- **CreditNoteService**: The application service that orchestrates credit note creation and storage.
- **InvoiceStore**: The in-memory or persistent store that holds issued invoices and makes them retrievable by invoice number.
- **Reversal_Lines**: The subset of lines from the original invoice that are being credited, each with a quantity between 1 and the original line quantity.
- **Partial_Credit**: A credit note that reverses only some lines, or only part of a line's quantity, of the original invoice.
- **Full_Credit**: A credit note that reverses all lines and quantities of the original invoice.

---

## Requirements

### Requirement 1: Invoice Persistence

**User Story:** As a billing operator, I want issued invoices to be retrievable by invoice number, so that a credit note can be validated against its original invoice.

#### Acceptance Criteria

1. WHEN an invoice is successfully created via `POST /api/invoices/{customerId}`, THE InvoiceStore SHALL persist the invoice before the POST response is returned.
2. WHEN an invoice is persisted, THE InvoiceStore SHALL make it retrievable by its invoice number for all subsequent lookups.
3. IF an invoice number is provided to the InvoiceStore and that invoice exists, THEN THE InvoiceStore SHALL return the complete Invoice object as originally persisted.
4. IF an invoice number is provided to the InvoiceStore and that invoice does not exist, THEN THE InvoiceStore SHALL throw an exception indicating no invoice exists for that number.

---

### Requirement 2: Credit Note Creation

**User Story:** As a billing operator, I want to issue a credit note against an existing invoice, so that I can fully or partially reverse charges already billed to a customer.

#### Acceptance Criteria

1. WHEN a valid credit note request is received for an existing invoice number, THE CreditNoteController SHALL accept the request at `POST /api/credit-notes/{invoiceNumber}`, where the request specifies either a list of invoice line identifiers to partially reverse or a full-reversal indicator, and the credited quantity for each line is a positive integer between 1 and the original invoiced quantity inclusive.
2. THE CreditNoteCalculator SHALL compute credit note amounts using the same calculation order as `InvoiceCalculator`: net amount → discount → VAT → total.
3. THE CreditNoteCalculator SHALL express all computed monetary amounts (net amount, discount amount, VAT amount, and total) as negative values in the resulting Credit_Note.
4. THE CreditNoteCalculator SHALL apply the discount rate derived from the original customer's tier at the time of the original invoice.
5. THE CreditNoteCalculator SHALL apply the VAT rate for the customer's country code from the original invoice.
6. THE CreditNoteCalculator SHALL round all monetary amounts to 2 decimal places using HALF_UP rounding, at each computation step.
7. WHEN computing a partial credit note, THE CreditNoteCalculator SHALL recalculate all monetary amounts (net, discount, VAT, total) independently from scratch using only the credited lines and quantities, applying HALF_UP rounding to 2 decimal places at each step, without deriving any amount from the original invoice's already-rounded values.
8. WHEN a credit note is successfully computed, THE CreditNoteService SHALL assign a unique credit note number using the format `CN-XXXXX` (zero-padded 5-digit sequential number starting from 00001), where no two persisted credit notes share the same credit note number.
9. WHEN a credit note is successfully computed, THE CreditNoteService SHALL persist the credit note linked to the originating invoice number and return the persisted credit note, including its assigned credit note number, in the response.
10. IF the invoice number in the request does not correspond to an existing invoice, THEN THE CreditNoteController SHALL reject the request with a `404 Not Found` error response indicating the invoice was not found, without creating any credit note.
11. IF a full-reversal credit note already exists for the requested invoice number, THEN THE CreditNoteController SHALL reject the request with a `409 Conflict` error response indicating the invoice has already been fully reversed, without creating a new credit note.
12. IF the credited quantity for any requested line exceeds the quantity remaining eligible for reversal on that line, THEN THE CreditNoteCalculator SHALL reject the request with a `400 Bad Request` error response indicating which line exceeds the reversible quantity, without persisting any credit note.

---

### Requirement 3: Partial Credit Note

**User Story:** As a billing operator, I want to credit only selected lines or quantities from an original invoice, so that I can make targeted corrections without reversing the entire invoice.

#### Acceptance Criteria

1. WHEN a credit note request specifies a subset of lines from the original invoice, THE CreditNoteCalculator SHALL compute the credited amount for each specified line as the unit price multiplied by the credited quantity, and the total credited amount as the sum of all credited line amounts.
2. WHEN a credit note request specifies a quantity for a line, THE CreditNoteCalculator SHALL use that quantity instead of the original line quantity when computing the credited line amount.
3. IF a requested line reference does not exist in the original invoice, THEN THE CreditNoteController SHALL return a `400 Bad Request` response with an error message indicating the unrecognized line reference, without modifying any existing invoice or credit note data.
4. IF a requested line quantity exceeds the quantity on the corresponding original invoice line, THEN THE CreditNoteController SHALL return a `400 Bad Request` response with an error message indicating the quantity limit exceeded, without modifying any existing invoice or credit note data.
5. IF a requested line quantity is less than 1, THEN THE CreditNoteController SHALL return a `400 Bad Request` response with an error message indicating the invalid quantity, without modifying any existing invoice or credit note data.
6. IF a partial credit note request contains no line references, THEN THE CreditNoteController SHALL return a `400 Bad Request` response with an error message indicating that at least one line must be specified, without modifying any existing invoice or credit note data.
7. WHEN a partial credit note is computed, THE CreditNoteCalculator SHALL apply the discount rate and VAT rate from the original invoice to the credited partial net amount (the sum of unit price × credited quantity for the specified lines only), not to any amount derived from the original invoice's already-computed amounts.
8. WHEN a partial credit note is issued against an invoice, THE system SHALL permit subsequent partial credit note requests against the same invoice provided the remaining reversible quantity on each requested line is greater than zero.

---

### Requirement 4: Full Credit Note

**User Story:** As a billing operator, I want to fully reverse an invoice with a single request, so that I can efficiently cancel an entire invoice without enumerating all lines.

#### Acceptance Criteria

1. WHEN a credit note request is received with no line specification, THE CreditNoteCalculator SHALL compute the credit note using all lines from the original invoice at their original quantities and original unit prices.
2. WHEN a full credit note is issued, THE Credit_Note total amount SHALL equal the negation of the original invoice total amount, including all taxes and discounts applied to the original invoice.
3. IF the referenced invoice does not exist, THEN THE CreditNoteCalculator SHALL reject the request and return a `404 Not Found` error indicating the invoice was not found, without creating a credit note.
4. IF the referenced invoice has already been fully reversed, THEN THE CreditNoteCalculator SHALL reject the request and return a `409 Conflict` error indicating the invoice has already been fully reversed, without creating a credit note.

---

### Requirement 5: Validation and Error Handling

**User Story:** As a billing operator, I want clear error responses when a credit note request is invalid, so that I can identify and correct problems without ambiguity.

#### Acceptance Criteria

1. IF the invoice number in the request does not exist in the system, THEN THE CreditNoteController SHALL return a `404 Not Found` response with an error payload containing a human-readable message indicating the invoice was not found and a numeric error code uniquely identifying the not-found condition.
2. IF a credit note request body is missing or empty when lines are expected, THEN THE CreditNoteController SHALL return a `400 Bad Request` response with an error payload containing a human-readable message indicating the missing or empty body and a numeric error code uniquely identifying the bad-request condition.
3. IF a credit note request contains one or more line items referencing a quantity outside the range of 1 to 999,999 or an amount outside the range of 0.01 to 999,999,999.99, THEN THE CreditNoteController SHALL return a `400 Bad Request` response with an error payload containing a human-readable message identifying the invalid field and a numeric error code uniquely identifying the validation failure.
4. THE BillingExceptionHandler SHALL handle all credit note validation exceptions and produce an error payload containing a human-readable error message of at most 500 characters and a numeric error code, leaving no partial state changes persisted.

---

### Requirement 6: Credit Note Retrieval

**User Story:** As a billing operator, I want to retrieve a previously issued credit note by its number, so that I can inspect or audit it.

#### Acceptance Criteria

1. WHEN a valid credit note number is provided to `GET /api/credit-notes/{creditNoteNumber}`, THE CreditNoteController SHALL return the corresponding Credit_Note with all its associated fields.
2. IF the credit note number does not exist, THEN THE CreditNoteController SHALL return a `404 Not Found` response with a human-readable error message indicating the credit note was not found and a numeric error code.
3. IF the credit note number is malformed (empty, null, or not a non-empty alphanumeric string of 1 to 50 characters), THEN THE CreditNoteController SHALL return a `400 Bad Request` response with a human-readable error message indicating the format violation and a numeric error code.

---

### Requirement 7: Credit Note Amount Correctness (Round-Trip)

**User Story:** As a billing engineer, I want the credit note amounts to be the exact arithmetic negation of the original invoice amounts for a full reversal, so that the financial impact is zero when the invoice and credit note are summed.

#### Acceptance Criteria

1. WHEN a full credit note is issued for an invoice, THE sum of the invoice net amount and the credit note net amount SHALL equal exactly 0.00.
2. WHEN a full credit note is issued for an invoice, THE sum of the invoice discount amount and the credit note discount amount SHALL equal exactly 0.00.
3. WHEN a full credit note is issued for an invoice, THE sum of the invoice VAT amount and the credit note VAT amount SHALL equal exactly 0.00.
4. WHEN a full credit note is issued for an invoice, THE sum of the invoice total amount and the credit note total amount SHALL equal exactly 0.00.
5. IF an invoice has a total amount of exactly 0.00, THEN issuing a full credit note SHALL still produce a credit note with a total of exactly 0.00, and the sum of both totals SHALL equal exactly 0.00.
6. WHEN a credit note is issued, THE absolute value of the credit note total SHALL NOT exceed the remaining reversible amount of the original invoice at the time of issuance, where remaining reversible amount equals the original invoice total minus the absolute value of all previously issued credit note totals against that invoice.

# Implementation Plan: Credit Note Issuance

## Overview

Implement credit note issuance on the existing Java 21 / Spring Boot billing service.
The work proceeds in layered increments: data models → exceptions → storage → calculation
→ orchestration → REST layer → exception wiring → tests. Each task builds on the
previous ones so that the feature is always in a compilable, runnable state after
every step.

---

## Tasks

- [x] 1. Add data model records for credit notes
  - [x] 1.1 Create `CreditNoteLine` record in `com.emg.billing.model`
    - Mirror `InvoiceLine` structure; field names: `reference`, `description`, `creditedQuantity`, `unitPrice`
    - Add `netAmount()` helper returning `unitPrice.multiply(BigDecimal.valueOf(creditedQuantity))`
    - _Requirements: 2.1, 3.1, 3.2_
  - [x] 1.2 Create `CreditNote` record in `com.emg.billing.model`
    - Fields: `creditNoteNumber`, `invoiceNumber`, `customerId`, `issueDate` (`LocalDate`), `lines` (`List<CreditNoteLine>`), `netAmount`, `discountAmount`, `vatAmount`, `totalAmount` (all `BigDecimal`)
    - All monetary fields are negative for issued credit notes
    - _Requirements: 2.3, 2.8, 2.9_
  - [x] 1.3 Create `CreditNoteRequest` record in `com.emg.billing`
    - Outer record fields: `fullReversal` (`boolean`), `lines` (`List<CreditNoteLineRequest>`)
    - Nested record `CreditNoteLineRequest`: fields `reference` (`String`), `quantity` (`int`)
    - _Requirements: 2.1, 4.1_

- [x] 2. Add exception types
  - [x] 2.1 Create `InvoiceNotFoundException` extending `RuntimeException` in `com.emg.billing`
    - Constructor accepts `invoiceNumber`; message: `"Invoice not found: " + invoiceNumber`
    - _Requirements: 1.4, 2.10, 5.1_
  - [x] 2.2 Create `CreditNoteNotFoundException` extending `RuntimeException` in `com.emg.billing`
    - Constructor accepts `creditNoteNumber`; message: `"Credit note not found: " + creditNoteNumber`
    - _Requirements: 6.2_
  - [x] 2.3 Create `AlreadyFullyReversedException` extending `RuntimeException` in `com.emg.billing`
    - Constructor accepts `invoiceNumber`; message: `"Invoice already fully reversed: " + invoiceNumber`
    - _Requirements: 2.11, 4.4_
  - [x] 2.4 Create `ExcessiveReversalException` extending `RuntimeException` in `com.emg.billing`
    - Constructor accepts `lineReference` and remaining `BigDecimal` qty; message names the offending line
    - _Requirements: 2.12, 3.4_
  - [x] 2.5 Create `InvalidCreditNoteRequestException` extending `RuntimeException` in `com.emg.billing`
    - Constructor accepts a `String message`; used for empty body, bad reference, qty < 1, qty range violations
    - _Requirements: 3.3, 3.5, 3.6, 5.2, 5.3_

- [ ] 3. Implement `InvoiceStore` and retrofit `InvoiceController`
  - [ ] 3.1 Define `InvoiceStore` interface in `com.emg.billing`
    - Method `void save(Invoice invoice)`
    - Method `Invoice findByNumber(String invoiceNumber)` — throws `InvoiceNotFoundException` if absent
    - _Requirements: 1.1, 1.2, 1.3, 1.4_
  - [ ] 3.2 Implement `InMemoryInvoiceStore` as `@Repository` in `com.emg.billing`
    - Back with `ConcurrentHashMap<String, Invoice>`
    - `save()` stores the invoice keyed by `invoice.invoiceNumber()`
    - `findByNumber()` returns from the map or throws `InvoiceNotFoundException`
    - _Requirements: 1.2, 1.3, 1.4_
  - [ ] 3.3 Retrofit `InvoiceController` to inject and use `InvoiceStore`
    - Add `InvoiceStore` constructor parameter
    - Call `invoiceStore.save(invoice)` immediately after `calculator.calculate(...)`, before returning the response
    - _Requirements: 1.1_

- [ ] 4. Implement `CreditNoteStore`
  - [ ] 4.1 Define `CreditNoteStore` interface in `com.emg.billing`
    - Methods: `CreditNote save(CreditNote)`, `CreditNote findByNumber(String)` (throws `CreditNoteNotFoundException`), `List<CreditNote> findByInvoiceNumber(String)`, `String nextCreditNoteNumber()`, `BigDecimal remainingReversibleAmount(String invoiceNumber, BigDecimal originalTotal)`, `boolean isFullyReversed(String invoiceNumber)`
    - _Requirements: 2.8, 2.9, 2.11, 7.6_
  - [ ] 4.2 Implement `InMemoryCreditNoteStore` as `@Repository` in `com.emg.billing`
    - `ConcurrentHashMap<String, CreditNote>` keyed by credit note number
    - `ConcurrentHashMap<String, List<CreditNote>>` keyed by invoice number for reversal tracking
    - `AtomicInteger sequenceCounter` seeded at 1; `nextCreditNoteNumber()` returns `"CN-%05d".formatted(counter.getAndIncrement())`
    - `remainingReversibleAmount`: original total minus sum of absolute values of all credit note totals for that invoice
    - `isFullyReversed`: returns true when `remainingReversibleAmount` equals 0.00 (using `compareTo`)
    - _Requirements: 2.8, 2.11, 7.6_

- [ ] 5. Implement `CreditNoteCalculator`
  - [ ] 5.1 Create `CreditNoteCalculator` as `@Service` in `com.emg.billing`
    - Constructor injects `VatRateProvider`
    - Method signature: `CreditNote calculate(String creditNoteNumber, String invoiceNumber, String customerId, String countryCode, CustomerTier tier, List<CreditNoteLine> lines)`
    - Calculation pipeline (same order as `InvoiceCalculator`):
      1. `net = Σ line.netAmount()` → `setScale(2, HALF_UP)`
      2. `discount = net × DiscountPolicy.rateFor(tier)` → `setScale(2, HALF_UP)`
      3. `taxable = net − discount`
      4. `vat = taxable × vatRateProvider.rateFor(countryCode)` → `setScale(2, DOWN)` (mirrors `InvoiceCalculator`)
      5. `total = taxable + vat`
    - Negate all five monetary fields with `.negate()` before constructing the `CreditNote`
    - Set `issueDate` to `LocalDate.now()`
    - Never reuse any already-rounded value from the original invoice
    - _Requirements: 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 3.7_
  - [ ]* 5.2 Write property test `CreditNoteRoundTripTest` — Property 1: full credit note negates every monetary field
    - Add jqwik 1.8.4 dependency to `pom.xml` (`<scope>test</scope>`)
    - Use `@Property(tries = 100)` with `@Label("Feature: credit-note-issuance, Property 1: full credit note negates every monetary field")`
    - Generate arbitrary lines (ref: short alphanumeric, qty: 1–99, unitPrice: BigDecimal in [0.01, 9999.99] with 2 dp), tier from `CustomerTier.values()`, country from `{FR, BE, DE, MA, IN}`
    - Assert `invoice.netAmount().add(cn.netAmount()).compareTo(BigDecimal.ZERO) == 0` for all four monetary fields
    - **Property 1: Full credit note negates every monetary field**
    - **Validates: Requirements 7.1, 7.2, 7.3, 7.4**
  - [ ]* 5.3 Write property test — Property 2: zero-total invoice full reversal yields zero credit note
    - Use `@Property(tries = 100)` with `@Label("Feature: credit-note-issuance, Property 2: zero-total invoice full reversal yields zero credit note")`
    - Generate invoice with all unit prices set to 0.00 or with lines that produce net = 0.00
    - Assert `cn.totalAmount().compareTo(BigDecimal.ZERO) == 0` and sum of totals equals 0.00
    - **Property 2: Zero-total invoice full reversal yields zero credit note**
    - **Validates: Requirement 7.5**
  - [ ]* 5.4 Write property test — Property 4: partial credit note amounts are computed from scratch
    - Use `@Property(tries = 100)` with `@Label("Feature: credit-note-issuance, Property 4: partial credit note amounts are computed from scratch")`
    - Generate an invoice and a random subset of its lines with valid credited quantities
    - Assert that the `CreditNoteCalculator` result equals independently running the full pipeline on those credited lines only
    - **Property 4: Partial credit note amounts are computed from scratch**
    - **Validates: Requirements 2.7, 3.7**
  - [ ]* 5.5 Write property test — Property 6: quantity below 1 is always rejected
    - Use `@Property(tries = 100)` with `@Label("Feature: credit-note-issuance, Property 6: whitespace/invalid line quantities are always rejected")`
    - Generate partial requests containing at least one line with `quantity` = 0 or negative
    - Assert `CreditNoteService.createCreditNote(...)` throws `InvalidCreditNoteRequestException` and store state is unchanged
    - **Property 6: Whitespace/invalid line quantities are always rejected**
    - **Validates: Requirement 3.5**

- [ ] 6. Implement `CreditNoteService`
  - [ ] 6.1 Create `CreditNoteService` as `@Service` in `com.emg.billing`
    - Constructor injects `InvoiceStore`, `CreditNoteStore`, `CreditNoteCalculator`
    - Method `CreditNote createCreditNote(String invoiceNumber, CreditNoteRequest request)`:
      1. `invoice = invoiceStore.findByNumber(invoiceNumber)` — propagates `InvoiceNotFoundException`
      2. Check `creditNoteStore.isFullyReversed(invoiceNumber)` → throw `AlreadyFullyReversedException` if true
      3. Validate request:
         - Full reversal (`fullReversal == true` or `lines` is null/empty + `fullReversal` false → throw `InvalidCreditNoteRequestException`)
         - Partial: validate each `lineRequest.reference()` exists in `invoice.lines()` → `InvalidCreditNoteRequestException`; validate `qty >= 1` → `InvalidCreditNoteRequestException`; validate `qty <= original line qty` → `ExcessiveReversalException`
      4. Build `List<CreditNoteLine>` from validated request
      5. Call `creditNoteCalculator.calculate(...)` with invoice's `customerId`, `countryCode`, `tier` derived from invoice context (look up customer tier by re-resolving via the original invoice's discount amount relative to net, or pass `CustomerTier` from the original invoice — store tier on `Invoice` if needed, else accept tier from request; **preferred**: add `tier` and `countryCode` fields to `Invoice` record)
      6. Assign `creditNoteNumber = creditNoteStore.nextCreditNoteNumber()`
      7. Rebuild `CreditNote` with the assigned number and persist via `creditNoteStore.save(...)`
      8. Return the persisted `CreditNote`
    - Note: all validation runs before any `save()` call to guarantee no partial state
    - _Requirements: 2.1, 2.8, 2.9, 2.10, 2.11, 2.12, 3.3, 3.5, 3.6_
  - [ ] 6.2 Extend `Invoice` record to carry `countryCode` and `customerTier` fields
    - Add `String countryCode` and `CustomerTier customerTier` fields to the `Invoice` record
    - Update `InvoiceCalculator.calculate(...)` to populate these two new fields from the `Customer` argument
    - Update `InvoiceCalculatorTest` to verify the new fields are populated correctly
    - _Requirements: 2.4, 2.5_

- [ ] 7. Checkpoint — verify core logic compiles and existing tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 8. Implement `CreditNoteController`
  - [ ] 8.1 Create `CreditNoteController` as `@RestController` at `/api/credit-notes` in `com.emg.billing`
    - Constructor injects `CreditNoteService`
    - `POST /api/credit-notes/{invoiceNumber}` — accepts `@RequestBody CreditNoteRequest`, delegates to `creditNoteService.createCreditNote(invoiceNumber, request)`, returns `ResponseEntity<CreditNote>` with HTTP 200
    - `GET /api/credit-notes/{creditNoteNumber}` — path variable annotated with `@Pattern(regexp = "[A-Za-z0-9]{1,50}")` (Bean Validation); delegates to `creditNoteService.getCreditNote(creditNoteNumber)`, returns `ResponseEntity<CreditNote>` with HTTP 200
    - Add `CreditNote getCreditNote(String creditNoteNumber)` method to `CreditNoteService` that calls `creditNoteStore.findByNumber(...)`
    - Annotate controller class with `@Validated` to enable Bean Validation on path variables
    - _Requirements: 2.1, 2.9, 2.10, 2.11, 6.1, 6.3_

- [ ] 9. Extend `BillingExceptionHandler`
  - [ ] 9.1 Add `@ExceptionHandler` methods for all new exception types in `BillingExceptionHandler`
    - `InvoiceNotFoundException` → 404, code 4041
    - `CreditNoteNotFoundException` → 404, code 4042
    - `AlreadyFullyReversedException` → 409, code 4091
    - `ExcessiveReversalException` → 400, code 4001
    - `InvalidCreditNoteRequestException` → 400, code 4002
    - `ConstraintViolationException` (from `jakarta.validation`) → 400, code 4002 (for path-variable format violations)
    - All responses use `{ "error": string, "code": number }` payload; truncate message to 500 characters before returning
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 6.2, 6.3_

- [ ] 10. Write unit tests
  - [ ] 10.1 Write `CreditNoteCalculatorTest` in `com.emg.billing`
    - Test correct `netAmount`, `discountAmount`, `vatAmount`, `totalAmount` for STANDARD, SILVER, and GOLD tier customers
    - Test that all amounts are negative
    - Test partial credit uses only the credited lines/quantities (not the full invoice lines)
    - Test rounding edge cases: prices that produce non-terminating decimals
    - _Requirements: 2.2, 2.3, 2.6, 3.1, 3.2_
  - [ ]* 10.2 Write property test — Property 3: remaining reversible amount is non-negative and decreases monotonically
    - Use `@Property(tries = 100)` with `@Label("Feature: credit-note-issuance, Property 3: remaining reversible amount is non-negative and decreases monotonically")`
    - Generate an invoice and a sequence of 1–5 valid partial credit notes consuming random subsets of remaining quantities
    - Assert remaining reversible amount after each issuance is ≥ 0.00 and strictly less than before
    - **Property 3: Remaining reversible amount is non-negative and decreases monotonically**
    - **Validates: Requirement 7.6**
  - [ ] 10.3 Write `CreditNoteStoreTest` in `com.emg.billing`
    - Test sequential numbering starts at `CN-00001` and increments correctly
    - Test `remainingReversibleAmount` decreases correctly after each partial note
    - Test `isFullyReversed` returns `false` after partial notes, `true` when sum equals invoice total
    - _Requirements: 2.8, 2.11, 7.6_
  - [ ] 10.4 Write `CreditNoteServiceTest` in `com.emg.billing`
    - Test 404 path: `InvoiceNotFoundException` propagated when invoice not found
    - Test 409 path: `AlreadyFullyReversedException` thrown when invoice fully reversed
    - Test 400 path: `InvalidCreditNoteRequestException` for unrecognised line reference
    - Test 400 path: `InvalidCreditNoteRequestException` for `qty < 1`
    - Test 400 path: `InvalidCreditNoteRequestException` for empty lines on partial request
    - Test 400 path: `ExcessiveReversalException` for qty exceeding original line qty
    - Test happy path full reversal: correct credit note returned and persisted
    - Test happy path partial reversal: correct credit note returned for subset of lines
    - _Requirements: 2.9, 2.10, 2.11, 2.12, 3.3, 3.5, 3.6_
  - [ ]* 10.5 Write property test — Property 5: credit note number uniqueness
    - Use `@Property(tries = 100)` with `@Label("Feature: credit-note-issuance, Property 5: credit note number uniqueness")`
    - Generate N (2–20) sequential credit note creation requests against distinct invoices
    - Assert all assigned credit note numbers are distinct and each matches `CN-\d{5}`
    - **Property 5: Credit note number uniqueness**
    - **Validates: Requirement 2.8**
  - [ ] 10.6 Write `CreditNoteControllerTest` using Spring MockMvc in `com.emg.billing`
    - Happy-path POST: returns HTTP 200 with correct JSON body (all monetary fields present and negative)
    - Happy-path GET: returns HTTP 200 with stored credit note JSON
    - 400 for malformed `creditNoteNumber` path variable (e.g., empty string, >50 chars, special characters)
    - 404 for unknown `invoiceNumber` on POST
    - 409 for already-fully-reversed invoice on POST
    - 400 for exceeded quantity on POST (`ExcessiveReversalException`)
    - 400 for invalid request body on POST (`InvalidCreditNoteRequestException`)
    - Verify all error responses carry `{ "error": string, "code": number }` payload
    - _Requirements: 2.10, 2.11, 2.12, 5.1, 5.2, 5.3, 6.1, 6.2, 6.3_

- [ ] 11. Final checkpoint — ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

---

## Notes

- Tasks marked with `*` are optional and can be skipped for an MVP delivery; all correctness properties are still exercised by the non-optional unit tests.
- Task 6.2 (extending `Invoice` with `countryCode` and `customerTier`) is a prerequisite for Task 6.1. These are sequenced within the same parent task to make the dependency explicit.
- `CreditNoteCalculator` must use `RoundingMode.DOWN` for the VAT step (not HALF_UP) to mirror `InvoiceCalculator` exactly and satisfy Property 1 (round-trip negation).
- All PBT tests run purely in memory — no Spring context is started; use plain constructor injection in test setup.
- jqwik 1.8.4 integrates natively with JUnit 5; no separate test runner configuration is needed.
- The `InvoiceController` retrofit (Task 3.3) is the only modification to existing production code outside of `BillingExceptionHandler` and the `Invoice` record extension (Task 6.2).

---

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "1.3"] },
    { "id": 1, "tasks": ["2.1", "2.2", "2.3", "2.4", "2.5"] },
    { "id": 2, "tasks": ["3.1", "4.1"] },
    { "id": 3, "tasks": ["3.2", "4.2"] },
    { "id": 4, "tasks": ["3.3", "6.2"] },
    { "id": 5, "tasks": ["5.1"] },
    { "id": 6, "tasks": ["5.2", "5.3", "5.4", "5.5", "6.1"] },
    { "id": 7, "tasks": ["8.1", "9.1"] },
    { "id": 8, "tasks": ["10.1", "10.2", "10.3", "10.4", "10.5"] },
    { "id": 9, "tasks": ["10.6"] }
  ]
}
```

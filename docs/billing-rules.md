# Billing rules

Working document. Maintained by the billing product team.
Last reviewed: incomplete, see open questions at the end.

## 1. Invoice structure

An invoice belongs to one customer and carries one or more lines. Each line has
a reference, a description, a quantity (strictly positive) and a unit price
excluding VAT.

## 2. Order of calculation

The order below is normative. The legacy batch and the Java service must both
follow it:

1. **Net amount** = sum of the line amounts (quantity x unit price)
2. **Commercial discount** applied on the net amount
3. **VAT** applied on the discounted amount, never on the gross amount
4. **Total** = discounted amount + VAT

## 3. Commercial tiers

The tier is derived from the customer's rolling 12-month revenue.

| Tier | Rolling revenue | Discount |
| --- | --- | --- |
| STANDARD | below 10 000 EUR | 0 % |
| SILVER | from 10 000 EUR | 5 % |
| GOLD | from 50 000 EUR | 10 % |

Thresholds are **inclusive**. A customer whose rolling revenue is exactly
10 000 EUR is SILVER, and a customer at exactly 50 000 EUR is GOLD.

## 4. VAT rates

| Country | Rate |
| --- | --- |
| FR | 20 % |
| BE | 21 % |
| DE | 19 % |
| MA | 20 % |
| IN | 18 % |

Any country not listed uses the default rate of 20 %.

## 5. Rounding

All monetary amounts are rounded to 2 decimals, **half-up**, at each step of the
calculation. Rounding half-up means 19,995 becomes 20,00.

This rule applies to the discount amount, the VAT amount and the total. It is
the single most common source of divergence between the batch and the API.

## 6. Unknown customer

A request for an invoice on an unknown customer identifier is a normal business
case, not a system failure. The API must answer `404` with the standard error
payload `{ "error": string, "code": number }`.

## Open questions

These points are not settled yet. Do not assume an answer.

- Credit notes: no rule is defined at this stage. See the Lab 2 exercise.
- Multi-currency: out of scope for now, everything is in EUR.
- Retroactive tier changes: behaviour undefined when a customer changes tier
  between the invoice issue date and the batch run.

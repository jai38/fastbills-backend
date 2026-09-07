# ADR 0001: Money Representation and Precision

## Status
Accepted

## Context
Financial software must not use binary floating-point representation (`float` or `double`) due to base-2 IEEE 754 precision loss (e.g., `0.1 + 0.2 != 0.3`). Tax and accounting regulations require exact decimal arithmetic to the paise, with explicit half-up rounding.

Furthermore, JavaScript native numbers are double-precision floats. If the API sends large numbers as JSON numbers, JavaScript clients may truncate or lose precision beyond 53 bits (e.g. `Number.MAX_SAFE_INTEGER`).

## Decision
1. In PostgreSQL, all money columns are defined as `NUMERIC(15,2)`. Quantities are `NUMERIC(15,3)`, and tax rates are `NUMERIC(5,2)`.
2. In Java, money is represented solely as `java.math.BigDecimal`, constructed via `new BigDecimal(String)` or `BigDecimal.valueOf(long)`. Never construct from `double`.
3. All rounding operations explicitly specify `RoundingMode.HALF_UP`.
4. In Jackson JSON serialization, `BigDecimal` values are serialized as JSON **strings** (e.g. `"14250.00"`), preventing browser JavaScript runtimes from converting them into IEEE 754 floats.

## Consequences
- Zero floating-point drift across the entire system.
- Frontend and backend agree on exact values down to the paise.
- Frontend clients must parse money fields using a decimal library (`big.js`) rather than native JavaScript numbers.

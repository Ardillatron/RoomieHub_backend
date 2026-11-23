# ADR-017: Estados de una deuda: Pendiente, Aprobada y Repartida

**Estado:** Aceptado (reemplaza al ADR-012)

## Contexto

La app no sabe a quién se le paga.

## Decisión

El deudor avisa que pagó y los demás lo confirman. Con todas las confirmaciones la deuda queda Aprobada. Repartida es para las deudas de quien se fue.

## Alternativas descartadas

- Los cuatro estados del ADR-012.

## Consecuencias

- A favor: Es simple y funciona con cualquier número de personas.
- En contra: Si el deudor no avisa, nadie puede confirmar todavía.

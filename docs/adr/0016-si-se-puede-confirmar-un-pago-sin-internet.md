# ADR-016: Sí se puede confirmar un pago sin internet

**Estado:** Aceptado (reemplaza al ADR-006)

## Contexto

Los pagos se hacen en persona y la gente olvidaba confirmarlos después.

## Decisión

Avisar y confirmar pagos se guarda sin internet y se envía después. Si el gasto ya no existe, se muestra un error.

## Alternativas descartadas

- Bloquearlo (ADR-006).

## Consecuencias

- A favor: Es más cómodo.
- En contra: Una confirmación puede rechazarse después.
- En contra: Falta hacerlo en la app (hoy se confirma con internet).

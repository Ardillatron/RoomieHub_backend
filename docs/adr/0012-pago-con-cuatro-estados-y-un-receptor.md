# ADR-012: Pago con cuatro estados y un receptor

**Estado:** Reemplazado por el ADR-017

## Contexto

La app no mueve dinero, así que alguien debe confirmar que se pagó.

## Decisión

Estados: Pendiente, Reportado, Por aprobar y Confirmado; confirma quien recibió el dinero.

## Alternativas descartadas

- Cerrar la deuda sin confirmación.

## Consecuencias

- A favor: Hay historial de confirmaciones.
- En contra: La app no sabe a quién se le paga.

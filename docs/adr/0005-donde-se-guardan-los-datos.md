# ADR-005: Dónde se guardan los datos

**Estado:** Aceptado

## Contexto

Los datos están muy relacionados entre sí (casas, personas, deudas).

## Decisión

Usamos PostgreSQL en el servidor y una copia en el celular para cuando no hay internet.

## Alternativas descartadas

- Una base de datos de documentos: no conviene para datos tan relacionados.

## Consecuencias

- A favor: Datos ordenados y confiables.
- En contra: Hay que mantener en orden dos copias. La copia en el celular aún está pendiente.

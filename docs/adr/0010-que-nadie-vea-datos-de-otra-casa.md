# ADR-010: Que nadie vea datos de otra casa

**Estado:** Aceptado

## Contexto

Varias casas comparten la misma base de datos.

## Decisión

La propia base de datos solo deja ver los datos de las casas donde estás (Row-Level Security). Así, aunque haya un error en el código, no se mezclan datos.

## Alternativas descartadas

- Revisarlo solo en el código: un error dejaría ver datos ajenos.

## Consecuencias

- A favor: Protección extra aunque el código falle.
- En contra: Es más difícil de configurar.

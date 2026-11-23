# ADR-019: Usar Clean Architecture

**Estado:** Aceptado

## Contexto

Queremos poder cambiar la base de datos o el framework sin rehacer todo.

## Decisión

El código se divide en 4 capas. El núcleo (reglas del negocio) no conoce la base de datos ni Spring.

## Alternativas descartadas

- Mezclar todo en el mismo lugar.

## Consecuencias

- A favor: Se puede cambiar la tecnología sin tocar las reglas.
- En contra: Hay más archivos y clases.

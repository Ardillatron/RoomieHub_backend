# ADR-002: Una sola base de datos central

**Estado:** Aceptado

## Contexto

Todos los de la casa deben ver la misma información.

## Decisión

Toda la información oficial se guarda en una base de datos en el servidor. El celular solo guarda una copia temporal.

## Alternativas descartadas

- Guardar todo en cada celular: cada quien vería datos distintos.

## Consecuencias

- A favor: Todos ven lo mismo y el historial no se pierde.
- En contra: Sin servidor no llegan datos nuevos.

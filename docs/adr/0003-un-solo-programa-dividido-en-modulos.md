# ADR-003: Un solo programa dividido en módulos

**Estado:** Aceptado

## Contexto

Somos dos personas y el sistema tiene varias partes: casas, tareas, gastos y chat.

## Decisión

Hacemos un solo backend, separado por capas y por módulos, en lugar de varios servicios.

## Alternativas descartadas

- Un servicio separado por módulo: demasiado trabajo de mantener.

## Consecuencias

- A favor: Es fácil de entender y de subir.
- En contra: Si una parte falla, puede fallar todo.

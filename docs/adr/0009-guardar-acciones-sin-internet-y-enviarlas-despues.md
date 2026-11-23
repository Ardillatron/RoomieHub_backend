# ADR-009: Guardar acciones sin internet y enviarlas después

**Estado:** Aceptado

## Contexto

No queremos que la falta de internet impida crear notas, marcar tareas o avisar pagos.

## Decisión

La app guarda esas acciones en una lista y las envía al volver el internet. El servidor ignora las repetidas.

## Alternativas descartadas

- Pedir internet siempre.

## Consecuencias

- A favor: No se pierde nada de lo que hace el usuario.
- En contra: Los demás ven los cambios hasta que vuelve el internet.
- En contra: El servidor ya lo soporta; falta hacerlo en la app.

# ADR-014: No borrar a quien se va de la casa

**Estado:** Aceptado

## Contexto

Si alguien se va, su historial de pagos debe quedarse.

## Decisión

No se borra a la persona: solo se marca como inactiva. Sus deudas pendientes se reparten entre los que se quedan.

## Alternativas descartadas

- Borrarla por completo: se perdería el historial.

## Consecuencias

- A favor: El historial queda completo.
- En contra: La base guarda datos de gente que ya no está.

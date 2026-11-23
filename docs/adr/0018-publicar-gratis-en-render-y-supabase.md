# ADR-018: Publicar gratis en Render y Supabase

**Estado:** Aceptado (reemplaza al ADR-008)

## Contexto

No tenemos presupuesto.

## Decisión

El servidor está en Render y la base de datos en Supabase, en planes gratuitos. Un servicio externo despierta el servidor cada 15 minutos para las tareas automáticas.

## Alternativas descartadas

- Tres entornos (ADR-008).

## Consecuencias

- A favor: Cuesta $0.
- En contra: La primera vez que se abre tarda cerca de un minuto en responder.

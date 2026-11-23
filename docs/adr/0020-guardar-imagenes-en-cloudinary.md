# ADR-020: Guardar imágenes en Cloudinary

**Estado:** Aceptado

## Contexto

El servidor gratuito borra sus archivos al reiniciarse.

## Decisión

Las imágenes se suben a Cloudinary y solo guardamos su referencia.

## Alternativas descartadas

- Guardarlas en el servidor: se perderían.

## Consecuencias

- A favor: Gratis y fácil de cambiar por otro servicio.
- En contra: Dependemos de otro servicio externo.

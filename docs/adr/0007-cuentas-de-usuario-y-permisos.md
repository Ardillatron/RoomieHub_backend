# ADR-007: Cuentas de usuario y permisos

**Estado:** Aceptado

## Contexto

Hay un Administrador y varios Habitantes. Esconder botones en la app no basta para proteger.

## Decisión

Cada persona tiene su cuenta (correo o Google). El servidor revisa en cada acción si la persona pertenece a la casa y si tiene permiso.

## Alternativas descartadas

- Revisar permisos solo en la app: alguien podría saltárselo.

## Consecuencias

- A favor: Solo el Administrador hace acciones de Administrador.
- En contra: Cada acción tarda un poco más por la revisión.

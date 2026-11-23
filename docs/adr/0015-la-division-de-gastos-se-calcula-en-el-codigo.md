# ADR-015: La división de gastos se calcula en el código

**Estado:** Aceptado (reemplaza al ADR-011)

## Contexto

Las reglas de negocio no deben depender de la base de datos.

## Decisión

El cálculo vive en el núcleo del programa (CalculadoraDeReparto). Se usan montos exactos y el centavo que sobra se turna cada mes.

## Alternativas descartadas

- Dejarlo en la base de datos (ADR-011).

## Consecuencias

- A favor: Se puede probar fácil y no depende de la base de datos.
- En contra: El servidor hace el cálculo cada periodo.

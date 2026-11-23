# ADR-011: Calcular la división de gastos dentro de la base de datos

**Estado:** Reemplazado por el ADR-015

## Contexto

Todos los celulares deben ver el mismo resultado.

## Decisión

Hacer el cálculo con procedimientos en la base de datos.

## Alternativas descartadas

- Calcular en cada celular.

## Consecuencias

- A favor: Cálculo en un solo lugar.
- En contra: Las reglas quedaban amarradas a la base de datos y era difícil probarlas.

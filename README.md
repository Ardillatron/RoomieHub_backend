# RoomieHub · Backend

App móvil para viviendas compartidas: reparte los quehaceres de forma equilibrada según el esfuerzo acumulado
de cada roomie y lleva un registro inmutable de los gastos compartidos y de quién confirmó cada pago.
Este repositorio es la API; la app Flutter está en [THEconLECHE/Frontend](https://github.com/THEconLECHE/Frontend).

## Equipo

Equipo **Mamut y Paput**

- Josué Isaac Palacios Aguado — 333351 — Backend (Spring Boot), app Flutter, despliegue
- Isaías Rangel Ledezma — 333318 — Requerimientos, reglas de negocio, ADRs y pruebas

## Stack

- Java 17 + Spring Boot 4 (Web, Security, Data JPA, WebSocket/STOMP)
- PostgreSQL 16 con Row-Level Security y migraciones Flyway
- JWT propio + Google Sign-In, Firebase Cloud Messaging (push), Cloudinary (imágenes)
- App móvil: Flutter (Android e iOS)
- Despliegue: Docker en Render + PostgreSQL en Supabase

## Arquitectura

![Diagrama](<img width="2460" height="1393" alt="arquitectura" src="https://github.com/user-attachments/assets/d8801e6a-0c8a-422d-9d47-fd2b642eb818" />
)

Clean Architecture en 4 capas; las dependencias apuntan hacia el dominio. Todo vive en `src/main/java/roomiehub/vivienda/`:

| Capa | Carpeta | Contenido |
|---|---|---|
| Entities / Domain | `domain/` | Entidades y value objects (`Sala`, `Deuda`, `Dinero`), servicios de dominio (`AsignadorDeTareas`, `CalculadoraDeReparto`), interfaces de repositorio y excepciones. Java puro: sin Spring ni JPA. |
| Use Cases | `application/` | Una clase por caso de uso (`usecase/finanzas/ConfirmarPago` = CU-22) y puertos de servicios externos (`port/NotificadorPort`). |
| Interface Adapters | `adapters/` | Controllers REST y DTOs (`web/`), procesos programados (`scheduler/`) y `Jpa*RepositoryAdapter` que implementan las interfaces del dominio (`persistence/`). |
| Frameworks & Drivers | `infrastructure/` | Entidades JPA y Spring Data, seguridad (JWT, aspecto RLS), Firebase, Cloudinary y `UseCaseConfig` (raíz de composición). |

## Cómo correrlo

1. Requisitos: JDK 17, PostgreSQL 16 o posterior (o Docker). Maven viene incluido (`mvnw`).
2. Crea los roles y la base una vez, como superusuario:
   `psql -U postgres -v owner_pw="'...'" -v app_pw="'...'" -f db/roles.sql`
3. Configura los secretos: `cp .env.example .env` (para Docker) o `cp local.properties.example local.properties`
   (para correr local; Spring lo lee solo). Llena al menos `DB_PASSWORD`, `DB_OWNER_PASSWORD` y `JWT_SECRET`.
4. Arranca la API (Flyway crea el esquema al iniciar):
   - Local: `./mvnw spring-boot:run`
   - Docker: `docker build -t roomiehub . && docker run --env-file .env -p 8080:8080 roomiehub`
5. Verifica: `curl http://localhost:8080/actuator/health` → `{"status":"UP"}`
6. Pruebas:
   - Unitarias (dominio y casos de uso, sin base de datos): `./mvnw test`
   - Integración con PostgreSQL real (RLS, endpoints): `./mvnw test -Ddb=true`

Producción: <https://roomiehub-backend-mvg4.onrender.com> (plan gratuito: la primera petición tras inactividad tarda ~1 min).

## Documentación

- Diagrama: [`docs/arquitectura.png`](docs/arquitectura.png)
- ADRs: [`docs/adr/`](docs/adr/)
- Video: PENDIENTE

-- Se ejecuta UNA vez, como superusuario, antes de arrancar la app (sección 9).
-- Los roles son del clúster, no de una base, por eso no van en una migración de Flyway.
--
--   psql -U postgres -v owner_pw="'...'" -v app_pw="'...'" -f db/roles.sql
--
-- roomiehub_owner: dueño del esquema; con él corren las migraciones (spring.flyway.user).
-- app_user:        con él se conecta la app; no es dueño de nada, así que RLS lo filtra.
-- app_scheduler:   sin login; app_user lo adopta con SET LOCAL ROLE en los procesos programados.
--
-- Supabase (SQL Editor): contraseñas literales en vez de :owner_pw/:app_pw, sin CREATE DATABASE (se usa
-- la base postgres) y con GRANT USAGE, CREATE ON SCHEMA public TO roomiehub_owner, porque public no es suyo.

CREATE ROLE roomiehub_owner LOGIN PASSWORD :owner_pw;
CREATE ROLE app_user LOGIN PASSWORD :app_pw;
CREATE ROLE app_scheduler NOLOGIN;

-- PostgreSQL 16+: sin herencia, app_user solo obtiene los permisos del scheduler tras SET ROLE.
GRANT app_scheduler TO app_user WITH INHERIT FALSE;

CREATE DATABASE roomiehub_dev OWNER roomiehub_owner;

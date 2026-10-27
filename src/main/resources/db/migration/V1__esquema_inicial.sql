-- Esquema inicial (sección 11) con Row-Level Security (sección 9).
-- Corre como roomiehub_owner. Los roles app_user y app_scheduler los crea db/roles.sql.

-- ─── Cuentas (sin sala_id ni RLS: el login las consulta antes de conocer al usuario) ───

CREATE TABLE usuarios (
    id             bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email          text,                 -- nulo tras eliminar la cuenta (RN-37)
    password_hash  text,                 -- nulo si usa Google
    proveedor_auth text NOT NULL CHECK (proveedor_auth IN ('EMAIL', 'GOOGLE', 'APPLE')),
    google_sub     text UNIQUE,
    nombre         text NOT NULL,
    foto_url       text,
    creado_en      timestamptz NOT NULL DEFAULT now(),
    eliminado_en   timestamptz
);
CREATE UNIQUE INDEX ux_usuarios_email ON usuarios (lower(email));

CREATE TABLE refresh_tokens (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id  bigint NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    token_hash  text NOT NULL UNIQUE,
    dispositivo text,
    creado_en   timestamptz NOT NULL DEFAULT now(),
    expira_en   timestamptz NOT NULL,
    revocado_en timestamptz
);

CREATE TABLE dispositivos_usuario (
    id             bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id     bigint NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    fcm_token      text NOT NULL UNIQUE,
    plataforma     text NOT NULL CHECK (plataforma IN ('ANDROID', 'IOS')),
    actualizado_en timestamptz NOT NULL DEFAULT now()
);

-- ─── Salas y membresías ───

CREATE TABLE salas (
    id                bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre            text NOT NULL CHECK (length(nombre) BETWEEN 1 AND 60),
    creador_id        bigint NOT NULL REFERENCES usuarios (id),
    zona_horaria      text NOT NULL,                       -- IANA (RN-38)
    codigo_invitacion text NOT NULL UNIQUE,                -- fijo (RN-05)
    estado            text NOT NULL DEFAULT 'ACTIVA' CHECK (estado IN ('ACTIVA', 'DESACTIVADA')),
    desactivada_en    timestamptz,
    creado_en         timestamptz NOT NULL DEFAULT now(),
    CHECK ((estado = 'DESACTIVADA') = (desactivada_en IS NOT NULL))
);

CREATE TABLE usuario_sala (
    id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id bigint NOT NULL REFERENCES usuarios (id),
    sala_id    bigint NOT NULL REFERENCES salas (id) ON DELETE CASCADE,
    rol        text NOT NULL CHECK (rol IN ('ADMIN', 'HABITANTE')),
    activo     boolean NOT NULL DEFAULT true,
    unido_en   timestamptz NOT NULL DEFAULT now(),   -- se reinicia al volver (RN-07)
    UNIQUE (usuario_id, sala_id)
);
-- RN-03: un solo Administrador activo por sala
CREATE UNIQUE INDEX ux_un_admin_por_sala ON usuario_sala (sala_id) WHERE rol = 'ADMIN' AND activo;

-- ─── Convivencia ───

CREATE TABLE notas_tablon (
    id           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sala_id      bigint NOT NULL REFERENCES salas (id) ON DELETE CASCADE,
    autor_id     bigint NOT NULL REFERENCES usuarios (id),
    texto        text NOT NULL DEFAULT '' CHECK (length(texto) <= 2000),
    imagen_url   text,
    fijada_hasta timestamptz,
    eliminada    boolean NOT NULL DEFAULT false,
    creado_en    timestamptz NOT NULL DEFAULT now(),
    client_uuid  uuid UNIQUE,
    CHECK (texto <> '' OR imagen_url IS NOT NULL)
);

CREATE TABLE reacciones_nota (
    id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nota_id    bigint NOT NULL REFERENCES notas_tablon (id) ON DELETE CASCADE,
    sala_id    bigint NOT NULL REFERENCES salas (id) ON DELETE CASCADE,
    usuario_id bigint NOT NULL REFERENCES usuarios (id),
    tipo       text NOT NULL CHECK (tipo IN ('LIKE', 'DISLIKE')),
    UNIQUE (nota_id, usuario_id)
);

CREATE TABLE chat_mensajes (
    id                 bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sala_id            bigint NOT NULL REFERENCES salas (id) ON DELETE CASCADE,
    autor_id           bigint NOT NULL REFERENCES usuarios (id),
    texto              text CHECK (length(texto) <= 4000),
    imagen_url         text,
    nota_respondida_id bigint REFERENCES notas_tablon (id),
    eliminado          boolean NOT NULL DEFAULT false,
    creado_en          timestamptz NOT NULL DEFAULT now(),
    client_uuid        uuid UNIQUE,
    CHECK (texto IS NOT NULL OR imagen_url IS NOT NULL)
);

CREATE TABLE reportes (
    id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sala_id    bigint NOT NULL REFERENCES salas (id) ON DELETE CASCADE,
    nota_id    bigint REFERENCES notas_tablon (id) ON DELETE CASCADE,
    mensaje_id bigint REFERENCES chat_mensajes (id) ON DELETE CASCADE,
    usuario_id bigint NOT NULL REFERENCES usuarios (id),
    motivo     text NOT NULL CHECK (length(motivo) BETWEEN 1 AND 500),
    creado_en  timestamptz NOT NULL DEFAULT now(),
    resuelto   boolean NOT NULL DEFAULT false,
    CHECK (num_nonnulls(nota_id, mensaje_id) = 1)
);

-- ─── Tareas ───

CREATE TABLE tareas (
    id           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sala_id      bigint NOT NULL REFERENCES salas (id) ON DELETE CASCADE,
    nombre       text NOT NULL CHECK (length(nombre) BETWEEN 1 AND 80),
    valor        smallint NOT NULL CHECK (valor BETWEEN 1 AND 10),
    frecuencia   text NOT NULL CHECK (frecuencia IN ('DIARIA', 'SEMANAL', 'MENSUAL')),
    fecha_inicio date NOT NULL,
    activa       boolean NOT NULL DEFAULT true,
    creado_en    timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE tarea_asignaciones (
    id               bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tarea_id         bigint NOT NULL REFERENCES tareas (id) ON DELETE CASCADE,
    sala_id          bigint NOT NULL REFERENCES salas (id) ON DELETE CASCADE,
    usuario_id       bigint REFERENCES usuarios (id),          -- nulo = sin asignar
    periodo_inicio   timestamptz NOT NULL,
    periodo_fin      timestamptz NOT NULL CHECK (periodo_fin > periodo_inicio),
    valor_al_asignar smallint NOT NULL CHECK (valor_al_asignar BETWEEN 1 AND 10),
    estado           text NOT NULL DEFAULT 'ASIGNADA' CHECK (estado IN ('ASIGNADA', 'COMPLETADA', 'VENCIDA')),
    completada_en    timestamptz,                              -- hora del servidor (RN-16)
    UNIQUE (tarea_id, periodo_inicio),
    CHECK ((estado = 'COMPLETADA') = (completada_en IS NOT NULL))
);

CREATE TABLE movimientos_puntos (
    id            bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id    bigint NOT NULL REFERENCES usuarios (id),
    sala_id       bigint NOT NULL REFERENCES salas (id) ON DELETE CASCADE,
    asignacion_id bigint REFERENCES tarea_asignaciones (id) ON DELETE CASCADE,
    puntos        integer NOT NULL,
    tipo          text NOT NULL CHECK (tipo IN ('COMPLETADA', 'PENALIZACION', 'INICIAL', 'AJUSTE')),
    creado_en     timestamptz NOT NULL DEFAULT now(),
    CHECK ((tipo IN ('COMPLETADA', 'PENALIZACION')) = (asignacion_id IS NOT NULL))
);
-- No se puntea ni penaliza dos veces la misma asignación
CREATE UNIQUE INDEX ux_movimiento ON movimientos_puntos (asignacion_id, tipo) WHERE asignacion_id IS NOT NULL;
CREATE INDEX ix_movimientos_sala_usuario ON movimientos_puntos (sala_id, usuario_id, creado_en);

-- ─── Finanzas ───

CREATE TABLE servicios_pago (
    id            bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sala_id       bigint NOT NULL REFERENCES salas (id) ON DELETE CASCADE,
    concepto      text NOT NULL CHECK (length(concepto) BETWEEN 1 AND 80),
    monto         numeric(12, 2) NOT NULL CHECK (monto > 0),
    moneda        text NOT NULL CHECK (moneda IN ('MXN', 'USD')),
    frecuencia    text NOT NULL CHECK (frecuencia IN ('UNICA', 'SEMANAL', 'MENSUAL', 'BIMESTRAL')),
    fecha_inicio  date NOT NULL,
    eliminado     boolean NOT NULL DEFAULT false,
    eliminado_por bigint REFERENCES usuarios (id),
    eliminado_en  timestamptz,
    creado_en     timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE servicio_participantes (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    servicio_id bigint NOT NULL REFERENCES servicios_pago (id) ON DELETE CASCADE,
    sala_id     bigint NOT NULL REFERENCES salas (id) ON DELETE CASCADE,
    usuario_id  bigint NOT NULL REFERENCES usuarios (id),
    orden       smallint NOT NULL,             -- fijo, para rotar los centavos (RN-22)
    activo      boolean NOT NULL DEFAULT true,
    UNIQUE (servicio_id, usuario_id),
    UNIQUE (servicio_id, orden)
);

CREATE TABLE deudas_usuarios (
    id                           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    servicio_id                  bigint NOT NULL REFERENCES servicios_pago (id) ON DELETE CASCADE,
    sala_id                      bigint NOT NULL REFERENCES salas (id) ON DELETE CASCADE,
    periodo                      date NOT NULL,          -- día de inicio del periodo, en la zona de la sala
    deudor_id                    bigint NOT NULL REFERENCES usuarios (id),
    monto                        numeric(12, 2) NOT NULL CHECK (monto >= 0),
    estado                       text NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE', 'APROBADA', 'REPARTIDA')),
    deuda_origen_id              bigint REFERENCES deudas_usuarios (id) ON DELETE CASCADE,
    aviso_pago_en                timestamptz,
    recordatorio_confirmacion_en timestamptz,
    recordatorios_enviados       smallint NOT NULL DEFAULT 0 CHECK (recordatorios_enviados BETWEEN 0 AND 3),
    ultimo_recordatorio_en       timestamptz,
    creado_en                    timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_deuda_periodo ON deudas_usuarios (servicio_id, periodo, deudor_id)
    WHERE deuda_origen_id IS NULL;
CREATE UNIQUE INDEX ux_deuda_reparto ON deudas_usuarios (deuda_origen_id, deudor_id)
    WHERE deuda_origen_id IS NOT NULL;

CREATE TABLE confirmaciones_pago (
    id             bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    deuda_id       bigint NOT NULL REFERENCES deudas_usuarios (id) ON DELETE CASCADE,
    sala_id        bigint NOT NULL REFERENCES salas (id) ON DELETE CASCADE,
    confirmador_id bigint NOT NULL REFERENCES usuarios (id),
    creado_en      timestamptz NOT NULL DEFAULT now(),
    client_uuid    uuid UNIQUE,
    UNIQUE (deuda_id, confirmador_id)
);

-- ─── Offline ───

CREATE TABLE acciones_procesadas (
    client_uuid  uuid PRIMARY KEY,
    usuario_id   bigint NOT NULL REFERENCES usuarios (id),
    sala_id      bigint NOT NULL REFERENCES salas (id) ON DELETE CASCADE,
    procesada_en timestamptz NOT NULL DEFAULT now()
);

-- ─── Contexto de RLS ───

-- nullif: tras una transacción con set_config(..., true), la conexión del pool devuelve '' y no
-- nulo; sin nullif, ''::bigint lanzaría un error en vez de no devolver filas.
CREATE FUNCTION usuario_actual() RETURNS bigint
    LANGUAGE sql STABLE AS $$ SELECT nullif(current_setting('app.usuario_id', true), '')::bigint $$;

CREATE FUNCTION sala_del_sistema() RETURNS bigint
    LANGUAGE sql STABLE AS $$ SELECT nullif(current_setting('app.sala_id', true), '')::bigint $$;

-- Membresías activas del usuario actual. SECURITY DEFINER evita la recursión en usuario_sala.
CREATE FUNCTION salas_del_usuario() RETURNS SETOF bigint
    LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public, pg_temp AS $$
    SELECT sala_id FROM usuario_sala WHERE usuario_id = usuario_actual() AND activo $$;

-- Salas que creó el usuario actual. Va como función porque la política de INSERT de usuario_sala
-- no puede consultar salas directamente: la política de salas consulta usuario_sala y PostgreSQL
-- lo rechaza como recursión infinita.
CREATE FUNCTION salas_creadas_por_usuario() RETURNS SETOF bigint
    LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public, pg_temp AS $$
    SELECT id FROM salas WHERE creador_id = usuario_actual() $$;

-- CU-03: datos mínimos para unirse sin ser miembro todavía
CREATE FUNCTION buscar_sala_por_codigo(p_codigo text)
    RETURNS TABLE (sala_id bigint, estado text, miembros_activos int)
    LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public, pg_temp AS $$
    SELECT s.id, s.estado,
           (SELECT count(*) FROM usuario_sala us WHERE us.sala_id = s.id AND us.activo)::int
    FROM salas s WHERE s.codigo_invitacion = p_codigo $$;

-- Salas que el proceso programado debe revisar.
-- shortcut: devuelve todas las salas activas y no calcula periodos en SQL; filtrar aquí cuando haya
-- tantas salas que recorrerlas cada 15 minutos sea lento.
CREATE FUNCTION salas_con_trabajo_pendiente() RETURNS SETOF bigint
    LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public, pg_temp AS $$
    SELECT id FROM salas
    WHERE estado = 'ACTIVA' OR desactivada_en < now() - interval '15 days' $$;

-- ─── Políticas ───
-- Ninguna tabla usa FORCE: app_user y app_scheduler no son dueños, así que RLS ya los filtra,
-- y con FORCE las funciones SECURITY DEFINER (del dueño) no verían ninguna fila.

ALTER TABLE salas ENABLE ROW LEVEL SECURITY;

CREATE POLICY crear_sala ON salas FOR INSERT TO app_user
    WITH CHECK (creador_id = usuario_actual());

-- El creador ve su sala solo mientras no tiene membresía en ella, es decir, durante su creación.
CREATE POLICY ver_salas ON salas FOR SELECT TO app_user
    USING (id IN (SELECT salas_del_usuario())
           OR (creador_id = usuario_actual()
               AND NOT EXISTS (SELECT 1 FROM usuario_sala us WHERE us.sala_id = salas.id)));

CREATE POLICY actualizar_sala ON salas FOR UPDATE TO app_user
    USING (id IN (SELECT salas_del_usuario()));

CREATE POLICY borrar_sala ON salas FOR DELETE TO app_user
    USING (id IN (SELECT salas_del_usuario()));

CREATE POLICY sistema_una_sala ON salas TO app_scheduler
    USING (id = sala_del_sistema());

ALTER TABLE usuario_sala ENABLE ROW LEVEL SECURITY;

CREATE POLICY ver_membresias ON usuario_sala FOR SELECT TO app_user
    USING (usuario_id = usuario_actual() OR sala_id IN (SELECT salas_del_usuario()));

CREATE POLICY insertar_propia_membresia ON usuario_sala FOR INSERT TO app_user
    WITH CHECK (usuario_id = usuario_actual()
                AND (rol = 'HABITANTE'
                     OR sala_id IN (SELECT salas_creadas_por_usuario())));

CREATE POLICY actualizar_membresias ON usuario_sala FOR UPDATE TO app_user
    USING (usuario_id = usuario_actual() OR sala_id IN (SELECT salas_del_usuario()));

CREATE POLICY sistema_una_sala ON usuario_sala TO app_scheduler
    USING (sala_id = sala_del_sistema());

-- Tablas con datos de una sala: la misma pareja de políticas en cada una.
DO $$
DECLARE
    tabla text;
BEGIN
    FOREACH tabla IN ARRAY ARRAY[
        'notas_tablon', 'reacciones_nota', 'chat_mensajes', 'reportes',
        'tareas', 'tarea_asignaciones', 'movimientos_puntos',
        'servicios_pago', 'servicio_participantes', 'deudas_usuarios', 'confirmaciones_pago',
        'acciones_procesadas']
    LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', tabla);
        EXECUTE format('CREATE POLICY usuario_sus_salas ON %I TO app_user
                            USING (sala_id IN (SELECT salas_del_usuario()))', tabla);
        EXECUTE format('CREATE POLICY sistema_una_sala ON %I TO app_scheduler
                            USING (sala_id = sala_del_sistema())', tabla);
    END LOOP;
END $$;

-- ─── Permisos ───

GRANT USAGE ON SCHEMA public TO app_user, app_scheduler;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO app_user, app_scheduler;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO app_user, app_scheduler;
-- Las tablas de migraciones futuras reciben los mismos permisos
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO app_user, app_scheduler;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO app_user, app_scheduler;
-- El historial de Flyway es solo del dueño
REVOKE ALL ON flyway_schema_history FROM app_user, app_scheduler;

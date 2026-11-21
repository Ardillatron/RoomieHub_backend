-- RN-39: una tarea se repite cada N días (1 a 365) o cada mes, guardado como periodo ISO-8601 (P3D, P1M).
ALTER TABLE tareas DROP CONSTRAINT tareas_frecuencia_check;

UPDATE tareas
SET frecuencia = CASE frecuencia WHEN 'DIARIA' THEN 'P1D' WHEN 'SEMANAL' THEN 'P7D' ELSE 'P1M' END;

ALTER TABLE tareas ADD CONSTRAINT tareas_frecuencia_check CHECK (
    frecuencia = 'P1M'
    OR (frecuencia ~ '^P[1-9][0-9]{0,2}D$' AND substring(frecuencia FROM 2 FOR length(frecuencia) - 2)::int <= 365)
);

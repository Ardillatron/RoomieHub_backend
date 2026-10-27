-- RN-41: primer día cuyo periodo genera deudas. Al reactivar una sala avanza a ese día, para que los
-- periodos que empezaron mientras estuvo desactivada no generen deudas.
ALTER TABLE servicios_pago ADD COLUMN generar_desde date;
UPDATE servicios_pago SET generar_desde = fecha_inicio;
ALTER TABLE servicios_pago ALTER COLUMN generar_desde SET NOT NULL;

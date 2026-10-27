-- Se guarda el identificador de la imagen en el almacenamiento (public_id de Cloudinary), no una URL:
-- la entrega es autenticada y la URL firmada se genera al leer (AlmacenamientoImagenesPort).
-- Las restricciones CHECK que usan la columna se actualizan solas con el nuevo nombre.
ALTER TABLE notas_tablon RENAME COLUMN imagen_url TO imagen_id;
ALTER TABLE chat_mensajes RENAME COLUMN imagen_url TO imagen_id;

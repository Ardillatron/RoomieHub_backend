package roomiehub.vivienda.application.port;

import java.util.Map;

/**
 * Sección 2: las imágenes viven fuera del servidor (su disco es efímero, sección 12). La app sube directo
 * al proveedor con una firma del backend, y para verlas se usan URLs firmadas.
 */
public interface AlmacenamientoImagenesPort {

    /**
     * @param urlDeSubida a dónde manda la app el archivo (multipart, campo "file")
     * @param imagenId    el identificador que tendrá la imagen; la app lo envía después con la nota o mensaje
     * @param parametros  campos firmados que la app manda tal cual junto con el archivo
     */
    record FirmaDeSubida(String urlDeSubida, String imagenId, Map<String, String> parametros) {}

    /** Firma la subida de una imagen nueva dentro de {@code carpeta}, con nombre aleatorio. */
    FirmaDeSubida firmarSubida(String carpeta);

    /** URL firmada de la imagen completa (vista ampliada). */
    String urlDeEntrega(String imagenId);

    /** URL firmada de una versión pequeña, para listas como el tablón. */
    String urlDeMiniatura(String imagenId);

    /**
     * Borra todas las imágenes de {@code carpeta} cuando se confirma la transacción en curso (RN-09, RN-10):
     * las URLs firmadas no caducan, así que borrar la sala no basta para que dejen de verse.
     */
    void borrarCarpeta(String carpeta);
}

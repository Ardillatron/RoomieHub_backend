package roomiehub.vivienda.application.port;

import java.util.Map;

/* Las imagenes viven fuera del servidor porque su disco se borra
   la app sube directo al proveedor con una firma y se ven con URLs firmadas */
public interface AlmacenamientoImagenesPort {

    /* urlDeSubida es a donde manda la app el archivo
       imagenId es el id que tendra la imagen y la app lo manda con la nota o mensaje
       parametros son campos firmados que la app manda tal cual con el archivo */
    record FirmaDeSubida(String urlDeSubida, String imagenId, Map<String, String> parametros) {}

    // Firma la subida de una imagen nueva en la carpeta con nombre al azar
    FirmaDeSubida firmarSubida(String carpeta);

    // URL firmada de la imagen completa
    String urlDeEntrega(String imagenId);

    // URL firmada de una version pequeña para listas como el tablon
    String urlDeMiniatura(String imagenId);

    /* Borra todas las imagenes de la carpeta cuando se confirma la transaccion
       las URLs firmadas no caducan y borrar la sala no basta */
    void borrarCarpeta(String carpeta);
}

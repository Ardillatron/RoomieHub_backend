package roomiehub.vivienda.application.usecase.convivencia;

import roomiehub.vivienda.domain.model.convivencia.MensajeChat;

/* Un mensaje listo para mostrar con nombre del autor imagenes firmadas
   y la nota citada si responde una */
public record MensajeVista(MensajeChat mensaje, String autor, String imagenUrl, String imagenMiniaturaUrl,
                           NotaCitada notaCitada) {

    // Texto e imagen en null si la nota citada se borro
    public record NotaCitada(long id, String texto, String imagenMiniaturaUrl) {}
}

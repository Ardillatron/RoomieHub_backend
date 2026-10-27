package roomiehub.vivienda.application.usecase.convivencia;

import roomiehub.vivienda.domain.model.convivencia.MensajeChat;

/**
 * Un mensaje listo para mostrar: nombre del autor, imágenes firmadas y, si responde una nota (RN-30), la
 * nota citada.
 */
public record MensajeVista(MensajeChat mensaje, String autor, String imagenUrl, String imagenMiniaturaUrl,
                           NotaCitada notaCitada) {

    /** {@code null} en texto e imagen si la nota citada se eliminó después. */
    public record NotaCitada(long id, String texto, String imagenMiniaturaUrl) {}
}

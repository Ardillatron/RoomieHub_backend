package roomiehub.vivienda.application.usecase.convivencia;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import roomiehub.vivienda.domain.exception.DatoInvalidoException;

class FirmarSubidaDeImagenTest {

    @Test
    void soloAceptaImagenesFirmadasParaEsaSala() {
        String propia = "roomiehub/salas/7/" + UUID.randomUUID();
        FirmarSubidaDeImagen.exigirImagenDeLaSala(propia, 7);

        assertThrows(DatoInvalidoException.class, () -> FirmarSubidaDeImagen.exigirImagenDeLaSala(propia, 8));
        assertThrows(DatoInvalidoException.class,
                () -> FirmarSubidaDeImagen.exigirImagenDeLaSala("roomiehub/salas/77/" + UUID.randomUUID(), 7),
                "la sala 77 empieza igual que la 7");
        assertThrows(DatoInvalidoException.class,
                () -> FirmarSubidaDeImagen.exigirImagenDeLaSala("roomiehub/salas/7/../8/foto", 7));
        assertThrows(DatoInvalidoException.class,
                () -> FirmarSubidaDeImagen.exigirImagenDeLaSala("https://evil.example/x.png", 7));
    }
}

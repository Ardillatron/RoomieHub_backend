package roomiehub.vivienda.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import roomiehub.vivienda.domain.exception.DatoInvalidoException;
import roomiehub.vivienda.domain.model.tareas.ValorTarea;

class CalculadoraDePuntosTest {

    @Test
    void ejemploDelDocumento() {
        // Promedio 21.4 + tarea más alta 8 = 29.4 → 30
        assertEquals(30, CalculadoraDePuntos.puntajeInicial(List.of(15, 20, 23, 24, 25), 8));
    }

    @Test
    void creadorIniciaEnCero() {
        assertEquals(0, CalculadoraDePuntos.puntajeInicial(List.of(), 8));
    }

    @Test
    void sinTareasLaMasAltaCuentaComoCero() {
        assertEquals(11, CalculadoraDePuntos.puntajeInicial(List.of(10, 11), 0));
    }

    @Test
    void redondeaHaciaArribaConNegativos() {
        // (-7 + -2) / 2 + 1 = -3.5 → -3
        assertEquals(-3, CalculadoraDePuntos.puntajeInicial(List.of(-7, -2), 1));
    }

    @Test
    void valorFueraDeRango() {
        assertThrows(DatoInvalidoException.class, () -> CalculadoraDePuntos.puntajeInicial(List.of(1), 11));
    }

    @Test
    void penalizacionEsLaMitadHaciaArriba() {
        assertEquals(4, new ValorTarea(8).penalizacion());
        assertEquals(3, new ValorTarea(5).penalizacion());
        assertEquals(1, new ValorTarea(1).penalizacion());
        assertThrows(DatoInvalidoException.class, () -> new ValorTarea(11));
        assertThrows(DatoInvalidoException.class, () -> new ValorTarea(0));
    }
}

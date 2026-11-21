package roomiehub.vivienda.domain.model.tareas;

import roomiehub.vivienda.domain.exception.DatoInvalidoException;

// Dificultad de 1 a 10
public record ValorTarea(int valor) {

    public ValorTarea {
        if (valor < 1 || valor > 10) {
            throw new DatoInvalidoException("El valor de una tarea va de 1 a 10, no " + valor);
        }
    }

    // Se resta la mitad del valor redondeada hacia arriba
    public int penalizacion() {
        return (valor + 1) / 2;
    }
}

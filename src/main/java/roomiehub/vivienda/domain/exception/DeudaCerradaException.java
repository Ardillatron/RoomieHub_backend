package roomiehub.vivienda.domain.exception;

// Una deuda Aprobada o Repartida ya no cambia
public class DeudaCerradaException extends DomainException {

    public DeudaCerradaException() {
        super("La deuda ya está cerrada y no puede cambiar");
    }
}

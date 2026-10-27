package roomiehub.vivienda.domain.exception;

/** RN-01. */
public class CapacidadExcedidaException extends DomainException {

    public CapacidadExcedidaException() {
        super("La sala ya tiene 6 personas");
    }
}

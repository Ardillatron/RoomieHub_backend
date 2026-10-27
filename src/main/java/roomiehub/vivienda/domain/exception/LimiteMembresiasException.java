package roomiehub.vivienda.domain.exception;

/** RN-02. */
public class LimiteMembresiasException extends DomainException {

    public LimiteMembresiasException() {
        super("Ya perteneces a 2 salas, el máximo permitido");
    }
}

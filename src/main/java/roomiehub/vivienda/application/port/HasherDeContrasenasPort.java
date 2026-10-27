package roomiehub.vivienda.application.port;

public interface HasherDeContrasenasPort {

    String hash(String contrasena);

    boolean coincide(String contrasena, String hash);
}

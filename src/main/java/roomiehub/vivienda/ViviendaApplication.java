package roomiehub.vivienda;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Vive en la raíz y no en infrastructure/config para que el escaneo de componentes,
// entidades JPA y repositorios cubra todas las capas sin configuración extra.
@SpringBootApplication
public class ViviendaApplication {

    public static void main(String[] args) {
        SpringApplication.run(ViviendaApplication.class, args);
    }
}

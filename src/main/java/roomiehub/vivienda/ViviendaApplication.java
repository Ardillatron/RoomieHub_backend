package roomiehub.vivienda;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/* Vive en la raiz para que el escaneo de componentes entidades y repositorios
   cubra todas las capas sin configuracion extra */
@SpringBootApplication
public class ViviendaApplication {

    public static void main(String[] args) {
        SpringApplication.run(ViviendaApplication.class, args);
    }
}

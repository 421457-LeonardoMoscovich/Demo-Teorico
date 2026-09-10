package ar.edu.utn.tup.g04.teoricosencuestas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class MsTeoricosEncuestasApplication {

    static {
        // El driver de PostgreSQL manda la zona horaria por defecto de la JVM en
        // el mensaje de arranque de cada conexion, y PostgreSQL rechaza los ids
        // que no conoce: una maquina con America/Buenos_Aires (en vez de
        // America/Argentina/Buenos_Aires) no puede ni abrir la conexion.
        //
        // Fijarla en UTC no es un parche del entorno: el servicio guarda
        // timestamptz y publica timestamps ISO 8601 en UTC en el envelope de
        // eventos, asi que trabajar en UTC adentro es lo que ya haciamos.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    public static void main(String[] args) {
        SpringApplication.run(MsTeoricosEncuestasApplication.class, args);
    }
}

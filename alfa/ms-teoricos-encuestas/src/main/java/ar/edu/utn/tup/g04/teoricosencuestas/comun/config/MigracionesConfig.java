package ar.edu.utn.tup.g04.teoricosencuestas.comun.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Un migrador por esquema, con su propia tabla de historia.
 *
 * Corren todos con dsMigraciones (app_owner), el unico rol con DDL. El
 * EntityManagerFactory declara @DependsOn sobre estos beans, asi que la
 * validacion del mapeo nunca corre antes que las migraciones.
 */
@Configuration
public class MigracionesConfig {

    /** Marca de que un esquema quedo migrado. Existe para el @DependsOn. */
    public record EsquemaMigrado(String esquema) {}

    @Bean
    public EsquemaMigrado migradorTeoricos(@Qualifier("dsMigraciones") DataSource dsMigraciones) {
        Flyway.configure()
                .dataSource(dsMigraciones)
                .schemas("teoricos")
                .locations("classpath:db/migration/teoricos")
                .table("flyway_history")
                .load()
                .migrate();
        return new EsquemaMigrado("teoricos");
    }
}

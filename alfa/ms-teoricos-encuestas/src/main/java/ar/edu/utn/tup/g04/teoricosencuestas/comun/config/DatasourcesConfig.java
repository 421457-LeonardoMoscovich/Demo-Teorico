package ar.edu.utn.tup.g04.teoricosencuestas.comun.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Un origen de datos por rol de PostgreSQL.
 *
 * En la alfa son dos. Cuando entre el modulo `encuestas` (G04-HU06) se suman
 * dsCatalogo, dsCumplimiento y dsRespuestas: son tres bloques mas de yml y tres
 * beans mas aca, no un refactor. Por eso no existe un datasource "por defecto".
 */
@Configuration
public class DatasourcesConfig {

    /** app_owner: solo DDL, en el arranque. Ningun repositorio lo usa. */
    @Bean
    @ConfigurationProperties("app.datasource.migraciones")
    public DataSourceProperties propiedadesMigraciones() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource dsMigraciones(DataSourceProperties propiedadesMigraciones) {
        HikariDataSource ds = propiedadesMigraciones.initializeDataSourceBuilder()
                .type(HikariDataSource.class).build();
        ds.setMaximumPoolSize(2);
        ds.setPoolName("hikari-migraciones");
        return ds;
    }

    @Bean
    @ConfigurationProperties("app.datasource.teoricos")
    public DataSourceProperties propiedadesTeoricos() {
        return new DataSourceProperties();
    }

    /**
     * @Primary UNICAMENTE para que Spring Boot arme el EntityManagerFactoryBuilder,
     * que solo se autoconfigura cuando hay un DataSource candidato unico.
     *
     * No significa que sea el origen de datos por defecto del codigo: cada
     * @EnableJpaRepositories apunta a un solo paquete, y a partir de G04-HU02 el
     * test de arquitectura falla si un repositorio queda cubierto por dos
     * configuraciones o por ninguna. La marca es del arranque, no del diseno.
     */
    @Bean
    @Primary
    public DataSource dsTeoricos(DataSourceProperties propiedadesTeoricos) {
        HikariDataSource ds = propiedadesTeoricos.initializeDataSourceBuilder()
                .type(HikariDataSource.class).build();
        ds.setPoolName("hikari-teoricos");
        return ds;
    }
}

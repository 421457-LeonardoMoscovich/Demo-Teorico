package ar.edu.utn.tup.g04.teoricosencuestas.comun.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Map;

/**
 * El EMF del modulo `teoricos`, acotado a UN solo paquete de entidades y de
 * repositorios. Si alguien inyecta el repositorio equivocado no compila o falla
 * al arrancar, no en produccion.
 */
@Configuration
@EnableJpaRepositories(
        basePackages = "ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia",
        entityManagerFactoryRef = "emfTeoricos",
        transactionManagerRef = "txTeoricos")
public class JpaTeoricosConfig {

    @Bean
    @Primary
    @DependsOn("migradorTeoricos")
    public LocalContainerEntityManagerFactoryBean emfTeoricos(
            EntityManagerFactoryBuilder builder,
            @Qualifier("dsTeoricos") DataSource dsTeoricos) {
        return builder
                .dataSource(dsTeoricos)
                .packages("ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia")
                .persistenceUnit("teoricos")
                .properties(Map.of("hibernate.default_schema", "teoricos"))
                .build();
    }

    @Bean
    @Primary
    public PlatformTransactionManager txTeoricos(@Qualifier("emfTeoricos") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}

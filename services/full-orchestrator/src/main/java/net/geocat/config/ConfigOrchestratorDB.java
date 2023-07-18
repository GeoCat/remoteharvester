package net.geocat.config;


import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        entityManagerFactoryRef = "entityManagerFactoryOrchestrator",
        basePackages = {"net.geocat.database.orchestrator.repos"}
)
public class ConfigOrchestratorDB {

    @Autowired
    Environment env;


    // @Primary
    @Bean(name = "dataSourceOrchestrator")
    @ConfigurationProperties(prefix = "orchestrator.datasource")
    public DataSource dataSource() {

        DataSource dataSource = DataSourceBuilder.create().build();
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hds = (HikariDataSource) dataSource;
            hds.setDriverClassName(env.getProperty("orchestrator.datasource.driver-class-name"));
            hds.setJdbcUrl(env.getProperty("orchestrator.datasource.url"));
            hds.setUsername(env.getProperty("orchestrator.datasource.username"));
            hds.setPassword(env.getProperty("orchestrator.datasource.password"));
        }
        return dataSource;
    }

    @Primary
    @Bean(name = "entityManagerFactoryOrchestrator")
    public LocalContainerEntityManagerFactoryBean
    entityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("dataSourceOrchestrator") DataSource dataSource,
            JpaProperties jpaProperties
    ) {
        return builder
                .dataSource(dataSource)
                .packages("net.geocat.database.orchestrator.entities")
                .persistenceUnit("orchestrator")
                .properties(jpaProperties.getProperties())
                .build();
    }

    // @Primary
    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager(
            @Qualifier("entityManagerFactoryOrchestrator") EntityManagerFactory
                    entityManagerFactory
    ) {
        return new JpaTransactionManager(entityManagerFactory);
    }

}

package com.geocat.ingester.config;

import com.google.common.base.Preconditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;

@Configuration
@EnableJpaRepositories(basePackages = "com.geocat.ingester.dao.ingester", entityManagerFactoryRef = "ingesterEntityManager", transactionManagerRef = "ingesterTransactionManager")
@Profile("!tc")
public class PersistenceIngesterCamelConfiguration {
    @Autowired
    private Environment env;

    public PersistenceIngesterCamelConfiguration() {
        super();
    }

    //

    @Bean
    public LocalContainerEntityManagerFactoryBean ingesterEntityManager() {
        final LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(ingesterDataSource());
        em.setPackagesToScan("com.geocat.ingester.model.ingester");


        final HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        final HashMap<String, Object> properties = new HashMap<String, Object>();
        //properties.put("hibernate.hbm2ddl.auto", env.getProperty("hibernate.hbm2ddl.auto"));
        properties.put("hibernate.hbm2ddl.auto", "update");
        properties.put("hibernate.physical_naming_strategy", env.getProperty("hibernate.physical_naming_strategy"));
        em.setJpaPropertyMap(properties);

        return em;
    }

    @Bean
    public DataSource ingesterDataSource() {
        final DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(Preconditions.checkNotNull(env.getProperty("jdbc.driverClassName")));
        dataSource.setUrl(Preconditions.checkNotNull(env.getProperty("ingester.jdbc.url")));
        dataSource.setUsername(Preconditions.checkNotNull(env.getProperty("ingester.jdbc.user")));
        dataSource.setPassword(Preconditions.checkNotNull(env.getProperty("ingester.jdbc.pass")));

        return dataSource;
    }

    @Bean
    public PlatformTransactionManager ingesterTransactionManager() {
        final JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(ingesterEntityManager().getObject());
        return transactionManager;
    }


    /*@Bean(name = "PROPAGATION_INGEST_REQUIRED")
    public SpringTransactionPolicy propagationIngestRequired() {
        SpringTransactionPolicy propagationRequired = new SpringTransactionPolicy();
        propagationRequired.setTransactionManager(ingesterTransactionManager());
        propagationRequired.setPropagationBehaviorName("PROPAGATION_REQUIRED");
        return propagationRequired;
    }*/

}

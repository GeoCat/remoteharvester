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
@EnableJpaRepositories(basePackages = "com.geocat.ingester.dao.metadata", entityManagerFactoryRef = "metadataEntityManager", transactionManagerRef = "metadataTransactionManager")
@Profile("!tc")
public class PersistenceMetadataConfiguration {
    @Autowired
    private Environment env;

    public PersistenceMetadataConfiguration() {
        super();
    }

    //

    @Bean
    public LocalContainerEntityManagerFactoryBean metadataEntityManager() {
        final LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(metadataDataSource());
        em.setPackagesToScan("com.geocat.ingester.model.metadata");


        final HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        final HashMap<String, Object> properties = new HashMap<String, Object>();
        properties.put("hibernate.hbm2ddl.auto", env.getProperty("hibernate.hbm2ddl.auto"));
        //properties.put("hibernate.physical_naming_strategy", env.getProperty("hibernate.physical_naming_strategy"));
        properties.put("hibernate.dialect", env.getProperty("hibernate.dialect"));
        //properties.put("hibernate.generate_statistics", true);
        em.setJpaPropertyMap(properties);

        return em;
    }

    @Bean
    public DataSource metadataDataSource() {
        final DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(Preconditions.checkNotNull(env.getProperty("jdbc.driverClassName")));
        dataSource.setUrl(Preconditions.checkNotNull(env.getProperty("metadata.jdbc.url")));
        dataSource.setUsername(Preconditions.checkNotNull(env.getProperty("metadata.jdbc.user")));
        dataSource.setPassword(Preconditions.checkNotNull(env.getProperty("metadata.jdbc.pass")));

        return dataSource;
    }

    @Bean
    public PlatformTransactionManager metadataTransactionManager() {
        final JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(metadataEntityManager().getObject());
        return transactionManager;
    }

}

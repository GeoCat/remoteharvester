package com.geocat.ingester.config;

import com.google.common.base.Preconditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
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
@EnableJpaRepositories(basePackages = "com.geocat.ingester.dao.harvester", entityManagerFactoryRef = "harvesterEntityManager", transactionManagerRef = "harvesterTransactionManager")
@Profile("!tc")
public class PersistenceHarvesterConfiguration {
    @Autowired
    private Environment env;

    public PersistenceHarvesterConfiguration() {
        super();
    }

    //


    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean harvesterEntityManager() {
        final LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(harvesterDataSource());
        em.setPackagesToScan("com.geocat.ingester.model.harvester");

        final HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        final HashMap<String, Object> properties = new HashMap<String, Object>();
        properties.put("hibernate.hbm2ddl.auto", env.getProperty("hibernate.hbm2ddl.auto"));
        properties.put("hibernate.physical_naming_strategy", env.getProperty("hibernate.physical_naming_strategy"));
        //properties.put("hibernate.dialect", env.getProperty("hibernate.dialect"));
        em.setJpaPropertyMap(properties);

        return em;
    }


    @Bean
    @Primary
    public DataSource harvesterDataSource() {
        final DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(Preconditions.checkNotNull(env.getProperty("jdbc.driverClassName")));
        dataSource.setUrl(Preconditions.checkNotNull(env.getProperty("harvester.jdbc.url")));
        dataSource.setUsername(Preconditions.checkNotNull(env.getProperty("harvester.jdbc.user")));
        dataSource.setPassword(Preconditions.checkNotNull(env.getProperty("harvester.jdbc.pass")));

        return dataSource;
    }


    @Bean
    @Primary
    public PlatformTransactionManager harvesterTransactionManager() {
        final JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(harvesterEntityManager().getObject());
        return transactionManager;
    }

}

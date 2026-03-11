package com.cobasesys.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Map;

@Configuration
@EnableJpaRepositories(
        basePackages = "com.cobasesys.module.billing",
        entityManagerFactoryRef = "billingEntityManagerFactory",
        transactionManagerRef = "billingTransactionManager"
)
public class BillingDataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.billing")
    public DataSourceProperties billingDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.billing.hikari")
    public DataSource billingDataSource() {
        return billingDataSourceProperties().initializeDataSourceBuilder()
                .type(HikariDataSource.class).build();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean billingEntityManagerFactory(
            EntityManagerFactoryBuilder builder, @Qualifier("billingDataSource") DataSource ds) {
        return builder.dataSource(ds)
                .packages("com.cobasesys.module.billing.entity")
                .persistenceUnit("billing")
                .properties(Map.of(
                        "hibernate.dialect", "org.hibernate.dialect.MySQLDialect",
                        "hibernate.hbm2ddl.auto", "validate",
                        "hibernate.jdbc.batch_size", "50"
                ))
                .build();
    }

    @Bean
    public PlatformTransactionManager billingTransactionManager(
            @Qualifier("billingEntityManagerFactory") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}

package com.cobasesys.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class FlywayConfig {

    @Bean(initMethod = "migrate")
    public Flyway primaryFlyway(@Qualifier("primaryDataSource") DataSource ds) {
        return Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();
    }

    @Bean(initMethod = "migrate")
    public Flyway billingFlyway(@Qualifier("billingDataSource") DataSource ds) {
        return Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration/mysql")
                .baselineOnMigrate(true)
                .load();
    }
}

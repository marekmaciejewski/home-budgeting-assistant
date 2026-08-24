package pl.mm.testsupport;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;

import javax.sql.DataSource;

@TestConfiguration(proxyBeanMethods = false)
public class LedgerHttpTestConfiguration {

    @Bean
    @Primary
    R2dbcTransactionManager connectionFactoryTransactionManager(ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory);
    }

    @Bean
    DataSource sqlDataSource(@Value("${spring.liquibase.url}") String url) {
        return new DriverManagerDataSource(url);
    }
}

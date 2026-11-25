package br.com.guardia.config;

import java.util.regex.Pattern;
import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
@Profile({"teste", "teste-push"})
public class DataSourceTesteConfig {

    private static final Pattern URL_TESTE = Pattern.compile(
            "jdbc:mysql://(?:localhost|127\\.0\\.0\\.1)(?::[0-9]{1,5})?/guardia_teste");
    private static final String USUARIO_TESTE = "guardia_teste_app";

    @Bean
    DataSource dataSource(Environment ambiente) {
        if (ambiente.matchesProfiles("teste-push") &&
                (!"validate".equals(ambiente.getProperty("spring.jpa.hibernate.ddl-auto")) ||
                 !"never".equals(ambiente.getProperty("spring.sql.init.mode", "never")) ||
                 ambiente.getProperty("spring.flyway.enabled", Boolean.class, false) ||
                 ambiente.getProperty("spring.liquibase.enabled", Boolean.class, false))) {
            throw new IllegalStateException("O ensaio push exige validate e migrações desativadas.");
        }
        String url = ambiente.getProperty("spring.datasource.url", "");
        String usuario = ambiente.getProperty("spring.datasource.username", "");
        if (!URL_TESTE.matcher(url).matches() || !USUARIO_TESTE.equals(usuario)) {
            throw new IllegalStateException(
                    "O perfil teste exige MySQL local, schema guardia_teste e usuário guardia_teste_app.");
        }

        String senha = ambiente.getProperty("GUARDIA_TEST_DB_PASSWORD", "");
        if (senha.isBlank()) {
            throw new IllegalStateException("Defina GUARDIA_TEST_DB_PASSWORD para o perfil teste.");
        }

        HikariDataSource fonte = new HikariDataSource();
        fonte.setJdbcUrl(url);
        fonte.setUsername(USUARIO_TESTE);
        fonte.setPassword(senha);
        return fonte;
    }
}

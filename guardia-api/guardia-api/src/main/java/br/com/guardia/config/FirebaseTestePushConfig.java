package br.com.guardia.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
@Profile("teste-push & !teste")
public class FirebaseTestePushConfig {
    @Bean
    LimitesPushTeste limitesPushTeste(Environment ambiente) {
        return LimitesPushTeste.carregar(ambiente);
    }

    @Bean(destroyMethod = "delete")
    FirebaseApp firebaseAppTestePush(LimitesPushTeste limites, Environment ambiente) {
        String caminho = ambiente.getProperty("GOOGLE_APPLICATION_CREDENTIALS", "");
        if (caminho.isBlank()) {
            throw new IllegalStateException("Defina o arquivo local de credencial Firebase do ensaio.");
        }
        try (var entrada = Files.newInputStream(Path.of(caminho))) {
            var credencial = ServiceAccountCredentials.fromStream(entrada);
            if (!limites.projetoId().equals(credencial.getProjectId())) {
                throw new IllegalStateException("A credencial Firebase pertence a outro projeto.");
            }
            return FirebaseApp.initializeApp(FirebaseOptions.builder()
                    .setCredentials(credencial).setProjectId(limites.projetoId()).build(), "guardia-teste-push");
        } catch (IOException | java.nio.file.InvalidPathException e) {
            throw new IllegalStateException("Não foi possível ler a credencial local do ensaio.");
        }
    }

    @Bean
    FirebaseMessaging firebaseMessagingTestePush(FirebaseApp firebaseAppTestePush) {
        return FirebaseMessaging.getInstance(firebaseAppTestePush);
    }
}

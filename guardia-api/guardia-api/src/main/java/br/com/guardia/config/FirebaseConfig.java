package br.com.guardia.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import jakarta.annotation.PostConstruct;

import java.io.IOException;

@Configuration
@Profile("!teste & !teste-push")
public class FirebaseConfig {

    @PostConstruct
    public void inicializarFirebase() throws IOException {

        if (FirebaseApp.getApps().isEmpty()) {

            FirebaseOptions options =
                    FirebaseOptions.builder()
                            .setCredentials(
                                    GoogleCredentials.getApplicationDefault()
                            )
                            .build();

            FirebaseApp.initializeApp(options);

            System.out.println(
                    "Firebase Admin inicializado com sucesso!"
            );
        }
    }
}

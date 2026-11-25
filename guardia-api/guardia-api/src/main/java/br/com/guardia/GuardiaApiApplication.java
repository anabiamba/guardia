package br.com.guardia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GuardiaApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(GuardiaApiApplication.class, args);
    }
}
package config;

import database.InicializadorBanco;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    ApplicationRunner inicializarBanco() {
        return args -> InicializadorBanco.inicializar();
    }
}

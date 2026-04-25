package br.com.fightConnect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableCaching
@EnableAspectJAutoProxy
public class ApiSuporteApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiSuporteApplication.class, args);
    }
}

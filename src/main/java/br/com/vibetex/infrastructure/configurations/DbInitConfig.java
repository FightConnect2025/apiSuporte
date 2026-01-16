package br.com.vibetex.infrastructure.configurations;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DbInitConfig {

    @Bean
    ApplicationRunner createTicketSequence(JdbcTemplate jdbc) {
        return args -> {
            // Postgres: cria sequence se não existir
            jdbc.execute("""
                DO $$
                BEGIN
                  IF NOT EXISTS (
                    SELECT 1
                    FROM pg_class c
                    JOIN pg_namespace n ON n.oid = c.relnamespace
                    WHERE c.relkind = 'S'
                      AND c.relname = 'ticket_num_seq'
                  ) THEN
                    CREATE SEQUENCE ticket_num_seq START 1;
                  END IF;
                END
                $$;
            """);
        };
    }
}

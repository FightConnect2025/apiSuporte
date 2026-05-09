package br.com.fightConnect.infrastructure.configurations;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class HealthCheckConfig implements HealthIndicator {

    private final JdbcTemplate jdbc;
    private final RabbitTemplate rabbitTemplate;

    @Override
    public Health health() {
        Map<String, Object> details = new LinkedHashMap<>();
        boolean up = true;

        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
            details.put("database", "UP");
        } catch (Exception e) {
            details.put("database", "DOWN - " + e.getMessage());
            up = false;
        }

        try {
            rabbitTemplate.getConnectionFactory().createConnection().close();
            details.put("rabbitmq", "UP");
        } catch (Exception e) {
            details.put("rabbitmq", "DOWN - " + e.getMessage());
            up = false;
        }

        if (up) {
            return Health.up().withDetails(details).build();
        }
        return Health.down().withDetails(details).build();
    }
}

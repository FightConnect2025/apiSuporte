package br.com.fightConnect.infrastructure.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class PayloadSanitizer {

    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "senha", "password", "newPassword", "oldPassword", "confirmacaoSenha",
            "cpf", "rg", "token", "accessToken", "refreshToken", "apiToken",
            "cartao", "cartaoCredito", "cardNumber", "cvv", "codigoSeguranca",
            "chavePix", "chaveAcesso", "secretKey", "secret", "authorization"
    );

    private static final String MASK = "********";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Object sanitize(Object payload) {
        if (payload == null) {
            return null;
        }

        try {
            JsonNode jsonNode = objectMapper.valueToTree(payload);
            JsonNode sanitized = sanitizeNode(jsonNode);
            return objectMapper.treeToValue(sanitized, Object.class);
        } catch (Exception e) {
            return payload.getClass().getSimpleName() + " (sanitizacao falhou)";
        }
    }

    private JsonNode sanitizeNode(JsonNode node) {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            objectNode.fieldNames().forEachRemaining(field -> {
                JsonNode child = objectNode.get(field);
                if (isSensitiveField(field)) {
                    objectNode.put(field, MASK);
                } else if (child.isObject() || child.isArray()) {
                    sanitizeNode(child);
                }
            });
        } else if (node.isArray()) {
            node.forEach(this::sanitizeNode);
        }
        return node;
    }

    private boolean isSensitiveField(String field) {
        String lower = field.toLowerCase();
        return SENSITIVE_FIELDS.stream().anyMatch(lower::contains);
    }
}

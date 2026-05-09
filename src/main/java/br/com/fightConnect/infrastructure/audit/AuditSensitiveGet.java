package br.com.fightConnect.infrastructure.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditSensitiveGet {
    String entidade() default "LEITURA_SENSIVEL";
    String descricao() default "";
}

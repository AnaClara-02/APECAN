package com.aclg.apecan.despesa.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = DadosDespesaValidosValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DadosDespesaValidos {
	String message() default "Revise os dados fiscais e do banco de pagamento.";
	Class<?>[] groups() default {};
	Class<? extends Payload>[] payload() default {};
}

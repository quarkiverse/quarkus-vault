package io.quarkus.vault.runtime.client;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface Private {

    class Literal extends AnnotationLiteral<Private> implements Private {
        public static final Literal INSTANCE = new Literal();

        private Literal() {
        }
    }

}

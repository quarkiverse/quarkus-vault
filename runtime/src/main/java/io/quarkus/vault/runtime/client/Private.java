package io.quarkus.vault.runtime.client;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;

@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface Private {

    class Literal extends AnnotationLiteral<Private> implements Private {
        public static final Literal INSTANCE = new Literal();

        private Literal() {
        }
    }

}

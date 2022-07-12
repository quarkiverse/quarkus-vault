package io.quarkus.vault.runtime.client;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;

@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface Shared {

    class Literal extends AnnotationLiteral<Shared> implements Shared {
        public static final Literal INSTANCE = new Literal();

        private Literal() {
        }
    }

}

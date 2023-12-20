package io.quarkus.vault.client.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@ExtendWith(VaultClientTestExtension.class)
public @interface VaultClientTest {

    String logLevel() default "debug";

    @interface Mount {
        String type();

        String path();

        String[] options() default "";
    }

    Mount[] secrets() default {};

    Mount[] auths() default {};
}

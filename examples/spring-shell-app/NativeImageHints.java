package com.example;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

import static org.springframework.aot.hint.ExecutableMode.INVOKE;

@Configuration
@ImportRuntimeHints(NativeImageHints.class)
class NativeImageHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        try {
            hints.reflection()
                .registerConstructor(org.hibernate.validator.internal.util.logging.Log_$logger.class.getConstructor(org.jboss.logging.Logger.class), INVOKE)
                .registerField(org.hibernate.validator.internal.util.logging.Messages_$bundle.class.getField("INSTANCE"));
        } catch (NoSuchMethodException | NoSuchFieldException e) {
            throw new RuntimeHintsException(e);
        }
    }

    private static class RuntimeHintsException extends RuntimeException {
        RuntimeHintsException(Throwable cause) {
            super(cause);
        }
    }
}

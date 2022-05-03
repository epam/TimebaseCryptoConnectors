package com.epam.deltix.data.connectors.commons;

import java.util.function.Supplier;
import java.util.logging.Level;

public class JulLogger implements Logger {
    public static Logger forConnector(final String connector) {
        return new JulLogger("com.epam.deltix.data.connectors", connector);
    }

    private final java.util.logging.Logger jul;
    private final String prefix;

    public JulLogger(final String julPrefix, final String id) {
        this(java.util.logging.Logger.getLogger(julPrefix + '.' + id), id);
    }

    public JulLogger(final java.util.logging.Logger jul, final String id) {
        this.jul = jul;
        this.prefix = '[' + id + "] ";
    }

    @Override
    public void warning(final Supplier<String> msgSupplier) {
        if (!jul.isLoggable(Level.WARNING)) {
            return;
        }

        jul.log(Level.WARNING, prefix + msgSupplier.get());
    }

    @Override
    public void warning(final Supplier<String> msgSupplier, final Throwable t) {
        if (!jul.isLoggable(Level.WARNING)) {
            return;
        }

        jul.log(Level.WARNING,prefix + msgSupplier.get(), t);
    }

    @Override
    public void info(final Supplier<String> msgSupplier) {
        if (!jul.isLoggable(Level.INFO)) {
            return;
        }

        jul.log(Level.INFO, prefix + msgSupplier.get());
    }

    @Override
    public void debug(final Supplier<String> msgSupplier) {
        if (!jul.isLoggable(Level.FINE)) {
            return;
        }

        jul.log(Level.FINE, prefix + msgSupplier.get());
    }
}

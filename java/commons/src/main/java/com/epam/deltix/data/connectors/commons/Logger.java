package com.epam.deltix.data.connectors.commons;

import java.util.function.Supplier;

public interface Logger {

    void warning(String msg);

    void warning(String msg, Throwable t);

    void warning(Supplier<String> msgSupplier);

    void warning(Supplier<String> msgSupplier, Throwable t);

    void info(Supplier<String> msgSupplier);

    boolean isDebugEnabled();

    void debug(final String msg);

    void debug(Supplier<String> msgSupplier);

}

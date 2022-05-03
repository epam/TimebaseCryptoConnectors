package com.epam.deltix.data.connectors.commons;

import java.util.function.Supplier;

public interface Logger {

    void warning(Supplier<String> msgSupplier);

    void warning(Supplier<String> msgSupplier, Throwable t);

    void info(Supplier<String> msgSupplier);

    void debug(Supplier<String> msgSupplier);

}

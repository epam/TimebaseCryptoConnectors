package com.epam.deltix.data.connectors.commons;

import java.io.Closeable;

public interface RetriableFactory<C extends Closeable> {

    C create(final ErrorListener errorListener) throws Exception;

}

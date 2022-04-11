package com.epam.deltix.data.connectors.commons;

import java.io.Closeable;

public interface CloseableMessageOutput extends MessageOutput, Closeable {

    @Override
    void close();

}

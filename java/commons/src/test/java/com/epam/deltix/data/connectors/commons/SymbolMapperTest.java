package com.epam.deltix.data.connectors.commons;

import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.timebase.messages.universal.PackageHeader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class SymbolMapperTest {

    @Test
    void renaming() throws Exception {
        final String[] symbols = new String[] {"SYM1", "SYM2=2MYS", "S\\=M3=S\\\\M3"};
        final String[] expectedNormalizedSymbols = new String[] {"SYM1", "SYM2", "S=M3"};
        final String[] expectedReceivedSymbols = new String[] {"SYM1", "2MYS", "S\\M3"};

        final List<String> receivedSymbols = new ArrayList<>();

        final SymbolMapper mapper = new SymbolMapper(() -> new CloseableMessageOutput() {
            @Override
            public void close() {
            }

            @Override
            public void send(final InstrumentMessage message) {
                receivedSymbols.add(message.getSymbol().toString());
            }
        }, symbols);

        Assertions.assertArrayEquals(expectedNormalizedSymbols, mapper.normalized());

        final MessageOutput output = mapper.create();

        final PackageHeader message = new PackageHeader();
        Arrays.stream(mapper.normalized()).forEach(s -> { message.setSymbol(s); output.send(message); });

        Assertions.assertArrayEquals(expectedReceivedSymbols, receivedSymbols.toArray());
    }
}
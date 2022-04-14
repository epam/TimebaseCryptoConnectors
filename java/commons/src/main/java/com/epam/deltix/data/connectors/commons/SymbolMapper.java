package com.epam.deltix.data.connectors.commons;

import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.collections.CharSequenceToObjectMap;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>This class is thread-safe.
 * <p>Symbology for the symbols: "NAME_FOR_CONNECTOR[=NAME_FOR_TIMEBASE]". If a symbol name contains char '=',
 * use escaping with the '\' character. For '\' use escaping '\\'. Examples:
 * <ul>
 *   <li>BTC/USD - the data connector will request the vendor using the "BTC/USD" name and the same name writing the result data to TB</li>
 *   <li>BTC-USD=BTC/USD - the data connector will request the vendor using the "BTC-USD" name and the "BTC/USD" name writing the result data to TB</li>
 *   <li>BTC\=USD=BTC\\USD - the data connector will request the vendor using the "BTC=USD" name and the "BTC\USD" name writing the result data to TB</li>
 * </ul>
 * <p>
 */
public class SymbolMapper implements CloseableMessageOutputFactory {
    private final CharSequenceToObjectMap<String> mapping = new CharSequenceToObjectMap<>(); // guarded by itself
    private final CloseableMessageOutputFactory delegate;
    private final String[] normalizedSymbols;

    public SymbolMapper(final CloseableMessageOutputFactory delegate, final String... symbols) {
        this.delegate = delegate;

        final List<Symbol> preparedSymbols = symbols != null ?
            Arrays.asList(symbols).stream().
                    map(s -> new Symbol(s)).
                    filter(s -> !s.from.isEmpty()).
                    collect(Collectors.toList()) : Collections.emptyList();

        normalizedSymbols = preparedSymbols.stream().
                map(s -> s.from).toArray(String[]::new);

        preparedSymbols.stream().
                filter(s -> !s.to.isEmpty()).
                forEach(s -> mapping.put(s.from, s.to));
    }

    public String[] normalized() {
        return normalizedSymbols;
    }

    @Override
    public CloseableMessageOutput create() throws Exception {
        final CloseableMessageOutput output = delegate.create();

        synchronized (mapping) {
            if (mapping.isEmpty()) {
                return output;
            }
        }

        return new CloseableMessageOutput() {
            @Override
            public void close() {
                output.close();
            }

            @Override
            public void send(final InstrumentMessage message) {
                final CharSequence from = message.getSymbol();
                if (from == null) {
                    output.send(message);
                    return;
                }

                final String to;
                synchronized (mapping) {
                    to = mapping.get(from);
                }

                if (to == null) {
                    output.send(message);
                    return;
                }

                message.setSymbol(to);
                output.send(message);
                message.setSymbol(from);
            }
        };
    }

    private static class Symbol {
        private static final int INITIAL_STATE = 0;
        private static final int FROM_STATE = INITIAL_STATE + 1;
        private static final int ESCAPING_IN_FROM_STATE = FROM_STATE + 1;
        private static final int TO_STATE = ESCAPING_IN_FROM_STATE + 1;
        private static final int ESCAPING_IN_TO_STATE = TO_STATE + 1;

        private final String from;
        private final String to;

        private Symbol(final String symbol) {
            final StringBuilder fromSymbol = new StringBuilder();
            final StringBuilder toSymbol = new StringBuilder();

            int state = INITIAL_STATE;
            for (int i = 0; i < symbol.length(); i++) {
                final char c = symbol.charAt(i);
                switch (state) {
                    case INITIAL_STATE:
                        switch (c) {
                            case '\\':
                                state = ESCAPING_IN_FROM_STATE;
                                break;
                            case '=':
                                throw new IllegalArgumentException("Unexpected '=' at " + i);
                            default:
                                state = FROM_STATE;
                                fromSymbol.append(c);
                                break;
                        }
                        break;
                    case FROM_STATE:
                        switch (c) {
                            case '\\':
                                state = ESCAPING_IN_FROM_STATE;
                                break;
                            case '=':
                                state = TO_STATE;
                                break;
                            default:
                                fromSymbol.append(c);
                                break;
                        }
                        break;
                    case ESCAPING_IN_FROM_STATE:
                        switch (c) {
                            case '\\':
                            case '=':
                                state = FROM_STATE;
                                fromSymbol.append(c);
                                break;
                            default:
                                throw new IllegalArgumentException("Unexpected '" + c + "' at " + i);
                        }
                        break;
                    case TO_STATE:
                        switch (c) {
                            case '\\':
                                state = ESCAPING_IN_TO_STATE;
                                break;
                            case '=':
                                throw new IllegalArgumentException("Unexpected '=' at " + i);
                            default:
                                toSymbol.append(c);
                                break;
                        }
                        break;
                    case ESCAPING_IN_TO_STATE:
                        switch (c) {
                            case '\\':
                            case '=':
                                state = TO_STATE;
                                toSymbol.append(c);
                                break;
                            default:
                                throw new IllegalArgumentException("Unexpected '" + c + "' at " + i);
                        }
                        break;
                }
            }

            from = fromSymbol.toString().trim();
            to = toSymbol.toString().trim();
        }

        @Override
        public String toString() {
            return "Symbol{" +
                    "from='" + from + '\'' +
                    ", to='" + to + '\'' +
                    '}';
        }
    }
}

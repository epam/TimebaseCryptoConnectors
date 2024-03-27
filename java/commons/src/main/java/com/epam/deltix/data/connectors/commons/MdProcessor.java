package com.epam.deltix.data.connectors.commons;

import com.epam.deltix.data.connectors.commons.l2.BestBidOfferProducer;
import com.epam.deltix.data.connectors.commons.l2.ChainedL2Listener;
import com.epam.deltix.data.connectors.commons.l2.DefaultEvent;
import com.epam.deltix.data.connectors.commons.l2.DefaultItem;
import com.epam.deltix.data.connectors.commons.l2.L2Processor;
import com.epam.deltix.data.connectors.commons.l2.L2Producer;
import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.qsrv.hf.pub.ExchangeCodec;
import com.epam.deltix.qsrv.hf.tickdb.pub.TimeConstants;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.timebase.messages.service.FeedStatus;
import com.epam.deltix.timebase.messages.service.SecurityFeedStatusMessage;
import com.epam.deltix.timebase.messages.universal.AggressorSide;
import com.epam.deltix.util.collections.CharSequenceToObjectMap;

import java.util.ArrayList;
import java.util.List;

/**
 * Threading model of the class: onBookSnapshot(), onBookUpdate(), onTrade() must be called from one single thread;
 * close(String cause) may be called from another thread.
 */
public class MdProcessor {
    public static MdProcessor create(
            final String exchangeId,
            final MessageOutput output,
            final MdModel.Options selected,
            final int bookSize) {

        return create(exchangeId, output, selected, bookSize, L2Processor.UNLIMITED_BOOK_SIZE);
    }

    public static MdProcessor create(
        final String exchangeId,
        final MessageOutput output,
        final MdModel.Options selected,
        final int bookSize,
        final int fixedBookSize) {

        return new MdProcessor(exchangeId, output, selected, bookSize, fixedBookSize);
    }

    private static final ThreadLocal<SecurityFeedStatusMessage> SECURITY_FEED_STATUS_MESSAGE_THREAD_LOCAL =
            ThreadLocal.withInitial(() -> new SecurityFeedStatusMessage());

    private final CharSequenceToObjectMap<InstrumentDataProcessors> processors =
            new CharSequenceToObjectMap<>();

    private final MessageOutput output;
    private final MdModel.Options selected;
    private final long source;
    private final int bookSize;
    private final int fixedBookSize;

    private MessageOutput targetOutput; // guarded by this

    private MdProcessor(
            final String exchangeId,
            final MessageOutput output,
            final MdModel.Options selected,
            final int bookSize,
            final int fixedBookSize) {

        this.output = new MessageOutput() {
            @Override
            public void send(final InstrumentMessage message) {
                synchronized (this) {
                    if (targetOutput == null) {
                        return;
                    }
                    targetOutput.send(message);
                }
            }
        };
        this.targetOutput = output;
        this.selected = selected;
        this.source = ExchangeCodec.codeToLong(exchangeId);
        this.bookSize = bookSize;
        this.fixedBookSize = fixedBookSize;
    }

    public QuoteSequenceProcessor onBookSnapshot(final CharSequence instrument) {
        return onBookSnapshot(instrument, TimeConstants.TIMESTAMP_UNKNOWN);
    }

    public QuoteSequenceProcessor onBookSnapshot(final CharSequence instrument, final long timestamp) {
        final QuoteSequenceProcessor l2Processor = getProcessors(instrument).level2;
        l2Processor.packageStarted(true, timestamp);
        return l2Processor;
    }

    public QuoteSequenceProcessor onBookUpdate(final CharSequence instrument) {
        return onBookUpdate(instrument, TimeConstants.TIMESTAMP_UNKNOWN);
    }

    public QuoteSequenceProcessor onBookUpdate(final CharSequence instrument, final long timestamp) {
        final QuoteSequenceProcessor l2Processor = getProcessors(instrument).level2;
        l2Processor.packageStarted(false, timestamp);
        return l2Processor;
    }

    public void onTrade(
            final String instrument,
            final long timestamp,
            final @Decimal long price,
            final @Decimal long size) {
        onTrade(instrument, timestamp, price, size, null);
    }

    public void onTrade(
            final String instrument,
            final long timestamp,
            final @Decimal long price,
            final @Decimal long size,
            final AggressorSide side) {
        getProcessors(instrument).trades.onTrade(timestamp, instrument, price, size, side);
    }

    public void onTrade(
        final String instrument,
        final long timestamp,
        final @Decimal long price,
        final @Decimal long size,
        final AggressorSide side,
        final String exchange,
        final String condition) {
        getProcessors(instrument).trades.onTrade(timestamp, instrument, price, size, side, exchange, condition);
    }

    public void onL1Snapshot(
        final String instrument,
        final long timestamp,
        final @Decimal long bidPrice,
        final @Decimal long bidSize,
        final @Decimal long askPrice,
        final @Decimal long askSize) {

        getProcessors(instrument).level1.onSnapshot(
            instrument, timestamp, bidPrice, bidSize, askPrice, askSize
        );
    }

    public void onL1Snapshot(
        final String instrument,
        final long timestamp,
        final @Decimal long bidPrice,
        final @Decimal long bidSize,
        final String bidExchange,
        final @Decimal long askPrice,
        final @Decimal long askSize,
        final String askExchange) {

        getProcessors(instrument).level1.onSnapshot(
            instrument, timestamp, bidPrice, bidSize, bidExchange, askPrice, askSize, askExchange
        );
    }

    void close(final String reason) {
        MessageOutput out;
        synchronized (this) {
            if (targetOutput == null) {
                return;
            }
            out = targetOutput;
            targetOutput = null;
        }

        final List<String> symbols;
        synchronized (processors) {
            symbols = new ArrayList<>(processors.keySet());
        }

        final SecurityFeedStatusMessage status = SECURITY_FEED_STATUS_MESSAGE_THREAD_LOCAL.get();
        status.setExchangeId(source);
        status.setStatus(FeedStatus.NOT_AVAILABLE);
        status.setCause(reason);

        for (final String symbol : symbols) {
            status.setSymbol(symbol);
            out.send(status);
        }
    }

    private InstrumentDataProcessors getProcessors(final CharSequence instrument) {
        InstrumentDataProcessors result;
        boolean isNew = false;
        synchronized (processors) {
            result = processors.get(instrument);
            if (result == null) {
                result = new InstrumentDataProcessors(instrument.toString());
                processors.put(instrument, result);
                isNew = true;
            }
        }

        if (!isNew) {
           return result;
        }

        final SecurityFeedStatusMessage status = SECURITY_FEED_STATUS_MESSAGE_THREAD_LOCAL.get();
        status.setExchangeId(source);
        status.setStatus(FeedStatus.AVAILABLE);
        status.setCause("Connected to the feed successfully");
        status.setSymbol(instrument);
        output.send(status);

        return result;
    }

    private class InstrumentDataProcessors {
        private final QuoteSequenceProcessor level2;
        private final L1Producer level1;
        private final TradeProducer trades;

        private InstrumentDataProcessors(final String instrument) {
            final ChainedL2Listener.Builder<DefaultItem<DefaultEvent>, DefaultEvent> builder =
                    ChainedL2Listener.builder();

            if (selected.level1()) {
                builder.with(new BestBidOfferProducer<>(output));
            }
            if (selected.level2()) {
                builder.with(new L2Producer<>(output));
            }

            level2 = new QuoteSequenceProcessor(
                    L2Processor.builder()
                            .withInstrument(instrument)
                            .withSource(source)
                            .withBookOutputSize(bookSize)
                            .withFixedBookSize(fixedBookSize)
                            .buildWithPriceBook(
                                    builder.build()
                            )
            );

            level1 = new L1Producer(source, output);
            trades = new TradeProducer(source, output);
        }
    }
}

package com.epam.deltix.data.connectors.commons;

import com.epam.deltix.data.connectors.commons.l2.DefaultEvent;
import com.epam.deltix.data.connectors.commons.l2.DefaultItem;
import com.epam.deltix.data.connectors.commons.l2.L2Processor;
import com.epam.deltix.data.connectors.commons.l2.PriceBook;
import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.qsrv.hf.tickdb.pub.TimeConstants;

public class L2BookProcessorImpl implements L2BookProcessor {

    private final L2Processor<PriceBook<DefaultItem<DefaultEvent>, DefaultEvent>, DefaultItem<DefaultEvent>, DefaultEvent>
        l2Processor;

    private final DefaultEvent priceBookEvent = new DefaultEvent();

    private boolean packageStarted = false;

    public L2BookProcessorImpl(
        L2Processor<PriceBook<DefaultItem<DefaultEvent>, DefaultEvent>, DefaultItem<DefaultEvent>, DefaultEvent> l2Processor) {

        this.l2Processor = l2Processor;
    }

    void packageStarted(boolean snapshot, long timestamp) {
        checkPackageNotStarted();

        if (snapshot) {
            l2Processor.onSnapshotPackageStarted(TimeConstants.TIMESTAMP_UNKNOWN, timestamp);
        } else {
            l2Processor.onIncrementalPackageStarted(timestamp);
        }

        packageStarted = true;
    }

    @Override
    public void onQuote(@Decimal long price, @Decimal long size, boolean isAsk) {
        checkPackageStarted();

        priceBookEvent.reset();
        priceBookEvent.set(isAsk, price, size);
        l2Processor.onEvent(priceBookEvent);
    }

    @Override
    public void onFinish() {
        checkPackageStarted();
        packageStarted = false;

        l2Processor.onPackageFinished();
    }

    private void checkPackageNotStarted() {
        if (packageStarted) {
            throw new IllegalStateException(
                "Previous package is not finished. Please call 'onFinish()' before starting new package"
            );
        }
    }

    private void checkPackageStarted() {
        if (!packageStarted) {
            throw new IllegalStateException(
                "Package is not started. Please use 'onBookSnapshot()' or 'onBookUpdate()' to start new package."
            );
        }
    }
}

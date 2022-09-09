package com.epam.deltix.data.connectors.validator;

import com.epam.deltix.containers.CharSequenceUtils;
import com.epam.deltix.containers.MutableString;
import com.epam.deltix.containers.interfaces.LogProcessor;
import com.epam.deltix.containers.interfaces.Severity;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.timebase.messages.TypeConstants;
import com.epam.deltix.timebase.messages.universal.*;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import org.testcontainers.shaded.org.apache.commons.lang.NotImplementedException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("all")
public class DataValidatorImpl implements DataValidator {
    L1DataValidator l1dataValidator;
    L2DataValidator l2dataValidator;
    private boolean isL1Data, isL2Data, isL3Data;
    private PackageHeader l1header = new PackageHeader();
    private PackageHeader l2header = new PackageHeader();
    private PackageHeader l3header = new PackageHeader();

    private CharSequence symbol;
    private long tickPrice, tickSize;
    private boolean checkNegativePrice;
    private boolean checkEqualLevels = false;
    private boolean checkBidMoreThanAsk = true;
    private boolean checkEmptySide = true;
    private short marketDepth, minValidNumberOfLevels;
    private MutableString logBuilder = new MutableString();
    private LogProcessor logProcessor;
    final private DXTickStream stream;

    private boolean waitingForSnapshot = true;

    private long anchorPrice = Decimal64Utils.NULL;

    public DataValidatorImpl(CharSequence symbol, LogProcessor logProcessor, long tickPrice, long tickSize,
                             boolean checkNegativePrice, DXTickStream stream) {
        this.logProcessor = logProcessor;
        l1dataValidator = new L1DataValidator(symbol, logProcessor, tickPrice, tickSize, checkNegativePrice);
        l2dataValidator = new L2DataValidator(symbol, logProcessor, tickPrice, tickSize, checkNegativePrice, stream);
        if (Decimal64Utils.isNaN(tickPrice)) {
            l1dataValidator.checkTickPrice = false;
            l2dataValidator.checkTickPrice = false;
        }
        if (Decimal64Utils.isNaN(tickSize)) {
            l1dataValidator.checkTickSize = false;
            l2dataValidator.checkTickSize = false;
        }

        l1header.setEntries(new ObjectArrayList<>());
        l2header.setEntries(new ObjectArrayList<>());
        l3header.setEntries(new ObjectArrayList<>());
        this.tickPrice = tickPrice;
        this.tickSize = tickSize;
        this.checkNegativePrice = checkNegativePrice;
        this.symbol = symbol;
        this.stream = stream;
    }

    @Override
    public void sendPackage(PackageHeaderInfo headerInfo) {
        if (waitingForSnapshot && headerInfo.getPackageType() == PackageType.INCREMENTAL_UPDATE) {
            return;
        }

        waitingForSnapshot = false;

        if ((!headerInfo.hasSymbol()) || (!CharSequenceUtils.equals(headerInfo.getSymbol(), symbol))) {
            sendMessageToLogger(headerInfo, "Incorrect symbol", Severity.ERROR);
        }
        if (!headerInfo.hasEntries()) {
            sendMessageToLogger(headerInfo, "Missed entries list", Severity.ERROR);
        }
        if (!headerInfo.hasPackageType()) {
            sendMessageToLogger(headerInfo, "Missed package type", Severity.ERROR);
        }
        if (headerInfo.getEntries().size() == 0) {
            sendMessageToLogger(headerInfo, "Empty entries list", Severity.WARNING);
        }
        clearHeaders(headerInfo, l1header);
        clearHeaders(headerInfo, l2header);
        clearHeaders(headerInfo, l3header);
        for (int i = 0; i < headerInfo.getEntries().size(); ++i) {
            BaseEntryInfo entry = headerInfo.getEntries().get(i);
            if (entry instanceof L1Entry) {
                l1header.getEntries().add(entry);
                isL1Data = true;
            } else
            if (entry instanceof L2EntryNew || entry instanceof L2EntryUpdate) {
                l2header.getEntries().add(entry);
                isL2Data = true;
            } else if (entry instanceof L3EntryNew || entry instanceof L3EntryUpdate) {
                l3header.getEntries().add(entry);
                isL3Data = true;
            } else if (entry instanceof BookResetEntry) {
                BookResetEntry bookResetEntry = (BookResetEntry)entry;
                if (bookResetEntry.getModelType() == DataModelType.LEVEL_ONE) {
                    l1header.getEntries().add(entry);
                    isL1Data = true;
                } else if (bookResetEntry.getModelType() == DataModelType.LEVEL_TWO) {
                    l2header.getEntries().add(entry);
                    isL2Data = true;
                } else {
                    l3header.getEntries().add(entry);
                    isL3Data = true;
                }
            } else if (entry instanceof TradeEntry) {
                if (isL1Data) l1header.getEntries().add(entry);
                if (isL2Data) l2header.getEntries().add(entry);
                if (isL3Data) l3header.getEntries().add(entry);
            }
        }
        if (l1header.getEntries().size() > 0) l1dataValidator.sendPackage(l1header);
        if (l2header.getEntries().size() > 0) l2dataValidator.sendPackage(l2header);
    }

    public void setCheckBidMoreThanAsk(boolean value) {
        this.checkBidMoreThanAsk = value;
        l1dataValidator.checkBidMoreThanAsk = value;
        l2dataValidator.checkBidMoreThanAsk = value;
    }

    public void setCheckEmptySide(boolean value) {
        this.checkEmptySide = value;
        l1dataValidator.checkEmptySide = value;
        l2dataValidator.checkEmptySide = value;
    }

    public void setL2MarketDepth(short marketDepth) {
        this.marketDepth = marketDepth;
        ((L2DataValidator)l2dataValidator).setL2MarketDepth(marketDepth);
    }

    public void setExchangeMisprisingLevel(long misprisingLevel) {
        if (!Decimal64Utils.isNaN(misprisingLevel)) {
            l2dataValidator.exchangeMisPrice = misprisingLevel;
            l2dataValidator.checkExchangeMisPrice = true;
            l1dataValidator.exchangeMisPrice = misprisingLevel;
            l1dataValidator.checkExchangeMisPrice = true;
        } else {
            l2dataValidator.checkExchangeMisPrice = false;
            l1dataValidator.checkExchangeMisPrice = false;
        }
    }

    public void setLevelsMisprisingLevel(long misprisingLevel) {
        if (!Decimal64Utils.isNaN(misprisingLevel)) {
            l2dataValidator.levelMisPrice = misprisingLevel;
            l2dataValidator.checkLevelMisPrice = true;
            l1dataValidator.levelMisPrice = misprisingLevel;
            l1dataValidator.checkLevelMisPrice = true;
        } else {
            l2dataValidator.checkLevelMisPrice = false;
            l1dataValidator.checkLevelMisPrice = false;
        }
    }

    public void setPackageMisprisingLevel(long misprisingLevel) {
        if (!Decimal64Utils.isNaN(misprisingLevel)) {
            l2dataValidator.packageMisPrice = misprisingLevel;
            l2dataValidator.checkPackageMisPrice = true;
            l1dataValidator.packageMisPrice = misprisingLevel;
            l1dataValidator.checkPackageMisPrice = true;
        } else {
            l2dataValidator.checkPackageMisPrice = false;
            l1dataValidator.checkPackageMisPrice = false;
        }
    }

    public void setL2CheckEqualLevels(boolean checkEqualLevels) {
        this.checkEqualLevels = checkEqualLevels;
        ((L2DataValidator)l2dataValidator).setL2CheckEqualLevels(checkEqualLevels);
    }

    public void setL2MinValidNumberOfLevels(short minValidNumberOfLevels) {
        this.minValidNumberOfLevels = minValidNumberOfLevels;
        ((L2DataValidator)l2dataValidator).setL2MinValidNumberOfLevels(minValidNumberOfLevels);
    }

    private void sendMessageToLogger(PackageHeaderInfo header, CharSequence message, Severity severity) {
        if (severity == Severity.INFO) {
            if (minSeverity != Severity.INFO) return;
        } else if (severity == Severity.WARNING) {
            if (minSeverity == Severity.ERROR) return;
        }
        DataValidator.generateLogMessage(logBuilder, symbol, TypeConstants.EXCHANGE_NULL, header.getTimeStampMs(), null,  (short)-1, message);
        if (logProcessor != null) logProcessor.onLogEvent(this, severity, null, logBuilder);
    }

    private void clearHeaders(PackageHeaderInfo headerInfo, PackageHeader destHeader) {
        destHeader.getEntries().clear();
        destHeader.setPackageType(headerInfo.getPackageType());
        destHeader.setTimeStampMs(headerInfo.getTimeStampMs());
        destHeader.setSymbol(headerInfo.getSymbol());
    }

    public long getTotalProcessedEntries() {
        // todo
//        L2DataValidator l2Validator = (L2DataValidator)l2dataValidator;
//        L3DataValidator l3Validator = (L3DataValidator)l3dataValidator;
//        long totalCount =  l2Validator.book.getTotalDeleteCount() + l2Validator.book.getTotalInsertCount() + l2Validator.book.getTotalUpdateCount() + l3Validator.book.getTotalDeleteCount() +
//            l3Validator.book.getTotalUpdateCount() + l3Validator.book.getTotalInsertCount();
//        long failedCount = l2Validator.book.getFailedDeleteCount() + l2Validator.book.getFailedInsertCount() + l2Validator.book.getFailedUpdateCount() + l3Validator.book.getFailedDeleteCount() +
//            l3Validator.book.getFailedUpdateCount() + l3Validator.book.getFailedInsertCount();
//        return totalCount - failedCount;

        throw new NotImplementedException();
    }

    @Override
    public void setLogger(LogProcessor logger) {
        this.logProcessor = logger;
        l1dataValidator.setLogger(logger);
        l2dataValidator.setLogger(logger);
    }

    private Severity minSeverity = Severity.INFO;

    @Override
    public void setMinimalSeverityToLog(Severity minSeverity) {
        this.minSeverity = minSeverity;
        l1dataValidator.setMinimalSeverityToLog(minSeverity);
        l2dataValidator.setMinimalSeverityToLog(minSeverity);
    }

    @Override
    public Severity getMinimalSeverityToLog() {
        return minSeverity;
    }

    public static void main(String[] args) {
        DXTickDB db = TickDBFactory.openFromUrl("dxtick://localhost:8160", true);
        final DXTickStream stream = db.getStream("bitfinex");

        Map<String, DataValidator> validators = new HashMap<>();
        AtomicLong errors = new AtomicLong();

        try (TickCursor cursor = stream.select(TimeConstants.TIMESTAMP_UNKNOWN, new SelectionOptions(false, true))) {
            int messages = 0;
            while (cursor.next()) {
                InstrumentMessage message = cursor.getMessage();
                if (message.getSymbol() == null) {
                    continue;
                }

                // validate
                if (message instanceof PackageHeaderInfo) {
                    PackageHeaderInfo packageHeader = (PackageHeaderInfo) message;
                    DataValidator validator = validators.computeIfAbsent(
                        message.getSymbol().toString(),
                        k -> createValidator(k, (sender, severity, exception, stringMessage) -> {
                                if (severity == Severity.ERROR) {
                                    errors.addAndGet(1);
                                    System.out.println(severity + " | " + stringMessage);
                                } else {
                                    System.out.println(severity + " | " + stringMessage);
                                }

                                if (exception != null) {
                                    exception.printStackTrace();
                                }
                            },
                            stream
                        )
                    );

                    validator.sendPackage(packageHeader);
                }
            }
        }
    }

    private static DataValidator createValidator(String symbol, LogProcessor log, DXTickStream strem) {
        DataValidatorImpl validator = new DataValidatorImpl(symbol, log,
            Decimal64Utils.parse("0.0000000000000000001"), Decimal64Utils.parse("0.0000000000000000001"),
            true, strem
        );
        validator.setCheckEmptySide(true);
        validator.setCheckBidMoreThanAsk(true);

        return validator;
    }

}

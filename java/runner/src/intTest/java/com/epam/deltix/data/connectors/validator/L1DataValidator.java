package com.epam.deltix.data.connectors.validator;

import com.epam.deltix.containers.CharSequenceUtils;
import com.epam.deltix.containers.MutableString;
import com.epam.deltix.containers.generated.*;
import com.epam.deltix.containers.interfaces.LogProcessor;
import com.epam.deltix.containers.interfaces.Severity;
import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.timebase.messages.TypeConstants;
import com.epam.deltix.timebase.messages.universal.*;
import com.epam.deltix.util.collections.generated.LongToObjectHashMap;

import java.io.IOException;

@SuppressWarnings("all")
public class L1DataValidator implements DataValidator {
    private LogProcessor logger;
    private CharSequence symbol;
    private @Decimal
    LongToObjectHashMap<DecimalLongDecimalLongPair> bestAskDictionary = new LongToObjectHashMap<>();
    private @Decimal LongToObjectHashMap<DecimalLongDecimalLongPair> bestBidDictionary = new LongToObjectHashMap<>();
    private MutableString logBuilder = new MutableString(), internalLogBuilder = new MutableString();
    private long tickPrice, tickSize;
    private boolean checkNegativePrice;
    boolean checkExchangeMisPrice = false, checkPackageMisPrice = false, checkLevelMisPrice = false;
    long exchangeMisPrice, packageMisPrice, levelMisPrice;
    boolean checkTickSize = true, checkTickPrice = true;
    private LongArrayList exchangeIds = new LongArrayList();
    private DecimalLongArrayList previousBestAsks = new DecimalLongArrayList();
    private DecimalLongArrayList previousBestBids = new DecimalLongArrayList();
    boolean checkBidMoreThanAsk = true;
    boolean checkEmptySide = true;
    private LogProcessor logProcessor;

    public L1DataValidator(CharSequence symbol, LogProcessor logProcessor, long tickPrice, long tickSize, boolean checkNegativePrice) {
        this.symbol = symbol;
        this.logProcessor = logProcessor;
        this.tickPrice = tickPrice;
        this.tickSize = tickSize;
        this.checkNegativePrice = checkNegativePrice;
    }

    private void validateEntry(PackageHeaderInfo headerInfo, L1Entry entry) {
        if (!entry.hasPrice() || (Decimal64Utils.isNaN(entry.getPrice()))) {
            sendMessageToLogger(headerInfo, entry.getExchangeId(), "Unexpected (or missed) entry price in L1Entry", Severity.ERROR);
        }
        if (entry.hasSize() && (Decimal64Utils.isLessOrEqual(entry.getSize(), Decimal64Utils.ZERO)) || (Decimal64Utils.isNaN(entry.getSize())) || (Decimal64Utils.isInfinity(entry.getSize()))) {
            sendMessageToLogger(headerInfo, entry.getExchangeId(), "Unexpected entry size in L1Entry", Severity.ERROR);
        }
        if (checkNegativePrice && Decimal64Utils.isLess(entry.getPrice(), Decimal64Utils.ZERO)) {
            sendMessageToLogger(headerInfo, entry.getExchangeId(), "L1Entry price is negative", Severity.WARNING);
        }
        if (checkTickPrice && entry.hasPrice() && Decimal64Utils.isNotEqual(Decimal64Utils.round(entry.getPrice(), tickPrice), entry.getPrice())) {
            try {
                sendMessageToLogger(headerInfo, entry.getExchangeId(), (CharSequence)Decimal64Utils.appendTo(entry.getPrice(), internalLogBuilder.clear().append("L1Entry price is not multiple of tickSize. Price: ")), Severity.ERROR);
            } catch (IOException e) {
                sendMessageToLogger(headerInfo, entry.getExchangeId(),"Error in Decimal64.appendTo(MutableString)", Severity.ERROR);
            }
        }
        if (checkTickSize && entry.hasSize() && Decimal64Utils.isNotEqual(Decimal64Utils.round(entry.getSize(), tickSize), entry.getSize())) {
            try {
                sendMessageToLogger(headerInfo, entry.getExchangeId(), (CharSequence)Decimal64Utils.appendTo(entry.getSize(), internalLogBuilder.clear().append("L1Entry size is not multiple of lotSize. Size: ")), Severity.ERROR);
            } catch (IOException e) {
                sendMessageToLogger(headerInfo, entry.getExchangeId(),"Error in Decimal64.appendToMutableString", Severity.ERROR);
            }
        }
    }

    private void validateEntry(PackageHeaderInfo headerInfo, TradeEntry tradeEntry) {
        if (!tradeEntry.hasPrice() || (Decimal64Utils.isNaN(tradeEntry.getPrice())) || (Decimal64Utils.isInfinity(tradeEntry.getPrice()))) {
            sendMessageToLogger(headerInfo, tradeEntry.getExchangeId(), "Unexpected (or missed) entry price in TradeEntry", Severity.ERROR);
        }
        if ((!tradeEntry.hasSize()) || (Decimal64Utils.isLessOrEqual(tradeEntry.getSize(), Decimal64Utils.ZERO)) || (Decimal64Utils.isNaN(tradeEntry.getSize())) || (Decimal64Utils.isInfinity(tradeEntry.getSize()))) {
            sendMessageToLogger(headerInfo, tradeEntry.getExchangeId(), "Unexpected (or missed) entry size in TradeEntry", Severity.ERROR);
        }
        if (!tradeEntry.hasExchangeId()) {
            sendMessageToLogger(headerInfo, tradeEntry.getExchangeId(), "Missed exchangeId in TradeEntry", Severity.ERROR);
        }

        if (checkNegativePrice && Decimal64Utils.isLess(tradeEntry.getPrice(), Decimal64Utils.ZERO)) {
            sendMessageToLogger(headerInfo, tradeEntry.getExchangeId(), "TradeEntry price is negative", Severity.WARNING);
        }
        if (checkTickPrice && Decimal64Utils.isNotEqual(Decimal64Utils.round(tradeEntry.getPrice(), tickPrice), tradeEntry.getPrice())) {
            try {
                sendMessageToLogger(headerInfo, tradeEntry.getExchangeId(), (CharSequence)Decimal64Utils.appendTo(tradeEntry.getPrice(), internalLogBuilder.clear().append("TradeEntry price is not multiple of tickSize. Price: ")), Severity.ERROR);
            } catch (IOException e) {
                sendMessageToLogger(headerInfo, tradeEntry.getExchangeId(), "Error in Decimal64.appendToMutableString", Severity.ERROR);
            }
        }
        if (checkTickSize && Decimal64Utils.isNotEqual(Decimal64Utils.round(tradeEntry.getSize(), tickSize), tradeEntry.getSize())) {
            try {
                sendMessageToLogger(headerInfo, tradeEntry.getExchangeId(), (CharSequence) Decimal64Utils.appendTo(tradeEntry.getSize(), internalLogBuilder.clear().append("TradeEntry size is not multiple of lotSize. Size: ")), Severity.ERROR);
            } catch (IOException e) {
                sendMessageToLogger(headerInfo, tradeEntry.getExchangeId(), "Error in Decimal64.appendToMutableString", Severity.ERROR);
            }
        }
    }


    private void checkPeriodicalSnapshotState(PackageHeaderInfo headerInfo) {
        long exchangeId = headerInfo.getEntries().get(0).getExchangeId();
        for (int i = 0; i < headerInfo.getEntries().size(); ++i) {
            if (headerInfo.getEntries().get(i).getExchangeId() != exchangeId) {
                sendMessageToLogger(headerInfo, TypeConstants.EXCHANGE_NULL, "We support only one exchangeId for snapshots", Severity.ERROR);
            }
        }

        long bestAskSnapshotPrice = Decimal64Utils.POSITIVE_INFINITY;
        long bestBidSnapshotPrice = Decimal64Utils.NEGATIVE_INFINITY;
        long bestAskSnapshotSize = Decimal64Utils.ZERO;
        long bestBidSnapshotSize = Decimal64Utils.ZERO;
        long bestAskBookPrice = Decimal64Utils.POSITIVE_INFINITY;
        long bestBidBookPrice = Decimal64Utils.NEGATIVE_INFINITY;
        long bestAskBookSize = Decimal64Utils.ZERO;
        long bestBidBookSize = Decimal64Utils.ZERO;

        for (int i = 0 ; i < headerInfo.getEntries().size(); ++i) {
            L1Entry l1Entry = (L1Entry)headerInfo.getEntries().get(i);
            validateEntry(headerInfo, l1Entry);
            if (l1Entry.getSide() == QuoteSide.ASK) {
                bestAskSnapshotPrice = l1Entry.getPrice();
                bestAskSnapshotSize = l1Entry.getSize();
            } else {
                bestBidSnapshotPrice = l1Entry.getPrice();
                bestBidSnapshotSize = l1Entry.getSize();
            }
        }
        DecimalLongDecimalLongPair askPair = bestAskDictionary.get(exchangeId, null);
        DecimalLongDecimalLongPair bidPair = bestBidDictionary.get(exchangeId, null);
        if (askPair != null) {
            bestAskBookPrice = askPair  .getFirst();
            bestAskBookSize = askPair.getSecond();
        } else return;;
        if (bidPair != null) {
            bestBidBookPrice = bidPair.getFirst();
            bestBidBookSize = bidPair.getSecond();
        } else return;
        if (Decimal64Utils.isNotEqual(bestAskSnapshotPrice, bestAskBookPrice) || Decimal64Utils.isNotEqual(bestBidSnapshotPrice, bestBidBookPrice) || Decimal64Utils.isNotEqual(bestAskSnapshotSize, bestAskBookSize) || Decimal64Utils.isNotEqual(bestBidSnapshotSize, bestBidBookSize)) {
            sendMessageToLogger(headerInfo, TypeConstants.EXCHANGE_NULL, "Current state of book doesn't equal to periodical snapshot.", Severity.ERROR);
        }
    }

    @SuppressWarnings("ForLoopReplaceableByForEach")
    @Override
    public void sendPackage(PackageHeaderInfo headerInfo) {
        if ((!headerInfo.hasSymbol()) || (!CharSequenceUtils.equals(headerInfo.getSymbol(), symbol))) {
            sendMessageToLogger(headerInfo, TypeConstants.EXCHANGE_NULL, "Incorrect symbol", Severity.ERROR);
        }
        if (!headerInfo.hasEntries()) {
            sendMessageToLogger(headerInfo, TypeConstants.EXCHANGE_NULL, "Missed entries list", Severity.ERROR);
        }
        if (!headerInfo.hasPackageType()) {
            sendMessageToLogger(headerInfo, TypeConstants.EXCHANGE_NULL, "Missed package type", Severity.ERROR);
        }
        if (headerInfo.getEntries().size() == 0) {
            sendMessageToLogger(headerInfo, TypeConstants.EXCHANGE_NULL, "Empty entries list", Severity.WARNING);
        }
        if (headerInfo.getPackageType() == PackageType.VENDOR_SNAPSHOT) {
            long exchangeId = headerInfo.getEntries().get(0).getExchangeId();
            for (int i = 0; i < headerInfo.getEntries().size(); ++i) {
                if (headerInfo.getEntries().get(i).getExchangeId() != exchangeId) {
                    sendMessageToLogger(headerInfo, TypeConstants.EXCHANGE_NULL, "We support only one exchangeId for snapshots", Severity.ERROR);
                }
            }
            DecimalLongDecimalLongPair askPair = bestAskDictionary.get(exchangeId, null);
            DecimalLongDecimalLongPair bidPair = bestBidDictionary.get(exchangeId, null);
            if (askPair != null) {
                askPair.setFirst(Decimal64Utils.POSITIVE_INFINITY);
                bidPair.setFirst(Decimal64Utils.NEGATIVE_INFINITY);
                askPair.setSecond(Decimal64Utils.ZERO);
                bidPair.setSecond(Decimal64Utils.ZERO);
            }
        }
        else if (headerInfo.getPackageType() == PackageType.PERIODICAL_SNAPSHOT) {
            checkPeriodicalSnapshotState(headerInfo);
        }
        exchangeIds.clear();
        previousBestBids.clear();
        previousBestAsks.clear();
        for (int i = 0; i < headerInfo.getEntries().size(); ++i) {
            if (headerInfo.getEntries().get(i) instanceof L1Entry) {
                L1Entry l1Entry = (L1Entry)headerInfo.getEntries().get(i);
                validateEntry(headerInfo, l1Entry);
                if (!l1Entry.hasSize()) continue;
                long exchangeId = l1Entry.getExchangeId();
                if (bestAskDictionary.get(exchangeId, null) == null) {
                    bestAskDictionary.put(exchangeId, new DecimalLongDecimalLongPair(Decimal64Utils.POSITIVE_INFINITY, Decimal64Utils.ZERO));
                    bestBidDictionary.put(exchangeId, new DecimalLongDecimalLongPair(Decimal64Utils.POSITIVE_INFINITY, Decimal64Utils.ZERO));
                }

                DecimalLongDecimalLongPair askPair = bestAskDictionary.get(exchangeId, null);
                DecimalLongDecimalLongPair bidPair = bestBidDictionary.get(exchangeId, null);
                if (!exchangeIds.contains(exchangeId)) {
                    exchangeIds.add(exchangeId);
                    previousBestAsks.add(askPair.getFirst());
                    previousBestBids.add(bidPair.getFirst());
                }
                if ((!l1Entry.hasPrice()) || (!l1Entry.hasSize())) continue;
                if (l1Entry.getSide() == QuoteSide.ASK) {
                    askPair.setFirst(l1Entry.getPrice());
                    askPair.setSecond(l1Entry.getSize());
                } else {
                    bidPair.setFirst(l1Entry.getPrice());
                    bidPair.setSecond(l1Entry.getSize());
                }
            } else if (headerInfo.getEntries().get(i) instanceof TradeEntry) {
                TradeEntry tradeEntry = (TradeEntry)headerInfo.getEntries().get(i);
                validateEntry(headerInfo, tradeEntry);
            }
        }
        long minAsk = Decimal64Utils.POSITIVE_INFINITY;
        long maxAsk = Decimal64Utils.NEGATIVE_INFINITY;
        long minBid = Decimal64Utils.POSITIVE_INFINITY;
        long maxBid = Decimal64Utils.NEGATIVE_INFINITY;

        for (int i = 0; i < exchangeIds.size(); ++i) {
            DecimalLongDecimalLongPair askPair = bestAskDictionary.get(exchangeIds.get(i), null);
            DecimalLongDecimalLongPair bidPair = bestBidDictionary.get(exchangeIds.get(i), null);
            if (!Decimal64Utils.isInfinity(bidPair.getFirst()) && !Decimal64Utils.isInfinity(askPair.getFirst()) && Decimal64Utils.isGreater(bidPair.getFirst(), askPair.getFirst()) && checkBidMoreThanAsk) {
                sendMessageToLogger(headerInfo, TypeConstants.EXCHANGE_NULL, "Bid > Ask", Severity.WARNING);
            }
            if (!Decimal64Utils.isInfinity(askPair.getFirst())) {
                minAsk = Decimal64Utils.min(minAsk, askPair.getFirst());
                maxAsk = Decimal64Utils.max(maxAsk, askPair.getFirst());
            }

            if (!Decimal64Utils.isInfinity(bidPair.getFirst())) {
                minBid = Decimal64Utils.min(minBid, bidPair.getFirst());
                maxBid = Decimal64Utils.max(maxBid, bidPair.getFirst());
            }
            if (checkPackageMisPrice) {
                if (!Decimal64Utils.isInfinity(previousBestAsks.get(i)) && !Decimal64Utils.isZero(previousBestAsks.get(i)) && (!Decimal64Utils.isInfinity(askPair.getFirst()))) {
                    if (Decimal64Utils.isGreater(Decimal64Utils.abs(Decimal64Utils.divide(Decimal64Utils.subtract(previousBestAsks.get(i), askPair.getFirst()), previousBestAsks.get(i))), packageMisPrice)) {
                        sendMessageToLogger(headerInfo, TypeConstants.EXCHANGE_NULL, "Too big price change after this package", Severity.WARNING);
                    }
                }

                if (!Decimal64Utils.isInfinity(previousBestBids.get(i)) && !Decimal64Utils.isZero(previousBestBids.get(i)) && (!Decimal64Utils.isInfinity(bidPair.getFirst()))) {
                    if (Decimal64Utils.isGreater(Decimal64Utils.abs(Decimal64Utils.divide(Decimal64Utils.subtract(previousBestBids.get(i), bidPair.getFirst()), previousBestBids.get(i))), packageMisPrice)) {
                        sendMessageToLogger(headerInfo, TypeConstants.EXCHANGE_NULL, "Too big price change after this package", Severity.WARNING);
                    }
                }
            }
        }
        if (checkExchangeMisPrice) {
            if (!Decimal64Utils.isZero(minAsk) && Decimal64Utils.isGreater(Decimal64Utils.divide(Decimal64Utils.subtract(maxAsk, minAsk), Decimal64Utils.abs(minAsk)), exchangeMisPrice)) {
                sendMessageToLogger(headerInfo, TypeConstants.EXCHANGE_NULL, "Ask Mispricing between exchanges", Severity.WARNING);
            }
            if (!Decimal64Utils.isZero(minBid) && Decimal64Utils.isGreater(Decimal64Utils.divide(Decimal64Utils.subtract(maxBid, minBid), Decimal64Utils.abs(minBid)), exchangeMisPrice)) {
                sendMessageToLogger(headerInfo, TypeConstants.EXCHANGE_NULL, "Bid Mispricing between exchanges", Severity.WARNING);
            }
        }

    }

    private void sendMessageToLogger(PackageHeaderInfo header, long exchangeId, CharSequence message, Severity severity) {
        if (severity == Severity.INFO) {
            if (minSeverity != Severity.INFO) return;
        } else if (severity == Severity.WARNING) {
            if (minSeverity == Severity.ERROR) return;
        }
        DataValidator.generateLogMessage(logBuilder, symbol, exchangeId, header.getTimeStampMs(), null,  (short)-1, message);
        if (logger != null) logger.onLogEvent(this, severity, null, logBuilder);
    }

    @Override
    public void setLogger(LogProcessor logger) {
        this.logger = logger;
    }

    private Severity minSeverity = Severity.INFO;
    /**
     * Set minimal value of severity for sent messages.
     *
     * @param minSeverity Minimal value of severity for sent messages. (Info less than Warning, Warning less than Error).
     */
    @Override
    public void setMinimalSeverityToLog(Severity minSeverity) {
        this.minSeverity = minSeverity;
    }

    public Severity getMinimalSeverityToLog() {
        return minSeverity;
    }


}

package com.epam.deltix.data.connectors.validator;

import com.epam.deltix.containers.MutableString;
import com.epam.deltix.containers.generated.DecimalArrayList;
import com.epam.deltix.containers.generated.LongArrayList;
import com.epam.deltix.containers.interfaces.LogProcessor;
import com.epam.deltix.containers.interfaces.Severity;
import com.epam.deltix.timebase.messages.universal.*;
import com.epam.deltix.util.collections.CharSequenceToObjectMap;
import com.epam.deltix.util.collections.generated.ObjectArrayList;

@SuppressWarnings("all")
public class L3DataValidator implements DataValidator {

    private MutableString logBuilder = new MutableString(), internalLogBuilder = new MutableString();
    private CharSequence symbol;
//    FullOrderBook book;
    private PackageHeader incrementHeader = new PackageHeader();
    private long tickPrice, tickSize;
    private boolean checkNegativePrice;
    boolean checkExchangeMisPrice = false, checkPackageMisPrice = false, checkLevelMisPrice = false;
    long exchangeMisPrice, packageMisPrice, levelMisPrice;
    boolean checkTickSize = true, checkTickPrice = true;
    private CharSequenceToObjectMap<L3EntryNew> askPeriodicalSnapshot = new CharSequenceToObjectMap<>();
    private CharSequenceToObjectMap<L3EntryNew> bidPeriodicalSnapshot = new CharSequenceToObjectMap<>();
    private DecimalArrayList previousBestAsks = new DecimalArrayList();
    private DecimalArrayList previousBestBids = new DecimalArrayList();
    private LogProcessor logger;

    private LongArrayList exchangeIds = new LongArrayList();
    boolean checkBidMoreThanAsk = true;
    boolean checkEmptySide = true;


    @SuppressWarnings({"unused", "WeakerAccess"})
    public L3DataValidator(CharSequence symbol, LogProcessor logProcessor, long tickPrice, long tickSize, boolean checkNegativePrice) {
        this.symbol = symbol;
        this.logger = logProcessor;
//        book = new FullOrderBook(symbol);
        incrementHeader.setEntries(new ObjectArrayList<>());
        incrementHeader.setPackageType(PackageType.INCREMENTAL_UPDATE);
        incrementHeader.setSymbol(symbol);
        this.tickPrice = tickPrice;
        this.tickSize = tickSize;
        this.checkNegativePrice = checkNegativePrice;
    }

    @Override
    public void sendPackage(PackageHeaderInfo headerInfo) {

    }

    @Override
    public void setLogger(LogProcessor logger) {
        this.logger = logger;
    }

//    private void sendMessageToLogger(PackageHeaderInfo header, long exchangeId, CharSequence message, Severity severity) {
//        if (severity == Severity.INFO) {
//            if (minSeverity != Severity.INFO) return;
//        } else if (severity == Severity.WARNING) {
//            if (minSeverity == Severity.ERROR) return;
//        }
//        LogMessageGenerator.generateLogMessage(logBuilder, symbol, exchangeId, header.getTimeStampMs(), null,  (short)-1, message);
//        if (logger != null) logger.onLogEvent(this, severity, null, logBuilder);
//    }
//
//    /**
//     * Send next package from stream to validate.
//     * @param headerInfo Package.
//     */
//    @SuppressWarnings("ForLoopReplaceableByForEach")
//    public void sendPackage(PackageHeaderInfo headerInfo) {
//        if ((!headerInfo.hasSymbol()) || (!CharSequenceUtils.equals(headerInfo.getSymbol(), symbol))) {
//            sendMessageToLogger(headerInfo, TypeConstants.EXCHANGE_NULL, "Incorrect symbol", Severity.ERROR);
//        }
//        if (!headerInfo.hasEntries()) {
//            sendMessageToLogger(headerInfo, TypeConstants.EXCHANGE_NULL, "Missed entries list", Severity.ERROR);
//        }
//        if (!headerInfo.hasPackageType()) {
//            sendMessageToLogger(headerInfo, TypeConstants.EXCHANGE_NULL, "Missed package type", Severity.ERROR);
//        }
//        if (headerInfo.getEntries().size() == 0) {
//            sendMessageToLogger(headerInfo, TypeConstants.EXCHANGE_NULL, "Empty entries list", Severity.WARNING);
//            return;
//        }
//        checkAllEntriesFields(headerInfo);
//        exchangeIds.clear();
//        previousBestAsks.clear();
//        previousBestBids.clear();
//
//        if (headerInfo.getPackageType() != PackageType.INCREMENTAL_UPDATE) {
//            processSnapshot(headerInfo);
//            long exchangeId = headerInfo.getEntries().get(0).getExchangeId();
//
//            exchangeIds.add(exchangeId);
//            OrderBookQuote askQuote = book.getExchange(exchangeId).getBestAskQuote();
//            OrderBookQuote bidQuote = book.getExchange(exchangeId).getBestBidQuote();
//            if (askQuote != null) previousBestAsks.add(askQuote.getPrice()); else previousBestAsks.add(Decimal64Utils.NEGATIVE_INFINITY);
//            if (bidQuote != null) previousBestBids.add(bidQuote.getPrice()); else previousBestBids.add(Decimal64Utils.POSITIVE_INFINITY);
//
//            book.update(headerInfo);
//        } else {
//            for (int i = 0; i < headerInfo.getEntries().size(); ++i) {
//                incrementHeader.getEntries().add(headerInfo.getEntries().get(i));
//
//                if (!exchangeIds.contains(incrementHeader.getEntries().get(0).getExchangeId())) {
//                    long exchangeId = incrementHeader.getEntries().get(0).getExchangeId();
//                    exchangeIds.add(exchangeId);
//                    OrderBookQuote askQuote = book.getExchange(exchangeId).getBestAskQuote();
//                    OrderBookQuote bidQuote = book.getExchange(exchangeId).getBestBidQuote();
//                    if (askQuote != null) previousBestAsks.add(askQuote.getPrice()); else previousBestAsks.add(Decimal64Utils.NEGATIVE_INFINITY);
//                    if (bidQuote != null) previousBestBids.add(bidQuote.getPrice()); else previousBestBids.add(Decimal64Utils.POSITIVE_INFINITY);
//                }
//                if (incrementHeader.getEntries().get(0) instanceof L3EntryUpdate) {
//                    L3EntryUpdate entryUpdate = (L3EntryUpdate)incrementHeader.getEntries().get(0);
//                    validateIncrementEntry(headerInfo, entryUpdate);
//                } else if (incrementHeader.getEntries().get(0) instanceof L3EntryNew) {
//                    L3EntryNew entryNew = (L3EntryNew)incrementHeader.getEntries().get(0);
//                    validateIncrementEntry(headerInfo, entryNew);
//                } else if (incrementHeader.getEntries().get(0) instanceof TradeEntry) {
//                    TradeEntry tradeEntry = (TradeEntry)incrementHeader.getEntries().get(0);
//                    validateIncrementEntry(headerInfo, tradeEntry);
//                }
//                book.update(incrementHeader);
//                incrementHeader.getEntries().clear();
//            }
//        }
//
//        if (checkExchangeMisPrice) {
//            long minAsk = Decimal64Utils.POSITIVE_INFINITY;
//            long maxAsk = Decimal64Utils.NEGATIVE_INFINITY;
//            long minBid = Decimal64Utils.POSITIVE_INFINITY;
//            long maxBid = Decimal64Utils.NEGATIVE_INFINITY;
//            for (int i = 0; i < this.book.size(); ++i) {
//                ExchangeOrderBook book = (ExchangeOrderBook) this.book.get(i);
//                if (book.getAllAskQuotes().size() > 0) {
//                    minAsk = Decimal64Utils.min(book.getBestAskQuote().getPrice(), minAsk);
//                    maxAsk = Decimal64Utils.max(book.getBestAskQuote().getPrice(), maxAsk);
//                }
//                if (book.getAllBidQuotes().size() > 0) {
//                    minBid = Decimal64Utils.min(book.getBestBidQuote().getPrice(), minBid);
//                    maxBid = Decimal64Utils.max(book.getBestBidQuote().getPrice(), maxBid);
//                }
//            }
//            if (!Decimal64Utils.isZero(minAsk) && Decimal64Utils.isGreater(Decimal64Utils.divide(Decimal64Utils.subtract(maxAsk, minAsk), Decimal64Utils.abs(minAsk)), exchangeMisPrice)) {
//                sendMessageToLogger(headerInfo, TypeConstants.EXCHANGE_NULL, internalLogBuilder.clear().append("Ask Mispricing between exchanges"), Severity.WARNING);
//            }
//            if (!Decimal64Utils.isZero(minBid) && Decimal64Utils.isGreater(Decimal64Utils.divide(Decimal64Utils.subtract(maxBid, minBid), Decimal64Utils.abs(minBid)), exchangeMisPrice)) {
//                sendMessageToLogger(headerInfo, TypeConstants.EXCHANGE_NULL, internalLogBuilder.clear().append("Bid Mispricing between exchanges"), Severity.WARNING);
//            }
//        }
//        if (checkLevelMisPrice) {
//            for (int i = 0; i < this.book.size(); ++i) {
//                ExchangeOrderBook book = (ExchangeOrderBook)this.book.get(i);
//                for (int level = 1; level < book.getAllAskQuotes().size() ;level++) {
//                    long previousPrice = book.getAllQuotes(QuoteSide.ASK).getObjectAt(level - 1).getPrice();
//                    long currentPrice = book.getAllQuotes(QuoteSide.ASK).getObjectAt(level).getPrice();
//                    if (Decimal64Utils.isZero(currentPrice)) continue;
//                    long r = Decimal64Utils.divide(previousPrice, currentPrice);
//
//                    if (Decimal64Utils.isLess(r, Decimal64Utils.subtract(Decimal64Utils.ONE, levelMisPrice))) {
//                        sendMessageToLogger(headerInfo, book.getExchangeId(), internalLogBuilder.clear().append("Ask Mispricing in the exchange ").append(AlphanumericUtils.toString(book.getExchangeId())).append(" at level ").append(level), Severity.WARNING);
//                    }
//                }
//                for (int level = 1; level < book.getAllBidQuotes().size() ;level++) {
//                    long previousPrice = book.getAllQuotes(QuoteSide.BID).getObjectAt(level - 1).getPrice();
//                    long currentPrice = book.getAllQuotes(QuoteSide.BID).getObjectAt(level).getPrice();
//                    if (Decimal64Utils.isZero(previousPrice)) continue;;
//                    long r = Decimal64Utils.divide(currentPrice, previousPrice);
//                    if (Decimal64Utils.isLess(r, Decimal64Utils.subtract(Decimal64Utils.ONE, levelMisPrice))) {
//                        sendMessageToLogger(headerInfo, book.getExchangeId(), internalLogBuilder.clear().append("Bid Mispricing in the exchange ").append(AlphanumericUtils.toString(book.getExchangeId())).append(" at level ").append(level), Severity.WARNING);
//                    }
//                }
//            }
//        }
//        for (int i = 0; i < exchangeIds.size(); ++i) {
//            ExchangeOrderBook book = this.book.getExchange(exchangeIds.get(i));
//            OrderBookQuote bestAskQuote = book.getBestAskQuote();
//            OrderBookQuote bestBidQuote = book.getBestBidQuote();
//            if (bestAskQuote != null && bestBidQuote != null) {
//                if (Decimal64Utils.isLess(bestAskQuote.getPrice(), bestBidQuote.getPrice()) && checkBidMoreThanAsk) {
//                    sendMessageToLogger(headerInfo, book.getExchangeId(), "Bid > Ask", Severity.WARNING);
//                }
//                if (checkPackageMisPrice) {
//                    if (!Decimal64Utils.isInfinity(previousBestAsks.get(i)) && !Decimal64Utils.isZero(previousBestAsks.get(i))) {
//                        if (Decimal64Utils.isGreater(Decimal64Utils.abs(Decimal64Utils.divide(Decimal64Utils.subtract(previousBestAsks.get(i), bestAskQuote.getPrice()), previousBestAsks.get(i))), packageMisPrice)) {
//                            sendMessageToLogger(headerInfo, book.getExchangeId(), "Too big ask price change after this package", Severity.WARNING);
//                        }
//                    }
//                    if (!Decimal64Utils.isInfinity(previousBestBids.get(i)) && !Decimal64Utils.isZero(previousBestBids.get(i))) {
//                        if (Decimal64Utils.isGreater(Decimal64Utils.abs(Decimal64Utils.divide(Decimal64Utils.subtract(previousBestBids.get(i), bestBidQuote.getPrice()), previousBestBids.get(i))), packageMisPrice)) {
//                            sendMessageToLogger(headerInfo, book.getExchangeId(), "Too big bid price change after this package", Severity.WARNING);
//                        }
//                    }
//                }
//            }
//            if (bestAskQuote == null && (!book.isWaitingForSnapshot()) && checkEmptySide) {
//                sendMessageToLogger(headerInfo, book.getExchangeId(), "Ask side is empty", Severity.WARNING);
//            }
//            if (bestBidQuote == null && (!book.isWaitingForSnapshot()) && checkEmptySide) {
//                sendMessageToLogger(headerInfo, book.getExchangeId(), "Bid side is empty", Severity.WARNING);
//            }
//        }
//    }
//
//    private void validateIncrementEntry(PackageHeaderInfo headerInfo, L3EntryUpdate entryUpdate) {
//        if (!entryUpdate.hasQuoteId()) {
//            sendMessageToLogger(headerInfo, entryUpdate.getExchangeId(), "Missed QuoteId in L3EntryUpdate", Severity.ERROR);
//        }
//        if (!entryUpdate.hasExchangeId()) {
//            sendMessageToLogger(headerInfo, entryUpdate.getExchangeId(), "Missed ExchangeId in L3EntryUpdate", Severity.ERROR);
//        }
//        ExchangeOrderBook exchange = book.getExchange(entryUpdate.getExchangeId());
//        if (exchange.isWaitingForSnapshot()) return;
//        OrderBookQuote quote = exchange.getQuoteById(entryUpdate.getQuoteId());
//        if (quote == null) {
//            sendMessageToLogger(headerInfo, entryUpdate.getExchangeId(), "L3Update entry to non-existing id", Severity.ERROR);
//            return;
//        }
//        if (entryUpdate.getAction() == QuoteUpdateAction.MODIFY && entryUpdate.hasSide() && entryUpdate.getSide() != quote.getQuoteSide()) {
//            sendMessageToLogger(headerInfo, entryUpdate.getExchangeId(), "Try to update QuoteSide in Modify Event. It's incorrect", Severity.ERROR);
//        }
//        if (entryUpdate.getAction() == QuoteUpdateAction.MODIFY && entryUpdate.hasPrice() && Decimal64Utils.isNotEqual(entryUpdate.getPrice(), quote.getPrice())) {
//            sendMessageToLogger(headerInfo, entryUpdate.getExchangeId(), "Try to update price of quote in L3EntryUpdate with Action=Modify", Severity.ERROR);
//        }
//        if (!entryUpdate.hasSize() || (Decimal64Utils.isLessOrEqual(entryUpdate.getSize(), Decimal64Utils.ZERO)) || Decimal64Utils.isNaN(entryUpdate.getSize()) || Decimal64Utils.isInfinity(entryUpdate.getSize())) {
//            sendMessageToLogger(headerInfo, entryUpdate.getExchangeId(), "Unexpected (or missed) size in L3EntryUpdate", Severity.ERROR);
//        }
//        if (entryUpdate.hasPrice() && (Decimal64Utils.isInfinity(entryUpdate.getPrice()) || Decimal64Utils.isNaN(entryUpdate.getPrice()))) {
//            sendMessageToLogger(headerInfo, entryUpdate.getExchangeId(), "Unexpected price in L3EntryUpdate", Severity.ERROR);
//        }
//
//        if (checkNegativePrice && Decimal64Utils.isLess(entryUpdate.getPrice(), Decimal64Utils.ZERO)) {
//            sendMessageToLogger(headerInfo, entryUpdate.getExchangeId(), "L3EntryUpdate price is negative", Severity.WARNING);
//        }
//        if (checkTickPrice &&Decimal64Utils.isNotEqual(Decimal64Utils.round(entryUpdate.getPrice(), tickPrice), entryUpdate.getPrice())) {
//            try {
//                sendMessageToLogger(headerInfo, entryUpdate.getExchangeId(), (CharSequence)Decimal64Utils.appendTo(entryUpdate.getPrice(), internalLogBuilder.clear().append("L3EntryUpdate price is not multiple of tickSize. Price: ")), Severity.ERROR);
//            } catch (IOException e) {
//                sendMessageToLogger(headerInfo, entryUpdate.getExchangeId(), "Error in Decimal64.appendToMutableString", Severity.ERROR);
//            }
//        }
//        if (checkTickSize && Decimal64Utils.isNotEqual(Decimal64Utils.round(entryUpdate.getSize(), tickSize), entryUpdate.getSize())) {
//            try {
//                sendMessageToLogger(headerInfo, entryUpdate.getExchangeId(), (CharSequence)Decimal64Utils.appendTo(entryUpdate.getSize(), internalLogBuilder.clear().append("L3EntryUpdate size is not multiple of lotSize. Size: ")), Severity.ERROR);
//            } catch (IOException e) {
//                sendMessageToLogger(headerInfo, entryUpdate.getExchangeId(), "Error in Decimal64.appendToMutableString", Severity.ERROR);
//            }
//        }
//
//    }
//
//    private void validateIncrementEntry(PackageHeaderInfo headerInfo, TradeEntry tradeEntry) {
//        OrderBookQuote quote;
//        if (!tradeEntry.hasExchangeId()) {
//            sendMessageToLogger(headerInfo, tradeEntry.getExchangeId(), "Missed ExchangeId in TradeEntry", Severity.ERROR);
//        }
//
//        ExchangeOrderBook exchange = book.getExchange(tradeEntry.getExchangeId());
//        if (exchange.isWaitingForSnapshot()) return;
//
//
//
//        if (tradeEntry.getSide() == AggressorSide.BUY) {
//            if (!tradeEntry.hasSellerOrderId()) {
//                sendMessageToLogger(headerInfo, tradeEntry.getExchangeId(), "Missed SellerOrderId in TradeEntry", Severity.ERROR);
//            }
//            quote = exchange.getQuoteById(tradeEntry.getSellerOrderId());
//        } else {
//            if (!tradeEntry.hasBuyerOrderId()) {
//                sendMessageToLogger(headerInfo, tradeEntry.getExchangeId(), "Missed BuyerOrderId in TradeEntry", Severity.ERROR);
//            }
//            quote = exchange.getQuoteById(tradeEntry.getBuyerOrderId());
//        }
//        if (quote == null) {
//            sendMessageToLogger(headerInfo, tradeEntry.getExchangeId(), "TradeEntry to non-existing id", Severity.ERROR);
//            return;
//        }
//        if ((quote.getQuoteSide() != QuoteSide.ASK && tradeEntry.getSide() == AggressorSide.BUY) || (quote.getQuoteSide() != QuoteSide.BID && tradeEntry.getSide() == AggressorSide.SELL) ) {
//            sendMessageToLogger(headerInfo, tradeEntry.getExchangeId(), "TradeEntry.getSide() does'nt match with quote side. It's incorrect", Severity.ERROR);
//        }
//        if (!tradeEntry.hasSize() || Decimal64Utils.isLessOrEqual(tradeEntry.getSize(), Decimal64Utils.ZERO) || Decimal64Utils.isNaN(tradeEntry.getSize()) || Decimal64Utils.isInfinity(tradeEntry.getSize())) {
//            sendMessageToLogger(headerInfo, tradeEntry.getExchangeId(), "Unexpected (or missed) size in TradeEntry", Severity.ERROR);
//        }
//        if (!tradeEntry.hasPrice() ||  Decimal64Utils.isNaN(tradeEntry.getPrice()) || Decimal64Utils.isInfinity(tradeEntry.getPrice())) {
//            sendMessageToLogger(headerInfo, tradeEntry.getExchangeId(), "Unexpected (or missed) price in TradeEntry", Severity.ERROR);
//        }
//
//        if (checkNegativePrice && Decimal64Utils.isLess(tradeEntry.getPrice(), Decimal64Utils.ZERO)) {
//            sendMessageToLogger(headerInfo, tradeEntry.getExchangeId(), "TradeEntry price is negative", Severity.WARNING);
//        }
//        if (checkTickPrice && Decimal64Utils.isNotEqual(Decimal64Utils.round(tradeEntry.getPrice(), tickPrice), tradeEntry.getPrice())) {
//            try {
//                sendMessageToLogger(headerInfo, tradeEntry.getExchangeId(), (CharSequence)Decimal64Utils.appendTo(tradeEntry.getPrice(), internalLogBuilder.clear().append("TradeEntry price is not multiple of tickSize. Price: ")), Severity.ERROR);
//            } catch (IOException e) {
//                sendMessageToLogger(headerInfo, tradeEntry.getExchangeId(), "Error in Decimal64.appendToMutableString", Severity.ERROR);
//            }
//        }
//        if (checkTickSize && Decimal64Utils.isNotEqual(Decimal64Utils.round(tradeEntry.getSize(), tickSize), tradeEntry.getSize())) {
//            try {
//                sendMessageToLogger(headerInfo, tradeEntry.getExchangeId(), (CharSequence)Decimal64Utils.appendTo(tradeEntry.getSize(), internalLogBuilder.clear().append("TradeEntry size is not multiple of lotSize. Size: ")), Severity.ERROR);
//            } catch (IOException e) {
//                sendMessageToLogger(headerInfo, tradeEntry.getExchangeId(), "Error in Decimal64.appendToMutableString", Severity.ERROR);
//            }
//        }
//
//    }
//
//    private void validateIncrementEntry(PackageHeaderInfo headerInfo, L3EntryNew entryNew) {
//        if (!entryNew.hasQuoteId()) {
//            sendMessageToLogger(headerInfo, entryNew.getExchangeId(), "Missed QuoteId in L3EntryNew", Severity.ERROR);
//        }
//        if (!entryNew.hasExchangeId()) {
//            sendMessageToLogger(headerInfo, entryNew.getExchangeId(), "Missed ExchangeId in L3EntryNew", Severity.ERROR);
//        }
//
//        ExchangeOrderBook exchange = book.getExchange(entryNew.getExchangeId());
//        if (exchange.isWaitingForSnapshot()) return;
//
//        OrderBookQuote quote = exchange.getQuoteById(entryNew.getQuoteId());
//        if (quote != null) {
//            sendMessageToLogger(headerInfo, entryNew.getExchangeId(), "L3New entry to existing id", Severity.ERROR);
//        }
//        if (entryNew.getInsertType() == InsertType.ADD_BEFORE) {
//            OrderBookQuote quoteBefore = exchange.getQuoteById(entryNew.getInsertBeforeQuoteId());
//            if (quoteBefore == null || Decimal64Utils.isNotEqual(quoteBefore.getPrice(), entryNew.getPrice())) {
//                sendMessageToLogger(headerInfo, entryNew.getExchangeId(), "L3New entry with InsertType = AddBefore and non-existing InsertBeforeQuoteId in level", Severity.ERROR);
//            }
//        }
//
//        if (!entryNew.hasSize() || Decimal64Utils.isLessOrEqual(entryNew.getSize(), Decimal64Utils.ZERO) || Decimal64Utils.isNaN(entryNew.getSize()) || Decimal64Utils.isInfinity(entryNew.getSize())) {
//            sendMessageToLogger(headerInfo, entryNew.getExchangeId(), "Unexpected (or missed) size in L3EntryNew", Severity.ERROR);
//        }
//        if (!entryNew.hasPrice() ||  Decimal64Utils.isNaN(entryNew.getPrice()) || Decimal64Utils.isInfinity(entryNew.getPrice())) {
//            sendMessageToLogger(headerInfo, entryNew.getExchangeId(), "Unexpected (or missed) price in L3EntryNew", Severity.ERROR);
//        }
//
//        if (checkNegativePrice && Decimal64Utils.isLess(entryNew.getPrice(), Decimal64Utils.ZERO)) {
//            sendMessageToLogger(headerInfo, entryNew.getExchangeId(), "L3EntryNew price is negative", Severity.WARNING);
//        }
//        if (checkTickPrice && Decimal64Utils.isNotEqual(Decimal64Utils.round(entryNew.getPrice(), tickPrice), entryNew.getPrice())) {
//            try {
//                sendMessageToLogger(headerInfo, entryNew.getExchangeId(), (CharSequence)Decimal64Utils.appendTo(entryNew.getPrice(), internalLogBuilder.clear().append("L3EntryNew price is not multiple of tickSize. Price: ")), Severity.ERROR);
//            } catch (IOException e) {
//                sendMessageToLogger(headerInfo, entryNew.getExchangeId(), "Error in Decimal64.appendToMutableString", Severity.ERROR);
//            }
//        }
//        if (checkTickSize && Decimal64Utils.isNotEqual(Decimal64Utils.round(entryNew.getSize(), tickSize), entryNew.getSize())) {
//            try {
//                sendMessageToLogger(headerInfo, entryNew.getExchangeId(), (CharSequence)Decimal64Utils.appendTo(entryNew.getSize(), internalLogBuilder.clear().append("L3EntryNew size is not multiple of lotSize. Size: ")), Severity.ERROR);
//            } catch (IOException e) {
//                sendMessageToLogger(headerInfo, entryNew.getExchangeId(), "Error in Decimal64.appendToMutableString", Severity.ERROR);
//            }
//        }
//
//    }
//
//
//    private void checkPeriodicalSide(PackageHeaderInfo headerInfo, OrderBookList<OrderBookQuote> quotes, CharSequenceToObjectMap<L3EntryNew> periodicalSnapshot) {
//        for (int i = 0; i < quotes.size(); ++i) {
//            L3EntryNew entryNew = periodicalSnapshot.get(quotes.getObjectAt(i).getQuoteId());
//            if (entryNew == null) {
//                sendMessageToLogger(headerInfo, TypeConstants.EXCHANGE_NULL, "Current state of book doesn't equal to periodical snapshot.", Severity.ERROR);
//                return;
//            } else {
//                if (Decimal64Utils.isNotEqual(entryNew.getPrice(), quotes.getObjectAt(i).getPrice()) || Decimal64Utils.isNotEqual(entryNew.getSize(), quotes.getObjectAt(i).getSize())) {
//                    sendMessageToLogger(headerInfo, TypeConstants.EXCHANGE_NULL, "Current state of book doesn't equal to periodical snapshot.", Severity.ERROR);
//                    return;
//                }
//            }
//        }
//    }
//
//
//
//    private void processSnapshot(PackageHeaderInfo headerInfo) {
//        for (int i = 0; i < headerInfo.getEntries().size(); ++i) {
//            if (headerInfo.getEntries().get(i).getExchangeId() != headerInfo.getEntries().get(0).getExchangeId()) {
//                sendMessageToLogger(headerInfo, TypeConstants.EXCHANGE_NULL, "We support only one exchangeId for snapshots", Severity.ERROR);
//            }
//        }
//        for (int i = 0; i < headerInfo.getEntries().size(); ++i) {
//            if (headerInfo.getEntries().get(i) instanceof BookResetEntry) continue;
//            if (!(headerInfo.getEntries().get(i) instanceof L3EntryNew)) {
//                sendMessageToLogger(headerInfo, TypeConstants.EXCHANGE_NULL, "We support only L3EntryNew and BookResetEntry entries for level2 snapshots", Severity.ERROR);
//            }
//        }
//
//        if (headerInfo.getPackageType() == PackageType.PERIODICAL_SNAPSHOT && (!book.getExchange(headerInfo.getEntries().get(0).getExchangeId()).isWaitingForSnapshot())) {
//            askPeriodicalSnapshot.clear();
//            bidPeriodicalSnapshot.clear();
//            for (int i = 0; i < headerInfo.getEntries().size(); ++i) {
//                L3EntryNew entryNew = (L3EntryNew)headerInfo.getEntries().get(i);
//                if (entryNew.getSide() == QuoteSide.ASK) {
//                    askPeriodicalSnapshot.put(entryNew.getQuoteId(), entryNew);
//                } else {
//                    bidPeriodicalSnapshot.put(entryNew.getQuoteId(), entryNew);
//                }
//            }
//
//            OrderBookList<OrderBookQuote> askQuotes = book.getExchange(headerInfo.getEntries().get(0).getExchangeId()).getAllAskQuotes();
//            OrderBookList<OrderBookQuote> bidQuotes = book.getExchange(headerInfo.getEntries().get(0).getExchangeId()).getAllBidQuotes();
//            int askCount = askQuotes.size();
//            int bidCount = bidQuotes.size();
//
//            if (askCount != askPeriodicalSnapshot.size()) {
//                sendMessageToLogger(headerInfo, TypeConstants.EXCHANGE_NULL, "Current state of book doesn't equal to periodical snapshot.", Severity.ERROR);
//            }
//            if (bidCount != bidPeriodicalSnapshot.size()) {
//                sendMessageToLogger(headerInfo, TypeConstants.EXCHANGE_NULL, "Current state of book doesn't equal to periodical snapshot.", Severity.ERROR);
//            }
//
//            checkPeriodicalSide(headerInfo, askQuotes, askPeriodicalSnapshot);
//            checkPeriodicalSide(headerInfo, bidQuotes, bidPeriodicalSnapshot);
//        }
//    }
//
//    private void checkAllEntriesFields(PackageHeaderInfo headerInfo) {
//        for (int i = 0; i < headerInfo.getEntries().size(); ++i) {
//            if (headerInfo.getEntries().get(i) instanceof L3EntryNew) {
//                L3EntryNew entryNew = (L3EntryNew)headerInfo.getEntries().get(i);
//                validationCheck(headerInfo, entryNew);
//            } else if (headerInfo.getEntries().get(i) instanceof L3EntryUpdate) {
//                L3EntryUpdate entryUpdate = (L3EntryUpdate)headerInfo.getEntries().get(i);
//                validationCheck(headerInfo, entryUpdate);
//            } else if (headerInfo.getEntries().get(i) instanceof TradeEntry) {
//                TradeEntry tradeEntry = (TradeEntry)headerInfo.getEntries().get(i);
//                validationCheck(headerInfo, tradeEntry);
//            } else if (headerInfo.getEntries().get(i) instanceof BookResetEntry) {
//                BookResetEntry bookResetEntry = (BookResetEntry) headerInfo.getEntries().get(i);
//                validationCheck(headerInfo, bookResetEntry);
//            } else {
//                sendMessageToLogger(headerInfo, headerInfo.getEntries().get(i).getExchangeId(),"Unexpected entry type for level3 messages", Severity.ERROR);
//            }
//        }
//    }
//
//
//    private void validationCheck(PackageHeaderInfo headerInfo, L3EntryUpdate entryUpdate) {
//
//        if (entryUpdate.getAction() != QuoteUpdateAction.CANCEL) {
//            if ((!entryUpdate.hasPrice()) && (!entryUpdate.hasSize())) {
//                sendMessageToLogger(headerInfo, entryUpdate.getExchangeId(), "Missed price and size in L3EntryUpdate", Severity.ERROR);
//            }
//        }
//
//        if (!entryUpdate.hasExchangeId()) {
//            sendMessageToLogger(headerInfo, entryUpdate.getExchangeId(), "Missed exchangeId in L3EntryUpdate", Severity.ERROR);
//        }
//        if (!entryUpdate.hasQuoteId()) {
//            sendMessageToLogger(headerInfo, entryUpdate.getExchangeId(), "Missed quoteId in L3EntryUpdate", Severity.ERROR);
//        }
//    }
//
//    private void validationCheck(PackageHeaderInfo headerInfo, L3EntryNew entryNew) {
//        if (!entryNew.hasPrice()) {
//            sendMessageToLogger(headerInfo, entryNew.getExchangeId(), "Missed entry price in L3EntryNew", Severity.ERROR);
//        }
//        if ((!entryNew.hasSize()) || Decimal64Utils.isLessOrEqual(entryNew.getSize(), Decimal64Utils.ZERO)) {
//            sendMessageToLogger(headerInfo, entryNew.getExchangeId(), "Missed/Unexpected entry size in L3EntryNew", Severity.ERROR);
//        }
//        if (!entryNew.hasSide()) {
//            sendMessageToLogger(headerInfo, entryNew.getExchangeId(), "Missed entry side in L3EntryNew", Severity.ERROR);
//        }
//        if (!entryNew.hasExchangeId()) {
//            sendMessageToLogger(headerInfo, entryNew.getExchangeId(), "Missed exchangeId in L3EntryNew", Severity.ERROR);
//        }
//        if (!entryNew.hasQuoteId()) {
//            sendMessageToLogger(headerInfo, entryNew.getExchangeId(), "Missed quoteId in L3EntryNew", Severity.ERROR);
//        }
//        if (!entryNew.hasInsertType()) {
//            sendMessageToLogger(headerInfo, entryNew.getExchangeId(), "Missed insertType in L3EntryNew", Severity.ERROR);
//        }
//        if (entryNew.getInsertType() == InsertType.ADD_BEFORE) {
//            if (!entryNew.hasInsertBeforeQuoteId()) {
//                sendMessageToLogger(headerInfo, entryNew.getExchangeId(), "Missed InsertBeforeQuoteId in L3EntryNew", Severity.ERROR);
//            }
//        }
//    }
//
//    private void validationCheck(PackageHeaderInfo headerInfo, TradeEntry tradeEntry) {
//        if (!tradeEntry.hasPrice()) {
//            sendMessageToLogger(headerInfo, tradeEntry.getExchangeId(), "Missed entry price in TradeEntry", Severity.ERROR);
//        }
//        if ((!tradeEntry.hasSize()) || (Decimal64Utils.isLessOrEqual(tradeEntry.getSize(), Decimal64Utils.ZERO))) {
//            sendMessageToLogger(headerInfo, tradeEntry.getExchangeId(), "Unexpected (or missed) entry size in TradeEntry", Severity.ERROR);
//        }
//        if (!tradeEntry.hasExchangeId()) {
//            sendMessageToLogger(headerInfo, tradeEntry.getExchangeId(), "Missed exchangeId in TradeEntry", Severity.ERROR);
//        }
//        if (!tradeEntry.hasSide()) {
//            sendMessageToLogger(headerInfo, tradeEntry.getExchangeId(), "Missed AggressorSide in TradeEntry. It's required for L3 data", Severity.ERROR);
//        }
//        if (tradeEntry.getSide() == AggressorSide.BUY) {
//            if (!tradeEntry.hasSellerOrderId()) {
//                sendMessageToLogger(headerInfo, tradeEntry.getExchangeId(), "Missed seller order id in TradeEntry with AggressorSide = BUY in TradeEntry", Severity.ERROR);
//            }
//        } else {
//            if (!tradeEntry.hasBuyerOrderId()) {
//                sendMessageToLogger(headerInfo, tradeEntry.getExchangeId(), "Missed buyer order id in TradeEntry with AggressorSide = SELL in TradeEntry", Severity.ERROR);
//            }
//        }
//    }
//
//    private void validationCheck(PackageHeaderInfo headerInfo, BookResetEntry bookResetEntry) {
//        if (!bookResetEntry.hasExchangeId()) {
//            sendMessageToLogger(headerInfo, bookResetEntry.getExchangeId(), "Missed exchangeId in BookResetEntry", Severity.ERROR);
//        }
//        if (bookResetEntry.getModelType() != DataModelType.LEVEL_THREE) {
//            sendMessageToLogger(headerInfo, bookResetEntry.getExchangeId(), "Bad DataModelType in BookResetEntry", Severity.ERROR);
//        }
//    }

    private Severity minSeverity = Severity.INFO;

    @Override
    public void setMinimalSeverityToLog(Severity minSeverity) {
        this.minSeverity = minSeverity;
    }

    @Override
    public Severity getMinimalSeverityToLog() {
        return minSeverity;
    }

}

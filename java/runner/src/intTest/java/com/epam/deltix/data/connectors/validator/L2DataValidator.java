package com.epam.deltix.data.connectors.validator;

import com.epam.deltix.containers.AlphanumericUtils;
import com.epam.deltix.containers.CharSequenceUtils;
import com.epam.deltix.containers.MutableString;
import com.epam.deltix.containers.generated.DecimalLongArrayList;
import com.epam.deltix.containers.generated.LongArrayList;
import com.epam.deltix.containers.interfaces.LogProcessor;
import com.epam.deltix.containers.interfaces.Severity;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.timebase.messages.TypeConstants;
import com.epam.deltix.timebase.messages.universal.*;
import com.epam.deltix.timebase.orderbook.api.MarketSide;
import com.epam.deltix.timebase.orderbook.api.OrderBook;
import com.epam.deltix.timebase.orderbook.api.OrderBookFactory;
import com.epam.deltix.timebase.orderbook.api.OrderBookQuote;
import com.epam.deltix.timebase.orderbook.options.OrderBookOptionsBuilder;
import com.epam.deltix.timebase.orderbook.options.OrderBookType;
import com.epam.deltix.timebase.orderbook.options.UpdateMode;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import com.epam.deltix.util.collections.generated.ObjectList;

import java.io.IOException;
import java.util.ArrayList;

@SuppressWarnings("all")
public class L2DataValidator implements DataValidator {
    private MutableString logBuilder = new MutableString();
    private MutableString internalLogBuilder = new MutableString();
    private ArrayList<L2EntryNew> askSnapshotEntries = new ArrayList<>();
    private ArrayList<L2EntryNew> bidSnapshotEntries = new ArrayList<>();
    private CharSequence symbol;
    private OrderBook<OrderBookQuote> book;
    private PackageHeader incrementHeader = new PackageHeader();
    private long tickPrice, tickSize;
    private boolean checkNegativePrice;
    boolean checkExchangeMisPrice = false, checkPackageMisPrice = false, checkLevelMisPrice = false;
    long exchangeMisPrice, packageMisPrice, levelMisPrice;
    boolean checkTickSize = true, checkTickPrice = true;
    private boolean checkEqualLevels;
    private short marketDepth = Short.MAX_VALUE, minValidNumberOfLevels = 0;
    private LongArrayList exchangeIds = new LongArrayList();
    private LogProcessor logger;

    private DecimalLongArrayList previousBestAsks = new DecimalLongArrayList();
    private DecimalLongArrayList previousBestBids = new DecimalLongArrayList();
    boolean checkBidMoreThanAsk = true;
    boolean checkEmptySide = true;

    @SuppressWarnings({"unused", "WeakerAccess"})
    public L2DataValidator(CharSequence symbol, LogProcessor logProcessor, long tickPrice, long tickSize, boolean checkNegativePrice) {
        this.symbol = symbol;
        this.logger = logProcessor;
        incrementHeader.setEntries(new ObjectArrayList<>());
        incrementHeader.setPackageType(PackageType.INCREMENTAL_UPDATE);
        incrementHeader.setSymbol(symbol);
        this.tickPrice = tickPrice;
        this.tickSize = tickSize;
        this.checkNegativePrice = checkNegativePrice;

        book = OrderBookFactory.create(
            new OrderBookOptionsBuilder()
                .quoteLevels(DataModelType.LEVEL_TWO)
                .symbol(symbol.toString())
                .orderBookType(OrderBookType.SINGLE_EXCHANGE)
                .updateMode(UpdateMode.WAITING_FOR_SNAPSHOT)
                .build()
        );
    }


    /**
     * Set market depth in L2 format.
     * @param marketDepth market depth
     */
    @SuppressWarnings("WeakerAccess")
    public void setL2MarketDepth(short marketDepth) {
        this.marketDepth = marketDepth;
    }

    /**
     * If this parameter true L2DataValidator will check equal level.
     * @param checkEqualLevels If this parameter true L2DataValidator will check equal level.
     */
    @SuppressWarnings("WeakerAccess")
    public void setL2CheckEqualLevels(boolean checkEqualLevels) {
        this.checkEqualLevels = checkEqualLevels;
    }

    /**
     * Set minimal valid number of levels in L2 Format
     * @param minValidNumberOfLevels Minimal valid number of levels in L2 Format
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public void setL2MinValidNumberOfLevels(short minValidNumberOfLevels) {
        this.minValidNumberOfLevels = minValidNumberOfLevels;
    }

    private void checkSnapshotSide(PackageHeaderInfo headerInfo, ArrayList<L2EntryNew> bookSide, QuoteSide side) {
        if (bookSide.size() > 0) {
            if (bookSide.get(0).getLevel() != 0) {
                sendMessageToLogger(headerInfo, TypeConstants.EXCHANGE_NULL, "Incorrect order of levels in snapshot.", Severity.ERROR);
            }
        }
        for (int i = 1; i < bookSide.size(); ++i) {
            if (side == QuoteSide.ASK) {
                if (Decimal64Utils.isLess(bookSide.get(i).getPrice(), bookSide.get(i - 1).getPrice())) {
                    sendMessageToLogger(headerInfo, TypeConstants.EXCHANGE_NULL, "Price at ask level is less than at previous", Severity.ERROR);
                }
            } else {
                if (Decimal64Utils.isGreater(bookSide.get(i).getPrice(), bookSide.get(i - 1).getPrice())) {
                    sendMessageToLogger(headerInfo, TypeConstants.EXCHANGE_NULL, "Price at bid level is greater than at previous", Severity.ERROR);
                }
            }
            if (checkEqualLevels && Decimal64Utils.isEqual(bookSide.get(i).getPrice(), bookSide.get(i - 1).getPrice())) {
                if (side == QuoteSide.BID)
                    sendMessageToLogger(headerInfo, TypeConstants.EXCHANGE_NULL, "Price at bid level is equals to previous", Severity.ERROR);
                else
                    sendMessageToLogger(headerInfo, TypeConstants.EXCHANGE_NULL, "Price at ask level is equals to previous", Severity.ERROR);
            }
            if (bookSide.get(i).getLevel() != bookSide.get(i - 1).getLevel() + 1) {
                sendMessageToLogger(headerInfo, TypeConstants.EXCHANGE_NULL, "Incorrect order of levels in snapshot.", Severity.ERROR);
            }
        }
    }

    private void processSnapshot(PackageHeaderInfo headerInfo) {
        for (int i = 0; i < headerInfo.getEntries().size(); ++i) {
            if (headerInfo.getEntries().get(i).getExchangeId() != headerInfo.getEntries().get(0).getExchangeId()) {
                sendMessageToLogger(headerInfo, TypeConstants.EXCHANGE_NULL, "We support only one exchangeId for snapshots", Severity.ERROR);
            }
        }
        askSnapshotEntries.clear();
        bidSnapshotEntries.clear();
        for (int i = 0; i < headerInfo.getEntries().size(); ++i) {
            if (headerInfo.getEntries().get(i) instanceof BookResetEntry) continue;
            if (!(headerInfo.getEntries().get(i) instanceof L2EntryNew)) {
                sendMessageToLogger(headerInfo, TypeConstants.EXCHANGE_NULL, "We support only L2EntryNew and BookResetEntry entries for level2 snapshots", Severity.ERROR);
            }
            L2EntryNew entryNew = (L2EntryNew)headerInfo.getEntries().get(i);
            if (entryNew.getSide() == QuoteSide.ASK)
                askSnapshotEntries.add(entryNew);
            else
                bidSnapshotEntries.add(entryNew);
        }
        checkSnapshotSide(headerInfo, askSnapshotEntries, QuoteSide.ASK);
        checkSnapshotSide(headerInfo, bidSnapshotEntries, QuoteSide.BID);
        if (headerInfo.getPackageType() == PackageType.PERIODICAL_SNAPSHOT) {
            MarketSide<OrderBookQuote> askQuotes = book.getMarketSide(QuoteSide.ASK);
            MarketSide<OrderBookQuote> bidQuotes = book.getMarketSide(QuoteSide.BID);
            int askCount = askQuotes.depth();
            int bidCount = bidQuotes.depth();
            if (askCount != askSnapshotEntries.size() || bidCount != bidSnapshotEntries.size()) {
                sendMessageToLogger(headerInfo, TypeConstants.EXCHANGE_NULL, "Current state of book doesn't equal to periodical snapshot.", Severity.ERROR);
                return;
            }

            for (int i = 0; i < askCount; ++i) {
                if (Decimal64Utils.isNotEqual(askSnapshotEntries.get(i).getPrice(), askQuotes.getQuote(i).getPrice()) || Decimal64Utils.isNotEqual(askSnapshotEntries.get(i).getSize(), askQuotes.getQuote(i).getSize())) {
                    sendMessageToLogger(headerInfo, TypeConstants.EXCHANGE_NULL, "Current state of book doesn't equal to periodical snapshot.", Severity.ERROR);
                    return;
                }
            }

            for (int i = 0; i < bidCount; ++i) {
                if (Decimal64Utils.isNotEqual(bidSnapshotEntries.get(i).getPrice(), bidQuotes.getQuote(i).getPrice()) || Decimal64Utils.isNotEqual(bidSnapshotEntries.get(i).getSize(), bidQuotes.getQuote(i).getSize())) {
                    sendMessageToLogger(headerInfo, TypeConstants.EXCHANGE_NULL, "Current state of book doesn't equal to periodical snapshot.", Severity.ERROR);
                    return;
                }
            }

        }
    }

    private void validationCheck(PackageHeaderInfo headerInfo, L2EntryNew entryNew) {
        if ((!entryNew.hasLevel()) || entryNew.getLevel() < 0) {
            sendMessageToLogger(headerInfo, entryNew.getExchangeId(),"Unexpected (or missed) entry level in L2EntryNew", Severity.ERROR);
        }
        if (!entryNew.hasPrice() || (Decimal64Utils.isNaN(entryNew.getPrice())) || (Decimal64Utils.isInfinity(entryNew.getPrice()))) {
            sendMessageToLogger(headerInfo, entryNew.getExchangeId(), "Unexpected (or missed) entry price in L2EntryNew", Severity.ERROR);
        }
        if ((!entryNew.hasSize()) || (Decimal64Utils.isLessOrEqual(entryNew.getSize(), Decimal64Utils.ZERO)) || (Decimal64Utils.isNaN(entryNew.getSize())) || (Decimal64Utils.isInfinity(entryNew.getSize()))) {
            sendMessageToLogger(headerInfo, entryNew.getExchangeId(), "Unexpected (or missed) entry size in L2EntryNew", Severity.ERROR);
        }
        if (!entryNew.hasSide()) {
            sendMessageToLogger(headerInfo, entryNew.getExchangeId(), "Missed entry side in L2EntryNew", Severity.ERROR);
        }
        if (!entryNew.hasExchangeId()) {
            sendMessageToLogger(headerInfo, entryNew.getExchangeId(), "Missed exchangeId in L2EntryNew", Severity.ERROR);
        }

        if (checkNegativePrice && Decimal64Utils.isLess(entryNew.getPrice(), Decimal64Utils.ZERO)) {
            sendMessageToLogger(headerInfo, entryNew.getExchangeId(), "L2EntryNew price is negative", Severity.WARNING);
        }
        if (checkTickPrice && Decimal64Utils.isNotEqual(Decimal64Utils.round(entryNew.getPrice(), tickPrice), entryNew.getPrice())) {
            try {
                sendMessageToLogger(headerInfo,entryNew.getExchangeId(),  (CharSequence)Decimal64Utils.appendTo(entryNew.getPrice(), internalLogBuilder.clear().append("L2EntryNew price is not multiple of tickSize. Price: ")), Severity.ERROR);
            } catch (IOException e) {
                sendMessageToLogger(headerInfo, entryNew.getExchangeId(), "Error in Decimal64.appendToMutableString", Severity.ERROR);
            }
        }
        if (checkTickSize &&Decimal64Utils.isNotEqual(Decimal64Utils.round(entryNew.getSize(), tickSize), entryNew.getSize())) {
            try {
                sendMessageToLogger(headerInfo, entryNew.getExchangeId(), (CharSequence)Decimal64Utils.appendTo(entryNew.getSize(), internalLogBuilder.clear().append("L2EntryNew size is not multiple of lotSize. Size: ")), Severity.ERROR);
            } catch (IOException e) {
                sendMessageToLogger(headerInfo, entryNew.getExchangeId(), "Error in Decimal64.appendToMutableString", Severity.ERROR);
            }
        }
        if (entryNew.getLevel() >= marketDepth) {
            sendMessageToLogger(headerInfo, entryNew.getExchangeId(), "L2EntryNew level is greater than market depth.", Severity.ERROR);
        }

    }

    private void validationCheck(PackageHeaderInfo headerInfo, L2EntryUpdate entryUpdate) {
        if ((!entryUpdate.hasLevel()) || entryUpdate.getLevel() < 0) {
            sendMessageToLogger(headerInfo, entryUpdate.getExchangeId(), "Unexpected (or missed) entry level in L2EntryUpdate", Severity.ERROR);
        }
        if (!entryUpdate.hasSide()) {
            sendMessageToLogger(headerInfo, entryUpdate.getExchangeId(), "Missed entry side in L2EntryUpdate", Severity.ERROR);
        }
        if (!entryUpdate.hasExchangeId()) {
            sendMessageToLogger(headerInfo, entryUpdate.getExchangeId(), "Missed exchangeId in L2EntryUpdate", Severity.ERROR);
        }
        if (!entryUpdate.hasAction()) {
            sendMessageToLogger(headerInfo, entryUpdate.getExchangeId(), "Missed action in L2EntryUpdate", Severity.ERROR);
        }
        if (entryUpdate.getAction() == BookUpdateAction.UPDATE) {
            if (!entryUpdate.hasPrice() || (Decimal64Utils.isNaN(entryUpdate.getPrice())) || (Decimal64Utils.isInfinity(entryUpdate.getPrice()))) {
                sendMessageToLogger(headerInfo, entryUpdate.getExchangeId(), "Unexpected (or missed) entry price in L2EntryUpdate with Action = Update", Severity.ERROR);
            }
            if ((!entryUpdate.hasSize()) || (Decimal64Utils.isLessOrEqual(entryUpdate.getSize(), Decimal64Utils.ZERO)) || (Decimal64Utils.isNaN(entryUpdate.getSize())) || (Decimal64Utils.isInfinity(entryUpdate.getSize()))) {
                sendMessageToLogger(headerInfo, entryUpdate.getExchangeId(), "Unexpected (or missed) entry size in L2EntryUpdate with Action = Update", Severity.ERROR);
            }
            if (checkTickSize && Decimal64Utils.isNotEqual(Decimal64Utils.round(entryUpdate.getSize(), tickSize), entryUpdate.getSize())) {
                try {
                    sendMessageToLogger(headerInfo, entryUpdate.getExchangeId(), (CharSequence)Decimal64Utils.appendTo(entryUpdate.getSize(), internalLogBuilder.clear().append("L2EntryUpdate size is not multiple of lotSize. Size: ")), Severity.ERROR);
                } catch (IOException e) {
                    sendMessageToLogger(headerInfo, entryUpdate.getExchangeId(), "Error in Decimal64.appendToMutableString", Severity.ERROR);
                }
            }
        } else if (entryUpdate.getAction() == BookUpdateAction.INSERT) {
            sendMessageToLogger(headerInfo, entryUpdate.getExchangeId(), "Invalid action in L2EntryUpdate. We don't support inserting by L2EntryUpdate.", Severity.ERROR);
        }

        if (entryUpdate.getLevel() >= marketDepth) {
            sendMessageToLogger(headerInfo, entryUpdate.getExchangeId(), "L2EntryUpdate level is greater than market depth.", Severity.ERROR);
        }

    }

    private void validationCheck(PackageHeaderInfo headerInfo, TradeEntry tradeEntry) {
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
                sendMessageToLogger(headerInfo, tradeEntry.getExchangeId(), (CharSequence)Decimal64Utils.appendTo(tradeEntry.getSize(), internalLogBuilder.clear().append("TradeEntry size is not multiple of lotSize. Size: ")), Severity.ERROR);
            } catch (IOException e) {
                sendMessageToLogger(headerInfo, tradeEntry.getExchangeId(), "Error in Decimal64.appendToMutableString", Severity.ERROR);
            }
        }

    }

    private void validationCheck(PackageHeaderInfo headerInfo, BookResetEntry bookResetEntry) {
//        if (!bookResetEntry.hasExchangeId()) {
//            sendMessageToLogger(headerInfo, bookResetEntry.getExchangeId(), "Missed exchangeId in BookResetEntry", Severity.ERROR);
//        }
//        if (bookResetEntry.getModelType() != DataModelType.LEVEL_TWO) {
//            sendMessageToLogger(headerInfo, bookResetEntry.getExchangeId(), "Bad DataModelType in BookResetEntry", Severity.ERROR);
//        }
    }

    private void checkTradePrice(PackageHeaderInfo headerInfo) {
        ObjectList<BaseEntryInfo> entries = headerInfo.getEntries();
        for (int i = 0; i < entries.size(); ++i) {
            BaseEntryInfo entry = entries.get(i);
            if (entry instanceof TradeEntry) {
                checkDiffPrice(headerInfo, ((TradeEntry) entry).getPrice());
            }
        }
    }

    private void checkDiffPrice(PackageHeaderInfo headerInfo, long price) {
        MarketSide<OrderBookQuote> quotes = book.getMarketSide(QuoteSide.ASK);
        if (quotes == null || quotes.depth() == 0) {
            quotes = book.getMarketSide(QuoteSide.BID);
        }

        if (quotes != null && quotes.depth() > 0) {
            long anchorPrice = quotes.getQuote(0).getPrice();
            long diff = Decimal64Utils.isGreater(anchorPrice, price) ?
                Decimal64Utils.divide(anchorPrice, price) :
                Decimal64Utils.divide(price, anchorPrice);
            if (Decimal64Utils.isGreater(diff, Decimal64Utils.fromInt(3))) {
                sendMessageToLogger(headerInfo, quotes.getQuote(0).getExchangeId(),
                    "Price difference is lager than 2 (" + Decimal64Utils.toString(anchorPrice) + " : " + Decimal64Utils.toString(price) + ")", Severity.ERROR
                );
            }
        }
    }

    private void checkAllEntriesFields(PackageHeaderInfo headerInfo) {
        for (int i = 0; i < headerInfo.getEntries().size(); ++i) {
            if (headerInfo.getEntries().get(i) instanceof L2EntryNew) {
                L2EntryNew entryNew = (L2EntryNew)headerInfo.getEntries().get(i);
                validationCheck(headerInfo, entryNew);
            } else if (headerInfo.getEntries().get(i) instanceof L2EntryUpdate) {
                L2EntryUpdate entryUpdate = (L2EntryUpdate)headerInfo.getEntries().get(i);
                validationCheck(headerInfo, entryUpdate);
            } else if (headerInfo.getEntries().get(i) instanceof TradeEntry) {
                TradeEntry tradeEntry = (TradeEntry)headerInfo.getEntries().get(i);
                validationCheck(headerInfo, tradeEntry);
            } else if (headerInfo.getEntries().get(i) instanceof BookResetEntry) {
                BookResetEntry bookResetEntry = (BookResetEntry) headerInfo.getEntries().get(i);
                validationCheck(headerInfo, bookResetEntry); // todo
            } else {
                sendMessageToLogger(headerInfo, headerInfo.getEntries().get(i).getExchangeId(), "Unexpected entry type for level2 messages", Severity.ERROR);
            }
        }
    }

    private void validateIncrementEntry(PackageHeaderInfo headerInfo, L2EntryNew entryNew) {
        MarketSide<OrderBookQuote> quotes = book.getMarketSide(entryNew.getSide());
        if (entryNew.getLevel() > quotes.depth()) {
            sendMessageToLogger(headerInfo, entryNew.getExchangeId(), "Incorrect level. Greater than level's count + 1", Severity.ERROR);
            return;
        }

        if (entryNew.getLevel() == 0) {
            if (quotes.depth() == 0) return;
            if (entryNew.getSide() == QuoteSide.ASK) {
                if (Decimal64Utils.isGreater(entryNew.getPrice(), quotes.getQuote(0).getPrice())) {
                    sendMessageToLogger(headerInfo, entryNew.getExchangeId(), "Incorrect price. Prices after this entry wouldn't be sorted", Severity.ERROR);
                }
            } else {
                if (Decimal64Utils.isLess(entryNew.getPrice(), quotes.getQuote(0).getPrice())) {
                    sendMessageToLogger(headerInfo, entryNew.getExchangeId(), "Incorrect price. Prices after this entry wouldn't be sorted", Severity.ERROR);
                }
            }
            if (checkEqualLevels) {
                if (Decimal64Utils.isEqual(entryNew.getPrice(), quotes.getQuote(0).getPrice())) {
                    sendMessageToLogger(headerInfo, entryNew.getExchangeId(), "Incorrect price. Prices after this entry wouldn't be strictly sorted (without equal levels)", Severity.ERROR);
                }
            }
        } else if (entryNew.getLevel() == quotes.depth()) {
            if (entryNew.getSide() == QuoteSide.ASK) {
                if (Decimal64Utils.isLess(entryNew.getPrice(), quotes.getQuote(quotes.depth() - 1).getPrice())) {
                    sendMessageToLogger(headerInfo, entryNew.getExchangeId(), "Incorrect price. Prices after this entry wouldn't be sorted", Severity.ERROR);
                }
            } else {
                if (Decimal64Utils.isGreater(entryNew.getPrice(), quotes.getQuote(quotes.depth() - 1).getPrice())) {
                    sendMessageToLogger(headerInfo, entryNew.getExchangeId(), "Incorrect price. Prices after this entry wouldn't be sorted", Severity.ERROR);
                }
            }
            if (checkEqualLevels) {
                if (Decimal64Utils.isEqual(entryNew.getPrice(), quotes.getQuote(quotes.depth() - 1).getPrice())) {
                    sendMessageToLogger(headerInfo, entryNew.getExchangeId(), "Incorrect price. Prices after this entry wouldn't be strictly sorted (without equal levels)", Severity.ERROR);
                }
            }
        } else {
            if (entryNew.getSide() == QuoteSide.ASK) {
                if (Decimal64Utils.isLess(entryNew.getPrice(), quotes.getQuote(entryNew.getLevel() - 1).getPrice()) || Decimal64Utils.isGreater(entryNew.getPrice(), quotes.getQuote(entryNew.getLevel()).getPrice())) {
                    sendMessageToLogger(headerInfo, entryNew.getExchangeId(), "Incorrect price. Prices after this entry wouldn't be sorted", Severity.ERROR);
                }
            } else {
                if (Decimal64Utils.isGreater(entryNew.getPrice(), quotes.getQuote(entryNew.getLevel() - 1).getPrice()) || Decimal64Utils.isLess(entryNew.getPrice(), quotes.getQuote(entryNew.getLevel()).getPrice())) {
                    sendMessageToLogger(headerInfo, entryNew.getExchangeId(), "Incorrect price. Prices after this entry wouldn't be sorted", Severity.ERROR);
                }
            }
            if (checkEqualLevels) {
                if (Decimal64Utils.isEqual(entryNew.getPrice(), quotes.getQuote(entryNew.getLevel() - 1).getPrice()) || Decimal64Utils.isEqual(entryNew.getPrice(), quotes.getQuote(entryNew.getLevel()).getPrice())) {
                    sendMessageToLogger(headerInfo, entryNew.getExchangeId(), "Incorrect price. Prices after this entry wouldn't be strictly sorted (without equal levels)", Severity.ERROR);
                }

            }
        }
    }

    private void validateIncrementEntry(PackageHeaderInfo headerInfo, L2EntryUpdate entryUpdate) {
        MarketSide<OrderBookQuote> quotes = book.getMarketSide(entryUpdate.getSide());
        if (entryUpdate.getLevel() >= quotes.depth()) {
            sendMessageToLogger(headerInfo, entryUpdate.getExchangeId(), "Incorrect level. Greater than level's count.", Severity.ERROR);
        }
        if (entryUpdate.getAction() == BookUpdateAction.UPDATE) {
            if (Decimal64Utils.isNotEqual(entryUpdate.getPrice(), quotes.getQuote(entryUpdate.getLevel()).getPrice())) {
                sendMessageToLogger(headerInfo, entryUpdate.getExchangeId(), "Incorrect price in L2EntryUpdate with action = Update. Not equals to book price", Severity.ERROR);
            }
        }
    }
    /**
     * Send next package from stream to validate.
     * @param headerInfo Package.
     */
    @SuppressWarnings("ForLoopReplaceableByForEach")
    public void sendPackage(PackageHeaderInfo headerInfo) {
        try {
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
                return;
            }

            checkTradePrice(headerInfo);

            checkAllEntriesFields(headerInfo);
            exchangeIds.clear();
            previousBestAsks.clear();
            previousBestBids.clear();
            if (headerInfo.getPackageType() != PackageType.INCREMENTAL_UPDATE) {
                processSnapshot(headerInfo);
                long exchangeId = headerInfo.getEntries().get(0).getExchangeId();
                exchangeIds.add(exchangeId);
                OrderBookQuote askQuote = book.getMarketSide(QuoteSide.ASK).getQuote(0);
                OrderBookQuote bidQuote = book.getMarketSide(QuoteSide.BID).getQuote(0);
                if (askQuote != null) previousBestAsks.add(askQuote.getPrice()); else previousBestAsks.add(Decimal64Utils.NEGATIVE_INFINITY);
                if (bidQuote != null) previousBestBids.add(bidQuote.getPrice()); else previousBestBids.add(Decimal64Utils.POSITIVE_INFINITY);
                book.update(headerInfo);
            } else {
                for (int i = 0; i < headerInfo.getEntries().size(); ++i) {
                    incrementHeader.getEntries().add(headerInfo.getEntries().get(i));

                    if (!exchangeIds.contains(incrementHeader.getEntries().get(0).getExchangeId())) {
                        long exchangeId = incrementHeader.getEntries().get(0).getExchangeId();
                        exchangeIds.add(exchangeId);
                        OrderBookQuote askQuote = book.getMarketSide(QuoteSide.ASK).getQuote(0);
                        OrderBookQuote bidQuote = book.getMarketSide(QuoteSide.BID).getQuote(0);
                        if (askQuote != null) previousBestAsks.add(askQuote.getPrice()); else previousBestAsks.add(Decimal64Utils.NEGATIVE_INFINITY);
                        if (bidQuote != null) previousBestBids.add(bidQuote.getPrice()); else previousBestBids.add(Decimal64Utils.POSITIVE_INFINITY);
                    }
                    if (incrementHeader.getEntries().get(0) instanceof L2EntryUpdate) {
                        L2EntryUpdate entryUpdate = (L2EntryUpdate) incrementHeader.getEntries().get(0);
                        validateIncrementEntry(headerInfo, entryUpdate);
                    } else if (incrementHeader.getEntries().get(0) instanceof L2EntryNew) {
                        L2EntryNew entryNew = (L2EntryNew) incrementHeader.getEntries().get(0);
                        validateIncrementEntry(headerInfo, entryNew);
                    }
                    book.update(incrementHeader);
                    incrementHeader.getEntries().clear();
                }
            }
            if (checkExchangeMisPrice) {
                long minAsk = Decimal64Utils.POSITIVE_INFINITY;
                long maxAsk = Decimal64Utils.NEGATIVE_INFINITY;
                long minBid = Decimal64Utils.POSITIVE_INFINITY;
                long maxBid = Decimal64Utils.NEGATIVE_INFINITY;

                MarketSide<OrderBookQuote> askQuotes = book.getMarketSide(QuoteSide.ASK);
                MarketSide<OrderBookQuote> bidQuotes = book.getMarketSide(QuoteSide.BID);
                if (askQuotes.depth() > 0) {
                    minAsk = Decimal64Utils.min(askQuotes.getQuote(0).getPrice(), minAsk);
                    maxAsk = Decimal64Utils.max(askQuotes.getQuote(0).getPrice(), maxAsk);
                }
                if (bidQuotes.depth() > 0) {
                    minBid = Decimal64Utils.min(bidQuotes.getQuote(0).getPrice(), minBid);
                    maxBid = Decimal64Utils.max(bidQuotes.getQuote(0).getPrice(), maxBid);
                }
                if (!Decimal64Utils.isZero(minAsk) && Decimal64Utils.isGreater(Decimal64Utils.divide(Decimal64Utils.subtract(maxAsk, minAsk), Decimal64Utils.abs(minAsk)), exchangeMisPrice)) {
                    sendMessageToLogger(headerInfo, TypeConstants.EXCHANGE_NULL, internalLogBuilder.clear().append("Ask Mispricing between exchanges"), Severity.WARNING);
                }
                if (!Decimal64Utils.isZero(minBid) && Decimal64Utils.isGreater(Decimal64Utils.divide(Decimal64Utils.subtract(maxBid, minBid), Decimal64Utils.abs(minBid)), exchangeMisPrice)) {
                    sendMessageToLogger(headerInfo, TypeConstants.EXCHANGE_NULL, internalLogBuilder.clear().append("Bid Mispricing between exchanges"), Severity.WARNING);
                }
            }
            if (checkLevelMisPrice) {
                MarketSide<OrderBookQuote> askQuotes = book.getMarketSide(QuoteSide.ASK);
                MarketSide<OrderBookQuote> bidQuotes = book.getMarketSide(QuoteSide.BID);
                for (int level = 1; level < askQuotes.depth() ;level++) {
                    long previousPrice = askQuotes.getQuote(level - 1).getPrice();
                    long currentPrice = askQuotes.getQuote(level).getPrice();
                    if (Decimal64Utils.isZero(currentPrice)) continue;
                    long r = Decimal64Utils.divide(previousPrice, currentPrice);
                    if (Decimal64Utils.isLess(r, Decimal64Utils.subtract(Decimal64Utils.ONE, levelMisPrice))) {
                        sendMessageToLogger(headerInfo, askQuotes.getQuote(level).getExchangeId(), internalLogBuilder.clear().append("Ask Mispricing in the exchange ").append(AlphanumericUtils.toString(askQuotes.getQuote(level).getExchangeId())).append(" at level ").append(level), Severity.WARNING);
                    }
                }
                for (int level = 1; level < bidQuotes.depth() ;level++) {
                    long previousPrice = bidQuotes.getQuote(level - 1).getPrice();
                    long currentPrice = bidQuotes.getQuote(level).getPrice();
                    long r = Decimal64Utils.divide(currentPrice, previousPrice);
                    if (Decimal64Utils.isZero(previousPrice)) continue;
                    if (Decimal64Utils.isLess(r, Decimal64Utils.subtract(Decimal64Utils.ONE, levelMisPrice))) {
                        sendMessageToLogger(headerInfo, bidQuotes.getQuote(level).getExchangeId(), internalLogBuilder.clear().append("Bid Mispricing in the exchange ").append(AlphanumericUtils.toString(bidQuotes.getQuote(level).getExchangeId())).append(" at level ").append(level), Severity.WARNING);
                    }
                }
            }
            for (int i = 0; i < exchangeIds.size(); ++i) {
                MarketSide<OrderBookQuote> askQuotes = book.getMarketSide(QuoteSide.ASK);
                MarketSide<OrderBookQuote> bidQuotes = book.getMarketSide(QuoteSide.BID);
                OrderBookQuote bestAskQuote = askQuotes != null ? askQuotes.getQuote(0) : null;
                OrderBookQuote bestBidQuote = bidQuotes != null ? bidQuotes.getQuote(0) : null;

                if (bestAskQuote != null && bestBidQuote != null) {
                    if (Decimal64Utils.isLess(bestAskQuote.getPrice(), bestBidQuote.getPrice()) && checkBidMoreThanAsk) {
                        sendMessageToLogger(headerInfo, bestAskQuote.getExchangeId(), "Bid > Ask", Severity.WARNING);
                    }
                    if (checkPackageMisPrice) {
                        if (!Decimal64Utils.isInfinity(previousBestAsks.get(i)) && !Decimal64Utils.isZero(previousBestAsks.get(i))) {
                            if (Decimal64Utils.isGreater(Decimal64Utils.abs(Decimal64Utils.divide(Decimal64Utils.subtract(previousBestAsks.get(i), bestAskQuote.getPrice()), previousBestAsks.get(i))), packageMisPrice)) {
                                sendMessageToLogger(headerInfo, bestAskQuote.getExchangeId(), "Too big ask price change after this package", Severity.WARNING);
                            }
                        }


                        if (!Decimal64Utils.isInfinity(previousBestBids.get(i)) && !Decimal64Utils.isZero(previousBestBids.get(i))) {
                            if (Decimal64Utils.isGreater(Decimal64Utils.abs(Decimal64Utils.divide(Decimal64Utils.subtract(previousBestBids.get(i), bestBidQuote.getPrice()), previousBestBids.get(i))), packageMisPrice)) {
                                sendMessageToLogger(headerInfo, bestAskQuote.getExchangeId(), "Too big bid price change after this package", Severity.WARNING);
                            }
                        }
                    }
                }

                if (bestAskQuote == null && checkEmptySide) {
                    sendMessageToLogger(headerInfo, exchangeIds.get(i), "Ask side is empty", Severity.WARNING);
                }

                if (bestBidQuote == null && checkEmptySide) {
                    sendMessageToLogger(headerInfo, exchangeIds.get(i), "Bid side is empty", Severity.WARNING);
                }

                if (askQuotes.depth() < minValidNumberOfLevels) {
                    sendMessageToLogger(headerInfo, exchangeIds.get(i), "Ask side has less than minimal valid number of levels", Severity.WARNING);
                }

                if (bidQuotes.depth() < minValidNumberOfLevels)  {
                    sendMessageToLogger(headerInfo, exchangeIds.get(i), "Bid side has less than minimal valid number of levels", Severity.WARNING);
                }
            }
        } catch (Throwable e) {
            logger.onLogEvent(this, Severity.ERROR, e, "Error in L2DataValidator.");
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

    private Severity minSeverity = Severity.INFO;

    @Override
    public void setLogger(LogProcessor logger) {
        this.logger = logger;
    }

    @Override
    public void setMinimalSeverityToLog(Severity minSeverity) {
        this.minSeverity = minSeverity;
    }

    @Override
    public Severity getMinimalSeverityToLog() {
        return minSeverity;
    }

}

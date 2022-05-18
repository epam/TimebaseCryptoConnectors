package com.epam.deltix.data.connectors.commons;

import com.epam.deltix.qsrv.hf.pub.md.Introspector;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.timebase.messages.service.SecurityFeedStatusMessage;
import com.epam.deltix.timebase.messages.universal.PackageHeader;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

// TODO: finish custom type management when have a case/an example

/**
 * The purpose of the class is to:
 * <ul>
 *   <li>describe a market data model the data connector supports</li>
 *   <li>store a subset of the data model selected by a user to produce/store</li>
 * </ul>
 *
 */
public class MdModel {
    private static final RecordClassDescriptor[] EMPTY_RCDS = new RecordClassDescriptor[] {};

    private static final RecordClassDescriptor PACKAGE_HEADER_RCD;
    private static final RecordClassDescriptor SECURITY_FEED_STATUS_RCD;
    private static final RecordClassDescriptor[] DEFAULT_RCDS;

    static {
        final Introspector introspector = Introspector.createEmptyMessageIntrospector();
        try {
            PACKAGE_HEADER_RCD = introspector.introspectRecordClass(PackageHeader.class);
            SECURITY_FEED_STATUS_RCD = introspector.introspectRecordClass(SecurityFeedStatusMessage.class);
        } catch (Introspector.IntrospectionException e) {
            throw new Error(e);
        }
        DEFAULT_RCDS = new RecordClassDescriptor[]{
                PACKAGE_HEADER_RCD,
                SECURITY_FEED_STATUS_RCD
        };
    }

    /**
     * Returns available data model
     * @return the data model
     */
    public static Availability availability() {
        return new Availability();
    }

    /**
     * Options of the data model with associated {@link com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor}s
     * @see com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor RecordClassDescriptor
     */
    public static class Options {
        protected RecordClassDescriptor[] trades;
        protected RecordClassDescriptor[] level1;
        protected RecordClassDescriptor[] level2;
        protected RecordClassDescriptor[] custom;

        private Options() {
        }

        private Options(
                final RecordClassDescriptor[] trades,
                final RecordClassDescriptor[] level1,
                final RecordClassDescriptor[] level2,
                final RecordClassDescriptor[] custom) {
            this.trades = trades;
            this.level1 = level1;
            this.level2 = level2;
            this.custom = custom;
        }

        /**
         * Shows if trades set
         * @return true if trades set for the data model, otherwise false
         */
        public boolean trades() {
            return tradesTypes().length > 0;
        }

        /**
         * Returns {@link com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor}s
         *
         * @see com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor RecordClassDescriptor
         * @return RecordClassDescriptors for trades
         */
        public RecordClassDescriptor[] tradesTypes() {
            return trades != null ? trades : EMPTY_RCDS;
        }

        public boolean level1() {
            return level1Types().length > 0;
        }

        /**
         *
         * @see com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor RecordClassDescriptor
         * @return
         */
        public RecordClassDescriptor[] level1Types() {
            return level1 != null ? level1 : EMPTY_RCDS;
        }

        public boolean level2() {
            return level2Types().length > 0;
        }

        /**
         *
         * @see com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor RecordClassDescriptor
         * @return
         */
        public RecordClassDescriptor[] level2Types() {
            return level2 != null ? level2 : EMPTY_RCDS;
        }

        public boolean custom() {
            return customTypes().length > 0;
        }

        /**
         *
         * @see com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor RecordClassDescriptor
         * @return
         */
        public RecordClassDescriptor[] customTypes() {
            return custom != null ? custom : EMPTY_RCDS;
        }

        public RecordClassDescriptor[] types() {
            // TODO: maybe replace with a Map to
            // make unique set by the type name, not by the magic GUID
            // OR make a more complex RCD joining
            final Set<RecordClassDescriptor> result = new HashSet<>();
            result.addAll(Arrays.asList(tradesTypes()));
            result.addAll(Arrays.asList(level1Types()));
            result.addAll(Arrays.asList(level2Types()));
            result.addAll(Arrays.asList(customTypes()));
            return result.toArray(RecordClassDescriptor[]::new);
        }

        /**
         *
         * @return
         */
        public boolean isEmpty() {
            return !(trades() || level1() || level2() || custom());
        }

        @Override
        public String toString() {
            return "Trades [" + (trades() ? "+" : "-") + "]" + Util.NATIVE_LINE_BREAK +
                    "Level1 [" + (level1() ? "+" : "-") + "]" + Util.NATIVE_LINE_BREAK +
                    "Level2 [" + (level1() ? "+" : "-") + "]" + Util.NATIVE_LINE_BREAK +
                    "Custom [" + (custom() ? "+" : "-") + "]";
        }
    }

    /**
     *
     * @param <S>
     */
    public static class ModifiableOptions<S extends Options> extends Options {
        private ModifiableOptions() {
        }

        /**
         *
         * @return
         */
        @SuppressWarnings("unchecked")
        public S withTrades() {
            trades = DEFAULT_RCDS;
            return (S) this;
        }

        /**
         *
         * @param tradesType
         * @return
         */
        @SuppressWarnings("unchecked")
        public S withTrades(final RecordClassDescriptor... tradesType) {
            trades = tradesType;
            return (S) this;
        }

        /**
         *
         * @return
         */
        @SuppressWarnings("unchecked")
        public S withLevel1() {
            level1 = DEFAULT_RCDS;
            return (S) this;
        }

        /**
         *
         * @param level1Types
         * @return
         */
        @SuppressWarnings("unchecked")
        public S withLevel1(final RecordClassDescriptor... level1Types) {
            level1 = level1Types;
            return (S) this;
        }

        /**
         *
         * @return
         */
        @SuppressWarnings("unchecked")
        public S withLevel2() {
            level2 = DEFAULT_RCDS;
            return (S) this;
        }

        /**
         *
         * @param level2Types
         * @return
         */
        @SuppressWarnings("unchecked")
        public S withLevel2(final RecordClassDescriptor... level2Types) {
            level2 = level2Types;
            return (S) this;
        }

        /**
         *
         * @param customTypes
         * @return
         */
        @SuppressWarnings("unchecked")
        public S withCustom(final RecordClassDescriptor... customTypes) {
            custom = customTypes;
            return (S) this;
        }
    }

    /**
     *
     */
    public static class Availability extends ModifiableOptions<Availability> {
        /**
         *
         * @return
         */
        public MdModel build() {
            return new MdModel(this);
        }
    }

    /**
     *
     */
    public class Selection extends ModifiableOptions<Selection> {
        /**
         *
         * @return
         */
        @Override
        public Selection withTrades() {
            if (!availability.trades()) {
                throw new IllegalArgumentException("Trades are not supported by the model");
            }
            return super.withTrades();
        }

        /**
         *
         * @return
         */
        @Override
        public Selection withLevel1() {
            if (!availability.level1()) {
                throw new IllegalArgumentException("Level 1 is not supported by the model");
            }
            return super.withLevel1();
        }

        /**
         *
         * @return
         */
        @Override
        public Selection withLevel2() {
            if (!availability.level2()) {
                throw new IllegalArgumentException("Level 2 is not supported by the model");
            }
            return super.withLevel2();
        }

        /**
         *
         * @return
         */
        public Options build() {
            return new Options(
                    tradesTypes(),
                    level1Types(),
                    level2Types(),
                    customTypes());
        }
    }

    private final Availability availability;

    private MdModel(final Availability availability) {
        this.availability = availability;
    }

    /**
     *
     * @return
     */
    public Availability available() {
        return availability;
    }

    /**
     *
     * @return
     */
    public Selection select() {
        return new Selection();
    }
}

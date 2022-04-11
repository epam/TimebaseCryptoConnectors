package com.epam.deltix.data.connectors.commons;

public enum MdModelEnum {
    TRADES,
    L1,
    L2;

    public static void with(MdModel.ModifiableOptions options, String enumValue) {
        with(options, MdModelEnum.valueOf(enumValue));
    }

    public static void with(MdModel.ModifiableOptions options, MdModelEnum enumValue) {
        switch (enumValue) {
            case TRADES:
                options.withTrades();
                return;
            case L1:
                options.withLevel1();
                return;
            case L2:
                options.withLevel2();
                return;
        }
    }
}

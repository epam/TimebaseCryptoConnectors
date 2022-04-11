package com.epam.deltix.data.connectors.commons.l2;

/**
 * <p>Represents actions that can be applied to a book. <br>
 *  All actions are named according FIX.</p>
 */
public enum BookAction {

    New('0'),
    Change('1'),
    Delete('2'),
    DeleteThru('3'),
    DeleteFrom('4'),
    Overlay('5');

    private final char code;

    /**
     * <p>Constructs new object with specified code</p>
     *
     * @param code code of action for new object
     */
    BookAction(final char code) {
        this.code = code;
    }

    /**
     * <p>Returns enum value of book action.
     * Use this method to decode action from it's number.</p>
     *
     * @param c number of action in this enum
     * @return enum value witch matches specified number
     * @throws IllegalArgumentException if specified action number was not found
     */
    public static BookAction decode(final char c) {
        switch (c) {
            case '0':
                return New;
            case '1':
                return Change;
            case '2':
                return Delete;
            case '3':
                return DeleteThru;
            case '4':
                return DeleteFrom;
            case '5':
                return Overlay;
        }

        throw new IllegalArgumentException("Unknown action code: " + c);
    }

    /**
     * <p>Returns code of this enum</p>
     *
     * @return code of this enum
     */
    public char getCode() {
        return this.code;
    }
}

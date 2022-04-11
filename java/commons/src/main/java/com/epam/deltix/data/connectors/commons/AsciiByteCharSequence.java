package com.epam.deltix.data.connectors.commons;

public class AsciiByteCharSequence implements CharSequence {
    private byte[] bytes;
    private int length;

    public AsciiByteCharSequence() {
        this(128);
    }

    public AsciiByteCharSequence(final int size) {
        this(new byte[size]);
    }

    public AsciiByteCharSequence(final byte[] bytes) {
        this.bytes = bytes;
    }

    public void set(final byte[] bytes, final int length) {
        this.bytes = bytes;
        this.length = length;
    }

    public byte[] bytes() {
        return bytes;
    }

    public void setLength(final int length) {
        this.length = length;
    }

    @Override
    public int length() {
        return length;
    }

    public void clear() {
        length = 0;
    }

    @Override
    public char charAt(final int index) {
        return (char) bytes[index];
    }

    @Override
    public CharSequence subSequence(final int start, final int end) {
        return new String(bytes, start, end);
    }
}

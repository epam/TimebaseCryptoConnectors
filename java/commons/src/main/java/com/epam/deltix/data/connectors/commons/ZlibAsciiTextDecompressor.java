package com.epam.deltix.data.connectors.commons;

import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class ZlibAsciiTextDecompressor {
    private static final int BUFFER_SIZE = 2048;

    private final Inflater inflater = new Inflater(true);
    private final byte[] unzippedDataPortion = new byte[BUFFER_SIZE];
    private final AsciiByteCharSequence text = new AsciiByteCharSequence();
    private final int skipHeaderSize;

    private byte[] receivedData = new byte[BUFFER_SIZE];
    private byte[] unzippedData = new byte[BUFFER_SIZE];

    public ZlibAsciiTextDecompressor(boolean skipGzipHeader) {
        this.skipHeaderSize = skipGzipHeader ? 10 : 0;
    }

    public CharSequence decompress(final ByteBuffer data) throws DataFormatException {
        final int copiedBytes = copyReceivedData(data);
        final int decompressedBytes = decompress(receivedData, copiedBytes);
        if (decompressedBytes == -1) {
            return null;
        }
        text.set(unzippedData, decompressedBytes);
        return text;
    }

    private int copyReceivedData(final ByteBuffer data) {
        final int length = data.remaining();
        if (receivedData.length < length) {
            receivedData = new byte[length << 1];
        }
        data.get(receivedData, 0, length);
        return length;
    }

    private int decompress(final byte[] input, final int length) throws DataFormatException {
        try {
            inflater.setInput(input, skipHeaderSize, length - skipHeaderSize);
            int total = 0;
            while (!inflater.finished()) {
                final int count = inflater.inflate(unzippedDataPortion);

                if (total + count > unzippedData.length) {
                    final byte[] newData = new byte[(total + count) << 1];
                    System.arraycopy(unzippedData, 0, newData, 0, unzippedData.length);
                    unzippedData = newData;
                }

                System.arraycopy(unzippedDataPortion, 0, unzippedData, total, count);
                total += count;
            }
            return total;
        } finally {
            inflater.reset();
        }
    }
}

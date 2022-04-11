package com.epam.deltix.data.connectors.coinbase;

import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickCursor;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickDBFactory;
import com.epam.deltix.qsrv.hf.tickdb.pub.TimeConstants;

/**
 * Remove from the repo OR transform to an integration test
 */
public class ReadCoinbaseData {
    public static void main(String[] args) {
        final DXTickDB db = TickDBFactory.createFromUrl("dxtick://localhost:8011");
        db.open(false);
        final DXTickStream stream = db.getStream("coinbase");

        try (TickCursor cursor = stream.select(TimeConstants.USE_CURRENT_TIME,
                new SelectionOptions(true, true))) {
            while (cursor.next()) {
                System.out.println(cursor.getMessage());
            }
        }
    }
}

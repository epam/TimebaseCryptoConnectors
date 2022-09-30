package com.epam.deltix.data.connectors.uniswapsubgraph;

import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickCursor;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickDBFactory;
import com.epam.deltix.qsrv.hf.tickdb.pub.TimeConstants;

// Remove from the repo OR transform to an integration test
public class ReadUniswapData {
    public static void main(String[] args) {
        final DXTickDB db = TickDBFactory.createFromUrl("dxtick://localhost:8011");
        db.open(true);
        for (final DXTickStream stream : db.listStreams()) {
            System.out.println(stream.getKey() + " [" + stream.getName() + "]");
        }

        final DXTickStream stream = db.getStream("uniswap");

        try (TickCursor cursor = stream.select(TimeConstants.USE_CURRENT_TIME,
                new SelectionOptions(false, true))) {
            while (cursor.next()) {
                System.out.println(cursor.getMessage());
            }
        }
    }
}

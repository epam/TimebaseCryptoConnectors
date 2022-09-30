package com.epam.deltix.data.uniswap;

import com.epam.deltix.data.connectors.commons.Util;
import com.epam.deltix.data.connectors.commons.json.JsonArray;
import com.epam.deltix.data.connectors.commons.json.JsonObject;
import com.epam.deltix.timebase.messages.SchemaElement;

public class Transaction implements Updatable {
    private String id;
    private String blockNumber;
    private String timestamp;
    private String gasUsed;
    private String gasPrice;
    private String mintIds;
    private String burnIds;
    private String swapIds;
    private String flashedIds;
    private String collectsIds;

    @Override
    public String getTbSymbol() {
        return "transaction";
    }

    @SchemaElement()
    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public boolean updateId(final String id) {
        if (Util.equals(this.id, id)) {
            return false;
        }
        this.id = id;
        return true;
    }

    @SchemaElement()
    public String getBlockNumber() {
        return blockNumber;
    }

    public void setBlockNumber(final String blockNumber) {
        this.blockNumber = blockNumber;
    }

    public boolean updateBlockNumber(final String blockNumber) {
        if (Util.equals(this.blockNumber, blockNumber)) {
            return false;
        }
        this.blockNumber = blockNumber;
        return true;
    }

    @SchemaElement()
    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(final String timestamp) {
        this.timestamp = timestamp;
    }

    public boolean updateTimestamp(final String timestamp) {
        if (Util.equals(this.timestamp, timestamp)) {
            return false;
        }
        this.timestamp = timestamp;
        return true;
    }

    @SchemaElement()
    public String getGasUsed() {
        return gasUsed;
    }

    public void setGasUsed(final String gasUsed) {
        this.gasUsed = gasUsed;
    }

    public boolean updateGasUsed(final String gasUsed) {
        if (Util.equals(this.gasUsed, gasUsed)) {
            return false;
        }
        this.gasUsed = gasUsed;
        return true;
    }

    @SchemaElement()
    public String getGasPrice() {
        return gasPrice;
    }

    public void setGasPrice(final String gasPrice) {
        this.gasPrice = gasPrice;
    }

    public boolean updateGasPrice(final String gasPrice) {
        if (Util.equals(this.gasPrice, gasPrice)) {
            return false;
        }
        this.gasPrice = gasPrice;
        return true;
    }

    @SchemaElement()
    public String getMintIds() {
        return mintIds;
    }

    public void setMintIds(final String mintIds) {
        this.mintIds = mintIds;
    }

    public boolean updateMintIds(final String mintIds) {
        if (Util.equals(this.mintIds, mintIds)) {
            return false;
        }
        this.mintIds = mintIds;
        return true;
    }

    @SchemaElement()
    public String getBurnIds() {
        return burnIds;
    }

    public void setBurnIds(final String burnIds) {
        this.burnIds = burnIds;
    }

    public boolean updateBurnIds(final String burnIds) {
        if (Util.equals(this.burnIds, burnIds)) {
            return false;
        }
        this.burnIds = burnIds;
        return true;
    }

    @SchemaElement()
    public String getSwapIds() {
        return swapIds;
    }

    public void setSwapIds(final String swapIds) {
        this.swapIds = swapIds;
    }

    public boolean updateSwapIds(final String swapIds) {
        if (Util.equals(this.swapIds, swapIds)) {
            return false;
        }
        this.swapIds = swapIds;
        return true;
    }

    @SchemaElement()
    public String getFlashedIds() {
        return flashedIds;
    }

    public void setFlashedIds(final String flashedIds) {
        this.flashedIds = flashedIds;
    }

    public boolean updateFlashedIds(final String flashedIds) {
        if (Util.equals(this.flashedIds, flashedIds)) {
            return false;
        }
        this.flashedIds = flashedIds;
        return true;
    }

    @SchemaElement()
    public String getCollectsIds() {
        return collectsIds;
    }

    public void setCollectsIds(final String collectsIds) {
        this.collectsIds = collectsIds;
    }

    public boolean updateCollectsIds(final String collectsIds) {
        if (Util.equals(this.collectsIds, collectsIds)) {
            return false;
        }
        this.collectsIds = collectsIds;
        return true;
    }

    @Override
    public boolean update(final JsonObject from) {
        boolean result = false;
        result |= updateId(from.getString("id"));
        result |= updateBlockNumber(from.getString("blockNumber"));
        result |= updateTimestamp(from.getString("timestamp"));
        result |= updateGasUsed(from.getString("gasUsed"));
        result |= updateGasPrice(from.getString("gasPrice"));

        //mints
        final JsonArray mints = from.getArray("mints");
        result |= updateMintIds(parseJsonArray(mints));

        //burns
        final JsonArray burns = from.getArray("burns");
        result |= updateBurnIds(parseJsonArray(burns));

        //swaps
        final JsonArray swaps = from.getArray("swaps");
        result |= updateSwapIds(parseJsonArray(swaps));

        //collects
        final JsonArray flashed = from.getArray("flashed");
        result |= updateFlashedIds(parseJsonArray(flashed));

        //collects
        final JsonArray collects = from.getArray("collects");
        result |= updateCollectsIds(parseJsonArray(collects));

        return result;
    }

    private String parseJsonArray(JsonArray array) {
        StringBuilder itemIdsList = new StringBuilder();
        if (array != null) {
            for (int i = 0; i < array.size(); i++) {
                final JsonObject item = array.getObject(i);
                String id = item.getString("id");
                itemIdsList.append(id);
                if (i != array.size() - 1) {
                    itemIdsList.append(",");
                }
            }
        }

        return itemIdsList.toString();
    }
}

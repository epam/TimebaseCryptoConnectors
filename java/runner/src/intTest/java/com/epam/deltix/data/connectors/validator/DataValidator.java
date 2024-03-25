package com.epam.deltix.data.connectors.validator;

import com.epam.deltix.containers.AlphanumericUtils;
import com.epam.deltix.containers.DateTimeHelper;
import com.epam.deltix.containers.MutableString;
import com.epam.deltix.containers.interfaces.Loggable;
import com.epam.deltix.timebase.messages.TypeConstants;
import com.epam.deltix.timebase.messages.universal.PackageHeaderInfo;

public interface DataValidator extends Loggable {

    void sendPackage(PackageHeaderInfo headerInfo);

    static MutableString generateLogMessage(MutableString buffer, CharSequence symbol,
                                            long exchangeId, long timestamp, CharSequence quoteId, short level,
                                            CharSequence message) {
        if (buffer == null) buffer = new MutableString();
        buffer.clear();
        if (symbol != "") buffer.append("Symbol: ").append(symbol).append(" ");
        if (exchangeId != TypeConstants.EXCHANGE_NULL) {
            buffer.append("Exchange Id: ");
            if ((int)(exchangeId >>> 60) != 0) {
                buffer = AlphanumericUtils.appendAlphanumeric(buffer, exchangeId).append(" ");
            }
            else
                buffer.append(exchangeId).append(" ");
        }
        if (timestamp >= 0) buffer.append("Time: ").append(timestamp).append(" ");
        DateTimeHelper.append(buffer, timestamp);
        buffer.append(' ');
        if (quoteId != null) buffer.append("QuoteId: ").append(quoteId).append(" ");
        if (level >= 0) buffer.append("Level: ").append(level).append(" ");
        if (message != "") buffer.append("Message: ").append(message).append(" ");
        return buffer;
    }


}

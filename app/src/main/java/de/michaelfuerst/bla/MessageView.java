package de.michaelfuerst.bla;

import android.view.View;
import android.widget.LinearLayout;

/**
 * @author Michael
 * @version 1.0
 */
public class MessageView {
    public static View createMessage(final Chat parent, ChatMessage m, String user) {
        final LinearLayout q = (LinearLayout) LinearLayout.inflate(parent, R.layout.view_message_left, null);

        return q;
    }
}

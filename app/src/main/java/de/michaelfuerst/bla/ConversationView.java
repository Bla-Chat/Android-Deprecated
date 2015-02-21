/**
 * 
 */

package de.michaelfuerst.bla;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * @author Michael
 * 
 */
public class ConversationView {

	private static final int PROFILE_IMAGE_SIZE = 64;
	
	public static View createChat(final Conversations parent, ConversationViewData d, String user) {
        final LinearLayout q = (LinearLayout)LinearLayout.inflate(parent, R.layout.view_chat, null);
		final String name = d.name;
		final String nick = d.nick;
		createChilds(q, parent, name, nick, user);
        q.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                q.setBackgroundColor(Color.rgb(245, 245, 255));
                BlaNetwork networkAdapter = BlaNetwork.getInstance();
                if (networkAdapter != null) {
                    networkAdapter.unmark(nick);
                    // detachAllViewsFromParent();
                    // createChilds(parent, name, nick);
                    if (q.getContext() != null) {
                        Intent intent = new Intent(q.getContext(), Chat.class);
                        intent.putExtra("chatnick", nick);
                        intent.putExtra("chatname", name);
                        q.getContext().startActivity(intent);
                    }
                }
                new AsyncTask<Void, Void, Void>() {

                    @Override
                    protected Void doInBackground(Void... params) {
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void v) {
                        q.setBackgroundColor(0);
                    }
                }.execute();
            }
        });
        return q;
	}

	private static void createChilds(LinearLayout q, Context parent, String name, String nick,
			String user) {
		String s[] = nick.split(",");
		String localNick = nick;
		if (s.length == 2) {
			if (s[1].equals(user)) {
				localNick = s[0];
			} else {
				localNick = s[1];
			}
		}
		if (localNick.equals(nick)) {
			localNick = nick.replaceAll(",", "-");
		}

        ImageView iv = (ImageView)(q.findViewById(R.id.chatImage));
        String path = BlaNetwork.getServer(parent)
                + "/imgs/profile_" + localNick + ".png";
        Drawable preload = LocalResourceManager.getDrawable(parent, BlaNetwork.getServer(parent) + "/imgs/user.png",
                PROFILE_IMAGE_SIZE, 1);
        Drawable image = LocalResourceManager.getDrawable(parent, path,
                PROFILE_IMAGE_SIZE, 1);
        if (image != null) {
            iv.setImageDrawable(image);
        } else if (preload != null) {
            iv.setImageDrawable(preload);
        }

        TextView t = (TextView)q.findViewById(R.id.chatName);
		t.setText(name);

		String text = BlaNetwork.getInstance().getLastMessage(nick);
        TextView t2 = (TextView)q.findViewById(R.id.chatMessage);
		if (BlaNetwork.getInstance().getMarkedConversations().contains(nick)) {
			t2.setTextColor(Color.rgb(130, 200, 130));
		} else {
			t2.setTextColor(Color.LTGRAY);
		}
		if (!text.equals(" "))
			t2.setText("> " + text);
	}
}

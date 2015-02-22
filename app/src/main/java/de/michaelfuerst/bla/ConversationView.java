/**
 * 
 */

package de.michaelfuerst.bla;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * @author Michael
 * 
 */
public class ConversationView {

	private static final int PROFILE_IMAGE_SIZE = 64;
	
	public static View createChat(LocalResourceManager manager, final Conversations parent, ConversationViewData d, String user) {
        final LinearLayout q = (LinearLayout)LinearLayout.inflate(parent, R.layout.view_chat, null);
		final String name = d.name;
		final String nick = d.nick;
		createChilds(manager, q, parent, name, nick, user);
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

	private static void createChilds(LocalResourceManager manager, LinearLayout q, Context parent, String name, String nick,
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
        Drawable preload = manager.getDrawable(parent, BlaNetwork.getServer(parent) + "/imgs/user.png",
                PROFILE_IMAGE_SIZE, 1);
        Drawable image = manager.getDrawable(parent, path,
                PROFILE_IMAGE_SIZE, 1);
        if (image != null) {
            iv.setImageDrawable(image);
        } else if (preload != null) {
            iv.setImageDrawable(preload);
        }

        TextView t = (TextView)q.findViewById(R.id.chatName);
        t.setText(name);

		String text = BlaNetwork.getInstance().getLastMessage(nick);
        String time = BlaNetwork.getInstance().getLastMessageTime(nick);
        TextView t2 = (TextView)q.findViewById(R.id.chatMessage);
        TextView t3 = (TextView)q.findViewById(R.id.chatTime);

        if (time != null && !time.equals("")) {
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
            try {
                Date date = format.parse(time);

                Calendar yesterday = Calendar.getInstance();
                yesterday.add(Calendar.DATE, -1);

                Calendar week = Calendar.getInstance();
                week.add(Calendar.DATE, -7);

                Calendar year = Calendar.getInstance();
                year.add(Calendar.YEAR, -1);

                if (time.startsWith(new SimpleDateFormat("yyyy-MM-dd").format(new Date()))) {
                    // TODAY
                    DateFormat f = new SimpleDateFormat("HH:mm", Locale.GERMAN);
                    time = "Heute, " + f.format(date);
                } else if (time.startsWith(new SimpleDateFormat("yyyy-MM-dd").format(yesterday.getTime()))) {
                    // Yesterday
                    DateFormat f = new SimpleDateFormat("HH:mm", Locale.GERMAN);
                    time = "Gestern, " + f.format(date);
                } else if (date.after(week.getTime())) {
                    // Week
                    DateFormat f = new SimpleDateFormat("EEEE, HH:mm", Locale.GERMAN);
                    time = f.format(date);
                } else if (date.after(year.getTime())) {
                    // YEAR
                    DateFormat f = new SimpleDateFormat("d. MMMM", Locale.GERMAN);
                    time = f.format(date);
                } else {
                    DateFormat f = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN);
                    time = f.format(date);
                }

            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        t3.setText(time);

		if (BlaNetwork.getInstance().getMarkedConversations().contains(nick)) {
			t2.setTextColor(Color.rgb(130, 200, 130));
		} else {
			t2.setTextColor(Color.LTGRAY);
		}
		if (!text.equals(" "))
			t2.setText("> " + text);
	}
}

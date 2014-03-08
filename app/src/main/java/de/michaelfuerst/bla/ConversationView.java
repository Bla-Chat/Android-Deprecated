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
public class ConversationView extends LinearLayout {

	private static final int PROFILE_IMAGE_SIZE = 64;
	
	public ConversationView(final Conversations parent, ConversationViewData d, String user) {
		super(parent);
		final String name = d.name;
		final String nick = d.nick;
		createChilds(parent, name, nick, user);
		final ConversationView that = this;
		this.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				setBackgroundColor(Color.rgb(245, 245, 255));
				BlaNetwork networkAdapter = BlaNetwork.getInstance();
				if (networkAdapter != null) {
					networkAdapter.unmark(nick);
					// detachAllViewsFromParent();
					// createChilds(parent, name, nick);
                    if (getContext() != null) {
					    Intent intent = new Intent(getContext(), Chat.class);
					    intent.putExtra("chatnick", nick);
					    intent.putExtra("chatname", name);
					    getContext().startActivity(intent);
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
						that.setBackgroundColor(0);
					}
				}.execute();
			}
		});
	}

	private void createChilds(Context parent, String name, String nick,
			String user) {

		Resources r = parent.getResources();
		double px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1,
				r.getDisplayMetrics());
		int res = (int) (PROFILE_IMAGE_SIZE * px);
		
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
		RelativeLayout v = new RelativeLayout(parent);
		v.setMinimumHeight(res);
		v.setMinimumWidth(res);
		v.setGravity(Gravity.CLIP_HORIZONTAL);
		ImageView iv = getImageView(parent, BlaNetwork.getServer(parent)
				+ "/imgs/profile_" + localNick + ".png", PROFILE_IMAGE_SIZE, 1);
		if (iv != null) {
			//iv.setAdjustViewBounds(true);
			iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
			iv.setMaxHeight(res);
			iv.setMaxWidth(res);
			iv.setMinimumHeight(res);
			iv.setMinimumWidth(res);
			v.addView(iv);
			addView(v);
		}
		LinearLayout ll = new LinearLayout(parent);
		ll.setMinimumHeight(res);
		ll.setOrientation(VERTICAL);
		TextView t = new TextView(parent);
		String text = name;
		if (text.length() > 30) {
			text = text.substring(0, 27) + "...";
		}
		t.setText(text);
		t.setPadding(24, 16, 16, 0);
		ll.addView(t);
		TextView t2 = new TextView(parent);
		text = BlaNetwork.getInstance().getLastMessage(nick);
		if (text.length() > 30) {
			text = text.substring(0, 27) + "...";
		}
		if (BlaNetwork.getInstance().getMarkedConversations().contains(nick)) {
			t2.setTextColor(Color.rgb(130, 200, 130));
		} else {
			t2.setTextColor(Color.LTGRAY);
		}
		if (!text.equals(" "))
			t2.setText("> " + text);
		t2.setPadding(32, 0, 16, 0);
		ll.addView(t2);
		addView(ll);
		/*
		 * if
		 * (HangoutNetwork.getInstance().getMarkedConversations().contains(nick
		 * )) { iv = getImageView(parent, HangoutNetwork.HANGOUT_SERVER +
		 * "/imgs/marker.png");
		 * 
		 * v = new RelativeLayout (parent); v.setMinimumHeight(96);
		 * v.setMinimumWidth(96);
		 * 
		 * iv.setAdjustViewBounds(true); iv.setMinimumHeight(96);
		 * iv.setMinimumWidth(96); iv.setMaxHeight(96); iv.setMaxWidth(96);
		 * iv.setScaleType(ScaleType.CENTER_INSIDE); v.addView(iv);
		 * v.setGravity(Gravity.RIGHT); addView(v); }
		 */
		setPadding(5, 5, 5, 5);
	}

	static public ImageView getImageView(final Context ctx, final String path, final int size, final int buffer) {
		final AutoBufferingImageView iv = new AutoBufferingImageView(
				ctx, true);
		Drawable preload = LocalResourceManager.getDrawable(ctx, BlaNetwork.getServer(ctx) + "/imgs/user.png",
				size, buffer);
		Drawable image = LocalResourceManager.getDrawable(ctx, path,
				size, buffer);
		if (image != null) {
			iv.setImageDrawable(image);
			iv.setImage(path, size, buffer);
		} else if (preload != null) {
			iv.setImageDrawable(preload);
			iv.setImage(path, size, buffer);
		}
		return iv;
	}
}

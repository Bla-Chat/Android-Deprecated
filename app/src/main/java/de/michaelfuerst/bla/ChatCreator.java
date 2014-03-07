package de.michaelfuerst.bla;

import java.util.LinkedList;

import de.michaelfuerst.bla.R;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.support.v4.app.NavUtils;
import android.annotation.TargetApi;
import android.graphics.Color;
import android.os.Build;

public class ChatCreator extends Activity {
	private String[] friendNames;
	private String[] friendNicks;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final ChatCreator that = this;
		setContentView(R.layout.activity_chat_creator);

		Button b1 = (Button) findViewById(R.id.CancelOpen);
		b1.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				NavUtils.navigateUpFromSameTask(that);
			}
		});

		Button b2 = (Button) findViewById(R.id.OpenChat);
		b2.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				LinearLayout ll = (LinearLayout) findViewById(R.id.chatPartnerList);
				final LinkedList<String> participants = new LinkedList<String>();
				for (int i = 0; i < ll.getChildCount()/2; i++) {
					CheckBox c = (CheckBox) ((LinearLayout) ll.getChildAt(2*i)).getChildAt(0);
					if (c.isChecked()) {
						participants.add(friendNicks[i]);
					}
				}
				participants.add(BlaNetwork.getUser(that));
				new Thread() {
					@Override
					public void run() {
						BlaNetwork.getInstance().open(participants);
					}
				}.start();
				NavUtils.navigateUpFromSameTask(that);
			}
		});

		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				friendNames = BlaNetwork.getInstance().getContactNames();
				friendNicks = BlaNetwork.getInstance().getContactNicks();
				return null;
			}

			protected void onPostExecute(Void v) {
				setFriendList();
			}
		}.execute();

		setupActionBar();
	}

	private void setFriendList() {
		int size = 48;
		LinearLayout ll = (LinearLayout) findViewById(R.id.chatPartnerList);
		for (int i = 0; i < friendNames.length && i < friendNicks.length; i++) {
			String path = BlaNetwork.getServer(this) + "/profile_"+friendNicks[i]+".png";
			View iv = ConversationView.getImageView(this, path, size, 0);
			CheckBox c = new CheckBox(getBaseContext());
			TextView tv = new TextView(this);
			tv.setText(friendNames[i]);
			tv.setTextColor(Color.BLACK);
			tv.setPadding(0, 16, 0, 16);
			LinearLayout v = new LinearLayout(this);
			v.setOrientation(LinearLayout.HORIZONTAL);
			v.setMinimumHeight(size);
			v.addView(c);
			v.addView(iv);
			v.addView(tv);
			ll.addView(v);
			ll.addView(new Delimiter(this));
		}
	}

	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.chat_creator, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}

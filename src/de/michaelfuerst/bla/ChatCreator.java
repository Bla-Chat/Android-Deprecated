package de.michaelfuerst.bla;

import java.util.LinkedList;

import de.michaelfuerst.hangout.R;

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
				for (int i = 0; i < ll.getChildCount(); i++) {
					CheckBox c = (CheckBox) ll.getChildAt(i);
					if (c.isChecked()) {
						String tmp = c.getText() + "";
						tmp = tmp.substring(tmp.lastIndexOf("(") + 1,
								tmp.length() - 1);
						participants.add(tmp);
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
		LinearLayout ll = (LinearLayout) findViewById(R.id.chatPartnerList);
		for (int i = 0; i < friendNames.length && i < friendNicks.length; i++) {
			CheckBox c = new CheckBox(getBaseContext());
			c.setText(friendNames[i] + " (" + friendNicks[i] + ")");
			c.setTextColor(Color.BLACK);
			c.setPadding(0, 16, 0, 16);
			ll.addView(c);
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

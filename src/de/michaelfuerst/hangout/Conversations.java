package de.michaelfuerst.hangout;

import java.util.LinkedList;

import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.ScrollView;

public class Conversations extends Activity {

	private LinkedList<String> conversations = null;
	private HangoutNetwork networkAdapter = null;
	boolean waiting = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final Conversations that = this;

		networkAdapter = HangoutNetwork.getInstance();
		if (networkAdapter == null) {
			startService(new Intent(this, HangoutNetwork.class));
			new AsyncTask<Integer, Integer, Integer>() {
				@Override
				protected Integer doInBackground(Integer... params) {
					while (HangoutNetwork.getInstance() == null) {
						networkAdapter = null;
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					networkAdapter = HangoutNetwork.getInstance();
					// Sleep 1 second to give login time.
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					if (!networkAdapter.isRunning()) {
						LoginNetworkThread t = new LoginNetworkThread(that);
						t.start();
					}
					networkAdapter.setParent(that);
					conversations = networkAdapter.getConversations();

					networkAdapter.setActiveConversation(null);
					networkAdapter.updateConversations();
					return null;
				}

				@Override
				public void onPostExecute(Integer v) {
					updateData();
				}
			}.execute();
		} else {
			if (!networkAdapter.isRunning()) {
				LoginNetworkThread t = new LoginNetworkThread(this);
				t.start();
			}
			networkAdapter.setParent(this);
			conversations = networkAdapter.getConversations();
			networkAdapter.setActiveConversation(null);
		}
		setContentView(R.layout.activity_conversations);
		if (conversations != null) {
			if (conversations.isEmpty()) {
				SharedPreferences app_preferences = PreferenceManager
						.getDefaultSharedPreferences(this);

				String[] splits = app_preferences
						.getString("conversations", "").split(";");
				for (String s : splits) {
					String[] tmp = s.split("-");
					if (tmp.length == 3) {
						conversations.add(tmp[0]);
						if (networkAdapter != null) {
							networkAdapter.addConversationNick(tmp[1]);
						}
					}
				}
			}
			updateData();
		}
	}

	public void autoMark() {
		SharedPreferences app_preferences = PreferenceManager
				.getDefaultSharedPreferences(this);

		String[] splits = app_preferences.getString("conversations", "").split(
				";");
		for (String s : splits) {
			String[] tmp = s.split("-");
			if (tmp.length == 3) {
				if (networkAdapter != null) {
					if (tmp[2].equals("true")) {
						networkAdapter.mark(tmp[1]);
					}
				}
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.conversations, menu);
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
		case R.id.action_addFriend:
			// TODO
			return true;
		case R.id.action_newConversation:
			// TODO
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onPause() {
		if (networkAdapter != null) {
			networkAdapter.requestPause();
			networkAdapter.setParent(null);
		}
		super.onPause();
	}

	@Override
	protected void onResume() {
		if (networkAdapter != null) {
			networkAdapter.requestResumeLowFrequency();
			networkAdapter.setParent(this);
			ConversationInitThread t = new ConversationInitThread(this);
			t.execute();
		}
		super.onResume();
	}

	public void updateData() {
		if (networkAdapter != null) {
			autoMark();
			ScrollView scrollView = (ScrollView) findViewById(R.id.ScrollView1);
			LinearLayout ll = new LinearLayout(this);
			ll.setOrientation(LinearLayout.VERTICAL);
			conversations = networkAdapter.getConversations();
			for (int i = 0; i < conversations.size(); i++) {
				ll.addView(new ConversationView(this, conversations.get(i),
						networkAdapter.getConversationNickAt(i)));
				ll.addView(new Delimiter(this));
			}
			scrollView.removeAllViews();
			scrollView.addView(ll);
		}
	}

	@Override
	public void onStop() {
		saveConversations();
		LocalResourceManager.clear();
		super.onStop();
	}

	public void saveConversations() {
		if (networkAdapter != null) {
			String temp = "";
			conversations = networkAdapter.getConversations();
			for (int i = 0; i < conversations.size(); i++) {
				temp += conversations.get(i);
				temp += "-" + networkAdapter.getConversationNickAt(i);
				if (networkAdapter.getMarkedConversations().contains(
						networkAdapter.getConversationNickAt(i))) {
					temp += "-true;";
				} else {
					temp += "-false;";
				}
			}

			SharedPreferences app_preferences = PreferenceManager
					.getDefaultSharedPreferences(this);
			SharedPreferences.Editor editor = app_preferences.edit();
			editor.putString("conversations", temp);
			editor.commit();
		}
	}
}

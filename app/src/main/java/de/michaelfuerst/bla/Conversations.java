package de.michaelfuerst.bla;

import java.util.LinkedList;
import java.util.List;

import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v4.view.MenuCompat;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

public class Conversations extends Activity implements MessageListener {

	private BlaNetwork networkAdapter = null;
	private boolean isAlive = false;
	private final List<ConversationViewData> conversationData = new LinkedList<ConversationViewData>();
    private LocalResourceManager manager = new LocalResourceManager();

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d("Created", "Conversations created!");
		final Conversations that = this;
		conversationData.clear();
		conversationData.addAll(loadAsConversations(this));
		isAlive = true;

		networkAdapter = BlaNetwork.getInstance();
		if (networkAdapter == null) {
			startService(new Intent(this, BlaNetwork.class));
			
			new Thread() {
				@Override
				public void run() {
					networkAdapter = BlaNetwork.getInstanceInitialized();

                    if (!networkAdapter.canLogin() && !networkAdapter.isRunning()) {
                        Intent intent = new Intent(that.getApplicationContext(),
                                Login.class);
                        that.startActivity(intent);
                    }

					networkAdapter.setActiveConversation(null);
					networkAdapter.updateConversations();
					networkAdapter.attachMessageListener(that);
				}
			}.start();
		} else {
			if (!networkAdapter.isRunning() && !networkAdapter.canLogin()) {
                if (!networkAdapter.canLogin() && !networkAdapter.isRunning()) {
                    Intent intent = new Intent(this.getApplicationContext(),
                            Login.class);
                    this.startActivity(intent);
                }
            }

			networkAdapter.setActiveConversation(null);
			networkAdapter.attachMessageListener(this);

            UpdateApp updater = new UpdateApp();
            updater.setContext(getApplicationContext());
            updater.execute(BlaNetwork.UPDATE_SERVER);

		}
		setContentView(R.layout.activity_conversations);
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.conversations, menu);
		MenuCompat.setShowAsAction(menu.findItem(R.id.action_newConversation),
				1);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final Conversations parent = this;
		int itemId = item.getItemId();
		if (itemId == android.R.id.home) {
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		} else if (itemId == R.id.action_addFriend) {
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			alert.setTitle("Add Friend");
			alert.setMessage("Enter the nickname of your friend");
			// Set an EditText view to get user input
			final EditText input = new EditText(this);
			alert.setView(input);
			alert.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
                            if (input.getText() == null) return;
							final String value = input.getText().toString();
							if (networkAdapter != null) {
								new AsyncTask<Void, Void, Void>() {
									@Override
									public Void doInBackground(Void... params) {
										networkAdapter.friend(value);
										return null;
									}
								}.execute();
								Toast.makeText(parent, "Add " + value,
										Toast.LENGTH_SHORT).show();
							}
						}
					});
			alert.setNegativeButton("Cancel",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							Toast.makeText(parent, "Canceled",
									Toast.LENGTH_SHORT).show();
						}
					});
			alert.show();
			return true;
		} else if (itemId == R.id.action_newConversation) {
			startActivity(new Intent(this, ChatCreator.class));
			return true;
		} else if (itemId == R.id.action_settings) {
            Intent intent = new Intent(getApplicationContext(),
                    SettingsActivity.class);
            startActivity(intent);
            return true;
        }
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onPause() {
		if (networkAdapter != null) {
            networkAdapter.setActivity(null);
			networkAdapter.requestPause();
			BlaWidget.updateWidgets();
		}

		// LocalResourceManager.clear();
		super.onPause();
	}

	@Override
	protected void onResume() {
		Log.d("Resume", "conversations");
		isAlive = true;
		updateData();
		if (networkAdapter != null) {
            networkAdapter.setActivity(this);
			networkAdapter.requestResumeLowFrequency();
			ConversationInitThread t = new ConversationInitThread(this);
			t.execute();
		}
		super.onResume();
	}

	public void updateData() {
		if (isAlive) {
			Log.d("StartUpdate", "conversations");
			final Conversations that = this;
			new AsyncTask<Void, Void, LinearLayout>() {
				@Override
				protected LinearLayout doInBackground(Void... params) {
					Log.d("Update", "Updating conversation view.");
                    networkAdapter = BlaNetwork.getInstanceInitialized();
                    if (!networkAdapter.canLogin()) return null;

					conversationData.clear();
					conversationData.addAll(loadAsConversations(that));
					LinearLayout ll = new LinearLayout(that);
					ll.setOrientation(LinearLayout.VERTICAL);
                    for (final ConversationViewData aConversationData : conversationData) {
                        ll.addView(ConversationView.createChat(manager, that, aConversationData, BlaNetwork.getUser(that)));
                        ll.addView(new Delimiter(that));
                    }
					Log.d("Update2", "Updating conversation view.");
					return ll;
				}

				protected void onPostExecute(LinearLayout ll) {
                    if (ll == null) return;
					ScrollView scrollView = (ScrollView) findViewById(R.id.ScrollView1);
					scrollView.removeAllViews();
					scrollView.addView(ll);
					Log.d("Update3", "Updating conversation view.");
				}
			}.execute();
			Log.d("UpdateF", "Updating invoked.");
		}
	}

    @Override
	public void onStop() {
		isAlive = false;
		if (networkAdapter != null) {
			networkAdapter.detachMessageListener(this);
		}
		super.onStop();
	}

	public static void saveAsConversations(
			List<ConversationViewData> conversations, Context ctx) {
		Log.d("ConversationSave", "size:"+conversations.size());
		String temp = "";
        for (ConversationViewData current : conversations) {
            temp += current.name;
            temp += BlaNetwork.SEPARATOR + current.nick;
            if (current.marked) {
                temp += BlaNetwork.SEPARATOR + "true";
            } else {
                temp += BlaNetwork.SEPARATOR + "false";
            }
            temp += BlaNetwork.SEPARATOR + current.lastMessage;
            temp += BlaNetwork.SEPARATOR + current.lastMessageTime;
            temp += BlaNetwork.EOL;
        }

		SharedPreferences app_preferences = PreferenceManager
				.getDefaultSharedPreferences(ctx);
		SharedPreferences.Editor editor = app_preferences.edit();
		editor.putString("conversations", temp);
		editor.commit();
	}

	public static List<ConversationViewData> loadAsConversations(Context ctx) {
		Log.d("Load", "Load conversations");
		List<ConversationViewData> result = new LinkedList<ConversationViewData>();
		SharedPreferences app_preferences;
        app_preferences = PreferenceManager.getDefaultSharedPreferences(ctx);

		String[] lines = app_preferences.getString("conversations", "").split(
				BlaNetwork.EOL);

		for (String s : lines) {
			String[] splits = s.split(BlaNetwork.SEPARATOR);
			if (splits.length == BlaNetwork.CONVERSATION_PARAMETERS) {
				ConversationViewData tmp = new ConversationViewData();
				tmp.name = splits[0];
				tmp.nick = splits[1];
				tmp.marked = splits[2].equals("true");
				tmp.lastMessage = splits[3];
                tmp.lastMessageTime = splits[4];
				result.add(tmp);
			}
		}
		return result;
	}

	@Override
	public void onMessageReceived(String message, String conversation) {
		Log.d("Conversations", "message");
		updateData();
	}

	@Override
	public String toString() {
		return "Conversations->" + super.toString();
	}
}

package de.michaelfuerst.bla;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import de.michaelfuerst.bla.R;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.NavUtils;
import android.support.v4.view.MenuCompat;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

public class Conversations extends Activity implements MessageListener {

	private static final int IMAGE_RESULT = 0;
	private BlaNetwork networkAdapter = null;
	boolean waiting = false;
	private boolean isAlive = false;
	private final List<ConversationViewData> conversationData = new LinkedList<ConversationViewData>();
	private static String tmp_image = Environment.getExternalStorageDirectory()
			+ "/Pictures/BlaChat/tmp.png";

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
					synchronized (BlaNetwork.class) {
						while (BlaNetwork.getInstance() == null) {
							networkAdapter = null;
							try {
								BlaNetwork.class.wait();
							} catch (InterruptedException e) {
								break;
							}
						}
					}
					networkAdapter = BlaNetwork.getInstance();
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

					networkAdapter.setActiveConversation(null);
					networkAdapter.updateConversations();
					networkAdapter.attachMessageListener(that);
				}
			}.start();
		} else {
			if (!networkAdapter.isRunning()) {
				LoginNetworkThread t = new LoginNetworkThread(this);
				t.start();
			}

			networkAdapter.setActiveConversation(null);
			networkAdapter.attachMessageListener(this);
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
		} else if (itemId == R.id.action_setProfileImage) {
			Intent pickIntent = new Intent();
			pickIntent.setType("image/*");
			pickIntent.setAction(Intent.ACTION_GET_CONTENT);
			Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT,
					Uri.fromFile(new File(tmp_image)));
			String pickTitle = "Select or take a new Picture"; // Or get from
			// strings.xml
			Intent chooserIntent = Intent.createChooser(pickIntent, pickTitle);
			chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS,
					new Intent[] { takePhotoIntent });
			startActivityForResult(chooserIntent, IMAGE_RESULT);
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
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == IMAGE_RESULT) {
			if (resultCode == Activity.RESULT_OK) {
				if (data != null && data.getData() != null) {
					final Conversations that = this;
					Uri _uri = data.getData();

					// User had pick an image.
					Cursor cursor = getContentResolver()
							.query(_uri,
									new String[] { android.provider.MediaStore.Images.ImageColumns.DATA },
									null, null, null);
					cursor.moveToFirst();

					// Link to the image
					final String imageFilePath = cursor.getString(0);
					cursor.close();

					Toast.makeText(this, "Uploading image", Toast.LENGTH_LONG)
							.show();
					Log.d("Chat", "Starting image upload");
					new AsyncTask<Void, Void, Void>() {
						@Override
						protected Void doInBackground(Void... params) {
							BitmapFactory.Options options = new BitmapFactory.Options();
							options.inPreferredConfig = Bitmap.Config.ARGB_8888;
							options.inSampleSize = 2;
							Bitmap bmp = BitmapFactory.decodeFile(
									imageFilePath, options);
							if (bmp != null) {
								networkAdapter.setImage(bmp);
							}
							return null;
						}

						@Override
						protected void onPostExecute(Void v) {
							Toast.makeText(that, "Uploaded image",
									Toast.LENGTH_LONG).show();
							Log.d("Chat", "Done image upload");
						}
					}.execute();
				} else {
					final String imageFilePath = tmp_image;
					Toast.makeText(this, "Uploading image", Toast.LENGTH_LONG)
							.show();
					final Conversations that = this;
					Log.d("Chat", "Starting image upload");
					new AsyncTask<Void, Void, Void>() {
						@Override
						protected Void doInBackground(Void... params) {
							BitmapFactory.Options options = new BitmapFactory.Options();
							options.inPreferredConfig = Bitmap.Config.ARGB_8888;
							options.inSampleSize = 2;
							Bitmap bmp = BitmapFactory.decodeFile(
									imageFilePath, options);
							if (bmp != null) {
								networkAdapter.setImage(bmp);
							}
							return null;
						}

						@Override
						protected void onPostExecute(Void v) {
							Toast.makeText(that, "Uploaded image",
									Toast.LENGTH_LONG).show();
							Log.d("Chat", "Done image upload");
						}
					}.execute();
				}
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onPause() {
		if (networkAdapter != null) {
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
					conversationData.clear();
					conversationData.addAll(loadAsConversations(that));
					LinearLayout ll = new LinearLayout(that);
					ll.setOrientation(LinearLayout.VERTICAL);
					for (int i = 0; i < conversationData.size(); i++) {
						ll.addView(new ConversationView(that, conversationData
								.get(i), BlaNetwork.getUser(that)));
						ll.addView(new Delimiter(that));
					}
					Log.d("Update2", "Updating conversation view.");
					return ll;
				}

				protected void onPostExecute(LinearLayout ll) {
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
		LocalResourceManager.clear(1);
		if (networkAdapter != null) {
			networkAdapter.detachMessageListener(this);
		}
		super.onStop();
	}

	public static void saveAsConversations(
			List<ConversationViewData> conversations, Context ctx) {
		Log.d("ConversationSave", "size:"+conversations.size());
		String temp = "";
		for (int i = 0; i < conversations.size(); i++) {
			ConversationViewData current = conversations.get(i);
			temp += current.name;
			temp += BlaNetwork.SEPERATOR + current.nick;
			if (current.marked) {
				temp += BlaNetwork.SEPERATOR + "true";
			} else {
				temp += BlaNetwork.SEPERATOR + "false";
			}
			temp += BlaNetwork.SEPERATOR + current.lastMessage;
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
		SharedPreferences app_preferences = null;
		try {
			app_preferences = PreferenceManager
					.getDefaultSharedPreferences(ctx);

		} catch (NullPointerException e) {
			Log.d("NP", "error:");
			return result;
		}

		String[] lines = app_preferences.getString("conversations", "").split(
				BlaNetwork.EOL);

		for (String s : lines) {
			String[] splits = s.split(BlaNetwork.SEPERATOR);
			if (splits.length == BlaNetwork.CONVERSATION_PARAMETERS) {
				ConversationViewData tmp = new ConversationViewData();
				tmp.name = splits[0];
				tmp.nick = splits[1];
				tmp.marked = splits[2].equals("true");
				tmp.lastMessage = splits[3];
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

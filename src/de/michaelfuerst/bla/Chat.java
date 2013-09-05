package de.michaelfuerst.bla;

import java.io.File;

import de.michaelfuerst.hangout.R;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.app.AlertDialog;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import android.widget.ImageView.ScaleType;
import android.support.v4.app.NavUtils;
import android.support.v4.view.MenuCompat;
import android.text.util.Linkify;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.MediaStore.Video;

public class Chat extends Activity {

	private static final int VIDEO_RESULT = 0;
	private static final int IMAGE_RESULT = 0;
	private static final int IMAGE_MAX_SIZE = 250;
	private String nick = null;
	private String name = null;
	private BlaNetwork networkAdapter = null;
	private boolean isAlive = false;
	private MessageListener ml = new MessageListener() {
		@Override
		public void onMessageReceived(String message, String conversation) {
			if (conversation.equals(nick)) {
				updateHistory();
			}
		}
	};
	private ChatMessage[] chatList;
	private boolean fotoReturn;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat);

		final Chat parent = this;
		EditText editText = (EditText) findViewById(R.id.editText1);
		editText.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				boolean handled = false;
				if (actionId == EditorInfo.IME_ACTION_SEND) {
					String message = ((EditText) findViewById(R.id.editText1))
							.getText().toString();
					if (!message.equals("")) {
						new SendMessageThread(parent, message, nick).start();
						((EditText) findViewById(R.id.editText1)).setText("");
						insertMessage(message);
					}
					handled = true;
				}
				return handled;
			}
		});

		// Show the Up button in the action bar.
		setupActionBar();
		Intent intent = getIntent();
		nick = intent.getStringExtra("chatnick");
		name = intent.getStringExtra("chatname");
		setTitle(name);

		networkAdapter = BlaNetwork.getInstance();
		if (networkAdapter == null) {
			startService(new Intent(this, BlaNetwork.class));
		}
	}

	private void initializeAsync() {
		final Chat that = this;
		/*
		 * if (fotoReturn) { return; }
		 */
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				if (!networkAdapter.isRunning()) {
					startService(new Intent(that, BlaNetwork.class));
					LoginNetworkThread t = new LoginNetworkThread(that);
					t.start();
				}

				chatList = loadChatAs(that, nick);
				return null;
			}

			@Override
			public void onPostExecute(Void a) {
				drawHistory(chatList);
				networkAdapter.attachMessageListener(ml);
				networkAdapter.setActiveConversation(nick);
				updateHistory();
			}

		}.execute();
	}

	public static ChatMessage[] loadChatAs(Context ctx, String nick) {
		SharedPreferences app_preferences = PreferenceManager
				.getDefaultSharedPreferences(ctx);

		String[] splits = app_preferences.getString("chatHistory_" + nick, "")
				.split(BlaNetwork.EOL);
		ChatMessage[] chatList = new ChatMessage[splits.length];
		for (int i = 0; i < splits.length; i++) {
			String[] sub = splits[i].split(BlaNetwork.SEPERATOR);
			if (sub.length == 4) {
				chatList[i] = new ChatMessage();
				chatList[i].author = sub[0];
				chatList[i].message = sub[1];
				chatList[i].sender = sub[2];
				chatList[i].time = sub[3];
			}
		}
		return chatList;
	}

	public static void saveChatAs(Context ctx, String nick,
			ChatMessage[] chatList) {
		String temp = "";
		for (int i = 0; i < chatList.length; i++) {
			temp += chatList[i].author + BlaNetwork.SEPERATOR
					+ chatList[i].message + BlaNetwork.SEPERATOR
					+ chatList[i].sender + BlaNetwork.SEPERATOR
					+ chatList[i].time + BlaNetwork.EOL;
		}

		SharedPreferences app_preferences = PreferenceManager
				.getDefaultSharedPreferences(ctx);
		SharedPreferences.Editor editor = app_preferences.edit();
		editor.putString("chatHistory_" + nick, temp);
		editor.commit();
	}

	private void insertMessage(String message) {
		for (int i = chatList.length - 1; i > 0; i--) {
			chatList[i] = chatList[i - 1];
		}
		ChatMessage m = new ChatMessage();
		m.author = BlaNetwork.getInstance().getUser();
		m.message = message;
		m.sender = "You";
		m.time = "uploading...";// "0000-00-00 00:00:00";
		if (chatList.length == 0) {
			chatList = new ChatMessage[1];
		}
		chatList[0] = m;
		isAlive = true;
		drawHistory(chatList);
		networkAdapter.setLastMessage(nick, message);
	}

	public void drawHistory(ChatMessage[] messages) {
		if (isAlive) {
			Log.d("Chat", "redrawing history");
			LinearLayout ll = (LinearLayout) findViewById(R.id.messages);
			ll.removeAllViews();

			ChatMessage c = null;
			if (messages.length == 0) {
				TextView textView = new TextView(this);
				textView.setText("No messages");
				textView.setPadding(48, 48, 48, 48);
				textView.setTextColor(Color.LTGRAY);
				ll.addView(textView);
			}
			for (int i = messages.length - 1; i >= 0; i--) {
				c = messages[i];
				if (c == null) {
					continue;
				}
				RelativeLayout outer = new RelativeLayout(this);
				LinearLayout view = new LinearLayout(this);
				view.setOrientation(LinearLayout.VERTICAL);
				TextView title = new TextView(this);
				view.addView(title);

				while (i >= 0 && c.author.equals(messages[i].author)
						&& !timeDif(c.time, messages[i].time)) {
					c = messages[i];
					TextView textView = new TextView(this);
					textView.setPadding(8, 0, 8, 8);
					textView.setAutoLinkMask(Linkify.ALL);
					c.message = c.message.replaceAll("&quot;", "\"");
					c.message = c.message.replaceAll("&lt;", "<");
					c.message = c.message.replaceAll("&gt;", ">");
					textView.setText(c.message);
					if (c.message.startsWith("#image")) {
						view.addView(getImageView(c.message.split(" ")[1]));
					} else if (c.message.startsWith("#video")) {
						// view.addView(getVideoView(c.message.split(" ")[1]));
						view.addView(textView);
						c.message = "video: " + c.message.split(" ")[1];
					} else if (c.message.startsWith("#file")) {
						// view.addView(getFileView(c.message.split(" ")[1]));
						view.addView(textView);
						c.message = "file: " + c.message.split(" ")[1];
					} else if (c.message.startsWith("#hangout")) {
						if (c.message.startsWith("#hangout on")) {
							view.addView(textView);
							c.message = "hangouts are not supported on this device";
						} else {
							view.addView(textView);
							c.message = "hangout is over";
						}
					} else {
						view.addView(textView);
					}
					i--;
				}
				i++;

				view.setPadding(4, 4, 4, 4);
				outer.addView(view);
				if (c.author.equals(BlaNetwork.getInstance().getUser())) {
					view.setBackgroundColor(Color.rgb(245, 255, 245));
					int r = (c.author.hashCode() & 0xFF0000) >> 16;
					int g = (c.author.hashCode() & 0x00FF00) >> 8;
					int b = (c.author.hashCode() & 0x0000FF) >> 0;
					title.setTextSize(11);
					title.setPadding(8, 8, 8, 4);
					title.setTextColor(Color.rgb(r / 2, g / 2, b / 2));
					outer.setGravity(Gravity.RIGHT);
					outer.setPadding(48, 8, 4, 8);
					title.setText("You (" + c.time + ")");
				} else {
					view.setBackgroundColor(Color.rgb(245, 245, 255));
					int r = (c.author.hashCode() & 0xFF0000) >> 16;
					int g = (c.author.hashCode() & 0x00FF00) >> 8;
					int b = (c.author.hashCode() & 0x0000FF) >> 0;
					title.setTextSize(11);
					title.setPadding(8, 8, 8, 4);
					title.setTextColor(Color.rgb(r / 2, g / 2, b / 2));
					outer.setPadding(4, 8, 48, 8);
					title.setText(c.sender + " (" + c.time + ")");
				}
				ll.addView(outer);
			}
		}

		final ScrollView sc = (ScrollView) findViewById(R.id.scrollView1);
		sc.post(new Runnable() {

			@Override
			public void run() {
				sc.fullScroll(ScrollView.FOCUS_DOWN);
			}
		});
		setChatList(messages);
	}

	/*
	 * private View getFileView(String path) { // TODO Auto-generated method
	 * stub return null; }
	 * 
	 * private View getVideoView(String path) { VideoView vv = new
	 * VideoView(this); vv.setMediaController(new MediaController(this));
	 * vv.setVideoURI(Uri.parse(path)); vv.start(); return vv; }
	 */

	private boolean timeDif(String time, String time2) {
		if (time.equals("uploading...") || time2.equals("uploading...")) {
			if (!time.equals("uploading...") || !time2.equals("uploading...")) {
				return true;
			} else {
				return false;
			}
		}

		String tmp = time.substring(time.length() - 5, time.length() - 3);
		int t1 = Integer.parseInt(tmp);

		tmp = time2.substring(time2.length() - 5, time2.length() - 3);
		int t2 = Integer.parseInt(tmp);

		time = time.substring(0, time.length() - 6);
		time2 = time2.substring(0, time2.length() - 6);

		return time.equals(time2) ? Math.abs(t1 - t2) > 2 : true;
	}

	private View getImageView(final String path) {
		LinearLayout parent = (LinearLayout) findViewById(R.id.messages);
		final AutoBufferingImageView iv = new AutoBufferingImageView(this);
		String preFile = path.split("/")[path.split("/").length - 1];
		final String filename = Environment.getExternalStorageDirectory()
				+ "/Pictures/BlaChat/" + preFile.split("\\.")[0] + ".png";
		iv.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_UP) {
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setDataAndType(Uri.parse("file://" + filename),
							"image/*");
					startActivity(intent);
				}
				return true;
			}
		});
		iv.setMaxHeight(IMAGE_MAX_SIZE);
		iv.setMaxWidth(IMAGE_MAX_SIZE);
		iv.setScaleType(ScaleType.CENTER_INSIDE);
		Drawable image = LocalResourceManager.getDrawable(this, filename,
				IMAGE_MAX_SIZE, 0);
		if (image != null) {
			iv.setImageDrawable(image);
		} else {
			iv.setMinimumHeight(IMAGE_MAX_SIZE);
			iv.setMinimumWidth(IMAGE_MAX_SIZE);
			iv.setBackgroundColor(Color.LTGRAY);
		}
		iv.setImage(path, IMAGE_MAX_SIZE, 0);
		return iv;
	}

	protected void updateHistory() {
		new ChatHistoryThread().execute(this, nick);
		networkAdapter.requestResume();
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

	@SuppressWarnings("deprecation")
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.chat, menu);
		MenuCompat.setShowAsAction(
				menu.findItem(R.id.action_addToConversation), 1);
		MenuCompat.setShowAsAction(menu.findItem(R.id.action_addImage), 2);
		return true;
	}

	private static String tmp_image = Environment.getExternalStorageDirectory()
			+ "/Pictures/BlaChat/tmp.png";

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final Chat parent = this;
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		case R.id.action_addImage:
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
		case R.id.action_addVideo:
			Toast.makeText(this, "Upcoming feature", Toast.LENGTH_SHORT).show();
			// startActivityForResult(new
			// Intent(MediaStore.ACTION_VIDEO_CAPTURE),
			// VIDEO_RESULT);
			return true;
		case R.id.action_rename:
			AlertDialog.Builder alert = new AlertDialog.Builder(this);

			alert.setTitle("Rename Conversation");
			alert.setMessage("The new name for your conversation");

			// Set an EditText view to get user input
			final EditText input = new EditText(this);
			alert.setView(input);

			alert.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							String value = input.getText().toString();
							// TODO
							Toast.makeText(parent,
									"Upcoming feature: Rename " + value,
									Toast.LENGTH_SHORT).show();
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
		case R.id.action_addToConversation:
			AlertDialog.Builder alert2 = new AlertDialog.Builder(this);

			alert2.setTitle("Add to Conversation");
			alert2.setMessage("The nick of your friend");

			// Set an EditText view to get user input
			final EditText input2 = new EditText(this);
			alert2.setView(input2);

			alert2.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							String value = input2.getText().toString();
							// TODO
							Toast.makeText(parent,
									"Upcoming feature: Add " + value,
									Toast.LENGTH_SHORT).show();
						}
					});

			alert2.setNegativeButton("Cancel",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							Toast.makeText(parent, "Canceled",
									Toast.LENGTH_SHORT).show();
						}
					});

			alert2.show();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == IMAGE_RESULT) {
			if (resultCode == Activity.RESULT_OK) {
				fotoReturn = true;
				if (data != null && data.getData() != null) {
					final Chat that = this;
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
					insertMessage("#image " + imageFilePath);
					new AsyncTask<Void, Void, Void>() {
						@Override
						protected Void doInBackground(Void... params) {
							BitmapFactory.Options options = new BitmapFactory.Options();
							options.inPreferredConfig = Bitmap.Config.ARGB_8888;
							options.inSampleSize = 2;
							Bitmap bmp = BitmapFactory.decodeFile(
									imageFilePath, options);
							if (bmp != null) {
								networkAdapter.send(bmp, nick);
							}
							fotoReturn = false;
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
					final Chat that = this;
					Log.d("Chat", "Starting image upload");
					insertMessage("#image " + imageFilePath);
					new AsyncTask<Void, Void, Void>() {
						@Override
						protected Void doInBackground(Void... params) {
							BitmapFactory.Options options = new BitmapFactory.Options();
							options.inPreferredConfig = Bitmap.Config.ARGB_8888;
							options.inSampleSize = 2;
							Bitmap bmp = BitmapFactory.decodeFile(
									imageFilePath, options);
							if (bmp != null) {
								networkAdapter.send(bmp, nick);
							}
							fotoReturn = false;
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
		} else if (requestCode == VIDEO_RESULT) {
			if (resultCode == Activity.RESULT_OK) {
				Toast.makeText(this, "Uploading video", Toast.LENGTH_LONG)
						.show();
				Bundle b = data.getExtras();
				Video vid = (Video) b.get("data");
				if (vid != null) {
					networkAdapter.send(vid, nick);
				}
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onPause() {
		fotoReturn = false;
		isAlive = false;
		if (networkAdapter != null) {
			networkAdapter.requestPause();
			BlaWidget.updateWidgets();
		}

		if (chatList != null) {
			saveChatAs(this, nick, chatList);
		}

		// LocalResourceManager.clear();
		super.onPause();
	}

	@Override
	protected void onResume() {
		isAlive = true;
		initializeAsync();
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				synchronized (BlaNetwork.class) {
					while (networkAdapter == null) {
						try {
							BlaNetwork.class.wait();
						} catch (InterruptedException e) {
							return null;
						}
						networkAdapter = BlaNetwork.getInstance();
					}
				}
				networkAdapter.requestResume();
				networkAdapter.unmark(nick);
				return null;
			}

		}.execute();
		super.onResume();
	}

	@Override
	public void onStop() {
		LocalResourceManager.clear(0);

		super.onStop();
	}

	public void setChatList(ChatMessage[] result) {
		chatList = result;
		if (result.length > 0 && result[0] != null) {
			networkAdapter.setLastMessage(nick, result[0].message);
		}
	}
}

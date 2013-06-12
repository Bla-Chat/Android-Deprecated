package de.michaelfuerst.hangout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.support.v4.app.NavUtils;
import android.text.util.Linkify;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
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
	private HangoutNetwork networkAdapter = null;
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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat);

		// Show the Up button in the action bar.
		setupActionBar();
		Intent intent = getIntent();
		nick = intent.getStringExtra("chatnick");
		name = intent.getStringExtra("chatname");
		setTitle(name);

		networkAdapter = HangoutNetwork.getInstance();
		if (networkAdapter == null) {
			startService(new Intent(this, HangoutNetwork.class));
		}
	}

	private void initializeAsync() {
		final Chat that = this;
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				while (networkAdapter == null) {
					networkAdapter = HangoutNetwork.getInstance();
					try {
						Thread.sleep(16);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				if (!networkAdapter.isRunning()) {
					startService(new Intent(that, HangoutNetwork.class));
					LoginNetworkThread t = new LoginNetworkThread(that);
					t.start();
				}

				SharedPreferences app_preferences = PreferenceManager
						.getDefaultSharedPreferences(that);

				String[] splits = app_preferences.getString(
						"chatHistory_" + nick, "").split("ﺿ");
				chatList = new ChatMessage[splits.length];
				for (int i = 0; i < splits.length; i++) {
					String[] sub = splits[i].split("◘");
					if (sub.length == 4) {
						chatList[i] = new ChatMessage();
						chatList[i].author = sub[0];
						chatList[i].message = sub[1];
						chatList[i].sender = sub[2];
						chatList[i].time = sub[3];
					}
				}
				return null;
			}

			@Override
			public void onPostExecute(Void a) {
				drawHistory(chatList);

				final Button button = (Button) findViewById(R.id.button1);
				final Chat parent = that;
				button.setOnTouchListener(new View.OnTouchListener() {

					@Override
					public boolean onTouch(View arg0, MotionEvent arg1) {
						networkAdapter.requestResume();
						if (arg0 == button) {
							String message = ((EditText) findViewById(R.id.editText1))
									.getText().toString();
							if (!message.equals("")) {
								new SendMessageThread(parent, message, nick)
										.start();
								((EditText) findViewById(R.id.editText1))
										.setText("");
								insertMessage(message);
							}
							return true;
						}
						return false;
					}
				});

				networkAdapter.attachMessageListener(ml);
				networkAdapter.setActiveConversation(nick);
				updateHistory();
			}

		}.execute();
	}

	private void insertMessage(String message) {
		for (int i = chatList.length - 1; i > 0; i--) {
			chatList[i] = chatList[i - 1];
		}
		ChatMessage m = new ChatMessage();
		m.author = HangoutNetwork.getInstance().getUser();
		m.message = message;
		m.sender = "You";
		m.time = "0000-00-00 00:00";
		chatList[0] = m;
		drawHistory(chatList);
	}

	public void drawHistory(ChatMessage[] messages) {
		if (isAlive) {
			LinearLayout ll = (LinearLayout) findViewById(R.id.messages);
			ll.removeAllViews();

			for (ChatMessage c : messages) {
				if (c == null) {
					continue;
				}
				RelativeLayout outer = new RelativeLayout(this);
				LinearLayout view = new LinearLayout(this);
				view.setOrientation(LinearLayout.VERTICAL);
				TextView textView = new TextView(this);
				TextView title = new TextView(this);
				view.addView(title);
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
					c.message = c.message.replaceAll("&quot;", "\"");
					c.message = c.message.replaceAll("&lt;", "<");
					c.message = c.message.replaceAll("&gt;", ">");
				}
				view.setPadding(4, 4, 4, 4);
				outer.addView(view);
				textView.setAutoLinkMask(Linkify.ALL);
				textView.setText(c.message);
				if (c.author.equals(HangoutNetwork.getInstance().getUser())) {
					view.setBackgroundColor(Color.rgb(245, 255, 245));
					outer.setGravity(Gravity.RIGHT);
					outer.setPadding(48, 4, 4, 4);
					title.setText("You (" + c.time + ")");
				} else {
					view.setBackgroundColor(Color.rgb(245, 245, 255));
					outer.setPadding(4, 4, 48, 4);
					title.setText(c.sender + " (" + c.time + ")");
				}
				textView.setAutoLinkMask(Linkify.ALL);
				ll.addView(outer);
			}
		}
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

	private View getImageView(final String path) {
		final ImageView iv = new ImageView(this);
		String preFile = path.split("/")[path.split("/").length - 1];
		final String filename = Environment.getExternalStorageDirectory()
				+ "/Pictures/BlaChat/" + preFile.split("\\.")[0] + ".png";
		iv.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_UP) {
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setDataAndType(Uri.parse("file://"+filename), "image/*");
					startActivity(intent);
				}
				return true;
			}
		});
		iv.setMaxHeight(IMAGE_MAX_SIZE);
		iv.setMaxWidth(IMAGE_MAX_SIZE);
		if (new File(filename).exists()) {
			iv.setImageDrawable(LocalResourceManager.getDrawable(this,
					filename, IMAGE_MAX_SIZE));
		}
		final Chat that = this;
		new AsyncTask<Object, Object, Drawable>() {

			@Override
			protected Drawable doInBackground(Object... arg0) {
				Drawable image = null;
				try {
					File sysPath = new File(
							Environment.getExternalStorageDirectory()
									+ "/Pictures/BlaChat");
					if (!sysPath.exists()) {
						sysPath.mkdirs();
					}
					String preFile = path.split("/")[path.split("/").length - 1];
					String filename = Environment.getExternalStorageDirectory()
							+ "/Pictures/BlaChat/" + preFile.split("\\.")[0] + ".png";
					if (!new File(filename).exists()) {
						// First create a new URL object
						URL url = new URL(path);

						// Next create a file, the example below will save to
						// the
						// SDCARD using JPEG format
						File file = new File(filename);

						// Next create a Bitmap object and download the image to
						// bitmap

						Bitmap bitmap = BitmapFactory.decodeStream(url
								.openStream());

						// Finally compress the bitmap, saving to the file
						// previously created
						bitmap.compress(CompressFormat.PNG, 100,
								new FileOutputStream(file));
						bitmap.recycle();
					}
					image = LocalResourceManager.getDrawable(that, filename,
							IMAGE_MAX_SIZE);
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				return image;
			}

			@Override
			protected void onPostExecute(Drawable image) {
				if (image != null)
					iv.setImageDrawable(image);
			}
		}.execute();
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.chat, menu);
		return true;
	}

	private static String tmp_image = Environment.getExternalStorageDirectory()
			+ "/Pictures/BlaChat/tmp.png";

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
		case R.id.action_addImage:
			Intent imageCaptureIntent = new Intent(
					MediaStore.ACTION_IMAGE_CAPTURE);
			imageCaptureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
					Uri.fromFile(new File(tmp_image)));
			startActivityForResult(imageCaptureIntent, IMAGE_RESULT);
			return true;
		case R.id.action_addVideo:
			startActivityForResult(new Intent(MediaStore.ACTION_VIDEO_CAPTURE),
					VIDEO_RESULT);
			return true;
		case R.id.action_addToConversation:
			// TODO
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == IMAGE_RESULT) {
			if (resultCode == Activity.RESULT_OK) {
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inPreferredConfig = Bitmap.Config.ARGB_8888;
				options.inSampleSize = 2;
				Bitmap bmp = BitmapFactory.decodeFile(tmp_image, options);
				if (bmp != null) {
					networkAdapter.send(bmp, nick);
				}
			}
		} else if (requestCode == VIDEO_RESULT) {
			if (resultCode == Activity.RESULT_OK) {
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
		isAlive = false;
		if (networkAdapter != null) {
			networkAdapter.requestPause();
		}

		LinearLayout ll = (LinearLayout) findViewById(R.id.messages);
		if (ll != null)
			ll.removeAllViews();
		
		LocalResourceManager.clear();
		super.onPause();
	}

	@Override
	protected void onResume() {
		isAlive = true;
		initializeAsync();
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				while (networkAdapter == null) {
					try {
						Thread.sleep(16);
					} catch (InterruptedException e) {
						e.printStackTrace();
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
		if (chatList != null) {
			String temp = "";
			for (int i = 0; i < chatList.length; i++) {
				temp += chatList[i].author + "◘" + chatList[i].message + "◘"
						+ chatList[i].sender + "◘" + chatList[i].time + "ﺿ";
			}

			SharedPreferences app_preferences = PreferenceManager
					.getDefaultSharedPreferences(this);
			SharedPreferences.Editor editor = app_preferences.edit();
			editor.putString("chatHistory_" + nick, temp);
			editor.commit();
		}

		super.onStop();
	}

	public void setChatList(ChatMessage[] result) {
		chatList = result;
	}
}

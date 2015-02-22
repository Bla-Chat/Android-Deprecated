package de.michaelfuerst.bla;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
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
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.MediaStore;

public class Chat extends Activity {

	//private static final int VIDEO_RESULT = 0;
	private static final int IMAGE_RESULT = 0;
	private static final int IMAGE_MAX_WIDTH = 200;
	private static final int IMAGE_MAX_HEIGHT = 150;
	private static final double PROFILE_IMAGE_SIZE = 24;
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
	//private boolean fotoReturn;
	private boolean isSetImage = false;
    private LocalResourceManager manager = new LocalResourceManager();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat);

		final Chat parent = this;
		ImageView img = (ImageView) findViewById(R.id.imageView1);

        final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        final EditText editText = (EditText) findViewById(R.id.editText1);
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    if (editText.getText() != null) {
                        String message = editText.getText().toString();

                        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);

                        if (!message.equals("")) {
                            editText.setText("");
                            new SendMessageThread(parent, message, nick).start();
                            insertMessage(message);
                        }

                        handled = true;
                    }
                }
                return handled;
            }
        });

		img.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
                if (editText.getText() != null) {
				    String message = editText.getText().toString();

                    imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);

				    if (!message.equals("")) {
					    editText.setText("");
					    new SendMessageThread(parent, message, nick).start();
					    insertMessage(message);
				    }
                }
            }
		});

		// Show the Up button in the action bar.
		setupActionBar();
		Intent intent = getIntent();

		nick = intent.getStringExtra("chatnick");
		name = intent.getStringExtra("chatname");

        Log.d("Chat", nick+"|"+name);

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
		new Thread() {
			@Override
			public void run() {
				if (!networkAdapter.isRunning()) {
					startService(new Intent(that, BlaNetwork.class));
					LoginNetworkThread t = new LoginNetworkThread(that);
					t.start();
				}
				networkAdapter.attachMessageListener(ml);
				networkAdapter.setActiveConversation(nick);
				networkAdapter.requestResume();
			}
		}.start();

		chatList = loadChatAs(that, nick);
		drawHistory(chatList);
		updateHistory();
	}

	public static ChatMessage[] loadChatAs(Context ctx, String nick) {
		SharedPreferences app_preferences = PreferenceManager
				.getDefaultSharedPreferences(ctx);

		String[] splits = app_preferences.getString("chatHistory_" + nick, "")
				.split(BlaNetwork.EOL);
		ChatMessage[] chatList = new ChatMessage[splits.length];
		for (int i = 0; i < splits.length; i++) {
			String[] sub = splits[i].split(BlaNetwork.SEPARATOR);
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
        if (chatList == null) return;
		String temp = "";
        for (ChatMessage aChatList : chatList) {
            temp += aChatList.author + BlaNetwork.SEPARATOR
                    + aChatList.message + BlaNetwork.SEPARATOR
                    + aChatList.sender + BlaNetwork.SEPARATOR
                    + aChatList.time + BlaNetwork.EOL;
        }

		SharedPreferences app_preferences = PreferenceManager
				.getDefaultSharedPreferences(ctx);
		SharedPreferences.Editor editor = app_preferences.edit();
		editor.putString("chatHistory_" + nick, temp);
		editor.commit();
	}

	private void insertMessage(String message) {
        System.arraycopy(chatList, 0, chatList, 1, chatList.length - 1);
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
		networkAdapter.setLastMessage(nick, message, m.time);
	}

	public void drawHistory(ChatMessage[] messages) {
		if (isAlive) {
			Log.d("Chat", "redrawing history");
			LinearLayout ll = (LinearLayout) findViewById(R.id.messages);
			ll.removeAllViews();

			ChatMessage c;
			if (messages.length == 0) {
				TextView textView = new TextView(this);
				textView.setText("No messages");
				textView.setPadding(48, 48, 48, 48);
				textView.setTextColor(Color.LTGRAY);
				ll.addView(textView);
			}
			for (int i = messages.length - 1; i >= 0; i--) {
				int tmp = i;
				c = messages[i];
				if (c == null) {
					continue;
				}
				RelativeLayout outer = new RelativeLayout(this);
				LinearLayout outer2 = new LinearLayout(this);
				LinearLayout outer3 = new LinearLayout(this);
				outer3.setOrientation(LinearLayout.HORIZONTAL);
				LinearLayout view = new LinearLayout(this);
				view.setOrientation(LinearLayout.VERTICAL);

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
						textView.setMinimumWidth(96+48);
						view.addView(textView);
					}
					i--;
				}
				i++;

				TextView vTime = new TextView(this);
				vTime.setTextColor(Color.rgb(150, 150, 150));
				vTime.setGravity(Gravity.RIGHT);
				if (c.time.equals("uploading...")) {
					vTime.setText(c.time);
				} else {
					if (!(tmp < messages.length - 1 && c.time.substring(0, 10)
							.equals(messages[tmp + 1].time.substring(0, 10)))) {
						ll.addView(getTimestamp(c.time));
					}
					vTime.setText(c.time.substring(11, 16));
				}
				view.addView(vTime);
				view.setPadding(4, 4, 4, 1);
				int a = 0;
				if (c.author.equals(BlaNetwork.getInstance().getUser())) {
					view.setBackgroundColor(Color
							.rgb(245 - a, 255 - a, 245 - a));
					outer.setGravity(Gravity.RIGHT);
					outer.setPadding(48 + 36, 8, 4, 8);
				} else {
					view.setBackgroundColor(Color
							.rgb(245 - a, 245 - a, 255 - a));
					outer.setPadding(4, 8, 48, 8);

					Resources r = this.getResources();
					double px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1,
							r.getDisplayMetrics());
					final int res = (int)(PROFILE_IMAGE_SIZE * px);
					
					RelativeLayout v = new RelativeLayout(this);
					v.setMinimumHeight(res);
					v.setMinimumWidth(res);
					v.setGravity(Gravity.CLIP_HORIZONTAL);

					v.addView(getImageViewTiny(this, BlaNetwork.getServer(this)
							+ "/imgs/profile_" + c.author + ".png"));
					outer3.addView(v);
				}
				outer2.setPadding(0, 0, 1, 1);
				outer2.setBackgroundColor(Color.rgb(30, 30, 30));
				outer2.addView(view);
				outer3.addView(outer2);
				outer.addView(outer3);
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

	private View getTimestamp(String timestamp) {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        try {
            Date date = format.parse(timestamp);

            Calendar yesterday = Calendar.getInstance();
            yesterday.add(Calendar.DATE, -1);

            Calendar week = Calendar.getInstance();
            week.add(Calendar.DATE, -7);

            Calendar year = Calendar.getInstance();
            year.add(Calendar.YEAR, -1);

            if (timestamp.startsWith(new SimpleDateFormat("yyyy-MM-dd").format(new Date()))) {
                timestamp = "Heute";
            } else if (timestamp.startsWith(new SimpleDateFormat("yyyy-MM-dd").format(yesterday.getTime()))) {
                timestamp = "Gestern";
            } else if (date.after(week.getTime())) {
                // Week
                DateFormat f = new SimpleDateFormat("EEEE", Locale.GERMAN);
                timestamp = f.format(date);
            } else if (date.after(year.getTime())) {
                DateFormat f = new SimpleDateFormat("d. MMMM", Locale.GERMAN);
                timestamp = f.format(date);
            } else {
                DateFormat f = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN);
                timestamp = f.format(date);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
		LinearLayout view = new LinearLayout(this);
		view.setPadding(48 + 36, 8, 48, 8);
		view.setGravity(Gravity.CENTER);
		LinearLayout shaddow = new LinearLayout(this);
		shaddow.setPadding(0, 0, 1, 1);
		shaddow.setBackgroundColor(Color.rgb(30, 30, 30));
		shaddow.setGravity(Gravity.CENTER);
		LinearLayout inner = new LinearLayout(this);
		inner.setPadding(8, 2, 8, 2);
		inner.setBackgroundColor(Color.rgb(230, 230, 230));
		inner.setGravity(Gravity.CENTER);
		TextView textView = new TextView(this);
		textView.setPadding(8, 8, 8, 8);
		textView.setText(timestamp);
		textView.setTextColor(Color.rgb(150, 150, 150));
		textView.setGravity(Gravity.CENTER);
		inner.addView(textView);
		shaddow.addView(inner);
		view.addView(shaddow);
		return view;
	}

	private boolean timeDif(String time, String time2) {
		if (time.equals("uploading...") || time2.equals("uploading..."))
            return !time.equals("uploading...") || !time2.equals("uploading...");

		String tmp = time.substring(time.length() - 5, time.length() - 3);
		int t1 = Integer.parseInt(tmp);

		tmp = time2.substring(time2.length() - 5, time2.length() - 3);
		int t2 = Integer.parseInt(tmp);

		time = time.substring(0, time.length() - 6);
		time2 = time2.substring(0, time2.length() - 6);

		return !time.equals(time2) || Math.abs(t1 - t2) > 2;
	}

	private View getImageView(final String path) {
		
		final ImageView iv = new ImageView(this);
		String preFile = path.split("/")[path.split("/").length - 1];
		final String filename = getApplicationInfo().dataDir
				+ "/Pictures/" + preFile.split("\\.")[0] + ".png";
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
		
		Resources r = this.getResources();
		double px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1,
				r.getDisplayMetrics());
		int res = (int) (IMAGE_MAX_HEIGHT * px);
		int resW = (int) (IMAGE_MAX_WIDTH * px);
		
		iv.setMaxHeight(res);
		iv.setMaxWidth(resW);
		iv.setMinimumHeight(res);
		iv.setMinimumWidth(resW);
		iv.setScaleType(ScaleType.CENTER_CROP);
		Drawable image = manager.getDrawable(this, filename,
				IMAGE_MAX_WIDTH, 0);
		if (image != null) {
			iv.setImageDrawable(image);
		} else {
			iv.setBackgroundColor(Color.LTGRAY);
		}
		return iv;
	}

	protected void updateHistory() {
		new ChatHistoryThread().execute(this, nick);
	}

	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && getActionBar() != null) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.chat, menu);
		MenuCompat.setShowAsAction(menu.findItem(R.id.action_addImage), 1);
		return true;
	}

	private static String tmp_image = "/Pictures/tmp.png";

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final Chat parent = this;
		int itemId = item.getItemId();
		if (itemId == android.R.id.home) {
			NavUtils.navigateUpFromSameTask(this);
			return true;
		} else if (itemId == R.id.action_addImage) {
            isSetImage = false;
			Intent pickIntent = new Intent();
			pickIntent.setType("image/*");
			pickIntent.setAction(Intent.ACTION_GET_CONTENT);
			Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT,
					Uri.fromFile(new File(getApplicationInfo().dataDir + tmp_image)));
			String pickTitle = "Select or take a new Picture"; // Or get from
			// strings.xml
			Intent chooserIntent = Intent.createChooser(pickIntent, pickTitle);
			chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS,
					new Intent[] { takePhotoIntent });
			startActivityForResult(chooserIntent, IMAGE_RESULT);
			return true;
		} else if (itemId == R.id.action_setImage) {
			isSetImage = true;
			Intent pickIntent2 = new Intent();
			pickIntent2.setType("image/*");
			pickIntent2.setAction(Intent.ACTION_GET_CONTENT);
			Intent takePhotoIntent2 = new Intent(
					MediaStore.ACTION_IMAGE_CAPTURE);
			takePhotoIntent2.putExtra(MediaStore.EXTRA_OUTPUT,
					Uri.fromFile(new File(getApplicationInfo().dataDir + tmp_image)));
			String pickTitle2 = "Select or take a new Picture"; // Or get from
			// strings.xml
			Intent chooserIntent2 = Intent.createChooser(pickIntent2,
					pickTitle2);
			chooserIntent2.putExtra(Intent.EXTRA_INITIAL_INTENTS,
					new Intent[] { takePhotoIntent2 });
			startActivityForResult(chooserIntent2, IMAGE_RESULT);
			return true;
		}/* else if (itemId == R.id.action_addVideo) {
            Toast.makeText(this, "Upcoming feature", Toast.LENGTH_SHORT).show();
            // startActivityForResult(new
            // Intent(MediaStore.ACTION_VIDEO_CAPTURE),
            // VIDEO_RESULT);
            return true;
        }*/ else if (itemId == R.id.action_settings) {
            Intent intent = new Intent(getApplicationContext(),
                    SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.action_rename) {
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
                            if (input.getText() == null) return;
							final String value = input.getText().toString();
							networkAdapter = BlaNetwork.getInstance();
							if (networkAdapter != null) {
								new AsyncTask<Void, Void, Void>() {
									@Override
									public Void doInBackground(Void... params) {
										networkAdapter.rename(nick, value);
										return null;
									}
								}.execute();
                                name = value;
                                setTitle(name);
							} else {
								Toast.makeText(parent, "Error cannot rename.",
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
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == IMAGE_RESULT) {
			if (resultCode == Activity.RESULT_OK) {
				if (data != null && data.getData() != null) {
					final Chat that = this;
					Uri _uri = data.getData();

					// User had pick an image.
					Cursor cursor = getContentResolver()
							.query(_uri,
									new String[] { android.provider.MediaStore.Images.ImageColumns.DATA },
									null, null, null);
                    if (cursor == null) return;

					cursor.moveToFirst();

					// Link to the image
                    String tmp = cursor.getString(0);
                    if (tmp == null) {
                        tmp = Utils.getUriAdv(this, data);
                    }
					final String imageFilePath = tmp;
					cursor.close();

					Toast.makeText(this, "Uploading image " + imageFilePath, Toast.LENGTH_LONG)
							.show();
					if (!isSetImage) {
						insertMessage("#image " + imageFilePath);
					}
					new AsyncTask<Void, Void, Void>() {
						@Override
						protected Void doInBackground(Void... params) {
							BitmapFactory.Options options = new BitmapFactory.Options();
							options.inPreferredConfig = Bitmap.Config.ARGB_8888;
							options.inSampleSize = 2;
							Bitmap bmp = BitmapFactory.decodeFile(
									imageFilePath, options);
							if (bmp != null) {
								if (isSetImage) {
									networkAdapter.setImage(bmp, nick);
								} else {
									networkAdapter.send(bmp, nick);
								}
							}
							return null;
						}

						@Override
						protected void onPostExecute(Void v) {
							Toast.makeText(that, "Uploaded image",
									Toast.LENGTH_LONG).show();
						}
					}.execute();
				} else {
					final String imageFilePath = getApplicationInfo().dataDir + tmp_image;
					Toast.makeText(this, "Uploading image", Toast.LENGTH_LONG)
							.show();
					final Chat that = this;
					if (!isSetImage) {
						insertMessage("#image " + imageFilePath);
					}
					new AsyncTask<Void, Void, Void>() {
						@Override
						protected Void doInBackground(Void... params) {
							BitmapFactory.Options options = new BitmapFactory.Options();
							options.inPreferredConfig = Bitmap.Config.ARGB_8888;
							options.inSampleSize = 2;
							Bitmap bmp = BitmapFactory.decodeFile(
									imageFilePath, options);
							if (bmp != null) {
								if (isSetImage) {
									networkAdapter.setImage(bmp, nick);
								} else {
									networkAdapter.send(bmp, nick);
								}
							}
							return null;
						}

						@Override
						protected void onPostExecute(Void v) {
							Toast.makeText(that, "Uploaded image",
									Toast.LENGTH_LONG).show();
						}
					}.execute();
				}
			}
		} /*else if (requestCode == VIDEO_RESULT) {
			if (resultCode == Activity.RESULT_OK) {
				Toast.makeText(this, "Uploading video", Toast.LENGTH_LONG)
						.show();
				Bundle b = data.getExtras();
                if ( b== null ) return;
				Video vid = (Video) b.get("data");
				if (vid != null) {
					networkAdapter.send(vid, nick);
				}
			}
		}*/
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onPause() {
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
		new Thread() {

			@Override
			public void run() {
				synchronized (BlaNetwork.class) {
					while (networkAdapter == null) {
						try {
							BlaNetwork.class.wait();
						} catch (InterruptedException e) {
							return;
						}
						networkAdapter = BlaNetwork.getInstance();
					}
				}
				networkAdapter.requestResume();
				networkAdapter.unmark(nick);
			}
		}.start();
		super.onResume();
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	public void setChatList(ChatMessage[] result) {
		chatList = result;
		if (result.length > 0 && result[0] != null) {
			networkAdapter.setLastMessage(nick, result[0].message, result[0].time);
		}
	}

	private ImageView getImageViewTiny(final Context ctx, final String path) {
		Resources r = ctx.getResources();
		double px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1,
				r.getDisplayMetrics());
		final int size = (int)(PROFILE_IMAGE_SIZE * px);
		
		final ImageView iv = new ImageView(ctx);
		iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
		//iv.setAdjustViewBounds(true);
		iv.setMaxWidth(size);
		iv.setMaxHeight(size);
		iv.setMinimumWidth(size);
		iv.setMinimumWidth(size);

		Drawable image = manager.getDrawable(ctx, path,
				size, 0);

		if (image == null) {
			new AsyncTask<Void, Void, Drawable>() {
				private String p = "none";

				@Override
				protected Drawable doInBackground(Void... params) {
					p = BlaNetwork.getServer(ctx) + "/imgs/user.png";

                    return manager.getDrawable(ctx, p, size, 0);
				}

				@Override
				public void onPostExecute(Drawable image) {
					if (image == null) {
						iv.setBackgroundColor(Color.rgb(0, 0, 0));
					} else {
						iv.setImageDrawable(image);
					}
				}
			}.execute();
		} else {
			iv.setImageDrawable(image);
		}
		return iv;
	}
}

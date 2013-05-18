/**
 * 
 */
package de.michaelfuerst.hangout;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.MediaStore.Video;

/**
 * Retrieves and sends network messages.
 * 
 * @author Michael Fürst
 * @version 1.0
 */
public class HangoutNetwork extends Service implements Runnable {
	private static HangoutNetwork instance = null;

	public static String HANGOUT_SERVER = "https://www.ssl-id.de/hangout.f-online.net/api";

	private LinkedList<MessageListener> listeners = new LinkedList<MessageListener>();
	private LinkedList<String> conversations = new LinkedList<String>();
	private LinkedList<String> conversationNicks = new LinkedList<String>();
	private String nick = null;
	private String pw = null;
	private int status = 120;
	private String id = "";
	private boolean isRunning = false;
	private String activeConversation = null;
	private static final int mId = 0;
	private Activity parent = null;
	private boolean isReady = false;
	private LinkedList<String> markedConversations = new LinkedList<String>();
	private boolean offline = false;

	public void setParent(Activity parent) {
		this.parent = parent;
	}

	public void setActiveConversation(String newConversation) {
		activeConversation = newConversation;
	}

	public String getActiveConversation() {
		return activeConversation;
	}

	public HangoutNetwork() {
		super();
		instance = this;
	}

	public static HangoutNetwork getInstance() {
		// if (instance == null) {
		// instance = new HangoutNetwork();
		// }
		return instance;
	}

	@Override
	public void run() {
		try {
			runService();
		} catch (Exception e) {
			addNotification("ERROR", e.getMessage(), true);
		}
	}

	private void runService() {
		status = 120;
		setPersistent("Authentificating");
		if (!isReady) {
			LoginNetworkThread t = new LoginNetworkThread(this);
			t.start();
			try {
				t.join();
			} catch (InterruptedException e) {
				setPersistent("Login error");
			}
		}
		int loginDelay = 1;
		while (!isReady) {
			try {
				Thread.sleep(100 * loginDelay); // sleep longer the more time
												// passes.
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			loginDelay++;
		}
		setPersistent("Online");
		isRunning = true;
		tryToRetrieveOldMessages();
		SharedPreferences app_preferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = app_preferences.edit();
		editor.putString("id", id);
		editor.commit();
		while (status > 0) {
			long delay = status * 100;
			if (status < 120) {
				status += 10;
				removeNotification();
			} else if (status < 300) { // slightly increase status
				status += 20;
			} else if (status < 1500) { // make a cut to inactivity
				status = 1500;
			} else if (status < 3000) { // increase to max
				status += 500;
			} else {
				status = 3000; // 5 minute lock
			}
			if (!offline) {
				setPersistent("Online (" + status + ")");
			}

			String jsonString = "{\"type\":\"onEvent\", \"msg\":{\"user\":\""
					+ nick + "\" , \"password\": \"" + pw + "\", \"id\": \""
					+ id + "\"}}";
			try {
				String result = submit(jsonString, HANGOUT_SERVER);
				if (result != null && !result.equals("")) {
					JSONArray ja = new JSONArray(result);
					for (int i = 0; i < ja.length(); i++) {
						JSONObject jo = (JSONObject) ja.get(i);
						handleIncoming(jo);
					}
				}
			} catch (JSONException e1) {
				e1.printStackTrace();
			}

			try {
				Thread.sleep(delay);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		isRunning = false;
	}

	private void tryToRetrieveOldMessages() {
		SharedPreferences app_preferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		String oldId = app_preferences.getString("id", "");

		String jsonString = "{\"type\":\"onEvent\", \"msg\":{\"user\":\""
				+ nick + "\" , \"password\": \"" + pw + "\", \"id\": \""
				+ oldId + "\"}}";
		try {
			JSONArray ja = new JSONArray(submit(jsonString, HANGOUT_SERVER));
			for (int i = 0; i < ja.length(); i++) {
				JSONObject jo = (JSONObject) ja.get(i);
				handleIncoming(jo);
			}
		} catch (JSONException e1) {
			e1.printStackTrace();
		}
	}

	private void handleIncoming(JSONObject jo) throws JSONException {
		String type = jo.getString("type");
		String msg = jo.getString("msg");
		String trigger = jo.getString("nick");
		String text = jo.getString("text");
		if (type.equals("onMessage")) {
			onReceiveMessage(trigger, msg, text);
		} else if (type.equals("onMessageHandled")) {
			unmarkLocal(msg);
		} else if (type.equals("onConversation")) {
			updateConversations();
		} else if (type.equals("forceReload")) {
			requireFileReload(msg);
		}
	}

	private void setStatus(int level) {
		status = level;
	}

	/**
	 * Called when an event is received in the message loop.
	 * 
	 * @param message
	 *            The message that was received.
	 * @param conversation
	 *            The conversation for which it was.
	 * @param text
	 */
	private void onReceiveMessage(String trigger, String conversation,
			String text) {
		if (!conversation.equals(this.activeConversation) || status >= 120) {
			if (text.startsWith("#image")) {
				text = "Image received";
			} else if (text.startsWith("#video")) {
				text = "Video received";
			} else if (text.startsWith("#file")) {
				text = "File received";
			}
			addNotification(conversation, text, true);
			status = 120;
		}
		for (MessageListener l : listeners) {
			l.onMessageReceived(trigger, conversation);
		}
	}

	private String submit(String jsonString, String server) {
		if (!isActive()) {
			throw new NullPointerException("Logindata must be set first.");
		}
		String message = "";
		try {
			// Create a new HttpClient and Post Header
			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = new HttpPost(server + "/api.php");

			// Add your data
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
			jsonString = escape(jsonString);
			nameValuePairs.add(new BasicNameValuePair("msg", jsonString));
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

			// Execute HTTP Post Request
			HttpResponse response = httpclient.execute(httppost);

			BufferedReader bufferedReader = new BufferedReader(
					new InputStreamReader(response.getEntity().getContent()));
			String next;
			while ((next = bufferedReader.readLine()) != null) {
				JSONObject jo = new JSONObject(next);

				String type = jo.getString("type");
				if (type.equals("onRejected")) {
					message = "ERROR";
				} else if (type.equals("onEvent")) {
					message = jo.getString("msg");
				} else {
					message = jo.getString("msg");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if ((message == null || message.equals("")) && !offline) {
			setPersistent("Lost Connection");
			offline = true;
		} else if (offline) {
			setPersistent("Online");
			offline = false;
		}

		return message;
	}

	private String escape(String jsonString) {
		String result = null;

		try {
			result = URLEncoder.encode(jsonString, "UTF-8").replaceAll("\\+",
					"%20")
			/*
			 * .replaceAll("\\%21", "!") .replaceAll("\\%27", "'")
			 * .replaceAll("\\%28", "(") .replaceAll("\\%29", ")")
			 * .replaceAll("\\%7E", "~")
			 */;
		}

		// This exception should never occur.
		catch (UnsupportedEncodingException e) {
			result = jsonString;
		}

		return result;
	}

	/**
	 * Send a message in the given conversation.
	 * 
	 * @param message
	 *            The message to send.
	 * @param conversation
	 *            The conversation where to post it.
	 */
	public String send(String message, String conversation) {
		if (!isActive()) {
			throw new NullPointerException("Logindata must be set first.");
		}
		while (!isRunning()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		String jsonString = "{\"type\":\"onMessage\", \"msg\":{\"user\":\""
				+ nick + "\" , \"password\": \"" + pw + "\" , \"id\": \"" + id
				+ "\" , \"conversation\": \"" + conversation
				+ "\", \"message\": \"" + message + "\"}}";
		return submit(jsonString, HANGOUT_SERVER);
	}

	public void tryLogin(Context parent) {
		SharedPreferences app_preferences = PreferenceManager
				.getDefaultSharedPreferences(parent);

		if ((app_preferences.getString("nick", null) == null || app_preferences
				.getString("pw", null) == null)) {

			if ((parent != this) && (parent != null)) {
				Intent intent = new Intent(parent.getApplicationContext(),
						Login.class);
				parent.startActivity(intent);
			}
		} else {
			nick = app_preferences.getString("nick", null);
			pw = app_preferences.getString("pw", null);
			id = getNetworkId();
			// id = null;
			if ((id == null || id.equals("ERROR") || id.equals(""))) {
				if ((parent != this) && (parent != null)) {
					Intent intent = new Intent(parent.getApplicationContext(),
							Login.class);
					parent.startActivity(intent);
				}
			} else {
				isReady = true;
			}
		}
	}

	private String getNetworkId() {
		if (!isActive()) {
			return null;
		}
		String jsonString = "{\"type\":\"onIdRequest\", \"msg\":{\"user\":\""
				+ nick + "\" , \"password\": \"" + pw + "\"}}";
		return submit(jsonString, HANGOUT_SERVER);
	}

	/**
	 * Give the required userdata to the NetworkManager and start the event
	 * loop.
	 * 
	 * @param user
	 *            The user.
	 * @param pw
	 *            The password.
	 * @return
	 */
	public boolean login(String user, String pw, Context activity) {
		nick = user;
		this.pw = pw;
		id = getNetworkId();
		SharedPreferences app_preferences = PreferenceManager
				.getDefaultSharedPreferences(activity);

		SharedPreferences.Editor editor = app_preferences.edit();
		editor.putString("pw", pw);
		editor.putString("nick", nick);
		editor.commit();

		if (id == null | id.equals("ERROR") || id.equals("")) {
			// The logindata must have been wrong. Refill form.
			return false;
		} else {
			isReady = true;
			return true;
		}
	}

	/**
	 * Attach a listener that listens for events.
	 * 
	 * @param listener
	 *            The listener for the events.
	 */
	public void attachMessageListener(MessageListener listener) {
		listeners.add(listener);
	}

	/**
	 * Get the list of conversations.
	 * 
	 * @return The list of conversations.
	 */
	public void updateConversations() {
		while (!isRunning) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		String jsonString = "{\"type\":\"onGetChats\", \"msg\":{\"user\":\""
				+ nick + "\" , \"password\": \"" + pw + "\", \"id\": \"" + id
				+ "\"}}";
		String result = submit(jsonString, HANGOUT_SERVER);
		if (result == null || result.equals("")) {
			return;
		}
		try {
			JSONArray ja = new JSONArray(result);
			conversations.clear();
			conversationNicks.clear();
			for (int i = 0; i < ja.length(); i++) {
				conversations.add(((JSONObject) ja.get(i)).getString("name"));
				conversationNicks.add(((JSONObject) ja.get(i))
						.getString("nick"));
			}
		} catch (JSONException e1) {
			e1.printStackTrace();
		}
	}

	public LinkedList<String> getConversations() {
		return conversations;
	}

	public String getConversationNickAt(int pos) {
		return conversationNicks.get(pos);
	}

	/**
	 * Get a chat history.
	 * 
	 * @param id
	 *            The id of the chat.
	 * @return The chat history at the given id.
	 */
	public ChatMessage[] getChat(String conversation) {
		while (!isRunning) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		LinkedList<ChatMessage> out = new LinkedList<ChatMessage>();
		String jsonString = "{\"type\":\"onGetHistory\", \"msg\":{\"user\":\""
				+ nick + "\" , \"password\": \"" + pw + "\", \"id\": \"" + id
				+ "\", \"conversation\":\"" + conversation + "\"}}";
		String result = submit(jsonString, HANGOUT_SERVER);
		if (result == null || result.equals("")) {
			return null;
		}
		try {
			JSONArray ja = new JSONArray(result);
			for (int i = 0; i < ja.length(); i++) {
				ChatMessage c = new ChatMessage();
				c.sender = ((JSONObject) ja.get(i)).getString("author");
				c.time = ((JSONObject) ja.get(i)).getString("time");
				c.message = ((JSONObject) ja.get(i)).getString("message");
				c.author = ((JSONObject) ja.get(i)).getString("authorNick");
				out.add(c);
			}
		} catch (JSONException e1) {
			e1.printStackTrace();
		}
		return out.toArray(new ChatMessage[0]);
	}

	/**
	 * Determine if the client is active.
	 * 
	 * @return True, iff the client is active.
	 */
	private boolean isActive() {
		return nick != null && pw != null;
	}

	/**
	 * True, iff the event loop is already running.
	 * 
	 * @return True, iff the event loop is already running.
	 */
	public boolean isRunning() {
		return isRunning;
	}

	public void requestPause() {
		setStatus(110);
	}

	public void requestResume() {
		setStatus(2);
	}

	public void requestResumeLowFrequency() {
		setStatus(110);
	}

	@SuppressWarnings("deprecation")
	public void addNotification(String conversation, String text,
			boolean vibrate) {
		String name = null;
		// Get conversation names from memory if not loaded yet.
		SharedPreferences app_preferences = PreferenceManager
				.getDefaultSharedPreferences(this);

		String[] splits = app_preferences.getString("conversations", "").split(
				";");
		for (String s : splits) {
			String[] tmp = s.split("-");
			if (tmp.length == 3) {
				conversations.add(tmp[0]);
				addConversationNick(tmp[1]);
				if (tmp[2].equals("true")) {
					mark(tmp[1]);
				}
			}
		}
		if (conversations.size() == 0) {
			updateConversations();
		}
		for (int i = 0; i < conversations.size(); i++) {
			if (getConversationNickAt(i).equals(conversation)) {
				name = conversations.get(i);
			}
		}
		if (name == null) {
			name = conversation;
		} else {
			mark(conversation);
		}
		final Notification notification = new Notification(
				R.drawable.ic_launcher, text, System.currentTimeMillis());

		// used to call up this specific intent when you click on the
		// notification
		final PendingIntent contentIntent = PendingIntent.getActivity(
				getBaseContext(), 0, new Intent(getBaseContext(),
						Conversations.class), Notification.FLAG_AUTO_CANCEL);

		notification.setLatestEventInfo(getBaseContext(), name, text,
				contentIntent);
		notification.defaults = Notification.DEFAULT_ALL;

		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		// mId allows you to update the notification later on.

		Uri alarmSound = RingtoneManager
				.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		if (alarmSound != null) {
			notification.sound = alarmSound;
		}
		if (vibrate) {
			long[] pattern = { 100, 400 /* , 300, 800 */};
			notification.vibrate = pattern;
		}
		if (name.equals("ERROR")) {
			mNotificationManager.notify(mId + 1, notification);
		} else {
			mNotificationManager.notify(mId, notification);
		}
	}

	@SuppressWarnings("deprecation")
	public void setPersistent(String status) {
		String name = getString(R.string.app_name);
		Notification notification = new Notification(R.drawable.ic_launcher,
				name, System.currentTimeMillis());
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, Conversations.class),
				Notification.FLAG_ONGOING_EVENT);
		notification.flags = Notification.FLAG_ONGOING_EVENT;
		notification.setLatestEventInfo(this, name, status, contentIntent);
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(mId + 1, notification);
	}

	public void removeNotification() {
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel(mId);
	}

	public void addConversationNick(String string) {
		conversationNicks.add(string);
	}

	public String getUser() {
		return nick;
	}

	@Override
	public void onStart(Intent intent, int startId) {
		handleCommand(intent);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		handleCommand(intent);
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		instance = null;
		super.onDestroy();
	}

	private void handleCommand(Intent intent) {
		instance = this;
		Thread t = new Thread(this);
		t.start();
	}

	@Override
	public void onCreate() {
		instance = this;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return new HIBinder(this);
	}

	public void mark(String conversation) {
		markedConversations.add(conversation);
		String temp = "";
		conversations = getConversations();
		for (int i = 0; i < conversations.size(); i++) {
			temp += conversations.get(i);
			temp += "-" + getConversationNickAt(i);
			if (getMarkedConversations().contains(getConversationNickAt(i))) {
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

	public void unmark(String conversation) {
		unmarkLocal(conversation);

		String jsonString = "{\"type\":\"onEvent\", \"msg\":{\"user\":\""
				+ nick + "\" , \"password\": \"" + pw + "\", \"id\": \"" + id
				+ "\", \"type\":\"onMessage\", \"message\":\"" + conversation
				+ "\"}}";
		submit(jsonString, HANGOUT_SERVER);
	}
	
	private void unmarkLocal(String conversation) {
		markedConversations.remove(conversation);
		String temp = "";
		conversations = getConversations();
		for (int i = 0; i < conversations.size(); i++) {
			temp += conversations.get(i);
			temp += "-" + getConversationNickAt(i);
			if (getMarkedConversations().contains(getConversationNickAt(i))) {
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

	public LinkedList<String> getMarkedConversations() {
		return markedConversations;
	}

	public void setConversationNickAt(int pos, String string) {
		conversationNicks.set(pos, string);
	}

	static String lineEnd = "\r\n";
	static String twoHyphens = "--";
	static String boundary = "AaB03x87yxdkjnxvi7";

	public void send(final Bitmap bmp, final String conversation) {
		new Thread() {
			public void run() {
				String jsonString = "{\"type\":\"onData\", \"msg\":{\"user\":\""
						+ nick
						+ "\" , \"password\": \""
						+ pw
						+ "\", \"conversation\":\""
						+ conversation
						+ "\", \"type\":\"image\"}}";
				try {
					HttpURLConnection conn = null;
					DataOutputStream dos = null;
					DataInputStream dis = null;
					URL url = new URL(HANGOUT_SERVER + "/api.php");
					// ------------------ CLIENT REQUEST

					// open a URL connection to the Servlet
					// Open a HTTP connection to the URL
					conn = (HttpURLConnection) url.openConnection();
					// Allow Inputs
					conn.setDoInput(true);
					// Allow Outputs
					conn.setDoOutput(true);
					// Don't use a cached copy.
					conn.setUseCaches(false);
					// Use a post method.
					conn.setRequestMethod("POST");
					conn.setRequestProperty("Content-Type",
							"multipart/form-data;boundary=" + boundary);

					dos = new DataOutputStream(conn.getOutputStream());

					dos.writeBytes(twoHyphens + boundary + lineEnd);
					dos.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\"; filename=\"image.png\""
							+ lineEnd);
					dos.writeBytes("Content-Type: text/xml" + lineEnd);
					dos.writeBytes(lineEnd);
					bmp.compress(CompressFormat.PNG, 100, dos);
					bmp.recycle();

					dos.writeBytes(lineEnd);
					dos.writeBytes(twoHyphens + boundary + lineEnd);
					dos.writeBytes("Content-Disposition: form-data; name=\"msg\""
							+ lineEnd);
					dos.writeBytes(lineEnd);
					dos.writeBytes(jsonString);

					// send multipart form data necessary after file data...
					dos.writeBytes(lineEnd);
					dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
					dos.flush();
					// ------------------ read the SERVER RESPONSE
					try {
						dis = new DataInputStream(conn.getInputStream());
						StringBuilder response = new StringBuilder();

						String line;
						while ((line = dis.readLine()) != null) {
							response.append(line).append('\n');
						}

						String result = response.toString();
						// Ignored atm.
					} finally {
						if (dis != null)
							dis.close();
					}
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (ProtocolException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		}.start();
	}

	public void send(final Video vid, final String conversation) {
		new Thread() {
			public void run() {
				String jsonString = "{\"type\":\"onData\", \"msg\":{\"user\":\""
						+ nick
						+ "\" , \"password\": \""
						+ pw
						+ "\", \"conversation\":\""
						+ conversation
						+ "\", \"type\":\"image\"}}";
				try {
					HttpURLConnection conn = null;
					DataOutputStream dos = null;
					DataInputStream dis = null;
					FileInputStream fileInputStream = null;
					URL url = new URL(HANGOUT_SERVER + "/api.php");
					// ------------------ CLIENT REQUEST

					// open a URL connection to the Servlet
					// Open a HTTP connection to the URL
					conn = (HttpURLConnection) url.openConnection();
					// Allow Inputs
					conn.setDoInput(true);
					// Allow Outputs
					conn.setDoOutput(true);
					// Don't use a cached copy.
					conn.setUseCaches(false);
					// Use a post method.
					conn.setRequestMethod("POST");
					conn.setRequestProperty("Content-Type",
							"multipart/form-data;boundary=" + boundary);

					dos = new DataOutputStream(conn.getOutputStream());

					dos.writeBytes(twoHyphens + boundary + lineEnd);
					dos.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\"; filename=\"video.png\""
							+ lineEnd);
					dos.writeBytes("Content-Type: text/xml" + lineEnd);
					dos.writeBytes(lineEnd);
					// vid.compress(CompressFormat.PNG, 50, dos);

					dos.writeBytes(lineEnd);
					dos.writeBytes(twoHyphens + boundary + lineEnd);
					dos.writeBytes("Content-Disposition: form-data; name=\"msg\""
							+ lineEnd);
					dos.writeBytes(lineEnd);
					dos.writeBytes(jsonString);

					// send multipart form data necessary after file data...
					dos.writeBytes(lineEnd);
					dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
					dos.flush();
					// ------------------ read the SERVER RESPONSE
					try {
						dis = new DataInputStream(conn.getInputStream());
						StringBuilder response = new StringBuilder();

						String line;
						while ((line = dis.readLine()) != null) {
							response.append(line).append('\n');
						}

						String result = response.toString();
						// Ignored atm.
					} finally {
						if (dis != null)
							dis.close();
					}
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (ProtocolException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	private LinkedList<String> reloadFiles = new LinkedList<String>();
	public void requireFileReload(String preFile) {
		reloadFiles.add(preFile);
	}
	
	public boolean reloadFile(String preFile) {
		return reloadFiles.contains(preFile);
	}
	
	public void fileReloaded(String preFile) {
		reloadFiles.remove(preFile);
	}
}

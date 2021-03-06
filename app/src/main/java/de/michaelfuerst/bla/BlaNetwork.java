/**
 * The bla package.
 */
package de.michaelfuerst.bla;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
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
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Retrieves and sends network messages.
 * 
 * @author Michael F�rst
 * @version 1.0
 */
public class BlaNetwork extends Service implements Runnable {
    public static final String UPDATE_SERVER = "https://raw.github.com/Bla-Chat/Android-Deprecated/master/app";
    private static BlaNetwork instance = null;

	public final static String SEPARATOR = "◘";
	public final static String EOL = "ﺿ";
	public final static int CONVERSATION_PARAMETERS = 5;
	public final static String DEFAULT_BLA_SERVER = "https://www.ssl-id.de/bla.f-online.net/api";
    private static String server = null;

	private LinkedList<MessageListener> listeners = new LinkedList<MessageListener>();
	private final LinkedList<String> conversations;
	private final LinkedList<String> conversationNicks;
	private static String nick = null;
	private String pw = null;
	private int status = 120;
	private String id = "REJECTED";
	private boolean isRunning = false;
	private String activeConversation = null;
	private static final int mId = 0;
	private boolean isReady = false;
	private final LinkedList<String> markedConversations;
	private boolean offline = false;
    private boolean update = false;
    private Activity activity = null;

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public static void setServer(String server, Context ctx) {
        BlaNetwork.server = server;

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
        preferences.edit().putString("bla_server", server).commit();
    }

    private String getServer() {
        return getServer(this);
    }

    public static String getServer(Context ctx) {
        if (server == null) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
            server = preferences.getString("bla_server", DEFAULT_BLA_SERVER);
        }

        return server;
    }

    public void setActiveConversation(String newConversation) {
		activeConversation = newConversation;
	}

	/*public String getActiveConversation() {
		return activeConversation;
	}*/

	/**
	 * You must not call this, this is only public because the service needs to
	 * call this.
	 */
	public BlaNetwork() {
		super();

		instance = this;
		conversations = new LinkedList<String>();
		conversationNicks = new LinkedList<String>();
		markedConversations = new LinkedList<String>();
        lastMessages = new HashMap<String, String>();
        lastMessagesTime = new HashMap<String, String>();

		synchronized (BlaNetwork.class) {
			BlaNetwork.class.notifyAll();
		}
	}

	public static BlaNetwork getInstance() {
		return instance;
	}

    public void setReady(boolean value) {
        synchronized (BlaNetwork.class) {
            isReady = value;
            BlaNetwork.class.notifyAll();
        }
    }

    public void setRunning(boolean value) {
        synchronized (BlaNetwork.class) {
            isRunning = value;
            BlaNetwork.class.notifyAll();
        }
    }

	@Override
	public void run() {
        getServer(this);

		UpdateApp updater = new UpdateApp();
		updater.setContext(getApplicationContext());
		updater.execute(UPDATE_SERVER);
		try {
			runService();
		} catch (NullPointerException e) {
			addNotification("ERROR", e.getMessage(), true, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
		}
	}

	private void runService() {
        synchronized (BlaNetwork.class) {
            BlaNetwork.class.notifyAll();
        }
		status = 500;
		if (!isReady) {
            if (canLogin()) {
                LoginNetworkThread t = new LoginNetworkThread(this);
                t.start();
                try {
                    t.join();
                } catch (InterruptedException e) {
                    Log.d("BlaChat", "We were interrupted!");
                    return;
                }
            }
            synchronized (BlaNetwork.class) {
                while (!isReady) {
                    try {
                        BlaNetwork.class.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
		}

		setRunning(true);
		tryToRetrieveOldMessages();
		updateNotifications(false);
        SharedPreferences app_preferences = PreferenceManager
                .getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = app_preferences.edit();
		editor.putString("id", id);
		editor.commit();
		while (status > 0) {
			if (status < 12000) {
				status += 1000;
			} else if (status < 30000) { // slightly increase status
				status += 2000;
			} else {
				status = 120000; // 2 minute lock
			}

            int syncFrequency = Integer.parseInt(app_preferences.getString("sync_frequency", "2"));
            int tmpstatus = (int) (status * syncFrequency / 2.0);

            ConnectivityManager mgrConn = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mWifi = mgrConn.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (mWifi != null && mWifi.isConnected()) {
                // Force higher priority updates when connected to WIFI
                tmpstatus = (int) (status * syncFrequency / 4.0);

                // WLAN only mode
                if (tmpstatus < 0) {
                    tmpstatus = status;
                }
            }

            if (tmpstatus > 0) {
			    String jsonString = "{\"type\":\"onEvent\", \"msg\":{\"user\":\""
					+ nick + "\" , \"password\": \"" + pw + "\", \"id\": \""
					+ id + "\"}}";
			    try {
				    String result = submit(jsonString, getServer());
				    if (result != null && !result.equals("")) {
					    JSONArray ja = new JSONArray(result);
					    for (int i = 0; i < ja.length(); i++) {
						    JSONObject jo = (JSONObject) ja.get(i);
						    handleIncoming(jo);
					    }
				    }
			    } catch (JSONException e1) {
                    // Quiet
			    }
            } else {
                tmpstatus = status;
            }

			try {
				Thread.sleep(tmpstatus);
            } catch (Exception e) {
				e.printStackTrace();
			}
		}
		setRunning(false);
	}

	private void tryToRetrieveOldMessages() {
		SharedPreferences app_preferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		String oldId = app_preferences.getString("id", "");

		String jsonString = "{\"type\":\"onEvent\", \"msg\":{\"user\":\""
				+ nick + "\" , \"password\": \"" + pw + "\", \"id\": \""
				+ oldId + "\"}}";
		try {
			JSONArray ja = new JSONArray(submit(jsonString, getServer()));
			for (int i = 0; i < ja.length(); i++) {
				JSONObject jo = (JSONObject) ja.get(i);
				handleIncoming(jo);
			}
		} catch (JSONException e1) {
            // Quiet
		}
	}

	private void handleIncoming(JSONObject jo) throws JSONException {
		String type = jo.getString("type");
		String msg = jo.getString("msg");
		String trigger = jo.getString("nick");
		String text = jo.getString("text");
		if (type.equals("onMessage")) {
			onReceiveMessage(trigger, msg, text, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
		} else if (type.equals("onMessageHandled")) {
			unmarkLocal(msg);
			removeNotification(msg);
			Log.d("BlaChat", "Handled: " + trigger + ";" + msg);
		} else if (type.equals("onConversation")) {
			updateConversations();
		} else if (type.equals("forceReload")) {
			forceReload(msg);
		} else {
			Log.d("BlaChat", type + ":" + msg);
		}
	}

    private void forceReload(String msg) {
        Log.d("BlaChat", "TODO" + msg);
    }

    private void setStatus(int level) {
		status = level;
	}

	/**
	 * Called when an event is received in the message loop.
	 * 
	 * @param text
	 *            The message that was received.
	 * @param conversation
	 *            The conversation for which it was.
	 * @param trigger
     *            The person who triggered the event.
	 */
	private void onReceiveMessage(String trigger, String conversation,
			String text, String time) {
		if (!trigger.equals(getUser()) && (!conversation.equals(this.activeConversation) || status >= 120)) {
			if (text.startsWith("#image")) {
				text = "Image received";
			} else if (text.startsWith("#video")) {
				text = "Video received";
			} else if (text.startsWith("#file")) {
				text = "File received";
			}
			addNotification(conversation, text, true, time);
			status = 120;
		}
		// sort conversations list
		int i = conversationNicks.indexOf(conversation);
		if (i > 0) {
			String s1 = conversations.remove(i);
			String s2 = conversationNicks.remove(i);
			conversations.addFirst(s1);
			conversationNicks.addFirst(s2);

			saveConversations();
		}
		
		setLastMessage(conversation, text, time);
		Chat.saveChatAs(this, conversation, getChat(conversation));
		for (MessageListener l : listeners) {
			l.onMessageReceived(trigger, conversation);
		}
	}

	private String submit(String jsonString, String server) {
		if (!isActive()) {
			throw new NullPointerException("Logindata must be set first.");
		}
		String message = "";
		String totalMessage = "";
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
				totalMessage += next + "\\n";
				JSONObject jo = new JSONObject(next);

				String type = jo.getString("type");
				if (type.equals("onRejected")) {
					message = "REJECTED";
				} else if (type.equals("onEvent")) {
					message = jo.getString("msg");
				} else {
					message = jo.getString("msg");
				}
			}
		} catch (IOException e) {
            message = "ERROR";
            // Quiet
		} catch (JSONException e) {
			message = "ERROR";
            // Quiet
		} catch (IllegalStateException e) {
            Context context = BlaNetwork.getInstance().getApplicationContext();
            if (context != null) {
                SharedPreferences settings = PreferenceManager
                        .getDefaultSharedPreferences(context);
                settings.edit().clear().commit();

                Intent mStartActivity = new Intent(context, Conversations.class);
                int mPendingIntentId = 123456;
                PendingIntent mPendingIntent = PendingIntent.getActivity(context, mPendingIntentId,    mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
                AlarmManager mgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
                mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
            }
            System.exit(0);
        }
		if ((message == null || message.equals("")) && !offline) {
			offline = true;
			Log.d("BlaChat", jsonString);
			requestPause();
		} else if (offline) {
			offline = false;
			requestResumeLowFrequency();
		}

		return message;
	}

	private static String escape(String jsonString) {
		String result;

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
        message = message.replaceAll("\\\\", "\\\\\\\\");
        message = message.replaceAll("\"", "\\\\\"");
		if (!isActive()) {
			throw new NullPointerException("Logindata must be set first.");
		}
		synchronized (BlaNetwork.class) {
			while (!isRunning()) {
				try {
					BlaNetwork.class.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		String jsonString = "{\"type\":\"onMessage\", \"msg\":{\"user\":\""
				+ nick + "\" , \"password\": \"" + pw + "\" , \"id\": \"" + id
				+ "\" , \"conversation\": \"" + conversation
				+ "\", \"message\": \"" + message + "\"}}";
        String result = submit(jsonString, getServer());
        while (result.equals("ERROR")) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                return result;
            }
            result = submit(jsonString, getServer());
        }
		return result;
	}

	/**
	 * Rename a conversation
	 * 
	 * @param conversation
	 *            The conversation to rename.
	 * @param name
	 *            The new name.
	 */
	public String rename(String conversation, String name) {
		if (!isActive()) {
			throw new NullPointerException("Logindata must be set first.");
		}
		synchronized (BlaNetwork.class) {
			while (!isRunning()) {
				try {
					BlaNetwork.class.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		String jsonString = "{\"type\":\"onRenameConversation\", \"msg\":{\"user\":\""
				+ nick
				+ "\" , \"password\": \""
				+ pw
				+ "\" , \"id\": \""
				+ id
				+ "\" , \"conversation\": \""
				+ conversation
				+ "\" , \"name\": \"" + name + "\"}}";
		return submit(jsonString, getServer());
	}

	/**
	 * Add someone as a friend of you.
	 *
	 * @param name
	 *            The nick name of your friend.
	 */
	public String friend(String name) {
		if (!isActive()) {
			throw new NullPointerException("Logindata must be set first.");
		}
		synchronized (BlaNetwork.class) {
			while (!isRunning()) {
				try {
					BlaNetwork.class.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		String jsonString = "{\"type\":\"onAddFriend\", \"msg\":{\"user\":\""
				+ nick + "\" , \"password\": \"" + pw + "\" , \"id\": \"" + id
				+ "\" , \"name\": \"" + name + "\"}}";

        String result = submit(jsonString, getServer());

        // Update contacts before we leave.
        contactNames = null;
        contactNicks = null;
        updateContacts();

		return result;
	}

	/**
	 * Open a conversation
	 * 
	 * @param users
	 *            The list of users who should be in the conversation.
	 */
	public String open(List<String> users) {
		if (!isActive()) {
			throw new NullPointerException("Logindata must be set first.");
		}
		synchronized (BlaNetwork.class) {
			while (!isRunning()) {
				try {
					BlaNetwork.class.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		Collections.sort(users);
		String conversation = "";
		boolean first = true;
		for (String u : users) {
			if (first) {
				first = false;
			} else {
				conversation += ",";
			}
			conversation += u;
		}

		if (conversationNicks.contains(conversation)) {
			return "ERROR";
		}

		String jsonString = "{\"type\":\"onNewConversation\", \"msg\":{\"user\":\""
				+ nick
				+ "\" , \"password\": \""
				+ pw
				+ "\" , \"id\": \""
				+ id
				+ "\" , \"conversation\": \"" + conversation + "\"}}";
		return submit(jsonString, getServer());
	}

    private boolean loadNickAndPw() {
        try {
            SharedPreferences app_preferences = PreferenceManager
                    .getDefaultSharedPreferences(this);

            if (nick == null || pw == null) {
                nick = app_preferences.getString("nick", nick);
                pw = app_preferences.getString("pw", pw);
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

	/**
	 * Tries a login procedure. When the caller does not need to do anything,
	 * true is returned. When false is returned you should call this method
	 * again after some time.
	 *
	 * @return Weather the login was successful.
	 */
	public boolean tryLogin() {
        if (!loadNickAndPw()) return false;
		
		if (nick == null || pw == null) {
			return false;
		} else {
			id = getNetworkId();
			if ((id == null || id.equals("REJECTED") || id.equals(""))) {
				id = null;
				return false;
			} else {
				setReady(true);
				return true;
			}
		}
	}

	private String getNetworkId() {
		if (!isActive()) {
			return null;
		}
		String jsonString = "{\"type\":\"onIdRequest\", \"msg\":{\"user\":\""
				+ nick + "\" , \"password\": \"" + pw + "\"}}";
		return submit(jsonString, getServer());
	}

	/**
	 * Give the required userdata to the NetworkManager and start the event
	 * loop.
	 * 
	 * @param user
	 *            The user.
	 * @param pw
	 *            The password.
	 * @return Weather the login was successful or not.
	 */
	public boolean login(String user, String pw, Context activity) {
		nick = user;
		this.pw = pw;
		id = getNetworkId();

		if (id == null || id.equals("ERROR") || id.equals("REJECTED") || id.equals("")) {
			// The logindata must have been wrong. Refill form.
			id = null;
			return false;
		} else {
			SharedPreferences app_preferences = PreferenceManager
					.getDefaultSharedPreferences(activity);

			SharedPreferences.Editor editor = app_preferences.edit();
			editor.putString("pw", pw);
			editor.putString("nick", nick);
			editor.commit();
			
			setReady(true);
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
		Log.d("BlaChat", "Attached: " + listener.toString());
		listeners.add(listener);
	}

	/**
	 * Update the conversation list.
	 */
	public void updateConversations() {
		Log.d("BlaChat", "UpdateStart");
		synchronized (BlaNetwork.class) {
			while (!isRunning) {
				try {
					BlaNetwork.class.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		Log.d("BlaChat", "Update");
		String jsonString = "{\"type\":\"onGetChats\", \"msg\":{\"user\":\""
				+ nick + "\" , \"password\": \"" + pw + "\", \"id\": \"" + id
				+ "\"}}";
		String result = submit(jsonString, getServer());
		if (result == null || result.equals("")) {
			return;
		}
        synchronized (this) {
		    try {
			    Log.d("BlaChat", result);
			    JSONArray ja = new JSONArray(result);
			    conversations.clear();
			    conversationNicks.clear();
			    for (int i = 0; i < ja.length(); i++) {
				    conversations.add(((JSONObject) ja.get(i)).getString("name"));
				    conversationNicks.add(((JSONObject) ja.get(i))
						.getString("nick"));
			    }
			    saveConversations();
		    } catch (JSONException e1) {
                // Quiet
		    }
        }
	}

	/*public LinkedList<String> getConversations() {
		return conversations;
	}*/

	public String getConversationNickAt(int pos) {
		return conversationNicks.get(pos);
	}

	/**
	 * Get a chat history.
	 * 
	 * @param conversation The conversation which to retrieve.
	 *            The id of the chat.
	 * @return The chat history at the given id.
	 */
	public ChatMessage[] getChat(String conversation) {
		synchronized (BlaNetwork.class) {
			while (!isRunning) {
				try {
					BlaNetwork.class.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		LinkedList<ChatMessage> out = new LinkedList<ChatMessage>();
		String jsonString = "{\"type\":\"onGetHistory\", \"msg\":{\"user\":\""
				+ nick + "\" , \"password\": \"" + pw + "\", \"id\": \"" + id
				+ "\", \"conversation\":\"" + conversation + "\"}}";
		String result = submit(jsonString, getServer());
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
            // Quiet
            return null;
		}
		return out.toArray(new ChatMessage[out.size()]);
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

    public boolean isReady() {
        return isReady;
    }

	public boolean isOnline() {
		return !offline;
	}

	public void requestPause() {
		setStatus(10000);
	}

	public void requestResume() {
		setStatus(200);
	}

	public void requestResumeLowFrequency() {
		setStatus(10000);
	}

	LinkedList<LocalNotification> notifications = new LinkedList<LocalNotification>();

	public void addNotification(final String conversation, final String text,
			final boolean vibrate, final String time) {
		LocalNotification notification = new LocalNotification();
		notification.conversation = conversation;
		String name = getNameOfConversation(conversation);
		if (name == null) {
			name = conversation;
		} else {
			mark(conversation);
			setLastMessage(conversation, text, time);
		}
		notification.name = name;
		notification.message = text;
		loadNotifications();
		if (notifications.contains(notification)) {
			notifications.remove(notification);
		}
		notifications.add(notification);
		storeNotifications();
        if (!update) {
            update = true;
            new Thread() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(1500);
                    } catch(InterruptedException i) {
                        Log.d("BlaChat", "Add notification was interrupted");
                    }
                    update = false;
                    updateNotifications(vibrate);
                }
            }.start();
        }
	}

	private String getNameOfConversation(String conversation) {
		String name = null;

		if (conversations.size() == 0) {
			updateConversations();
		}
        synchronized (this) {
		    for (int i = 0; i < conversations.size(); i++) {
			    if (getConversationNickAt(i).equals(conversation)) {
				    name = conversations.get(i);
			    }
		    }
        }
		return name;
	}

	@SuppressWarnings("deprecation")
	private void displayNotification(String conversation, String name,
			String text, boolean vibrate) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		if (!preferences.getBoolean("notifications_new_message", true)) {
            return;
        }
        final Notification notification = new Notification(
				R.drawable.ic_launcher, text, System.currentTimeMillis());

		PendingIntent resultIntent;
		if (name != null && !conversation.equals("ERROR")) {
            Log.d("BlaChat", conversation+"|"+name);
			Intent intent = new Intent(this, Chat.class);
			intent.putExtra("chatname", name);
			intent.putExtra("chatnick", conversation);
            Log.d("BlaChat", conversation + "|" + name);
			resultIntent = PendingIntent.getActivity(this, 0,
					intent, Notification.FLAG_AUTO_CANCEL | PendingIntent.FLAG_UPDATE_CURRENT);
		} else {
            Log.d("BlaChat", conversation+"|"+name);
			Intent intent = new Intent(getBaseContext(), Conversation.class);
			resultIntent = PendingIntent.getActivity(getBaseContext(), 0,
					intent, Notification.FLAG_AUTO_CANCEL | PendingIntent.FLAG_UPDATE_CURRENT);
		}

		notification.setLatestEventInfo(getBaseContext(), name, text,
				resultIntent);

		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		// mId allows you to update the notification later on.

		Uri alarmSound = RingtoneManager
				.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		if (alarmSound != null) {
			notification.sound = alarmSound;
		} else {
			notification.sound = null;
		}
		if (!vibrate || !preferences.getBoolean("notifications_new_message_vibrate", true)) {
            notification.vibrate = new long[]{};
		} else {
			notification.defaults = Notification.DEFAULT_ALL;
		}

		notification.ledARGB = Color.MAGENTA;
		notification.ledOffMS = 500;
		notification.ledOnMS = 500;
		
		if (conversation.equals("ERROR")) {
			mNotificationManager.notify(mId + 1, notification);
		} else {
			mNotificationManager.notify(mId, notification);
		}
	}

	public void removeNotification(String conversation) {
		Log.d("BlaChat", "Removed: " + conversation);
		LocalNotification notification = new LocalNotification();
		loadNotifications();
		notification.conversation = conversation;
		if (notifications.contains(notification)) {
			notifications.remove(notification);
		}
		storeNotifications();
		updateNotifications(false);
		BlaWidget.updateWidgets();
	}

	private void storeNotifications() {
		String temp = "";
		for (LocalNotification n : notifications) {
			if (n.conversation.equals("ERROR"))
				continue;
			temp += n.conversation + SEPARATOR + n.message
					+ BlaNetwork.SEPARATOR + n.name + BlaNetwork.EOL;
		}

		SharedPreferences app_preferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = app_preferences.edit();
		editor.putString("notifications_" + nick, temp);
		editor.commit();
		Log.d("BlaChat", "Stored Notifications");
	}

	private void loadNotifications() {
		SharedPreferences app_preferences = PreferenceManager
				.getDefaultSharedPreferences(this);

		String[] splits = app_preferences
				.getString("notifications_" + nick, "").split(BlaNetwork.EOL);
		notifications.clear();
        for (String split : splits) {
            String[] sub = split.split(BlaNetwork.SEPARATOR);
            if (sub.length == 3) {
                LocalNotification n = new LocalNotification();
                n.conversation = sub[0];
                n.message = sub[1];
                n.name = sub[2];
                notifications.add(n);
            }
        }
		Log.d("BlaChat", "Loaded Notifications");
	}

	private void updateNotifications(boolean vibrate) {
		loadNotifications();
		if (notifications.isEmpty()) {
			NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			mNotificationManager.cancel(mId);
		} else {
			LocalNotification notification = notifications.getLast();
			displayNotification(notification.conversation, notification.name,
					notification.message, vibrate);
		}
		BlaWidget.updateWidgets();
	}

	/*public void addConversationNick(String string) {
		conversationNicks.add(string);
	}*/

	public String getUser() {
		return nick;
	}
    @SuppressWarnings("deprecation")
	@Override
	public void onStart(Intent intent, int startId) {
		handleCommand();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		handleCommand();
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		instance = null;
		super.onDestroy();
	}

	private void handleCommand() {
		instance = this;
		Thread t = new Thread(this);
		t.start();
	}

	@Override
	public void onCreate() {
		instance = this;
		refreshConversations();
		BlaWidget.updateWidgets();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	public void mark(String conversation) {
		markedConversations.add(conversation);
		saveConversations();
	}

	public void unmark(final String conversation) {
		unmarkLocal(conversation);
		removeNotification(conversation);
		new Thread() {
			@Override
			public void run() {

				String jsonString = "{\"type\":\"onRemoveEvent\",\"msg\":"
						+ "{\"user\":\"" + nick + "\" , \"password\": \"" + pw
						+ "\", \"id\": \"" + id + "\", \"conversation\":\""
						+ conversation + "\"}}";
				submit(jsonString, getServer());
			}

		}.start();
	}

	private void unmarkLocal(String conversation) {
		markedConversations.remove(conversation);
		saveConversations();
	}

	private void saveConversations() {
		LinkedList<ConversationViewData> list = new LinkedList<ConversationViewData>();
		for (int i = 0; i < conversations.size(); i++) {
			ConversationViewData temp = new ConversationViewData();
			temp.name = conversations.get(i);
			temp.nick = getConversationNickAt(i);
			temp.marked = getMarkedConversations().contains(temp.nick);
			temp.lastMessage = getLastMessage(temp.nick);
            temp.lastMessageTime = getLastMessageTime(temp.nick);
			list.add(temp);
		}
		Conversations.saveAsConversations(list, this);

	}

	public void refreshConversations() {
		List<ConversationViewData> list = Conversations
				.loadAsConversations(this);
		conversations.clear();
		conversationNicks.clear();
		markedConversations.clear();
		lastMessages.clear();
        lastMessagesTime.clear();
		for (ConversationViewData temp : list) {
			conversations.add(temp.name);
			conversationNicks.add(temp.nick);
			if (temp.marked) {
				markedConversations.add(temp.nick);
			}
			lastMessages.put(temp.nick, temp.lastMessage);
            lastMessagesTime.put(temp.nick, temp.lastMessageTime);
		}
		Conversations.saveAsConversations(list, this);
	}

	public LinkedList<String> getMarkedConversations() {
		return markedConversations;
	}

	/*public void setConversationNickAt(int pos, String string) {
		conversationNicks.set(pos, string);
	}*/

	static String lineEnd = "\r\n";
	static String twoHyphens = "--";
	static String boundary = "AaB03x87yxdkjnxvi7";

	@SuppressWarnings("deprecation")
	public void send(final Bitmap bmp, final String conversation) {
        boolean error = true;
        while (error) {
            String jsonString = "{\"type\":\"onData\", \"msg\":{\"user\":\"" + nick
                    + "\" , \"password\": \"" + pw + "\", \"conversation\":\""
                    + conversation + "\", \"type\":\"image\"}}";
            try {
                HttpURLConnection conn;
                DataOutputStream dos;
                DataInputStream dis = null;
                URL url = new URL(getServer() + "/api.php");
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
                    Log.d("BlaChat", result);
                } finally {
                    if (dis != null)
                        dis.close();
                }
                error = false;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                // Assume we have connection issues.
                e.printStackTrace();
                error = true;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                    return;
                }
            }
        }
	}

	/*public void send(final Video vid, final String conversation) {
		new Thread() {
			@SuppressWarnings("deprecation")
			public void run() {
                boolean error = true;
                while (error) {
                    error = false;
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
					// FileInputStream fileInputStream = null;
					URL url = new URL(getServer() + "/api.php");
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

						// String result = response.toString();
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
                    // Assume we have connection issues.
					e.printStackTrace();
                    error = true;
                    try {
                        sleep(1000);
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                        return;
                    }
				}
                }
			}
		}.start();
	}*/

    private final HashMap<String, String> lastMessages;
    private final HashMap<String, String> lastMessagesTime;

	/*private LinkedList<String> reloadFiles = new LinkedList<String>();

	public void requireFileReload(String preFile) {
		reloadFiles.add(preFile);
	}

	public boolean reloadFile(String preFile) {
		return reloadFiles.contains(preFile);
	}

	public void fileReloaded(String preFile) {
		reloadFiles.remove(preFile);
	}*/

    public String getLastMessage(String nick) {
        if (!lastMessages.containsKey(nick)) {
            return " ";
        }
        String result = lastMessages.get(nick);
        if (result.startsWith("#image")) {
            result = "(image)";
        } else if (result.startsWith("#video")) {
            result = "(video)";
        } else if (result.startsWith("#file")) {
            result = "(file)";
        } else if (result.startsWith("#hangout")) {
            result = "(hangout)";
        }
        return result;
    }

    public String getLastMessageTime(String nick) {
        if (!lastMessagesTime.containsKey(nick)) {
            return " ";
        }
        return lastMessagesTime.get(nick);
    }

	public void setLastMessage(String nick, String message, String time) {
		lastMessages.put(nick, message);
        if (time != null) {
            lastMessagesTime.put(nick, time);
        }
		saveConversations();
        if (activity instanceof Conversations) {
            Conversations tmp = (Conversations)activity;
            tmp.updateData();
        }
	}

	public void detachMessageListener(MessageListener toRemove) {
		Log.d("BlaChat", "Detached: " + toRemove.toString());
		listeners.remove(toRemove);
	}

	public String getNotificationText() {
		if (notifications.size() > 0) {
			String result = "";
            for (LocalNotification notification : notifications) {
                result += notification.name + ": "
                        + notification.message + "\n";
            }
			return result;
		} else {
			return "No news";
		}
	}

	private String[] contactNames = null;
	private String[] contactNicks = null;

	public String[] getContactNames() {
		if (contactNames == null) {
			SharedPreferences app_preferences = PreferenceManager.getDefaultSharedPreferences(this);

			String[] splits = app_preferences.getString("contacts", "").split(
					BlaNetwork.EOL);
			contactNames = new String[splits.length];
			contactNicks = new String[splits.length];

			for (int i = 0; i < splits.length; i++) {
				String[] s = splits[i].split(SEPARATOR);
				if (s.length >= 2) {
					contactNames[i] = s[0];
					contactNicks[i] = s[1];
				}
			}

			if (contactNames.length == 0) {
				updateContacts();
			} else {
				new Thread() {
					@Override
					public void run() {
						updateContacts();
					}
				}.start();
			}
		} else {
            updateContacts();
		}
		return contactNames;
	}

	private void updateContacts() {

		String jsonString = "{\"type\":\"onGetContacts\", \"msg\":{\"user\":\""
				+ nick + "\" , \"password\": \"" + pw + "\" }}";

		String result = submit(jsonString, getServer());

		if (result.equals("ERROR")) {
			return;
		}
		JSONArray ja;
		try {
			ja = new JSONArray(result);
			contactNames = new String[ja.length()];
			contactNicks = new String[ja.length()];
			for (int i = 0; i < ja.length(); i++) {
				JSONObject jo = ja.getJSONObject(i);
				contactNames[i] = jo.getString("name");
				contactNicks[i] = jo.getString("nick");
			}
		} catch (JSONException e) {
            // Quiet
			return;
		}

		String temp = "";
		for (int i = 0; i < contactNames.length; i++) {
			temp += contactNames[i] + SEPARATOR + contactNicks[i] + EOL;
		}
		SharedPreferences app_preferences = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = app_preferences.edit();
		editor.putString("contacts", temp);
		editor.commit();

	}

	public String[] getContactNicks() {
		if (contactNicks == null) {
			getContactNames();
		}
		return contactNicks;
	}

	public static String getUser(Context ctx) {
		if (nick == null) {
			SharedPreferences app_preferences = PreferenceManager
					.getDefaultSharedPreferences(ctx);

			if ((app_preferences.getString("nick", null) == null)) {
				return "";
			} else {
				nick = app_preferences.getString("nick", null);
			}
		}
		return nick;
	}

	/**
	 * Rename self.
	 * 
	 * @param name
	 *            The new name.
	 * @return The result of the request.
	 */
	public String renameSelf(String name) {
		if (!isActive()) {
			throw new NullPointerException("Logindata must be set first.");
		}
		synchronized (BlaNetwork.class) {
			while (!isRunning()) {
				try {
					BlaNetwork.class.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		String jsonString = "{\"type\":\"onSetName\", \"msg\":{\"user\":\""
				+ nick + "\" , \"password\": \"" + pw + "\" , \"id\": \"" + id
				+ "\" , \"name\": \"" + name + "\"}}";
		return submit(jsonString, getServer());
	}

    @SuppressWarnings("deprecation")
	public String setImage(Bitmap bmp, String conversation) {
		conversation = conversation.replaceAll(",", "-");
		String jsonString = "{\"type\":\"onSetGroupImage\", \"msg\":{\"user\":\""
				+ nick
				+ "\" , \"password\": \""
				+ pw
				+ "\", \"conversation\":\""
				+ conversation
				+ "\", \"type\":\"image\"}}";

		String result = "ERROR";
		try {
			HttpURLConnection conn;
			DataOutputStream dos;
			DataInputStream dis = null;
			URL url = new URL(getServer() + "/api.php");
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

				result = response.toString();
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
		return result;
	}

    @SuppressWarnings("deprecation")
	public String setImage(Bitmap bmp) {
		String jsonString = "{\"type\":\"onSetProfileImage\", \"msg\":{\"user\":\""
				+ nick
				+ "\" , \"password\": \""
				+ pw
				+ "\", \"type\":\"image\"}}";

		String result = "ERROR";
		try {
			HttpURLConnection conn;
			DataOutputStream dos;
			DataInputStream dis = null;
			URL url = new URL(getServer() + "/api.php");
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

				result = response.toString();
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
		Log.d("BlaChat", result);
		return result;
    }

    public boolean canLogin() {
        return loadNickAndPw() && pw != null && nick != null;
    }

    public static BlaNetwork getInstanceInitialized() {
        BlaNetwork networkAdapter;
        synchronized (BlaNetwork.class) {
            while ((networkAdapter = BlaNetwork.getInstance()) == null) {
                try {
                    BlaNetwork.class.wait();
                } catch (InterruptedException e) {
                    return null;
                }
            }
            while (!networkAdapter.isReady() && networkAdapter.canLogin()) {
                try {
                    BlaNetwork.class.wait();
                } catch (InterruptedException e) {
                    return null;
                }
            }
        }
        return networkAdapter;
    }
}

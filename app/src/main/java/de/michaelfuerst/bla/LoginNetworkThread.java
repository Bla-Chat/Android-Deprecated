/**
 * 
 */
package de.michaelfuerst.bla;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Multithread the network login.
 * 
 * @author Michael
 * @version 1.0
 */
public class LoginNetworkThread extends Thread {
	private Context activity = null;
	private String nick = null;
	private String pw = null;

	public LoginNetworkThread(Context activity) {
		this.activity = activity;
	}

	public LoginNetworkThread(Context activity, String nick, String pw) {
		this.activity = activity;
        if (nick != null) {
            String[] nicksplit = nick.split("@");
            this.nick = nicksplit[0];
            if (nicksplit.length > 1) {
                // User wants to use other server
                BlaNetwork.setServer(nick.split("@")[1], activity);
            } else {
                BlaNetwork.setServer(BlaNetwork.DEFAULT_BLA_SERVER, activity);
            }
        }
		this.pw = pw;
	}

	@Override
	public void run() {
		if (nick != null && pw != null) {
			for(int i = 0; i < 3 && !BlaNetwork.getInstance().login(nick, pw, activity); i++) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					return;
				}
				if (i == 2) {
					Log.d("LoginNetworkThread", "Login failed completly.");
                    if ((activity != null && activity != BlaNetwork.getInstance())) {
                        Intent intent = new Intent(activity.getApplicationContext(),
                                Login.class);
                        activity.startActivity(intent);
                    }
					return;
				}
			}
			Log.d("LoginNetworkThread", "Login successfull.");
			activity.startActivity(new Intent(activity.getApplicationContext(),
					Conversations.class));
		} else {
			for(int i = 0; i < 3 && !BlaNetwork.getInstance().tryLogin(activity); i++) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					return;
				}
				if (i == 2) {
					Log.d("LoginNetworkThread", "Login failed completly. (with parameters)");
                    if ((activity != null && activity != BlaNetwork.getInstance())) {
                        Intent intent = new Intent(activity.getApplicationContext(),
                                Login.class);
                        activity.startActivity(intent);
                    }
					return;
				}
			}
			Log.d("LoginNetworkThread", "Login successfull with parameters.");
		}
	}
}

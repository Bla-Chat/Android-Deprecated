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
	private Login activity = null;
	private String nick = null;
	private String pw = null;

	public LoginNetworkThread(Context activity) {
	}

	public LoginNetworkThread(Login activity, String nick, String pw) {
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
					return;
				}
			}
			Log.d("BlaChat", "Login successful.");
			activity.finish();
		} else {
			while(!BlaNetwork.getInstance().tryLogin()) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					return;
				}
			}
			Log.d("BlaChat", "Login successful with parameters.");
		}
	}
}

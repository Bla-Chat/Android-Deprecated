/**
 * 
 */
package de.michaelfuerst.bla;

import android.content.Context;
import android.content.Intent;

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
		this.nick = nick;
		this.pw = pw;
	}

	@Override
	public void run() {
		if (nick != null && pw != null) {
			BlaNetwork.getInstance().login(nick, pw, activity);
			activity.startActivity(new Intent(activity.getApplicationContext(),
					Conversations.class));
		} else {
			while(!BlaNetwork.getInstance().tryLogin(activity)) {
				/*try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					return;
				}*/
				return;
			}
		}
	}
}

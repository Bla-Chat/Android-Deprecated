/**
 * 
 */
package de.michaelfuerst.hangout;

import android.os.AsyncTask;

/**
 * @author Michael
 * 
 */
public class ConversationInitThread extends AsyncTask<Object, Integer, Void> {
	private Conversations parent;

	public ConversationInitThread(Conversations parent) {
		this.parent = parent;
	}

	@Override
	protected Void doInBackground(Object... params) {
		HangoutNetwork networkAdapter = HangoutNetwork.getInstance();
		networkAdapter.updateConversations();
		parent.saveConversations();
		return null;
	}

	@Override
	protected void onPostExecute(Void v) {
		parent.updateData();
	}
}

/**
 * 
 */
package de.michaelfuerst.bla;

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
		BlaNetwork networkAdapter = BlaNetwork.getInstance();
		networkAdapter.updateConversations();
		return null;
	}

	@Override
	protected void onPostExecute(Void v) {
		parent.updateData();
	}
}

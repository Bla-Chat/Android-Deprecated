/**
 * 
 */
package de.michaelfuerst.bla;

import android.os.AsyncTask;
import android.util.Log;

/**
 * @author Michael
 * 
 */
public class ChatHistoryThread extends
		AsyncTask<Object, Integer, ChatMessage[]> {

	private Chat parent;

	@Override
	protected ChatMessage[] doInBackground(Object... arg) {
		parent = (Chat) arg[0];
		String conversationName = (String) arg[1];
		ChatMessage[] result = null;
		if (BlaNetwork.getInstance() != null
				&& BlaNetwork.getInstance().isOnline()) {
			result = BlaNetwork.getInstance().getChat(conversationName);
		}
		return result;
	}

	@Override
	protected void onProgressUpdate(Integer... progress) {

	}

	@Override
	protected void onPostExecute(ChatMessage[] result) {
		if (result != null) {
			Log.d("ChatHistoryThread", "Updating history");
			parent.drawHistory(result);
		}
	}

}

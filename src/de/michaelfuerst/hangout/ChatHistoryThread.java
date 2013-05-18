/**
 * 
 */
package de.michaelfuerst.hangout;


import android.os.AsyncTask;

/**
 * @author Michael
 *
 */
public class ChatHistoryThread extends AsyncTask<Object, Integer, ChatMessage[]> {

	private Chat parent;
	
	@Override
	protected ChatMessage[] doInBackground(Object... arg) {
		parent = (Chat) arg[0];
		String conversationName = (String) arg[1];
		
		ChatMessage[] result = HangoutNetwork.getInstance().getChat(conversationName);
		return result;
	}
	
	@Override
	protected void onProgressUpdate(Integer... progress) {
		
	}
	
	@Override
	protected void onPostExecute(ChatMessage[] result) {
		if (result != null) {
			parent.drawHistory(result);
		}
	}

}

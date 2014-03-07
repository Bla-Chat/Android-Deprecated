/**
 * 
 */
package de.michaelfuerst.bla;

/**
 * @author Michael
 * 
 */
public class SendMessageThread extends Thread {
	private String message;
	private String nick;
	private Chat parent;

	public SendMessageThread(Chat parent, String message, String nick) {
		this.message = message;
		this.nick = nick;
		this.parent = parent;
	}

	@Override
	public void run() {
		BlaNetwork.getInstance().send(message, nick);
		parent.updateHistory();
	}

}

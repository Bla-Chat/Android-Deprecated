/**
 * 
 */
package de.michaelfuerst.hangout;

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
		HangoutNetwork.getInstance().send(message, nick);
		parent.updateHistory();
	}

}

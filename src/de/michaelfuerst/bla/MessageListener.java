/**
 * 
 */
package de.michaelfuerst.bla;

/**
 * When there has arrived a message.
 * 
 * @author michael
 * @version 1.0
 */
public interface MessageListener {
	public void onMessageReceived(String message, String conversation);
}

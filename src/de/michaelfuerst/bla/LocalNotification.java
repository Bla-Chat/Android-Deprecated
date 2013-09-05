package de.michaelfuerst.bla;

public class LocalNotification {
	public String conversation = "";
	public String message = "";
	public String name = "";

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof LocalNotification) {
			LocalNotification o = (LocalNotification) obj;
			if (o.conversation.equals(this.conversation)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return "[" + conversation + "|" + message + "|" + name + "]";
	}
}

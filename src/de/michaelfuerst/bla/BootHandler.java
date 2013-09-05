/**
 * 
 */
package de.michaelfuerst.bla;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * 
 * Retrieves the onboot event and starts service.
 * 
 * @author Michael
 * 
 */
public class BootHandler extends BroadcastReceiver {

	@Override
	public void onReceive(Context ctx, Intent intent) {
		ctx.startService(new Intent(ctx, BlaNetwork.class));
	}
}

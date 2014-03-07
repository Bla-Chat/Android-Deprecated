/**
 * 
 */
package de.michaelfuerst.bla;

import android.content.Context;
import android.graphics.Color;
import android.view.View;

/**
 * @author Michael
 * 
 */
public class Delimiter extends View {

	public Delimiter(Context context) {
		super(context);
		setPadding(10, 2, 10, 2);
		setMinimumHeight(1);
		setBackgroundColor(Color.rgb(200, 200, 200));
	}

}

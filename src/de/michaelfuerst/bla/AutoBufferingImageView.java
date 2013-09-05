/**
 * 
 */
package de.michaelfuerst.bla;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

/**
 * @author Michael F�rst
 * @version 1.0
 */
public class AutoBufferingImageView extends ImageView {
	private Bitmap bmp = null;
	private String path = null;
	private Context ctx = null;
	private double maxSize = 1.0d;
	private boolean loading = false;
	private int buffer = 0;

	public AutoBufferingImageView(Context context) {
		super(context);
		ctx = context;
		path = "none";
	}

	public void setImage(String path, double maxSize, int buffer) {
		this.path = path;
		this.maxSize = maxSize;
		this.buffer = buffer;
	}

	@Override
	public void setImageDrawable(Drawable drawable) {
		if (drawable != null) {
			bmp = ((BitmapDrawable) drawable).getBitmap();
		} else {
			bmp = null;
		}
		super.setImageDrawable(drawable);
		loading = false;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (path != null) {
			if (path.equals("none")) {
				super.onDraw(canvas);
			} else {
				if (bmp != null && !bmp.isRecycled()) {
					super.onDraw(canvas);
				} else {
					if (!loading) {
						Log.d("ImageLoading", "Loading: " + path);
						loading = true;
						new AsyncTask<Void, Void, Drawable>() {

							@Override
							protected Drawable doInBackground(Void... params) {
								return LocalResourceManager.getDrawable(ctx,
										path, maxSize, buffer);
							}

							@Override
							protected void onPostExecute(Drawable result) {
								setImageDrawable(result);
								loading = false;
							}
						}.execute();
					}
				}
			}
		} else {
			Log.d("Null", "Nullpath");
		}
	}
}

/**
 * 
 */
package de.michaelfuerst.hangout;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.widget.ImageView;

/**
 * @author Michael Fürst
 * @version 1.0
 */
public class AutoBufferingImageView extends ImageView {
	private Bitmap bmp = null;
	private String path = null;
	private Context ctx = null;
	private double maxSize = 1.0d;
	private boolean loading = false;

	public AutoBufferingImageView(Context context) {
		super(context);
		ctx = context;
	}

	public void setImage(String path, double maxSize) {
		this.path = path;
		this.maxSize = maxSize;
	}

	@Override
	public void setImageDrawable(Drawable drawable) {
		bmp = ((BitmapDrawable) drawable).getBitmap();
		super.setImageDrawable(drawable);
		loading = false;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (path != null) {
			if (path.equals("none")) {
				super.onDraw(canvas);
			} else {
				if (!bmp.isRecycled()) {
					super.onDraw(canvas);
				} else {
					loading = true;
					new AsyncTask<Void, Void, Drawable>() {

						@Override
						protected Drawable doInBackground(Void... params) {
							return LocalResourceManager.getDrawable(ctx, path,
									maxSize);
						}

						@Override
						protected void onPostExecute(Drawable result) {
							setImageDrawable(result);
						}
					}.execute();
				}
			}
		}
	}
}

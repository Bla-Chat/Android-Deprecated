package de.michaelfuerst.hangout;

import java.util.HashMap;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;

public class LocalResourceManager {
	private static HashMap<String, Drawable> map = new HashMap<String, Drawable>();
	@SuppressWarnings("deprecation")
	public static Drawable getDrawable(Context ctx, String path, double maxSize) {
		Resources r = ctx.getResources();
		double px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, r.getDisplayMetrics());
		maxSize = maxSize*px;
		if (!map.containsKey(path)) {
			Bitmap bitmap = BitmapFactory.decodeFile(path);
			if (bitmap == null) {
				return null;
			}
			double scalar = 1.0;
			if (bitmap.getWidth() > maxSize) {
				scalar = bitmap.getWidth()/maxSize;
			}
			bitmap.recycle();
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inPreferredConfig = Bitmap.Config.ARGB_8888;
			options.inSampleSize = (int) Math.round(scalar - 0.25);
			Bitmap bmp = BitmapFactory.decodeFile(path, options);
			if (bmp == null) {
				return null;
			}
			map.put(path, new BitmapDrawable(bmp));
		}
		return map.get(path);
	}
	
	public static void clear() {
		map.clear();
	}
}

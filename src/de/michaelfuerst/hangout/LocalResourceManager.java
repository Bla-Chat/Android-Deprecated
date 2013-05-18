package de.michaelfuerst.hangout;

import java.util.HashMap;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class LocalResourceManager {
	private static HashMap<String, Drawable> map = new HashMap<String, Drawable>();
	@SuppressWarnings("deprecation")
	public static Drawable getDrawable(String path) {
		if (!map.containsKey(path)) {
			Bitmap bitmap = BitmapFactory.decodeFile(path);
			if (bitmap == null) {
				return null;
			}
			double scalar = bitmap.getWidth()/500;
			bitmap.recycle();
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inPreferredConfig = Bitmap.Config.ARGB_8888;
			options.inSampleSize = (int) scalar;
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

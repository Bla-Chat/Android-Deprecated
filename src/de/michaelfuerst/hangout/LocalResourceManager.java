package de.michaelfuerst.hangout;

import java.util.HashMap;
import java.util.Set;

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
		if (!map.containsKey(path) || ((BitmapDrawable)map.get(path)).getBitmap().isRecycled()) {
			BitmapFactory.Options bmpFac = new BitmapFactory.Options();
			bmpFac.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(path, bmpFac); 
			int scalar = 1;
			while (bmpFac.outWidth/(scalar*2) > maxSize || bmpFac.outHeight/(scalar*2) > maxSize) {
				scalar *= 2;
			}
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inPreferredConfig = Bitmap.Config.ARGB_8888;
			options.inSampleSize = scalar;
			Bitmap bmp = BitmapFactory.decodeFile(path, options);
			if (bmp == null) {
				return null;
			}
			map.put(path, new BitmapDrawable(bmp));
		}
		return map.get(path);
	}
	
	public static void clear() {
		Set<String> keys = map.keySet();
		for (String k: keys) {
			((BitmapDrawable)map.get(k)).getBitmap().recycle();
		}
		map.clear();
	}
}

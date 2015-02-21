package de.michaelfuerst.bla;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.TypedValue;

public class LocalResourceManager {
	@SuppressWarnings("unchecked")
	private HashMap<String, Drawable>[] map = new HashMap[2];
	@SuppressWarnings("unchecked")
	private HashMap<String, Integer>[] usages = new HashMap[2];
	private LinkedList<String> fetchQueue = new LinkedList<String>();
	public int maxPriority = 10;

	public void setPriority(String key, int value, int i) {
		if (usages[i] == null) {
			usages[i] = new HashMap<String, Integer>();
		}
		usages[i].put(key, value);
	}

	@SuppressWarnings("deprecation")
	public Drawable getDrawable(Context ctx, final String path,
			double maxSize, int i) {
		Resources r = ctx.getResources();
		double px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1,
				r.getDisplayMetrics());
		maxSize = maxSize * px;
		if (map[i] == null) {
			map[i] = new HashMap<String, Drawable>();
		}
		setPriority(path, maxPriority, i);
		if (!map[i].containsKey(path)
				|| ((BitmapDrawable) map[i].get(path)).getBitmap().isRecycled()) {
			String preFile = path.split("/")[path.split("/").length - 1];
            final String filename = ctx.getApplicationInfo().dataDir + "/Pictures/" + preFile.split("\\.")[0] + ".png";

			File sysPath = new File(ctx.getApplicationInfo().dataDir
					+ "/Pictures");
			if (!sysPath.exists()) {
				if(!sysPath.mkdirs()) return null;
			}
			if (!new File(filename).exists()) {
				if (BlaNetwork.getInstance() == null
						|| !BlaNetwork.getInstance().isOnline()) {
					return null;
				} else {
					if (fetchQueue.contains(path)) {
						return null;
					}
					fetchQueue.add(path);
					new Thread() {
						@Override
						public void run() {
							try {
								URL url = new URL(path);
								File file = new File(filename);
								Bitmap bitmap = BitmapFactory.decodeStream(url
										.openStream());
								bitmap.compress(CompressFormat.PNG, 100,
										new FileOutputStream(file));
								bitmap.recycle();
								fetchQueue.remove(path);
							} catch (MalformedURLException e) {
                                Log.d("LocalResourceManager", "invalid url" + path);
							} catch (IOException e) {
								Log.d("LocalResourceManager", "connection unstable");
							}
						}
					}.start();
					return null;
				}
			}

			BitmapFactory.Options bmpFac = new BitmapFactory.Options();
			bmpFac.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(filename, bmpFac);
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inPreferredConfig = Bitmap.Config.ARGB_8888;
			options.inSampleSize = calculateInSampleSize(bmpFac, maxSize,
					maxSize);
			if (options.inSampleSize < 1) {
				options.inSampleSize = 1;
			}
			options.inJustDecodeBounds = false;
			Bitmap bmp = BitmapFactory.decodeFile(filename, options);
			if (bmp == null) {
				return null;
			}
			map[i].put(path, new BitmapDrawable(bmp));
		}
		return map[i].get(path);
	}

	private int calculateInSampleSize(BitmapFactory.Options options,
			double reqWidth, double reqHeight) {
		// Raw height and width of image
		final double height = options.outHeight;
		final double width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {

			// Calculate ratios of height and width to requested height and
			// width
			final int heightRatio = (int) Math.round(height / reqHeight);
			final int widthRatio = (int) Math.round(width / reqWidth);

			// Choose the smallest ratio as inSampleSize value, this will
			// guarantee
			// a final image with both dimensions larger than or equal to the
			// requested height and width.
			inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
		}

		return inSampleSize;
	}
}

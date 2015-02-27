package de.michaelfuerst.bla;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.util.Log;
import android.util.TypedValue;

public class LocalResourceManager {
    private final ExecutorService threadPool =  Executors.newFixedThreadPool(1);
	@SuppressWarnings("unchecked")
	private HashMap<String, Future<Drawable>> map = new HashMap();
	@SuppressWarnings("unchecked")
	private HashMap<String, Integer> usages = new HashMap();
	public int maxPriority = 10;

    public String getImagePath(Context ctx, String path) {
        return Environment.getExternalStorageDirectory()
                + "/Pictures/BlaChat/" + path.split("/")[path.split("/").length - 1].split("\\.")[0] + ".png";
    }

	public void setPriority(String key, int value) {
		usages.put(key, value);
	}

    public Future<Drawable> getDrawable(final Context ctx, final String path, final double maxSize) {
        if (!map.containsKey(path)) {
            map.put(path, threadPool.submit(new Callable<Drawable>() {
                @Override
                public Drawable call() throws Exception {
                    return downloadDrawable(ctx, path, maxSize);
                }
            }));
        }
        return map.get(path);
    }

	@SuppressWarnings("deprecation")
	private Drawable downloadDrawable(Context ctx, final String path, double maxSize) {
        Resources r = ctx.getResources();
        double px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1,
                r.getDisplayMetrics());
        maxSize = maxSize * px;
        setPriority(path, maxPriority);

        final String filename = getImagePath(ctx, path);

        File sysPath = new File(Environment.getExternalStorageDirectory()
                + "/Pictures/BlaChat/");

        if (!sysPath.exists()) {
            if (!sysPath.mkdirs()) return null;
        }
        if (!new File(filename).exists()) {
            while (BlaNetwork.getInstance() == null || !BlaNetwork.getInstance().isOnline()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            boolean errors = true;
            for (int i = 0; i < 3 && errors; i++) {
                try {
                    URL url = new URL(path);
                    File file = new File(filename);
                    Bitmap bitmap = BitmapFactory.decodeStream(url
                            .openStream());
                    bitmap.compress(CompressFormat.PNG, 100,
                            new FileOutputStream(file));
                    bitmap.recycle();
                    errors = false;
                } catch (MalformedURLException e) {
                    Log.d("BlaChat", "invalid url" + path);
                } catch (IOException e) {
                    Log.d("BlaChat", "connection unstable");
                }
                if (errors) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (errors) {
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
        return new BitmapDrawable(bmp);
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

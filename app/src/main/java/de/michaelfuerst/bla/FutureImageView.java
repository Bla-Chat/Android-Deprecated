package de.michaelfuerst.bla;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author Michael
 * @version 1.0
 */
public class FutureImageView extends Thread {
    private ImageView iv;
    private Future<Drawable> future;
    private Activity activity;
    public FutureImageView(Activity activity, ImageView iv, Future<Drawable> future) {
        this.iv = iv;
        this.future = future;
        this.activity = activity;
    }

    public void run() {
        final Drawable x;
        try {
            x = future.get();
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    iv.setImageDrawable(x);
                }
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }
}

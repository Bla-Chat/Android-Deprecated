package de.michaelfuerst.bla;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

/**
 * A utility calss for special android methods.
 *
 * @author Michael Fürst
 * @version 1.0
 */
public class Utils {
    private static Uri getUri() {
        String state = Environment.getExternalStorageState();
        if(!state.equalsIgnoreCase(Environment.MEDIA_MOUNTED))
            return MediaStore.Images.Media.INTERNAL_CONTENT_URI;

        return MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    }

    public static String getUriAdv(Activity context, Intent data) {
        Uri originalUri = data.getData();

        if (originalUri == null) return null;
        String lastSegment = originalUri.getLastPathSegment();

        if (lastSegment == null) return null;
        String id = lastSegment.split(":")[1];

        final String[] imageColumns = {MediaStore.Images.Media.DATA };

        Uri uri = getUri();
        String selectedImagePath = null;

        @SuppressWarnings("deprecation")
        Cursor imageCursor = context.managedQuery(uri, imageColumns,
                MediaStore.Images.Media._ID + "=" + id, null, null);

        if (imageCursor.moveToFirst()) {
            selectedImagePath = imageCursor.getString(imageCursor.getColumnIndex(MediaStore.Images.Media.DATA));
        }

        return selectedImagePath;
    }
}

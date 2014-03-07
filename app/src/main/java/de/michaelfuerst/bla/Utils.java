package de.michaelfuerst.bla;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

/**
 * Created by michael on 07.03.14.
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

        String id = originalUri.getLastPathSegment().split(":")[1];
        final String[] imageColumns = {MediaStore.Images.Media.DATA };
        final String imageOrderBy = null;

        Uri uri = getUri();
        String selectedImagePath = null;

        Cursor imageCursor = context.managedQuery(uri, imageColumns,
                MediaStore.Images.Media._ID + "=" + id, null, imageOrderBy);

        if (imageCursor.moveToFirst()) {
            selectedImagePath = imageCursor.getString(imageCursor.getColumnIndex(MediaStore.Images.Media.DATA));
        }

        return selectedImagePath;
    }
}

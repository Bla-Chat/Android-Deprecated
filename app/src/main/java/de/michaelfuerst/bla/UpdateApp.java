/**
 * 
 */
package de.michaelfuerst.bla;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

/**
 * @author Michael
 * 
 */
public class UpdateApp extends AsyncTask<String, Void, Void> {
	public static final String VERSION = "2.1.0.1";
	private Context context;

	public void setContext(Context contextf) {
		context = contextf;
	}
	
	private boolean needsUpdate(String server) {
		try {
			// Create a new HttpClient and Post Header
            Log.d("BlaChat", server + "/version.txt");
            HttpURLConnection conn = (HttpURLConnection)new URL(server + "/version.txt").openConnection();
            conn.setDoInput(true);
            conn.setConnectTimeout(10000); // timeout 10 secs
            conn.connect();

			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

			String next;

			while ((next = bufferedReader.readLine()) != null) {
                if (next.contains("Page not found")) {
                    Log.d("BlaChat", "Page not found error!");
                    return false;
                }
                String[] split = next.split("\\.");
                String[] versions = VERSION.split("\\.");

				if (split.length > 2 && versions.length > 2 && isLargerOrEqual(split, versions)) {
                    Log.d("BlaChat", "Your version is still supported.");
					return false;
				}
			}
		} catch (IOException e) {
			return false;
		}

		return true;
	}

    /**
     * Determine if versions is lager or equal to split.
     *
     * @param split The split value from the server.
     * @param versions The versions from this app.
     * @return Weather versions is larger or equal to split.
     */
    private boolean isLargerOrEqual(String[] split, String[] versions) {
        if (Integer.parseInt(versions[0]) < Integer.parseInt(split[0]))
            return false;
        if (Integer.parseInt(versions[0]) > Integer.parseInt(split[0]))
            return true;
        if (Integer.parseInt(versions[1]) < Integer.parseInt(split[1]))
            return false;
        if (Integer.parseInt(versions[1]) > Integer.parseInt(split[1]))
            return true;
        if (Integer.parseInt(versions[2]) < Integer.parseInt(split[2]))
            return false;
        if (Integer.parseInt(versions[2]) > Integer.parseInt(split[2]))
            return true;
        if (Integer.parseInt(versions[3]) < Integer.parseInt(split[3]))
            return false;
        return true;
    }

    @Override
	protected Void doInBackground(String... arg0) {
		try {
			if (!needsUpdate(arg0[0])) {
				return null;
			}
            ConnectivityManager mgrConn = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mWifi = mgrConn.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (mWifi == null || !mWifi.isConnected()) {
                Log.d("BlaChat", "Skipped update due to missing WLAN!");
                return null;
            }
			Log.d("BlaChat", "Updating!");
			URL url = new URL(arg0[0] + "/bla.apk");
			HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setDoInput(true);
            c.setConnectTimeout(10000); // timeout 10 secs
            c.connect();

			String PATH = context.getApplicationInfo().dataDir + "/Download/";
			File file = new File(PATH);
			File outputFile = new File(file, "bla_update.apk");
			if (outputFile.exists()) {
				if(!outputFile.delete()) {
                    Log.d("BlaChat", "Cannot delete old file!");
                    return null;
                }
			} else {
                if(!(!file.exists() && !file.mkdirs())) {
                    Log.d("BlaChat", "Cannot create directories!");
                }
            }
			FileOutputStream fos = new FileOutputStream(outputFile);

			InputStream is = c.getInputStream();

			byte[] buffer = new byte[1024];
			int len1;
			while ((len1 = is.read(buffer)) != -1) {
				fos.write(buffer, 0, len1);
			}
			fos.close();
			is.close();

			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setDataAndType(Uri.fromFile(new File(
					PATH+"bla_update.apk")),
					"application/vnd.android.package-archive");
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // without this flag
															// android returned
															// a intent error!
			context.startActivity(intent);

		} catch (Exception e) {
			Log.e("BlaChat", "Update error! " + e.getMessage());
		}
		return null;
	}
}

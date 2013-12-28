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

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

/**
 * @author Michael
 * 
 */
public class UpdateApp extends AsyncTask<String, Void, Void> {
//	private static final String VERSION = "1.0.0.1";
	private static final String VERSION = "dev";
	private Context context;

	public void setContext(Context contextf) {
		context = contextf;
	}
	
	private boolean needsUpdate(String server) {
		try {
			// Create a new HttpClient and Post Header
			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = new HttpPost(server + "/version.txt");

			// Execute HTTP Post Request
			HttpResponse response = httpclient.execute(httppost);

			BufferedReader bufferedReader = new BufferedReader(
					new InputStreamReader(response.getEntity().getContent()));

			String next = null;
			while ((next = bufferedReader.readLine()) != null) {
				if (next.equals(VERSION)) {
					return false;
				}
			}
		} catch (IOException e) {
			return false;
		}

		return true;
	}

	@Override
	protected Void doInBackground(String... arg0) {
		try {
			if (!needsUpdate(BlaNetwork.BLA_SERVER)) {
				Log.d("Updater", "Skipping update already up to date!");
				return null;
			}
			Log.d("Updater", "Updating!");
			URL url = new URL(arg0[0]);
			HttpURLConnection c = (HttpURLConnection) url.openConnection();
			c.setRequestMethod("GET");
			c.setDoOutput(true);
			c.connect();

			String PATH = Environment.getExternalStorageDirectory()+"/Download/";
			File file = new File(PATH);
			file.mkdirs();
			File outputFile = new File(file, "bla_update.apk");
			if (outputFile.exists()) {
				outputFile.delete();
			}
			FileOutputStream fos = new FileOutputStream(outputFile);

			InputStream is = c.getInputStream();

			byte[] buffer = new byte[1024];
			int len1 = 0;
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
			Log.e("UpdateAPP", "Update error! " + e.getMessage());
		}
		return null;
	}
}

/**
 * 
 */
package de.michaelfuerst.hangout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * @author Michael
 *
 */
public class ConversationView extends LinearLayout {
	
	public ConversationView(Context ctx, final String name, final String nick) {
		super(ctx);
		String user = HangoutNetwork.getInstance().getUser();
		String s[] = nick.split(",");
		String localNick = nick;
		if (s.length == 2) {
			if (s[1].equals(user)) {
				localNick = s[0];
			} else {
				localNick = s[1];
			}
		}
		RelativeLayout  v = new RelativeLayout (ctx);
		v.setMinimumHeight(96);
		v.setMinimumWidth(96);
		ImageView iv = getImageView(HangoutNetwork.HANGOUT_SERVER + "/imgs/profile_"+localNick+".png");
		if (iv != null) {
			iv.setAdjustViewBounds(true);
			iv.setMaxHeight(96);
			iv.setMaxWidth(96);
			iv.setMinimumHeight(96);
			iv.setMinimumWidth(96);
			v.addView(iv);
			addView(v);
		}
		TextView t = new TextView(ctx);
		t.setText(name);
		t.setPadding(16, 24, 0, 24);
		addView(t);
		if (HangoutNetwork.getInstance().getMarkedConversations().contains(nick)) {
			iv = getImageView(HangoutNetwork.HANGOUT_SERVER + "/imgs/marker.png");

			v = new RelativeLayout (ctx);
			v.setMinimumHeight(96);
			v.setMinimumWidth(96);
			
			iv.setAdjustViewBounds(true);
			iv.setMaxHeight(96);
			iv.setMaxWidth(96);
			iv.setMinimumHeight(96);
			iv.setMinimumWidth(96);
			v.addView(iv);
			v.setGravity(Gravity.RIGHT);
			addView(v);
		}
		setPadding(5, 5, 5, 5);
		this.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				HangoutNetwork networkAdapter = HangoutNetwork.getInstance();
				if (networkAdapter != null) {
					String openConversation = nick;
					String openConversationName = name;
					Intent intent = new Intent(getContext(), Chat.class);
					intent.putExtra("chatnick", openConversation);
					intent.putExtra("chatname", openConversationName);
					getContext().startActivity(intent);
				}
			}
		});
	}

	
	private ImageView getImageView(final String path) {
		final ImageView iv = new ImageView(getContext());
		String preFile = path.split("/")[path.split("/").length - 1];
		String filename = Environment.getExternalStorageDirectory()+"/BlaChat/" + preFile.split("\\.")[0] + ".png";
		if (new File(filename).exists()) {
			iv.setImageDrawable(LocalResourceManager.getDrawable(filename));
		}
		new AsyncTask<Object, Object, Drawable>() {

			@Override
			protected Drawable doInBackground(Object... arg0) {
				Drawable image = null;
				try {
					File sysPath = new File(Environment.getExternalStorageDirectory()+"/BlaChat");
					if (!sysPath.exists()) {
						sysPath.mkdirs();
					}
					String preFile = path.split("/")[path.split("/").length - 1];
					String filename = Environment.getExternalStorageDirectory()+"/BlaChat/" + preFile.split("\\.")[0] + ".png";
					if (!new File(filename).exists() || Math.random() > 0.1) {
						// First create a new URL object
						URL url = new URL(path);

						// Next create a file, the example below will save to
						// the
						// SDCARD using JPEG format
						File file = new File(filename);

						// Next create a Bitmap object and download the image to
						// bitmap
						Bitmap bitmap = BitmapFactory.decodeStream(url
								.openStream());

						// Finally compress the bitmap, saving to the file
						// previously created
						bitmap.compress(CompressFormat.PNG, 100,
								new FileOutputStream(file));
					}
					image = LocalResourceManager.getDrawable(filename);
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				return image;
			}

			@Override
			protected void onPostExecute(Drawable image) {
				if (image != null) {
					iv.setImageDrawable(image);
				} else {
					iv.setBackgroundColor(Color.rgb(0, 0, 0));
				}
			}
		}.execute();
		return iv;
	}
}

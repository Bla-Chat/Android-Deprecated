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
	
	private static final int PROFILE_IMAGE_SIZE = 96;


	public ConversationView(final Conversations parent, final String name, final String nick) {
		super(parent);
		createChilds(parent, name, nick);
		final ConversationView that = this;
		this.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				setBackgroundColor(Color.rgb(245, 245, 255));
				HangoutNetwork networkAdapter = HangoutNetwork.getInstance();
				if (networkAdapter != null) {
					String openConversation = nick;
					networkAdapter.unmark(openConversation);
					parent.autoMark();
					detachAllViewsFromParent();
					createChilds(parent, name, nick);
					String openConversationName = name;
					Intent intent = new Intent(getContext(), Chat.class);
					intent.putExtra("chatnick", openConversation);
					intent.putExtra("chatname", openConversationName);
					getContext().startActivity(intent);
				}
				new AsyncTask<Void, Void, Void>() {

					@Override
					protected Void doInBackground(Void... params) {
						try {
							Thread.sleep(200);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						return null;
					}
					
					@Override
					protected void onPostExecute(Void v) {
						that.setBackgroundColor(0);
					}
				}.execute();
			}
		});
	}

	
	private void createChilds(Context parent, String name, String nick) {
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
		RelativeLayout  v = new RelativeLayout (parent);
		v.setMinimumHeight(96);
		v.setMinimumWidth(96);
		ImageView iv = getImageView(parent, HangoutNetwork.HANGOUT_SERVER + "/imgs/profile_"+localNick+".png");
		if (iv != null) {
			iv.setAdjustViewBounds(true);
			iv.setMaxHeight(96);
			iv.setMaxWidth(96);
			iv.setMinimumHeight(96);
			iv.setMinimumWidth(96);
			v.addView(iv);
			addView(v);
		}
		TextView t = new TextView(parent);
		t.setText(name);
		t.setPadding(16, 24, 0, 24);
		addView(t);
		if (HangoutNetwork.getInstance().getMarkedConversations().contains(nick)) {
			iv = getImageView(parent, HangoutNetwork.HANGOUT_SERVER + "/imgs/marker.png");

			v = new RelativeLayout (parent);
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
	}


	private ImageView getImageView(final Context ctx, final String path) {
		final AutoBufferingImageView iv = new AutoBufferingImageView(getContext());
		String preFile = path.split("/")[path.split("/").length - 1];
		final String filename = Environment.getExternalStorageDirectory()+"/Pictures/BlaChat/" + preFile.split("\\.")[0] + ".png";
		if (new File(filename).exists()) {
			iv.setImageDrawable(LocalResourceManager.getDrawable(ctx, filename, PROFILE_IMAGE_SIZE));
			iv.setImage(filename, PROFILE_IMAGE_SIZE);
		}
		new AsyncTask<Object, Object, Drawable>() {

			@Override
			protected Drawable doInBackground(Object... arg0) {
				Drawable image = null;
				try {
					File sysPath = new File(Environment.getExternalStorageDirectory()+"/Pictures/BlaChat");
					if (!sysPath.exists()) {
						sysPath.mkdirs();
					}
					String preFile = path.split("/")[path.split("/").length - 1];
					String filename = Environment.getExternalStorageDirectory()+"/Pictures/BlaChat/" + preFile.split("\\.")[0] + ".png";
					if (!new File(filename).exists() || HangoutNetwork.getInstance().reloadFile(preFile)) {
						HangoutNetwork.getInstance().fileReloaded(preFile);
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
					image = LocalResourceManager.getDrawable(ctx, filename, PROFILE_IMAGE_SIZE);
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
					iv.setImage(filename, PROFILE_IMAGE_SIZE);
				} else {
					iv.setBackgroundColor(Color.rgb(0, 0, 0));
					iv.setImage("none", PROFILE_IMAGE_SIZE);
				}
			}
		}.execute();
		return iv;
	}
}

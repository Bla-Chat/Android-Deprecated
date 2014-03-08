package de.michaelfuerst.bla;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

public class BlaWidget extends AppWidgetProvider {
	private static Context context = null;
	private static AppWidgetManager appWidgetManager = null;
	private static int[] appWidgetIds = null;

	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		BlaWidget.context = context;
		BlaWidget.appWidgetManager = appWidgetManager;
		BlaWidget.appWidgetIds = appWidgetIds;
		BlaWidget.updateWidgets();
	}

	public static void updateWidgets() {
		if (context == null || appWidgetManager == null || appWidgetIds == null
				|| BlaNetwork.getInstance() == null
				|| !BlaNetwork.getInstance().isRunning()) {
			Log.d("BlaWidget", "Missing information");
			return;
		}
		Log.d("BlaWidget", "Updating widgets");
		// Perform this loop procedure for each App Widget that belongs to this
		// provider
        for (int appWidgetId : appWidgetIds) {
            // Create an Intent to launch ExampleActivity
            Intent intent = new Intent(context, Conversations.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                    intent, 0);
            // Get the layout for the App Widget and attach an on-click listener
            // to the button
            RemoteViews views = new RemoteViews(context.getPackageName(),
                    R.layout.bla_widget);
            views.setOnClickPendingIntent(R.id.bla_widget_label, pendingIntent);
            // To update a label
            views.setTextViewText(R.id.bla_widget_label, BlaNetwork
                    .getInstance().getNotificationText());
            // Tell the AppWidgetManager to perform an update on the current app
            // widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
	}
}

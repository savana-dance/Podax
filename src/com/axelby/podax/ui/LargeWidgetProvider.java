package com.axelby.podax.ui;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.RemoteViews;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.axelby.podax.ActivePodcastReceiver;
import com.axelby.podax.Constants;
import com.axelby.podax.Helper;
import com.axelby.podax.PlayerService;
import com.axelby.podax.PlayerStatus;
import com.axelby.podax.R;

public class LargeWidgetProvider extends AppWidgetProvider {
	public static void setPlayerServiceClickIntent(Context context, RemoteViews views, int resourceId, int command) {
		Intent intent = new Intent(context, PlayerService.class);
		// pendingintent will reuse intent if possible, does not look at extras so datauri makes this unique to command
		intent.setData(Uri.parse("podax://playercommand/" + command));
		intent.putExtra(Constants.EXTRA_PLAYER_COMMAND, command);
		PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 0);
		views.setOnClickPendingIntent(resourceId, pendingIntent);
	}

	public static void setActivePodcastClickIntent(Context context, RemoteViews views, int resourceId, Uri command) {
		Intent intent = new Intent(context, ActivePodcastReceiver.class);
		intent.setData(command);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
		views.setOnClickPendingIntent(resourceId, pendingIntent);
	}

	@Override
	public void onUpdate(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
		if (appWidgetIds.length == 0)
			return;

		final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.largewidget);

		PlayerStatus playerState = PlayerStatus.getCurrentState(context);

		updatePodcastDetails(playerState, views);

		// set up pending intents
		setActivePodcastClickIntent(context, views, R.id.restart_btn, Constants.ACTIVE_PODCAST_DATA_RESTART);
		setActivePodcastClickIntent(context, views, R.id.rewind_btn, Constants.ACTIVE_PODCAST_DATA_BACK);
		setPlayerServiceClickIntent(context, views, R.id.play_btn, Constants.PLAYER_COMMAND_PLAYSTOP);
		setActivePodcastClickIntent(context, views, R.id.skip_btn, Constants.ACTIVE_PODCAST_DATA_FORWARD);
		setActivePodcastClickIntent(context, views, R.id.next_btn, Constants.ACTIVE_PODCAST_DATA_END);

		final String thumbnailUrl = playerState.getSubscriptionThumbnailUrl();
		if (thumbnailUrl != null) {
			final ImageLoader.ImageListener imageListener = new ImageLoader.ImageListener() {
				@Override
				public void onResponse(ImageLoader.ImageContainer imageContainer, boolean isImmediate) {
					if (imageContainer.getBitmap() != null) {
						views.setImageViewBitmap(R.id.show_btn, imageContainer.getBitmap());
						appWidgetManager.updateAppWidget(appWidgetIds, views);
					}
				}

				@Override
				public void onErrorResponse(VolleyError volleyError) {
					views.setImageViewResource(R.id.show_btn, R.drawable.icon);
					appWidgetManager.updateAppWidget(appWidgetIds, views);
				}
			};
			new Handler(Looper.getMainLooper()).post(new Runnable() {
				@Override
				public void run() {
					Helper.getImageLoader(context).get(thumbnailUrl, imageListener, 83, 83);
				}
			});
		} else {
			views.setImageViewResource(R.id.show_btn, R.drawable.icon);
		}

		Intent showIntent = new Intent(context, MainActivity.class);
		PendingIntent showPendingIntent = PendingIntent.getActivity(context, 0, showIntent, 0);
		views.setOnClickPendingIntent(R.id.show_btn, showPendingIntent);

		appWidgetManager.updateAppWidget(appWidgetIds, views);

		super.onUpdate(context, appWidgetManager, appWidgetIds);
	}

	public void updatePodcastDetails(PlayerStatus player, RemoteViews views) {
		if (player.hasActivePodcast()) {
			views.setTextViewText(R.id.title, player.getTitle());
			views.setTextViewText(R.id.podcast, player.getSubscriptionTitle());
			PodcastProgress.remoteSet(views, player.getPosition(), player.getDuration());

			int imageRes = player.isPlaying() ? R.drawable.ic_media_pause : R.drawable.ic_media_play;
			views.setImageViewResource(R.id.play_btn, imageRes);
		} else {
			views.setTextViewText(R.id.title, "Queue empty");
			views.setTextViewText(R.id.podcast, "");
			PodcastProgress.remoteClear(views);
			views.setImageViewResource(R.id.play_btn, R.drawable.ic_media_play);
		}
	}
}

package com.moc.button;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.pm.PackageManager;

import android.media.MediaRouter;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

import android.media.VolumeProvider;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;

import android.util.Log;

import androidx.core.app.NotificationCompat;

public class BackgroundService extends Service {
    private static final String TAG = BackgroundService.class.getName();

    private Context context;
    private MediaSession mediaSession = null;
    private ButtonHandler buttonHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();

        PackageManager pkgManager = context.getPackageManager();
        Intent intent = pkgManager.getLaunchIntentForPackage(context.getPackageName());
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                intent, PendingIntent.FLAG_IMMUTABLE);

        String channelId = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = "0";
            String channelName = "Background Service";
            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_MIN);
            NotificationManager nManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nManager.createNotificationChannel(channel);
        }
        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setOngoing(true)
                .setSubText("Active")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);
    }


    private void saveVolumeLevels(AudioManager am) {
        buttonHandler.fixed_stream_volume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        buttonHandler.fixed_ring_volume = am.getStreamVolume(AudioManager.STREAM_RING);
        buttonHandler.fixed_notify_volume = am.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        buttonHandler = new ButtonHandler(context);
        createMediaSession();

        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        MediaRouter mediaRouter = (MediaRouter) getSystemService(Context.MEDIA_ROUTER_SERVICE);

        MediaRouter.SimpleCallback mCallback = new MediaRouter.SimpleCallback() {
            @Override
            public void onRouteSelected(MediaRouter router, int type, MediaRouter.RouteInfo info) {
                    Log.d(TAG, "onRouteSelected");
                    saveVolumeLevels(am);
                }

                @Override
                public void onRouteVolumeChanged(MediaRouter router, MediaRouter.RouteInfo info) {
                    int current_volume = info.getVolume();
                    int type = info.getPlaybackStream();
                    int access_volume = (
                            (type == AudioManager.STREAM_MUSIC) ? buttonHandler.fixed_stream_volume :
                            (type == AudioManager.STREAM_RING) ? buttonHandler.fixed_ring_volume :
                            (type == AudioManager.STREAM_NOTIFICATION) ? buttonHandler.fixed_notify_volume : -1
                    );

                    if (access_volume == -1
                            || access_volume == current_volume) {
                        return;
                    }
                    Runnable runTask = () -> {
                        Log.d(TAG, "set volume to: " + access_volume);
                        am.setStreamVolume(type, access_volume, 0);
                    };
                    new Handler().postDelayed(runTask, 5);

                    mediaSessionToTop();
                    Log.d(TAG, "onRouteVolumeChanged: " + current_volume + " : " + access_volume);
                    buttonHandler.onButtonPress(current_volume < access_volume ? -1 : 1);
                }
        };
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);

        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action == null) {
                    Log.e(TAG, "intent.getAction() is null");
                    return;
                } else {
                    Log.d(TAG, action);
                }
                if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                    mediaSession.setActive(true);
                    mediaSessionToTop();
                    saveVolumeLevels(am);

                    mediaRouter.addCallback(MediaRouter.CALLBACK_FLAG_UNFILTERED_EVENTS,
                            mCallback, MediaRouter.CALLBACK_FLAG_UNFILTERED_EVENTS);
                }
                else if (action.equals(Intent.ACTION_SCREEN_ON)) {
                    mediaSession.setActive(false);
                    mediaRouter.removeCallback(mCallback);
                }
            }
        }, intentFilter);
        return START_STICKY;
    }

    private void mediaSessionToTop() {
        PlaybackState.Builder playbackState = new PlaybackState.Builder()
                .setState(PlaybackState.STATE_PAUSED, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1);
        mediaSession.setPlaybackState(playbackState.build());

        playbackState.setState(PlaybackState.STATE_PLAYING, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1);
        mediaSession.setPlaybackState(playbackState.build());
    }

    private void createMediaSession() {
        mediaSession = new MediaSession(context, "BackgroundService");
        mediaSession.setPlaybackState(new PlaybackState.Builder()
                .setState(PlaybackState.STATE_PLAYING, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1)
                .build());

        final VolumeProvider buttonProvider =
                new VolumeProvider(VolumeProvider.VOLUME_CONTROL_RELATIVE,100, 50) {
                    @Override
                    public void onAdjustVolume(int direction) {
                        Log.d(TAG, "onAdjustVolume " + direction);

                        if (direction != 0) {
                            buttonHandler.onButtonPress(direction);
                        }
                        else {
                            buttonHandler.onButtonRelease();
                        }
                    }
                };
        mediaSession.setPlaybackToRemote(buttonProvider);
        mediaSession.setCallback(new MediaSession.Callback() {});
        mediaSession.setActive(false);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaSession != null) {
            mediaSession.release();
        }
    }
}
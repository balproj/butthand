package com.moc.button;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.KeyEvent;

import com.moc.button.actions.ActionIntent;
import com.moc.button.actions.ActionTorch;
import com.moc.button.actions.ActionVibrate;
import com.moc.button.actions.ActionAudio;

import java.util.Map;


public class ButtonHandler {
    private static final String TAG = ButtonHandler.class.getName();
    private static final int DEFAULT_WAKELOCK = 5000;
    private static final int DEFAULT_VIBRATE = 50;

    private long press_time = 0;
    private int direction = 1;
    private boolean flash = false;

    final private Context context;
    final private Handler handler;
    final private ActionAudio actionAudio;

    private SharedPreferences sharedPref;
    final private ProfilesHelp profilesHelp;

    final private PowerManager pm;
    private PowerManager.WakeLock wl = null;
    private boolean user_wl = false;


    public ButtonHandler(Context context, ActionAudio actionVolume) {
        this.context = context;
        this.actionAudio = actionVolume;

        this.pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        this.handler = new Handler();
        this.profilesHelp = new ProfilesHelp(context);
        String id = profilesHelp.getProfilesList().get(0);
        this.sharedPref = profilesHelp.getProfile(id);
    }

    private int toInt(String str, int def) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public void switchProfile(String name) {
        SharedPreferences pref = profilesHelp.getProfile(name);
        if (pref == null) {
            return;
        }
        this.sharedPref = pref;

        Map<String, String> actions = profilesHelp.sharedGetMap(pref, "timeout_idle_action");
        if (actions.size() == 0) {
            return;
        }
        int dur = toInt(pref.getString("idle_dur", "0"), 0);
        if (dur <= 0) {
            startAction(actions);
            return;
        }
        Runnable runTask = () -> {
            Log.d(TAG, "idle timeout");
            startAction(actions);
        };
        this.handler.postDelayed(runTask, dur);
    }

    public void startAction(Map<String, String> actions) {
        if (actions == null) {
            return;
        }
        for (String action : actions.keySet()) {
            Log.d(TAG, "Actions key: " + action + ": " + actions.get(action));

            switch (action) {
                case "act_media_play_pause":
                    actionAudio.pressMediaButton(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                    break;
                case "act_media_next":
                    actionAudio.pressMediaButton(KeyEvent.KEYCODE_MEDIA_NEXT);
                    break;
                case "act_media_previous":
                    actionAudio.pressMediaButton(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
                    break;
                case "act_media_rewind":
                    actionAudio.pressMediaButton(KeyEvent.KEYCODE_MEDIA_REWIND);
                    break;
                case "act_media_forward":
                    actionAudio.pressMediaButton(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD);
                    break;

                case "act_volume_stream_up":
                    actionAudio.addStreamVolume(1);
                    break;
                case "act_volume_stream_down":
                    actionAudio.addStreamVolume(-1);
                    break;

                case "act_flashlight":
                    ActionTorch.turnFlashlight(context, !this.flash);
                    this.flash = !this.flash;
                    break;

                case "act_vibrate":
                    int dur = toInt(actions.get("param_vibrate"), DEFAULT_VIBRATE);
                    ActionVibrate.vibrate(context, dur);
                    break;

                case "act_vtime":
                    ActionVibrate.vibrateTime(context);
                    break;

                case "act_wakelock_acquire":
                    if (this.wl != null) {
                        this.wl.release();
                    }
                    dur = toInt(actions.get("param_wakelock"), DEFAULT_WAKELOCK);
                    int flags;
                    if (actions.containsKey("param_wakelock_screen")) {
                        flags = PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP;
                    } else {
                        flags = PowerManager.PARTIAL_WAKE_LOCK;
                    }
                    this.wl = pm.newWakeLock(flags, context.getPackageName() + ":lock");
                    this.wl.acquire(dur);
                    this.user_wl = true;
                    break;

                case "act_wakelock_release":
                    if (this.wl != null) {
                        this.wl.release();
                        this.wl = null;
                    }
                    this.user_wl = false;
                    break;

                case "act_intent":
                   try {
                       ActionIntent.sendIntent(context, actions);
                   } catch (Exception e) {
                       Log.e(TAG, "Send intent error: " + e);
                   }
                   break;

                case "act_switch":
                    String profileName = actions.get("param_switch");
                    if (profileName != null)
                        switchProfile(profileName);
                    break;

                case "ac_none":
                    break;
            }
        }
    }

    public void onButtonPress(int direction) {
        this.direction = direction;
        this.press_time = System.currentTimeMillis();

        if (!user_wl) {
            if (this.wl != null) {
                this.wl.release();
            }
            this.wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, context.getPackageName() + ":lock");
            this.wl.acquire(DEFAULT_WAKELOCK);
        }

        String key = direction == 1 ? "press_up_action" : "press_down_action";
        Map<String, String> actions = profilesHelp.sharedGetMap(sharedPref, key);

        if (actions.size() > 0) {
            handler.removeCallbacksAndMessages(null);
            startAction(actions);
        }
    }

    public void onButtonRelease() {
        long t = System.currentTimeMillis();
        Log.d(TAG, "press duration: " + (t - this.press_time));

        String key = direction == 1 ? "release_up_action" : "release_down_action";
        Map<String, String> actions = profilesHelp.sharedGetMap(sharedPref, key);

        if (actions.size() > 0) {
            handler.removeCallbacksAndMessages(null);
            startAction(actions);
        }
        this.press_time = 0;
        this.direction = 0;

        if (!user_wl && this.wl != null) {
            this.wl.release();
            this.wl = null;
        }
    }
}

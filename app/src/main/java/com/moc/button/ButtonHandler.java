package com.moc.button;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;
import android.view.KeyEvent;

import java.util.Calendar;
import java.util.Map;
import java.util.regex.Pattern;

public class ButtonHandler {
    private static final String TAG = ButtonHandler.class.getName();
    private static final int DEFAULT_WAKELOCK = 5000;
    private static final int DEFAULT_VIBRATE = 50;

    private long press_time = 0;
    private int direction = 1;
    private boolean flash = false;

    final private Context context;
    final private Handler handler;

    private SharedPreferences sharedPref;
    final private ProfilesHelp profilesHelp;
    final private AudioManager am;

    final private PowerManager pm;
    private PowerManager.WakeLock wl = null;
    private boolean user_wl = false;

    public int fixed_stream_volume = 0;
    public int fixed_ring_volume = 0;
    public int fixed_notify_volume = 0;


    public ButtonHandler(Context context) {
        this.am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        this.handler = new Handler();
        this.context = context;
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


    public void addStreamVolume(int inc) {
        int volume_current = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        this.fixed_stream_volume = volume_current + inc;
        am.setStreamVolume(AudioManager.STREAM_MUSIC, fixed_stream_volume, 0);
    }

    private void turnFlashlight(boolean mode) {
        try {
            CameraManager camManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            String cameraId = null;

            for (String camID : camManager.getCameraIdList()) {
                CameraCharacteristics camCharacter = camManager.getCameraCharacteristics(camID);
                if (camCharacter.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) != null) {
                    cameraId = camID;
                    break;
                }
            }
            if (cameraId != null) {
                camManager.setTorchMode(cameraId, mode);
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, e.toString());
        }
    }

    public void vibrate(long ms) {
        Vibrator vibrator;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = vm.getDefaultVibrator();
        }
        else {
            vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationEffect effect = VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM));
            } else {
                vibrator.vibrate(effect);
            }
        }
        else {
            vibrator.vibrate(ms);
        }
    }
    public void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Log.e(TAG, "InterruptedException");
        }
    }

    public void vibrateTime() {
        final long point = 40;
        final long tire = 120;
        final long space = 150;

        Calendar calendar = Calendar.getInstance();

        int hour = calendar.get(Calendar.HOUR);
        if (hour >= 12) {
            hour -= 12;
        }
        int minute = calendar.get(Calendar.MINUTE);
        //if ((minute % 5) >= 3 && minute < 55) {
        //    minute += 2;
        //}
        // hours
        if (hour == 0) {
            vibrate(tire * 2);
            sleep(tire * 2);
        }

        for (int i = 0; i < (hour / 3); i++) {
            vibrate(tire);
            sleep(tire + space);
        }
        sleep(space / 2);

        for (int i = 0; i < (hour % 3); i++) {
            vibrate(point);
            sleep(point + space);
        }
        sleep(space * 2);

        for (int i = 0; i < (minute / 15); i++) {
            vibrate(tire);
            sleep(tire + space);
        }
        sleep(space / 2);

        for (int i = 0; i < ((minute % 15) / 5); i++) {
            vibrate(point);
            sleep(point + space);
        }
        sleep(space * 2);

        for (int i = 0; i < (minute % 5); i++) {
            vibrate(tire);
            sleep(tire + space);
        }
    }

    public void pressMediaButton(int code) {
        am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, code));
        am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, code));
    }

    public void switchProfile(String name) {
        SharedPreferences pref = profilesHelp.getProfile(name);
        if (pref == null) {
            return;
        }
        this.sharedPref = pref;

        int dur = toInt(pref.getString("idle_dur", "0"), 0);
        if (dur <= 0) {
            return;
        }
        Runnable runTask = () -> {
            Map<String, String> actions = profilesHelp.sharedGetMap(pref, "timeout_idle_action");
            Log.d(TAG, "idle timeout");
            startAction(actions);
        };
        this.handler.postDelayed(runTask, dur);
    }

    boolean putIntentExtra(String string, Intent intent) {
        if (!string.contains(":")) {
            return false;
        }
        String[] a = string.split(":", 2);
        String name = a[0], str = a[1];

        if (str.matches("^['\"].+['\"]$")) { // "bug'
            intent.putExtra(name, str.substring(1, str.length() - 1));
        }
        else if (str.startsWith("{") && str.endsWith("}")) {
            String[] array = str.substring(1, str.length() - 1).split(",");
            intent.putExtra(name, array);
        }
        else if (str.equals("true")) {
           intent.putExtra(name, true);
        }
        else if (str.equals("false")) {
           intent.putExtra(name, false);
        }
        else try {
            if (str.matches("^[-+,0-9]+[Ll]$")) {
                long num = Long.parseLong(str.substring(0, str.length() - 1));
                intent.putExtra(name, num);
            } else if (str.matches("^[-+,0-9]$")) {
                int num = Integer.parseInt(str);
                intent.putExtra(name, num);
            } else if (str.matches("^[-+,0-9]+\\.[0-9]+[Ff]$")) {
                float num = Float.parseFloat(str.substring(0, str.length() - 1));
                intent.putExtra(name, num);
            } else if (str.matches("^[-+,0-9]+\\.[0-9]+$")) {
                double num = Double.parseDouble(str);
                intent.putExtra(name, num);
            } else {
                intent.putExtra(name, str);
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid number in extra");
            return false;
        }
        return true;
    }

    void sendIntent(Map<String, String> params) {
        String str = params.get("param_intent_action");
        if (str == null || str.equals(""))
            return;
        Intent intent = new Intent(str);

        if ((str = params.get("param_intent_package")) != null
                && !str.equals("")) {
            if (str.contains("/")) {
                intent.setComponent(ComponentName.unflattenFromString(str));
            } else {
                intent.setPackage(str);
            }
        }
        if ((str = params.get("param_intent_data")) != null
                && !str.equals("")) {
            intent.setData(Uri.parse(str));
        }
        if ((str = params.get("param_intent_mimetype")) != null
                && !str.equals("")) {
            intent.setType(str);
        }
        if ((str = params.get("param_intent_category")) != null
                && !str.equals("")) {
            intent.addCategory(str);
        }
        if ((str = params.get("param_intent_extra")) != null
                && !str.equals("")) {

            String delimiter;
            String[] extras;
            if ((delimiter = params.get("param_intent_extra_delimiter")) != null
                    && !delimiter.equals("")) {
                extras = str.split(Pattern.quote(delimiter));
            }
            else {
                extras = new String[]{str};
            }
            for (String extra : extras) {
                putIntentExtra(extra, intent);
            }
        }
        if ((str = params.get("param_intent_target")) != null
                && !str.equals("")) {
            try {
                switch (str) {
                    case "activity":
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                        break;
                    case "broadcast":
                        context.sendBroadcast(intent);
                        break;
                    case "service":
                        context.startService(intent);
                        break;
                    default:
                        Log.e(TAG, "Invalid intent type");
                }
            } catch (Exception e) {
                Log.e(TAG, "Send intent error: " + e);
            }
        }
    }

    public void startAction(Map<String, String> actions) {
        if (actions == null) {
            return;
        }
        for (String action : actions.keySet()) {
            Log.d(TAG, "startAction: " + action);

            switch (action) {
                case "act_media_play_pause":
                    pressMediaButton(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                    break;
                case "act_media_next":
                    pressMediaButton(KeyEvent.KEYCODE_MEDIA_NEXT);
                    break;
                case "act_media_previous":
                    pressMediaButton(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
                    break;
                case "act_media_rewind":
                    pressMediaButton(KeyEvent.KEYCODE_MEDIA_REWIND);
                    break;
                case "act_media_forward":
                    pressMediaButton(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD);
                    break;

                case "act_volume_stream_up":
                    addStreamVolume(1);
                    break;
                case "act_volume_stream_down":
                    addStreamVolume(-1);
                    break;

                case "act_flashlight":
                    turnFlashlight(!this.flash);
                    this.flash = !this.flash;
                    break;

                case "act_vibrate":
                    int dur = toInt(actions.get("param_vibrate"), DEFAULT_VIBRATE);
                    vibrate(dur);
                    break;

                case "act_vtime":
                    vibrateTime();
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
                    sendIntent(actions);
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

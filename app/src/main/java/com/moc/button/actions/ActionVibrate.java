package com.moc.button.actions;

import android.content.Context;
import android.os.Build;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

import java.util.Calendar;

public class ActionVibrate {
    static public void vibrate(Context context, long ms) {
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

    static private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
        }
    }

    static public void vibrateTime(Context c) {
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
            vibrate(c, tire * 2);
            sleep(tire * 2);
        }

        for (int i = 0; i < (hour / 3); i++) {
            vibrate(c, tire);
            sleep(tire + space);
        }
        sleep(space / 2);

        for (int i = 0; i < (hour % 3); i++) {
            vibrate(c, point);
            sleep(point + space);
        }
        sleep(space * 2);

        for (int i = 0; i < (minute / 15); i++) {
            vibrate(c, tire);
            sleep(tire + space);
        }
        sleep(space / 2);

        for (int i = 0; i < ((minute % 15) / 5); i++) {
            vibrate(c, point);
            sleep(point + space);
        }
        sleep(space * 2);

        for (int i = 0; i < (minute % 5); i++) {
            vibrate(c, tire);
            sleep(tire + space);
        }
    }
}

package com.moc.button.actions;

import android.media.AudioManager;
import android.view.KeyEvent;

public class ActionAudio {
    private int fixed_stream_volume = 0;
    private int fixed_ring_volume = 0;
    private int fixed_notify_volume = 0;

    private final AudioManager am;

    public ActionAudio(AudioManager am) {
        this.am = am;
    }

    public void addStreamVolume(int inc) {
        int volume_current = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        this.fixed_stream_volume = volume_current + inc;
        am.setStreamVolume(AudioManager.STREAM_MUSIC, fixed_stream_volume, 0);
    }

    public void addRingVolume(int inc) {
        int volume_current = am.getStreamVolume(AudioManager.STREAM_RING);
        this.fixed_ring_volume = volume_current + inc;
        am.setStreamVolume(AudioManager.STREAM_RING, fixed_ring_volume, 0);
    }

    public void addNotificationVolume(int inc) {
        int volume_current = am.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
        this.fixed_notify_volume = volume_current + inc;
        am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, fixed_notify_volume, 0);
    }

    public int getFixedVolume(int type) {
        switch (type) {
            case AudioManager.STREAM_MUSIC:
                return fixed_stream_volume;
            case AudioManager.STREAM_RING:
                return fixed_ring_volume;
            case AudioManager.STREAM_NOTIFICATION:
                return fixed_notify_volume;
            default:
                return -1;
        }
    }

    public void updateVolumeLevels() {
        this.fixed_stream_volume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        this.fixed_ring_volume = am.getStreamVolume(AudioManager.STREAM_RING);
        this.fixed_notify_volume = am.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
    }

    public void pressMediaButton(int code) {
        am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, code));
        am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, code));
    }
}

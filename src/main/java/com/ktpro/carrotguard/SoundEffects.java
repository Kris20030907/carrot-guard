package com.ktpro.carrotguard;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.util.EnumMap;
import java.util.Map;

public final class SoundEffects {
    private static final float SAMPLE_RATE = 22_050f;
    private static final int HIT_THROTTLE_MS = 75;

    private final Map<SoundEffect, byte[]> samples = new EnumMap<>(SoundEffect.class);
    private boolean enabled = true;
    private long lastHitMillis;

    public SoundEffects() {
        samples.put(SoundEffect.CLICK, tone(620, 0.045, 0.25));
        samples.put(SoundEffect.BUILD, chord(new int[] { 420, 640 }, 0.10, 0.24));
        samples.put(SoundEffect.UPGRADE, chord(new int[] { 520, 780, 1040 }, 0.13, 0.22));
        samples.put(SoundEffect.SELL, tone(330, 0.09, 0.22));
        samples.put(SoundEffect.HIT, noiseTone(190, 0.035, 0.18));
        samples.put(SoundEffect.LEAK, chord(new int[] { 180, 130 }, 0.16, 0.24));
        samples.put(SoundEffect.VICTORY, chord(new int[] { 520, 660, 880 }, 0.28, 0.24));
        samples.put(SoundEffect.DEFEAT, chord(new int[] { 260, 190, 140 }, 0.32, 0.24));
    }

    public void play(SoundEffect effect) {
        if (!enabled) {
            return;
        }
        if (effect == SoundEffect.HIT && isHitThrottled()) {
            return;
        }
        byte[] data = samples.get(effect);
        if (data == null) {
            return;
        }
        try {
            Clip clip = AudioSystem.getClip();
            clip.open(format(), data, 0, data.length);
            clip.addLineListener(event -> {
                switch (event.getType().toString()) {
                    case "Stop", "Close" -> clip.close();
                    default -> {
                    }
                }
            });
            clip.start();
        } catch (Exception e) {
            enabled = false;
        }
    }

    boolean isEnabled() {
        return enabled;
    }

    private boolean isHitThrottled() {
        long now = System.currentTimeMillis();
        if (now - lastHitMillis < HIT_THROTTLE_MS) {
            return true;
        }
        lastHitMillis = now;
        return false;
    }

    private AudioFormat format() {
        return new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
    }

    private byte[] tone(double frequency, double seconds, double volume) {
        int sampleCount = Math.max(1, (int) (seconds * SAMPLE_RATE));
        byte[] data = new byte[sampleCount];
        for (int i = 0; i < sampleCount; i++) {
            double time = i / SAMPLE_RATE;
            double envelope = 1.0 - i / (double) sampleCount;
            data[i] = (byte) (Math.sin(2.0 * Math.PI * frequency * time) * 127.0 * volume * envelope);
        }
        return data;
    }

    private byte[] chord(int[] frequencies, double seconds, double volume) {
        int sampleCount = Math.max(1, (int) (seconds * SAMPLE_RATE));
        byte[] data = new byte[sampleCount];
        for (int i = 0; i < sampleCount; i++) {
            double time = i / SAMPLE_RATE;
            double envelope = 1.0 - i / (double) sampleCount;
            double value = 0;
            for (int frequency : frequencies) {
                value += Math.sin(2.0 * Math.PI * frequency * time);
            }
            value /= frequencies.length;
            data[i] = (byte) (value * 127.0 * volume * envelope);
        }
        return data;
    }

    private byte[] noiseTone(double frequency, double seconds, double volume) {
        int sampleCount = Math.max(1, (int) (seconds * SAMPLE_RATE));
        byte[] data = new byte[sampleCount];
        int noise = 17;
        for (int i = 0; i < sampleCount; i++) {
            double time = i / SAMPLE_RATE;
            double envelope = 1.0 - i / (double) sampleCount;
            noise = noise * 1103515245 + 12345;
            double grain = ((noise >>> 16) & 0xff) / 255.0 - 0.5;
            double value = Math.sin(2.0 * Math.PI * frequency * time) * 0.75 + grain * 0.25;
            data[i] = (byte) (value * 127.0 * volume * envelope);
        }
        return data;
    }
}

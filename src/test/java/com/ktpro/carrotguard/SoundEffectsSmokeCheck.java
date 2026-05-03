package com.ktpro.carrotguard;

public final class SoundEffectsSmokeCheck {
    private SoundEffectsSmokeCheck() {
    }

    public static void main(String[] args) {
        SoundEffects sounds = new SoundEffects();
        sounds.play(SoundEffect.CLICK);
        sounds.play(SoundEffect.HIT);
        sounds.play(SoundEffect.HIT);
        sounds.play(SoundEffect.VICTORY);
        require(sounds != null, "sound effects should be constructable");
        System.out.println("SoundEffects smoke check passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}

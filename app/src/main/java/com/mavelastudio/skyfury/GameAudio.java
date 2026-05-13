package com.mavelastudio.skyfury;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.util.SparseBooleanArray;

final class GameAudio {
    private final SoundPool soundPool;
    private final SparseBooleanArray loadedSounds = new SparseBooleanArray();
    private MediaPlayer musicPlayer;
    private int shootSound;
    private int hitSound;
    private int explosionSound;
    private int powerupSound;
    private int laserSound;
    private int victorySound;
    private int buttonSound;
    private int highScoreSound;
    private int armorSound;
    private int atomicBombSound;
    private int extraLifeSound;
    private int guardianAngelSound;
    private int laserUpgradeSound;
    private int machineGunSound;
    private int rocketUpgradeSound;
    private int lifeLostSound;
    private boolean released;
    private boolean musicRequested = true;
    private float musicVolume = 0.68f;
    private float soundFxVolume = 0.86f;

    GameAudio(Context context) {
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setAudioAttributes(attributes)
                .setMaxStreams(10)
                .build();
        soundPool.setOnLoadCompleteListener((pool, sampleId, status) -> loadedSounds.put(sampleId, status == 0));
        shootSound = soundPool.load(context, R.raw.shoot, 1);
        hitSound = soundPool.load(context, R.raw.hit, 1);
        explosionSound = soundPool.load(context, R.raw.explosion, 1);
        powerupSound = soundPool.load(context, R.raw.powerup, 1);
        laserSound = soundPool.load(context, R.raw.laser, 1);
        victorySound = soundPool.load(context, R.raw.victory_sting, 1);
        buttonSound = soundPool.load(context, R.raw.button, 1);
        highScoreSound = soundPool.load(context, R.raw.high_score_fanfare, 1);
        armorSound = soundPool.load(context, R.raw.armor_upgrade, 1);
        atomicBombSound = soundPool.load(context, R.raw.atomic_bomb, 1);
        extraLifeSound = soundPool.load(context, R.raw.extra_life, 1);
        guardianAngelSound = soundPool.load(context, R.raw.guardian_angel, 1);
        laserUpgradeSound = soundPool.load(context, R.raw.laser_upgrade, 1);
        machineGunSound = soundPool.load(context, R.raw.machine_gun, 1);
        rocketUpgradeSound = soundPool.load(context, R.raw.rocket_upgrade, 1);
        lifeLostSound = soundPool.load(context, R.raw.life_lost, 1);

        musicPlayer = MediaPlayer.create(context, R.raw.bgmusic);
        if (musicPlayer != null) {
            musicPlayer.setLooping(true);
            musicPlayer.setVolume(musicVolume, musicVolume);
        }
    }

    void resumeMusic() {
        if (released || musicPlayer == null) {
            return;
        }
        musicRequested = true;
        soundPool.autoResume();
        if (musicVolume <= 0f) {
            if (musicPlayer.isPlaying()) {
                musicPlayer.pause();
            }
            return;
        }
        if (!musicPlayer.isPlaying()) {
            musicPlayer.start();
        }
        musicPlayer.setVolume(musicVolume, musicVolume);
    }

    void pauseMusic() {
        if (released) {
            return;
        }
        musicRequested = false;
        soundPool.autoPause();
        if (musicPlayer != null && musicPlayer.isPlaying()) {
            musicPlayer.pause();
        }
    }

    void playShoot() {
        play(machineGunSound, 0.34f, 1.04f);
    }

    void playHit() {
    }

    void playExplosion() {
        play(explosionSound, 0.74f, 0.92f);
    }

    void playPowerup() {
        play(powerupSound, 0.70f, 1.0f);
    }

    void playLaser() {
        play(laserUpgradeSound, 0.72f, 1.0f);
    }

    void playVictorySting() {
        playMusic(victorySound, 0.82f, 1.0f);
    }

    void playButton() {
        play(buttonSound, 0.42f, 1.0f);
    }

    void playHighScoreFanfare() {
        play(highScoreSound, 0.86f, 1.0f);
    }

    void playArmorUpgrade() {
        play(armorSound, 0.82f, 1.0f);
    }

    void playAtomicBomb() {
        play(atomicBombSound, 0.94f, 1.0f);
    }

    void playExtraLife() {
        play(extraLifeSound, 0.85f, 1.0f);
    }

    void playGuardianAngel() {
        play(guardianAngelSound, 0.88f, 1.0f);
    }

    void playLaserUpgrade() {
        play(laserUpgradeSound, 0.84f, 1.0f);
    }

    void playMachineGun() {
        play(machineGunSound, 0.78f, 1.0f);
    }

    void playRocketUpgrade() {
        play(rocketUpgradeSound, 0.80f, 1.0f);
    }

    void playLifeLost() {
        play(lifeLostSound, 0.90f, 1.0f);
    }

    void setMusicVolume(float volume) {
        musicVolume = clamp(volume);
        if (!released && musicPlayer != null) {
            musicPlayer.setVolume(musicVolume, musicVolume);
            if (musicVolume <= 0f && musicPlayer.isPlaying()) {
                musicPlayer.pause();
            } else if (musicRequested && musicVolume > 0f && !musicPlayer.isPlaying()) {
                musicPlayer.start();
            }
        }
    }

    void setSoundFxVolume(float volume) {
        soundFxVolume = clamp(volume);
    }

    float getMusicVolume() {
        return musicVolume;
    }

    float getSoundFxVolume() {
        return soundFxVolume;
    }

    void release() {
        if (released) {
            return;
        }
        released = true;
        soundPool.autoPause();
        soundPool.release();
        if (musicPlayer != null) {
            musicPlayer.release();
            musicPlayer = null;
        }
    }

    private void play(int soundId, float volume, float rate) {
        if (released || !loadedSounds.get(soundId)) {
            return;
        }
        float scaled = volume * soundFxVolume;
        if (scaled > 0f) {
            soundPool.play(soundId, scaled, scaled, 1, 0, rate);
        }
    }

    private void playMusic(int soundId, float volume, float rate) {
        if (released || !loadedSounds.get(soundId)) {
            return;
        }
        float scaled = volume * musicVolume;
        if (scaled > 0f) {
            soundPool.play(soundId, scaled, scaled, 1, 0, rate);
        }
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}

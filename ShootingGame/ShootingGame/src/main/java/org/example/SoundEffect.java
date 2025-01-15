package org.example;

import javax.sound.sampled.*;
public class SoundEffect {

    public static void playShootSound() {
        try {
            Clip clip = AudioSystem.getClip();
            AudioInputStream ais = AudioSystem.getAudioInputStream(getClass().getResource("/shoot.wav"));
            clip.open(ais);
            clip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

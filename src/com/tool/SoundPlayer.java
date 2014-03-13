/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tool;

import java.io.InputStream;
import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.data.SampleManager;
import net.beadsproject.beads.ugens.Gain;
import net.beadsproject.beads.ugens.SamplePlayer;


/**
 *
 * @author macbookpro
 */
public class SoundPlayer implements Runnable {

    private String soundFile;
    
    private AudioContext audioContext;
    
    private static final String COMPLETE_AUDIO_NAME = "complete.wav";
    
    private static final String WARNING_AUDIO_NAME = "warning.wav";
    
    public static final SoundPlayer COMPLETE_SOUND_PLAYER = new SoundPlayer(COMPLETE_AUDIO_NAME);
    
    public static final SoundPlayer WARNING_SOUND_PLAYER = new SoundPlayer(WARNING_AUDIO_NAME);
    
    public SoundPlayer(String soundFile) {
        this.soundFile = soundFile;
        audioContext = new AudioContext();
    }

    @Override
    public void run() {
        InputStream is = null;
        try {
            is = getClass().getClassLoader().getResourceAsStream(soundFile);
            SamplePlayer player = new SamplePlayer(audioContext, SampleManager
                                    .sample(is));
            Gain g = new Gain(audioContext, 2, 0.2f);
            g.addInput(player);
            audioContext.out.addInput(g);
            audioContext.start();
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != is) {
                    is.close();
                }
            } catch (Exception e) {
                //ignore
            }
        }
        
    }

}

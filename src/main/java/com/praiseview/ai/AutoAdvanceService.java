package com.praiseview.ai;

import com.praiseview.model.Song;
import com.praiseview.model.Verse;
import info.debatty.java.stringsimilarity.NormalizedLevenshtein;
import javax.sound.sampled.*;
import java.util.List;
import java.util.concurrent.*;

public class AutoAdvanceService {

    private boolean enabled = false;
    private Song currentSong;
    private int currentIndex = 0;
    private TargetDataLine mic;
    private ExecutorService executor;
    private final NormalizedLevenshtein similarity = new NormalizedLevenshtein();
    private double threshold = 0.65;

    private VerseChangeListener listener;

    public interface VerseChangeListener {
        void onVerseChanged(int index);
    }

    public void setListener(VerseChangeListener listener) {
        this.listener = listener;
    }

    public void toggle(boolean on, Song song) {
        this.enabled = on;
        this.currentSong = song;
        this.currentIndex = 0;

        if (on && song != null) {
            startListening();
        } else {
            stop();
        }
    }

    public void manualOverride(int newIndex) {
        this.currentIndex = newIndex;
        if (listener != null) listener.onVerseChanged(newIndex);
        stop();                    // Fully disable AI
        enabled = false;
    }

    private void startListening() {
        executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
                mic = (TargetDataLine) AudioSystem.getLine(new DataLine.Info(TargetDataLine.class, format));
                mic.open(format);
                mic.start();

                byte[] buffer = new byte[16000 * 6];

                while (enabled) {
                    int read = mic.read(buffer, 0, buffer.length);
                    if (read > 0) {
                        String text = "simulated transcription"; // Replace later with whisper
                        processTranscription(text);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void processTranscription(String text) {
        if (currentSong == null || currentIndex >= currentSong.getVerses().size() - 1) return;

        Verse next = currentSong.getVerses().get(currentIndex + 1);
        double sim = similarity.similarity(text.toLowerCase(), next.getContent().toLowerCase());

        if (sim > threshold) {
            currentIndex++;
            if (listener != null) listener.onVerseChanged(currentIndex);
        }
    }

    private void stop() {
        enabled = false;
        if (mic != null) mic.close();
        if (executor != null) executor.shutdown();
    }
}

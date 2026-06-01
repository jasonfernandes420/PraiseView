package com.praiseview.ai;

import com.praiseview.model.Song;
import info.debatty.java.stringsimilarity.NormalizedLevenshtein;
import io.github.givimad.whisperjni.WhisperContext;
import io.github.givimad.whisperjni.WhisperJNI;
import io.github.givimad.whisperjni.WhisperFullParams;
import io.github.givimad.whisperjni.WhisperSamplingStrategy;

import javax.sound.sampled.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class AutoAdvanceService {

    private WhisperJNI whisper;
    private WhisperContext context;

    private volatile boolean enabled = false;
    private volatile boolean paused = false;

    private Song currentSong;
    private int currentVerseIndex = 0;

    private TargetDataLine microphone;
    private ExecutorService executor;

    private final NormalizedLevenshtein similarity =
            new NormalizedLevenshtein();

    private double confidenceThreshold = 0.68;

    private Consumer<Integer> onVerseChange;

    public AutoAdvanceService(Consumer<Integer> onVerseChange) {

        this.onVerseChange = onVerseChange;

        try {
            WhisperJNI.loadLibrary();

            whisper = new WhisperJNI();

            context = whisper.init(
                    Paths.get("models/ggml-base.en.bin")
            );

            executor = Executors.newSingleThreadExecutor();

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to initialize Whisper",
                    e
            );
        }
    }

    public void toggle(boolean enable, Song song) {

        this.enabled = enable;
        this.currentSong = song;
        this.currentVerseIndex = 0;
        this.paused = false;

        if (enable) {
            startRealTimeListening();
        } else {
            stop();
        }
    }

    public void manualAdvance(int newIndex) {

        currentVerseIndex = newIndex;
        paused = true;

        if (onVerseChange != null) {
            onVerseChange.accept(newIndex);
        }

        new Thread(() -> {
            try {
                Thread.sleep(10000);
                paused = false;
            } catch (InterruptedException ignored) {
            }
        }).start();
    }

    private void startRealTimeListening() {

        executor.submit(() -> {

            try {

                AudioFormat format =
                        new AudioFormat(
                                16000f,
                                16,
                                1,
                                true,
                                false);

                microphone = AudioSystem.getTargetDataLine(format);

                microphone.open(format);
                microphone.start();

                byte[] buffer = new byte[16000 * 2 * 5];

                while (enabled) {

                    if (paused) {
                        Thread.sleep(500);
                        continue;
                    }

                    int bytesRead =
                            microphone.read(
                                    buffer,
                                    0,
                                    buffer.length);

                    if (bytesRead <= 0) {
                        continue;
                    }

                    float[] samples =
                            pcm16ToFloat(
                                    buffer,
                                    bytesRead);

                    String text =
                            transcribe(samples);

                    if (text != null &&
                            !text.trim().isEmpty()) {

                        processLiveTranscription(text);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private String transcribe(float[] samples) {

        try {

            WhisperFullParams params =
                    new WhisperFullParams(
                            WhisperSamplingStrategy.GREEDY
                    );
            whisper.full(
                    context,
                    params,
                    samples,
                    samples.length);

            int segments =
                    whisper.fullNSegments(context);

            StringBuilder result =
                    new StringBuilder();

            for (int i = 0; i < segments; i++) {

                result.append(
                        whisper.fullGetSegmentText(
                                context,
                                i));

                result.append(" ");
            }

            return result.toString().trim();

        } catch (Exception e) {

            e.printStackTrace();
            return "";
        }
    }

    private void processLiveTranscription(String text) {

        if (paused ||
                currentSong == null ||
                currentSong.getVerses() == null) {
            return;
        }

        if (currentVerseIndex >=
                currentSong.getVerses().size() - 1) {
            return;
        }

        String nextVerse =
                currentSong.getVerses()
                        .get(currentVerseIndex + 1).toString()
                        .toLowerCase();

        double score =
                similarity.similarity(
                        normalize(text),
                        normalize(nextVerse));

        System.out.println(
                "Detected: " + text +
                        " | Similarity=" + score);

        if (score >= confidenceThreshold) {
            advanceToNextVerse();
        }
    }

    private void advanceToNextVerse() {

        currentVerseIndex =
                Math.min(
                        currentVerseIndex + 1,
                        currentSong.getVerses().size() - 1);

        if (onVerseChange != null) {
            onVerseChange.accept(
                    currentVerseIndex);
        }
    }

    private String normalize(String text) {

        return text
                .toLowerCase()
                .replaceAll("[^a-z0-9 ]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private float[] pcm16ToFloat(
            byte[] audio,
            int bytesRead) {

        int sampleCount = bytesRead / 2;

        float[] samples =
                new float[sampleCount];

        ByteBuffer bb =
                ByteBuffer.wrap(audio)
                        .order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < sampleCount; i++) {

            short sample = bb.getShort();

            samples[i] =
                    sample / 32768.0f;
        }

        return samples;
    }

    public void stop() {

        enabled = false;

        try {

            if (microphone != null) {
                microphone.stop();
                microphone.close();
            }

        } catch (Exception ignored) {
        }
    }

    public void setThreshold(double threshold) {
        this.confidenceThreshold = threshold;
    }

    public int getCurrentVerseIndex() {
        return currentVerseIndex;
    }
}
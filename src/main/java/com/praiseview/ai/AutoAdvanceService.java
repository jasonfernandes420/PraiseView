package com.praiseview.ai;

import com.praiseview.model.Song;
import com.praiseview.model.Verse;
import info.debatty.java.stringsimilarity.NormalizedLevenshtein;
import io.github.givimad.whisperjni.WhisperContext;
import io.github.givimad.whisperjni.WhisperFullParams;
import io.github.givimad.whisperjni.WhisperJNI;
import io.github.givimad.whisperjni.WhisperSamplingStrategy;

import javax.sound.sampled.*;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.*;

public class AutoAdvanceService {

    private boolean enabled = false;
    private Song currentSong;
    private int currentIndex = 0;
    private TargetDataLine mic;
    private ExecutorService executor;
    private final NormalizedLevenshtein similarity = new NormalizedLevenshtein();
    private double threshold = 0.65; // Keep this as a configurable threshold

    private VerseChangeListener listener;

    // Whisper related fields
    private WhisperJNI whisperJNI;
    private WhisperContext whisperContext;
    private WhisperFullParams whisperParams;
    private volatile CountDownLatch shutdownLatch;

    public interface VerseChangeListener {
        void onVerseChanged(int index);
    }

    public void setListener(VerseChangeListener listener) {
        this.listener = listener;
    }

    public Song getCurrentSong() {
        return currentSong;
    }

    public void toggle(boolean on, Song song) {
        if (on) {
            // If enabling, first ensure everything is stopped and cleaned up
            stop(); // This will set enabled = false and clean resources

            this.enabled = true; // Now set enabled to true for the new session
            this.currentSong = song;
            this.currentIndex = 0;

            if (song != null) {
                try {
                    whisperJNI = new WhisperJNI();
                    String modelPath = "J:/PraiseView-Full-Project/models/ggml-tiny.en.bin";
                    File modelFile = new File(modelPath);
                    if (!modelFile.exists()) {
                        System.err.println("Whisper model not found at: " + modelPath);
                        this.enabled = false; // Disable if model not found
                        stop(); // Clean up any partially initialized resources
                        return;
                    }

                    whisperContext = whisperJNI.init(Path.of(modelPath));
                    whisperParams = new WhisperFullParams(WhisperSamplingStrategy.GREEDY);
                    
                    // Correctly set Whisper parameters using public fields
                    whisperParams.printProgress = false;
                    whisperParams.printRealtime = false;
                    whisperParams.printTimestamps = false;
                    whisperParams.printSpecial = false;
                    whisperParams.language = "en"; // Assuming English model

                    startListening();
                } catch (Exception e) {
                    System.err.println("Failed to initialize Whisper: " + e.getMessage());
                    e.printStackTrace();
                    this.enabled = false; // Disable on error
                    stop(); // Call stop to ensure full cleanup of all resources
                }
            } else {
                // If 'on' is true but song is null, it's an invalid state to start AI
                this.enabled = false;
                stop();
            }
        } else {
            // If disabling, just call stop() which handles setting enabled to false and cleanup
            stop();
        }
    }

    public void manualOverride(int newIndex) {
        this.currentIndex = newIndex;
        if (listener != null) listener.onVerseChanged(newIndex);
        stop(); // Fully disable AI and clean up
    }

    private void startListening() {
        shutdownLatch = new CountDownLatch(1);  // New latch for this session

        executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "Whisper-Listening-Thread");
            t.setDaemon(true);
            return t;
        });

        executor.submit(() -> {
            try {
                AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
                mic = (TargetDataLine) AudioSystem.getLine(new DataLine.Info(TargetDataLine.class, format));
                mic.open(format);
                mic.start();

                // Smaller buffer (2 seconds) for faster response to shutdown
                int bufferSize = (int) (format.getSampleRate() * format.getFrameSize() * 5);
                byte[] audioBuffer = new byte[bufferSize];
                float[] floatBuffer = new float[bufferSize / 2];

                while (enabled && whisperContext != null) {
                    int bytesRead = 0;

                    // Read in smaller chunks to be more responsive to shutdown
                    while (bytesRead < bufferSize && enabled) {
                        int chunkSize = Math.min(4096, bufferSize - bytesRead);
                        int read = mic.read(audioBuffer, bytesRead, chunkSize);
                        if (read > 0) {
                            bytesRead += read;
                        } else if (read == -1) {
                            break;
                        }
                    }

                    if (!enabled) break;  // Quick exit check

                    if (bytesRead > 0) {
                        if (bytesRead % 2 != 0) {
                            bytesRead--;
                        }
                        if (bytesRead == 0) continue;

                        // Convert to float
                        for (int i = 0; i < bytesRead / 2; i++) {
                            int sample = (audioBuffer[2 * i] & 0xFF) | (audioBuffer[2 * i + 1] << 8);
                            floatBuffer[i] = sample / 32768.0f;
                        }

                        if (!enabled) break;

                        // Transcribe
                        whisperJNI.full(whisperContext, whisperParams, floatBuffer, bytesRead / 2);

                        // Get text
                        StringBuilder transcribedText = new StringBuilder();
                        int numSegments = whisperJNI.fullNSegments(whisperContext);
                        for (int i = 0; i < numSegments; i++) {
                            transcribedText.append(whisperJNI.fullGetSegmentText(whisperContext, i));
                        }

                        String text = transcribedText.toString().trim();
                        if (!text.isEmpty() && enabled) {
                            processTranscription(text);
                        }
                    }
                }
            } catch (Exception e) {
                if (enabled) {  // Only log real errors, not shutdown
                    System.err.println("Error during speech recognition: " + e.getMessage());
                    e.printStackTrace();
                }
            } finally {
                // Signal that the thread has finished
                if (shutdownLatch != null) {
                    shutdownLatch.countDown();
                }

                if (mic != null) {
                    try {
                        mic.stop();
                        mic.close();
                    } catch (Exception ignored) {}
                    mic = null;
                }
            }
        });
    }
    private void processTranscription(String transcribedText) {
        // Check if we are already at the last verse, in which case there's no "next" verse to advance to.
        // Or if currentSong is null.
        if (currentSong == null || currentIndex >= currentSong.getVerses().size() - 1) {
            System.out.println("processTranscription: No current song or already at last verse. Not advancing.");
            return;
        }

        // Get the current verse (the one being displayed)
        Verse currentVerse = currentSong.getVerses().get(currentIndex);
        String currentVerseContent = currentVerse.getContent();

        // Extract the last line of the current verse for comparison
        String[] lines = currentVerseContent.split("\\r?\\n");
        String lastLineOfCurrentVerse = lines[lines.length - 1].trim();

        // Log for debugging
        System.out.println("processTranscription: Comparing...");
        System.out.println("  Transcribed: \"" + transcribedText.toLowerCase() + "\"");
        System.out.println("  Target (last line of current verse): \"" + lastLineOfCurrentVerse.toLowerCase() + "\"");

        double sim = similarity.similarity(transcribedText.toLowerCase(), lastLineOfCurrentVerse.toLowerCase());
        System.out.println("  Similarity: " + sim + " (Threshold: " + threshold + ")");

        if (sim > threshold) {
            currentIndex++;
            System.out.println("processTranscription: Similarity above threshold. Advancing to index: " + currentIndex);
            if (listener != null) listener.onVerseChanged(currentIndex);
        } else {
            System.out.println("processTranscription: Similarity below threshold. Not advancing.");
        }
    }

    private void stop() {
        this.enabled = false;   // Signal immediate stop

        // Wait for the listening thread to exit gracefully
        if (shutdownLatch != null) {
            try {
                boolean finished = shutdownLatch.await(4, TimeUnit.SECONDS);
                if (!finished) {
                    System.err.println("Warning: Whisper listening thread did not shut down within timeout");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Now safely clean up executor
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }

        // Clean up native resources
        if (whisperContext != null) {
            try {
                whisperContext.close();
            } catch (Exception e) {
                System.err.println("Error closing WhisperContext: " + e.getMessage());
            }
            whisperContext = null;
        }

        whisperJNI = null;
        this.currentSong = null;
        this.currentIndex = 0;
        this.shutdownLatch = null;  // Clean up latch
    }
}

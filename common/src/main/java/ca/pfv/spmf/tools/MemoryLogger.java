package main.java.ca.pfv.spmf.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * MemoryLogger tracks the maximum memory usage during execution.
 * It can also record the values to a file if recording mode is activated.
 */
public class MemoryLogger {

    // Only one instance (singleton design pattern)
    private static MemoryLogger instance = new MemoryLogger();

    // Maximum memory recorded (in megabytes)
    private double maxMemory = 0;

    // Recording mode flag and associated file writer
    private boolean recordingMode = false;
    private File outputFile = null;
    private BufferedWriter writer = null;

    public static MemoryLogger getInstance() {
        return instance;
    }

    /**
     * Returns the maximum memory usage (in MB).
     */
    public double getMaxMemory() {
        return maxMemory;
    }

    /**
     * Reset the memory usage counter.
     */
    public void reset() {
        maxMemory = 0;
    }

    /**
     * Check the current memory usage and update if greater than recorded.
     * Optionally record the value to a file.
     * @return current memory usage in MB.
     */
    public double checkMemory() {
        double currentMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024d / 1024d;
        if (currentMemory > maxMemory) {
            maxMemory = currentMemory;
        }
        if (recordingMode) {
            try {
                writer.write(currentMemory + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return currentMemory;
    }

    /**
     * Start recording memory usage to a file.
     * @param fileName output file path.
     */
    public void startRecordingMode(String fileName) {
        recordingMode = true;
        outputFile = new File(fileName);
        try {
            writer = new BufferedWriter(new FileWriter(outputFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Stop recording memory usage.
     */
    public void stopRecordingMode() {
        if (recordingMode) {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            recordingMode = false;
        }
    }
}
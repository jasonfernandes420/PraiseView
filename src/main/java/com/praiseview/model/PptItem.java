package com.praiseview.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.praiseview.util.AppLogger;
import com.praiseview.util.PptRenderer;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
public class PptItem implements Projectable {

    private String id = UUID.randomUUID().toString();
    private String title;
    private String originalFilePath;
    private List<String> renderedSlideImagePaths = new ArrayList<>();
    private String tempImageDirPath; // Path to the temporary directory holding slide images

    // Constructor for creating a new PptItem from a file
    public PptItem(File pptFile) throws IOException {
        this.id = UUID.randomUUID().toString();
        this.originalFilePath = pptFile.getAbsolutePath();
        this.title = pptFile.getName();
        AppLogger.log("PptItem: Creating new PptItem for: " + originalFilePath);

        // Render slides to images and store paths
        this.renderedSlideImagePaths = PptRenderer.renderPptToImages(originalFilePath);
        if (!renderedSlideImagePaths.isEmpty()) {
            // Extract the temporary directory path from the first image path
            Path firstImagePath = Paths.get(renderedSlideImagePaths.get(0));
            this.tempImageDirPath = firstImagePath.getParent().toString();
            AppLogger.log("PptItem: Rendered " + renderedSlideImagePaths.size() + " slides. Temp dir: " + tempImageDirPath);
        } else {
            AppLogger.log("PptItem: No slides rendered for " + originalFilePath);
        }

        AppLogger.log("PptItem created: " + title + " with " + renderedSlideImagePaths.size() + " slides.");
    }

    // Constructor for Jackson deserialization (will need to re-render or handle persisted paths)
    // For simplicity, during deserialization, we'll re-render the PPT if the temp dir is gone.
    // A more robust solution would persist the rendered images or a hash to check if re-rendering is needed.
    public PptItem(String id, String title, String originalFilePath, List<String> renderedSlideImagePaths, String tempImageDirPath) {
        this.id = id;
        this.title = title;
        this.originalFilePath = originalFilePath;
        this.renderedSlideImagePaths = renderedSlideImagePaths;
        this.tempImageDirPath = tempImageDirPath;
        AppLogger.log("PptItem: Deserializing PptItem for: " + originalFilePath + ". Temp dir: " + tempImageDirPath);


        // Check if temp images still exist, if not, re-render
        if (tempImageDirPath != null && !new File(tempImageDirPath).exists() && new File(originalFilePath).exists()) {
            AppLogger.log("PptItem: PPT temp images missing for " + title + ". Attempting to re-render.");
            try {
                this.renderedSlideImagePaths = PptRenderer.renderPptToImages(originalFilePath);
                if (!this.renderedSlideImagePaths.isEmpty()) {
                    Path firstImagePath = Paths.get(this.renderedSlideImagePaths.get(0));
                    this.tempImageDirPath = firstImagePath.getParent().toString();
                    AppLogger.log("PptItem: Re-rendered " + this.renderedSlideImagePaths.size() + " slides. New temp dir: " + this.tempImageDirPath);
                }
            } catch (IOException e) {
                AppLogger.log("PptItem: Failed to re-render PPT " + title + " on load: " + e.getMessage());
                this.renderedSlideImagePaths.clear(); // Clear if re-render fails
            }
        } else if (tempImageDirPath != null && new File(tempImageDirPath).exists()) {
            AppLogger.log("PptItem: Existing temp images found for " + title + " at " + tempImageDirPath);
        } else {
            AppLogger.log("PptItem: No re-rendering needed or original file not found for " + title);
        }
    }


    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getType() {
        return "PPT";
    }

    @Override
    public String getFullContent() {
        return "Presentation: " + title + "\nOriginal Path: " + originalFilePath;
    }

    @Override
    public List<String> paginateForDimensions(double fontSize, double maxWidth, double maxHeight) {
        return List.of();
    }

    @Override
    public String getSubItemContent(int index, double fontSize, double maxWidth, double maxHeight) {
        if (index >= 0 && index < renderedSlideImagePaths.size()) {
            String imagePath = renderedSlideImagePaths.get(index);
            AppLogger.log("PptItem: getSubItemContent for index " + index + " returning image path: " + imagePath);
            return imagePath; // Returns the path to the image file
        }
        AppLogger.log("PptItem: getSubItemContent for index " + index + " out of bounds. Total slides: " + renderedSlideImagePaths.size());
        return ""; // Or a path to a "slide not found" image
    }

    @Override
    public int getSubItemCount(double fontSize, double maxWidth, double maxHeight) {
        int count = renderedSlideImagePaths.size();
        AppLogger.log("PptItem: getSubItemCount returning: " + count);
        return count;
    }

    @Override
    public String getSubItemLabel(int index) {
        if (index >= 0 && index < renderedSlideImagePaths.size()) {
            return "Slide " + (index + 1) + " of " + renderedSlideImagePaths.size();
        }
        return "Slide (N/A)";
    }

    @Override
    public String toString() {
        return "[PPT] " + title + " (" + renderedSlideImagePaths.size() + " slides)";
    }

    /**
     * Cleans up the temporary directory containing the rendered slide images.
     */
    public void dispose() {
        if (tempImageDirPath != null) {
            AppLogger.log("PptItem: Disposing PptItem, cleaning up temp dir: " + tempImageDirPath);
            PptRenderer.cleanupTempDirectory(Paths.get(tempImageDirPath));
            tempImageDirPath = null; // Mark as cleaned
        } else {
            AppLogger.log("PptItem: No temp directory to dispose for " + title);
        }
    }
}

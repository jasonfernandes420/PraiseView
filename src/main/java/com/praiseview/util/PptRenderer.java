package com.praiseview.util;

import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFTextParagraph;
import org.apache.poi.hslf.usermodel.HSLFTextRun;
import org.apache.poi.hslf.usermodel.HSLFTextShape;
import org.apache.poi.sl.usermodel.Slide;
import org.apache.poi.sl.usermodel.SlideShow;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PptRenderer {

    private static final String TEMP_DIR_PREFIX = "praiseview_ppt_slides_";

    /**
     * Renders each slide of a PowerPoint presentation to a PNG image file.
     * Creates a temporary directory for the images.
     *
     * @param pptFilePath The path to the PowerPoint file (.ppt or .pptx).
     * @return A list of paths to the rendered PNG image files.
     * @throws IOException If there's an error reading the PPT or writing images.
     */
    public static List<String> renderPptToImages(String pptFilePath) throws IOException {
        AppLogger.log("PptRenderer: Attempting to render PPT: " + pptFilePath);
        List<String> imagePaths = new ArrayList<>();
        Path tempDir = null;
        try (FileInputStream is = new FileInputStream(pptFilePath)) {
            SlideShow<?, ?> ppt;
            if (pptFilePath.toLowerCase().endsWith(".pptx")) {
                ppt = new XMLSlideShow(is);
                AppLogger.log("PptRenderer: Opened PPTX file.");
            } else if (pptFilePath.toLowerCase().endsWith(".ppt")) {
                ppt = new HSLFSlideShow(is);
                AppLogger.log("PptRenderer: Opened PPT file.");
            } else {
                throw new IllegalArgumentException("Unsupported presentation format: " + pptFilePath);
            }

            // Create a temporary directory for this PPT's slides
            tempDir = Files.createTempDirectory(TEMP_DIR_PREFIX + UUID.randomUUID().toString());
            AppLogger.log("PptRenderer: Created temporary directory for PPT slides: " + tempDir.toString());

            Dimension pgsize = ppt.getPageSize();
            int slideNum = 0;

            for (Slide<?, ?> slide : ppt.getSlides()) {
                slideNum++;
                BufferedImage img = new BufferedImage(pgsize.width, pgsize.height, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = img.createGraphics();

                // Clear the background to white or a default color
                graphics.setPaint(Color.WHITE);
                graphics.fill(new Rectangle2D.Float(0, 0, pgsize.width, pgsize.height));

                // Render the slide
                slide.draw(graphics);

                // Add slide number to the image (optional, but good for debugging/preview)
                graphics.setColor(Color.BLACK);
                graphics.setFont(new Font("Arial", Font.BOLD, 20));
                graphics.drawString("Slide " + slideNum, 20, pgsize.height - 20);

                String outputPath = tempDir.resolve("slide_" + slideNum + ".png").toString();
                ImageIO.write(img, "png", new File(outputPath));
                imagePaths.add(outputPath);
                graphics.dispose();
                AppLogger.log("PptRenderer: Rendered slide " + slideNum + " to: " + outputPath);
            }
            ppt.close(); // Close the slideshow
            AppLogger.log("PptRenderer: Successfully rendered " + imagePaths.size() + " slides.");
        } catch (Exception e) {
            AppLogger.log("PptRenderer: Error rendering PPT to images: " + e.getMessage());
            // Clean up any partial temp directory if an error occurred
            if (tempDir != null && Files.exists(tempDir)) {
                cleanupTempDirectory(tempDir);
            }
            throw new IOException("Failed to render PPT slides: " + e.getMessage(), e);
        }
        return imagePaths;
    }

    /**
     * Cleans up a temporary directory created by PptRenderer.
     *
     * @param tempDirPath The path to the temporary directory.
     */
    public static void cleanupTempDirectory(Path tempDirPath) {
        if (tempDirPath == null || !Files.exists(tempDirPath)) {
            AppLogger.log("PptRenderer: No temporary directory to clean up or path is null/non-existent: " + tempDirPath);
            return;
        }
        try {
            Files.walk(tempDirPath)
                    .sorted(java.util.Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            AppLogger.log("PptRenderer: Cleaned up temporary directory: " + tempDirPath.toString());
        } catch (IOException e) {
            AppLogger.log("PptRenderer: Error cleaning up temporary directory " + tempDirPath.toString() + ": " + e.getMessage());
        }
    }
}

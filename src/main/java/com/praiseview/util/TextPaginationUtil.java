package com.praiseview.util;

import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;

public class TextPaginationUtil {

    /**
     * Helper method to paginate text based on font size, available width, and available height.
     * This method first splits by slide markers [==slide==], then paginates each slide section.
     *
     * @param fullText The entire text content to paginate.
     * @param fontSize The font size to use for measurement.
     * @param maxWidth The maximum width available for a line of text.
     * @param maxHeight The maximum height available for a page of text.
     * @return A list of strings, where each string represents a page of content.
     */
    public static List<String> paginateText(String fullText, double fontSize, double maxWidth, double maxHeight) {
        List<String> pages = new ArrayList<>();
        if (fullText == null || fullText.isEmpty()) {
            pages.add("");
            return pages;
        }

        // First, split by slide markers [==slide==]
        String[] slides = fullText.split("\\[==slide==\\]");
        
        // Process each slide section
        for (String slideContent : slides) {
            String trimmedSlide = slideContent.trim();
            if (trimmedSlide.isEmpty()) {
                continue; // Skip empty slide sections
            }
            
            // Paginate each slide section
            List<String> slidePages = paginateSingleSlide(trimmedSlide, fontSize, maxWidth, maxHeight);
            pages.addAll(slidePages);
        }

        // If no pages were added, return at least an empty page
        if (pages.isEmpty()) {
            pages.add("");
        }

        return pages;
    }

    /**
     * Paginates a single slide section (content between [==slide==] markers).
     */
    private static List<String> paginateSingleSlide(String slideText, double fontSize, double maxWidth, double maxHeight) {
        List<String> pages = new ArrayList<>();
        
        Font font = Font.font("System", fontSize);
        Text helperText = new Text();
        helperText.setFont(font);
        helperText.setWrappingWidth(maxWidth);

        StringBuilder currentPageContent = new StringBuilder();
        String[] lines = slideText.split("\n");

        for (String line : lines) {
            String testContent = currentPageContent.length() == 0 ? line : currentPageContent.toString() + "\n" + line;
            helperText.setText(testContent);

            if (helperText.getLayoutBounds().getHeight() > maxHeight) {
                if (currentPageContent.length() > 0) {
                    pages.add(currentPageContent.toString().trim());
                }
                helperText.setText(line);
                if (helperText.getLayoutBounds().getHeight() > maxHeight) {
                    List<String> subPages = splitLongLineByWords(line, fontSize, maxWidth, maxHeight);
                    pages.addAll(subPages);
                    currentPageContent = new StringBuilder();
                } else {
                    currentPageContent = new StringBuilder(line);
                }
            } else {
                if (currentPageContent.length() > 0) {
                    currentPageContent.append("\n");
                }
                currentPageContent.append(line);
            }
        }

        if (currentPageContent.length() > 0) {
            pages.add(currentPageContent.toString().trim());
        }

        return pages;
    }

    /**
     * Helper to split a very long line into sub-pages by words if it doesn't fit on a single page.
     */
    private static List<String> splitLongLineByWords(String line, double fontSize, double maxWidth, double maxHeight) {
        List<String> subPages = new ArrayList<>();
        Font font = Font.font("System", fontSize);
        Text helperText = new Text();
        helperText.setFont(font);
        helperText.setWrappingWidth(maxWidth);

        StringBuilder currentSubPage = new StringBuilder();
        String[] words = line.split("\\s+"); // Split by whitespace

        for (String word : words) {
            String testContent = currentSubPage.length() == 0 ? word : currentSubPage.toString() + " " + word;
            helperText.setText(testContent);

            if (helperText.getLayoutBounds().getHeight() > maxHeight) {
                if (currentSubPage.length() > 0) {
                    subPages.add(currentSubPage.toString().trim());
                    currentSubPage = new StringBuilder(word);
                } else {
                    // A single word is too long to fit on a page. This is an extreme edge case.
                    // For now, just add the word as a page.
                    subPages.add(word);
                    currentSubPage = new StringBuilder();
                }
            } else {
                if (currentSubPage.length() > 0) {
                    currentSubPage.append(" ");
                }
                currentSubPage.append(word);
            }
        }
        if (currentSubPage.length() > 0) {
            subPages.add(currentSubPage.toString().trim());
        }
        return subPages;
    }
}

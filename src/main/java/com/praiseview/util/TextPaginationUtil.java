package com.praiseview.util;

import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;

public class TextPaginationUtil {

    /**
     * Helper method to paginate text based on font size, available width, and available height.
     * This method attempts to split text by lines first, then by words if a single line is too long.
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

        Font font = Font.font("System", fontSize);
        Text helperText = new Text();
        helperText.setFont(font);
        helperText.setWrappingWidth(maxWidth); // Set the wrapping width for measurement

        StringBuilder currentPageContent = new StringBuilder();
        String[] lines = fullText.split("\n"); // Split by actual newlines first

        for (String line : lines) {
            // Test if adding this line (with a preceding newline if not the first) exceeds the current page height
            String testContent = currentPageContent.length() == 0 ? line : currentPageContent.toString() + "\n" + line;
            helperText.setText(testContent);

            if (helperText.getLayoutBounds().getHeight() > maxHeight) {
                // The current line, when added to currentPageContent, exceeds maxHeight.
                // So, currentPageContent (without the current line) forms a complete page.
                if (currentPageContent.length() > 0) {
                    pages.add(currentPageContent.toString().trim());
                }
                // Now, the 'line' itself needs to be processed.
                // Check if this single 'line' is too long to fit on a page by itself.
                helperText.setText(line); // Measure the single line
                if (helperText.getLayoutBounds().getHeight() > maxHeight) {
                    // The single line is too long, split it by words into sub-pages.
                    List<String> subPages = splitLongLineByWords(line, fontSize, maxWidth, maxHeight);
                    pages.addAll(subPages);
                    currentPageContent = new StringBuilder(); // Start fresh for the next line
                } else {
                    // The single line fits on a page by itself. Start a new page with it.
                    currentPageContent = new StringBuilder(line);
                }
            } else {
                // The line fits on the current page.
                if (currentPageContent.length() > 0) {
                    currentPageContent.append("\n");
                }
                currentPageContent.append(line);
            }
        }

        // Add any remaining content as the last page
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

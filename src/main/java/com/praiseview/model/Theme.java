package com.praiseview.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class Theme {
    private String name;
    private String fontFamily;
    private double fontSize;
    private String textColor; // Hex code e.g., #FFFFFF
    private String backgroundColor; // Hex code e.g., #000000
    private String backgroundImagePath; // Path to image file
    private String backgroundVideoPath; // Path to video file
    private String textAlignment; // e.g., "CENTER", "LEFT"
    private double lineSpacing;
    private boolean showTitle; // Whether to show the title on projection and stage view
    private boolean showTitleAsFirstSlide; // Whether to show a song title slide before lyrics

    // New properties for title font settings
    private String titleFontFamily;
    private double titleFontSize;
    private String titleTextColor;

    // Default constructor for Jackson
    public Theme() {
        // Set sensible defaults for main text
        this.name = "Default Theme";
        this.fontFamily = "Arial";
        this.fontSize = 62.0;
        this.textColor = "#FFFFFF";
        this.backgroundColor = "#000000";
        this.backgroundImagePath = null;
        this.backgroundVideoPath = null;
        this.textAlignment = "CENTER";
        this.lineSpacing = 8.0;
        this.showTitle = true; // Default to showing title
        this.showTitleAsFirstSlide = false;

        // Set sensible defaults for title
        this.titleFontFamily = "Arial";
        this.titleFontSize = 42.0; // Slightly smaller than main text
        this.titleTextColor = "#FFD700"; // Gold color for title
    }

    @JsonCreator
    public Theme(@JsonProperty("name") String name,
                 @JsonProperty("fontFamily") String fontFamily,
                 @JsonProperty("fontSize") double fontSize,
                 @JsonProperty("textColor") String textColor,
                 @JsonProperty("backgroundColor") String backgroundColor,
                 @JsonProperty("backgroundImagePath") String backgroundImagePath,
                 @JsonProperty("backgroundVideoPath") String backgroundVideoPath,
                 @JsonProperty("textAlignment") String textAlignment,
                 @JsonProperty("lineSpacing") double lineSpacing,
                 @JsonProperty("showTitle") boolean showTitle,
                 @JsonProperty("showTitleAsFirstSlide") boolean showTitleAsFirstSlide,
                 @JsonProperty("titleFontFamily") String titleFontFamily,
                 @JsonProperty("titleFontSize") double titleFontSize,
                 @JsonProperty("titleTextColor") String titleTextColor) {
        this.name = name;
        this.fontFamily = fontFamily;
        this.fontSize = fontSize;
        this.textColor = textColor;
        this.backgroundColor = backgroundColor;
        this.backgroundImagePath = backgroundImagePath;
        this.backgroundVideoPath = backgroundVideoPath;
        this.textAlignment = textAlignment;
        this.lineSpacing = lineSpacing;
        this.showTitle = showTitle;
        this.showTitleAsFirstSlide = showTitleAsFirstSlide;
        this.titleFontFamily = titleFontFamily;
        this.titleFontSize = titleFontSize;
        this.titleTextColor = titleTextColor;
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public double getFontSize() {
        return fontSize;
    }

    public String getTextColor() {
        return textColor;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public String getBackgroundImagePath() {
        return backgroundImagePath;
    }

    public String getBackgroundVideoPath() {
        return backgroundVideoPath;
    }

    public String getTextAlignment() {
        return textAlignment;
    }

    public double getLineSpacing() {
        return lineSpacing;
    }

    public boolean isShowTitle() {
        return showTitle;
    }

    public boolean isShowTitleAsFirstSlide() {
        return showTitleAsFirstSlide;
    }

    public String getTitleFontFamily() {
        return titleFontFamily;
    }

    public double getTitleFontSize() {
        return titleFontSize;
    }

    public String getTitleTextColor() {
        return titleTextColor;
    }

    // Setters
    public void setName(String name) {
        this.name = name;
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
    }

    public void setFontSize(double fontSize) {
        this.fontSize = fontSize;
    }

    public void setTextColor(String textColor) {
        this.textColor = textColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public void setBackgroundImagePath(String backgroundImagePath) {
        this.backgroundImagePath = backgroundImagePath;
    }

    public void setBackgroundVideoPath(String backgroundVideoPath) {
        this.backgroundVideoPath = backgroundVideoPath;
    }

    public void setTextAlignment(String textAlignment) {
        this.textAlignment = textAlignment;
    }

    public void setLineSpacing(double lineSpacing) {
        this.lineSpacing = lineSpacing;
    }

    public void setShowTitle(boolean showTitle) {
        this.showTitle = showTitle;
        if (showTitle) {
            this.showTitleAsFirstSlide = false;
        }
    }

    public void setShowTitleAsFirstSlide(boolean showTitleAsFirstSlide) {
        this.showTitleAsFirstSlide = showTitleAsFirstSlide;
        if (showTitleAsFirstSlide) {
            this.showTitle = false;
        }
    }

    public void setTitleFontFamily(String titleFontFamily) {
        this.titleFontFamily = titleFontFamily;
    }

    public void setTitleFontSize(double titleFontSize) {
        this.titleFontSize = titleFontSize;
    }

    public void setTitleTextColor(String titleTextColor) {
        this.titleTextColor = titleTextColor;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Theme theme = (Theme) o;
        return Double.compare(fontSize, theme.fontSize) == 0 &&
               Double.compare(lineSpacing, theme.lineSpacing) == 0 &&
               showTitle == theme.showTitle &&
               showTitleAsFirstSlide == theme.showTitleAsFirstSlide &&
               Double.compare(titleFontSize, theme.titleFontSize) == 0 &&
               Objects.equals(name, theme.name) &&
               Objects.equals(fontFamily, theme.fontFamily) &&
               Objects.equals(textColor, theme.textColor) &&
               Objects.equals(backgroundColor, theme.backgroundColor) &&
               Objects.equals(backgroundImagePath, theme.backgroundImagePath) &&
               Objects.equals(backgroundVideoPath, theme.backgroundVideoPath) &&
               Objects.equals(textAlignment, theme.textAlignment) &&
               Objects.equals(titleFontFamily, theme.titleFontFamily) &&
               Objects.equals(titleTextColor, theme.titleTextColor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, fontFamily, fontSize, textColor, backgroundColor, backgroundImagePath, backgroundVideoPath, textAlignment, lineSpacing, showTitle, showTitleAsFirstSlide, titleFontFamily, titleFontSize, titleTextColor);
    }
}

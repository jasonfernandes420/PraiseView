package com.praiseview.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ThemeTest {

    @Test
    void titleDisplayModesAreMutuallyExclusive() {
        Theme theme = new Theme();

        theme.setShowTitleAsFirstSlide(true);
        assertTrue(theme.isShowTitleAsFirstSlide());
        assertFalse(theme.isShowTitle());

        theme.setShowTitle(true);
        assertTrue(theme.isShowTitle());
        assertFalse(theme.isShowTitleAsFirstSlide());
    }

    @Test
    void defaultThemeHasProjectionFriendlyDefaults() {
        Theme theme = new Theme();

        assertEquals("Default Theme", theme.getName());
        assertEquals("#FFFFFF", theme.getTextColor());
        assertEquals("#000000", theme.getBackgroundColor());
        assertTrue(theme.isShowTitle());
    }
}

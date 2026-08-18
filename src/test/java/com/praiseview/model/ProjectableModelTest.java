package com.praiseview.model;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProjectableModelTest {

    @Test
    void mediaItemExposesOneProjectableItem() {
        MediaItem media = new MediaItem(new File("welcome.png"), MediaItem.MediaType.IMAGE);

        assertEquals("IMAGE", media.getType());
        assertEquals(1, media.getSubItemCount(48, 1920, 1080));
        assertEquals("IMAGE", media.getSubItemLabel(0));
        assertTrue(media.getSubItemContent(0, 48, 1920, 1080).contains("welcome.png"));
        assertEquals("", media.getSubItemContent(1, 48, 1920, 1080));
    }

    @Test
    void serviceItemDelegatesTitleAndTypeToItsContent() {
        Prayer prayer = new Prayer("Opening Prayer", "Let us pray", "Mass");
        ServiceItem serviceItem = new ServiceItem(prayer);

        assertEquals("Opening Prayer", serviceItem.getTitle());
        assertEquals("PRAYER", serviceItem.getType());
        assertEquals("Opening Prayer", serviceItem.toString());
    }

    @Test
    void serviceItemWithNoContentHasNoTitleOrType() {
        ServiceItem serviceItem = new ServiceItem();

        assertNull(serviceItem.getTitle());
        assertNull(serviceItem.getType());
    }

    @Test
    void textSlideNormalizesNullUiValues() {
        TextSlide textSlide = new TextSlide(null, null);

        assertEquals("", textSlide.getTitle());
        assertEquals("", textSlide.getFullContent());
        assertEquals("Untitled Text", textSlide.toString());
        assertEquals("Text Content", textSlide.getSubItemLabel(0));
        assertEquals("", textSlide.getSubItemLabel(1));
    }
}

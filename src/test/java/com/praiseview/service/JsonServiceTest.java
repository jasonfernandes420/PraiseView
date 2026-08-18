package com.praiseview.service;

import com.praiseview.model.Prayer;
import com.praiseview.model.ServiceItem;
import com.praiseview.model.Song;
import com.praiseview.model.Theme;
import com.praiseview.model.Verse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonServiceTest {

    private final JsonService jsonService = new JsonService();

    @TempDir
    Path temporaryDirectory;

    @Test
    void songsRoundTripWithTheirVersesAndOrder() {
        Song song = new Song();
        song.setTitle("Gloria");
        song.setLanguage("English");
        song.setVerses(List.of(new Verse("Verse 1", "Glory to God", Verse.VerseType.VERSE)));
        jsonService.exportSongs(List.of(song), temporaryDirectory.resolve("songs.json").toFile());

        List<Song> imported = jsonService.importSongs(temporaryDirectory.resolve("songs.json").toFile());

        assertNotNull(imported);
        assertEquals(1, imported.size());
        assertEquals("Gloria", imported.getFirst().getTitle());
        assertEquals("Glory to God", imported.getFirst().getSubItemContent(0, 48, 1920, 1080));
    }

    @Test
    void themesRoundTripWithDisplaySettings() {
        Theme theme = new Theme();
        theme.setName("Sunday");
        theme.setBackgroundColor("#123456");
        theme.setShowTitleAsFirstSlide(true);
        jsonService.exportThemes(List.of(theme), temporaryDirectory.resolve("themes.json").toFile());

        List<Theme> imported = jsonService.importThemes(temporaryDirectory.resolve("themes.json").toFile());

        assertNotNull(imported);
        assertEquals(theme, imported.getFirst());
    }

    @Test
    void serviceQueueRoundTripsDifferentProjectableTypes() {
        Song song = new Song();
        song.setTitle("Entrance");
        song.setVerses(List.of(new Verse("Verse 1", "Come in", Verse.VerseType.VERSE)));
        Prayer prayer = new Prayer("Opening", "Gather us", "Mass");
        jsonService.saveService(List.of(new ServiceItem(song), new ServiceItem(prayer)),
                temporaryDirectory.resolve("service.json").toFile());

        List<ServiceItem> imported = jsonService.loadService(temporaryDirectory.resolve("service.json").toFile());

        assertNotNull(imported);
        assertEquals(List.of("SONG", "PRAYER"), imported.stream().map(ServiceItem::getType).toList());
        assertEquals(List.of("Entrance", "Opening"), imported.stream().map(ServiceItem::getTitle).toList());
    }
}

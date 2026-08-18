package com.praiseview.service;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.praiseview.model.Prayer;
import com.praiseview.model.ServiceItem;
import com.praiseview.model.Song;
import com.praiseview.model.TextSlide; // Import Text model
import com.praiseview.model.Theme; // Import Theme
import java.io.File;
import java.util.List;

public class JsonService {

    private final ObjectMapper mapper;
    private final DefaultPrettyPrinter printer;
    
    public JsonService() {
        this.mapper = new ObjectMapper();
        // Configure pretty printer to prevent line wrapping issues with file paths
        this.printer = new DefaultPrettyPrinter();
        // Set indentation properly - use spaces, not tabs
        DefaultIndenter indenter = new DefaultIndenter("  ", "\n");
        printer.indentArraysWith(indenter);
        printer.indentObjectsWith(indenter);
    }

    public void exportSongs(List<Song> songs, File file) {
        try {
            mapper.writer(printer).writeValue(file, songs);
            System.out.println("✅ Songs exported successfully to: " + file.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Song> importSongs(File file) {
        try {
            List<Song> songs = mapper.readValue(file,
                    mapper.getTypeFactory().constructCollectionType(List.class, Song.class));
            System.out.println("✅ Imported " + songs.size() + " songs");
            return songs;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void exportPrayers(List<Prayer> prayersList, File file) {
        try {
            mapper.writer(printer).writeValue(file, prayersList);
            System.out.println("✅ Prayers exported successfully to: " + file.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Prayer> importPrayers(File file) {
        try {
            List<Prayer> prayers = mapper.readValue(file,
                    mapper.getTypeFactory().constructCollectionType(List.class, Prayer.class));
            System.out.println("✅ Imported " + prayers.size() + " prayers");
            return prayers;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void exportTexts(List<TextSlide> texts, File file) {
        try {
            mapper.writer(printer).writeValue(file, texts);
            System.out.println("✅ Texts exported successfully to: " + file.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<TextSlide> importTexts(File file) {
        try {
            List<TextSlide> texts = mapper.readValue(file,
                    mapper.getTypeFactory().constructCollectionType(List.class, TextSlide.class));
            System.out.println("✅ Imported " + texts.size() + " texts");
            return texts;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void exportThemes(List<Theme> themes, File file) {
        try {
            mapper.writer(printer).writeValue(file, themes);
            System.out.println("✅ Themes exported successfully to: " + file.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Theme> importThemes(File file) {
        try {
            List<Theme> themes = mapper.readValue(file,
                    mapper.getTypeFactory().constructCollectionType(List.class, Theme.class));
            System.out.println("✅ Imported " + themes.size() + " themes");
            return themes;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void saveService(List<ServiceItem> serviceQueue, File file) {
        try {
            mapper.writer(printer).writeValue(file, serviceQueue);
            System.out.println("✅ Service saved successfully to: " + file.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<ServiceItem> loadService(File file) {
        try {
            List<ServiceItem> service = mapper.readValue(file,
                    mapper.getTypeFactory().constructCollectionType(List.class, ServiceItem.class));
            System.out.println("✅ Loaded service with " + service.size() + " items");
            return service;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

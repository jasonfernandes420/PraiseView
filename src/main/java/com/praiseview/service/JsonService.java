package com.praiseview.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.praiseview.model.Prayer;
import com.praiseview.model.ServiceItem;
import com.praiseview.model.Song;
import com.praiseview.model.Theme; // Import Theme
import java.io.File;
import java.util.List;

public class JsonService {

    private final ObjectMapper mapper = new ObjectMapper();

    public void exportSongs(List<Song> songs, File file) {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, songs);
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
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, prayersList);
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

    public void exportThemes(List<Theme> themes, File file) {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, themes);
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
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, serviceQueue);
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

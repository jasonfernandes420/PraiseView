package com.praiseview.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.praiseview.model.Song;
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
}

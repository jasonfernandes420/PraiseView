package com.praiseview.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for sending verse/item list to phone remote
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerseListDTO {
    @JsonProperty("service_title")
    private String serviceTitle;

    @JsonProperty("content_id")
    private String contentId;

    @JsonProperty("verses")
    private List<VerseItemDTO> verses;

    @JsonProperty("current_verse_index")
    private int currentVerseIndex;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerseItemDTO {
        @JsonProperty("index")
        private int index;

        @JsonProperty("label")
        private String label;

        @JsonProperty("preview")
        private String preview;

        @JsonProperty("content")
        private String content;
    }
}

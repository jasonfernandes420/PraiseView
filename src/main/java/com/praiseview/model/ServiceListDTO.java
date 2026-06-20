package com.praiseview.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for sending service queue list to phone remote
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceListDTO {
    @JsonProperty("services")
    private List<ServiceItemDTO> services;

    @JsonProperty("current_index")
    private int currentIndex;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceItemDTO {
        @JsonProperty("id")
        private String id;

        @JsonProperty("index")
        private int index;

        @JsonProperty("title")
        private String title;

        @JsonProperty("type")
        private String type;
    }
}

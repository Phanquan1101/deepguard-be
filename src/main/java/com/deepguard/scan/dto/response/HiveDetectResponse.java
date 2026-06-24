package com.deepguard.scan.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Root response from Hive AI-Generated & Deepfake Content Detection API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class HiveDetectResponse {

    @JsonProperty("task_id")
    private String taskId;

    private String model;

    private Object metadata;

    private List<OutputItem> output;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OutputItem {

        private List<ExtraItem> extra;
        private List<ClassItem> classes;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ExtraItem {
            private String name;
            private Object value;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ClassItem {
            @JsonProperty("class")
            private String className;
            private Double value;
        }
    }
}
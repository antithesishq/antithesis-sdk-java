package com.antithesis.sdk.assertions;

import com.fasterxml.jackson.annotation.JsonProperty;

@lombok.Builder @lombok.AllArgsConstructor
class LocationInfo {
    @JsonProperty("class")
    private String className;

    @JsonProperty("function")
    private String functionName;
  
    @JsonProperty("file")
    private String fileName;

    @JsonProperty("begin_line")
    private int beginLine;

    @JsonProperty("begin_column")
    private int beginColumn;
}

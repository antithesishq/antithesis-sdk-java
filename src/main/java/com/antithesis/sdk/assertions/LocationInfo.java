package com.antithesis.sdk.assertions;

import com.fasterxml.jackson.annotation.JsonProperty;

class LocationInfo {
    @JsonProperty("class")
    private String className;

    @JsonProperty("function")
    private String functionName;
  
    @JsonProperty("file")
    private String fileName;

    @JsonProperty("begin_line")
    private int begin_line;

    @JsonProperty("begin_column")
    private int begin_column;

    LocationInfo(String className, String functionName, String fileName, int begin_line, int begin_column) {
        this.className = className;
        this.functionName = functionName;
        this.fileName = fileName;
        this.begin_line = begin_line;
        this.begin_column = begin_column;
    }
}

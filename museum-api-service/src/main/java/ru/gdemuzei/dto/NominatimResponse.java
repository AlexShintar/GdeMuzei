package ru.gdemuzei.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public record NominatimResponse(
        @JsonProperty("place_id") long placeId,
        @JsonProperty("display_name") String displayName,
        @JsonProperty("address") Map<String, String> address,
        @JsonProperty("extratags") Map<String, String> extratags
) {}

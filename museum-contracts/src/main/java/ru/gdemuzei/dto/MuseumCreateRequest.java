package ru.gdemuzei.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


public record MuseumCreateRequest(
        @NotBlank(message = "officialName is required")
        String officialName,

        @NotNull @DecimalMin(value = "-180.0") @DecimalMax("180.0")
        Double longitude,

        @NotNull @DecimalMin(value = "-90.0") @DecimalMax("90.0")
        Double latitude,

        String telegram,
        String website
) {}
package ru.gdemuzei.models;

public record OsmData(
        String locality,

        String fullAddress,

        String openingHours,

        String phone,

        String email,

        String website,

        String wikipedia,

        String wikidata,

        String heritage, // Статус объекта культурного наследия

        String startDate
) {}
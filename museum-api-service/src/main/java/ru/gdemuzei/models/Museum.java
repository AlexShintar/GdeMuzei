package ru.gdemuzei.models;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "museums")
@CompoundIndex(name = "name_city_unique_idx",
        def = "{'normalizedOfficialName': 1, 'city': 1}", unique = true, background = true)
@ToString(onlyExplicitlyIncluded = true)
public class Museum {

    @Id
    @EqualsAndHashCode.Include
    @ToString.Include
    private String id;

    @ToString.Include
    private String officialName;

    private String normalizedOfficialName;

    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJsonPoint location;

    @Indexed(background = true)
    private String locality;

    private String telegramChannel;

    private String website;

    private Set<String> adminTags;

    private OsmData osmData;

    private String addressOverride;

    @Indexed(background = true)

    private Boolean verified;

    @CreatedDate
    private Instant createdAt;

    private String createdBy;

    @LastModifiedDate
    private Instant lastUpdatedAt;

    private String lastUpdatedBy;
}

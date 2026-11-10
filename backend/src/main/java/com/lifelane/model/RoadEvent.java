package com.lifelane.model;

import com.lifelane.model.converter.LongArrayConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

/**
 * JPA entity for the {@code road_events} table.
 *
 * <p>A road event represents a traffic incident or planned closure on the road
 * network. When active, the {@link com.lifelane.service.RoutingService} uses it
 * to adjust route ETAs or avoid the affected segment.</p>
 *
 * <p>Valid {@code status} values: {@code active}, {@code inactive}.</p>
 * <p>Valid {@code eventType} values: {@code delay}, {@code closure}.</p>
 *
 * <p>The {@code affectedOsmNodeIds} column is a PostgreSQL {@code BIGINT[]}
 * converted via {@link LongArrayConverter}.</p>
 */
@Entity
@Table(name = "road_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoadEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** Raw string status: active | inactive */
    @Column(name = "status", nullable = false, length = 10)
    @Builder.Default
    private String status = "inactive";

    @Convert(converter = LongArrayConverter.class)
    @Column(name = "affected_osm_node_ids", columnDefinition = "BIGINT[]")
    @Builder.Default
    private Long[] affectedOsmNodeIds = new Long[0];

    @Column(name = "delay_seconds", nullable = false)
    @Builder.Default
    private Integer delaySeconds = 0;

    @Column(name = "event_type", nullable = false, length = 20)
    private String eventType;

    /** Centroid latitude for map display. */
    @Column(name = "latitude")
    private Double latitude;

    /** Centroid longitude for map display. */
    @Column(name = "longitude")
    private Double longitude;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime updatedAt;
}

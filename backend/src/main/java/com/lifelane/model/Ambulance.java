package com.lifelane.model;

import com.lifelane.model.converter.StringArrayConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

/**
 * JPA entity for the {@code ambulances} table.
 *
 * <p>The {@code equipment} column is a PostgreSQL {@code TEXT[]} stored as a
 * plain text array literal and converted via {@link StringArrayConverter}.</p>
 *
 * <p>Valid {@code status} values: {@code available}, {@code busy},
 * {@code out-of-service}.</p>
 */
@Entity
@Table(name = "ambulances")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ambulance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** Raw string status: available | busy | out-of-service */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Convert(converter = StringArrayConverter.class)
    @Column(name = "equipment", nullable = false, columnDefinition = "TEXT[]")
    @Builder.Default
    private String[] equipment = new String[0];

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime updatedAt;
}

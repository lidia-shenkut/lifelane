package com.lifelane.model;

import com.lifelane.model.converter.StringArrayConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

/**
 * JPA entity for the {@code hospitals} table.
 *
 * <p>The {@code capabilities} column is a PostgreSQL {@code TEXT[]} stored as a
 * plain text array literal and converted via {@link StringArrayConverter}.</p>
 */
@Entity
@Table(name = "hospitals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hospital {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Convert(converter = StringArrayConverter.class)
    @Column(name = "capabilities", nullable = false, columnDefinition = "TEXT[]")
    @Builder.Default
    private String[] capabilities = new String[0];

    @Column(name = "available", nullable = false)
    @Builder.Default
    private Boolean available = Boolean.TRUE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime updatedAt;
}

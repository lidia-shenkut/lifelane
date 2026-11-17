package com.lifelane.service;

import com.lifelane.model.RoadEvent;
import com.lifelane.repository.RoadEventRepository;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoadEventServiceProperties {

    @Property(tries = 100)
    @Label("Feature: lifelane-mini, Property: road-event status updates persist the requested status")
    void settingEitherSupportedStatusPersistsThatStatus(@ForAll("validStatuses") String status) {
        RoadEventRepository repository = mock(RoadEventRepository.class);
        RoadEvent event = RoadEvent.builder().id(42L).status("inactive").build();
        when(repository.findById(42L)).thenReturn(Optional.of(event));
        when(repository.save(event)).thenReturn(event);
        RoadEventService service = new RoadEventServiceImpl(repository);

        RoadEvent updated = service.setStatus(42L, status);

        assertSame(event, updated);
        assertEquals(status, updated.getStatus());
        verify(repository).save(event);
    }

    @Property(tries = 100)
    @Label("Feature: lifelane-mini, Property: active-road-event lookup requests only active events")
    void activeLookupDelegatesTheActiveStatusFilter(@ForAll("activeEventLists") List<RoadEvent> activeEvents) {
        RoadEventRepository repository = mock(RoadEventRepository.class);
        when(repository.findByStatus("active")).thenReturn(activeEvents);
        RoadEventService service = new RoadEventServiceImpl(repository);

        assertEquals(activeEvents, service.findActive());
        verify(repository).findByStatus("active");
    }

    @Test
    void setStatusThrowsWhenRoadEventDoesNotExist() {
        RoadEventRepository repository = mock(RoadEventRepository.class);
        when(repository.findById(404L)).thenReturn(Optional.empty());
        RoadEventService service = new RoadEventServiceImpl(repository);

        assertThrows(NoSuchElementException.class,
                () -> service.setStatus(404L, "active"));
    }

    @Provide
    Arbitrary<String> validStatuses() {
        return Arbitraries.of("active", "inactive");
    }

    @Provide
    Arbitrary<List<RoadEvent>> activeEventLists() {
        return Arbitraries.integers().between(0, 20)
                .map(count -> java.util.stream.IntStream.range(0, count)
                        .mapToObj(index -> RoadEvent.builder()
                                .id((long) index)
                                .status("active")
                                .build())
                        .toList());
    }
}
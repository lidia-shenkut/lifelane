package com.lifelane.service;

import com.lifelane.model.RoadEvent;
import com.lifelane.repository.RoadEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

/** Repository-backed operations for road-event scenario toggles. */
@Service
@RequiredArgsConstructor
public class RoadEventServiceImpl implements RoadEventService {

    private static final String ACTIVE_STATUS = "active";

    private final RoadEventRepository roadEventRepository;

    @Override
    public RoadEvent setStatus(Long id, String status) {
        RoadEvent roadEvent = roadEventRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Road event not found: " + id));
        roadEvent.setStatus(status);
        return roadEventRepository.save(roadEvent);
    }

    @Override
    public List<RoadEvent> findActive() {
        return roadEventRepository.findByStatus(ACTIVE_STATUS);
    }
}
package com.lifelane.service;

import com.lifelane.dto.response.AmbulanceDto;
import com.lifelane.model.Ambulance;
import com.lifelane.repository.AmbulanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Repository-backed ambulance fleet operations and eligibility filtering.
 */
@Service
@RequiredArgsConstructor
public class AmbulanceServiceImpl implements AmbulanceService {

    private static final String AVAILABLE_STATUS = "available";

    private final AmbulanceRepository ambulanceRepository;

    @Override
    public List<Ambulance> findAll() {
        return ambulanceRepository.findAll();
    }

    @Override
    public List<Ambulance> findEligible(Set<String> requiredEquipment) {
        Set<String> required = requiredEquipment == null ? Set.of() : requiredEquipment;
        return findAll().stream()
                .filter(ambulance -> AVAILABLE_STATUS.equals(ambulance.getStatus()))
                .filter(ambulance -> {
                    String[] equipment = ambulance.getEquipment();
                    return equipment != null && Arrays.asList(equipment).containsAll(required);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<AmbulanceDto> findAllDtos() {
        return findAll().stream()
                .map(ambulance -> AmbulanceDto.builder()
                        .id(ambulance.getId())
                        .name(ambulance.getName())
                        // A fleet listing has no incident context from which to calculate an ETA.
                        .etaToSceneSeconds(0)
                        .equipment(ambulance.getEquipment() == null
                                ? new String[0]
                                : ambulance.getEquipment().clone())
                        .build())
                .collect(Collectors.toList());
    }
}
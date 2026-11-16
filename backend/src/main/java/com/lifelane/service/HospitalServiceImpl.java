package com.lifelane.service;

import com.lifelane.dto.response.HospitalDto;
import com.lifelane.model.Hospital;
import com.lifelane.repository.HospitalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Repository-backed hospital listing and suitability filtering.
 */
@Service
@RequiredArgsConstructor
public class HospitalServiceImpl implements HospitalService {

    private final HospitalRepository hospitalRepository;

    @Override
    public List<Hospital> findAll() {
        return hospitalRepository.findAll();
    }

    @Override
    public List<Hospital> findEligible(String careType) {
        if (careType == null) {
            return List.of();
        }

        return findAll().stream()
                .filter(hospital -> Boolean.TRUE.equals(hospital.getAvailable()))
                .filter(hospital -> {
                    String[] capabilities = hospital.getCapabilities();
                    return capabilities != null && Arrays.asList(capabilities).contains(careType);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<HospitalDto> findAllDtos() {
        return findAll().stream()
                .map(hospital -> HospitalDto.builder()
                        .id(hospital.getId())
                        .name(hospital.getName())
                        // A fleet listing has no incident context from which to calculate an ETA.
                        .etaFromSceneSeconds(0)
                        .capabilities(hospital.getCapabilities() == null
                                ? new String[0]
                                : hospital.getCapabilities().clone())
                        .build())
                .collect(Collectors.toList());
    }
}
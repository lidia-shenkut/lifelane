package com.lifelane.service;

import com.lifelane.model.Hospital;
import com.lifelane.repository.HospitalRepository;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Provide;
import net.jqwik.api.Property;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HospitalServiceProperties {

    @Property(tries = 100)
    @Label("Feature: lifelane-mini, Property 3: Hospital suitability filter excludes ineligible hospitals")
    void eligibleHospitalsAreAvailableAndSupportCareType(
            @ForAll("hospitalLists") List<Hospital> hospitals,
            @ForAll("careTypes") String careType) {
        HospitalService service = serviceFor(hospitals);

        List<Hospital> eligible = service.findEligible(careType);

        List<Hospital> expected = hospitals.stream()
                .filter(hospital -> Boolean.TRUE.equals(hospital.getAvailable()))
                .filter(hospital -> Arrays.asList(hospital.getCapabilities()).contains(careType))
                .toList();
        assertEquals(expected, eligible);
        assertTrue(eligible.stream().allMatch(hospital ->
                Boolean.TRUE.equals(hospital.getAvailable())
                        && Arrays.asList(hospital.getCapabilities()).contains(careType)));
    }

    @Property(tries = 100)
    @Label("Feature: lifelane-mini, Property 4: Hospital suitability filter is a subset")
    void eligibleHospitalsNeverExceedInputList(
            @ForAll("hospitalLists") List<Hospital> hospitals,
            @ForAll("careTypes") String careType) {
        HospitalService service = serviceFor(hospitals);

        assertTrue(service.findEligible(careType).size() <= hospitals.size());
    }

    @Provide
    Arbitrary<List<Hospital>> hospitalLists() {
        Arbitrary<Boolean> availability = Arbitraries.of(Boolean.TRUE, Boolean.FALSE);
        Arbitrary<String[]> capabilities = careTypes()
                .list().ofMaxSize(5)
                .map(items -> items.toArray(new String[0]));

        return Combinators.combine(availability, capabilities)
                .as((available, hospitalCapabilities) -> Hospital.builder()
                        .available(available)
                        .capabilities(hospitalCapabilities)
                        .build())
                .list().ofMaxSize(20);
    }

    @Provide
    Arbitrary<String> careTypes() {
        return Arbitraries.of("trauma", "surgery", "ICU", "cardiology", "pediatrics");
    }

    private HospitalService serviceFor(List<Hospital> hospitals) {
        HospitalRepository repository = mock(HospitalRepository.class);
        when(repository.findAll()).thenReturn(hospitals);
        return new HospitalServiceImpl(repository);
    }
}
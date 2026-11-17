package com.lifelane.service;

import com.lifelane.model.Ambulance;
import com.lifelane.repository.AmbulanceRepository;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Provide;
import net.jqwik.api.Property;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AmbulanceServiceProperties {

    @Property(tries = 100)
    @Label("Feature: lifelane-mini, Property 1: Ambulance eligibility filter excludes ineligible ambulances")
    void eligibleAmbulancesAreAvailableAndHaveAllRequiredEquipment(
            @ForAll("ambulanceLists") List<Ambulance> ambulances,
            @ForAll("requiredEquipmentSets") Set<String> requiredEquipment) {
        AmbulanceService service = serviceFor(ambulances);

        List<Ambulance> eligible = service.findEligible(requiredEquipment);

        List<Ambulance> expected = ambulances.stream()
                .filter(ambulance -> "available".equals(ambulance.getStatus()))
                .filter(ambulance -> Arrays.asList(ambulance.getEquipment()).containsAll(requiredEquipment))
                .toList();
        assertEquals(expected, eligible);
        assertTrue(eligible.stream().allMatch(ambulance ->
                "available".equals(ambulance.getStatus())
                        && Arrays.asList(ambulance.getEquipment()).containsAll(requiredEquipment)));
    }

    @Property(tries = 100)
    @Label("Feature: lifelane-mini, Property 2: Ambulance eligibility filter is a subset")
    void eligibleAmbulancesNeverExceedInputList(
            @ForAll("ambulanceLists") List<Ambulance> ambulances,
            @ForAll("requiredEquipmentSets") Set<String> requiredEquipment) {
        AmbulanceService service = serviceFor(ambulances);

        assertTrue(service.findEligible(requiredEquipment).size() <= ambulances.size());
    }

    @Provide
    Arbitrary<List<Ambulance>> ambulanceLists() {
        Arbitrary<String> statuses = Arbitraries.of("available", "busy", "out-of-service", "unknown");
        Arbitrary<String[]> equipmentLists = Arbitraries.of(
                "AED", "oxygen", "trauma_kit", "ventilator", "monitor")
                .list().ofMaxSize(5)
                .map(items -> items.toArray(new String[0]));

        return Combinators.combine(statuses, equipmentLists)
                .as((status, equipment) -> Ambulance.builder()
                        .status(status)
                        .equipment(equipment)
                        .build())
                .list().ofMaxSize(20);
    }

    @Provide
    Arbitrary<Set<String>> requiredEquipmentSets() {
        return Arbitraries.of("AED", "oxygen", "trauma_kit", "ventilator", "monitor")
                .set().ofMaxSize(5);
    }

    private AmbulanceService serviceFor(List<Ambulance> ambulances) {
        AmbulanceRepository repository = mock(AmbulanceRepository.class);
        when(repository.findAll()).thenReturn(ambulances);
        return new AmbulanceServiceImpl(repository);
    }
}
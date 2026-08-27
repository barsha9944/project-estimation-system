package com.projectestimation.backend.opportunity.service;

import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SequenceService {

    private final EntityManager entityManager;

    public Long getCurrentValue() {
        return ((Number) entityManager
                .createNativeQuery("SELECT last_value FROM opportunities_id_seq")
                .getSingleResult())
                .longValue();
    }

    public Long getNextValue() {
        return ((Number) entityManager
                .createNativeQuery("SELECT nextval('opportunities_id_seq')")
                .getSingleResult())
                .longValue();
    }

    @Transactional
    public void setValue(Long value) {
        entityManager
                .createNativeQuery("SELECT setval('opportunities_id_seq', :value)")
                .setParameter("value", value)
                .getSingleResult();
    }
}

package com.projectestimation.backend.projectschedule.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.projectestimation.backend.psr.service.PsrService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProjectScheduleSavedEventListener {

    private final PsrService psrService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ProjectScheduleSavedEvent event) {

        System.out.println(
            "PSR EVENT RECEIVED -> opportunityId="
            + event.opportunityId()
            + ", affectedVersions="
            + event.affectedPsrVersions()
        );

        psrService.synchronizePsrs(
            event.opportunityId(),
            event.affectedPsrVersions()
        );
    }
}
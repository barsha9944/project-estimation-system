package com.projectestimation.backend.common.sync;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.projectestimation.backend.common.response.ApiResponse;

@RestController
@RequestMapping("/api/v1/opportunities/{opportunityId}/documents")
public class DocumentSseController {

    private final DocumentSyncService documentSyncService;

    public DocumentSseController(DocumentSyncService documentSyncService) {
        this.documentSyncService = documentSyncService;
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable Long opportunityId) {
        return documentSyncService.subscribe(opportunityId);
    }

    @PostMapping("/check-changes")
    public ResponseEntity<ApiResponse<List<DocumentChangeEvent>>> checkChanges(
            @PathVariable Long opportunityId
    ) {
        List<DocumentChangeEvent> changes = documentSyncService.checkForExternalChanges(opportunityId);
        return ResponseEntity.ok(
                ApiResponse.success(
                        changes.isEmpty() ? "No document changes detected" : "Document changes detected and synchronized",
                        changes
                )
        );
    }
}

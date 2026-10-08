package com.projectestimation.backend.rtm.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectestimation.backend.common.exception.ResourceNotFoundException;
import com.projectestimation.backend.opportunity.model.Opportunity;
import com.projectestimation.backend.opportunity.repository.OpportunityRepository;
import com.projectestimation.backend.rtm.ai.GeminiRtmOrchestrator;
import com.projectestimation.backend.rtm.dto.RtmResponseDto;
import com.projectestimation.backend.rtm.dto.RtmRowDto;
import com.projectestimation.backend.rtm.model.Rtm;
import com.projectestimation.backend.rtm.repository.RtmRepository;
import com.projectestimation.backend.sdd.dto.SddDto;
import com.projectestimation.backend.sdd.service.SddService;
import com.projectestimation.backend.srs.dto.SrsDto;
import com.projectestimation.backend.srs.service.SrsService;
import com.projectestimation.backend.testcase.model.TestCase;
import com.projectestimation.backend.testcase.repository.TestCaseRepository;

@ExtendWith(MockitoExtension.class)
class RtmServiceTest {

    @Mock
    private OpportunityRepository opportunityRepository;

    @Mock
    private TestCaseRepository testCaseRepository;

    @Mock
    private SrsService srsService;

    @Mock
    private SddService sddService;

    @Mock
    private SrsSddDocumentParser documentParser;

    @Mock
    private GeminiRtmOrchestrator geminiRtmOrchestrator;

    @Mock
    private RtmExcelService rtmExcelService;

    @Mock
    private RtmRepository rtmRepository;

    private ObjectMapper objectMapper;
    private RtmService rtmService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        rtmService = new RtmService(
                opportunityRepository,
                testCaseRepository,
                srsService,
                sddService,
                documentParser,
                geminiRtmOrchestrator,
                rtmExcelService,
                rtmRepository,
                objectMapper
        );
    }

    @Test
    void generateRtm_throwsWhenOpportunityNotFound() {
        when(opportunityRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> rtmService.generateRtm(99L));
    }

    @Test
    void generateRtm_throwsWhenSrsNotGenerated() {
        Opportunity opp = Opportunity.builder().id(1L).opportunityName("Test Opp").clientName("Client").build();
        when(opportunityRepository.findById(1L)).thenReturn(Optional.of(opp));
        when(srsService.getGeneratedSrs(1L)).thenReturn(null);

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> rtmService.generateRtm(1L));
        assertEquals("SRS document not found for opportunity id: 1. Please generate the SRS first before generating RTM.", ex.getMessage());
    }

    @Test
    void generateRtm_throwsWhenSddNotGenerated() {
        Opportunity opp = Opportunity.builder().id(1L).opportunityName("Test Opp").clientName("Client").build();
        when(opportunityRepository.findById(1L)).thenReturn(Optional.of(opp));
        SrsDto srsDto = mock(SrsDto.class);
        when(srsService.getGeneratedSrs(1L)).thenReturn(srsDto);
        when(sddService.getGeneratedSdd(1L)).thenReturn(null);

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> rtmService.generateRtm(1L));
        assertEquals("SDD document not found for opportunity id: 1. Please generate the SDD first before generating RTM.", ex.getMessage());
    }

    @Test
    void generateRtm_generatesAndSavesToRtmRepository() {
        Opportunity opp = Opportunity.builder().id(1L).opportunityName("Test Opp").clientName("Client").build();
        when(opportunityRepository.findById(1L)).thenReturn(Optional.of(opp));

        SrsDto srsDto = mock(SrsDto.class);
        when(srsService.getGeneratedSrs(1L)).thenReturn(srsDto);

        SddDto sddDto = mock(SddDto.class);
        when(sddService.getGeneratedSdd(1L)).thenReturn(sddDto);

        List<RtmRowDto> rows = new ArrayList<>();
        RtmRowDto row = RtmRowDto.builder()
                .serialNo("1")
                .requirementId("FR-001")
                .requirementName("User Login")
                .requirementDescription("Login functionality")
                .srsSubsection("3.1")
                .sddSubsection("4.1")
                .build();
        rows.add(row);

        when(documentParser.parseRequirements(srsDto, sddDto)).thenReturn(rows);
        when(geminiRtmOrchestrator.determineSourceCodeReferences(rows)).thenReturn(Map.of("FR-001", "AuthController.java"));

        TestCase tc = new TestCase();
        tc.setTestCaseId("TC-001");
        tc.setReqId("FR-001");
        tc.setTestCaseName("Login Test");
        when(testCaseRepository.findByOpportunityId(1L)).thenReturn(List.of(tc));

        when(rtmRepository.findByOpportunityId(1L)).thenReturn(Optional.empty());

        RtmResponseDto result = rtmService.generateRtm(1L);

        assertNotNull(result);
        assertEquals(1L, result.opportunityId());
        assertEquals(1, result.rows().size());
        assertEquals("AuthController.java", result.rows().get(0).getSourceCodeReference());
        assertEquals("TC-001", result.rows().get(0).getUnitTestCaseReference());
        assertEquals("TC-001", result.rows().get(0).getSystemTestCaseReference());

        ArgumentCaptor<Rtm> rtmCaptor = ArgumentCaptor.forClass(Rtm.class);
        verify(rtmRepository).save(rtmCaptor.capture());
        Rtm saved = rtmCaptor.getValue();
        assertNotNull(saved.getRtmData());
        assertEquals(opp, saved.getOpportunity());
    }

    @Test
    void getRtmData_returnsNullWhenNotYetGenerated() {
        when(opportunityRepository.existsById(1L)).thenReturn(true);
        when(rtmRepository.findByOpportunityId(1L)).thenReturn(Optional.empty());

        RtmResponseDto result = rtmService.getRtmData(1L);
        assertNull(result);
    }

    @Test
    void getRtmData_returnsSavedRtmFromRepository() throws Exception {
        when(opportunityRepository.existsById(1L)).thenReturn(true);

        RtmRowDto row = RtmRowDto.builder()
                .serialNo("1")
                .requirementId("FR-001")
                .requirementName("User Login")
                .build();
        RtmResponseDto storedDto = new RtmResponseDto(1L, "Test Opp", "Client", List.of(row));
        String json = objectMapper.writeValueAsString(storedDto);

        Rtm rtm = Rtm.builder()
                .id(10L)
                .rtmData(json)
                .build();

        when(rtmRepository.findByOpportunityId(1L)).thenReturn(Optional.of(rtm));

        RtmResponseDto result = rtmService.getRtmData(1L);
        assertNotNull(result);
        assertEquals(1L, result.opportunityId());
        assertEquals("Test Opp", result.opportunityName());
        assertEquals(1, result.rows().size());
        assertEquals("FR-001", result.rows().get(0).getRequirementId());
    }

    @Test
    void downloadRtm_usesSavedRtmAndGeneratesExcel() throws IOException {
        Opportunity opp = Opportunity.builder().id(1L).opportunityName("Test Opp").clientName("Client").build();
        when(opportunityRepository.findById(1L)).thenReturn(Optional.of(opp));
        when(opportunityRepository.existsById(1L)).thenReturn(true);

        RtmResponseDto storedDto = new RtmResponseDto(1L, "Test Opp", "Client", List.of());
        String json = objectMapper.writeValueAsString(storedDto);
        Rtm rtm = Rtm.builder().id(10L).rtmData(json).build();
        when(rtmRepository.findByOpportunityId(1L)).thenReturn(Optional.of(rtm));

        byte[] fakeExcel = new byte[]{1, 2, 3};
        when(rtmExcelService.generateExcel(eq(opp), any())).thenReturn(fakeExcel);

        byte[] result = rtmService.downloadRtm(1L);
        assertNotNull(result);
        assertEquals(3, result.length);
        verify(rtmExcelService).generateExcel(eq(opp), any());
    }
}

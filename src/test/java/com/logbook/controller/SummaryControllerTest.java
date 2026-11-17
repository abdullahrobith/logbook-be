package com.logbook.controller;

import com.logbook.dto.SummaryResponse;
import com.logbook.service.AuthService;
import com.logbook.service.SummaryService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SummaryControllerTest {

    @InjectMocks
    private SummaryController summaryController;

    @Mock
    private SummaryService summaryService;

    @Mock
    private AuthService authService;

    @Mock
    private HttpSession session;

    @Test
    void getSummaryReturnsResponseForCurrentUser() {
        SummaryResponse expected = new SummaryResponse(
                180,
                1,
                5,
                List.of(new SummaryResponse.TypeSummary("Pengembangan", 180)),
                List.of(new SummaryResponse.ProjectSummary("Mobile Banking", "blue", 180)),
                List.of(new SummaryResponse.TaskSummary(
                        LocalDate.of(2031, 3, 3),
                        1L,
                        "Mobile Banking",
                        "blue",
                        "ABC-1",
                        "Pengembangan",
                        "Menyelesaikan alur transfer antarbank dengan validasi rekening.",
                        "Selesai",
                        480,
                        180
                )),
                List.of(),
                List.of(),
                List.of(new SummaryResponse.ShortSummaryDay(LocalDate.of(2031, 3, 4), 1))
        );
        when(authService.requireUserId(session)).thenReturn(1L);
        when(summaryService.getSummary(1L, "WEEK", "2031-03-03")).thenReturn(expected);

        SummaryResponse response = summaryController.getSummary("WEEK", "2031-03-03", session);

        assertEquals(expected, response);
        verify(authService).requireUserId(session);
        verify(summaryService).getSummary(1L, "WEEK", "2031-03-03");
    }

    @Test
    void getSummaryRejectsUnauthenticatedUser() {
        when(authService.requireUserId(session)).thenThrow(
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Silakan login terlebih dahulu")
        );

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> summaryController.getSummary("WEEK", null, session)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        verifyNoInteractions(summaryService);
    }
}

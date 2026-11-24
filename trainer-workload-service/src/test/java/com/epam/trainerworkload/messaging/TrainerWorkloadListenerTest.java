package com.epam.trainerworkload.messaging;

import com.epam.trainerworkload.model.ActionType;
import com.epam.trainerworkload.service.TrainerWorkloadService;
import com.epam.trainerworkload.web.logging.TransactionIdFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainerWorkloadListenerTest {

    private static final String PAYLOAD = "{...}";
    private static final String TOKEN = "Bearer token";
    private static final TrainerWorkloadMessage MESSAGE = new TrainerWorkloadMessage("Nodira.Karimova", "Nodira",
            "Karimova", true, LocalDate.of(2026, 11, 10), 60, ActionType.ADD);

    @Mock
    private WorkloadMessageReader reader;

    @Mock
    private TrainerWorkloadService service;

    @Mock
    private DeadLetterPublisher deadLetterPublisher;

    private TrainerWorkloadListener listener;

    @BeforeEach
    void setUp() {
        listener = new TrainerWorkloadListener(reader, service, deadLetterPublisher, 4);
    }

    @Test
    void validMessage_isAppliedUnderItsTransactionId() {
        // given
        AtomicReference<String> transactionIdDuringProcessing = new AtomicReference<>();
        when(reader.read(PAYLOAD, TOKEN)).thenReturn(MESSAGE);
        doAnswer(invocation -> {
            transactionIdDuringProcessing.set(MDC.get(TransactionIdFilter.MDC_KEY));
            return null;
        }).when(service).apply(MESSAGE.toUpdate());

        // when
        listener.onMessage(PAYLOAD, TOKEN, "tx-1", 1);

        // then
        assertEquals("tx-1", transactionIdDuringProcessing.get());
        assertNull(MDC.get(TransactionIdFilter.MDC_KEY));
        verifyNoInteractions(deadLetterPublisher);
    }

    @Test
    void invalidMessage_isDeadLetteredWithoutProcessingOrRetry() {
        // given
        when(reader.read(PAYLOAD, null)).thenThrow(new InvalidWorkloadMessageException("Missing service token"));

        // when
        listener.onMessage(PAYLOAD, null, "tx-2", 1);

        // then
        verify(deadLetterPublisher).deadLetter(PAYLOAD, "tx-2", "Missing service token");
        verifyNoInteractions(service);
    }

    @Test
    void processingFailureBeforeLastAttempt_isRethrownForRedelivery() {
        // given
        when(reader.read(PAYLOAD, TOKEN)).thenReturn(MESSAGE);
        doThrow(new IllegalStateException("store unavailable")).when(service).apply(any());

        // when / then
        assertThrows(IllegalStateException.class, () -> listener.onMessage(PAYLOAD, TOKEN, "tx-3", 3));
        verify(deadLetterPublisher, never()).deadLetter(anyString(), anyString(), anyString());
    }

    @Test
    void processingFailureOnLastAttempt_isDeadLettered() {
        // given
        when(reader.read(PAYLOAD, TOKEN)).thenReturn(MESSAGE);
        doThrow(new IllegalStateException("store unavailable")).when(service).apply(any());

        // when
        listener.onMessage(PAYLOAD, TOKEN, "tx-4", 4);

        // then
        verify(deadLetterPublisher).deadLetter(eq(PAYLOAD), eq("tx-4"),
                eq("Processing failed after 4 attempts: store unavailable"));
    }

    @Test
    void messageWithoutTransactionIdOrDeliveryCount_getsGeneratedIdAndFirstAttempt() {
        // given
        AtomicReference<String> transactionIdDuringProcessing = new AtomicReference<>();
        when(reader.read(PAYLOAD, TOKEN)).thenReturn(MESSAGE);
        doAnswer(invocation -> {
            transactionIdDuringProcessing.set(MDC.get(TransactionIdFilter.MDC_KEY));
            throw new IllegalStateException("boom");
        }).when(service).apply(any());

        // when / then: attempt 1 of 4, so rethrown
        assertThrows(IllegalStateException.class, () -> listener.onMessage(PAYLOAD, TOKEN, null, null));
        assertEquals(36, transactionIdDuringProcessing.get().length());
    }
}

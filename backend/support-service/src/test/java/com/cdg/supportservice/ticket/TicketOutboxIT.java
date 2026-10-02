package com.cdg.supportservice.ticket;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.cdg.supportservice.security.JwtTokenService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TicketOutboxIT {
    @Autowired MockMvc mvc;
    @Autowired TicketRepository tickets;
    @Autowired TicketOutboxDispatcher dispatcher;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenService tokens;
    @MockBean OrderLookupClient orders;
    @MockBean TicketEventPublisher publisher;
    @BeforeEach void clean() { clearData(); }
    @AfterEach void cleanAfter() { clearData(); }
    private void clearData() { jdbc.update("DELETE FROM ticket_outbox"); jdbc.update("DELETE FROM ticket_comments"); tickets.deleteAll(); }
    @Test void failedPublishLeavesCommittedStatusEventPendingForReplay() throws Exception {
        String customer = "Bearer " + tokens.create(UUID.randomUUID(), "CUSTOMER");
        String agent = "Bearer " + tokens.create(UUID.randomUUID(), "SUPPORT");
        String response = mvc.perform(post("/api/v1/tickets").header("Authorization", customer)
                .contentType(MediaType.APPLICATION_JSON).content("{\"subject\":\"Help\",\"description\":\"Problem\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response).get("id").asText();
        doThrow(new IllegalStateException("broker down")).when(publisher).publish(any());
        mvc.perform(patch("/api/v1/tickets/" + id).header("Authorization", agent)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM ticket_outbox WHERE published_at IS NULL", Integer.class));
        ArgumentCaptor<TicketEvent> attempted = ArgumentCaptor.forClass(TicketEvent.class);
        verify(publisher).publish(attempted.capture());
        reset(publisher);
        dispatcher.replayPending();
        ArgumentCaptor<TicketEvent> replayed = ArgumentCaptor.forClass(TicketEvent.class);
        verify(publisher).publish(replayed.capture());
        assertEquals(attempted.getValue().eventId(), replayed.getValue().eventId());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM ticket_outbox WHERE published_at IS NULL", Integer.class));
        dispatcher.replayPending();
        verify(publisher, times(1)).publish(any());
    }
}

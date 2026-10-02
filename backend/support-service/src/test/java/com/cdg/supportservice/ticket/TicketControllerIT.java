package com.cdg.supportservice.ticket;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.cdg.supportservice.security.JwtTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
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
class TicketControllerIT {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JwtTokenService tokens;
    @Autowired TicketRepository tickets;
    @Autowired JdbcTemplate jdbc;
    @MockBean OrderLookupClient orders;
    @MockBean TicketEventPublisher events;
    UUID alice = UUID.randomUUID();
    UUID bob = UUID.randomUUID();
    UUID agent = UUID.randomUUID();

    @BeforeEach void clean() { clearData(); }
    @AfterEach void cleanAfter() { clearData(); }
    private void clearData() { jdbc.update("DELETE FROM ticket_outbox"); jdbc.update("DELETE FROM ticket_comments"); tickets.deleteAll(); }
    String auth(UUID id, String role) { return "Bearer " + tokens.create(id, role); }
    String create(UUID owner, String body) throws Exception {
        String response = mvc.perform(post("/api/v1/tickets").header("Authorization", auth(owner, "CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asText();
    }

    @Test void malformedStatusHasStandardErrorBody() throws Exception {
        String id = create(alice, "{\"subject\":\"Question\",\"description\":\"Please help\"}");
        mvc.perform(patch("/api/v1/tickets/" + id).header("Authorization", auth(agent, "SUPPORT"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"UNKNOWN\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test void ticketEndpointsRequireAuthentication() throws Exception {
        mvc.perform(get("/api/v1/tickets")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/tickets").contentType(MediaType.APPLICATION_JSON)
                .content("{\"subject\":\"Help\",\"description\":\"Problem\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test void customerCreatesTicketForOwnOrderAndSeesOnlyOwnTickets() throws Exception {
        UUID order = UUID.randomUUID();
        doNothing().when(orders).validate(eq(order), eq(alice), anyString());
        String id = create(alice, "{\"orderId\":\"" + order + "\",\"subject\":\"Damaged item\",\"description\":\"Box arrived crushed\"}");
        mvc.perform(get("/api/v1/tickets/" + id).header("Authorization", auth(alice, "CUSTOMER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.orderId").value(order.toString()));
        mvc.perform(get("/api/v1/tickets/" + id).header("Authorization", auth(bob, "CUSTOMER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/tickets").header("Authorization", auth(bob, "CUSTOMER")))
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/v1/tickets?page=0&size=1").header("Authorization", auth(alice, "CUSTOMER")))
                .andExpect(jsonPath("$.totalElements").value(1));
        verify(orders).validate(eq(order), eq(alice), anyString());
    }

    @Test void invalidOrderReferenceAndInvalidPayloadAreRejected() throws Exception {
        UUID order = UUID.randomUUID();
        doThrow(new TicketException(org.springframework.http.HttpStatus.FORBIDDEN, "FORBIDDEN", "Order belongs to another customer"))
                .when(orders).validate(eq(order), eq(alice), anyString());
        mvc.perform(post("/api/v1/tickets").header("Authorization", auth(alice, "CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"orderId\":\"" + order + "\",\"subject\":\"Wrong\",\"description\":\"Wrong\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/tickets").header("Authorization", auth(alice, "CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"subject\":\"\",\"description\":\"\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        assertEquals(0, tickets.count());
    }

    @Test void supportAgentCanAssignTicketToSelf() throws Exception {
        String id = create(alice, "{\"subject\":\"Question\",\"description\":\"Please help\"}");
        mvc.perform(patch("/api/v1/tickets/" + id).header("Authorization", auth(agent, "SUPPORT"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"assignToMe\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.assigneeId").value(agent.toString()));
    }

    @Test void supportAssignsAdvancesAndCommentsButCustomerCannotManage() throws Exception {
        String id = create(alice, "{\"subject\":\"Question\",\"description\":\"Please help\"}");
        mvc.perform(patch("/api/v1/tickets/" + id).header("Authorization", auth(alice, "CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/tickets/" + id).header("Authorization", auth(agent, "SUPPORT"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"IN_PROGRESS\",\"assigneeId\":\"" + agent + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.assigneeId").value(agent.toString()));
        mvc.perform(post("/api/v1/tickets/" + id + "/comments").header("Authorization", auth(agent, "SUPPORT"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Checking now\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.comments[0].body").value("Checking now"));
        mvc.perform(post("/api/v1/tickets/" + id + "/comments").header("Authorization", auth(bob, "CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Intrusion\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/tickets/" + id).header("Authorization", auth(agent, "SUPPORT"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));
        verify(events, times(1)).publish(any(TicketEvent.class));
    }
}

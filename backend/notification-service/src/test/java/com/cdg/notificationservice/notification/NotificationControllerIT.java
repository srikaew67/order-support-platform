package com.cdg.notificationservice.notification;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.cdg.notificationservice.security.JwtTokenService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationControllerIT {
    @Autowired MockMvc mvc;
    @Autowired NotificationRepository notifications;
    @Autowired JwtTokenService tokens;
    @BeforeEach void clean() { notifications.deleteAll(); }
    private String auth(UUID id) { return "Bearer " + tokens.create(id, "CUSTOMER"); }
    @Test void customerListsOnlyOwnNotificationsAndCannotReadAnotherCustomersDetail() throws Exception {
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        var first = notifications.save(new Notification(UUID.randomUUID(), alice, "OrderCreated",
                UUID.randomUUID(), "Order placed", "request-1"));
        notifications.save(new Notification(UUID.randomUUID(), bob, "TicketStatusChanged",
                UUID.randomUUID(), "Ticket resolved", "request-2"));
        mvc.perform(get("/api/v1/notifications?page=0&size=1").header("Authorization", auth(alice)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(first.getId().toString()));
        mvc.perform(get("/api/v1/notifications/" + first.getId()).header("Authorization", auth(bob)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isUnauthorized());
    }
}

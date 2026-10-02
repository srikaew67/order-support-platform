package com.cdg.ordersupport.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.cdg.ordersupport.security.JwtTokenService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SupportAgentLookupIT {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired JwtTokenService tokens;
    private String token(Role role) {
        User caller = users.save(new User(UUID.randomUUID() + "@example.com", "hash", "Caller", role));
        return "Bearer " + tokens.create(caller);
    }
    @Test void staffCanVerifySupportAgentButNotCustomerOrMissingUser() throws Exception {
        User agent = users.save(new User(UUID.randomUUID() + "@example.com", "hash", "Agent", Role.SUPPORT));
        User customer = users.save(new User(UUID.randomUUID() + "@example.com", "hash", "Customer", Role.CUSTOMER));
        String staff = token(Role.ADMIN);
        mvc.perform(get("/api/v1/support-agents/" + agent.getId()).header("Authorization", staff))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(agent.getId().toString()));
        mvc.perform(get("/api/v1/support-agents/" + customer.getId()).header("Authorization", staff))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/support-agents/" + UUID.randomUUID()).header("Authorization", staff))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/support-agents/" + agent.getId()).header("Authorization", token(Role.CUSTOMER)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/support-agents/" + agent.getId()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}

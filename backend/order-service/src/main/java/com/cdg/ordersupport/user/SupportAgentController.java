package com.cdg.ordersupport.user;

import com.cdg.ordersupport.common.ApiException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/support-agents")
public class SupportAgentController {
    private final UserRepository users;
    public SupportAgentController(UserRepository users) { this.users = users; }
    public record SupportAgent(UUID id) {}
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPPORT', 'ADMIN')")
    public SupportAgent get(@PathVariable UUID id) {
        User user = users.findById(id).orElseThrow(() -> missing());
        if (user.getRole() != Role.SUPPORT) throw missing();
        return new SupportAgent(user.getId());
    }
    private static ApiException missing() {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Support agent not found");
    }
}

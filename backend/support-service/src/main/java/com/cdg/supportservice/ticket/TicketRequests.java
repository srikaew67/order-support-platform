package com.cdg.supportservice.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public final class TicketRequests {
    private TicketRequests() {}
    public record Create(UUID orderId, @NotBlank @Size(max = 200) String subject,
            @NotBlank @Size(max = 4000) String description) {}
    public record Update(TicketStatus status, UUID assigneeId, Boolean assignToMe) {}
    public record Comment(@NotBlank @Size(max = 4000) String body) {}
}

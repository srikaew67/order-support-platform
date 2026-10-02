package com.cdg.supportservice.ticket;

import java.util.List;
import org.springframework.data.domain.Page;

public record TicketPage(List<TicketResponse> content, int page, int size, long totalElements, int totalPages) {
    public static TicketPage from(Page<TicketResponse> result) {
        return new TicketPage(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }
}

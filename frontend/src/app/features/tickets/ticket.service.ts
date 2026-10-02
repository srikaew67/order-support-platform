import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../../environments/environment';

export type TicketStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED';
export interface TicketComment { id: string; authorId: string; body: string; createdAt: string; }
export interface Ticket { id: string; customerId: string; orderId: string | null; subject: string;
  description: string; status: TicketStatus; assigneeId: string | null; createdAt: string;
  updatedAt: string; comments: TicketComment[]; }
export interface TicketPage { content: Ticket[]; page: number; size: number; totalElements: number; totalPages: number; }
export interface NewTicket { orderId: string | null; subject: string; description: string; }

@Injectable({ providedIn: 'root' })
export class TicketService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.api.support}/api/v1/tickets`;
  list(page = 0, size = 10) { return this.http.get<TicketPage>(this.url, { params: { page, size } }); }
  get(id: string) { return this.http.get<Ticket>(`${this.url}/${id}`); }
  create(ticket: NewTicket) { return this.http.post<Ticket>(this.url, ticket); }
  update(id: string, changes: { status?: TicketStatus; assigneeId?: string; assignToMe?: boolean }) {
    return this.http.patch<Ticket>(`${this.url}/${id}`, changes);
  }
  comment(id: string, body: string) {
    return this.http.post<Ticket>(`${this.url}/${id}/comments`, { body });
  }
}

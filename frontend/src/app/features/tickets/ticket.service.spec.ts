import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TicketService } from './ticket.service';
import { environment } from '../../../environments/environment';

describe('TicketService', () => {
  it('sends ticket creation and status updates to support API', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const service = TestBed.inject(TicketService);
    const http = TestBed.inject(HttpTestingController);
    service.create({ orderId: 'order-1', subject: 'Help', description: 'Problem' }).subscribe();
    const create = http.expectOne(`${environment.api.support}/api/v1/tickets`);
    expect(create.request.body).toEqual({ orderId: 'order-1', subject: 'Help', description: 'Problem' });
    create.flush({ id: 'ticket-1' });
    service.update('ticket-1', { status: 'IN_PROGRESS', assigneeId: 'agent-1' }).subscribe();
    const update = http.expectOne(`${environment.api.support}/api/v1/tickets/ticket-1`);
    expect(update.request.method).toBe('PATCH');
    expect(update.request.body.assigneeId).toBe('agent-1');
    update.flush({ id: 'ticket-1' });
    http.verify();
  });
});

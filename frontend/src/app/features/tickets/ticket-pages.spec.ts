import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { TicketListPage } from './ticket-list-page';
import { TicketCreatePage } from './ticket-create-page';
import { TicketDetailPage } from './ticket-detail-page';
import { environment } from '../../../environments/environment';

const ticket = { id: 'ticket-1', customerId: 'customer-1', orderId: null, subject: 'Help',
  description: 'Problem', status: 'OPEN', assigneeId: null, comments: [],
  createdAt: '2026-10-01T00:00:00Z', updatedAt: '2026-10-01T00:00:00Z' };
const base = `${environment.api.support}/api/v1/tickets`;

describe('Ticket pages', () => {
  beforeEach(() => localStorage.clear());
  it('shows a paginated ticket list and load error', () => {
    TestBed.configureTestingModule({ imports: [TicketListPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
    const fixture = TestBed.createComponent(TicketListPage);
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne(`${base}?page=0&size=10`).flush({ content: [ticket], page: 0,
      size: 10, totalElements: 11, totalPages: 2 });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Help');
    fixture.componentInstance.nextPage();
    http.expectOne(`${base}?page=1&size=10`).flush('error', { status: 503, statusText: 'Unavailable' });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role=alert]')).toBeTruthy();
    http.verify();
  });
  it('creates a ticket with an optional order reference', () => {
    TestBed.configureTestingModule({ imports: [TicketCreatePage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: { get: () => 'order-1' } } } }] });
    const fixture = TestBed.createComponent(TicketCreatePage);
    fixture.detectChanges();
    fixture.componentInstance.subject = 'Help';
    fixture.componentInstance.description = 'Problem';
    fixture.componentInstance.submit();
    const http = TestBed.inject(HttpTestingController);
    const create = http.expectOne(base);
    expect(create.request.body).toEqual({ orderId: 'order-1', subject: 'Help', description: 'Problem' });
    create.flush(ticket);
    http.verify();
  });
  it('lets a support agent advance status and add a comment', () => {
    localStorage.setItem('role', 'SUPPORT');
    TestBed.configureTestingModule({ imports: [TicketDetailPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'ticket-1' } } } }] });
    const fixture = TestBed.createComponent(TicketDetailPage);
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne(`${base}/ticket-1`).flush(ticket);
    fixture.componentInstance.assignToMe();
    const assignment = http.expectOne(`${base}/ticket-1`);
    expect(assignment.request.body).toEqual({ assignToMe: true });
    assignment.flush({ ...ticket, assigneeId: 'agent-1' });
    fixture.componentInstance.advance();
    const update = http.expectOne(`${base}/ticket-1`);
    expect(update.request.body.status).toBe('IN_PROGRESS');
    update.flush({ ...ticket, status: 'IN_PROGRESS' });
    fixture.componentInstance.commentBody = 'Checking now';
    fixture.componentInstance.addComment();
    const comment = http.expectOne(`${base}/ticket-1/comments`);
    expect(comment.request.body).toEqual({ body: 'Checking now' });
    comment.flush({ ...ticket, status: 'IN_PROGRESS', comments: [] });
    http.verify();
  });
});

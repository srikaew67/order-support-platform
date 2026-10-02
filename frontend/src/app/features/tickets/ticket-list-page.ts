import { DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { TicketPage, TicketService } from './ticket.service';

@Component({
  selector: 'app-ticket-list-page',
  imports: [DatePipe, RouterLink],
  template: `<section class="tickets">
    <header><h2>Support tickets</h2>@if (isCustomer) { <a routerLink="/tickets/new">Open ticket</a> }</header>
    @if (loading) { <p>Loading tickets…</p> }
    @if (error) { <p role="alert">{{ error }}</p> }
    @if (data) {
      @if (data.content.length === 0) { <p>No tickets yet.</p> }
      <ul>@for (ticket of data.content; track ticket.id) {
        <li><a [routerLink]="['/tickets', ticket.id]">{{ ticket.subject }}</a>
          <span>{{ ticket.status }}</span> · {{ ticket.createdAt | date:'mediumDate' }}</li>
      }</ul>
      <nav aria-label="Ticket pages"><button type="button" (click)="previousPage()" [disabled]="page === 0">Previous</button>
        <span>Page {{ page + 1 }} of {{ data.totalPages || 1 }}</span>
        <button type="button" (click)="nextPage()" [disabled]="page + 1 >= data.totalPages">Next</button></nav>
    }
  </section>`,
  styles: [`.tickets { max-width: 900px; margin: 2rem auto; padding: 0 1.5rem; }
    header, nav { display: flex; justify-content: space-between; align-items: center; gap: 1rem; }
    ul { list-style: none; padding: 0; } li { padding: 1rem; border-bottom: 1px solid #ddd; }
    li span { margin-left: 1rem; } nav { justify-content: center; margin: 2rem 0; }
    [role=alert] { color: #a42121; }`]
})
export class TicketListPage implements OnInit {
  private readonly tickets = inject(TicketService);
  private readonly auth = inject(AuthService);
  readonly isCustomer = this.auth.role() === 'CUSTOMER';
  data: TicketPage | null = null;
  page = 0;
  loading = false;
  error = '';
  ngOnInit(): void { this.load(); }
  load(): void {
    this.loading = true; this.error = '';
    this.tickets.list(this.page).subscribe({
      next: data => { this.data = data; this.loading = false; },
      error: () => { this.data = null; this.error = 'Unable to load tickets.'; this.loading = false; }
    });
  }
  nextPage(): void { if (this.data && this.page + 1 < this.data.totalPages) { this.page++; this.load(); } }
  previousPage(): void { if (this.page > 0) { this.page--; this.load(); } }
}

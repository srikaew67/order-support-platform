import { DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { Ticket, TicketService, TicketStatus } from './ticket.service';

@Component({
  selector: 'app-ticket-detail-page',
  imports: [DatePipe, FormsModule, RouterLink],
  template: `<section class="ticket-detail"><a routerLink="/tickets">← Tickets</a>
    @if (loading) { <p>Loading ticket…</p> }
    @if (error) { <p role="alert">{{ error }}</p> }
    @if (ticket) {
      <h2>{{ ticket.subject }}</h2><p>{{ ticket.description }}</p>
      <p>Status: <strong>{{ ticket.status }}</strong> · Opened {{ ticket.createdAt | date:'medium' }}</p>
      @if (ticket.orderId) { <p>Order: <a [routerLink]="['/orders', ticket.orderId]">{{ ticket.orderId }}</a></p> }
      @if (isStaff) {
        @if (isSupport) { <button type="button" (click)="assignToMe()" [disabled]="saving">Assign to me</button> }
        @if (isAdmin) {
          <label>Support agent ID<input name="supportAgentId" [(ngModel)]="supportAgentId" /></label>
          <button type="button" (click)="assignAgent()" [disabled]="saving || !supportAgentId.trim()">Assign agent</button>
        }
        @if (nextStatus) { <button type="button" (click)="advance()" [disabled]="saving">Move to {{ nextStatus }}</button> }
      }
      <h3>Conversation</h3>
      @if (ticket.comments.length === 0) { <p>No comments yet.</p> }
      <ul>@for (comment of ticket.comments; track comment.id) {
        <li><p>{{ comment.body }}</p><small>{{ comment.createdAt | date:'medium' }}</small></li>
      }</ul>
      <label>Add a comment<textarea name="commentBody" [(ngModel)]="commentBody"></textarea></label>
      <button type="button" (click)="addComment()" [disabled]="saving || !commentBody.trim()">Post comment</button>
    }
  </section>`,
  styles: [`.ticket-detail { max-width: 800px; margin: 2rem auto; padding: 0 1.5rem; }
    li { padding: .7rem 0; border-bottom: 1px solid #ddd; } ul { list-style: none; padding: 0; }
    input, textarea { display: block; width: 100%; padding: .6rem; box-sizing: border-box; margin: .5rem 0; }
    button { margin: .5rem .7rem .5rem 0; padding: .7rem; } [role=alert] { color: #a42121; }`]
})
export class TicketDetailPage implements OnInit {
  private readonly tickets = inject(TicketService);
  private readonly route = inject(ActivatedRoute);
  private readonly auth = inject(AuthService);
  readonly isSupport = this.auth.role() === 'SUPPORT';
  readonly isAdmin = this.auth.role() === 'ADMIN';
  readonly isStaff = this.isSupport || this.isAdmin;
  ticket: Ticket | null = null;
  commentBody = '';
  supportAgentId = '';
  loading = false;
  saving = false;
  error = '';
  get nextStatus(): TicketStatus | null {
    if (!this.isStaff) return null;
    switch (this.ticket?.status) {
      case 'OPEN': return 'IN_PROGRESS';
      case 'IN_PROGRESS': return 'RESOLVED';
      case 'RESOLVED': return 'CLOSED';
      default: return null;
    }
  }
  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) { this.error = 'Ticket not found.'; return; }
    this.loading = true;
    this.tickets.get(id).subscribe({
      next: ticket => { this.ticket = ticket; this.loading = false; },
      error: () => { this.error = 'Unable to load this ticket.'; this.loading = false; }
    });
  }
  assignToMe(): void {
    if (!this.ticket || !this.isStaff || this.saving) return;
    this.saving = true; this.error = '';
    this.tickets.update(this.ticket.id, { assignToMe: true }).subscribe({
      next: ticket => { this.ticket = ticket; this.saving = false; },
      error: failure => { this.error = failure.error?.message || 'Unable to assign ticket.'; this.saving = false; }
    });
  }
  assignAgent(): void {
    if (!this.ticket || !this.isAdmin || !this.supportAgentId.trim() || this.saving) return;
    this.saving = true; this.error = '';
    this.tickets.update(this.ticket.id, { assigneeId: this.supportAgentId.trim() }).subscribe({
      next: ticket => { this.ticket = ticket; this.saving = false; },
      error: failure => { this.error = failure.error?.message || 'Unable to assign ticket.'; this.saving = false; }
    });
  }
  advance(): void {
    if (!this.ticket || !this.nextStatus || this.saving) return;
    this.saving = true; this.error = '';
    this.tickets.update(this.ticket.id, { status: this.nextStatus }).subscribe({
      next: ticket => { this.ticket = ticket; this.saving = false; },
      error: failure => { this.error = failure.error?.message || 'Unable to update ticket.'; this.saving = false; }
    });
  }
  addComment(): void {
    if (!this.ticket || !this.commentBody.trim() || this.saving) return;
    this.saving = true; this.error = '';
    this.tickets.comment(this.ticket.id, this.commentBody.trim()).subscribe({
      next: ticket => { this.ticket = ticket; this.commentBody = ''; this.saving = false; },
      error: failure => { this.error = failure.error?.message || 'Unable to post comment.'; this.saving = false; }
    });
  }
}

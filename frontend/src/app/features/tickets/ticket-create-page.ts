import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TicketService } from './ticket.service';

@Component({
  selector: 'app-ticket-create-page',
  imports: [FormsModule, RouterLink],
  template: `<section class="ticket-form"><a routerLink="/tickets">← Tickets</a><h2>Open a support ticket</h2>
    @if (error) { <p role="alert">{{ error }}</p> }
    <form #form="ngForm" (ngSubmit)="submit()">
      <label>Order ID (optional)<input name="orderId" [(ngModel)]="orderId" /></label>
      <label>Subject<input name="subject" [(ngModel)]="subject" required maxlength="200" /></label>
      <label>Description<textarea name="description" [(ngModel)]="description" required maxlength="4000"></textarea></label>
      <button type="submit" [disabled]="form.invalid || saving">{{ saving ? 'Opening…' : 'Open ticket' }}</button>
    </form>
  </section>`,
  styles: [`.ticket-form { max-width: 680px; margin: 2rem auto; padding: 0 1.5rem; }
    label { display: block; margin: 1rem 0; } input, textarea { display: block; width: 100%; padding: .6rem; box-sizing: border-box; }
    textarea { min-height: 9rem; } [role=alert] { color: #a42121; }`]
})
export class TicketCreatePage {
  private readonly tickets = inject(TicketService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  orderId = this.route.snapshot.queryParamMap.get('orderId') || '';
  subject = '';
  description = '';
  saving = false;
  error = '';
  submit(): void {
    if (this.saving || !this.subject.trim() || !this.description.trim()) return;
    this.saving = true; this.error = '';
    this.tickets.create({ orderId: this.orderId.trim() || null, subject: this.subject.trim(),
      description: this.description.trim() }).subscribe({
      next: ticket => void this.router.navigate(['/tickets', ticket.id]),
      error: failure => { this.error = failure.error?.message || 'Unable to open this ticket.'; this.saving = false; }
    });
  }
}

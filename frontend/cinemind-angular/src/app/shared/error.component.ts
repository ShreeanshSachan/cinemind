import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-error',
  template: `
    <div class="error glass" role="alert">
      <div class="icon" aria-hidden="true">!</div>
      <h3>Something interrupted the show</h3>
      <p class="message">{{ message }}</p>
      <div class="actions">
        @if (showRetry) {
          <button type="button" class="btn btn-primary btn-sm" (click)="retry.emit()">
            Try again
          </button>
        }
        <a class="btn btn-ghost btn-sm" href="/">Back to search</a>
      </div>
    </div>
  `,
  styles: [
    `
      .error {
        padding: 2.25rem;
        text-align: center;
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 0.7rem;
        animation: fade-up 380ms ease both;
      }
      .icon {
        width: 46px;
        height: 46px;
        display: grid;
        place-items: center;
        border-radius: 50%;
        font-weight: 800;
        font-size: 1.3rem;
        color: var(--danger);
        background: rgba(255, 122, 138, 0.12);
        border: 1px solid rgba(255, 122, 138, 0.3);
      }
      h3 { font-size: 1.15rem; }
      .message { color: var(--muted); max-width: 46ch; font-size: 0.92rem; }
      .actions { display: flex; gap: 0.6rem; flex-wrap: wrap; justify-content: center; margin-top: 0.5rem; }
    `,
  ],
})
export class ErrorComponent {
  @Input() message = 'Something went wrong.';
  @Input() showRetry = true;
  @Output() retry = new EventEmitter<void>();
}

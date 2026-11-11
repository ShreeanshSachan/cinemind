import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-loading',
  template: `
    <div class="loading" role="status" aria-live="polite">
      <div class="orbit" aria-hidden="true"><span></span><span></span></div>
      <p class="message">{{ message }}</p>
      @if (detail) {
        <p class="detail">{{ detail }}</p>
      }
      <div class="bar" aria-hidden="true"><i></i></div>
    </div>
  `,
  styles: [
    `
      .loading {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 0.75rem;
        padding: 3.5rem 1.5rem;
        text-align: center;
        animation: fade-up 380ms ease both;
      }
      .orbit {
        position: relative;
        width: 58px;
        height: 58px;
      }
      .orbit span {
        position: absolute;
        inset: 0;
        border-radius: 50%;
        border: 2px solid transparent;
        border-top-color: var(--purple);
        border-right-color: var(--blue);
        animation: spin 1.1s linear infinite;
      }
      .orbit span:last-child {
        inset: 10px;
        border-top-color: var(--blue);
        border-right-color: var(--purple);
        animation-duration: 1.6s;
        animation-direction: reverse;
      }
      .message { font-weight: 650; font-size: 1.02rem; }
      .detail { color: var(--muted); font-size: 0.87rem; }
      .bar {
        width: 190px;
        height: 4px;
        border-radius: 4px;
        overflow: hidden;
        background: rgba(255, 255, 255, 0.07);
      }
      .bar i {
        display: block;
        width: 40%;
        height: 100%;
        border-radius: 4px;
        background: var(--gradient);
        background-size: 600px 100%;
        animation: shimmer 1.5s ease-in-out infinite;
      }
    `,
  ],
})
export class LoadingComponent {
  @Input() message = 'CineMind AI is thinking…';
  @Input() detail = '';
}

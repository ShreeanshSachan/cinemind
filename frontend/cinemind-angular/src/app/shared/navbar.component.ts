import { Component, signal } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-navbar',
  imports: [RouterLink, RouterLinkActive],
  template: `
    <header class="nav">
      <div class="container inner">
        <a class="brand" routerLink="/" (click)="close()">
          <span class="mark" aria-hidden="true"><i></i><i></i><i></i></span>
          <span class="brand-text">CineMind <b>AI</b></span>
        </a>

        <nav class="links" [class.open]="menuOpen()" aria-label="Main navigation">
          <a routerLink="/" fragment="search" routerLinkActive="active"
             [routerLinkActiveOptions]="{ exact: true }" (click)="close()">Discover</a>
          <a routerLink="/" fragment="featured" (click)="close()">Featured</a>
          <a routerLink="/trace" routerLinkActive="active" (click)="close()">Agent trace</a>
        </nav>

        <a class="cta" routerLink="/" fragment="search" (click)="close()">
          Get recommendations
        </a>

        <button type="button" class="toggle" [attr.aria-expanded]="menuOpen()"
                aria-label="Toggle navigation" (click)="toggle()">
          <span></span><span></span><span></span>
        </button>
      </div>
    </header>
  `,
  styles: [
    `
      .nav {
        position: sticky;
        top: 0;
        z-index: 20;
        border-bottom: 1px solid var(--border);
        background: rgba(10, 10, 20, 0.82);
        backdrop-filter: blur(16px);
      }
      .inner { display: flex; align-items: center; gap: 1.5rem; height: 72px; }
      .brand { display: inline-flex; align-items: center; gap: 0.65rem; font-size: 1.12rem; font-weight: 700; }
      .brand-text b {
        background: var(--gradient);
        -webkit-background-clip: text;
        background-clip: text;
        color: transparent;
      }
      .mark { display: flex; gap: 3px; align-items: flex-end; height: 22px; }
      .mark i { width: 5px; border-radius: 3px; background: var(--gradient); }
      .mark i:nth-child(1) { height: 12px; }
      .mark i:nth-child(2) { height: 22px; }
      .mark i:nth-child(3) { height: 16px; }
      .links { display: flex; gap: 1.6rem; margin-left: auto; }
      .links a {
        color: var(--muted);
        font-size: 0.9rem;
        font-weight: 600;
        transition: color 180ms ease;
      }
      .links a:hover, .links a.active { color: var(--text); }
      .cta {
        padding: 0.55rem 1.1rem;
        border-radius: 999px;
        font-size: 0.85rem;
        font-weight: 650;
        color: #fff;
        background: var(--gradient);
        box-shadow: 0 6px 20px rgba(122, 92, 255, 0.3);
      }
      .toggle { display: none; flex-direction: column; gap: 5px; background: none; border: 0; padding: 8px; }
      .toggle span { width: 22px; height: 2px; border-radius: 2px; background: var(--text); }
      @media (max-width: 860px) {
        .cta { display: none; }
        .toggle { display: flex; margin-left: auto; }
        .links {
          position: absolute;
          top: 72px;
          left: 0;
          right: 0;
          display: none;
          flex-direction: column;
          gap: 0;
          padding: 0.5rem 1.25rem 1rem;
          background: var(--bg-800);
          border-bottom: 1px solid var(--border);
        }
        .links.open { display: flex; }
        .links a { padding: 0.75rem 0; border-bottom: 1px solid var(--border); }
      }
    `,
  ],
})
export class NavbarComponent {
  readonly menuOpen = signal(false);

  toggle(): void {
    this.menuOpen.update((open) => !open);
  }

  close(): void {
    this.menuOpen.set(false);
  }
}

import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { Movie } from '../core/models/movie.model';
import { EnrichedRecommendation } from '../core/models/recommendation.model';
import { ErrorComponent } from './error.component';
import { LoadingComponent } from './loading.component';
import { NavbarComponent } from './navbar.component';
import { RecommendationListComponent } from './recommendation-list.component';

function movie(id: number): Movie {
  return {
    id,
    title: `Movie ${id}`,
    overview: null,
    genres: ['Drama'],
    releaseYear: 2012,
    runtimeMinutes: 110,
    keywords: [],
    rating: 8.0,
    posterUrl: null,
  };
}

describe('LoadingComponent', () => {
  it('shows the supplied message', async () => {
    await TestBed.configureTestingModule({ imports: [LoadingComponent] }).compileComponents();
    const fixture = TestBed.createComponent(LoadingComponent);
    fixture.componentRef.setInput('message', 'The agents are working…');
    fixture.detectChanges();
    await fixture.whenStable();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('The agents are working…');
  });
});

describe('ErrorComponent', () => {
  it('shows the message and emits retry', async () => {
    await TestBed.configureTestingModule({ imports: [ErrorComponent] }).compileComponents();
    const fixture = TestBed.createComponent(ErrorComponent);
    fixture.componentRef.setInput('message', 'Gateway is down');
    fixture.detectChanges();
    await fixture.whenStable();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('Gateway is down');

    let retried = false;
    fixture.componentInstance.retry.subscribe(() => (retried = true));
    (element.querySelector('button') as HTMLButtonElement).click();
    expect(retried).toBe(true);
  });
});

describe('NavbarComponent', () => {
  it('toggles the mobile menu', async () => {
    await TestBed.configureTestingModule({
      imports: [NavbarComponent],
      providers: [provideRouter([])],
    }).compileComponents();
    const fixture = TestBed.createComponent(NavbarComponent);
    fixture.detectChanges();
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('CineMind');
    expect(fixture.componentInstance.menuOpen()).toBe(false);
    fixture.componentInstance.toggle();
    expect(fixture.componentInstance.menuOpen()).toBe(true);
    fixture.componentInstance.close();
    expect(fixture.componentInstance.menuOpen()).toBe(false);
  });
});

describe('RecommendationListComponent', () => {
  it('renders one card per recommendation and emits the matching recommendation', async () => {
    await TestBed.configureTestingModule({
      imports: [RecommendationListComponent],
    }).compileComponents();

    const items: EnrichedRecommendation[] = [
      {
        recommendation: {
          movieId: 17,
          title: 'Movie 17',
          releaseYear: 2012,
          matchScore: 95,
          reason: 'Great match',
        },
        movie: movie(17),
      },
    ];
    const fixture = TestBed.createComponent(RecommendationListComponent);
    fixture.componentRef.setInput('items', items);
    fixture.detectChanges();
    await fixture.whenStable();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelectorAll('app-movie-card').length).toBe(1);
    expect(element.textContent).toContain('95%');

    let emitted: number | null = null;
    fixture.componentInstance.findSimilar.subscribe((rec) => (emitted = rec.movieId));
    (element.querySelector('button.similar') as HTMLButtonElement).click();
    expect(emitted).toBe(17);
  });
});

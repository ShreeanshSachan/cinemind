import { TestBed } from '@angular/core/testing';

import { Movie } from '../core/models/movie.model';
import { MovieCardComponent } from './movie-card.component';

function sampleMovie(): Movie {
  return {
    id: 17,
    title: 'Shutter Island',
    overview: 'An island asylum.',
    genres: ['Mystery', 'Thriller'],
    releaseYear: 2010,
    runtimeMinutes: 138,
    keywords: [],
    rating: 8.2,
    posterUrl: null,
  };
}

describe('MovieCardComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MovieCardComponent],
    }).compileComponents();
  });

  it('renders real movie metadata and the backend match score/reason', async () => {
    const fixture = TestBed.createComponent(MovieCardComponent);
    fixture.componentRef.setInput('movie', sampleMovie());
    fixture.componentRef.setInput('matchScore', 95);
    fixture.componentRef.setInput('reason', 'Dark psychological tension');
    fixture.detectChanges();
    await fixture.whenStable();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Shutter Island');
    expect(text).toContain('2010');
    expect(text).toContain('138 min');
    expect(text).toContain('Thriller');
    expect(text).toContain('8.2');
    expect(text).toContain('95%');
    expect(text).toContain('Dark psychological tension');
  });

  it('emits findSimilar with the clicked movie', async () => {
    const fixture = TestBed.createComponent(MovieCardComponent);
    fixture.componentRef.setInput('movie', sampleMovie());
    fixture.componentRef.setInput('showSimilarButton', true);
    fixture.detectChanges();
    await fixture.whenStable();

    const emitted: Movie[] = [];
    fixture.componentInstance.findSimilar.subscribe((movie) => emitted.push(movie));

    const button = (fixture.nativeElement as HTMLElement).querySelector(
      'button.similar',
    ) as HTMLButtonElement;
    expect(button).not.toBeNull();
    button.click();

    expect(emitted.length).toBe(1);
    expect(emitted[0].id).toBe(17);
  });

  it('shows a placeholder when there is no poster', async () => {
    const fixture = TestBed.createComponent(MovieCardComponent);
    fixture.componentRef.setInput('movie', sampleMovie());
    fixture.detectChanges();
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).querySelector('.fallback')).not.toBeNull();
  });
});

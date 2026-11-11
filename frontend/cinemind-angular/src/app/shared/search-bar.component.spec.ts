import { TestBed } from '@angular/core/testing';

import { SearchBarComponent, SearchCriteria } from './search-bar.component';

describe('SearchBarComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SearchBarComponent],
    }).compileComponents();
  });

  it('does not emit when the reactive form is invalid', () => {
    const fixture = TestBed.createComponent(SearchBarComponent);
    fixture.detectChanges();

    let emitted: SearchCriteria | null = null;
    fixture.componentInstance.search.subscribe((c) => (emitted = c));
    fixture.componentInstance.submit();

    expect(emitted).toBeNull();
    expect(fixture.componentInstance.form.controls.query.touched).toBe(true);
  });

  it('emits the criteria entered into the form', () => {
    const fixture = TestBed.createComponent(SearchBarComponent);
    fixture.componentRef.setInput('genres', ['Thriller', 'Drama']);
    fixture.detectChanges();

    const form = fixture.componentInstance.form;
    form.controls.query.setValue('a dark psychological thriller');
    form.controls.genre.setValue('Thriller');
    form.controls.mood.setValue('Mind-bending');
    form.controls.yearFrom.setValue(2010);
    form.controls.maxRuntime.setValue(140);
    form.controls.maxResults.setValue(3);

    let emitted: SearchCriteria | null = null;
    fixture.componentInstance.search.subscribe((c) => (emitted = c));
    fixture.componentInstance.submit();

    expect(emitted).toEqual({
      query: 'a dark psychological thriller',
      genre: 'Thriller',
      mood: 'Mind-bending',
      yearFrom: 2010,
      yearTo: null,
      maxRuntime: 140,
      minRating: null,
      maxResults: 3,
    });
  });

  it('fills the query from a sample prompt', () => {
    const fixture = TestBed.createComponent(SearchBarComponent);
    fixture.detectChanges();

    fixture.componentInstance.usePrompt('dark crime mysteries with a twist');

    expect(fixture.componentInstance.form.controls.query.value)
      .toBe('dark crime mysteries with a twist');
  });
});

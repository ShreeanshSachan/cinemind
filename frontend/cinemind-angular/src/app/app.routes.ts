import { Routes } from '@angular/router';

import { HomeComponent } from './pages/home/home.component';
import { RecommendationsComponent } from './pages/recommendations/recommendations.component';
import { TraceComponent } from './pages/trace/trace.component';

export const routes: Routes = [
  { path: '', component: HomeComponent, title: 'CineMind AI — Movie discovery' },
  {
    path: 'recommendations',
    component: RecommendationsComponent,
    title: 'Recommendations — CineMind AI',
  },
  { path: 'trace', component: TraceComponent, title: 'Agent trace — CineMind AI' },
  { path: '**', redirectTo: '' },
];

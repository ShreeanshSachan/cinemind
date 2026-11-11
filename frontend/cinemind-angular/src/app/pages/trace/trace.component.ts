import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { RecommendationStateService } from '../../core/services/recommendation-state.service';
import { AgentTraceComponent } from '../../shared/agent-trace.component';

@Component({
  selector: 'app-trace',
  imports: [RouterLink, AgentTraceComponent],
  templateUrl: './trace.component.html',
  styleUrl: './trace.component.css',
})
export class TraceComponent {
  readonly state = inject(RecommendationStateService);
}

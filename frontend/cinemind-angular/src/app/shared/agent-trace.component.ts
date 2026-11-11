import { Component, Input } from '@angular/core';

import { AgentTrace, MoviePreferences } from '../core/models/recommendation.model';

/** Renders the REAL agentTrace / preferences returned by the backend. */
@Component({
  selector: 'app-agent-trace',
  templateUrl: './agent-trace.component.html',
  styleUrl: './agent-trace.component.css',
})
export class AgentTraceComponent {
  @Input({ required: true }) trace!: AgentTrace;
  @Input({ required: true }) preferences!: MoviePreferences;
  @Input() retrievedDocuments = 0;

  isDone(status: string): boolean {
    return typeof status === 'string' && status.toUpperCase() === 'COMPLETED';
  }
}

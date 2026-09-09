import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { OrdemServicoService } from '../../../core/services/ordem-servico';
import { OrdemServico, StatusOs } from '../../../shared/models/ordem-servico.model';

@Component({
  selector: 'app-lista-os',
  standalone: true,
  imports: [CommonModule, FormsModule, MatTableModule, MatFormFieldModule,
            MatInputModule, MatSelectModule, MatButtonModule, MatIconModule,
            MatChipsModule, MatProgressSpinnerModule],
  templateUrl: './lista-os.html',
  styleUrl: './lista-os.css'
})
export class ListaOs implements OnInit {
  private service = inject(OrdemServicoService);
  private router  = inject(Router);

  ordens: OrdemServico[] = [];
  colunas = ['numero', 'cliente', 'veiculo', 'status', 'dataEntrada', 'totalGeral', 'acoes'];

  /** Apenas os 4 status reais do backend */
  statusOptions: { valor: StatusOs | '', label: string }[] = [
    { valor: '',             label: 'Todos'        },
    { valor: 'ABERTA',       label: 'Aberta'       },
    { valor: 'EM_ANDAMENTO', label: 'Em andamento' },
    { valor: 'CONCLUIDA',    label: 'Concluída'    },
    { valor: 'ENTREGUE',     label: 'Entregue'     },
  ];

  filtroStatus: StatusOs | '' = '';
  termoBusca = '';
  carregando = false;
  erro = '';

  ngOnInit() { this.carregar(); }

  carregar() {
    this.carregando = true;
    this.erro = '';
    this.service.listar(this.filtroStatus || undefined, this.termoBusca || undefined)
      .subscribe({
        next: (d) => { this.ordens = d; this.carregando = false; },
        error: () => { this.erro = 'Erro ao carregar ordens de serviço'; this.carregando = false; }
      });
  }

  abrirDetalhe(os: OrdemServico) {
    this.router.navigate(['/ordens', os.id]);
  }

  novaOs() {
    this.router.navigate(['/ordens', 'nova']);
  }

  corStatus(status: StatusOs): string {
    const cores: Record<StatusOs, string> = {
      ABERTA:       '#1976d2',
      EM_ANDAMENTO: '#f57c00',
      CONCLUIDA:    '#2e7d32',
      ENTREGUE:     '#388e3c',
    };
    return cores[status] ?? '#757575';
  }

  labelStatus(status: StatusOs): string {
    return this.statusOptions.find(s => s.valor === status)?.label ?? status;
  }
}



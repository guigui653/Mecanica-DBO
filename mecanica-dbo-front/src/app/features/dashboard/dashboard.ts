import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { OrdemServicoService } from '../../core/services/ordem-servico';
import { ClienteService } from '../../core/services/cliente';
import { OrdemServico } from '../../shared/models/ordem-servico.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule
  ],
  template: `
    <div class="dashboard-container">
      <div class="header">
        <h2>Dashboard</h2>
        <div class="api-badge" [class.online]="apiOnline" [class.offline]="apiOffline">
          <mat-icon>{{ apiOnline ? 'check_circle' : (apiOffline ? 'error' : 'sync') }}</mat-icon>
          <span>{{ statusTexto }}</span>
        </div>
      </div>

      @if (carregando) {
        <div class="loading-state">
          <mat-spinner diameter="40"></mat-spinner>
          <p>Conectando ao sistema...</p>
        </div>
      } @else if (apiOffline) {
        <div class="offline-card">
          <mat-icon color="warn">cloud_off</mat-icon>
          <h3>Não foi possível conectar ao Backend</h3>
          <p>Certifique-se de que a API Spring Boot está rodando no IntelliJ (porta 8080).</p>
          <button mat-stroked-button color="primary" (click)="carregarDados()">
            <mat-icon>refresh</mat-icon> Tentar novamente
          </button>
        </div>
      } @else {
        <div class="cards-grid">
          <mat-card class="stat-card blue" routerLink="/ordens">
            <mat-card-header>
              <mat-icon mat-card-avatar>build_circle</mat-icon>
              <mat-card-title>OS Abertas</mat-card-title>
              <mat-card-subtitle>Aguardando atendimento</mat-card-subtitle>
            </mat-card-header>
            <mat-card-content>
              <span class="stat-number">{{ totalAbertas }}</span>
            </mat-card-content>
          </mat-card>

          <mat-card class="stat-card green" routerLink="/clientes">
            <mat-card-header>
              <mat-icon mat-card-avatar>people</mat-icon>
              <mat-card-title>Clientes</mat-card-title>
              <mat-card-subtitle>Cadastrados na oficina</mat-card-subtitle>
            </mat-card-header>
            <mat-card-content>
              <span class="stat-number">{{ totalClientes }}</span>
            </mat-card-content>
          </mat-card>

          <mat-card class="stat-card orange" routerLink="/ordens">
            <mat-card-header>
              <mat-icon mat-card-avatar>assignment</mat-icon>
              <mat-card-title>Total de OS</mat-card-title>
              <mat-card-subtitle>Histórico completo</mat-card-subtitle>
            </mat-card-header>
            <mat-card-content>
              <span class="stat-number">{{ totalOrdens }}</span>
            </mat-card-content>
          </mat-card>
        </div>

        @if (ordensAbertas.length > 0) {
          <div class="recentes-secao">
            <h3>Ordens de Serviço Abertas Recentes</h3>
            <div class="os-mini-list">
              @for (os of ordensAbertas.slice(0, 5); track os.id) {
                <div class="os-mini-card" [routerLink]="['/ordens', os.id]">
                  <div class="os-mini-info">
                    <strong>OS #{{ os.id }}</strong>
                    <span>{{ os.reclamacoes || 'Sem descrição' }}</span>
                  </div>
                  <div class="os-mini-meta">
                    <span class="data">{{ os.dataEntrada | date:'dd/MM/yyyy' }}</span>
                    <mat-icon>chevron_right</mat-icon>
                  </div>
                </div>
              }
            </div>
          </div>
        }
      }
    </div>
  `,
  styles: [`
    .dashboard-container { max-width: 1000px; }
    .header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 24px;
    }
    .header h2 { margin: 0; }
    .api-badge {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 6px 14px;
      border-radius: 20px;
      font-size: 13px;
      font-weight: 500;
      background: #f0f0f0;
      color: #666;
    }
    .api-badge.online {
      background: #e8f5e9;
      color: #2e7d32;
    }
    .api-badge.offline {
      background: #ffebee;
      color: #c62828;
    }
    .api-badge mat-icon {
      font-size: 18px;
      width: 18px;
      height: 18px;
    }
    .cards-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
      gap: 20px;
      margin-bottom: 32px;
    }
    .stat-card {
      cursor: pointer;
      transition: transform 0.2s, box-shadow 0.2s;
      border-radius: 12px;
    }
    .stat-card:hover {
      transform: translateY(-3px);
      box-shadow: 0 6px 16px rgba(0,0,0,0.1);
    }
    .stat-number {
      font-size: 38px;
      font-weight: 700;
      display: block;
      margin-top: 12px;
      color: #333;
    }
    .stat-card.blue mat-icon { color: #1976d2; }
    .stat-card.green mat-icon { color: #2e7d32; }
    .stat-card.orange mat-icon { color: #f57c00; }
    .loading-state, .offline-card {
      text-align: center;
      padding: 48px;
      background: #fafafa;
      border-radius: 12px;
      margin-top: 24px;
    }
    .offline-card mat-icon {
      font-size: 48px;
      width: 48px;
      height: 48px;
    }
    .recentes-secao {
      background: white;
      border-radius: 12px;
      padding: 20px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.05);
    }
    .recentes-secao h3 { margin-top: 0; margin-bottom: 16px; }
    .os-mini-card {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 16px;
      border-bottom: 1px solid #eee;
      cursor: pointer;
      transition: background 0.2s;
    }
    .os-mini-card:hover { background: #f8f9fa; }
    .os-mini-info { display: flex; flex-direction: column; gap: 4px; }
    .os-mini-meta { display: flex; align-items: center; gap: 8px; color: #777; }
  `]
})
export class Dashboard implements OnInit {
  private osService = inject(OrdemServicoService);
  private clienteService = inject(ClienteService);

  carregando = true;
  apiOnline = false;
  apiOffline = false;
  statusTexto = 'Verificando conexão...';

  totalAbertas = 0;
  totalClientes = 0;
  totalOrdens = 0;
  ordensAbertas: OrdemServico[] = [];

  ngOnInit() {
    this.carregarDados();
  }

  carregarDados() {
    this.carregando = true;
    this.apiOnline = false;
    this.apiOffline = false;
    this.statusTexto = 'Conectando ao backend...';

    // Opção B: chama /ordens/abertas — se responder, a API está online!
    this.osService.listarAbertas().subscribe({
      next: (abertas) => {
        this.apiOnline = true;
        this.apiOffline = false;
        this.statusTexto = 'API Online (Porta 8080)';
        this.carregando = false;
        this.ordensAbertas = abertas;
        this.totalAbertas = abertas.length;

        // Busca clientes e total de ordens em segundo plano
        this.clienteService.listar().subscribe({
          next: (cls) => this.totalClientes = cls.length,
          error: () => {}
        });

        this.osService.buscar().subscribe({
          next: (todas) => this.totalOrdens = todas.length,
          error: () => {}
        });
      },
      error: () => {
        this.carregando = false;
        this.apiOnline = false;
        this.apiOffline = true;
        this.statusTexto = 'API Offline';
      }
    });
  }
}

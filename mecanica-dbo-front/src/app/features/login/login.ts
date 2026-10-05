import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/services/auth';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  private auth = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  usuario = '';
  senha = '';
  carregando = signal(false);
  erro = signal<string | null>(null);

  constructor() {
    // Já tem sessão ativa? Pula a tela de login.
    this.auth.verificarSessao().subscribe((ok) => {
      if (ok) this.irParaDestino();
    });
  }

  entrar(): void {
    if (this.carregando()) return;
    this.erro.set(null);
    this.carregando.set(true);

    this.auth.login(this.usuario, this.senha).subscribe({
      next: () => this.irParaDestino(),
      error: (e: HttpErrorResponse) => {
        this.carregando.set(false);
        this.senha = '';
        this.erro.set(
          e.status === 0
            ? 'Não foi possível conectar ao servidor.'
            : (e.error?.mensagem ?? 'Não foi possível entrar.'),
        );
      },
    });
  }

  private irParaDestino(): void {
    const destino = this.route.snapshot.queryParamMap.get('returnUrl');
    // só aceita caminho interno (evita redirecionar para outro site)
    const seguro = destino && destino.startsWith('/') && !destino.startsWith('//');
    this.router.navigateByUrl(seguro ? destino : '/dashboard');
  }
}

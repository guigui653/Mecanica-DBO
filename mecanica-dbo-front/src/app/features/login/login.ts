import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class Login {
  private auth = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  usuario = '';
  senha = '';
  carregando = signal(false);
  erro = signal('');

  entrar(): void {
    if (this.carregando()) return;
    if (!this.usuario.trim() || !this.senha) {
      this.erro.set('Informe o usuário e a senha.');
      return;
    }

    this.carregando.set(true);
    this.erro.set('');
    this.auth.login(this.usuario.trim(), this.senha).subscribe({
      next: () => {
        const retorno = this.route.snapshot.queryParamMap.get('retorno');
        this.router.navigateByUrl(retorno?.startsWith('/') ? retorno : '/dashboard');
      },
      error: (e) => {
        this.carregando.set(false);
        this.erro.set(e.error?.mensagem ?? 'Não foi possível entrar. Tente novamente.');
      }
    });
  }
}

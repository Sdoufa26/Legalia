import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './forgot-password.component.html',
  styleUrl: './forgot-password.component.css'
})
export class ForgotPasswordComponent {
  email = '';
  loading = false;
  successMessage = '';
  errorMessage = '';

  constructor(private http: HttpClient) {}

  onSubmit(): void {
    if (!this.email) {
      this.errorMessage = 'Veuillez saisir votre email.';
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.http.post('http://localhost:8080/api/auth/forgot-password', { email: this.email })
      .subscribe({
        next: () => {
          this.loading = false;
          this.successMessage = 'Si cet email existe, un lien de réinitialisation a été envoyé.';
        },
        error: () => {
          this.loading = false;
          this.errorMessage = 'Une erreur est survenue. Réessayez plus tard.';
        }
      });
  }
}

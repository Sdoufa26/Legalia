import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './register.component.css'
})
export class RegisterComponent {
  prenom = '';
  nom = '';
  email = '';
  password = '';
  confirm = '';
  loading = false;
  errorMessage = '';
  successMessage = '';

  constructor(private authService: AuthService, private router: Router) {}

  onSubmit(): void {
    if (!this.prenom || !this.nom || !this.email || !this.password) {
      this.errorMessage = 'Veuillez remplir tous les champs.';
      return;
    }
    if (this.password !== this.confirm) {
      this.errorMessage = 'Les mots de passe ne correspondent pas.';
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    this.authService.register({
      email: this.email,
      prenom: this.prenom,
      nom: this.nom,
      password: this.password
    }).subscribe({
      next: () => this.router.navigate(['/auth/login']),
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.erreur ?? 'Une erreur est survenue.';
      }
    });
  }
}

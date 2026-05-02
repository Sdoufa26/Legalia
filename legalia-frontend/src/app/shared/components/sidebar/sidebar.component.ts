import {Component, OnInit} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import {AuthService, UserResponse} from "../../../core/auth/auth.service";

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.css'
})
export class SidebarComponent implements OnInit{
  user: UserResponse | null = null;

  constructor(private authService : AuthService) {}

  ngOnInit() : void {
    this.user = this.authService.getCachedUser();
    this.authService.getProfile().subscribe({next: (u) => this.user = u});
  }

  get isAdmin(): boolean {
    return this.user?.role === 'ADMIN';
  }
}

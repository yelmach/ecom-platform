import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { AuthService } from '../../../core/services/auth.service';
import { ProfileDialog } from '../user-profile/user-profile';

@Component({
  selector: 'app-header',
  imports: [RouterLink, MatButton, MatIconButton, MatIcon],
  templateUrl: './header.html',
  styleUrl: './header.scss',
})
export class Header {
  private authService = inject(AuthService);
  private dialog = inject(MatDialog);

  currentUser = this.authService.currentUser;
  isAuthenticated = this.authService.isAuthenticated;

  get isSeller(): boolean {
    return this.currentUser()?.role === 'SELLER';
  }

  openProfile(): void {
    this.dialog.open(ProfileDialog, {
      data: this.currentUser(),
      width: '360px',
    });
  }

  logout(): void {
    this.authService.logout();
  }
}

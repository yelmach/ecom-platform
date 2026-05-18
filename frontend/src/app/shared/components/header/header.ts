import { Component, effect, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';
import { MediaService } from '../../../core/services/media.service';
import { ProfileDialog } from '../user-profile/user-profile';

@Component({
  selector: 'app-header',
  imports: [RouterLink, MatButtonModule, MatIconModule],
  templateUrl: './header.html',
  styleUrl: './header.scss',
})
export class Header {
  private readonly authService = inject(AuthService);
  private readonly mediaService = inject(MediaService);
  private readonly dialog = inject(MatDialog);

  readonly currentUser = this.authService.currentUser;
  readonly isAuthenticated = this.authService.isAuthenticated;
  readonly avatarUrl = signal<string | null>(null);

  constructor() {
    effect(() => {
      const user = this.currentUser();
      if (!user?.avatarMediaId) {
        this.avatarUrl.set(null);
        return;
      }

      this.mediaService.getProfile(user.userId).subscribe({
        next: (profile) => this.avatarUrl.set(profile.avatar.url),
        error: () => this.avatarUrl.set(null),
      });
    });
  }

  get isSeller(): boolean {
    return this.currentUser()?.role === 'SELLER';
  }

  logout(): void {
    this.authService.logout();
  }

  openProfile(): void {
    const user = this.currentUser();
    if (!user) {
      return;
    }

    this.dialog
      .open(ProfileDialog, { data: user, width: '550px' })
      .afterClosed()
      .subscribe((updatedUser) => {
        if (updatedUser) {
          this.authService.currentUser.set(updatedUser);
        }
      });
  }
}

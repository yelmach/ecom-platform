import { Component, effect, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { catchError, of } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { ProfileDialog } from '../user-profile/user-profile';
import { MediaService } from '../../../core/services/media.service';

@Component({
  selector: 'app-header',
  imports: [RouterLink, MatButton, MatIconButton, MatIcon],
  templateUrl: './header.html',
  styleUrl: './header.scss',
})
export class Header {
  private authService = inject(AuthService);
  private dialog = inject(MatDialog);
  private mediaService = inject(MediaService);

  currentUser = this.authService.currentUser;
  isAuthenticated = this.authService.isAuthenticated;
  avatarUrl = signal<string | null>(null);

  constructor() {
    effect((onCleanup) => {
      const user = this.currentUser();
      if (!user || !user.avatarMediaId) {
        this.avatarUrl.set(null);
        return;
      }

      const subscription = this.mediaService.getProfile(user.userId).pipe(
        catchError(() => {
          this.avatarUrl.set(null);
          return of(null);
        }),
      ).subscribe((profile) => {
        this.avatarUrl.set(profile?.avatar?.url ?? null);
      });

      onCleanup(() => subscription.unsubscribe());
    });
  }

  get isSeller(): boolean {
    return this.currentUser()?.role === 'SELLER';
  }

  openProfile(): void {
    const dialogRef = this.dialog.open(ProfileDialog, {
      data: this.currentUser(),
      width: '550px',
    });

    dialogRef.afterClosed().subscribe((updatedUser) => {
      if (updatedUser) {
        this.authService.currentUser.set(updatedUser);
      }
    });
  }

  logout(): void {
    this.authService.logout();
  }
}

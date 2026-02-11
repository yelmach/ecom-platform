import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { AuthResponse, LoginRequest, RegisterRequest } from '../models/auth';
import { User } from '../models/user';
import { map, Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  private readonly TOKEN_KEY = environment.auth.tokenStorageKey;

  public currentUser = signal<User | null>(null);
  public readonly isAuthenticated = computed(() => !!this.currentUser());

  constructor() {
    this.loadCurrentUser();
  }

  register(payload: RegisterRequest): Observable<User> {
    return this.http.post<AuthResponse>(`/auth/register`, payload).pipe(
      tap((response) => this.setSession(response)),
      map((response) => response.user),
    );
  }

  login(payload: LoginRequest): Observable<User> {
    return this.http.post<AuthResponse>(`/auth/login`, payload).pipe(
      tap((response) => this.setSession(response)),
      map((response) => response.user),
    );
  }

  logout(redirectTo = '/login'): void {
    this.currentUser.set(null);
    localStorage.removeItem(this.TOKEN_KEY);

    this.router.navigateByUrl(redirectTo);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  private setSession(response: AuthResponse): void {
    localStorage.setItem(this.TOKEN_KEY, response.token);
    this.currentUser.set(response.user);
  }

  private loadCurrentUser(): void {
    this.http.get<User>(`/users/me`).pipe(
      tap((user) => {
        this.currentUser.set(user);
      }),
    );
  }
}

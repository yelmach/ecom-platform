import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, map, tap } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AuthResponse, LoginRequest, RegisterRequest } from '../models/auth';
import { User } from '../models/user';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly tokenKey = environment.auth.tokenStorageKey;

  readonly currentUser = signal<User | null>(null);
  readonly isAuthenticated = computed(() => this.currentUser() !== null);

  register(payload: RegisterRequest): Observable<User> {
    return this.http.post<AuthResponse>(environment.auth.endpoints.register, payload).pipe(
      tap((response) => this.setSession(response)),
      map((response) => response.user),
    );
  }

  login(payload: LoginRequest): Observable<User> {
    return this.http.post<AuthResponse>(environment.auth.endpoints.login, payload).pipe(
      tap((response) => this.setSession(response)),
      map((response) => response.user),
    );
  }

  logout(redirectUrl = '/login'): void {
    localStorage.removeItem(this.tokenKey);
    this.currentUser.set(null);
    this.router.navigateByUrl(redirectUrl);
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  loadCurrentUser(): Observable<User> {
    return this.http.get<User>(environment.auth.endpoints.me).pipe(
      tap((user) => {
        this.currentUser.set(user);
      }),
    );
  }

  private setSession(response: AuthResponse): void {
    localStorage.setItem(this.tokenKey, response.token);
    this.currentUser.set(response.user);
  }
}

import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { UpdateUserRequest, User } from '../models/user';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  private http = inject(HttpClient);

  updateProfile(payload: UpdateUserRequest): Observable<User> {
    return this.http.patch<User>('/users/me', payload);
  }
}

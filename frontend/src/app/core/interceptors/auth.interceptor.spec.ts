import { TestBed } from '@angular/core/testing';
import {
  HttpClient,
  provideHttpClient,
  withInterceptors,
} from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from '../services/auth.service';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpTestingController: HttpTestingController;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
    // Create a spy for AuthService to mock token retrieval and track logout calls
    authServiceSpy = jasmine.createSpyObj('AuthService', ['getToken', 'logout']);

    TestBed.configureTestingModule({
      providers: [
        // Setup HttpClient with our functional interceptor
        provideHttpClient(withInterceptors([authInterceptor])),
        // Provide the testing controller to mock responses
        provideHttpClientTesting(),
        // Provide the mocked AuthService
        { provide: AuthService, useValue: authServiceSpy },
      ],
    });

    http = TestBed.inject(HttpClient);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    // Verify that no unmatched requests are outstanding
    httpTestingController.verify();
  });

  it('should add an Authorization header when a token exists', () => {
    authServiceSpy.getToken.and.returnValue('my-mock-token');

    // Trigger an HTTP request
    http.get('/api/data').subscribe();

    // Expect the request and inspect its headers
    const req = httpTestingController.expectOne('/api/data');
    expect(req.request.headers.has('Authorization')).toBeTrue();
    expect(req.request.headers.get('Authorization')).toBe('Bearer my-mock-token');

    req.flush({}); // Complete the request
  });

  it('should NOT add an Authorization header when no token exists', () => {
    authServiceSpy.getToken.and.returnValue(null);

    http.get('/api/data').subscribe();

    const req = httpTestingController.expectOne('/api/data');
    expect(req.request.headers.has('Authorization')).toBeFalse();

    req.flush({});
  });

  it('should call authService.logout() when a 401 error is returned', () => {
    authServiceSpy.getToken.and.returnValue('my-mock-token');

    http.get('/api/data').subscribe({
      error: (err) => expect(err.status).toBe(401),
    });

    const req = httpTestingController.expectOne('/api/data');
    // Simulate a 401 Unauthorized response from the server
    req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });

    expect(authServiceSpy.logout).toHaveBeenCalled();
  });

  it('should NOT call authService.logout() for non-401 errors', () => {
    authServiceSpy.getToken.and.returnValue('my-mock-token');

    http.get('/api/data').subscribe({
      error: (err) => expect(err.status).toBe(500),
    });

    const req = httpTestingController.expectOne('/api/data');
    // Simulate a 500 Internal Server Error
    req.flush('Server Error', { status: 500, statusText: 'Server Error' });

    expect(authServiceSpy.logout).not.toHaveBeenCalled();
  });
});
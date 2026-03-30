import { TestBed } from '@angular/core/testing';
import {
    HttpClientTestingModule,
    HttpTestingController,
} from '@angular/common/http/testing';
import { Router } from '@angular/router';

import { AuthService } from './auth.service';
import { AuthResponse, LoginRequest, RegisterRequest } from '../models/auth';
import { User } from '../models/user';
import { environment } from '../../../environments/environment';

describe('AuthService', () => {
    let service: AuthService;
    let httpTestingController: HttpTestingController;
    let routerSpy: jasmine.SpyObj<Router>;
    const tokenKey = environment.auth.tokenStorageKey;

    // Mock data to be used in tests
    const mockUser: User = {
        userId: '1',
        email: 'test@example.com',
        username: 'Test User',
        role: 'CLIENT',
        avatarMediaId: null,
    };

    const mockAuthResponse: AuthResponse = {
        token: 'mock-jwt-token',
        user: mockUser,
    };

    beforeEach(() => {
        // Create a spy object for the Router to track navigation calls
        routerSpy = jasmine.createSpyObj('Router', ['navigateByUrl']);

        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [AuthService, { provide: Router, useValue: routerSpy }],
        });

        // Inject the service, the HTTP controller, and the router spy
        service = TestBed.inject(AuthService);
        httpTestingController = TestBed.inject(HttpTestingController);

        // Clear local storage before each test to ensure isolation
        localStorage.removeItem(tokenKey);
    });

    afterEach(() => {
        // After each test, verify that there are no outstanding HTTP requests.
        httpTestingController.verify();
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    it('should have isAuthenticated as false and currentUser as null initially', () => {
        expect(service.isAuthenticated()).toBe(false);
        expect(service.currentUser()).toBeNull();
    });

    describe('#register', () => {
        it('should send a POST request and set session on successful registration', () => {
            const registerPayload: RegisterRequest = {
                email: 'test@example.com',
                password: 'password123',
                username: 'Test User',
                role: 'CLIENT'
            };

            service.register(registerPayload).subscribe((user) => {
                expect(user).toEqual(mockUser);
                expect(service.currentUser()).toEqual(mockUser);
                expect(service.isAuthenticated()).toBe(true);
                expect(localStorage.getItem(tokenKey)).toBe(mockAuthResponse.token);
            });

            const req = httpTestingController.expectOne('/auth/register');
            expect(req.request.method).toBe('POST');
            expect(req.request.body).toEqual(registerPayload);
            req.flush(mockAuthResponse);
        });
    });

    describe('#login', () => {
        it('should send a POST request and set session on successful login', () => {
            const loginPayload: LoginRequest = {
                email: 'test@example.com',
                password: 'password123',
            };

            service.login(loginPayload).subscribe((user) => {
                expect(user).toEqual(mockUser);
                expect(service.currentUser()).toEqual(mockUser);
                expect(service.isAuthenticated()).toBe(true);
                expect(localStorage.getItem(tokenKey)).toBe(mockAuthResponse.token);
            });

            const req = httpTestingController.expectOne('/auth/login');
            expect(req.request.method).toBe('POST');
            expect(req.request.body).toEqual(loginPayload);
            req.flush(mockAuthResponse);
        });
    });

    describe('#logout', () => {
        it('should clear session and navigate to the default URL', () => {
            // First, simulate a login to set the session state
            localStorage.setItem(tokenKey, 'some-token');
            service.currentUser.set(mockUser);
            expect(service.isAuthenticated()).toBe(true);

            service.logout();

            expect(service.currentUser()).toBeNull();
            expect(service.isAuthenticated()).toBe(false);
            expect(localStorage.getItem(tokenKey)).toBeNull();
            expect(routerSpy.navigateByUrl).toHaveBeenCalledWith('/login');
        });

        it('should navigate to a custom redirect URL on logout', () => {
            service.logout('/home');
            expect(routerSpy.navigateByUrl).toHaveBeenCalledWith('/home');
        });
    });

    describe('#getToken', () => {
        it('should return the token from localStorage', () => {
            localStorage.setItem(tokenKey, 'my-secret-token');
            expect(service.getToken()).toBe('my-secret-token');
        });

        it('should return null if token is not in localStorage', () => {
            expect(service.getToken()).toBeNull();
        });
    });

    describe('#loadCurrentUser', () => {
        it('should send a GET request and set the current user', () => {
            service.loadCurrentUser().subscribe((user) => {
                expect(user).toEqual(mockUser);
                expect(service.currentUser()).toEqual(mockUser);
                expect(service.isAuthenticated()).toBe(true);
            });

            const req = httpTestingController.expectOne('/users/me');
            expect(req.request.method).toBe('GET');
            req.flush(mockUser);
        });
    });
});
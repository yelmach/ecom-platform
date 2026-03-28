import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { authGuard } from './auth.guard';

describe('authGuard', () => {
  let routerSpy: jasmine.SpyObj<Router>;
  let authServiceSpy: jasmine.SpyObj<AuthService>; //Create a spy object that has the same method names as AuthService

  beforeEach(() => {
    // Create spies for the dependencies
    routerSpy = jasmine.createSpyObj('Router', ['createUrlTree']);
    authServiceSpy = jasmine.createSpyObj('AuthService', ['isAuthenticated']);

    TestBed.configureTestingModule({
      providers: [
        { provide: Router, useValue: routerSpy },
        { provide: AuthService, useValue: authServiceSpy },
      ],
    });
  });

  it('should be created', () => {
    expect(authGuard).toBeTruthy();
  });

  it('should allow access (return true) if the user is not authenticated', () => {
    // Arrange: Simulate an unauthenticated user
    authServiceSpy.isAuthenticated.and.returnValue(false);

    // Act: Run the guard function inside the test injection context
    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot)
    );

    // Assert: It should return true
    expect(result).toBe(true);
    expect(routerSpy.createUrlTree).not.toHaveBeenCalled();
  });

  it('should deny access and redirect to /shop if the user is authenticated', () => {

    const dummyUrlTree = {} as UrlTree;
    authServiceSpy.isAuthenticated.and.returnValue(true);
    routerSpy.createUrlTree.and.returnValue(dummyUrlTree);


    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot)
    );

    expect(result).toBe(dummyUrlTree);
    expect(routerSpy.createUrlTree).toHaveBeenCalledWith(['/shop']);
  });
});
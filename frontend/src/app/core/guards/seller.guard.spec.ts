import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree } from '@angular/router';

import { sellerGuard } from './seller.guard';
import { AuthService } from '../services/auth.service';
import { User } from '../models/user';

describe('sellerGuard', () => {
  let routerSpy: jasmine.SpyObj<Router>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  const mockUrlTree = {} as UrlTree;

  const mockClientUser: User = {
    userId: 'client-123',
    email: 'client@example.com',
    username: 'Test Client',
    role: 'CLIENT',
    avatarMediaId: null,
  };

  const mockSellerUser: User = {
    userId: 'seller-123',
    email: 'seller@example.com',
    username: 'Test Seller',
    role: 'SELLER',
    avatarMediaId: null,
  };

  beforeEach(() => {
    routerSpy = jasmine.createSpyObj('Router', ['createUrlTree']);
    authServiceSpy = jasmine.createSpyObj('AuthService', ['currentUser']);

    // Mock createUrlTree to simply return our mockUrlTree object
    routerSpy.createUrlTree.and.returnValue(mockUrlTree);

    TestBed.configureTestingModule({
      providers: [
        { provide: Router, useValue: routerSpy },
        { provide: AuthService, useValue: authServiceSpy },
      ],
    });
  });

  // Helper to run the guard in the proper injection context
  const runGuard = () => {
    return TestBed.runInInjectionContext(() =>
      sellerGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot)
    );
  };

  it('should be created', () => {
    expect(sellerGuard).toBeTruthy();
  });

  it('should redirect to /login if there is no current user', () => {
    authServiceSpy.currentUser.and.returnValue(null);

    const result = runGuard();

    expect(result).toBe(mockUrlTree);
    expect(routerSpy.createUrlTree).toHaveBeenCalledWith(['/login']);
  });

  it('should redirect to /shop if the current user is not a SELLER', () => {
    authServiceSpy.currentUser.and.returnValue(mockClientUser);

    const result = runGuard();

    expect(result).toBe(mockUrlTree);
    expect(routerSpy.createUrlTree).toHaveBeenCalledWith(['/shop']);
  });

  it('should allow access (return true) if the current user is a SELLER', () => {
    authServiceSpy.currentUser.and.returnValue(mockSellerUser);

    const result = runGuard();

    expect(result).toBe(true);
    expect(routerSpy.createUrlTree).not.toHaveBeenCalled();
  });
});
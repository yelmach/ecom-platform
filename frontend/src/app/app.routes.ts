import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { sellerGuard } from './core/guards/seller.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: '/shop',
    pathMatch: 'full',
  },
  {
    path: 'register',
    loadComponent: () => import('./features/auth/register/register').then((m) => m.Register),
    canActivate: [authGuard],
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login').then((m) => m.Login),
    canActivate: [authGuard],
  },
  {
    path: 'shop',
    loadComponent: () =>
      import('./features/shop/product-list/product-list').then((m) => m.ProductList),
  },
  {
    path: 'seller',
    loadComponent: () => import('./features/seller/dashboard/dashboard').then((m) => m.Dashboard),
    canActivate: [sellerGuard],
  },
];

import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    redirectTo: '/shop',
    pathMatch: 'full',
  },
  {
    path: 'register',
    loadComponent: () => import('./features/auth/register/register').then((m) => m.Register),
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login').then((m) => m.Login),
  },
  {
    path: 'shop',
    loadComponent: () => import('./features/shop/product-list/product-list').then((m) => m.ProductList),
  },
  {
    path: 'seller',
    loadComponent: () => import('./features/seller/dashboard/dashboard').then((m) => m.Dashboard),
  },
];

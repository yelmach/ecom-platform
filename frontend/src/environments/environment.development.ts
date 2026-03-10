export const environment = {
  production: false,
  apiUrl: '',
  auth: {
    tokenStorageKey: 'jwt_token',
    endpoints: {
      login: '/auth/login',
      register: '/auth/register',
      me: '/users/me',
    },
  },
};

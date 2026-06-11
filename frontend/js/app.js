document.addEventListener('DOMContentLoaded', () => {
  const hash = window.location.hash || '';
  if (hash.startsWith('#submit')) {
    const query = hash.split('?')[1] || '';
    const params = {};
    new URLSearchParams(query).forEach((v, k) => params[k] = v);
    Router.go('submit', params);
  } else {
    Router.go(Auth.isLoggedIn() ? 'dashboard' : 'login');
  }
});

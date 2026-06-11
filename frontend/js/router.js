const Router = {
  current: null,
  routes: {},

  register(name, fn) { this.routes[name] = fn; },

  go(page, params = {}) {
    this.current = page;
    const app = document.getElementById('app');
    if (!Auth.isLoggedIn() && page !== 'login' && page !== 'submit') { this.go('login'); return; }
    if (Auth.isLoggedIn() && page === 'login') { this.go('dashboard'); return; }
    const fn = this.routes[page];
    if (fn) fn(app, params);
  }
};

Router.register('login', (app) => {
  app.innerHTML = `
    <div class="login-wrap">
      <div class="login-box">
        <div class="login-logo">
          <h1>Resume<span>IQ</span></h1>
          <p>AI-Assisted Recruitment Platform</p>
        </div>
        <div id="login-alert"></div>
        <div class="form-group">
          <label class="form-label">Email Address</label>
          <input type="email" id="login-email" placeholder="admin@resumeiq.local" value="admin@resumeiq.local"/>
        </div>
        <div class="form-group">
          <label class="form-label">Password</label>
          <input type="password" id="login-pass" placeholder="••••••••" value="admin123"/>
        </div>
        <button class="btn btn-primary" style="width:100%;justify-content:center;padding:11px" id="login-btn" onclick="LoginPage.submit()">
          <i class="fa-solid fa-sign-in-alt"></i> Sign In
        </button>
        <div style="margin-top:20px;padding:14px;background:var(--gray-50);border-radius:var(--radius);font-size:12px;color:var(--gray-500)">
          <strong>Demo credentials:</strong><br/>
          Admin: admin@resumeiq.local / admin123<br/>
          Recruiter: recruiter@resumeiq.local / recruit123
        </div>
      </div>
    </div>`;

  document.getElementById('login-pass').addEventListener('keydown', e => {
    if (e.key === 'Enter') LoginPage.submit();
  });
});

const LoginPage = {
  async submit() {
    const email = document.getElementById('login-email').value.trim();
    const pass  = document.getElementById('login-pass').value;
    const btn   = document.getElementById('login-btn');
    const alertEl = document.getElementById('login-alert');

    if (!email || !pass) {
      alertEl.innerHTML = C.alert('Please enter email and password.', 'error');
      return;
    }

    btn.innerHTML = '<div class="spinner"></div> Signing in...';
    btn.disabled = true;

    try {
      const res = await api.post('/auth/login', { email, password: pass });
      if (res && res.token) {
        Auth.login(res);
        Router.go('dashboard');
      } else {
        alertEl.innerHTML = C.alert(res?.error || 'Login failed. Check credentials.', 'error');
        btn.innerHTML = '<i class="fa-solid fa-sign-in-alt"></i> Sign In';
        btn.disabled = false;
      }
    } catch (e) {
      alertEl.innerHTML = C.alert('Cannot connect to server. Is the backend running?', 'error');
      btn.innerHTML = '<i class="fa-solid fa-sign-in-alt"></i> Sign In';
      btn.disabled = false;
    }
  }
};

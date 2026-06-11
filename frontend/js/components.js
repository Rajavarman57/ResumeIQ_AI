const C = {
  // Score bar
  scoreBar(val, color = null) {
    const pct = Math.round(val);
    const col = color || (pct >= 70 ? 'var(--green)' : pct >= 45 ? 'var(--amber)' : 'var(--red)');
    return `<div class="score-bar-wrap">
      <div class="score-bar"><div class="score-bar-fill" style="width:${pct}%;background:${col}"></div></div>
      <span class="score-val" style="color:${col}">${pct}%</span>
    </div>`;
  },

  // Status badge
  statusBadge(status) {
    const map = {
      APPLIED: 'badge-blue', UNDER_REVIEW: 'badge-purple',
      SELECTED: 'badge-green', WAITLISTED: 'badge-amber', REJECTED: 'badge-red'
    };
    const labels = { APPLIED:'Applied', UNDER_REVIEW:'Under Review',
                     SELECTED:'Selected', WAITLISTED:'Waitlisted', REJECTED:'Rejected' };
    return `<span class="badge ${map[status]||'badge-gray'}">${labels[status]||status}</span>`;
  },

  // Skills display
  skills(skillStr, max = 8) {
    if (!skillStr) return '<span style="color:var(--gray-400);font-size:12px">No skills detected</span>';
    const arr = skillStr.split(',').map(s=>s.trim()).filter(Boolean);
    const shown = arr.slice(0, max);
    const rest = arr.length - max;
    return shown.map(s=>`<span class="skill-tag">${s}</span>`).join('') +
           (rest > 0 ? `<span class="skill-tag" style="background:var(--gray-100);color:var(--gray-600)">+${rest} more</span>` : '');
  },

  // Alert
  alert(msg, type = 'info') {
    const icons = { success:'fa-check-circle', error:'fa-exclamation-circle', info:'fa-info-circle' };
    return `<div class="alert alert-${type}"><i class="fa-solid ${icons[type]}"></i>${msg}</div>`;
  },

  // Loading
  loading() { return `<div class="loading-center"><div class="spinner"></div></div>`; },

  // Empty state
  empty(icon, title, sub = '') {
    return `<div class="empty-state">
      <i class="fa-solid ${icon}"></i>
      <h3>${title}</h3>${sub ? `<p>${sub}</p>` : ''}
    </div>`;
  },

  // Main layout wrapper
  layout(page, content, subtitle = '') {
    const user = Auth.user() || {};
    const initials = (user.fullName || 'U').split(' ').map(n=>n[0]).join('').toUpperCase().slice(0,2);

    const navItems = [
      { id:'dashboard', icon:'fa-gauge', label:'Dashboard' },
      { id:'jobs',      icon:'fa-briefcase', label:'Job Roles' },
      { id:'upload',    icon:'fa-cloud-arrow-up', label:'Upload Resume' },
      { id:'candidates',icon:'fa-users', label:'Candidates' },
    ];
    if (user.role === 'ADMIN') {
      navItems.push({ id:'analytics', icon:'fa-chart-bar', label:'Analytics' });
    }

    const pageLabels = { dashboard:'Dashboard', jobs:'Job Roles', upload:'Upload Resume',
                         candidates:'Candidates', analytics:'Analytics & Reports' };

    const nav = navItems.map(n => `
      <div class="nav-item ${page===n.id?'active':''}" onclick="Router.go('${n.id}')">
        <i class="fa-solid ${n.icon}"></i>${n.label}
      </div>`).join('');

    return `
      <div class="sidebar">
        <div class="sidebar-logo">
          <h2>Resume<span>IQ</span></h2>
          <p>AI Recruitment Platform</p>
        </div>
        <nav class="sidebar-nav">${nav}</nav>
        <div class="sidebar-footer">
          <div class="user-info">
            <div class="user-avatar">${initials}</div>
            <div>
              <div class="user-name">${user.fullName||'User'}</div>
              <div class="user-role">${user.role||''}</div>
            </div>
          </div>
          <button class="btn-logout" onclick="Auth.logout()">
            <i class="fa-solid fa-sign-out-alt"></i> Sign Out
          </button>
        </div>
      </div>
      <div class="main-content">
        <div class="topbar">
          <div>
            <div class="topbar-title">${pageLabels[page]||page}</div>
            ${subtitle ? `<div class="topbar-sub">${subtitle}</div>` : ''}
          </div>
        </div>
        <div class="page-content fade-in">${content}</div>
      </div>`;
  },

  // Modal
  modal(id, title, body, footer = '') {
    return `<div class="modal-overlay" id="${id}-overlay" onclick="if(event.target===this)C.closeModal('${id}')">
      <div class="modal">
        <div class="modal-header">
          <div class="modal-title">${title}</div>
          <button class="modal-close" onclick="C.closeModal('${id}')">&times;</button>
        </div>
        <div>${body}</div>
        ${footer ? `<div class="modal-footer">${footer}</div>` : ''}
      </div>
    </div>`;
  },

  openModal(id) { document.getElementById(id+'-overlay').style.display='flex'; },
  closeModal(id) {
    const el = document.getElementById(id+'-overlay');
    if (el) el.remove();
  },

  showToast(msg, type='success') {
    const d = document.createElement('div');
    d.className = `alert alert-${type}`;
    d.style.cssText = 'position:fixed;top:20px;right:20px;z-index:999;max-width:360px;animation:slideUp .2s';
    d.innerHTML = `<i class="fa-solid ${type==='success'?'fa-check-circle':'fa-exclamation-circle'}"></i>${msg}`;
    document.body.appendChild(d);
    setTimeout(() => d.remove(), 3500);
  }
};

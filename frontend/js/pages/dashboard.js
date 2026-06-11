Router.register('dashboard', async (app) => {
  app.innerHTML = C.layout('dashboard', C.loading(), 'AI-powered recruitment overview');
  const data = await api.get('/analytics/dashboard');
  if (!data) return;

  const statCards = [
    { label:'Total Resumes',  val:data.totalResumes,  icon:'fa-file-user',       bg:'var(--blue-l)',   ic:'var(--blue)' },
    { label:'Open Jobs',      val:data.openJobs,       icon:'fa-briefcase',       bg:'var(--green-l)',  ic:'var(--green)' },
    { label:'Selected',       val:data.selected,       icon:'fa-user-check',      bg:'var(--green-l)',  ic:'var(--green)' },
    { label:'Under Review',   val:data.underReview,    icon:'fa-magnifying-glass',bg:'var(--purple-l)', ic:'var(--purple)' },
    { label:'Waitlisted',     val:data.waitlisted,     icon:'fa-clock',           bg:'var(--amber-l)',  ic:'var(--amber)' },
    { label:'Rejected',       val:data.rejected,       icon:'fa-user-xmark',      bg:'var(--red-l)',    ic:'var(--red)' },
  ].map(s => `
    <div class="stat-card">
      <div class="stat-icon" style="background:${s.bg}">
        <i class="fa-solid ${s.icon}" style="color:${s.ic}"></i>
      </div>
      <div><div class="stat-num">${s.val}</div><div class="stat-label">${s.label}</div></div>
    </div>`).join('');

  const jobRows = (data.jobStats||[]).slice(0,6).map(j => `
    <tr>
      <td><strong>${j.jobTitle}</strong></td>
      <td><span class="badge ${j.status==='OPEN'?'badge-green':'badge-gray'}">${j.status}</span></td>
      <td>${j.applicants}</td>
      <td>${C.scoreBar(j.avgScore)}</td>
      <td><button class="btn btn-sm btn-secondary" onclick="Router.go('candidates')">
        <i class="fa-solid fa-users"></i> View
      </button></td>
    </tr>`).join('') || `<tr><td colspan="5">${C.empty('fa-briefcase','No jobs yet','Create a job role to start')}</td></tr>`;

  const topCandidates = (data.topCandidates||[]).map(t => `
    <div style="display:flex;align-items:center;justify-content:space-between;padding:10px 0;border-bottom:1px solid var(--gray-100)">
      <div>
        <div style="font-weight:600;font-size:14px">${t.candidateName}</div>
        <div style="font-size:12px;color:var(--gray-500)">${t.jobTitle}</div>
      </div>
      <div style="min-width:140px">${C.scoreBar(t.matchScore)}</div>
    </div>`).join('') || '<p style="color:var(--gray-400);font-size:13px">Upload resumes to see AI-ranked candidates</p>';

  const pipelineData = [
    { label:'Applied',      val:data.applied||0,      color:'var(--blue)' },
    { label:'Under Review', val:data.underReview||0,  color:'var(--purple)' },
    { label:'Selected',     val:data.selected||0,     color:'var(--green)' },
    { label:'Waitlisted',   val:data.waitlisted||0,   color:'var(--amber)' },
    { label:'Rejected',     val:data.rejected||0,     color:'var(--red)' },
  ];
  const total = pipelineData.reduce((a,b)=>a+b.val,0)||1;
  const pipeline = pipelineData.map(p=>`
    <div class="chart-bar-row">
      <div class="chart-bar-label">${p.label}</div>
      <div class="chart-bar-outer">
        <div class="chart-bar-inner" style="width:${Math.round(p.val/total*100)}%;background:${p.color};min-width:${p.val?'24px':'0'}">
          ${p.val>0?p.val:''}
        </div>
      </div>
      <div class="chart-bar-num">${p.val}</div>
    </div>`).join('');

  const user = Auth.user() || {};
  const isRecruiter = user.role === 'RECRUITER';
  const bannerTitle = isRecruiter ? 'Gemini AI Recruiter Portal' : 'Gemini AI is active';
  const bannerSub = isRecruiter ? 'Evaluating resumes and tracking applicants for your assigned job roles' : 'Resumes are parsed, scored, and ranked using real-time AI analysis';

  const content = `
    <!-- AI Banner -->
    <div style="background:linear-gradient(135deg,var(--navy),var(--blue));border-radius:var(--radius-lg);
                padding:20px 24px;margin-bottom:24px;display:flex;align-items:center;justify-content:space-between">
      <div>
        <div style="color:#fff;font-size:16px;font-weight:700;margin-bottom:4px">
          <i class="fa-solid fa-brain"></i> ${bannerTitle}
        </div>
        <div style="color:rgba(255,255,255,.7);font-size:13px">
          ${bannerSub}
        </div>
      </div>
      <button class="btn" style="background:rgba(255,255,255,.15);color:#fff;border:1px solid rgba(255,255,255,.3)"
        onclick="Router.go('upload')">
        <i class="fa-solid fa-cloud-arrow-up"></i> Upload Resume
      </button>
    </div>

    <div class="stats-grid">${statCards}</div>

    <div style="display:grid;grid-template-columns:1fr 1fr;gap:20px;margin-bottom:20px">
      <div class="card">
        <div class="card-header">
          <div class="card-title"><i class="fa-solid fa-chart-bar" style="color:var(--blue)"></i> Hiring Pipeline</div>
        </div>
        ${pipeline}
      </div>
      <div class="card">
        <div class="card-header">
          <div class="card-title"><i class="fa-solid fa-trophy" style="color:var(--amber)"></i> Top AI-Ranked Candidates</div>
          <button class="btn btn-sm btn-secondary" onclick="Router.go('candidates')">View All</button>
        </div>
        ${topCandidates}
      </div>
    </div>

    <div class="card">
      <div class="card-header">
        <div class="card-title"><i class="fa-solid fa-briefcase" style="color:var(--teal)"></i> Job Roles</div>
        <button class="btn btn-sm btn-primary" onclick="Router.go('jobs')">Manage Jobs</button>
      </div>
      <div class="table-wrap">
        <table>
          <thead><tr><th>Job Title</th><th>Status</th><th>Applicants</th><th>Avg AI Score</th><th>Action</th></tr></thead>
          <tbody>${jobRows}</tbody>
        </table>
      </div>
    </div>`;

  app.innerHTML = C.layout('dashboard', content, 'AI-powered recruitment overview');
});

Router.register('analytics', async (app) => {
  app.innerHTML = C.layout('analytics', C.loading(), 'Hiring insights and reports');
  const data = await api.get('/analytics/dashboard');
  if (!data) return;

  const total = (data.applied||0)+(data.underReview||0)+(data.selected||0)+(data.waitlisted||0)+(data.rejected||0) || 1;

  const pipelineItems = [
    { label:'Applied',      val:data.applied||0,      color:'var(--blue)' },
    { label:'Under Review', val:data.underReview||0,   color:'var(--purple)' },
    { label:'Selected',     val:data.selected||0,      color:'var(--green)' },
    { label:'Waitlisted',   val:data.waitlisted||0,    color:'var(--amber)' },
    { label:'Rejected',     val:data.rejected||0,      color:'var(--red)' },
  ];

  const pipeline = pipelineItems.map(p => `
    <div class="chart-bar-row">
      <div class="chart-bar-label">${p.label}</div>
      <div class="chart-bar-outer">
        <div class="chart-bar-inner" style="width:${Math.round(p.val/total*100)}%;background:${p.color};min-width:${p.val?'24px':'0'}">
          ${p.val > 0 ? p.val : ''}
        </div>
      </div>
      <div class="chart-bar-num">${p.val}</div>
    </div>`).join('');

  const maxApplicants = Math.max(...(data.jobStats||[]).map(j=>j.applicants), 1);
  const jobBars = (data.jobStats||[]).map(j => `
    <div class="chart-bar-row">
      <div class="chart-bar-label" title="${j.jobTitle}">${j.jobTitle}</div>
      <div class="chart-bar-outer">
        <div class="chart-bar-inner" style="width:${Math.round(j.applicants/maxApplicants*100)}%;background:var(--blue);min-width:${j.applicants?'24px':'0'}">
          ${j.applicants > 0 ? j.applicants : ''}
        </div>
      </div>
      <div class="chart-bar-num">${j.applicants}</div>
    </div>`).join('') || '<p style="color:var(--gray-400);font-size:13px;padding:8px 0">No jobs yet</p>';

  const jobTable = (data.jobStats||[]).map(j => `
    <tr>
      <td><strong>${j.jobTitle}</strong></td>
      <td><span class="badge ${j.status==='OPEN'?'badge-green':'badge-gray'}">${j.status}</span></td>
      <td>${j.applicants}</td>
      <td>${C.scoreBar(j.avgScore)}</td>
    </tr>`).join('') || `<tr><td colspan="4" style="text-align:center;color:var(--gray-400);padding:20px">No data</td></tr>`;

  const topRows = (data.topCandidates||[]).map((t,i) => `
    <tr>
      <td><span style="font-weight:700;color:var(--${i===0?'amber':i===1?'gray-500':i===2?'amber':''}-${i<3?'600':'400'})">#${i+1}</span></td>
      <td><strong>${t.candidateName}</strong></td>
      <td>${t.jobTitle}</td>
      <td>${C.scoreBar(t.matchScore)}</td>
    </tr>`).join('') || `<tr><td colspan="4" style="text-align:center;color:var(--gray-400);padding:20px">No scored candidates yet</td></tr>`;

  const content = `
    <div style="display:flex;justify-content:flex-end;margin-bottom:16px;gap:10px">
      <button class="btn btn-success" onclick="api.downloadCsv().then(()=>C.showToast('CSV downloaded!'))">
        <i class="fa-solid fa-file-csv"></i> Export CSV Report
      </button>
    </div>

    <div class="stats-grid" style="margin-bottom:20px">
      ${[
        {l:'Total Resumes', v:data.totalResumes, i:'fa-file-user', c:'var(--blue)'},
        {l:'Total Jobs',    v:data.totalJobs,    i:'fa-briefcase', c:'var(--teal)'},
        {l:'Open Jobs',     v:data.openJobs,     i:'fa-door-open', c:'var(--green)'},
        {l:'Selected',      v:data.selected,     i:'fa-user-check',c:'var(--green)'},
      ].map(s=>`
        <div class="stat-card">
          <div class="stat-icon" style="background:${s.c}22">
            <i class="fa-solid ${s.i}" style="color:${s.c}"></i>
          </div>
          <div><div class="stat-num">${s.v}</div><div class="stat-label">${s.l}</div></div>
        </div>`).join('')}
    </div>

    <div style="display:grid;grid-template-columns:1fr 1fr;gap:20px;margin-bottom:20px">
      <div class="card">
        <div class="card-header">
          <div class="card-title"><i class="fa-solid fa-filter" style="color:var(--blue)"></i> Hiring Pipeline</div>
        </div>
        ${pipeline}
        <div class="divider"></div>
        <div class="donut-list">
          ${pipelineItems.map(p=>`
            <div class="donut-item">
              <div class="donut-dot" style="background:${p.color}"></div>
              <span>${p.label}</span>
              <span style="margin-left:auto;font-weight:600">${Math.round(p.val/total*100)}%</span>
            </div>`).join('')}
        </div>
      </div>
      <div class="card">
        <div class="card-header">
          <div class="card-title"><i class="fa-solid fa-briefcase" style="color:var(--teal)"></i> Applicants per Job</div>
        </div>
        ${jobBars}
      </div>
    </div>

    <div style="display:grid;grid-template-columns:1fr 1fr;gap:20px">
      <div class="card">
        <div class="card-header">
          <div class="card-title"><i class="fa-solid fa-trophy" style="color:var(--amber)"></i> Top Scoring Candidates</div>
        </div>
        <div class="table-wrap">
          <table>
            <thead><tr><th>Rank</th><th>Candidate</th><th>Job</th><th>Score</th></tr></thead>
            <tbody>${topRows}</tbody>
          </table>
        </div>
      </div>
      <div class="card">
        <div class="card-header">
          <div class="card-title"><i class="fa-solid fa-chart-line" style="color:var(--purple)"></i> Job Role Performance</div>
        </div>
        <div class="table-wrap">
          <table>
            <thead><tr><th>Job</th><th>Status</th><th>Applicants</th><th>Avg Score</th></tr></thead>
            <tbody>${jobTable}</tbody>
          </table>
        </div>
      </div>
    </div>`;

  app.innerHTML = C.layout('analytics', content, 'Hiring insights and reports');
});

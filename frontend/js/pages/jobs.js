let jobsData = [];

Router.register('jobs', async (app) => {
  app.innerHTML = C.layout('jobs', C.loading(), 'Create and manage open positions');
  jobsData = await api.get('/jobs') || [];
  JobsPage.render(app);
});

const JobsPage = {
  render(app) {
    const rows = jobsData.map(j => `
      <tr>
        <td>
          <div style="font-weight:600">${j.title}</div>
          <div style="font-size:12px;color:var(--gray-500)">${(j.description||'').substring(0,70)}${j.description?.length>70?'...':''}</div>
        </td>
        <td><span class="badge ${j.status==='OPEN'?'badge-green':j.status==='CLOSED'?'badge-red':'badge-gray'}">${j.status}</span></td>
        <td>${(j.requiredSkills||'').split(',').slice(0,3).map(s=>`<span class="skill-tag">${s.trim()}</span>`).join('')}</td>
        <td>${j.experienceYears} yr${j.experienceYears!==1?'s':''}</td>
        <td style="font-size:12px;color:var(--gray-500)">${j.createdBy||''}</td>
        <td>
          <div style="display:flex;gap:6px">
            <button class="btn btn-sm btn-secondary" onclick="JobsPage.edit(${j.id})"><i class="fa-solid fa-pen"></i></button>
            <button class="btn btn-sm btn-secondary" onclick="Router.go('candidates',{jobId:${j.id}})"><i class="fa-solid fa-users"></i></button>
            <button class="btn btn-sm btn-danger" onclick="JobsPage.archive(${j.id})"><i class="fa-solid fa-archive"></i></button>
          </div>
        </td>
      </tr>`).join('') || `<tr><td colspan="6">${C.empty('fa-briefcase','No jobs yet','Click "New Job Role" to create one')}</td></tr>`;

    const content = `
      <div style="display:flex;justify-content:flex-end;gap:10px;margin-bottom:16px">
        <button class="btn btn-secondary" onclick="JobsPage.openAiParse()">
          <i class="fa-solid fa-brain"></i> AI Parse JD
        </button>
        <button class="btn btn-primary" onclick="JobsPage.openCreate()">
          <i class="fa-solid fa-plus"></i> New Job Role
        </button>
      </div>
      <div class="card">
        <div class="card-header">
          <div class="card-title">All Job Roles (${jobsData.length})</div>
        </div>
        <div class="table-wrap">
          <table>
            <thead><tr><th>Title</th><th>Status</th><th>Required Skills</th><th>Experience</th><th>Created By</th><th>Actions</th></tr></thead>
            <tbody>${rows}</tbody>
          </table>
        </div>
      </div>`;
    app.innerHTML = C.layout('jobs', content, 'Create and manage open positions');
  },

  openCreate() {
    document.body.insertAdjacentHTML('beforeend', C.modal('job-modal', 'Create Job Role', `
      <div class="form-group">
        <label class="form-label">Job Title *</label>
        <input id="jm-title" placeholder="e.g. Senior Java Developer"/>
      </div>
      <div class="form-group">
        <label class="form-label">Description</label>
        <textarea id="jm-desc" rows="3" placeholder="Describe the role..."></textarea>
      </div>
      <div class="form-group">
        <label class="form-label">Required Skills * <span style="color:var(--gray-400);font-size:11px">(comma-separated)</span></label>
        <input id="jm-skills" placeholder="e.g. Java, Spring Boot, MySQL, REST APIs"/>
      </div>
      <div class="form-group">
        <label class="form-label">Minimum Experience (years)</label>
        <input id="jm-exp" type="number" min="0" max="20" value="0"/>
      </div>
      <div id="jm-alert"></div>
    `, `
      <button class="btn btn-secondary" onclick="C.closeModal('job-modal')">Cancel</button>
      <button class="btn btn-primary" onclick="JobsPage.save()"><i class="fa-solid fa-save"></i> Save</button>
    `));
  },

  // AI-powered: paste a JD and Claude extracts skills automatically
  openAiParse() {
    document.body.insertAdjacentHTML('beforeend', C.modal('ai-parse-modal', '🤖 AI Parse Job Description', `
      <div style="background:var(--blue-l);border-radius:var(--radius);padding:10px;font-size:13px;color:var(--blue);margin-bottom:14px">
        Paste a full job description — Claude AI will extract the title, required skills, experience level, and more.
      </div>
      <div class="form-group">
        <label class="form-label">Paste Job Description *</label>
        <textarea id="ai-jd-text" rows="8" placeholder="Paste the full job description here..."></textarea>
      </div>
      <div id="ai-jd-alert"></div>
      <div id="ai-jd-result" style="display:none"></div>
    `, `
      <button class="btn btn-secondary" onclick="C.closeModal('ai-parse-modal')">Cancel</button>
      <button class="btn btn-primary" id="ai-parse-btn" onclick="JobsPage.runAiParse()">
        <i class="fa-solid fa-brain"></i> Parse with Claude AI
      </button>
    `));
  },

  async runAiParse() {
    const text = document.getElementById('ai-jd-text').value.trim();
    if (!text) { document.getElementById('ai-jd-alert').innerHTML = C.alert('Please paste a job description.','error'); return; }
    const btn = document.getElementById('ai-parse-btn');
    btn.innerHTML = '<div class="spinner"></div> Claude is parsing...';
    btn.disabled = true;

    const res = await api.post('/jobs/ai-parse', { description: text });
    btn.innerHTML = '<i class="fa-solid fa-brain"></i> Parse with Claude AI';
    btn.disabled = false;

    if (!res || !res.coreSkills) {
      document.getElementById('ai-jd-alert').innerHTML = C.alert('AI parsing failed. Try again.','error');
      return;
    }

    const allSkills = [...(res.coreSkills||[]), ...(res.niceToHaveSkills||[])];
    document.getElementById('ai-jd-result').innerHTML = `
      <div style="border:1px solid var(--gray-200);border-radius:var(--radius);padding:14px;margin-top:12px">
        <div class="section-label">AI Extracted</div>
        <div style="margin-bottom:8px"><strong>Level:</strong> ${res.experienceLevel||'Mid'} · <strong>Min Exp:</strong> ${res.minExperienceYears||0} yrs · <strong>Education:</strong> ${res.educationRequired||'Any'}</div>
        <div style="margin-bottom:8px"><strong>Core Skills:</strong> ${(res.coreSkills||[]).map(s=>`<span class="skill-tag">${s}</span>`).join('')}</div>
        ${res.niceToHaveSkills?.length?`<div style="margin-bottom:8px"><strong>Nice to Have:</strong> ${res.niceToHaveSkills.map(s=>`<span class="skill-tag" style="background:var(--gray-100)">${s}</span>`).join('')}</div>`:''}
        <button class="btn btn-primary btn-sm" style="margin-top:8px" onclick="JobsPage.useAiResult(${JSON.stringify(res).replace(/"/g,'&quot;')})">
          Use This & Create Job
        </button>
      </div>`;
    document.getElementById('ai-jd-result').style.display = 'block';
  },

  useAiResult(res) {
    C.closeModal('ai-parse-modal');
    const allSkills = [...(res.coreSkills||[]), ...(res.niceToHaveSkills||[])].join(', ');
    document.body.insertAdjacentHTML('beforeend', C.modal('job-modal', 'Create Job Role (AI Pre-filled)', `
      <div style="background:var(--green-l);border-radius:var(--radius);padding:8px 12px;font-size:12px;color:var(--green);margin-bottom:12px">
        ✓ Pre-filled by Claude AI — review and adjust before saving
      </div>
      <div class="form-group">
        <label class="form-label">Job Title *</label>
        <input id="jm-title" placeholder="Enter job title"/>
      </div>
      <div class="form-group">
        <label class="form-label">Description</label>
        <textarea id="jm-desc" rows="3">${res.responsibilities?.join('. ')||''}</textarea>
      </div>
      <div class="form-group">
        <label class="form-label">Required Skills *</label>
        <input id="jm-skills" value="${allSkills}"/>
      </div>
      <div class="form-group">
        <label class="form-label">Minimum Experience (years)</label>
        <input id="jm-exp" type="number" value="${res.minExperienceYears||0}"/>
      </div>
      <div id="jm-alert"></div>
    `, `
      <button class="btn btn-secondary" onclick="C.closeModal('job-modal')">Cancel</button>
      <button class="btn btn-primary" onclick="JobsPage.save()"><i class="fa-solid fa-save"></i> Save Job</button>
    `));
  },

  edit(id) {
    const j = jobsData.find(x => x.id === id);
    if (!j) return;
    document.body.insertAdjacentHTML('beforeend', C.modal('job-modal', 'Edit Job Role', `
      <input type="hidden" id="jm-id" value="${j.id}"/>
      <div class="form-group"><label class="form-label">Title *</label>
        <input id="jm-title" value="${j.title||''}"/></div>
      <div class="form-group"><label class="form-label">Description</label>
        <textarea id="jm-desc" rows="3">${j.description||''}</textarea></div>
      <div class="form-group"><label class="form-label">Required Skills *</label>
        <input id="jm-skills" value="${j.requiredSkills||''}"/></div>
      <div class="form-group"><label class="form-label">Experience (years)</label>
        <input id="jm-exp" type="number" value="${j.experienceYears||0}"/></div>
      <div class="form-group"><label class="form-label">Status</label>
        <select id="jm-status">
          <option ${j.status==='OPEN'?'selected':''}>OPEN</option>
          <option ${j.status==='CLOSED'?'selected':''}>CLOSED</option>
          <option ${j.status==='ARCHIVED'?'selected':''}>ARCHIVED</option>
        </select></div>
      <div id="jm-alert"></div>
    `, `
      <button class="btn btn-secondary" onclick="C.closeModal('job-modal')">Cancel</button>
      <button class="btn btn-primary" onclick="JobsPage.save()"><i class="fa-solid fa-save"></i> Update</button>
    `));
  },

  async save() {
    const id     = document.getElementById('jm-id')?.value;
    const title  = document.getElementById('jm-title').value.trim();
    const skills = document.getElementById('jm-skills').value.trim();
    if (!title || !skills) {
      document.getElementById('jm-alert').innerHTML = C.alert('Title and skills are required.','error');
      return;
    }
    const payload = {
      title, description: document.getElementById('jm-desc').value,
      requiredSkills: skills,
      experienceYears: parseInt(document.getElementById('jm-exp').value)||0,
    };
    const status = document.getElementById('jm-status');
    if (status) payload.status = status.value;
    const res = id ? await api.put('/jobs/'+id, payload) : await api.post('/jobs', payload);
    if (res?.id) {
      C.closeModal('job-modal');
      C.showToast(id ? 'Job updated!' : 'Job created!');
      jobsData = await api.get('/jobs') || [];
      JobsPage.render(document.getElementById('app'));
    } else {
      document.getElementById('jm-alert').innerHTML = C.alert('Failed to save.','error');
    }
  },

  async archive(id) {
    if (!confirm('Archive this job?')) return;
    await api.delete('/jobs/'+id);
    C.showToast('Job archived.');
    jobsData = await api.get('/jobs') || [];
    JobsPage.render(document.getElementById('app'));
  }
};

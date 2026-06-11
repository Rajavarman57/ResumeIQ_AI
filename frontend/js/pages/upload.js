Router.register('upload', async (app) => {
  const jobs = await api.get('/jobs/open') || [];
  const jobOptions = jobs.map(j => `<option value="${j.id}">${j.title}</option>`).join('');

  const content = `
    <div style="max-width:760px;margin:0 auto">
      <div class="card" style="margin-bottom:20px">
        <div class="card-header">
          <div class="card-title"><i class="fa-solid fa-brain" style="color:var(--blue)"></i> AI Resume Upload & Analysis</div>
          <span class="badge badge-purple"><i class="fa-solid fa-sparkles"></i> Powered by Gemini AI</span>
        </div>
        <div style="background:var(--blue-l);border-radius:var(--radius);padding:12px 16px;margin-bottom:16px;font-size:13px;color:var(--blue)">
          <i class="fa-solid fa-circle-info"></i> Gemini AI will extract your candidate's profile, score them against job requirements, identify skill gaps, and generate personalised improvement suggestions.
        </div>
        <div id="upload-alert"></div>
        <div class="form-group">
          <label class="form-label">Target Job Role <span style="color:var(--gray-400)">(optional)</span></label>
          <select id="upload-job">
            <option value="">— Score against all open roles —</option>
            ${jobOptions}
          </select>
          <div class="form-hint">Leave blank to score against every open position</div>
        </div>
        <div class="upload-zone" id="upload-zone" onclick="document.getElementById('file-input').click()">
          <input type="file" id="file-input" accept=".pdf,.docx" style="display:none" onchange="UploadPage.handleFile(this.files[0])"/>
          <i class="fa-solid fa-file-arrow-up"></i>
          <h3>Drop resume here or click to browse</h3>
          <p>Supports PDF and DOCX · Max 50MB · Gemini AI will parse and score</p>
        </div>
        <div id="file-preview" style="display:none;margin-top:16px">
          <div style="display:flex;align-items:center;gap:14px;padding:14px;background:var(--gray-50);border-radius:var(--radius);border:1px solid var(--gray-200)">
            <i class="fa-solid fa-file-pdf" style="font-size:28px;color:var(--red)" id="file-icon"></i>
            <div style="flex:1">
              <div id="file-name" style="font-weight:600;font-size:14px"></div>
              <div id="file-size" style="font-size:12px;color:var(--gray-500)"></div>
            </div>
            <button class="btn btn-sm btn-danger" onclick="UploadPage.clearFile()"><i class="fa-solid fa-trash"></i></button>
          </div>
        </div>
        <div style="margin-top:18px;display:flex;justify-content:flex-end">
          <button class="btn btn-primary" id="upload-btn" onclick="UploadPage.submit()" disabled>
            <i class="fa-solid fa-brain"></i> Analyse with Gemini AI
          </button>
        </div>
      </div>
      <div id="upload-result" style="display:none"></div>
    </div>`;

  app.innerHTML = C.layout('upload', content, 'AI-powered resume parsing and candidate scoring');
  UploadPage.setupDrag();
});

const UploadPage = {
  file: null,

  setupDrag() {
    const zone = document.getElementById('upload-zone');
    if (!zone) return;
    zone.addEventListener('dragover', e => { e.preventDefault(); zone.classList.add('drag'); });
    zone.addEventListener('dragleave', () => zone.classList.remove('drag'));
    zone.addEventListener('drop', e => {
      e.preventDefault(); zone.classList.remove('drag');
      const f = e.dataTransfer.files[0];
      if (f) this.handleFile(f);
    });
  },

  handleFile(f) {
    if (!f) return;
    const ext = f.name.split('.').pop().toUpperCase();
    if (!['PDF','DOCX'].includes(ext)) {
      document.getElementById('upload-alert').innerHTML = C.alert('Only PDF and DOCX files are supported.', 'error');
      return;
    }
    this.file = f;
    document.getElementById('file-name').textContent = f.name;
    document.getElementById('file-size').textContent = (f.size / 1024).toFixed(1) + ' KB · ' + ext;
    document.getElementById('file-icon').className = ext === 'PDF'
      ? 'fa-solid fa-file-pdf' : 'fa-solid fa-file-word';
    document.getElementById('file-icon').style.color = ext === 'PDF' ? 'var(--red)' : 'var(--blue)';
    document.getElementById('file-preview').style.display = 'block';
    document.getElementById('upload-btn').disabled = false;
    document.getElementById('upload-alert').innerHTML = '';
  },

  clearFile() {
    this.file = null;
    document.getElementById('file-preview').style.display = 'none';
    document.getElementById('upload-btn').disabled = true;
    document.getElementById('file-input').value = '';
  },

  async submit() {
    if (!this.file) return;
    const btn = document.getElementById('upload-btn');
    btn.innerHTML = '<div class="spinner"></div> Gemini AI is analysing...';
    btn.disabled = true;
    document.getElementById('upload-result').style.display = 'none';

    const fd = new FormData();
    fd.append('file', this.file);
    const jobId = document.getElementById('upload-job').value;
    if (jobId) fd.append('jobId', jobId);

    const res = await api.upload('/resumes/upload', fd);
    btn.innerHTML = '<i class="fa-solid fa-brain"></i> Analyse with Gemini AI';
    btn.disabled = false;

    if (!res || res.error) {
      document.getElementById('upload-alert').innerHTML = C.alert(res?.error || 'Upload failed. Check that the backend is running and GEMINI_API_KEY is set.', 'error');
      return;
    }

    this.showResult(res);
    this.clearFile();
    C.showToast('Resume analysed by Gemini AI!');
  },

  showResult(res) {
    const profile = res.aiProfile || {};
    const strengths = (profile.strengths || []).map(s => `<li>${s}</li>`).join('');
    const topSkills = (profile.topSkills || []).map(s => `<span class="skill-tag" style="background:var(--green-l);color:var(--green)">${s}</span>`).join('');
    const allSkills = (profile.skills || res.extractedSkills?.split(',') || [])
      .map(s => `<span class="skill-tag">${typeof s === 'string' ? s.trim() : s}</span>`).join('');
    const certs = (profile.certifications || []).map(c => `<span class="skill-tag" style="background:var(--amber-l);color:var(--amber)">${c}</span>`).join('');

    let educationHtml = '';
    try {
      const edu = typeof profile.education === 'string' ? JSON.parse(profile.education) : (profile.education || []);
      if (Array.isArray(edu) && edu.length > 0) {
        educationHtml = edu.map(e => `
          <div style="padding:8px 0;border-bottom:1px solid var(--gray-100)">
            <strong>${e.degree || ''}</strong> — ${e.institution || ''} ${e.year ? '('+e.year+')' : ''}
          </div>`).join('');
      }
    } catch(e) { educationHtml = `<span style="font-size:13px;color:var(--gray-500)">${res.education || 'Not detected'}</span>`; }

    const resultHtml = `
      <div class="card" style="margin-bottom:16px">
        <div class="card-header">
          <div class="card-title"><i class="fa-solid fa-circle-check" style="color:var(--green)"></i> Gemini AI Analysis Complete</div>
          <span class="badge badge-green">Scored against ${res.jobsScored || 0} role(s)</span>
        </div>

        <!-- Profile summary -->
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px;margin-bottom:20px">
          <div>
            <div class="section-label">Candidate</div>
            <div style="font-size:18px;font-weight:700;color:var(--gray-900)">${profile.fullName || res.candidateName || 'Unknown'}</div>
            <div style="font-size:13px;color:var(--gray-500);margin-top:2px">${profile.email || res.candidateEmail || ''}</div>
            <div style="font-size:13px;color:var(--gray-500)">${profile.phone || res.candidatePhone || ''}</div>
            <div style="margin-top:10px;font-size:13px">
              <span style="background:var(--blue-l);color:var(--blue);padding:3px 10px;border-radius:20px;font-weight:600">
                ${profile.totalExperienceYears || res.experienceYears || 0} yrs experience
              </span>
            </div>
          </div>
          <div>
            <div class="section-label">AI Summary</div>
            <div style="font-size:13px;color:var(--gray-700);line-height:1.6;font-style:italic">
              "${profile.summary || 'Summary not available'}"
            </div>
          </div>
        </div>

        <div class="divider"></div>

        <!-- Top Skills (AI picked) -->
        ${topSkills ? `<div style="margin-bottom:14px">
          <div class="section-label">Top Skills (AI Selected)</div>
          <div>${topSkills}</div>
        </div>` : ''}

        <!-- All Skills -->
        <div style="margin-bottom:14px">
          <div class="section-label">All Extracted Skills (${(profile.skills || []).length})</div>
          <div>${allSkills || '<span style="color:var(--gray-400);font-size:12px">None detected</span>'}</div>
        </div>

        <!-- Strengths -->
        ${strengths ? `<div style="margin-bottom:14px">
          <div class="section-label">AI-Identified Strengths</div>
          <ul style="margin-left:16px;font-size:13px;color:var(--gray-700)">${strengths}</ul>
        </div>` : ''}

        <!-- Education -->
        <div style="margin-bottom:14px">
          <div class="section-label">Education</div>
          ${educationHtml || '<span style="font-size:13px;color:var(--gray-400)">Not detected</span>'}
        </div>

        <!-- Certifications -->
        ${certs ? `<div style="margin-bottom:14px">
          <div class="section-label">Certifications</div>
          <div>${certs}</div>
        </div>` : ''}

        <div class="divider"></div>
        <div style="display:flex;gap:10px">
          <button class="btn btn-primary" onclick="Router.go('candidates')">
            <i class="fa-solid fa-users"></i> View AI Rankings
          </button>
          <button class="btn btn-secondary" onclick="document.getElementById('upload-result').style.display='none'">
            <i class="fa-solid fa-plus"></i> Upload Another
          </button>
        </div>
      </div>`;

    document.getElementById('upload-result').innerHTML = resultHtml;
    document.getElementById('upload-result').style.display = 'block';
  }
};

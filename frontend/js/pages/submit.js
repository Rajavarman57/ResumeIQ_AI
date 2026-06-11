Router.register('submit', async (app, params) => {
  // Public styles for the portal
  const extraStyles = `
    <style>
      .portal-container {
        min-height: 100vh;
        display: flex;
        align-items: center;
        justify-content: center;
        background: radial-gradient(circle at top right, var(--navy), #0b0f19);
        padding: 40px 20px;
        font-family: 'Inter', sans-serif;
      }
      .portal-card {
        background: rgba(255, 255, 255, 0.04);
        backdrop-filter: blur(16px);
        -webkit-backdrop-filter: blur(16px);
        border: 1px solid rgba(255, 255, 255, 0.08);
        border-radius: var(--radius-lg);
        width: 100%;
        max-width: 540px;
        padding: 40px;
        box-shadow: 0 20px 40px rgba(0,0,0,0.4);
        color: #fff;
        animation: fadeIn 0.4s ease-out;
      }
      .portal-header {
        text-align: center;
        margin-bottom: 30px;
      }
      .portal-logo h1 {
        font-size: 32px;
        font-weight: 800;
        margin-bottom: 6px;
        background: linear-gradient(135deg, #fff 30%, var(--blue));
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
      }
      .portal-header p {
        color: var(--gray-400);
        font-size: 14px;
      }
      .portal-form .form-group {
        margin-bottom: 20px;
      }
      .portal-form label {
        display: block;
        font-size: 13px;
        font-weight: 600;
        margin-bottom: 8px;
        color: var(--gray-300);
      }
      .portal-form select, .portal-form input {
        width: 100%;
        background: rgba(255,255,255,0.05);
        border: 1px solid rgba(255,255,255,0.1);
        border-radius: var(--radius);
        padding: 12px 14px;
        color: #fff;
        font-size: 14px;
        transition: all 0.2s;
        box-sizing: border-box;
      }
      .portal-form select:focus, .portal-form input:focus {
        outline: none;
        border-color: var(--blue);
        background: rgba(255,255,255,0.08);
        box-shadow: 0 0 0 3px rgba(37,99,235,0.2);
      }
      .portal-form select option {
        background: #111827;
        color: #fff;
      }
      .drag-area {
        border: 2px dashed rgba(255,255,255,0.15);
        border-radius: var(--radius);
        padding: 30px 20px;
        text-align: center;
        cursor: pointer;
        transition: all 0.2s;
        background: rgba(255,255,255,0.02);
      }
      .drag-area:hover, .drag-area.active {
        border-color: var(--blue);
        background: rgba(37,99,235,0.05);
      }
      .drag-area i {
        font-size: 36px;
        color: var(--blue);
        margin-bottom: 12px;
      }
      .drag-area p {
        margin: 0;
        font-size: 13px;
        color: var(--gray-400);
      }
      .file-name {
        margin-top: 10px;
        font-size: 13px;
        font-weight: 600;
        color: var(--green);
        display: none;
      }
      .success-box {
        text-align: center;
        padding: 20px 0;
        animation: scaleUp 0.3s ease-out;
      }
      .success-box i {
        font-size: 64px;
        color: var(--green);
        margin-bottom: 20px;
      }
      .success-box h2 {
        font-size: 24px;
        font-weight: 700;
        margin-bottom: 10px;
      }
      .success-box p {
        color: var(--gray-300);
        font-size: 14px;
        line-height: 1.6;
        margin-bottom: 24px;
      }
      @keyframes scaleUp {
        from { transform: scale(0.95); opacity: 0; }
        to { transform: scale(1); opacity: 1; }
      }
    </style>
  `;

  // Fetch open jobs
  app.innerHTML = `<div class="portal-container">${extraStyles}<div class="portal-card"><div style="text-align:center;padding:40px 0;"><div class="spinner"></div></div></div></div>`;
  
  const jobs = await api.get('/public/jobs') || [];

  const jobOptions = jobs.map(j => 
    `<option value="${j.id}" ${j.id == params?.jobId ? 'selected' : ''}>${j.title}</option>`
  ).join('');

  const renderForm = () => {
    return `
      <div class="portal-header">
        <div class="portal-logo">
          <h1>Resume<span>IQ</span></h1>
        </div>
        <p>AI-Powered Candidate Portal</p>
      </div>

      <div id="submit-alert"></div>

      <div class="portal-form">
        <div class="form-group">
          <label>Select Job Role *</label>
          <select id="pub-job-id" ${params?.jobId ? 'disabled' : ''}>
            <option value="">-- Choose a Position --</option>
            ${jobOptions}
          </select>
        </div>

        <div class="form-group">
          <label>Upload Resume * (PDF or DOCX)</label>
          <div class="drag-area" id="drag-area" onclick="document.getElementById('pub-file').click()">
            <i class="fa-solid fa-cloud-arrow-up"></i>
            <p>Click to select or drag & drop your resume file here</p>
            <div id="file-display" class="file-name"></div>
            <input type="file" id="pub-file" accept=".pdf,.docx" style="display:none" onchange="SubmitPage.handleFile(this)"/>
          </div>
        </div>

        <button class="btn btn-primary" id="pub-submit-btn" style="width:100%;justify-content:center;padding:12px;margin-top:10px" onclick="SubmitPage.submit()">
          <i class="fa-solid fa-paper-plane"></i> Submit Application
        </button>
      </div>
    `;
  };

  app.innerHTML = `
    <div class="portal-container">
      ${extraStyles}
      <div class="portal-card" id="portal-content">
        ${renderForm()}
      </div>
    </div>
  `;

  // Drag and drop events setup
  const area = document.getElementById('drag-area');
  if (area) {
    ['dragenter', 'dragover'].forEach(e => {
      area.addEventListener(e, (evt) => { evt.preventDefault(); area.classList.add('active'); }, false);
    });
    ['dragleave', 'drop'].forEach(e => {
      area.addEventListener(e, (evt) => { evt.preventDefault(); area.classList.remove('active'); }, false);
    });
    area.addEventListener('drop', (evt) => {
      const dt = evt.dataTransfer;
      const files = dt.files;
      if (files.length) {
        document.getElementById('pub-file').files = files;
        SubmitPage.handleFile(document.getElementById('pub-file'));
      }
    }, false);
  }
});

const SubmitPage = {
  selectedFile: null,

  handleFile(input) {
    const file = input.files[0];
    const display = document.getElementById('file-display');
    if (file) {
      this.selectedFile = file;
      display.innerText = `✓ ${file.name} (${Math.round(file.size/1024)} KB)`;
      display.style.display = 'block';
    } else {
      this.selectedFile = null;
      display.style.display = 'none';
    }
  },

  async submit() {
    const jobId = document.getElementById('pub-job-id')?.value || new URLSearchParams(window.location.hash.split('?')[1]).get('jobId');
    const alertEl = document.getElementById('submit-alert');
    const btn = document.getElementById('pub-submit-btn');

    if (!jobId) {
      alertEl.innerHTML = C.alert('Please select a job role.', 'error');
      return;
    }
    if (!this.selectedFile) {
      alertEl.innerHTML = C.alert('Please select a resume file to upload.', 'error');
      return;
    }

    btn.innerHTML = '<div class="spinner"></div> Submitting...';
    btn.disabled = true;

    const fd = new FormData();
    fd.append('file', this.selectedFile);
    fd.append('jobId', jobId);

    try {
      // Direct fetch for public upload endpoint (no auth header)
      const API_BASE = 'http://localhost:8080/api';
      const response = await fetch(API_BASE + '/public/resumes/upload', {
        method: 'POST',
        body: fd
      });

      const res = await response.json();

      if (response.ok) {
        const card = document.getElementById('portal-content');
        card.innerHTML = `
          <div class="success-box">
            <i class="fa-solid fa-circle-check"></i>
            <h2>Application Submitted!</h2>
            <p>
              Thank you, <strong>${res.candidateName || 'Candidate'}</strong>. 
              Your application for <strong>${res.jobTitle || 'the position'}</strong> has been successfully received. 
              Our Gemini AI pipeline has processed and scored your profile. Our recruitment team will review the results shortly.
            </p>
            <button class="btn btn-primary" style="margin: 0 auto;" onclick="window.location.reload()">
              <i class="fa-solid fa-rotate-left"></i> Submit Another Application
            </button>
          </div>
        `;
      } else {
        alertEl.innerHTML = C.alert(res.error || 'Failed to submit resume. Please check file type (PDF/DOCX).', 'error');
        btn.innerHTML = '<i class="fa-solid fa-paper-plane"></i> Submit Application';
        btn.disabled = false;
      }
    } catch (e) {
      alertEl.innerHTML = C.alert('Failed to connect to backend server.', 'error');
      btn.innerHTML = '<i class="fa-solid fa-paper-plane"></i> Submit Application';
      btn.disabled = false;
    }
  }
};

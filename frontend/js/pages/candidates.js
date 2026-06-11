let candJobs = [], candScores = [], candActiveJob = null, selectedForCompare = [];

Router.register('candidates', async (app, params) => {
  selectedForCompare = [];
  app.innerHTML = C.layout('candidates', C.loading(), 'AI-ranked candidates');
  candJobs = await api.get('/jobs/open') || [];
  if (!candJobs.length) {
    app.innerHTML = C.layout('candidates',
      C.empty('fa-users','No open jobs','Create a job role first'), '');
    return;
  }
  candActiveJob = params?.jobId || candJobs[0]?.id;
  await CandidatesPage.load(app);
});

const CandidatesPage = {
  async load(app) {
    const tabs = candJobs.map(j =>
      `<button class="tab ${j.id == candActiveJob ? 'active':''}" onclick="CandidatesPage.switchJob(${j.id},this)">${j.title}</button>`
    ).join('');

    candScores = candActiveJob ? (await api.get(`/candidates/ranked/${candActiveJob}`) || []) : [];

    const filterBar = `
      <div class="filter-bar">
        <input id="cand-search" placeholder="Search candidate name or email..." oninput="CandidatesPage.filter()"/>
        <input id="cand-skill"  placeholder="Filter by skill..." oninput="CandidatesPage.applySkillFilter()"/>
        <select id="cand-status" onchange="CandidatesPage.filter()">
          <option value="">All Statuses</option>
          <option>APPLIED</option><option>UNDER_REVIEW</option>
          <option>SELECTED</option><option>WAITLISTED</option><option>REJECTED</option>
        </select>
        <select id="cand-verdict" onchange="CandidatesPage.filter()">
          <option value="">All Verdicts</option>
          <option value="STRONG_MATCH">Strong Match</option>
          <option value="GOOD_MATCH">Good Match</option>
          <option value="PARTIAL_MATCH">Partial Match</option>
          <option value="WEAK_MATCH">Weak Match</option>
        </select>
      </div>`;

    const content = `
      <div class="tabs">${tabs}</div>
      ${filterBar}
      <div id="compare-bar" class="compare-bar" style="display:none;background:var(--purple-l);border:1px solid var(--purple);border-radius:var(--radius);padding:12px 16px;margin-bottom:20px;justify-content:space-between;align-items:center;animation:fadeIn 0.2s ease-out;">
        <span style="font-weight:600;color:var(--purple);font-size:13px"><i class="fa-solid fa-code-compare"></i> <span id="compare-count">0</span> candidate(s) selected for comparison</span>
        <button class="btn btn-sm" id="compare-btn" style="background:var(--purple);color:#fff" onclick="CandidatesPage.compare()">
          <i class="fa-solid fa-code-compare"></i> Compare Candidates
        </button>
      </div>
      <div id="candidates-grid" class="candidates-grid">${this.renderCards(candScores)}</div>`;

    app.innerHTML = C.layout('candidates', content,
      `${candScores.length} candidate(s) · AI-scored by Claude`);
  },

  verdictColor(verdict) {
    const map = { STRONG_MATCH:'var(--green)', GOOD_MATCH:'var(--blue)',
                  PARTIAL_MATCH:'var(--amber)', WEAK_MATCH:'var(--red)' };
    return map[verdict] || 'var(--gray-500)';
  },

  verdictLabel(verdict) {
    const map = { STRONG_MATCH:'⭐ Strong Match', GOOD_MATCH:'✓ Good Match',
                  PARTIAL_MATCH:'~ Partial Match', WEAK_MATCH:'✗ Weak Match' };
    return map[verdict] || verdict || '—';
  },

  recBadge(rec) {
    if (rec === 'RECOMMEND') return `<span class="badge badge-green">Recommend</span>`;
    if (rec === 'CONSIDER')  return `<span class="badge badge-amber">Consider</span>`;
    if (rec === 'SKIP')      return `<span class="badge badge-red">Skip</span>`;
    return '';
  },

  renderCards(scores) {
    if (!scores.length) return C.empty('fa-user-slash','No candidates yet','Upload resumes to see AI rankings');
    return scores.map((s, i) => {
      const verdict = s.verdict || 'PARTIAL_MATCH';
      const vColor  = this.verdictColor(verdict);
      const matched = Array.isArray(s.matchedSkills) ? s.matchedSkills : [];
      const missing = (s.skillGap || '').split(',').filter(Boolean);

      return `
      <div class="candidate-card" onclick="CandidatesPage.openDetail(${s.resumeId},${s.jobId})">
        <div style="display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:10px">
          <div>
            <div style="display:flex;align-items:center;gap:8px">
              <input type="checkbox" class="compare-checkbox" data-resume-id="${s.resumeId}"
                     ${selectedForCompare.includes(s.resumeId) ? 'checked' : ''}
                     onclick="event.stopPropagation(); CandidatesPage.toggleCompare(${s.resumeId}, this)"
                     style="width: 16px; height: 16px; cursor: pointer; accent-color: var(--purple); margin: 0;"/>
              <div style="width:28px;height:28px;border-radius:50%;background:var(--navy);color:#fff;
                          display:flex;align-items:center;justify-content:center;font-size:11px;font-weight:700">
                #${i+1}
              </div>
              <div>
                <div class="candidate-name">${s.candidateName || 'Unknown'}</div>
                <div class="candidate-email">${s.candidateEmail || ''}</div>
              </div>
            </div>
          </div>
          <div style="text-align:right">
            ${C.statusBadge(s.status || 'APPLIED')}
            <div style="font-size:11px;font-weight:700;color:${vColor};margin-top:3px">${this.verdictLabel(verdict)}</div>
          </div>
        </div>

        <div class="candidate-meta">
          <span><i class="fa-solid fa-briefcase"></i> ${s.experienceYears || 0} yrs</span>
          <span><i class="fa-solid fa-graduation-cap"></i> ${this.parseEdu(s.education)}</span>
          ${this.recBadge(s.recommendation)}
        </div>

        <div style="margin-bottom:8px">
          <div style="display:flex;justify-content:space-between;font-size:11px;color:var(--gray-500);margin-bottom:3px">
            <span>AI Match Score</span><span style="color:${s.matchScore>=70?'var(--green)':s.matchScore>=45?'var(--amber)':'var(--red)'}"><strong>${Math.round(s.matchScore)}%</strong></span>
          </div>
          ${C.scoreBar(s.matchScore)}
        </div>
        <div style="margin-bottom:10px">
          <div style="display:flex;justify-content:space-between;font-size:11px;color:var(--gray-500);margin-bottom:3px">
            <span>ATS Score</span><span style="color:var(--purple)"><strong>${Math.round(s.atsScore)}%</strong></span>
          </div>
          ${C.scoreBar(s.atsScore, 'var(--purple)')}
        </div>

        ${matched.length ? `<div style="margin-bottom:8px;font-size:11px">
          <span style="color:var(--green);font-weight:600">✓ Has: </span>
          ${matched.slice(0,4).map(sk=>`<span class="skill-tag" style="background:var(--green-l);color:var(--green)">${sk}</span>`).join('')}
        </div>` : ''}

        ${missing.length ? `<div style="margin-bottom:10px;font-size:11px">
          <span style="color:var(--red);font-weight:600">✗ Missing: </span>
          ${missing.slice(0,3).map(sk=>`<span class="skill-tag gap-tag">${sk.trim()}</span>`).join('')}
          ${missing.length>3?`<span style="font-size:11px;color:var(--red)">+${missing.length-3} more</span>`:''}
        </div>` : ''}

        <div style="display:flex;gap:6px;flex-wrap:wrap" onclick="event.stopPropagation()">
          ${this.statusSelect(s.resumeId, s.jobId, s.status)}
          <button class="btn btn-sm btn-secondary" onclick="event.stopPropagation();CandidatesPage.openDetail(${s.resumeId},${s.jobId})">
            <i class="fa-solid fa-brain"></i> AI Details
          </button>
          <button class="btn btn-sm" onclick="event.stopPropagation();ChatPage.open(${s.resumeId},${s.jobId},'${(s.candidateName||'').replace(/'/g,'')}','${(s.jobTitle||'').replace(/'/g,'')}')"
            style="background:var(--purple);color:#fff">
            <i class="fa-solid fa-comments"></i> Chat
          </button>
        </div>
      </div>`}).join('');
  },

  parseEdu(edu) {
    try {
      const arr = typeof edu === 'string' ? JSON.parse(edu) : edu;
      if (Array.isArray(arr) && arr.length) return arr[0].degree || 'N/A';
    } catch(e) {}
    return edu || 'N/A';
  },

  statusSelect(resumeId, jobId, current) {
    const opts = ['APPLIED','UNDER_REVIEW','SELECTED','WAITLISTED','REJECTED'];
    const colors = { APPLIED:'var(--blue)', UNDER_REVIEW:'var(--purple)',
                     SELECTED:'var(--green)', WAITLISTED:'var(--amber)', REJECTED:'var(--red)' };
    return `<select class="status-select" style="border-color:${colors[current]||'var(--gray-300)'};color:${colors[current]||'var(--gray-700)'}"
      onchange="CandidatesPage.updateStatus(${resumeId},${jobId},this.value)">
      ${opts.map(o=>`<option value="${o}" ${o===current?'selected':''}>${o.replace('_',' ')}</option>`).join('')}
    </select>`;
  },

  async updateStatus(resumeId, jobId, status) {
    await api.put(`/candidates/status/${resumeId}/${jobId}`, { status, notes: '' });
    C.showToast('Status updated to ' + status.replace('_',' '));
    candScores = await api.get(`/candidates/ranked/${candActiveJob}`) || [];
    document.getElementById('candidates-grid').innerHTML = this.renderCards(candScores);
  },

  async switchJob(jobId, btn) {
    candActiveJob = jobId;
    document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
    btn.classList.add('active');
    document.getElementById('candidates-grid').innerHTML = C.loading();
    candScores = await api.get(`/candidates/ranked/${jobId}`) || [];
    document.getElementById('candidates-grid').innerHTML = this.renderCards(candScores);
  },

  filter() {
    const search  = document.getElementById('cand-search')?.value.toLowerCase() || '';
    const status  = document.getElementById('cand-status')?.value || '';
    const verdict = document.getElementById('cand-verdict')?.value || '';
    const filtered = candScores.filter(s => {
      const ms = !search || (s.candidateName||'').toLowerCase().includes(search) || (s.candidateEmail||'').toLowerCase().includes(search);
      const ss = !status  || s.status === status;
      const vs = !verdict || (s.verdict || '') === verdict;
      return ms && ss && vs;
    });
    document.getElementById('candidates-grid').innerHTML = this.renderCards(filtered);
  },

  async applySkillFilter() {
    const skill = document.getElementById('cand-skill')?.value.trim() || '';
    const url   = skill
      ? `/candidates/ranked/${candActiveJob}?skills=${encodeURIComponent(skill)}`
      : `/candidates/ranked/${candActiveJob}`;
    const filtered = await api.get(url) || [];
    document.getElementById('candidates-grid').innerHTML = this.renderCards(filtered);
  },

  async openDetail(resumeId, jobId) {
    document.getElementById('detail-panel')?.remove();
    const score = await api.get(`/candidates/score/${resumeId}/${jobId}`);
    const alts  = await api.get(`/candidates/alternatives/${resumeId}/${jobId}`);
    if (!score) return;

    const verdict    = score.verdict || 'PARTIAL_MATCH';
    const vColor     = this.verdictColor(verdict);
    const matched    = Array.isArray(score.matchedSkills) ? score.matchedSkills : [];
    const bonus      = Array.isArray(score.bonusSkills) ? score.bonusSkills : [];
    const missing    = (score.skillGap || '').split(',').filter(Boolean);
    const keywords   = Array.isArray(score.keywordsToAdd) ? score.keywordsToAdd : [];
    const aiAlts     = (alts?.aiAlternatives || []);

    // Parse suggestions sections
    const rawSug = score.suggestions || '';
    const sections = {
      assessment: this.extractSec(rawSug, 'ASSESSMENT:'),
      experience: this.extractSec(rawSug, 'EXPERIENCE:'),
      improve:    this.extractSec(rawSug, 'IMPROVE:'),
      interview:  this.extractSec(rawSug, 'INTERVIEW:'),
      summary:    this.extractSec(rawSug, 'SUMMARY:'),
      advice:     this.extractSec(rawSug, 'ADVICE:'),
    };

    const improveList = sections.improve
      ? sections.improve.split('•').filter(Boolean).map(s=>`<li>${s.trim()}</li>`).join('')
      : '';
    const interviewList = sections.interview
      ? sections.interview.split('•').filter(Boolean).map(s=>`<li>${s.trim()}</li>`).join('')
      : '';

    const altRows = aiAlts.length
      ? aiAlts.map(a => `
          <div style="padding:8px 0;border-bottom:1px solid var(--gray-100)">
            <div style="display:flex;justify-content:space-between;align-items:center">
              <div>
                <div style="font-size:13px;font-weight:600">${a.jobTitle}</div>
                <div style="font-size:11px;color:var(--gray-500)">${a.fitReason || ''}</div>
              </div>
              <span style="font-size:13px;font-weight:700;color:${a.fitScore>=70?'var(--green)':a.fitScore>=45?'var(--amber)':'var(--gray-500)'}">${Math.round(a.fitScore)}%</span>
            </div>
          </div>`).join('')
      : '<div style="font-size:13px;color:var(--gray-400)">No alternative roles found</div>';

    const panel = document.createElement('div');
    panel.id = 'detail-panel';
    panel.className = 'detail-panel';
    panel.innerHTML = `
      <button class="detail-close" onclick="document.getElementById('detail-panel').remove()">&times;</button>
      <div style="margin-bottom:4px;display:flex;align-items:center;gap:8px">
        <h3 style="margin:0">${score.candidateName}</h3>
        ${this.recBadge(score.recommendation)}
      </div>
      <div style="font-size:12px;color:var(--gray-500);margin-bottom:4px">${score.candidateEmail||''} ${score.candidatePhone||''}</div>
      <div style="font-size:12px;font-weight:700;color:${vColor};margin-bottom:16px">${this.verdictLabel(verdict)}</div>

      ${sections.assessment ? `<div style="background:var(--blue-l);border-radius:var(--radius);padding:10px;font-size:12px;color:var(--blue);margin-bottom:14px">
        <i class="fa-solid fa-brain"></i> <strong>AI Assessment:</strong> ${sections.assessment}
      </div>` : ''}

      <div style="display:grid;grid-template-columns:1fr 1fr 1fr;gap:10px;margin-bottom:16px">
        <div class="card" style="text-align:center;padding:12px">
          <div style="font-size:24px;font-weight:700;color:${score.matchScore>=70?'var(--green)':score.matchScore>=45?'var(--amber)':'var(--red)'}">${Math.round(score.matchScore)}%</div>
          <div style="font-size:10px;color:var(--gray-500)">Match Score</div>
        </div>
        <div class="card" style="text-align:center;padding:12px">
          <div style="font-size:24px;font-weight:700;color:var(--purple)">${Math.round(score.atsScore)}%</div>
          <div style="font-size:10px;color:var(--gray-500)">ATS Score</div>
        </div>
        <div class="card" style="text-align:center;padding:12px">
          <div style="font-size:24px;font-weight:700;color:var(--teal)">${score.experienceYears||0}yr</div>
          <div style="font-size:10px;color:var(--gray-500)">Experience</div>
        </div>
      </div>

      ${matched.length ? `<div class="section-label">✓ Matched Skills</div>
      <div style="margin-bottom:12px">${matched.map(s=>`<span class="skill-tag" style="background:var(--green-l);color:var(--green)">${s}</span>`).join('')}</div>` : ''}

      ${missing.length ? `<div class="section-label">✗ Missing Skills (Skill Gap)</div>
      <div style="margin-bottom:12px">${missing.map(s=>`<span class="skill-tag gap-tag">${s.trim()}</span>`).join('')}</div>` : ''}

      ${bonus.length ? `<div class="section-label">⭐ Bonus Skills</div>
      <div style="margin-bottom:12px">${bonus.map(s=>`<span class="skill-tag" style="background:var(--purple-l);color:var(--purple)">${s}</span>`).join('')}</div>` : ''}

      ${keywords.length ? `<div class="section-label">🔑 Add These Keywords</div>
      <div style="margin-bottom:12px">${keywords.map(k=>`<span class="skill-tag" style="background:var(--amber-l);color:var(--amber)">${k}</span>`).join('')}</div>` : ''}

      ${sections.experience ? `<div style="background:var(--gray-50);border-radius:var(--radius);padding:10px;font-size:12px;margin-bottom:12px">
        <strong>Experience:</strong> ${sections.experience}
      </div>` : ''}

      ${improveList ? `<div class="suggestion-box" style="margin-bottom:12px">
        <strong><i class="fa-solid fa-lightbulb"></i> AI Improvement Suggestions</strong>
        <ul style="margin-top:6px;margin-left:16px">${improveList}</ul>
      </div>` : ''}

      ${sections.summary ? `<div style="background:var(--blue-l);border-radius:var(--radius);padding:10px;font-size:12px;color:var(--blue);margin-bottom:12px">
        <strong>Suggested Summary Rewrite:</strong><br/><em>${sections.summary}</em>
      </div>` : ''}

      ${interviewList ? `<div style="background:var(--green-l);border-radius:var(--radius);padding:10px;font-size:12px;color:var(--green);margin-bottom:12px">
        <strong>Interview Tips:</strong>
        <ul style="margin-top:6px;margin-left:16px">${interviewList}</ul>
      </div>` : ''}

      ${sections.advice ? `<div style="font-size:12px;color:var(--gray-600);font-style:italic;margin-bottom:14px">
        💡 ${sections.advice}
      </div>` : ''}

      <div class="divider"></div>
      <div class="section-label">🔄 AI Alternative Role Matches</div>
      <div style="margin-bottom:14px">${altRows}</div>

      <div style="margin-bottom:14px">
        <button class="btn btn-primary" style="width:100%;justify-content:center;background:var(--purple)"
          onclick="document.getElementById('detail-panel').remove();ChatPage.open(${resumeId},${jobId},'${score.candidateName?.replace(/'/g,'')||''}','${score.jobTitle?.replace(/'/g,'')||''}')">
          <i class="fa-solid fa-comments"></i> Chat with AI about this Candidate
        </button>
      </div>
      <div class="section-label">Update Status</div>
      <div style="display:flex;gap:6px;flex-wrap:wrap">
        ${['APPLIED','UNDER_REVIEW','SELECTED','WAITLISTED','REJECTED'].map(st =>
          `<button class="btn btn-sm ${score.status===st?'btn-primary':'btn-secondary'}"
            onclick="CandidatesPage.updateStatus(${resumeId},${jobId},'${st}');document.getElementById('detail-panel').remove()">
            ${st.replace('_',' ')}
          </button>`).join('')}
      </div>`;
    document.body.appendChild(panel);
  },

  extractSec(text, prefix) {
    if (!text || !text.includes(prefix)) return '';
    const start = text.indexOf(prefix) + prefix.length;
    const end   = text.indexOf(' | ', start);
    return (end > start ? text.substring(start, end) : text.substring(start)).trim();
  },

  toggleCompare(resumeId, checkbox) {
    if (checkbox.checked) {
      if (selectedForCompare.length >= 2) {
        checkbox.checked = false;
        C.showToast('You can only compare up to 2 candidates.', 'error');
        return;
      }
      selectedForCompare.push(resumeId);
    } else {
      selectedForCompare = selectedForCompare.filter(id => id !== resumeId);
    }
    CandidatesPage.updateCompareBar();
  },

  updateCompareBar() {
    const bar = document.getElementById('compare-bar');
    const count = document.getElementById('compare-count');
    const btn = document.getElementById('compare-btn');
    if (bar && count && btn) {
      const len = selectedForCompare.length;
      count.innerText = len;
      if (len > 0) {
        bar.style.display = 'flex';
        btn.disabled = len !== 2;
        btn.style.opacity = len === 2 ? '1' : '0.5';
      } else {
        bar.style.display = 'none';
      }
    }
  },

  async compare() {
    if (selectedForCompare.length !== 2) return;
    const [id1, id2] = selectedForCompare;
    
    // Open comparison modal with loading state
    const modalId = 'compare-modal';
    const loadingBody = `<div style="text-align:center;padding:60px 0;">${C.loading()}<p style="margin-top:16px;color:var(--gray-500)">Analyzing candidate profiles side-by-side using Claude AI...</p></div>`;
    
    const overlay = document.createElement('div');
    overlay.id = `${modalId}-overlay`;
    overlay.className = 'modal-overlay';
    overlay.style.display = 'flex';
    overlay.innerHTML = `
      <div class="modal" style="max-width:960px;width:90%;max-height:90vh;display:flex;flex-direction:column;animation:scaleUp 0.2s ease-out;">
        <div class="modal-header">
          <div class="modal-title"><i class="fa-solid fa-code-compare"></i> Side-by-Side Candidate Comparison</div>
          <button class="modal-close" onclick="CandidatesPage.closeCompare()">&times;</button>
        </div>
        <div style="flex:1;overflow-y:auto;padding:20px;" id="compare-modal-body">${loadingBody}</div>
      </div>
    `;
    document.body.appendChild(overlay);
    
    try {
      const comparison = await api.get(`/candidates/compare/${id1}/${id2}?jobId=${candActiveJob}`);
      if (!comparison) {
        document.getElementById('compare-modal-body').innerHTML = C.alert('Failed to generate comparison. Please try again.', 'error');
        return;
      }
      
      CandidatesPage.renderComparisonDetails(comparison);
    } catch (e) {
      document.getElementById('compare-modal-body').innerHTML = C.alert('Failed to connect to backend server.', 'error');
    }
  },
  
  closeCompare() {
    document.getElementById('compare-modal-overlay')?.remove();
    selectedForCompare = [];
    document.querySelectorAll('.compare-checkbox').forEach(c => c.checked = false);
    CandidatesPage.updateCompareBar();
  },

  renderComparisonDetails(data) {
    const score1 = candScores.find(s => s.resumeId === selectedForCompare[0]) || {};
    const score2 = candScores.find(s => s.resumeId === selectedForCompare[1]) || {};
    
    const pros1 = (data.candidate1Pros || []).map(p => `<li><i class="fa-solid fa-check" style="color:var(--green)"></i> ${p}</li>`).join('');
    const pros2 = (data.candidate2Pros || []).map(p => `<li><i class="fa-solid fa-check" style="color:var(--green)"></i> ${p}</li>`).join('');
    const cons1 = (data.candidate1Cons || []).map(c => `<li><i class="fa-solid fa-xmark" style="color:var(--red)"></i> ${c}</li>`).join('');
    const cons2 = (data.candidate2Cons || []).map(c => `<li><i class="fa-solid fa-xmark" style="color:var(--red)"></i> ${c}</li>`).join('');
    
    const gridRows = (data.comparisonGrid || []).map(row => {
      const win1 = row.winner === 'CANDIDATE_1' ? 'background:rgba(34,197,94,0.08);font-weight:600;' : '';
      const win2 = row.winner === 'CANDIDATE_2' ? 'background:rgba(34,197,94,0.08);font-weight:600;' : '';
      return `
        <tr>
          <td style="font-weight:600;color:var(--gray-700);width:20%;">${row.criteria}</td>
          <td style="${win1}">${row.candidate1}</td>
          <td style="${win2}">${row.candidate2}</td>
        </tr>
      `;
    }).join('');
    
    const body = `
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:20px;margin-bottom:20px;">
        <div class="card" style="padding:16px;">
          <h3 style="margin-top:0;display:flex;align-items:center;justify-content:space-between;color:var(--navy)">
            <span>${score1.candidateName}</span>
            <span style="font-size:20px;font-weight:700;color:var(--blue)">${Math.round(score1.matchScore)}% Match</span>
          </h3>
          <p style="font-size:13px;color:var(--gray-600);line-height:1.5;margin-bottom:12px;">${data.candidate1Summary}</p>
          <div style="font-size:12px;color:var(--gray-500);">
            <strong>ATS Keyword Score:</strong> ${Math.round(score1.atsScore)}%
          </div>
        </div>
        <div class="card" style="padding:16px;">
          <h3 style="margin-top:0;display:flex;align-items:center;justify-content:space-between;color:var(--navy)">
            <span>${score2.candidateName}</span>
            <span style="font-size:20px;font-weight:700;color:var(--blue)">${Math.round(score2.matchScore)}% Match</span>
          </h3>
          <p style="font-size:13px;color:var(--gray-600);line-height:1.5;margin-bottom:12px;">${data.candidate2Summary}</p>
          <div style="font-size:12px;color:var(--gray-500);">
            <strong>ATS Keyword Score:</strong> ${Math.round(score2.atsScore)}%
          </div>
        </div>
      </div>
      
      <div class="card" style="margin-bottom:20px;padding:0;">
        <div class="card-header" style="border-bottom:1px solid var(--gray-100);padding:14px 18px;">
          <div class="card-title" style="font-size:14px;"><i class="fa-solid fa-table-list" style="color:var(--blue)"></i> AI Side-by-Side Comparison</div>
        </div>
        <div class="table-wrap">
          <table style="font-size:13px;">
            <thead>
              <tr>
                <th style="width:20%;">Criteria</th>
                <th>${score1.candidateName}</th>
                <th>${score2.candidateName}</th>
              </tr>
            </thead>
            <tbody>
              ${gridRows}
            </tbody>
          </table>
        </div>
      </div>
      
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:20px;margin-bottom:20px;">
        <div class="card" style="padding:16px;">
          <h4 style="margin-top:0;color:var(--navy);font-size:14px;border-bottom:1px solid var(--gray-100);padding-bottom:8px;margin-bottom:10px;"><i class="fa-solid fa-thumbs-up" style="color:var(--green)"></i> Pros</h4>
          <ul style="list-style:none;padding-left:0;font-size:13px;line-height:1.6;margin-bottom:8px;">
            ${pros1}
          </ul>
          <h4 style="margin-top:10px;color:var(--navy);font-size:14px;border-bottom:1px solid var(--gray-100);padding-bottom:8px;margin-bottom:10px;"><i class="fa-solid fa-thumbs-down" style="color:var(--red)"></i> Cons</h4>
          <ul style="list-style:none;padding-left:0;font-size:13px;line-height:1.6;margin-bottom:0;">
            ${cons1}
          </ul>
        </div>
        <div class="card" style="padding:16px;">
          <h4 style="margin-top:0;color:var(--navy);font-size:14px;border-bottom:1px solid var(--gray-100);padding-bottom:8px;margin-bottom:10px;"><i class="fa-solid fa-thumbs-up" style="color:var(--green)"></i> Pros</h4>
          <ul style="list-style:none;padding-left:0;font-size:13px;line-height:1.6;margin-bottom:8px;">
            ${pros2}
          </ul>
          <h4 style="margin-top:10px;color:var(--navy);font-size:14px;border-bottom:1px solid var(--gray-100);padding-bottom:8px;margin-bottom:10px;"><i class="fa-solid fa-thumbs-down" style="color:var(--red)"></i> Cons</h4>
          <ul style="list-style:none;padding-left:0;font-size:13px;line-height:1.6;margin-bottom:0;">
            ${cons2}
          </ul>
        </div>
      </div>
      
      <div style="background:var(--blue-l);border:1px solid var(--blue);border-radius:var(--radius);padding:16px;font-size:13px;color:var(--blue);line-height:1.6;">
        <strong><i class="fa-solid fa-brain"></i> AI Hiring Recommendation:</strong>
        <p style="margin-top:6px;margin-bottom:0;font-style:italic;">${data.recommendation}</p>
      </div>
    `;
    
    document.getElementById('compare-modal-body').innerHTML = body;
  }
};

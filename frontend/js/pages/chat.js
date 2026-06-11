// Chat with Resume — AI conversation about a specific candidate
const ChatPage = {

  resumeId: null,
  jobId: null,
  candidateName: '',
  jobTitle: '',
  isTyping: false,

  async open(resumeId, jobId, candidateName, jobTitle) {
    this.resumeId = resumeId;
    this.jobId    = jobId;
    this.candidateName = candidateName;
    this.jobTitle      = jobTitle;

    document.getElementById('detail-panel')?.remove();

    // Load suggestions and history in parallel
    const [history, suggestions] = await Promise.all([
      api.get(`/chat/${resumeId}/${jobId}`),
      api.get(`/chat/suggestions/${resumeId}/${jobId}`)
    ]);

    this.renderPanel(history || [], (suggestions?.suggestions) || []);
  },

  renderPanel(history, suggestions) {
    const panel = document.createElement('div');
    panel.id = 'chat-panel';
    panel.style.cssText = `
      position:fixed;top:0;right:0;width:500px;height:100vh;
      background:#fff;box-shadow:-4px 0 24px rgba(0,0,0,.15);
      z-index:200;display:flex;flex-direction:column;animation:slideLeft .2s;
    `;

    const suggestionBtns = suggestions.map(s =>
      `<button class="suggestion-btn" onclick="ChatPage.sendSuggestion('${s.replace(/'/g,"\\'")}')">
        ${s}
      </button>`
    ).join('');

    panel.innerHTML = `
      <div style="background:var(--navy);padding:16px 20px;display:flex;align-items:center;justify-content:space-between;flex-shrink:0">
        <div>
          <div style="color:#fff;font-weight:700;font-size:15px">
            <i class="fa-solid fa-brain" style="color:#93C5FD;margin-right:8px"></i>Chat with Resume AI
          </div>
          <div style="color:rgba(255,255,255,.6);font-size:12px;margin-top:2px">
            ${this.candidateName} — ${this.jobTitle}
          </div>
        </div>
        <div style="display:flex;gap:8px;align-items:center">
          <button onclick="ChatPage.clearChat()" title="Clear chat"
            style="background:rgba(255,255,255,.1);border:1px solid rgba(255,255,255,.2);
                   color:rgba(255,255,255,.7);padding:5px 10px;border-radius:6px;cursor:pointer;font-size:12px">
            <i class="fa-solid fa-trash"></i>
          </button>
          <button onclick="document.getElementById('chat-panel').remove()"
            style="background:none;border:none;color:rgba(255,255,255,.7);font-size:20px;cursor:pointer;padding:0 4px">
            &times;
          </button>
        </div>
      </div>

      <div id="chat-messages" style="flex:1;overflow-y:auto;padding:16px;display:flex;flex-direction:column;gap:12px;background:#F8FAFC">
        ${history.length === 0 ? this.welcomeMessage() : history.map(m => this.renderMessage(m.sender, m.content, m.sentAt)).join('')}
      </div>

      <div style="padding:12px 16px;border-top:1px solid var(--gray-200);background:#fff;flex-shrink:0">
        <div style="margin-bottom:10px;overflow-x:auto;white-space:nowrap;padding-bottom:4px">
          ${suggestionBtns}
        </div>
        <div style="display:flex;gap:8px;align-items:flex-end">
          <textarea id="chat-input" placeholder="Ask anything about this candidate..."
            rows="2" onkeydown="ChatPage.handleKey(event)"
            style="flex:1;resize:none;border:1px solid var(--gray-300);border-radius:8px;
                   padding:10px 12px;font-size:13px;font-family:inherit;line-height:1.5;
                   outline:none;transition:border-color .2s"
            onfocus="this.style.borderColor='var(--blue)'"
            onblur="this.style.borderColor='var(--gray-300)'"></textarea>
          <button id="chat-send-btn" onclick="ChatPage.send()"
            style="background:var(--blue);color:#fff;border:none;border-radius:8px;
                   padding:10px 14px;cursor:pointer;transition:background .15s;flex-shrink:0">
            <i class="fa-solid fa-paper-plane"></i>
          </button>
        </div>
        <div style="font-size:11px;color:var(--gray-400);margin-top:6px;text-align:center">
          <i class="fa-solid fa-brain" style="color:var(--purple)"></i> Powered by Gemini AI · Press Enter to send
        </div>
      </div>`;

    document.body.appendChild(panel);

    // Add suggestion button styles
    if (!document.getElementById('chat-styles')) {
      const style = document.createElement('style');
      style.id = 'chat-styles';
      style.textContent = `
        .suggestion-btn {
          display:inline-block;margin-right:6px;padding:4px 10px;
          background:#EFF6FF;color:#2563EB;border:1px solid #BFDBFE;
          border-radius:16px;font-size:11px;cursor:pointer;white-space:nowrap;
          transition:all .15s;font-family:inherit;
        }
        .suggestion-btn:hover { background:#DBEAFE; }
        .chat-bubble-user { background:#2563EB;color:#fff;border-radius:16px 16px 4px 16px;padding:10px 14px;max-width:85%;align-self:flex-end;font-size:13px;line-height:1.5; }
        .chat-bubble-ai { background:#fff;border:1px solid #E2E8F0;border-radius:16px 16px 16px 4px;padding:12px 14px;max-width:92%;align-self:flex-start;font-size:13px;line-height:1.6;box-shadow:0 1px 3px rgba(0,0,0,.06); }
        .chat-time { font-size:10px;color:var(--gray-400);margin-top:3px; }
        .typing-indicator { display:flex;gap:4px;padding:8px 14px;background:#fff;border:1px solid #E2E8F0;border-radius:16px 16px 16px 4px;align-self:flex-start;width:fit-content; }
        .typing-dot { width:8px;height:8px;border-radius:50%;background:#94A3B8;animation:typingBounce 1.2s infinite; }
        .typing-dot:nth-child(2){animation-delay:.2s}
        .typing-dot:nth-child(3){animation-delay:.4s}
        @keyframes typingBounce{0%,60%,100%{transform:translateY(0)}30%{transform:translateY(-8px)}}
      `;
      document.head.appendChild(style);
    }

    this.scrollToBottom();
    document.getElementById('chat-input')?.focus();
  },

  welcomeMessage() {
    return `
      <div style="text-align:center;padding:20px 10px">
        <div style="font-size:36px;margin-bottom:12px">🤖</div>
        <div style="font-weight:600;color:var(--gray-900);margin-bottom:6px">Hi! I'm your ResumeIQ AI</div>
        <div style="font-size:13px;color:var(--gray-500);line-height:1.6">
          I have read <strong>${this.candidateName}'s</strong> resume and their match analysis for <strong>${this.jobTitle}</strong>.
          Ask me anything about this candidate!
        </div>
      </div>`;
  },

  renderMessage(sender, content, time) {
    const isUser = sender === 'USER';
    const timeStr = time ? new Date(time).toLocaleTimeString([], {hour:'2-digit',minute:'2-digit'}) : '';
    if (isUser) {
      return `<div style="display:flex;flex-direction:column;align-items:flex-end">
        <div class="chat-bubble-user">${this.escHtml(content)}</div>
        <div class="chat-time">${timeStr}</div>
      </div>`;
    }
    return `<div style="display:flex;flex-direction:column;align-items:flex-start">
      <div style="display:flex;align-items:center;gap:6px;margin-bottom:4px">
        <div style="width:22px;height:22px;border-radius:50%;background:var(--navy);display:flex;align-items:center;justify-content:center">
          <i class="fa-solid fa-brain" style="font-size:10px;color:#93C5FD"></i>
        </div>
        <span style="font-size:11px;font-weight:600;color:var(--gray-500)">Gemini AI</span>
      </div>
      <div class="chat-bubble-ai">${this.formatAI(content)}</div>
      <div class="chat-time">${timeStr}</div>
    </div>`;
  },

  formatAI(text) {
    return this.escHtml(text)
      .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
      .replace(/\*(.*?)\*/g, '<em>$1</em>')
      .replace(/\n\n/g, '</p><p style="margin:8px 0 0">')
      .replace(/\n/g, '<br/>')
      .replace(/^/, '<p style="margin:0">')
      .replace(/$/, '</p>');
  },

  escHtml(text) {
    const d = document.createElement('div');
    d.textContent = text;
    return d.innerHTML;
  },

  showTyping() {
    const msgs = document.getElementById('chat-messages');
    if (!msgs) return;
    const div = document.createElement('div');
    div.id = 'typing-indicator';
    div.innerHTML = `<div class="typing-indicator"><div class="typing-dot"></div><div class="typing-dot"></div><div class="typing-dot"></div></div>`;
    msgs.appendChild(div);
    this.scrollToBottom();
  },

  hideTyping() {
    document.getElementById('typing-indicator')?.remove();
  },

  scrollToBottom() {
    const msgs = document.getElementById('chat-messages');
    if (msgs) setTimeout(() => { msgs.scrollTop = msgs.scrollHeight; }, 50);
  },

  handleKey(e) {
    if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); this.send(); }
  },

  sendSuggestion(text) {
    const input = document.getElementById('chat-input');
    if (input) { input.value = text; this.send(); }
  },

  async send() {
    if (this.isTyping) return;
    const input = document.getElementById('chat-input');
    const message = input?.value?.trim();
    if (!message) return;

    input.value = '';
    this.isTyping = true;

    const btn = document.getElementById('chat-send-btn');
    if (btn) btn.disabled = true;

    // Show user message immediately
    const msgs = document.getElementById('chat-messages');
    if (msgs) {
      msgs.insertAdjacentHTML('beforeend', this.renderMessage('USER', message, new Date().toISOString()));
      this.scrollToBottom();
    }

    // Show typing indicator
    this.showTyping();

    try {
      const res = await api.post(`/chat/${this.resumeId}/${this.jobId}`, { message });
      this.hideTyping();
      if (res?.response && msgs) {
        msgs.insertAdjacentHTML('beforeend', this.renderMessage('AI', res.response, new Date().toISOString()));
        this.scrollToBottom();
      }
    } catch (e) {
      this.hideTyping();
      if (msgs) msgs.insertAdjacentHTML('beforeend',
        this.renderMessage('AI', 'Sorry, something went wrong. Please try again.', new Date().toISOString()));
    } finally {
      this.isTyping = false;
      if (btn) btn.disabled = false;
      input?.focus();
    }
  },

  async clearChat() {
    if (!confirm('Clear this conversation?')) return;
    await api.delete(`/chat/${this.resumeId}/${this.jobId}`);
    const msgs = document.getElementById('chat-messages');
    if (msgs) msgs.innerHTML = this.welcomeMessage();
    C.showToast('Chat cleared');
  }
};

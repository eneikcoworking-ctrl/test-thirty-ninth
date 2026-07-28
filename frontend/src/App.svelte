<script lang="ts">
  import { onMount } from 'svelte';

  // AI Configuration State (Tab 1)
  interface StopTrigger {
    keyword: string;
    enabled: boolean;
  }

  interface AiConfiguration {
    id?: number;
    systemPrompt: string;
    stopTriggers: StopTrigger[];
    modelVersion: string;
    updatedAt?: string;
  }

  let systemPrompt = '';
  let stopTriggers: StopTrigger[] = [];
  let modelVersion = 'GPT-4-Turbo';
  let subModel = 'Production (Stable)';
  let triggerInput = '';
  let isSaving = false;
  let saveStatus = 'All changes saved';
  let saveStatusType: 'success' | 'saving' | 'error' = 'success';
  let defaultPrompt = 'You are a highly analytical AI assistant specialized in technical documentation and software architecture. Your tone is professional, concise, and focused on providing empirical data and verifiable code snippets. Avoid flowery language or conversational fillers. When asked about complex systems, provide high-level abstractions followed by detailed component breakdowns.';

  let textareaElement: HTMLTextAreaElement | null = null;
  let overlayElement: HTMLDivElement | null = null;

  // Account Management Dashboard State (Tab 2)
  interface ProxyInfo {
    id: number;
    host: string;
    port: number;
    type: string;
    username?: string;
  }

  interface TelegramAccount {
    id: number;
    phoneNumber: string;
    status: 'Active' | 'Temporary Spam-Block' | 'Permanent Ban' | 'Re-authorization Required' | string;
    userId?: number;
    proxy?: ProxyInfo | null;
  }

  // Active Tab
  let activeTab: 'accounts' | 'ai-tuning' = 'accounts';

  // Accounts List State
  let accounts: TelegramAccount[] = [];
  let isLoadingAccounts = false;
  let accountsError = '';

  // OTP Onboarding State
  let otpPhoneNumber = '';
  let otpReferenceId = '';
  let otpCode = '';
  let otpStep: 'request' | 'verify' = 'request';
  let otpStatus = '';
  let otpStatusType: 'success' | 'loading' | 'error' = 'success';

  // File Onboarding State
  let uploadDragOver = false;
  let uploadFileElement: HTMLInputElement | null = null;
  let uploadProgress = 0;
  let uploadStatus = '';
  let uploadStatusType: 'idle' | 'uploading' | 'success' | 'error' = 'idle';

  // Proxy Binding State
  let selectedProxyAccount: TelegramAccount | null = null;
  let proxyHost = '';
  let proxyPort = 1080;
  let proxyType = 'SOCKS5';
  let proxyUsername = '';
  let proxyPassword = '';
  let proxyStatus = '';
  let proxyStatusType: 'idle' | 'saving' | 'success' | 'error' = 'idle';

  // On mount, load default configurations and lists
  onMount(async () => {
    await loadAiConfig();
    await loadAccounts();
  });

  // Load AI configuration
  async function loadAiConfig() {
    try {
      const res = await fetch('/api/ai-configuration');
      if (res.ok) {
        const data: AiConfiguration = await res.json();
        systemPrompt = data.systemPrompt;
        stopTriggers = data.stopTriggers || [];
        modelVersion = data.modelVersion || 'GPT-4-Turbo';
      }
    } catch (e) {
      console.error('Failed to load AI configuration', e);
      saveStatus = 'Connection error. Offline mode.';
      saveStatusType = 'error';
    }
  }

  // Load onboarded Accounts
  async function loadAccounts() {
    isLoadingAccounts = true;
    accountsError = '';
    try {
      const res = await fetch('/api/v1/accounts');
      if (res.ok) {
        accounts = await res.json();
      } else {
        accountsError = 'Failed to load accounts from server.';
      }
    } catch (err) {
      console.error(err);
      accountsError = 'Network error fetching account list.';
    } finally {
      isLoadingAccounts = false;
    }
  }

  // Synchronize scroll of textarea and highlighter overlay
  function handleScroll() {
    if (textareaElement && overlayElement) {
      overlayElement.scrollTop = textareaElement.scrollTop;
      overlayElement.scrollLeft = textareaElement.scrollLeft;
    }
  }

  // Escape HTML helper
  function escapeHtml(text: string): string {
    return text
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#039;");
  }

  // Highlight placeholders, e.g. {userName} or {Hi|Hello}
  function highlightPrompt(text: string): string {
    const escaped = escapeHtml(text);
    return escaped.replace(/\{([^}]+)\}/g, (match, content) => {
      const isSpintax = content.includes('|');
      const colorClass = isSpintax
        ? 'bg-amber-500/20 text-amber-300 border border-amber-500/50'
        : 'bg-blue-500/20 text-blue-300 border border-blue-500/50';
      return `<span class="px-1 rounded font-mono ${colorClass}">${match}</span>`;
    });
  }

  // Instantly save stop triggers to the backend
  async function saveStopTriggers(updatedTriggers: StopTrigger[]) {
    saveStatus = 'Saving stop triggers...';
    saveStatusType = 'saving';
    try {
      const res = await fetch('/api/ai-configuration/stop-triggers', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(updatedTriggers)
      });
      if (res.ok) {
        saveStatus = 'Triggers updated successfully!';
        saveStatusType = 'success';
        setTimeout(() => {
          if (saveStatusType === 'success') {
            saveStatus = 'All changes saved';
          }
        }, 3000);
      } else {
        saveStatus = 'Failed to update triggers';
        saveStatusType = 'error';
      }
    } catch (err) {
      console.error(err);
      saveStatus = 'Server error. Could not update triggers.';
      saveStatusType = 'error';
    }
  }

  // Save full AI configuration
  async function saveFullConfig() {
    saveStatus = 'Saving all changes...';
    saveStatusType = 'saving';
    isSaving = true;
    try {
      const res = await fetch('/api/ai-configuration', {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          systemPrompt,
          stopTriggers,
          modelVersion
        })
      });
      if (res.ok) {
        saveStatus = 'All settings saved successfully!';
        saveStatusType = 'success';
        setTimeout(() => {
          if (saveStatusType === 'success') {
            saveStatus = 'All changes saved';
          }
        }, 3000);
      } else {
        saveStatus = 'Failed to save settings';
        saveStatusType = 'error';
      }
    } catch (e) {
      console.error(e);
      saveStatus = 'Connection error. Could not save settings.';
      saveStatusType = 'error';
    } finally {
      isSaving = false;
    }
  }

  // Reset AI Settings to defaults
  async function resetToDefault() {
    systemPrompt = defaultPrompt;
    stopTriggers = [
      { keyword: 'Exit', enabled: true },
      { keyword: 'Cancel', enabled: true },
      { keyword: 'Error', enabled: true }
    ];
    modelVersion = 'GPT-4-Turbo';
    await saveFullConfig();
  }

  // Add stop trigger
  function addTrigger() {
    const trimmed = triggerInput.trim();
    if (trimmed && !stopTriggers.some(t => t.keyword === trimmed)) {
      stopTriggers = [...stopTriggers, { keyword: trimmed, enabled: true }];
      triggerInput = '';
      saveStopTriggers(stopTriggers);
    }
  }

  // Remove stop trigger
  function removeTrigger(keyword: string) {
    stopTriggers = stopTriggers.filter(t => t.keyword !== keyword);
    saveStopTriggers(stopTriggers);
  }

  // Handle trigger keydown
  function handleTriggerKeydown(event: KeyboardEvent) {
    if (event.key === 'Enter') {
      event.preventDefault();
      addTrigger();
    }
  }

  // OTP Request
  async function requestOtp() {
    if (!otpPhoneNumber.trim() || !otpPhoneNumber.match(/^\+[1-9]\d{1,14}$/)) {
      otpStatus = 'Please enter a valid phone number starting with + (e.g., +1234567890).';
      otpStatusType = 'error';
      return;
    }

    otpStatus = 'Sending OTP request...';
    otpStatusType = 'loading';

    try {
      const res = await fetch('/api/v1/accounts/onboard/otp-request', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ phoneNumber: otpPhoneNumber.trim() })
      });
      const data = await res.json();
      if (res.ok) {
        otpReferenceId = data.referenceId;
        otpStep = 'verify';
        otpStatus = 'OTP requested! Please enter the code received on Telegram.';
        otpStatusType = 'success';
      } else {
        otpStatus = data.message || 'Failed to request OTP from server.';
        otpStatusType = 'error';
      }
    } catch (err) {
      console.error(err);
      otpStatus = 'Network error during OTP request.';
      otpStatusType = 'error';
    }
  }

  // OTP Verification
  async function verifyOtp() {
    if (!otpCode.trim()) {
      otpStatus = 'Please enter the verification code.';
      otpStatusType = 'error';
      return;
    }

    otpStatus = 'Verifying verification code...';
    otpStatusType = 'loading';

    try {
      const res = await fetch('/api/v1/accounts/onboard/otp-verify', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ referenceId: otpReferenceId, code: otpCode.trim() })
      });
      const data = await res.json();
      if (res.ok) {
        otpStatus = `Session successfully authenticated! Phone: ${data.phoneNumber}`;
        otpStatusType = 'success';
        otpPhoneNumber = '';
        otpCode = '';
        otpReferenceId = '';
        otpStep = 'request';
        await loadAccounts();
        setTimeout(() => { otpStatus = ''; }, 5000);
      } else {
        otpStatus = data.message || 'Verification failed. Please check the code.';
        otpStatusType = 'error';
      }
    } catch (err) {
      console.error(err);
      otpStatus = 'Network error during OTP verification.';
      otpStatusType = 'error';
    }
  }

  // File Upload Onboarding (Drag & Drop)
  function handleDragOver(e: DragEvent) {
    e.preventDefault();
    uploadDragOver = true;
  }

  // Keyboard navigation for Dropzone (Accessibility WCAG)
  function handleDropzoneKeyDown(e: KeyboardEvent) {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      uploadFileElement?.click();
    }
  }

  function handleDragLeave() {
    uploadDragOver = false;
  }

  function handleDrop(e: DragEvent) {
    e.preventDefault();
    uploadDragOver = false;
    const files = e.dataTransfer?.files;
    if (files && files.length > 0) {
      triggerFileUpload(files[0]);
    }
  }

  function handleFileSelect(e: Event) {
    const target = e.target as HTMLInputElement;
    const files = target.files;
    if (files && files.length > 0) {
      triggerFileUpload(files[0]);
    }
  }

  // Simulated animated upload with bio-semantic success alert
  async function triggerFileUpload(file: File) {
    if (!file.name.endsWith('.session') && !file.name.endsWith('.zip')) {
      uploadStatus = 'Unsupported file format. Please upload a .session file.';
      uploadStatusType = 'error';
      return;
    }

    uploadStatus = `Uploading file: ${file.name}...`;
    uploadStatusType = 'uploading';
    uploadProgress = 0;

    // Simulate animated upload progress (0 to 100%)
    const interval = setInterval(() => {
      if (uploadProgress < 90) {
        uploadProgress += Math.floor(Math.random() * 20) + 5;
      }
    }, 150);

    try {
      const formData = new FormData();
      formData.append('file', file);
      formData.append('format', file.name.endsWith('.zip') ? 'TDATA_ZIP' : 'SESSION');

      const res = await fetch('/api/v1/accounts/onboard/file', {
        method: 'POST',
        body: formData
      });
      clearInterval(interval);
      uploadProgress = 100;

      const data = await res.json();
      if (res.ok) {
        uploadStatus = `File "${file.name}" onboarded successfully! Added account: ${data.phoneNumber}`;
        uploadStatusType = 'success';
        await loadAccounts();
        setTimeout(() => {
          uploadStatusType = 'idle';
          uploadStatus = '';
          uploadProgress = 0;
        }, 8000);
      } else {
        uploadStatus = data.message || 'Failed to onboard account from file.';
        uploadStatusType = 'error';
        uploadProgress = 0;
      }
    } catch (err) {
      clearInterval(interval);
      console.error(err);
      uploadStatus = 'Network error while uploading session file.';
      uploadStatusType = 'error';
      uploadProgress = 0;
    }
  }

  // Delete Account
  async function deleteAccount(id: number, phone: string) {
    if (!confirm(`Are you sure you want to terminate the session for account ${phone}?`)) {
      return;
    }
    try {
      const res = await fetch(`/api/v1/accounts/${id}`, {
        method: 'DELETE'
      });
      if (res.ok) {
        await loadAccounts();
      } else {
        alert('Failed to delete account session.');
      }
    } catch (err) {
      console.error(err);
      alert('Network error deleting account session.');
    }
  }

  // Simulate account Ban (turns status badge red immediately)
  async function simulateBan(id: number) {
    try {
      const res = await fetch(`/api/v1/accounts/${id}/status`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status: 'Permanent Ban' })
      });
      if (res.ok) {
        // Find index and update status in-place immediately for high response and instant reactivity
        const updated = await res.json();
        accounts = accounts.map(acc => acc.id === id ? { ...acc, status: updated.status } : acc);
      } else {
        alert('Failed to simulate ban.');
      }
    } catch (err) {
      console.error(err);
      alert('Error updating status.');
    }
  }

  // Open proxy modal/form for selected account
  function openProxyForm(acc: TelegramAccount) {
    selectedProxyAccount = acc;
    proxyHost = acc.proxy?.host || '';
    proxyPort = acc.proxy?.port || 1080;
    proxyType = acc.proxy?.type || 'SOCKS5';
    proxyUsername = acc.proxy?.username || '';
    proxyPassword = '';
    proxyStatus = '';
    proxyStatusType = 'idle';
  }

  // Save/Bind Proxy details
  async function saveProxy() {
    if (!selectedProxyAccount) return;
    if (!proxyHost.trim() || !proxyPort) {
      proxyStatus = 'Please provide host and port for proxy.';
      proxyStatusType = 'error';
      return;
    }

    proxyStatus = 'Binding proxy...';
    proxyStatusType = 'saving';

    try {
      const res = await fetch(`/api/v1/accounts/${selectedProxyAccount.id}/proxy`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          host: proxyHost.trim(),
          port: proxyPort,
          type: proxyType,
          username: proxyUsername.trim() || null,
          password: proxyPassword.trim() || null
        })
      });
      const data = await res.json();
      if (res.ok) {
        proxyStatus = 'Proxy bound successfully!';
        proxyStatusType = 'success';
        await loadAccounts();
        setTimeout(() => {
          selectedProxyAccount = null;
        }, 1500);
      } else {
        proxyStatus = data.message || 'Failed to bind proxy to session.';
        proxyStatusType = 'error';
      }
    } catch (err) {
      console.error(err);
      proxyStatus = 'Network error saving proxy configuration.';
      proxyStatusType = 'error';
    }
  }

  // Helper styles for status badges
  function getStatusBadgeStyle(status: string): string {
    switch (status) {
      case 'Active':
        return 'text-green-400 bg-green-500/10 border border-green-500/20';
      case 'Temporary Spam-Block':
        return 'text-amber-500 bg-amber-500/10 border border-amber-500/20';
      case 'Permanent Ban':
        return 'text-red-400 bg-red-500/10 border border-red-500/20';
      case 'Re-authorization Required':
        return 'text-[#adc6ff] bg-[#adc6ff]/10 border border-[#adc6ff]/20';
      default:
        return 'text-gray-400 bg-gray-500/10 border border-gray-500/20';
    }
  }
</script>

<div class="min-h-screen bg-[#0A0A0B] text-[#e3e2e7] flex flex-col font-sans selection:bg-[#adc6ff]/30 selection:text-white">
  <!-- TopAppBar -->
  <header class="fixed top-0 left-0 w-full z-50 flex items-center justify-between px-4 md:px-8 h-16 bg-[#121317]/80 backdrop-blur-xl border-b border-white/10" aria-label="Main Header">
    <div class="flex items-center gap-4">
      <button
        class="flex items-center justify-center p-2 rounded-full hover:bg-white/5 active:scale-95 transition-transform text-[#adc6ff] focus:outline-none focus-visible:ring-2 focus-visible:ring-[#adc6ff]"
        aria-label="Go back"
      >
        <span class="material-symbols-outlined">arrow_back</span>
      </button>
      <h1 class="text-lg md:text-xl font-semibold text-white">
        {activeTab === 'accounts' ? 'Account Management Dashboard' : 'AI Settings'}
      </h1>
    </div>
    <div class="flex items-center gap-2">
      {#if activeTab === 'ai-tuning'}
        <!-- Bio-semantic save indicator -->
        <span
          class="text-xs md:text-sm font-medium transition-colors duration-300 px-3 py-1 rounded
            {saveStatusType === 'success' ? 'text-green-400 bg-green-500/10' : ''}
            {saveStatusType === 'saving' ? 'text-amber-400 bg-amber-500/10' : ''}
            {saveStatusType === 'error' ? 'text-red-400 bg-red-500/10' : ''}"
          role="status"
        >
          {saveStatus}
        </span>
      {:else}
        <!-- Accounts Status Summary Indicator -->
        <div class="flex items-center gap-2 bg-[#1C1C1E] px-3 py-1 rounded-full border border-white/5">
          <span class="w-2 h-2 rounded-full bg-green-400 animate-pulse" aria-hidden="true"></span>
          <span class="text-xs font-mono text-gray-300">Pool: {accounts.length} Accounts</span>
        </div>
      {/if}
      <button
        class="flex items-center justify-center p-2 rounded-full hover:bg-white/5 active:scale-95 transition-transform text-[#adc6ff] focus:outline-none focus-visible:ring-2 focus-visible:ring-[#adc6ff]"
        aria-label="More settings"
      >
        <span class="material-symbols-outlined">more_vert</span>
      </button>
    </div>
  </header>

  <!-- Main Content Viewport -->
  <main class="pt-24 pb-48 px-4 md:px-8 max-w-5xl mx-auto w-full space-y-8 flex-grow">

    <!-- TAB 1: ACCOUNT MANAGEMENT DASHBOARD -->
    {#if activeTab === 'accounts'}
      <section class="space-y-2" aria-labelledby="accounts-section-title">
        <h2 id="accounts-section-title" class="text-2xl md:text-3xl font-bold text-[#adc6ff] tracking-tight">Onboard & Monitor Sessions</h2>
        <p class="text-gray-400 max-w-xl text-sm md:text-base">Operators can visually manage the Telegram accounts pool, proxies, and session health in real time.</p>
      </section>

      <!-- Bento Overview Cards -->
      <div class="grid grid-cols-1 sm:grid-cols-3 gap-4" aria-label="Account pool overview">
        <div class="glass-card p-5 rounded-xl flex flex-col justify-between">
          <div class="flex items-center justify-between mb-2">
            <span class="material-symbols-outlined text-[#adc6ff]" aria-hidden="true">group</span>
            <span class="text-xs font-mono text-green-400 font-semibold">Ready</span>
          </div>
          <div>
            <h3 class="text-2xl font-bold font-mono text-white">
              {accounts.filter(a => a.status === 'Active').length}
            </h3>
            <p class="text-xs text-gray-400 uppercase tracking-wider font-semibold mt-1">Active Accounts</p>
          </div>
        </div>

        <div class="glass-card p-5 rounded-xl flex flex-col justify-between">
          <div class="flex items-center justify-between mb-2">
            <span class="material-symbols-outlined text-amber-500" aria-hidden="true">warning</span>
            <span class="text-xs font-mono text-amber-400 font-semibold">Spam Blocked</span>
          </div>
          <div>
            <h3 class="text-2xl font-bold font-mono text-white">
              {accounts.filter(a => a.status === 'Temporary Spam-Block').length}
            </h3>
            <p class="text-xs text-gray-400 uppercase tracking-wider font-semibold mt-1">Temporary Limits</p>
          </div>
        </div>

        <div class="glass-card p-5 rounded-xl flex flex-col justify-between">
          <div class="flex items-center justify-between mb-2">
            <span class="material-symbols-outlined text-red-400" aria-hidden="true">dangerous</span>
            <span class="text-xs font-mono text-red-400 font-semibold">Banned</span>
          </div>
          <div>
            <h3 class="text-2xl font-bold font-mono text-white">
              {accounts.filter(a => a.status === 'Permanent Ban').length}
            </h3>
            <p class="text-xs text-gray-400 uppercase tracking-wider font-semibold mt-1">Banned Sessions</p>
          </div>
        </div>
      </div>

      <!-- Onboarding Panels (File Upload + OTP Onboarding) -->
      <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <!-- Panel A: Drag & Drop .session File Ingestion -->
        <div class="glass-card p-6 rounded-xl space-y-4" aria-labelledby="file-onboarding-title">
          <h3 id="file-onboarding-title" class="text-lg font-semibold flex items-center gap-2 text-white">
            <span class="material-symbols-outlined text-[#adc6ff]" aria-hidden="true">upload_file</span>
            Onboard via Session Files
          </h3>
          <p class="text-xs text-gray-400">Directly upload pre-authenticated <code>.session</code> or compressed <code>tdata</code> files to instantly mount active sessions.</p>

          <!-- Dropzone (Accessible) -->
          <div
            class="border-2 border-dashed rounded-xl p-6 text-center cursor-pointer transition-all flex flex-col items-center justify-center space-y-2
              {uploadDragOver ? 'border-[#adc6ff] bg-[#adc6ff]/5' : 'border-white/10 hover:border-white/20 bg-white/[0.02]'}"
            on:dragover={handleDragOver}
            on:dragleave={handleDragLeave}
            on:drop={handleDrop}
            on:click={() => uploadFileElement?.click()}
            on:keydown={handleDropzoneKeyDown}
            role="button"
            tabindex="0"
            aria-label="Upload .session file dropzone"
          >
            <input
              type="file"
              accept=".session,.zip"
              bind:this={uploadFileElement}
              on:change={handleFileSelect}
              class="hidden"
            />
            <span class="material-symbols-outlined text-3xl text-gray-400" aria-hidden="true">cloud_upload</span>
            <div>
              <p class="text-sm font-semibold text-white">Drag & drop session files here</p>
              <p class="text-xs text-gray-400 mt-1">or click to browse from device</p>
            </div>
            <span class="text-[10px] bg-white/5 text-gray-400 px-2 py-0.5 rounded uppercase font-mono">Supports .session, tdata.zip</span>
          </div>

          <!-- File Upload Progress Alert -->
          {#if uploadStatus}
            <div
              class="p-4 rounded-lg text-xs space-y-2 transition-all duration-300
                {uploadStatusType === 'success' ? 'bg-green-500/10 text-green-400 border border-green-500/20' : ''}
                {uploadStatusType === 'uploading' ? 'bg-amber-500/10 text-amber-400 border border-amber-500/20' : ''}
                {uploadStatusType === 'error' ? 'bg-red-500/10 text-red-400 border border-red-500/20' : ''}"
              role="alert"
            >
              <div class="flex items-center gap-2">
                {#if uploadStatusType === 'uploading'}
                  <span class="material-symbols-outlined text-sm animate-spin" aria-hidden="true">sync</span>
                {:else if uploadStatusType === 'success'}
                  <span class="material-symbols-outlined text-sm" aria-hidden="true">check_circle</span>
                {:else}
                  <span class="material-symbols-outlined text-sm" aria-hidden="true">error</span>
                {/if}
                <span class="font-medium">{uploadStatus}</span>
              </div>

              {#if uploadStatusType === 'uploading'}
                <!-- Visual progress bar protection for CLS and immediate feedback -->
                <div class="w-full bg-white/10 rounded-full h-1.5 overflow-hidden">
                  <div class="bg-amber-400 h-full rounded-full transition-all duration-200" style="width: {uploadProgress}%"></div>
                </div>
                <div class="text-[10px] text-right text-gray-400 font-mono">{uploadProgress}% uploaded</div>
              {/if}
            </div>
          {/if}
        </div>

        <!-- Panel B: Interactive OTP Verification Onboarding -->
        <div class="glass-card p-6 rounded-xl space-y-4" aria-labelledby="otp-onboarding-title">
          <h3 id="otp-onboarding-title" class="text-lg font-semibold flex items-center gap-2 text-white">
            <span class="material-symbols-outlined text-[#adc6ff]" aria-hidden="true">vpn_key</span>
            Onboard via OTP Verification
          </h3>
          <p class="text-xs text-gray-400">Request a security credential directly from Telegram to start a safe authenticated session on the platform.</p>

          {#if otpStep === 'request'}
            <!-- Step 1: Request OTP -->
            <form class="space-y-3" on:submit|preventDefault={requestOtp}>
              <div class="space-y-1">
                <label for="otp-phone" class="text-xs font-semibold text-gray-300">Telegram Phone Number (E.164)</label>
                <div class="flex items-center bg-[#1C1C1E] border border-white/10 focus-within:border-[#adc6ff] rounded-lg px-3 py-2 transition-all">
                  <span class="material-symbols-outlined text-sm text-gray-400 mr-2" aria-hidden="true">call</span>
                  <input
                    type="tel"
                    id="otp-phone"
                    bind:value={otpPhoneNumber}
                    placeholder="+1234567890"
                    class="bg-transparent border-none p-0 focus:ring-0 text-sm text-white outline-none w-full font-mono"
                    aria-label="Telegram phone number input"
                  />
                </div>
              </div>
              <button
                type="submit"
                disabled={otpStatusType === 'loading'}
                class="w-full bg-[#adc6ff] text-[#002e69] py-2 px-4 rounded-lg font-semibold text-sm hover:bg-[#c1d7ff] active:scale-95 transition-all"
              >
                Request OTP Code
              </button>
            </form>
          {:else}
            <!-- Step 2: Verify OTP -->
            <form class="space-y-3" on:submit|preventDefault={verifyOtp}>
              <div class="space-y-2 bg-white/5 p-3 rounded-lg border border-white/5">
                <p class="text-xs font-semibold text-[#adc6ff] flex items-center gap-1">
                  <span class="material-symbols-outlined text-sm" aria-hidden="true">fingerprint</span>
                  Reference ID: <span class="font-mono text-white">{otpReferenceId}</span>
                </p>
                <p class="text-[10px] text-gray-400">Please provide the numeric code dispatched to your authorized Telegram active application sessions.</p>
              </div>

              <div class="space-y-1">
                <label for="otp-code" class="text-xs font-semibold text-gray-300">One-Time Password Code</label>
                <div class="flex items-center bg-[#1C1C1E] border border-white/10 focus-within:border-[#adc6ff] rounded-lg px-3 py-2 transition-all">
                  <span class="material-symbols-outlined text-sm text-gray-400 mr-2" aria-hidden="true">lock</span>
                  <input
                    type="text"
                    id="otp-code"
                    bind:value={otpCode}
                    placeholder="Enter 5-digit code"
                    class="bg-transparent border-none p-0 focus:ring-0 text-sm text-white outline-none w-full font-mono"
                    aria-label="OTP verification code input"
                  />
                </div>
              </div>

              <div class="grid grid-cols-2 gap-2">
                <button
                  type="button"
                  on:click={() => { otpStep = 'request'; otpStatus = ''; }}
                  class="bg-white/5 border border-white/10 text-white py-2 rounded-lg text-xs font-semibold hover:bg-white/10 transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={otpStatusType === 'loading'}
                  class="bg-[#adc6ff] text-[#002e69] py-2 rounded-lg text-xs font-semibold hover:bg-[#c1d7ff] active:scale-95 transition-all"
                >
                  Verify Code
                </button>
              </div>
            </form>
          {/if}

          <!-- OTP Status feedback -->
          {#if otpStatus}
            <div
              class="p-3 rounded-lg text-xs flex items-center gap-2 border transition-all duration-200
                {otpStatusType === 'success' ? 'bg-green-500/10 text-green-400 border-green-500/20' : ''}
                {otpStatusType === 'loading' ? 'bg-amber-500/10 text-amber-400 border-amber-500/20' : ''}
                {otpStatusType === 'error' ? 'bg-red-500/10 text-red-400 border-red-500/20' : ''}"
              role="status"
            >
              {#if otpStatusType === 'loading'}
                <span class="material-symbols-outlined text-sm animate-spin" aria-hidden="true">sync</span>
              {:else if otpStatusType === 'success'}
                <span class="material-symbols-outlined text-sm" aria-hidden="true">check_circle</span>
              {:else}
                <span class="material-symbols-outlined text-sm" aria-hidden="true">error</span>
              {/if}
              <span>{otpStatus}</span>
            </div>
          {/if}
        </div>
      </div>

      <!-- Proxy Configuration Modal/Drawer Form Overlay -->
      {#if selectedProxyAccount}
        <div class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm" role="dialog" aria-modal="true" aria-labelledby="proxy-modal-title">
          <div class="glass-card max-w-md w-full rounded-2xl p-6 space-y-4 shadow-2xl border border-white/10">
            <div class="flex justify-between items-start">
              <div>
                <h3 id="proxy-modal-title" class="text-lg font-bold text-white flex items-center gap-1">
                  <span class="material-symbols-outlined text-[#adc6ff]" aria-hidden="true">router</span>
                  Configure Isolated Proxy
                </h3>
                <p class="text-xs text-gray-400 mt-1">Bind proxy to prevent multi-account IP ban chaining.</p>
              </div>
              <button
                class="material-symbols-outlined text-gray-400 hover:text-white transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-white rounded-full p-1"
                on:click={() => selectedProxyAccount = null}
                aria-label="Close proxy configuration dialog"
              >
                close
              </button>
            </div>

            <div class="space-y-3">
              <div class="bg-white/5 p-3 rounded-lg border border-white/5 text-xs">
                <span class="font-semibold text-gray-300">Target Session:</span>
                <span class="font-mono text-[#adc6ff] ml-1">{selectedProxyAccount.phoneNumber}</span>
              </div>

              <!-- Host & Port input row -->
              <div class="grid grid-cols-3 gap-2">
                <div class="col-span-2 space-y-1">
                  <label for="proxy-host" class="text-xs font-semibold text-gray-300">Host (IPv4/IPv6)</label>
                  <input
                    type="text"
                    id="proxy-host"
                    bind:value={proxyHost}
                    placeholder="127.0.0.1"
                    class="bg-[#1C1C1E] border border-white/10 focus:border-[#adc6ff] rounded-lg px-3 py-2 text-sm text-white outline-none w-full font-mono"
                  />
                </div>
                <div class="space-y-1">
                  <label for="proxy-port" class="text-xs font-semibold text-gray-300">Port</label>
                  <input
                    type="number"
                    id="proxy-port"
                    bind:value={proxyPort}
                    placeholder="1080"
                    class="bg-[#1C1C1E] border border-white/10 focus:border-[#adc6ff] rounded-lg px-3 py-2 text-sm text-white outline-none w-full font-mono"
                  />
                </div>
              </div>

              <!-- Proxy Type Selection -->
              <div class="space-y-1">
                <label for="proxy-type" class="text-xs font-semibold text-gray-300">Protocol Type</label>
                <div class="relative">
                  <select
                    id="proxy-type"
                    bind:value={proxyType}
                    class="w-full appearance-none bg-[#1C1C1E] border border-white/10 focus:border-[#adc6ff] rounded-lg px-3 py-2 text-sm text-white outline-none pr-10"
                    aria-label="Proxy protocol type selector"
                  >
                    <option value="SOCKS5">SOCKS5 (Recommended)</option>
                    <option value="HTTP">HTTP Proxy</option>
                  </select>
                  <span class="material-symbols-outlined absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none text-gray-400" aria-hidden="true">unfold_more</span>
                </div>
              </div>

              <!-- Credentials row -->
              <div class="grid grid-cols-2 gap-2">
                <div class="space-y-1">
                  <label for="proxy-user" class="text-xs font-semibold text-gray-300">Username (Optional)</label>
                  <input
                    type="text"
                    id="proxy-user"
                    bind:value={proxyUsername}
                    placeholder="user"
                    class="bg-[#1C1C1E] border border-white/10 focus:border-[#adc6ff] rounded-lg px-3 py-2 text-sm text-white outline-none w-full font-mono"
                  />
                </div>
                <div class="space-y-1">
                  <label for="proxy-pass" class="text-xs font-semibold text-gray-300">Password (Optional)</label>
                  <input
                    type="password"
                    id="proxy-pass"
                    bind:value={proxyPassword}
                    placeholder="secret"
                    class="bg-[#1C1C1E] border border-white/10 focus:border-[#adc6ff] rounded-lg px-3 py-2 text-sm text-white outline-none w-full font-mono"
                  />
                </div>
              </div>
            </div>

            <!-- Proxy Submit Feedback -->
            {#if proxyStatus}
              <div
                class="p-3 rounded-lg text-xs flex items-center gap-2 border transition-all duration-200
                  {proxyStatusType === 'success' ? 'bg-green-500/10 text-green-400 border-green-500/20' : ''}
                  {proxyStatusType === 'saving' ? 'bg-amber-500/10 text-amber-400 border-amber-500/20' : ''}
                  {proxyStatusType === 'error' ? 'bg-red-500/10 text-red-400 border-red-500/20' : ''}"
                role="status"
              >
                {#if proxyStatusType === 'saving'}
                  <span class="material-symbols-outlined text-sm animate-spin" aria-hidden="true">sync</span>
                {:else if proxyStatusType === 'success'}
                  <span class="material-symbols-outlined text-sm" aria-hidden="true">check_circle</span>
                {:else}
                  <span class="material-symbols-outlined text-sm" aria-hidden="true">error</span>
                {/if}
                <span>{proxyStatus}</span>
              </div>
            {/if}

            <div class="flex justify-end gap-2 pt-2">
              <button
                type="button"
                on:click={() => selectedProxyAccount = null}
                class="bg-white/5 hover:bg-white/10 border border-white/10 text-white py-2 px-4 rounded-lg text-xs font-semibold transition-colors"
              >
                Cancel
              </button>
              <button
                type="button"
                on:click={saveProxy}
                disabled={proxyStatusType === 'saving'}
                class="bg-[#adc6ff] text-[#002e69] py-2 px-4 rounded-lg text-xs font-semibold hover:bg-[#c1d7ff] active:scale-95 transition-all"
              >
                Save Binding
              </button>
            </div>
          </div>
        </div>
      {/if}

      <!-- Accounts Pool Section -->
      <section class="glass-card rounded-xl p-6 space-y-4" aria-labelledby="pool-title">
        <div class="flex items-center justify-between">
          <div class="space-y-1">
            <h3 id="pool-title" class="text-lg font-semibold text-white flex items-center gap-2">
              <span class="material-symbols-outlined text-[#adc6ff]" aria-hidden="true">dns</span>
              Onboarded Sessions Pool
            </h3>
            <p class="text-xs text-gray-400">Total active connections and isolated proxies mapped below.</p>
          </div>
          <button
            on:click={loadAccounts}
            disabled={isLoadingAccounts}
            class="text-[#adc6ff] hover:opacity-80 active:scale-95 font-medium text-sm transition-all flex items-center gap-1 focus:outline-none focus-visible:ring-2 focus-visible:ring-[#adc6ff]"
            aria-label="Refresh session accounts pool"
          >
            <span class="material-symbols-outlined text-sm {isLoadingAccounts ? 'animate-spin' : ''}">refresh</span>
            Refresh Pool
          </button>
        </div>

        {#if isLoadingAccounts && accounts.length === 0}
          <!-- Skeletal loader for CLS mitigation -->
          <div class="space-y-3 py-6 animate-pulse">
            <div class="h-10 bg-white/5 rounded-lg w-full"></div>
            <div class="h-10 bg-white/5 rounded-lg w-full"></div>
            <div class="h-10 bg-white/5 rounded-lg w-full"></div>
          </div>
        {:else if accountsError}
          <div class="bg-red-500/10 text-red-400 border border-red-500/20 p-4 rounded-xl text-center text-sm" role="alert">
            <p class="font-semibold">Failed to fetch active pool.</p>
            <p class="text-xs mt-1 text-gray-400">{accountsError}</p>
          </div>
        {:else if accounts.length === 0}
          <div class="text-center py-12 border border-dashed border-white/10 rounded-xl space-y-3">
            <span class="material-symbols-outlined text-4xl text-gray-500" aria-hidden="true">folder_open</span>
            <p class="text-sm font-semibold text-gray-400">No Telegram accounts onboarded yet.</p>
            <p class="text-xs text-gray-500">Add an account via File upload or OTP authentication to start outreach.</p>
          </div>
        {:else}
          <!-- Responsive Table & Cards layout -->
          <div class="overflow-x-auto rounded-lg border border-white/5">
            <table class="w-full text-left border-collapse" aria-label="Telegram Accounts Pool">
              <thead>
                <tr class="bg-white/[0.02] border-b border-white/5 text-xs text-gray-400 font-semibold uppercase tracking-wider font-mono">
                  <th class="px-4 py-3">Phone Identity</th>
                  <th class="px-4 py-3">Proxy Assign</th>
                  <th class="px-4 py-3">Session Health Status</th>
                  <th class="px-4 py-3 text-right">Actions / Controls</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-white/5 text-sm">
                {#each accounts as acc (acc.id)}
                  <tr class="hover:bg-white/[0.01] transition-colors group">
                    <td class="px-4 py-4">
                      <div class="flex items-center gap-3">
                        <div class="w-8 h-8 rounded-full bg-[#adc6ff]/10 text-[#adc6ff] flex items-center justify-center font-mono text-xs font-semibold">
                          TG
                        </div>
                        <div class="flex flex-col">
                          <span class="font-semibold text-white font-mono">{acc.phoneNumber}</span>
                          <span class="text-[10px] text-gray-500">ID: {acc.id}</span>
                        </div>
                      </div>
                    </td>
                    <td class="px-4 py-4">
                      {#if acc.proxy}
                        <div class="flex flex-col">
                          <span class="font-semibold text-white text-xs font-mono">{acc.proxy.host}:{acc.proxy.port}</span>
                          <span class="text-[10px] text-gray-400 font-semibold uppercase">{acc.proxy.type} {acc.proxy.username ? `(${acc.proxy.username})` : ''}</span>
                        </div>
                      {:else}
                        <span class="text-xs text-amber-400 font-semibold flex items-center gap-1">
                          <span class="material-symbols-outlined text-xs" aria-hidden="true">warning</span>
                          No Proxy Binded
                        </span>
                      {/if}
                    </td>
                    <td class="px-4 py-4">
                      <!-- Active Status Badge turns red immediately on Permanent Ban -->
                      <span class="px-2.5 py-1 rounded-full text-xs font-semibold {getStatusBadgeStyle(acc.status)}">
                        {acc.status}
                      </span>
                    </td>
                    <td class="px-4 py-4 text-right">
                      <div class="flex items-center justify-end gap-1.5 opacity-90 group-hover:opacity-100 transition-opacity">
                        <!-- Simulate Ban action -->
                        {#if acc.status !== 'Permanent Ban'}
                          <button
                            on:click={() => simulateBan(acc.id)}
                            class="bg-red-500/10 hover:bg-red-500/20 border border-red-500/20 text-red-400 py-1 px-2.5 rounded text-xs font-semibold active:scale-95 transition-all"
                            title="Simulate Telegram Ban immediately"
                          >
                            Simulate Ban
                          </button>
                        {/if}
                        <!-- Bind Proxy action -->
                        <button
                          on:click={() => openProxyForm(acc)}
                          class="bg-white/5 hover:bg-white/10 border border-white/5 text-gray-300 hover:text-white py-1 px-2.5 rounded text-xs font-semibold active:scale-95 transition-all"
                        >
                          Bind Proxy
                        </button>
                        <!-- Delete action -->
                        <button
                          on:click={() => deleteAccount(acc.id, acc.phoneNumber)}
                          class="material-symbols-outlined text-gray-400 hover:text-red-400 p-1 hover:bg-white/5 rounded transition-all focus:outline-none focus-visible:ring-1 focus-visible:ring-red-400"
                          aria-label="Remove and delete account session"
                        >
                          delete
                        </button>
                      </div>
                    </td>
                  </tr>
                {/each}
              </tbody>
            </table>
          </div>
        {/if}
      </section>
    {/if}

    <!-- TAB 2: ORIGINAL SYSTEM / PROMPT SETTINGS view -->
    {#if activeTab === 'ai-tuning'}
      <!-- Descriptive Header -->
      <section class="space-y-2" aria-labelledby="section-title">
        <h2 id="section-title" class="text-2xl md:text-3xl font-bold text-[#adc6ff] tracking-tight">AI Persona Tuning</h2>
        <p class="text-gray-400 max-w-xl text-sm md:text-base">Configure how your AI assistant perceives instructions and manages interaction boundaries.</p>
      </section>

      <!-- System Prompt Editor Section -->
      <section class="glass-card rounded-xl p-6 space-y-4" aria-labelledby="prompt-heading">
        <div class="flex items-center justify-between">
          <h3 id="prompt-heading" class="text-lg font-semibold flex items-center gap-2 text-white">
            <span class="material-symbols-outlined text-[#68d3ff]" aria-hidden="true">psychology</span>
            System Prompt
          </h3>
          <button
            class="text-[#adc6ff] hover:opacity-80 active:scale-95 font-medium text-sm transition-all focus:outline-none focus-visible:ring-2 focus-visible:ring-[#adc6ff]"
            on:click={resetToDefault}
            aria-label="Reset prompt to default"
          >
            Reset to Default
          </button>
        </div>

        <div class="relative highlighter-container">
          <!-- Overlay for highlighting placeholders -->
          <div
            bind:this={overlayElement}
            class="highlighter-overlay custom-scrollbar"
            aria-hidden="true"
          >
            {@html highlightPrompt(systemPrompt)}
          </div>
          <!-- Textarea input -->
          <textarea
            bind:this={textareaElement}
            bind:value={systemPrompt}
            on:scroll={handleScroll}
            on:input={handleScroll}
            class="highlighter-textarea custom-scrollbar"
            id="systemPrompt"
            placeholder="Enter system instructions..."
            maxlength="2000"
            aria-label="AI System Prompt"
          ></textarea>
        </div>

        <div class="flex justify-between items-center text-xs text-gray-400">
          <p>Tip: Placeholders inside curly braces like <code class="text-[#adc6ff]">{`{name}`}</code> or spintax like <code class="text-amber-300">{`{Hi|Hello}`}</code> will be highlighted.</p>
          <span class:text-red-400={systemPrompt.length > 1800} class="font-mono">
            {systemPrompt.length} / 2000
          </span>
        </div>
      </section>

      <!-- Stop Triggers Section -->
      <section class="glass-card rounded-xl p-6 space-y-4" aria-labelledby="triggers-heading">
        <div class="flex items-center justify-between">
          <h3 id="triggers-heading" class="text-lg font-semibold flex items-center gap-2 text-white">
            <span class="material-symbols-outlined text-[#ffb4ab]" aria-hidden="true">dangerous</span>
            Stop Triggers
          </h3>
          <span class="material-symbols-outlined text-gray-400 text-sm" aria-label="Stop triggers info">info</span>
        </div>

        <div class="flex flex-wrap gap-2 items-center" id="triggerContainer">
          {#each stopTriggers as trigger (trigger.keyword)}
            <div class="flex items-center gap-2 bg-[#343539] px-3 py-1.5 rounded-full border border-white/5 group transition-all hover:border-[#adc6ff]/50">
              <span class="font-mono text-xs text-white">{trigger.keyword}</span>
              <button
                class="material-symbols-outlined text-xs text-gray-400 hover:text-[#ffb4ab] transition-colors focus:outline-none focus-visible:ring-1 focus-visible:ring-[#ffb4ab] rounded-full"
                on:click={() => removeTrigger(trigger.keyword)}
                aria-label="Remove stop trigger {trigger.keyword}"
              >
                close
              </button>
            </div>
          {/each}

          <!-- Add Trigger Input -->
          <div class="flex items-center bg-[#1C1C1E] border border-[#3A3A3C] border-dashed rounded-full px-3 py-1 focus-within:border-[#adc6ff] transition-all">
            <input
              type="text"
              bind:value={triggerInput}
              on:keydown={handleTriggerKeydown}
              class="bg-transparent border-none p-0 focus:ring-0 text-xs font-mono w-24 text-white outline-none"
              id="triggerInput"
              placeholder="Add trigger..."
              aria-label="Add stop trigger input"
            />
            <button
              class="material-symbols-outlined text-sm text-[#adc6ff] ml-1 p-0.5 hover:bg-white/5 rounded-full focus:outline-none focus-visible:ring-1 focus-visible:ring-[#adc6ff]"
              on:click={addTrigger}
              aria-label="Add trigger button"
            >
              add
            </button>
          </div>
        </div>
        <p class="text-xs text-gray-400 italic">Generation will cease immediately if these keywords appear in the output stream.</p>
      </section>

      <!-- Model Version Section -->
      <section class="glass-card rounded-xl p-6 space-y-4" aria-labelledby="model-heading">
        <h3 id="model-heading" class="text-lg font-semibold flex items-center gap-2 text-white">
          <span class="material-symbols-outlined text-[#68d3ff]" aria-hidden="true">temp_preferences_custom</span>
          Model Version
        </h3>

        <!-- Segmented Buttons -->
        <div class="grid grid-cols-2 gap-1 bg-[#121317] p-1 rounded-xl border border-[#3A3A3C]" role="radiogroup" aria-label="Model Provider Selection">
          <button
            class="py-2.5 px-4 rounded-lg font-medium transition-all focus:outline-none focus-visible:ring-2 focus-visible:ring-[#adc6ff] text-sm
              {modelVersion === 'GPT-4-Turbo' ? 'bg-[#4b8eff] text-[#001a41]' : 'text-gray-400 hover:bg-white/5'}"
            on:click={() => { modelVersion = 'GPT-4-Turbo'; saveFullConfig(); }}
            role="radio"
            aria-checked={modelVersion === 'GPT-4-Turbo'}
          >
            GPT-4-Turbo
          </button>
          <button
            class="py-2.5 px-4 rounded-lg font-medium transition-all focus:outline-none focus-visible:ring-2 focus-visible:ring-[#adc6ff] text-sm
              {modelVersion === 'Claude-3.5-S' ? 'bg-[#4b8eff] text-[#001a41]' : 'text-gray-400 hover:bg-white/5'}"
            on:click={() => { modelVersion = 'Claude-3.5-S'; saveFullConfig(); }}
            role="radio"
            aria-checked={modelVersion === 'Claude-3.5-S'}
          >
            Claude-3.5-S
          </button>
        </div>

        <!-- Select Dropdown -->
        <div class="relative w-full">
          <select
            bind:value={subModel}
            class="w-full appearance-none bg-[#1C1C1E] border border-[#3A3A3C] rounded-lg px-4 py-3 text-sm text-white focus:ring-2 focus:ring-[#adc6ff] focus:border-transparent outline-none pr-10"
            aria-label="Model environment tier"
          >
            <option>Production (Stable)</option>
            <option>Canary (Bleeding Edge)</option>
            <option>Llama-3-70B-Offshore</option>
          </select>
          <span class="material-symbols-outlined absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none text-gray-400" aria-hidden="true">unfold_more</span>
        </div>
      </section>
    {/if}
  </main>

  <!-- Footer Action Bar (Contextual) -->
  {#if activeTab === 'ai-tuning'}
    <footer class="fixed bottom-16 left-0 w-full z-40 flex flex-row items-center justify-between px-4 md:px-8 py-4 bg-[#292a2e]/90 backdrop-blur-2xl border-t border-white/10 shadow-lg">
      <button
        class="text-gray-400 hover:text-white active:scale-95 font-medium text-sm transition-all focus:outline-none focus-visible:ring-2 focus-visible:ring-[#adc6ff] rounded px-2 py-1"
        on:click={resetToDefault}
        aria-label="Reset all fields to defaults"
      >
        Reset to Default
      </button>
      <button
        class="bg-[#adc6ff] text-[#002e69] px-6 md:px-8 py-2 md:py-2.5 rounded-full font-semibold text-sm md:text-base active:scale-95 transition-all shadow-xl shadow-[#adc6ff]/10 hover:bg-[#c1d7ff] focus:outline-none focus-visible:ring-2 focus-visible:ring-white"
        on:click={saveFullConfig}
        disabled={isSaving}
      >
        {isSaving ? 'Saving...' : 'Save Changes'}
      </button>
    </footer>
  {/if}

  <!-- BottomNavBar (Tab-based switcher toggles between Prompts/Models vs Account Management views) -->
  <nav class="fixed bottom-0 w-full z-50 flex justify-around items-center px-4 py-2 pb-safe bg-[#121317]/80 backdrop-blur-xl border-t border-white/10" aria-label="Bottom Navigation">
    <button
      class="flex flex-col items-center justify-center transition-all p-2 rounded focus:outline-none focus-visible:text-[#adc6ff]
        {activeTab === 'accounts' ? 'text-[#00285c] bg-[#adc6ff] rounded-full px-5 py-1 active:scale-90 font-semibold' : 'text-gray-400 hover:text-[#adc6ff] active:scale-90'}"
      on:click={() => activeTab = 'accounts'}
      aria-label="Toggle Account Management Dashboard View"
    >
      <span class="material-symbols-outlined">manage_accounts</span>
      <span class="font-mono text-[10px] tracking-wider uppercase mt-0.5">Accounts</span>
    </button>
    <button
      class="flex flex-col items-center justify-center transition-all p-2 rounded focus:outline-none focus-visible:text-[#adc6ff]
        {activeTab === 'ai-tuning' ? 'text-[#00285c] bg-[#adc6ff] rounded-full px-5 py-1 active:scale-90 font-semibold' : 'text-gray-400 hover:text-[#adc6ff] active:scale-90'}"
      on:click={() => activeTab = 'ai-tuning'}
      aria-label="Toggle AI Settings Tuning View"
    >
      <span class="material-symbols-outlined">psychology</span>
      <span class="font-mono text-[10px] tracking-wider uppercase mt-0.5">AI Tuning</span>
    </button>
  </nav>
</div>

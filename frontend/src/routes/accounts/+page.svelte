<script lang="ts">
  import { onMount } from 'svelte';

  // Account Management Dashboard State using Svelte 5 Runes
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

  // Active Tab / View
  let activeView = $state<'accounts'>('accounts');

  // Accounts List State
  let accounts = $state<TelegramAccount[]>([]);
  let isLoadingAccounts = $state(false);
  let accountsError = $state('');

  // OTP Onboarding State
  let otpPhoneNumber = $state('');
  let otpReferenceId = $state('');
  let otpCode = $state('');
  let otpStep = $state<'request' | 'verify'>('request');
  let otpStatus = $state('');
  let otpStatusType = $state<'success' | 'loading' | 'error'>('success');

  // File Onboarding State
  let uploadDragOver = $state(false);
  let uploadFileElement = $state<HTMLInputElement | null>(null);
  let uploadProgress = $state(0);
  let uploadStatus = $state('');
  let uploadStatusType = $state<'idle' | 'uploading' | 'success' | 'error'>('idle');

  // Proxy Binding State
  let selectedProxyAccount = $state<TelegramAccount | null>(null);
  let proxyHost = $state('');
  let proxyPort = $state(1080);
  let proxyType = $state('SOCKS5');
  let proxyUsername = $state('');
  let proxyPassword = $state('');
  let proxyStatus = $state('');
  let proxyStatusType = $state<'idle' | 'saving' | 'success' | 'error'>('idle');

  // On mount, load onboarded account lists
  onMount(async () => {
    await loadAccounts();
  });

  // Load onboarded Accounts from the REST API
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

  // Keyboard navigation for Dropzone (Accessibility WCAG)
  function handleDropzoneKeyDown(e: KeyboardEvent) {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      uploadFileElement?.click();
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

  // Delete Account Session
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
        // Update in-place immediately for instant visual feedback and reactivity
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

<div class="min-h-screen bg-[#0A0A0B] text-[#e3e2e7] flex flex-col font-sans selection:bg-[#adc6ff]/30 selection:text-white pb-24 md:pb-8">
  <!-- TopAppBar -->
  <header class="bg-[#121317] border-b border-white/10 px-4 py-3 flex items-center justify-between sticky top-0 z-40 h-16" aria-label="Main Header">
    <div class="flex items-center gap-3">
      <a
        href="/"
        class="flex items-center justify-center p-2 rounded-full hover:bg-white/5 active:scale-95 transition-transform text-[#adc6ff] focus:outline-none focus-visible:ring-2 focus-visible:ring-[#adc6ff]"
        aria-label="Go back to dashboard"
      >
        <span class="material-symbols-outlined">arrow_back</span>
      </a>
      <h1 class="text-lg md:text-xl font-semibold text-white">Account Management Dashboard</h1>
    </div>
    <div class="flex items-center gap-2">
      <!-- Accounts Status Summary Indicator -->
      <div class="flex items-center gap-2 bg-[#1C1C1E] px-3 py-1 rounded-full border border-white/5">
        <span class="w-2 h-2 rounded-full bg-green-400 animate-pulse" aria-hidden="true"></span>
        <span class="text-xs font-mono text-gray-300">Pool: {accounts.length} Accounts</span>
      </div>
      <button
        class="flex items-center justify-center p-2 rounded-full hover:bg-white/5 active:scale-95 transition-transform text-[#adc6ff] focus:outline-none focus-visible:ring-2 focus-visible:ring-[#adc6ff]"
        aria-label="More settings"
      >
        <span class="material-symbols-outlined">more_vert</span>
      </button>
    </div>
  </header>

  <!-- Main Content Viewport -->
  <main class="max-w-7xl mx-auto p-4 lg:p-8 w-full space-y-8 flex-grow">
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
          ondragover={handleDragOver}
          ondragleave={handleDragLeave}
          ondrop={handleDrop}
          onclick={() => uploadFileElement?.click()}
          onkeydown={handleDropzoneKeyDown}
          role="button"
          tabindex="0"
          aria-label="Upload .session file dropzone"
        >
          <input
            type="file"
            accept=".session,.zip"
            bind:this={uploadFileElement}
            onchange={handleFileSelect}
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
          <form class="space-y-3" onsubmit={requestOtp}>
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
          <form class="space-y-3" onsubmit={verifyOtp}>
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
                onclick={() => { otpStep = 'request'; otpStatus = ''; }}
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
              onclick={() => selectedProxyAccount = null}
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
              onclick={() => selectedProxyAccount = null}
              class="bg-white/5 hover:bg-white/10 border border-white/10 text-white py-2 px-4 rounded-lg text-xs font-semibold transition-colors"
            >
              Cancel
            </button>
            <button
              type="button"
              onclick={saveProxy}
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
          onclick={loadAccounts}
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
                          onclick={() => simulateBan(acc.id)}
                          class="bg-red-500/10 hover:bg-red-500/20 border border-red-500/20 text-red-400 py-1 px-2.5 rounded text-xs font-semibold active:scale-95 transition-all"
                          title="Simulate Telegram Ban immediately"
                        >
                          Simulate Ban
                        </button>
                      {/if}
                      <!-- Bind Proxy action -->
                      <button
                        onclick={() => openProxyForm(acc)}
                        class="bg-white/5 hover:bg-white/10 border border-white/5 text-gray-300 hover:text-white py-1 px-2.5 rounded text-xs font-semibold active:scale-95 transition-all"
                      >
                        Bind Proxy
                      </button>
                      <!-- Delete action -->
                      <button
                        onclick={() => deleteAccount(acc.id, acc.phoneNumber)}
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
  </main>

  <!-- Bottom Navigation Bar (Mobile) -->
  <nav class="fixed bottom-0 left-0 w-full z-50 flex justify-around items-center bg-surface border-t border-outline-variant py-2 px-4 pb-safe bg-[#121317]/85 backdrop-blur-md">
    <a class="flex flex-col items-center justify-center text-on-surface-variant hover:bg-surface-container-low transition-transform duration-150 active:scale-90 px-2 py-1 rounded min-w-[48px] min-h-[48px]" href="/">
      <span class="material-symbols-outlined">rocket_launch</span>
      <span class="text-label-sm font-label-md">Launch</span>
    </a>
    <a class="flex flex-col items-center justify-center text-primary font-bold transition-transform duration-150 active:scale-90 min-w-[48px] min-h-[48px]" href="/accounts">
      <span class="material-symbols-outlined">group</span>
      <span class="text-label-sm font-label-md">Accounts</span>
    </a>
    <a class="flex flex-col items-center justify-center text-on-surface-variant hover:bg-surface-container-low transition-transform duration-150 active:scale-90 px-2 py-1 rounded min-w-[48px] min-h-[48px]" href="/inbox">
      <span class="material-symbols-outlined">forum</span>
      <span class="text-label-sm font-label-md">Inbox</span>
    </a>
  </nav>
</div>

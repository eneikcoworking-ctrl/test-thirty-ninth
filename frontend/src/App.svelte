<script lang="ts">
  import { onMount } from 'svelte';

  // --- State for AI Persona Tuning (from origin/main) ---
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

  let systemPrompt = $state('');
  let stopTriggers = $state<StopTrigger[]>([]);
  let modelVersion = $state('GPT-4-Turbo');
  let subModel = $state('Production (Stable)');
  let triggerInput = $state('');
  let isSaving = $state(false);
  let saveStatus = $state('All changes saved');
  let saveStatusType = $state<'success' | 'saving' | 'error'>('success');
  let defaultPrompt = 'You are a highly analytical AI assistant specialized in technical documentation and software architecture. Your tone is professional, concise, and focused on providing empirical data and verifiable code snippets. Avoid flowery language or conversational fillers. When asked about complex systems, provide high-level abstractions followed by detailed component breakdowns.';

  let textareaElement: HTMLTextAreaElement | null = $state(null);
  let overlayElement: HTMLDivElement | null = $state(null);

  // Load configuration on mount
  onMount(async () => {
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
  });

  function handleScroll() {
    if (textareaElement && overlayElement) {
      overlayElement.scrollTop = textareaElement.scrollTop;
      overlayElement.scrollLeft = textareaElement.scrollLeft;
    }
  }

  function escapeHtml(text: string): string {
    return text
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#039;");
  }

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

  function addTrigger() {
    const trimmed = triggerInput.trim();
    if (trimmed && !stopTriggers.some(t => t.keyword === trimmed)) {
      stopTriggers = [...stopTriggers, { keyword: trimmed, enabled: true }];
      triggerInput = '';
      saveStopTriggers(stopTriggers);
    }
  }

  function removeTrigger(keyword: string) {
    stopTriggers = stopTriggers.filter(t => t.keyword !== keyword);
    saveStopTriggers(stopTriggers);
  }

  function handleTriggerKeydown(event: KeyboardEvent) {
    if (event.key === 'Enter') {
      event.preventDefault();
      addTrigger();
    }
  }

  // --- State for Account Management Dashboard UI ---
  interface Account {
    id: number;
    phoneNumber: string;
    username: string;
    status: 'Active' | 'Temporary Spam-Block' | 'Permanent Ban' | 'Re-authorization Required';
    proxy: string;
    latency: string;
    trustScore: number;
    warmupStage: string;
  }

  let accounts = $state<Account[]>([
    {
      id: 1,
      phoneNumber: '+1 555-0101',
      username: 'tg_manager_01',
      status: 'Active',
      proxy: '198.51.100.12:1080 (SOCKS5)',
      latency: '24ms',
      trustScore: 84,
      warmupStage: 'Warmed-up'
    },
    {
      id: 2,
      phoneNumber: '+44 7700 900077',
      username: 'lead_outreach_uk',
      status: 'Temporary Spam-Block',
      proxy: '203.0.113.45:8080 (HTTP)',
      latency: '150ms',
      trustScore: 45,
      warmupStage: 'Under review'
    },
    {
      id: 3,
      phoneNumber: '+49 151 2345678',
      username: 'banned_bot_de',
      status: 'Permanent Ban',
      proxy: '192.0.2.99:1080 (SOCKS5)',
      latency: 'N/A',
      trustScore: 12,
      warmupStage: 'Banned'
    },
    {
      id: 4,
      phoneNumber: '+81 90 1234 5678',
      username: 'sync_needed_jp',
      status: 'Re-authorization Required',
      proxy: '198.51.100.14:1080 (SOCKS5)',
      latency: '42ms',
      trustScore: 50,
      warmupStage: 'Pending Sync'
    }
  ]);

  let isUploading = $state(false);
  let uploadProgress = $state(0);
  let successBanner = $state('');
  let isProvisionModalOpen = $state(false);

  // Active view tab
  let activeTab = $state<'Dashboard' | 'Accounts' | 'Sessions' | 'Settings'>('Dashboard');

  // Stats calculation
  let activeCount = $derived(accounts.filter(a => a.status === 'Active').length);
  let totalCount = $derived(accounts.length);

  // Simulated upload function
  function handleFileSelected(event: any) {
    const file = event.target.files[0];
    if (!file) return;
    simulateUpload(file.name);
  }

  function simulateDragOver(event: DragEvent) {
    event.preventDefault();
  }

  function simulateDrop(event: DragEvent) {
    event.preventDefault();
    const file = event.dataTransfer?.files[0];
    if (file && file.name.endsWith('.session')) {
      simulateUpload(file.name);
    } else {
      alert("Please upload a valid .session file.");
    }
  }

  function simulateUpload(fileName: string) {
    isUploading = true;
    uploadProgress = 0;
    successBanner = '';

    const interval = setInterval(() => {
      uploadProgress += 10;
      if (uploadProgress >= 100) {
        clearInterval(interval);
        setTimeout(() => {
          isUploading = false;
          isProvisionModalOpen = false;
          successBanner = `Successfully uploaded and onboarded session file: "${fileName}". Added to the active pool.`;

          // Generate a new phone number and add to list
          const randomNum = Math.floor(1000 + Math.random() * 9000);
          const newAccount: Account = {
            id: accounts.length + 1,
            phoneNumber: `+1 202-555-${randomNum}`,
            username: `session_bot_${randomNum}`,
            status: 'Active',
            proxy: '198.51.100.22:1080 (SOCKS5)',
            latency: '35ms',
            trustScore: 78,
            warmupStage: 'Warmed-up'
          };
          accounts = [...accounts, newAccount];
        }, 500);
      }
    }, 150);
  }

  // Ban an account
  function banAccount(id: number) {
    accounts = accounts.map(a => {
      if (a.id === id) {
        return { ...a, status: 'Permanent Ban', warmupStage: 'Banned', latency: 'N/A', trustScore: 10 };
      }
      return a;
    });
  }

  // Restore an account
  function restoreAccount(id: number) {
    accounts = accounts.map(a => {
      if (a.id === id) {
        return { ...a, status: 'Active', warmupStage: 'Warmed-up', latency: '30ms', trustScore: 80 };
      }
      return a;
    });
  }

  function dismissBanner() {
    successBanner = '';
  }
</script>

<div class="min-h-screen font-sans {activeTab === 'Settings' ? 'bg-[#0A0A0B] text-[#e3e2e7]' : 'bg-slate-50 text-slate-900'} flex flex-col">
  <!-- TopAppBar -->
  <header class="w-full top-0 sticky z-50 transition-colors duration-200 {activeTab === 'Settings' ? 'bg-[#121317]/80 backdrop-blur-xl border-b border-white/10' : 'bg-white border-b border-slate-200'}">
    <div class="flex items-center justify-between px-4 py-3 w-full max-w-full">
      <div class="flex items-center gap-4">
        {#if activeTab === 'Settings'}
          <button
            onclick={() => activeTab = 'Dashboard'}
            class="flex items-center justify-center p-2 rounded-full hover:bg-white/5 active:scale-95 transition-transform text-[#adc6ff] focus:outline-none focus-visible:ring-2 focus-visible:ring-[#adc6ff] cursor-pointer"
            aria-label="Go back"
          >
            <span class="material-symbols-outlined">arrow_back</span>
          </button>
          <h1 class="text-lg md:text-xl font-semibold text-white">AI Settings</h1>
        {:else}
          <button class="material-symbols-outlined text-slate-500 hover:bg-slate-100 transition-colors p-2 rounded-full cursor-pointer" aria-label="Menu">
            menu
          </button>
          <h1 class="text-xl md:text-2xl font-bold text-[#3525cd]">AdminCenter</h1>
        {/if}
      </div>

      <div class="flex items-center gap-2">
        {#if activeTab === 'Settings'}
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
          <button
            class="flex items-center justify-center p-2 rounded-full hover:bg-white/5 active:scale-95 transition-transform text-[#adc6ff] focus:outline-none focus-visible:ring-2 focus-visible:ring-[#adc6ff] cursor-pointer"
            aria-label="More settings"
          >
            <span class="material-symbols-outlined">more_vert</span>
          </button>
        {:else}
          <button class="material-symbols-outlined text-[#3525cd] p-2 hover:bg-slate-100 rounded-full transition-colors cursor-pointer" aria-label="Search">
            search
          </button>
        {/if}

        <div class="hidden md:flex gap-4 ml-6">
          <nav class="flex gap-6 items-center">
            <button
              onclick={() => activeTab = 'Dashboard'}
              class="text-xs font-semibold tracking-wider pb-1 transition-colors border-b-2 {activeTab === 'Dashboard' ? 'text-[#3525cd] border-[#3525cd]' : activeTab === 'Settings' ? 'text-gray-400 border-transparent hover:text-[#adc6ff]' : 'text-slate-500 border-transparent hover:text-[#3525cd]'}"
            >
              DASHBOARD
            </button>
            <button
              onclick={() => activeTab = 'Accounts'}
              class="text-xs font-semibold tracking-wider pb-1 transition-colors border-b-2 {activeTab === 'Accounts' ? 'text-[#3525cd] border-[#3525cd]' : activeTab === 'Settings' ? 'text-gray-400 border-transparent hover:text-[#adc6ff]' : 'text-slate-500 border-transparent hover:text-[#3525cd]'}"
            >
              ACCOUNTS ({totalCount})
            </button>
            <button
              onclick={() => activeTab = 'Sessions'}
              class="text-xs font-semibold tracking-wider pb-1 transition-colors border-b-2 {activeTab === 'Sessions' ? 'text-[#3525cd] border-[#3525cd]' : activeTab === 'Settings' ? 'text-gray-400 border-transparent hover:text-[#adc6ff]' : 'text-slate-500 border-transparent hover:text-[#3525cd]'}"
            >
              SESSIONS
            </button>
            <button
              onclick={() => activeTab = 'Settings'}
              class="text-xs font-semibold tracking-wider pb-1 transition-colors border-b-2 {activeTab === 'Settings' ? 'text-[#adc6ff] border-[#adc6ff]' : 'text-slate-500 border-transparent hover:text-[#3525cd]'}"
            >
              SETTINGS
            </button>
          </nav>
        </div>
      </div>
    </div>
  </header>

  {#if activeTab === 'Settings'}
    <main class="pt-8 pb-48 px-4 md:px-8 max-w-5xl mx-auto w-full space-y-8 flex-grow">
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
            class="text-[#adc6ff] hover:opacity-80 active:scale-95 font-medium text-sm transition-all focus:outline-none focus-visible:ring-2 focus-visible:ring-[#adc6ff] cursor-pointer"
            onclick={resetToDefault}
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
            onscroll={handleScroll}
            oninput={handleScroll}
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
                class="material-symbols-outlined text-xs text-gray-400 hover:text-[#ffb4ab] transition-colors focus:outline-none focus-visible:ring-1 focus-visible:ring-[#ffb4ab] rounded-full cursor-pointer"
                onclick={() => removeTrigger(trigger.keyword)}
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
              onkeydown={handleTriggerKeydown}
              class="bg-transparent border-none p-0 focus:ring-0 text-xs font-mono w-24 text-white outline-none"
              id="triggerInput"
              placeholder="Add trigger..."
              aria-label="Add stop trigger input"
            />
            <button
              class="material-symbols-outlined text-sm text-[#adc6ff] ml-1 p-0.5 hover:bg-white/5 rounded-full focus:outline-none focus-visible:ring-1 focus-visible:ring-[#adc6ff] cursor-pointer"
              onclick={addTrigger}
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
            class="py-2.5 px-4 rounded-lg font-medium transition-all focus:outline-none focus-visible:ring-2 focus-visible:ring-[#adc6ff] text-sm cursor-pointer
              {modelVersion === 'GPT-4-Turbo' ? 'bg-[#4b8eff] text-[#001a41]' : 'text-gray-400 hover:bg-white/5'}"
            onclick={() => { modelVersion = 'GPT-4-Turbo'; saveFullConfig(); }}
            role="radio"
            aria-checked={modelVersion === 'GPT-4-Turbo'}
          >
            GPT-4-Turbo
          </button>
          <button
            class="py-2.5 px-4 rounded-lg font-medium transition-all focus:outline-none focus-visible:ring-2 focus-visible:ring-[#adc6ff] text-sm cursor-pointer
              {modelVersion === 'Claude-3.5-S' ? 'bg-[#4b8eff] text-[#001a41]' : 'text-gray-400 hover:bg-white/5'}"
            onclick={() => { modelVersion = 'Claude-3.5-S'; saveFullConfig(); }}
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
    </main>

    <!-- Footer Action Bar -->
    <footer class="fixed bottom-16 left-0 w-full z-40 flex flex-row items-center justify-between px-4 md:px-8 py-4 bg-[#292a2e]/90 backdrop-blur-2xl border-t border-white/10 shadow-lg">
      <button
        class="text-gray-400 hover:text-white active:scale-95 font-medium text-sm transition-all focus:outline-none focus-visible:ring-2 focus-visible:ring-[#adc6ff] rounded px-2 py-1 cursor-pointer"
        onclick={resetToDefault}
        aria-label="Reset all fields to defaults"
      >
        Reset to Default
      </button>
      <button
        class="bg-[#adc6ff] text-[#002e69] px-6 md:px-8 py-2 md:py-2.5 rounded-full font-semibold text-sm md:text-base active:scale-95 transition-all shadow-xl shadow-[#adc6ff]/10 hover:bg-[#c1d7ff] focus:outline-none focus-visible:ring-2 focus-visible:ring-white cursor-pointer"
        onclick={saveFullConfig}
        disabled={isSaving}
      >
        {isSaving ? 'Saving...' : 'Save Changes'}
      </button>
    </footer>
  {:else}
    <!-- Dashboard & Accounts Pool Management Table -->
    <main class="max-w-[1440px] mx-auto p-4 md:p-8 space-y-6 pb-24 md:pb-12">
      <!-- Success Banner -->
      {#if successBanner}
        <div class="bg-emerald-50 text-emerald-900 p-4 rounded-xl card-shadow flex items-center justify-between border border-emerald-200" role="alert" id="success-banner">
          <div class="flex items-center gap-4">
            <span class="material-symbols-outlined text-emerald-600">check_circle</span>
            <p class="text-sm font-semibold">{successBanner}</p>
          </div>
          <button onclick={dismissBanner} class="material-symbols-outlined hover:bg-emerald-100 p-1 rounded-full cursor-pointer" aria-label="Dismiss banner">
            close
          </button>
        </div>
      {/if}

      <!-- Systems Overview Section -->
      <section class="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h2 class="text-2xl font-bold text-slate-900">Systems Overview</h2>
          <p class="text-sm text-slate-500">Real-time status of active outreach infrastructure and account onboarding.</p>
        </div>
        <div class="flex gap-2">
          <button
            onclick={() => isProvisionModalOpen = true}
            class="bg-[#3525cd] text-white text-xs font-semibold tracking-wider px-6 py-2.5 rounded-lg hover:opacity-90 transition-opacity flex items-center gap-1 cursor-pointer"
            id="provision-account-btn"
          >
            <span class="material-symbols-outlined text-[18px]">add</span>
            PROVISION ACCOUNT
          </button>
          <button class="bg-white text-slate-700 border border-slate-200 text-xs font-semibold tracking-wider px-6 py-2.5 rounded-lg hover:bg-slate-50 transition-colors flex items-center gap-1 cursor-pointer">
            <span class="material-symbols-outlined text-[18px]">download</span>
            EXPORT REPORT
          </button>
        </div>
      </section>

      <!-- Bento Grid: Key Metrics -->
      <div class="grid grid-cols-1 md:grid-cols-12 gap-6">
        <!-- Active Sessions Card -->
        <div class="md:col-span-4 bg-white border border-slate-200 rounded-xl p-4 card-shadow group">
          <div class="flex justify-between items-start mb-4">
            <div class="bg-indigo-50 text-indigo-700 p-2 rounded-lg">
              <span class="material-symbols-outlined">monitoring</span>
            </div>
            <div class="flex items-center gap-1 text-emerald-600 text-xs font-semibold">
              <span class="material-symbols-outlined text-[14px]">trending_up</span>
              +12.4%
            </div>
          </div>
          <h3 class="text-xs font-semibold text-slate-500 uppercase tracking-wider">Active Sessions</h3>
          <div class="flex items-end gap-2 mt-1">
            <span class="text-3xl font-bold text-slate-900">1,284</span>
            <span class="text-xs text-slate-500 mb-1">Concurrent users</span>
          </div>
          <div class="mt-6 h-16 flex items-end gap-[4px]">
            <div class="flex-1 bg-indigo-100 rounded-t-sm h-[40%]" title="Mon"></div>
            <div class="flex-1 bg-indigo-100 rounded-t-sm h-[60%]" title="Tue"></div>
            <div class="flex-1 bg-indigo-100 rounded-t-sm h-[45%]" title="Wed"></div>
            <div class="flex-1 bg-indigo-100 rounded-t-sm h-[75%]" title="Thu"></div>
            <div class="flex-1 bg-indigo-100 rounded-t-sm h-[90%]" title="Fri"></div>
            <div class="flex-1 bg-[#3525cd] rounded-t-sm h-[100%]" title="Sat"></div>
            <div class="flex-1 bg-indigo-100 rounded-t-sm h-[80%]" title="Sun"></div>
          </div>
        </div>

        <!-- Total Accounts Card -->
        <div class="md:col-span-4 bg-white border border-slate-200 rounded-xl p-4 card-shadow">
          <div class="flex justify-between items-start mb-4">
            <div class="bg-slate-100 text-slate-700 p-2 rounded-lg">
              <span class="material-symbols-outlined">manage_accounts</span>
            </div>
            <span class="px-2 py-1 bg-emerald-50 text-emerald-700 text-xs font-semibold rounded-full border border-emerald-100">System Healthy</span>
          </div>
          <h3 class="text-xs font-semibold text-slate-500 uppercase tracking-wider">Accounts Pool</h3>
          <div class="flex items-end gap-2 mt-1">
            <span class="text-3xl font-bold text-slate-900" id="accounts-count">{totalCount}</span>
            <span class="text-xs text-slate-500 mb-1">Total verified</span>
          </div>
          <div class="mt-6 flex items-center justify-between">
            <div class="flex -space-x-2">
              <div class="w-8 h-8 rounded-full border-2 border-white bg-indigo-100 text-indigo-700 flex items-center justify-center font-bold text-[10px]">A1</div>
              <div class="w-8 h-8 rounded-full border-2 border-white bg-slate-200 text-slate-700 flex items-center justify-center font-bold text-[10px]">A2</div>
              <div class="w-8 h-8 rounded-full border-2 border-white bg-slate-300 text-slate-700 flex items-center justify-center font-bold text-[10px]">A3</div>
              <div class="flex items-center justify-center w-8 h-8 rounded-full border-2 border-white bg-slate-100 text-xs font-semibold text-slate-500">+{totalCount}</div>
            </div>
            <button onclick={() => activeTab = 'Accounts'} class="text-[#3525cd] text-xs font-semibold hover:underline cursor-pointer">Manage All</button>
          </div>
        </div>

        <!-- Node Health Status -->
        <div class="md:col-span-4 bg-slate-900 text-white rounded-xl p-4 card-shadow relative overflow-hidden">
          <div class="relative z-10 flex flex-col h-full">
            <div class="flex justify-between items-start mb-4">
              <div class="bg-white/10 p-2 rounded-lg">
                <span class="material-symbols-outlined text-white">dns</span>
              </div>
              <div class="flex items-center gap-1">
                <div class="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></div>
                <span class="text-xs font-semibold text-emerald-400">LIVE</span>
              </div>
            </div>
            <h3 class="text-xs font-semibold text-slate-300 uppercase tracking-wider">Node Distribution</h3>
            <div class="mt-1">
              <span class="text-3xl font-bold">99.98%</span>
              <div class="w-full bg-white/10 h-1 rounded-full mt-2 overflow-hidden">
                <div class="bg-emerald-400 h-full w-[99%]"></div>
              </div>
            </div>
            <div class="mt-auto pt-4 flex justify-between border-t border-white/10">
              <div class="text-center">
                <div class="text-xs font-mono font-bold">24ms</div>
                <div class="text-[10px] text-slate-400">Latency</div>
              </div>
              <div class="text-center">
                <div class="text-xs font-mono font-bold">1.2GB/s</div>
                <div class="text-[10px] text-slate-400">Throughput</div>
              </div>
              <div class="text-center">
                <div class="text-xs font-mono font-bold">82%</div>
                <div class="text-[10px] text-slate-400">Load</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Onboarding Modal / Drop Zone -->
      {#if isProvisionModalOpen}
        <div class="fixed inset-0 bg-slate-900/60 backdrop-blur-sm z-50 flex items-center justify-center p-4" role="dialog" aria-modal="true" aria-labelledby="modal-title">
          <div class="bg-white border border-slate-200 rounded-xl max-w-lg w-full p-6 space-y-4 card-shadow">
            <div class="flex justify-between items-center border-b border-slate-100 pb-3">
              <h3 class="text-lg font-bold text-slate-900" id="modal-title">Onboard Telegram Session</h3>
              <button onclick={() => isProvisionModalOpen = false} class="material-symbols-outlined hover:bg-slate-100 p-1 rounded-full cursor-pointer" aria-label="Close dialog">
                close
              </button>
            </div>

            <div class="space-y-2">
              <p class="text-sm text-slate-600">
                Upload pre-authenticated <code class="bg-slate-100 px-1.5 py-0.5 rounded text-xs">.session</code> files to add the warmed account to your operational pool instantly.
              </p>
            </div>

            <!-- Drag and Drop Area -->
            <div
              ondragover={simulateDragOver}
              ondrop={simulateDrop}
              class="border-2 border-dashed border-slate-300 hover:border-[#3525cd] rounded-xl p-8 flex flex-col items-center justify-center space-y-4 bg-slate-50/50 transition-colors cursor-pointer text-center"
              role="region"
              aria-label="File upload dropzone"
            >
              <span class="material-symbols-outlined text-[#3525cd] text-[48px]">cloud_upload</span>
              <div class="space-y-1">
                <p class="text-sm font-semibold text-slate-800">Drag & drop your <code class="bg-slate-100 px-1.5 py-0.5 rounded text-xs font-mono">.session</code> file here</p>
                <p class="text-xs text-slate-500">or browse files from your computer</p>
              </div>
              <input
                type="file"
                accept=".session"
                class="hidden"
                id="session-file-input"
                onchange={handleFileSelected}
              />
              <button
                onclick={() => document.getElementById('session-file-input').click()}
                class="bg-white text-[#3525cd] border border-slate-300 text-xs font-semibold px-4 py-2 rounded-lg hover:bg-slate-50 transition-colors cursor-pointer"
              >
                Browse Files
              </button>
            </div>

            <!-- Uploading State Indicator -->
            {#if isUploading}
              <div class="space-y-2" id="upload-progress-container">
                <div class="flex justify-between items-center text-xs font-semibold text-slate-600">
                  <span class="flex items-center gap-1">
                    <span class="material-symbols-outlined text-[16px] animate-spin">sync</span>
                    Uploading & verifying auth credentials...
                  </span>
                  <span>{uploadProgress}%</span>
                </div>
                <div class="w-full bg-slate-100 h-2 rounded-full overflow-hidden">
                  <div class="bg-[#3525cd] h-full transition-all duration-150" style="width: {uploadProgress}%" id="progress-bar-fill"></div>
                </div>
              </div>
            {/if}

            <div class="flex justify-end gap-2 pt-3 border-t border-slate-100">
              <button
                onclick={() => isProvisionModalOpen = false}
                class="bg-white text-slate-700 border border-slate-300 text-xs font-semibold px-4 py-2 rounded-lg hover:bg-slate-50 transition-colors cursor-pointer"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      {/if}

      <!-- Accounts Pool Management Table -->
      <div class="bg-white border border-slate-200 rounded-xl card-shadow overflow-hidden">
        <div class="px-4 py-4 border-b border-slate-200 flex flex-col md:flex-row justify-between md:items-center bg-slate-50/50 gap-4">
          <div>
            <h3 class="text-lg font-bold text-slate-900">Accounts Pool & Session Health</h3>
            <p class="text-xs text-slate-500">Monitor health status and assign proxies to prevent ban chaining.</p>
          </div>
          <div class="flex gap-2 items-center">
            <span class="text-xs font-semibold text-slate-500 uppercase tracking-wider">Filter:</span>
            <select class="bg-white text-slate-700 border border-slate-200 rounded-lg px-2 py-1 text-xs focus:outline-none focus:border-[#3525cd]">
              <option>All Accounts ({totalCount})</option>
              <option>Active ({activeCount})</option>
              <option>Spam Blocked / Banned</option>
            </select>
          </div>
        </div>

        <div class="overflow-x-auto">
          <table class="w-full text-left border-collapse">
            <thead>
              <tr class="bg-slate-50/50 text-xs font-semibold text-slate-500 uppercase tracking-wider border-b border-slate-200">
                <th class="px-6 py-3">Telegram Account</th>
                <th class="px-6 py-3">Assigned Proxy</th>
                <th class="px-6 py-3">Latency</th>
                <th class="px-6 py-3">Trust Score</th>
                <th class="px-6 py-3">Warm-up Stage</th>
                <th class="px-6 py-3">Status Badge</th>
                <th class="px-6 py-3 text-right">Interventions</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-200 text-sm">
              {#each accounts as account (account.id)}
                <tr class="hover:bg-slate-50/30 transition-colors" data-account-row={account.id}>
                  <td class="px-6 py-4">
                    <div class="flex items-center gap-3">
                      <div class="w-8 h-8 rounded-full bg-indigo-50 text-[#3525cd] flex items-center justify-center font-semibold text-xs">
                        TG
                      </div>
                      <div class="flex flex-col">
                        <span class="font-semibold text-slate-800">{account.phoneNumber}</span>
                        <span class="text-xs text-slate-400">@{account.username}</span>
                      </div>
                    </div>
                  </td>
                  <td class="px-6 py-4">
                    <span class="text-xs font-mono bg-slate-100 text-slate-700 px-2.5 py-1 rounded border border-slate-200">
                      {account.proxy}
                    </span>
                  </td>
                  <td class="px-6 py-4 font-mono text-xs text-slate-500">
                    {account.latency}
                  </td>
                  <td class="px-6 py-4">
                    <div class="flex items-center gap-1">
                      <span class="font-bold {account.trustScore > 70 ? 'text-emerald-600' : account.trustScore > 40 ? 'text-indigo-600' : 'text-red-600'}">
                        {account.trustScore}
                      </span>
                      <span class="text-xs text-slate-400">/100</span>
                    </div>
                  </td>
                  <td class="px-6 py-4 text-slate-600">
                    {account.warmupStage}
                  </td>
                  <td class="px-6 py-4">
                    {#if account.status === 'Active'}
                      <span class="inline-flex items-center gap-1.5 bg-emerald-50 text-emerald-700 px-2.5 py-1 text-xs font-semibold rounded-full border border-emerald-200" data-status-badge="Active">
                        <span class="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
                        Active
                      </span>
                    {:else if account.status === 'Temporary Spam-Block'}
                      <span class="inline-flex items-center gap-1.5 bg-amber-50 text-amber-700 px-2.5 py-1 text-xs font-semibold rounded-full border border-amber-200" data-status-badge="Spam-Block">
                        <span class="w-1.5 h-1.5 rounded-full bg-amber-500"></span>
                        Spam-Block
                      </span>
                    {:else if account.status === 'Permanent Ban'}
                      <span class="inline-flex items-center gap-1.5 bg-red-50 text-red-700 px-2.5 py-1 text-xs font-semibold rounded-full border border-red-200" data-status-badge="Banned">
                        <span class="w-1.5 h-1.5 rounded-full bg-red-500"></span>
                        Permanent Ban
                      </span>
                    {:else}
                      <span class="inline-flex items-center gap-1.5 bg-slate-100 text-slate-600 px-2.5 py-1 text-xs font-semibold rounded-full border border-slate-200" data-status-badge="Other">
                        <span class="w-1.5 h-1.5 rounded-full bg-slate-400"></span>
                        Re-auth Req.
                      </span>
                    {/if}
                  </td>
                  <td class="px-6 py-4 text-right">
                    <div class="flex justify-end gap-2">
                      {#if account.status !== 'Permanent Ban'}
                        <button
                          onclick={() => banAccount(account.id)}
                          class="text-red-600 border border-red-200 bg-white hover:bg-red-50 text-xs font-semibold px-2.5 py-1.5 rounded-lg transition-colors cursor-pointer"
                          data-action-ban={account.id}
                        >
                          Ban
                        </button>
                      {:else}
                        <button
                          onclick={() => restoreAccount(account.id)}
                          class="text-emerald-700 border border-emerald-200 bg-white hover:bg-emerald-50 text-xs font-semibold px-2.5 py-1.5 rounded-lg transition-colors cursor-pointer"
                          data-action-restore={account.id}
                        >
                          Unban
                        </button>
                      {/if}
                    </div>
                  </td>
                </tr>
              {/each}
            </tbody>
          </table>
        </div>
      </div>
    </main>
  {/if}

  <!-- BottomNavBar (Mobile view) -->
  <nav class="md:hidden fixed bottom-0 left-0 w-full flex justify-around items-center h-16 px-4 bg-white border-t border-slate-200 shadow-md z-50">
    <button
      onclick={() => activeTab = 'Dashboard'}
      class="flex flex-col items-center justify-center scale-95 transition-transform duration-150 {activeTab === 'Dashboard' ? 'bg-indigo-50 text-[#3525cd] rounded-full px-4 py-1' : 'text-slate-500'}"
    >
      <span class="material-symbols-outlined">dashboard</span>
      <span class="text-[10px] font-semibold">Dashboard</span>
    </button>
    <button
      onclick={() => activeTab = 'Accounts'}
      class="flex flex-col items-center justify-center scale-95 transition-transform duration-150 {activeTab === 'Accounts' ? 'bg-indigo-50 text-[#3525cd] rounded-full px-4 py-1' : 'text-slate-500'}"
    >
      <span class="material-symbols-outlined">manage_accounts</span>
      <span class="text-[10px] font-semibold">Accounts</span>
    </button>
    <button
      onclick={() => activeTab = 'Sessions'}
      class="flex flex-col items-center justify-center scale-95 transition-transform duration-150 {activeTab === 'Sessions' ? 'bg-indigo-50 text-[#3525cd] rounded-full px-4 py-1' : 'text-slate-500'}"
    >
      <span class="material-symbols-outlined">monitoring</span>
      <span class="text-[10px] font-semibold">Sessions</span>
    </button>
    <button
      onclick={() => activeTab = 'Settings'}
      class="flex flex-col items-center justify-center scale-95 transition-transform duration-150 {activeTab === 'Settings' ? 'bg-indigo-50 text-[#3525cd] rounded-full px-4 py-1' : 'text-slate-500'}"
    >
      <span class="material-symbols-outlined">settings</span>
      <span class="text-[10px] font-semibold">Settings</span>
    </button>
  </nav>
</div>

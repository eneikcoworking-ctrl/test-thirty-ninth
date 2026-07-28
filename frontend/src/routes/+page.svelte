<script lang="ts">
  import { onMount } from 'svelte';

  // --- Campaign Launch State (from upstream main) ---
  let leadsFile = $state<File | null>(null);
  let isUploadingLeads = $state(false);
  let uploadProgressLeads = $state(0);

  let spintaxInput = $state('{Hi|Hello|Hey} {first_name}, I saw your {company} website and {loved|really liked} the {layout|design}.');
  let spintaxPreview = $state('');
  let deterministicSeed = $state(0.5);

  function parseSpintax(text: string, randomSource: () => number = Math.random): string {
    const pattern = /\{([^{}]+)\}/g;
    let hasMatches = true;
    let parsed = text;
    while (hasMatches) {
      const matches = parsed.match(pattern);
      if (!matches) {
        hasMatches = false;
        break;
      }
      for (const match of matches) {
        const options = match.substring(1, match.length - 1).split('|');
        const index = Math.floor(randomSource() * options.length);
        parsed = parsed.replace(match, options[index]);
      }
    }
    return parsed;
  }

  $effect(() => {
    // Pseudo-random generator for reproducible previews during testing
    let seed = deterministicSeed;
    function seededRandom() {
      let x = Math.sin(seed++) * 10000;
      return x - Math.floor(x);
    }
    spintaxPreview = parseSpintax(spintaxInput, seededRandom);
  });

  async function handleUploadSubmit(e: SubmitEvent) {
    e.preventDefault();
    if (!leadsFile) return;

    isUploadingLeads = true;
    uploadProgressLeads = 0;

    const interval = setInterval(() => {
      uploadProgressLeads += 10;
      if (uploadProgressLeads >= 100) {
        clearInterval(interval);
        setTimeout(() => {
          isUploadingLeads = false;
          uploadProgressLeads = 0;
          leadsFile = null;
          const inputElement = document.getElementById('file-upload') as HTMLInputElement;
          if (inputElement) inputElement.value = '';
        }, 1000);
      }
    }, 100);
  }

  function handleFileChange(e: Event) {
    const target = e.target as HTMLInputElement;
    if (target.files && target.files.length > 0) {
      leadsFile = target.files[0];
    } else {
      leadsFile = null;
    }
  }

  // --- Account & Session Management State (our feature) ---
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

  let isUploadingSession = $state(false);
  let uploadProgressSession = $state(0);
  let successBanner = $state('');
  let isProvisionModalOpen = $state(false);

  // Active view tab state
  let activeTab = $state<'Launch' | 'Dashboard' | 'Accounts'>('Dashboard');

  // Derived metrics
  let activeCount = $derived(accounts.filter(a => a.status === 'Active').length);
  let totalCount = $derived(accounts.length);

  // File onboarding simulation functions
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
    isUploadingSession = true;
    uploadProgressSession = 0;
    successBanner = '';

    const interval = setInterval(() => {
      uploadProgressSession += 10;
      if (uploadProgressSession >= 100) {
        clearInterval(interval);
        setTimeout(() => {
          isUploadingSession = false;
          isProvisionModalOpen = false;
          successBanner = `Successfully uploaded and onboarded session file: "${fileName}". Added to the active pool.`;

          // Onboard new session
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

  // Change account status immediately
  function banAccount(id: number) {
    accounts = accounts.map(a => {
      if (a.id === id) {
        return { ...a, status: 'Permanent Ban', warmupStage: 'Banned', latency: 'N/A', trustScore: 10 };
      }
      return a;
    });
  }

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

<div class="min-h-screen bg-surface text-on-surface flex flex-col pb-16 md:pb-0">
  <!-- Top App Bar with Navigation -->
  <header class="bg-surface border-b border-outline-variant px-4 py-3 flex items-center justify-between sticky top-0 z-40 h-16">
    <div class="flex items-center gap-3">
      <span class="material-symbols-outlined text-on-surface cursor-pointer" data-icon="menu">menu</span>
      <h1 class="text-title-lg font-title-lg text-primary m-0">Campaign Dashboard</h1>
    </div>

    <!-- Desktop Navigation Tabs -->
    <div class="hidden md:flex gap-6 items-center">
      <nav class="flex gap-6 items-center">
        <button
          onclick={() => activeTab = 'Launch'}
          class="text-label-sm font-bold tracking-wider pb-1 transition-colors border-b-2 {activeTab === 'Launch' ? 'text-primary border-primary' : 'text-on-surface-variant border-transparent hover:text-primary'}"
        >
          LAUNCH CAMPAIGN
        </button>
        <button
          onclick={() => activeTab = 'Dashboard'}
          class="text-label-sm font-bold tracking-wider pb-1 transition-colors border-b-2 {activeTab === 'Dashboard' ? 'text-primary border-primary' : 'text-on-surface-variant border-transparent hover:text-primary'}"
        >
          DASHBOARD
        </button>
        <button
          onclick={() => activeTab = 'Accounts'}
          class="text-label-sm font-bold tracking-wider pb-1 transition-colors border-b-2 {activeTab === 'Accounts' ? 'text-primary border-primary' : 'text-on-surface-variant border-transparent hover:text-primary'}"
        >
          ACCOUNTS ({totalCount})
        </button>
      </nav>
    </div>

    <div class="flex items-center gap-4">
      <span class="material-symbols-outlined text-on-surface-variant" data-icon="notifications">notifications</span>
      <div class="w-8 h-8 bg-surface-container-highest rounded-full flex items-center justify-center border border-outline-variant">
        <span class="material-symbols-outlined text-on-surface text-sm" data-icon="person">person</span>
      </div>
    </div>
  </header>

  {#if activeTab === 'Launch'}
    <!-- Launch Campaign Tab Layout (Original) -->
    <main class="max-w-7xl mx-auto p-4 lg:p-8 w-full flex-grow">
      <!-- Title & Subtitle -->
      <div class="mb-8">
        <h2 class="text-display-sm font-display-sm">Launch New Campaign</h2>
        <p class="text-body-lg text-on-surface-variant mt-2">Upload leads and configure your outreach message.</p>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-12 gap-6">
        <!-- Configuration Section (Left 7 Columns) -->
        <section class="lg:col-span-7 flex flex-col gap-6">
          <!-- Lead Upload Card -->
          <div class="bg-surface border border-outline-variant p-6 rounded shadow-sm relative overflow-hidden">
            <h3 class="text-title-md font-title-md mb-4 flex items-center gap-2">
              <span class="material-symbols-outlined text-primary" data-icon="upload_file">upload_file</span>
              Upload Leads
            </h3>
            <form onsubmit={handleUploadSubmit} class="flex flex-col gap-4">
              <div>
                <label for="file-upload" class="block text-label-md font-label-md text-on-surface-variant mb-1">CSV File</label>
                <input
                  id="file-upload"
                  type="file"
                  accept=".csv,.txt"
                  onchange={handleFileChange}
                  class="block w-full text-body-md text-on-surface border border-outline-variant rounded p-2 focus:border-primary focus:ring-1 focus:ring-primary outline-none file:mr-4 file:py-2 file:px-4 file:rounded file:border-0 file:text-sm file:font-semibold file:bg-primary-fixed file:text-primary hover:file:bg-primary/20"
                  disabled={isUploadingLeads}
                  aria-disabled={isUploadingLeads}
                />
              </div>
              <button
                type="submit"
                class="bg-primary text-surface px-4 py-2 rounded text-label-md font-bold uppercase tracking-wider self-start disabled:opacity-50 min-h-[44px] hover:bg-primary/90 focus:ring-2 focus:ring-primary focus:ring-offset-2 transition-colors cursor-pointer"
                disabled={!leadsFile || isUploadingLeads}
                aria-disabled={!leadsFile || isUploadingLeads}
              >
                {isUploadingLeads ? 'Ingesting...' : 'Import Leads'}
              </button>
            </form>

            <!-- Progress Bar (Visible during upload) -->
            {#if isUploadingLeads}
              <div class="mt-4" role="progressbar" aria-valuenow={uploadProgressLeads} aria-valuemin="0" aria-valuemax="100">
                <div class="flex justify-between text-label-sm text-on-surface-variant mb-1">
                  <span>Ingestion Status</span>
                  <span>{uploadProgressLeads}%</span>
                </div>
                <div class="w-full bg-surface-container-highest h-2 rounded-full overflow-hidden">
                  <div class="bg-primary h-full transition-all duration-300 ease-out" style="width: {uploadProgressLeads}%"></div>
                </div>
              </div>
            {/if}
          </div>

          <!-- Spintax Configuration Card -->
          <div class="bg-surface border border-outline-variant p-6 rounded shadow-sm">
            <h3 class="text-title-md font-title-md mb-4 flex items-center gap-2">
              <span class="material-symbols-outlined text-primary" data-icon="edit_document">edit_document</span>
              Message Template
            </h3>
            <div class="flex flex-col gap-4">
              <div>
                <label for="spintax-textarea" class="block text-label-md font-label-md text-on-surface-variant mb-1">Spintax Editor</label>
                <textarea
                  id="spintax-textarea"
                  bind:value={spintaxInput}
                  class="w-full min-h-[150px] p-3 border border-outline-variant rounded text-body-md bg-surface-container-lowest focus:border-primary focus:ring-1 focus:ring-primary outline-none font-mono"
                  placeholder="Enter your spintax message here... e.g. {Hi|Hello} {first_name}"
                ></textarea>
                <p class="text-body-sm text-on-surface-variant mt-2">Use {'{opt1|opt2}'} to create random variations.</p>
              </div>
            </div>
          </div>
        </section>

        <!-- Preview & Info Section (Right 5 Columns) -->
        <aside class="lg:col-span-5 flex flex-col gap-6">
          <!-- Spintax Preview Section -->
          <div class="bg-surface-container-low border border-outline-variant p-6 rounded shadow-sm min-h-[200px]">
            <div class="flex justify-between items-center mb-4">
              <h3 class="text-title-md font-title-md">Preview</h3>
              <button
                onclick={() => { deterministicSeed += 0.1; }}
                class="text-primary p-2 hover:bg-surface-container-highest rounded-full transition-colors flex items-center justify-center min-w-[44px] min-h-[44px] cursor-pointer"
                aria-label="Regenerate Preview"
              >
                <span class="material-symbols-outlined text-xl" data-icon="refresh">refresh</span>
              </button>
            </div>

            <div class="bg-surface border border-outline-variant p-4 rounded text-body-md text-on-surface h-full">
              {#if spintaxInput}
                <p class="whitespace-pre-wrap">{spintaxPreview}</p>
              {:else}
                <p class="text-on-surface-variant italic">Enter spintax to see preview...</p>
              {/if}
            </div>
          </div>

          <!-- Info Card -->
          <div class="bg-surface border border-outline-variant p-6 rounded shadow-sm">
            <h3 class="text-title-md font-title-md mb-2">Campaign Readiness</h3>
            <ul class="space-y-3 mt-4">
              <li class="flex items-center gap-3">
                <span class="material-symbols-outlined text-primary" data-icon="check_circle">check_circle</span>
                <span class="text-body-md text-on-surface">Proxies Assigned</span>
              </li>
              <li class="flex items-center gap-3">
                <span class="material-symbols-outlined text-primary" data-icon="check_circle">check_circle</span>
                <span class="text-body-md text-on-surface">Accounts Warmed Up</span>
              </li>
              <li class="flex items-center gap-3 opacity-50">
                <span class="material-symbols-outlined text-on-surface-variant" data-icon="radio_button_unchecked">radio_button_unchecked</span>
                <span class="text-body-md text-on-surface-variant">Leads Imported</span>
              </li>
            </ul>
          </div>
        </aside>
      </div>
    </main>
  {:else if activeTab === 'Dashboard'}
    <!-- Bento Metrics & System Summary Tab -->
    <main class="max-w-[1440px] mx-auto p-4 md:p-8 space-y-6 w-full flex-grow">
      <!-- Success Banner -->
      {#if successBanner}
        <div class="bg-emerald-50 text-emerald-900 p-4 rounded-xl flex items-center justify-between border border-emerald-200" role="alert" id="success-banner">
          <div class="flex items-center gap-4">
            <span class="material-symbols-outlined text-emerald-600">check_circle</span>
            <p class="text-body-md font-bold">{successBanner}</p>
          </div>
          <button onclick={dismissBanner} class="material-symbols-outlined hover:bg-emerald-100 p-1 rounded-full cursor-pointer" aria-label="Dismiss banner">
            close
          </button>
        </div>
      {/if}

      <section class="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h2 class="text-headline-md font-bold text-on-surface">Systems Overview</h2>
          <p class="text-body-md text-on-surface-variant">Real-time status of active outreach infrastructure and account onboarding.</p>
        </div>
        <div class="flex gap-2">
          <button
            onclick={() => isProvisionModalOpen = true}
            class="bg-primary text-surface text-label-md font-bold tracking-wider px-6 py-2.5 rounded hover:opacity-90 transition-opacity flex items-center gap-1 cursor-pointer"
            id="provision-account-btn"
          >
            <span class="material-symbols-outlined text-[18px]">add</span>
            PROVISION ACCOUNT
          </button>
          <button class="bg-surface text-on-surface border border-outline-variant text-label-md font-bold tracking-wider px-6 py-2.5 rounded hover:bg-surface-container-low transition-colors flex items-center gap-1 cursor-pointer">
            <span class="material-symbols-outlined text-[18px]">download</span>
            EXPORT REPORT
          </button>
        </div>
      </section>

      <!-- Bento Grid: Key Metrics -->
      <div class="grid grid-cols-1 md:grid-cols-12 gap-6">
        <!-- Active Sessions Card -->
        <div class="md:col-span-4 bg-surface border border-outline-variant rounded p-4 shadow-sm group">
          <div class="flex justify-between items-start mb-4">
            <div class="bg-primary-container text-on-primary-container p-2 rounded">
              <span class="material-symbols-outlined">monitoring</span>
            </div>
            <div class="flex items-center gap-1 text-emerald-600 text-xs font-semibold">
              <span class="material-symbols-outlined text-[14px]">trending_up</span>
              +12.4%
            </div>
          </div>
          <h3 class="text-label-sm font-bold text-on-surface-variant uppercase tracking-wider">Active Sessions</h3>
          <div class="flex items-end gap-2 mt-1">
            <span class="text-display-sm font-bold text-on-surface">1,284</span>
            <span class="text-body-sm text-on-surface-variant mb-1">Concurrent users</span>
          </div>
          <div class="mt-6 h-16 flex items-end gap-[4px]">
            <div class="flex-1 bg-primary/10 rounded-t-sm h-[40%]" title="Mon"></div>
            <div class="flex-1 bg-primary/10 rounded-t-sm h-[60%]" title="Tue"></div>
            <div class="flex-1 bg-primary/10 rounded-t-sm h-[45%]" title="Wed"></div>
            <div class="flex-1 bg-primary/10 rounded-t-sm h-[75%]" title="Thu"></div>
            <div class="flex-1 bg-primary/10 rounded-t-sm h-[90%]" title="Fri"></div>
            <div class="flex-1 bg-primary rounded-t-sm h-[100%]" title="Sat"></div>
            <div class="flex-1 bg-primary/10 rounded-t-sm h-[80%]" title="Sun"></div>
          </div>
        </div>

        <!-- Total Accounts Card -->
        <div class="md:col-span-4 bg-surface border border-outline-variant rounded p-4 shadow-sm">
          <div class="flex justify-between items-start mb-4">
            <div class="bg-surface-container-high text-on-surface p-2 rounded">
              <span class="material-symbols-outlined">manage_accounts</span>
            </div>
            <span class="px-2 py-1 bg-emerald-50 text-emerald-700 text-xs font-semibold rounded border border-emerald-100">System Healthy</span>
          </div>
          <h3 class="text-label-sm font-bold text-on-surface-variant uppercase tracking-wider">Accounts Pool</h3>
          <div class="flex items-end gap-2 mt-1">
            <span class="text-display-sm font-bold text-on-surface" id="accounts-count">{totalCount}</span>
            <span class="text-body-sm text-on-surface-variant mb-1">Total verified</span>
          </div>
          <div class="mt-6 flex items-center justify-between">
            <div class="flex -space-x-2">
              <div class="w-8 h-8 rounded-full border-2 border-surface bg-primary/10 text-primary flex items-center justify-center font-bold text-[10px]">A1</div>
              <div class="w-8 h-8 rounded-full border-2 border-surface bg-surface-container-high text-on-surface flex items-center justify-center font-bold text-[10px]">A2</div>
              <div class="w-8 h-8 rounded-full border-2 border-surface bg-surface-container-highest text-on-surface flex items-center justify-center font-bold text-[10px]">A3</div>
              <div class="flex items-center justify-center w-8 h-8 rounded-full border-2 border-surface bg-surface-container-low text-xs font-semibold text-on-surface-variant">+{totalCount}</div>
            </div>
            <button onclick={() => activeTab = 'Accounts'} class="text-primary text-label-md font-bold hover:underline cursor-pointer">Manage All</button>
          </div>
        </div>

        <!-- Node Health Status -->
        <div class="md:col-span-4 bg-surface-container-highest text-on-surface rounded p-4 shadow-sm relative overflow-hidden">
          <div class="relative z-10 flex flex-col h-full">
            <div class="flex justify-between items-start mb-4">
              <div class="bg-surface p-2 rounded border border-outline-variant">
                <span class="material-symbols-outlined">dns</span>
              </div>
              <div class="flex items-center gap-1">
                <div class="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></div>
                <span class="text-xs font-semibold text-emerald-600">LIVE</span>
              </div>
            </div>
            <h3 class="text-label-sm font-bold text-on-surface-variant uppercase tracking-wider">Node Distribution</h3>
            <div class="mt-1">
              <span class="text-display-sm font-bold">99.98%</span>
              <div class="w-full bg-outline-variant h-1 rounded mt-2 overflow-hidden">
                <div class="bg-emerald-500 h-full w-[99%]"></div>
              </div>
            </div>
            <div class="mt-auto pt-4 flex justify-between border-t border-outline-variant">
              <div class="text-center">
                <div class="text-body-sm font-mono font-bold">24ms</div>
                <div class="text-[10px] text-on-surface-variant">Latency</div>
              </div>
              <div class="text-center">
                <div class="text-body-sm font-mono font-bold">1.2GB/s</div>
                <div class="text-[10px] text-on-surface-variant">Throughput</div>
              </div>
              <div class="text-center">
                <div class="text-body-sm font-mono font-bold">82%</div>
                <div class="text-[10px] text-on-surface-variant">Load</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Quick Action Section -->
      <div class="bg-surface border border-outline-variant p-6 rounded shadow-sm text-center">
        <h3 class="text-title-md font-bold mb-2">Ready to expand your outreach pool?</h3>
        <p class="text-body-md text-on-surface-variant mb-4">Upload pre-authenticated session files to add accounts into the warm pool instantly.</p>
        <button
          onclick={() => isProvisionModalOpen = true}
          class="bg-primary text-surface text-label-md font-bold px-6 py-2.5 rounded hover:opacity-90 cursor-pointer"
        >
          ONBOARD .SESSION FILE
        </button>
      </div>
    </main>
  {:else if activeTab === 'Accounts'}
    <!-- Accounts List Tab (Full Screen Pool Table) -->
    <main class="max-w-[1440px] mx-auto p-4 md:p-8 space-y-6 w-full flex-grow">
      <!-- Success Banner inside Accounts -->
      {#if successBanner}
        <div class="bg-emerald-50 text-emerald-900 p-4 rounded-xl flex items-center justify-between border border-emerald-200" role="alert" id="success-banner">
          <div class="flex items-center gap-4">
            <span class="material-symbols-outlined text-emerald-600">check_circle</span>
            <p class="text-body-md font-bold">{successBanner}</p>
          </div>
          <button onclick={dismissBanner} class="material-symbols-outlined hover:bg-emerald-100 p-1 rounded-full cursor-pointer" aria-label="Dismiss banner">
            close
          </button>
        </div>
      {/if}

      <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h2 class="text-headline-md font-bold text-on-surface">Account Pool</h2>
          <p class="text-body-md text-on-surface-variant">Manage credentials, proxies, latency metrics, and real-time session health.</p>
        </div>
        <button
          onclick={() => isProvisionModalOpen = true}
          class="bg-primary text-surface text-label-md font-bold tracking-wider px-6 py-2.5 rounded hover:opacity-90 transition-opacity flex items-center gap-1 cursor-pointer self-start"
          id="provision-account-btn"
        >
          <span class="material-symbols-outlined text-[18px]">add</span>
          PROVISION ACCOUNT
        </button>
      </div>

      <!-- Accounts Table Card -->
      <div class="bg-surface border border-outline-variant rounded shadow-sm overflow-hidden">
        <div class="px-4 py-4 border-b border-outline-variant flex flex-col md:flex-row justify-between md:items-center bg-surface-container-low gap-4">
          <div>
            <h3 class="text-title-md font-bold text-on-surface">Accounts Pool & Session Health</h3>
            <p class="text-body-sm text-on-surface-variant">Monitor health status and assign proxies to prevent ban chaining.</p>
          </div>
          <div class="flex gap-2 items-center">
            <span class="text-label-sm font-bold text-on-surface-variant uppercase tracking-wider">Filter:</span>
            <select class="bg-surface text-on-surface border border-outline-variant rounded px-2 py-1 text-xs focus:outline-none focus:border-primary">
              <option>All Accounts ({totalCount})</option>
              <option>Active ({activeCount})</option>
            </select>
          </div>
        </div>

        <div class="overflow-x-auto">
          <table class="w-full text-left border-collapse">
            <thead>
              <tr class="bg-surface-container-low text-xs font-semibold text-on-surface-variant uppercase tracking-wider border-b border-outline-variant">
                <th class="px-6 py-3">Telegram Account</th>
                <th class="px-6 py-3">Assigned Proxy</th>
                <th class="px-6 py-3">Latency</th>
                <th class="px-6 py-3">Trust Score</th>
                <th class="px-6 py-3">Warm-up Stage</th>
                <th class="px-6 py-3">Status Badge</th>
                <th class="px-6 py-3 text-right">Interventions</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-outline-variant text-sm">
              {#each accounts as account (account.id)}
                <tr class="hover:bg-surface-container-lowest transition-colors" data-account-row={account.id}>
                  <td class="px-6 py-4">
                    <div class="flex items-center gap-3">
                      <div class="w-8 h-8 rounded-full bg-primary/10 text-primary flex items-center justify-center font-bold text-xs">
                        TG
                      </div>
                      <div class="flex flex-col">
                        <span class="font-bold text-on-surface">{account.phoneNumber}</span>
                        <span class="text-xs text-on-surface-variant">@{account.username}</span>
                      </div>
                    </div>
                  </td>
                  <td class="px-6 py-4">
                    <span class="text-xs font-mono bg-surface-container-high text-on-surface px-2.5 py-1 rounded border border-outline-variant">
                      {account.proxy}
                    </span>
                  </td>
                  <td class="px-6 py-4 font-mono text-xs text-on-surface-variant">
                    {account.latency}
                  </td>
                  <td class="px-6 py-4">
                    <div class="flex items-center gap-1">
                      <span class="font-bold {account.trustScore > 70 ? 'text-emerald-600' : account.trustScore > 40 ? 'text-primary' : 'text-red-600'}">
                        {account.trustScore}
                      </span>
                      <span class="text-xs text-on-surface-variant">/100</span>
                    </div>
                  </td>
                  <td class="px-6 py-4 text-on-surface-variant">
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
                      <span class="inline-flex items-center gap-1.5 bg-surface-container-high text-on-surface px-2.5 py-1 text-xs font-semibold rounded-full border border-outline-variant" data-status-badge="Other">
                        <span class="w-1.5 h-1.5 rounded-full bg-on-surface-variant"></span>
                        Re-auth Req.
                      </span>
                    {/if}
                  </td>
                  <td class="px-6 py-4 text-right">
                    <div class="flex justify-end gap-2">
                      {#if account.status !== 'Permanent Ban'}
                        <button
                          onclick={() => banAccount(account.id)}
                          class="text-red-600 border border-red-200 bg-surface hover:bg-red-50 text-xs font-semibold px-2.5 py-1.5 rounded transition-colors cursor-pointer"
                          data-action-ban={account.id}
                        >
                          Ban
                        </button>
                      {:else}
                        <button
                          onclick={() => restoreAccount(account.id)}
                          class="text-emerald-700 border border-emerald-200 bg-surface hover:bg-emerald-50 text-xs font-semibold px-2.5 py-1.5 rounded transition-colors cursor-pointer"
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

  <!-- Onboarding Modal / Drop Zone Dialog -->
  {#if isProvisionModalOpen}
    <div class="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4" role="dialog" aria-modal="true" aria-labelledby="modal-title">
      <div class="bg-surface border border-outline-variant rounded-xl max-w-lg w-full p-6 space-y-4 shadow-xl">
        <div class="flex justify-between items-center border-b border-outline-variant pb-3">
          <h3 class="text-title-md font-bold text-on-surface" id="modal-title">Onboard Telegram Session</h3>
          <button onclick={() => isProvisionModalOpen = false} class="material-symbols-outlined hover:bg-surface-container-high p-1 rounded-full cursor-pointer" aria-label="Close dialog">
            close
          </button>
        </div>

        <div class="space-y-2">
          <p class="text-body-md text-on-surface-variant">
            Upload pre-authenticated <code class="bg-surface-container-high px-1.5 py-0.5 rounded text-xs text-primary font-mono">.session</code> files to add the warmed account to your operational pool instantly.
          </p>
        </div>

        <!-- Drag and Drop Area -->
        <div
          ondragover={simulateDragOver}
          ondrop={simulateDrop}
          class="border-2 border-dashed border-outline-variant hover:border-primary rounded-xl p-8 flex flex-col items-center justify-center space-y-4 bg-surface-container-lowest transition-colors cursor-pointer text-center"
          role="region"
          aria-label="File upload dropzone"
        >
          <span class="material-symbols-outlined text-primary text-[48px]">cloud_upload</span>
          <div class="space-y-1">
            <p class="text-body-md font-bold text-on-surface">Drag & drop your <code class="bg-surface-container-high px-1.5 py-0.5 rounded text-xs font-mono">.session</code> file here</p>
            <p class="text-body-sm text-on-surface-variant">or browse files from your computer</p>
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
            class="bg-surface text-primary border border-outline-variant text-label-md font-bold px-4 py-2 rounded hover:bg-surface-container-low transition-colors cursor-pointer"
          >
            Browse Files
          </button>
        </div>

        <!-- Uploading State Indicator -->
        {#if isUploadingSession}
          <div class="space-y-2" id="upload-progress-container">
            <div class="flex justify-between items-center text-label-sm font-bold text-on-surface-variant">
              <span class="flex items-center gap-1">
                <span class="material-symbols-outlined text-[16px] animate-spin">sync</span>
                Uploading & verifying auth credentials...
              </span>
              <span>{uploadProgressSession}%</span>
            </div>
            <div class="w-full bg-surface-container-high h-2 rounded-full overflow-hidden">
              <div class="bg-primary h-full transition-all duration-150" style="width: {uploadProgressSession}%" id="progress-bar-fill"></div>
            </div>
          </div>
        {/if}

        <div class="flex justify-end gap-2 pt-3 border-t border-outline-variant">
          <button
            onclick={() => isProvisionModalOpen = false}
            class="bg-surface text-on-surface border border-outline-variant text-label-md font-bold px-4 py-2 rounded hover:bg-surface-container-low transition-colors cursor-pointer"
          >
            Cancel
          </button>
        </div>
      </div>
    </div>
  {/if}

  <!-- Bottom Navigation Bar (Mobile) -->
  <nav class="md:hidden fixed bottom-0 left-0 w-full z-50 flex justify-around items-center bg-surface border-t border-outline-variant py-2 px-4 pb-safe shadow-md">
    <button onclick={() => activeTab = 'Launch'} class="flex flex-col items-center justify-center transition-transform duration-150 active:scale-90 min-w-[48px] min-h-[48px] {activeTab === 'Launch' ? 'text-primary font-bold' : 'text-on-surface-variant'}" href="/">
      <span class="material-symbols-outlined" data-icon="rocket_launch">rocket_launch</span>
      <span class="text-[10px] font-bold">Launch</span>
    </button>
    <button onclick={() => activeTab = 'Dashboard'} class="flex flex-col items-center justify-center transition-transform duration-150 active:scale-90 px-2 py-1 rounded min-w-[48px] min-h-[48px] {activeTab === 'Dashboard' ? 'text-primary font-bold' : 'text-on-surface-variant'}" href="/">
      <span class="material-symbols-outlined" data-icon="dashboard">dashboard</span>
      <span class="text-[10px] font-bold">Dashboard</span>
    </button>
    <button onclick={() => activeTab = 'Accounts'} class="flex flex-col items-center justify-center transition-transform duration-150 active:scale-90 px-2 py-1 rounded min-w-[48px] min-h-[48px] {activeTab === 'Accounts' ? 'text-primary font-bold' : 'text-on-surface-variant'}" href="/">
      <span class="material-symbols-outlined" data-icon="group">group</span>
      <span class="text-[10px] font-bold">Accounts</span>
    </button>
  </nav>
</div>

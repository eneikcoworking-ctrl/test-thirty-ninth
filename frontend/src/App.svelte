<script>
  import { onMount } from 'svelte';

  // Svelte 5 state runes for dynamic accounts
  let accounts = $state([
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
  let activeTab = $state('Dashboard');

  // Stats calculation
  let activeCount = $derived(accounts.filter(a => a.status === 'Active').length);
  let totalCount = $derived(accounts.length);

  // Simulated upload function
  function handleFileSelected(event) {
    const file = event.target.files[0];
    if (!file) return;
    simulateUpload(file.name);
  }

  function simulateDragOver(event) {
    event.preventDefault();
  }

  function simulateDrop(event) {
    event.preventDefault();
    const file = event.dataTransfer?.files[0];
    if (file && file.name.endsWith('.session')) {
      simulateUpload(file.name);
    } else {
      alert("Please upload a valid .session file.");
    }
  }

  function simulateUpload(fileName) {
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
          const newAccount = {
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
  function banAccount(id) {
    accounts = accounts.map(a => {
      if (a.id === id) {
        return { ...a, status: 'Permanent Ban', warmupStage: 'Banned', latency: 'N/A', trustScore: 10 };
      }
      return a;
    });
  }

  // Restore an account
  function restoreAccount(id) {
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

<div class="min-h-screen bg-slate-50 text-slate-900 font-sans">
  <!-- TopAppBar -->
  <header class="w-full top-0 sticky bg-white border-b border-slate-200 z-50 transition-colors duration-200">
    <div class="flex items-center justify-between px-4 py-3 w-full max-w-full">
      <div class="flex items-center gap-4">
        <button class="material-symbols-outlined text-slate-500 hover:bg-slate-100 transition-colors p-2 rounded-full cursor-pointer" aria-label="Menu">
          menu
        </button>
        <h1 class="text-xl md:text-2xl font-bold text-[#3525cd]">AdminCenter</h1>
      </div>
      <div class="flex items-center gap-2">
        <button class="material-symbols-outlined text-[#3525cd] p-2 hover:bg-slate-100 rounded-full transition-colors cursor-pointer" aria-label="Search">
          search
        </button>
        <div class="hidden md:flex gap-4 ml-6">
          <nav class="flex gap-6 items-center">
            <button
              onclick={() => activeTab = 'Dashboard'}
              class="text-xs font-semibold tracking-wider pb-1 transition-colors border-b-2 {activeTab === 'Dashboard' ? 'text-[#3525cd] border-[#3525cd]' : 'text-slate-500 border-transparent hover:text-[#3525cd]'}"
            >
              DASHBOARD
            </button>
            <button
              onclick={() => activeTab = 'Accounts'}
              class="text-xs font-semibold tracking-wider pb-1 transition-colors border-b-2 {activeTab === 'Accounts' ? 'text-[#3525cd] border-[#3525cd]' : 'text-slate-500 border-transparent hover:text-[#3525cd]'}"
            >
              ACCOUNTS ({totalCount})
            </button>
            <button
              onclick={() => activeTab = 'Sessions'}
              class="text-xs font-semibold tracking-wider pb-1 transition-colors border-b-2 {activeTab === 'Sessions' ? 'text-[#3525cd] border-[#3525cd]' : 'text-slate-500 border-transparent hover:text-[#3525cd]'}"
            >
              SESSIONS
            </button>
            <button
              onclick={() => activeTab = 'Settings'}
              class="text-xs font-semibold tracking-wider pb-1 transition-colors border-b-2 {activeTab === 'Settings' ? 'text-[#3525cd] border-[#3525cd]' : 'text-slate-500 border-transparent hover:text-[#3525cd]'}"
            >
              SETTINGS
            </button>
          </nav>
        </div>
      </div>
    </div>
  </header>

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

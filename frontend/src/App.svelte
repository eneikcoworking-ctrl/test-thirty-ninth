<script lang="ts">
  import SpintaxEditor from './lib/SpintaxEditor.svelte';
  import LeadUpload from './lib/LeadUpload.svelte';

  // Svelte 5 State
  let activeCampaigns = $state(12);
  let totalLeads = $state(8400);
  let recentUploadsCount = $state(0);
  let latestTemplateText = $state('');

  // Launch campaign state
  let isLaunching = $state(false);
  let isLaunched = $state(false);
  let launchSuccessMessage = $state('');

  // Handle uploaded leads
  function handleLeadUpload(filename: string, count: number) {
    totalLeads += count;
    recentUploadsCount += count;
  }

  // Handle template edit updates
  function handleTemplateChange(text: string) {
    latestTemplateText = text;
  }

  // Format leads count for display (e.g., 8400 -> 8.4k)
  let formattedLeads = $derived.by(() => {
    if (totalLeads >= 1000) {
      return (totalLeads / 1000).toFixed(1).replace(/\.0$/, '') + 'k';
    }
    return totalLeads.toString();
  });

  // Launch campaign handler
  function launchCampaign() {
    isLaunching = true;

    setTimeout(() => {
      isLaunching = false;
      isLaunched = true;
      activeCampaigns += 1;
      launchSuccessMessage = `Campaign launched successfully with ${recentUploadsCount || 'all'} leads! Outreach starting in background...`;

      // Auto-dismiss success message after 5 seconds
      setTimeout(() => {
        isLaunched = false;
        launchSuccessMessage = '';
      }, 5000);
    }, 1500);
  }
</script>

<div class="min-h-screen bg-background text-on-surface pb-24 md:pb-8 flex flex-col font-sans">
  <!-- Top App Bar -->
  <header class="bg-background border-b border-outline-variant flex justify-between items-center px-margin-mobile md:px-margin-desktop w-full py-4 sticky top-0 z-40">
    <div class="flex items-center gap-2">
      <span class="material-symbols-outlined text-primary text-headline-md">rocket_launch</span>
      <h1 class="font-headline-md text-headline-md font-bold text-primary">SvelteCampaign</h1>
    </div>
    <div class="w-10 h-10 rounded-full bg-surface-container-highest flex items-center justify-center overflow-hidden border border-outline-variant">
      <img
        class="w-full h-full object-cover"
        alt="User profile avatar"
        src="https://lh3.googleusercontent.com/aida-public/AB6AXuDtAe9W0_4EKhPlcHk1PzFIioDA5ntralZGsCnwRSui_i9Xjdk_QclfpjRr2Ijm-tPgNzZABnscKC05d5w2n9JHRhpXKEezRylxPcMcR3gj8dMGe1YpvIqqDU46XRYXqLAPDIteQx4pZ4HKx2xfAJNAPbXkCrxD77HgjX7ZiAgd4Qn1AYfoyAXh5sdoO1GwkaCw_UQOQJZaFZKY0UrMX2qdErYLveFmubbOyGItCjhdPS2aXEa43LDY_pmIprheJwaqcFFoxsVRYtM"
      />
    </div>
  </header>

  <!-- Main Container -->
  <main class="px-margin-mobile py-6 max-w-lg mx-auto w-full flex-grow space-y-6">

    <!-- Success Toast Notifications (Polite ARIA live region) -->
    <div aria-live="polite" class="empty:hidden">
      {#if launchSuccessMessage}
        <div class="p-4 bg-secondary-container text-on-secondary-container rounded-xl border border-outline-variant shadow-md flex items-start gap-3 animate-fade-in" id="toast-success">
          <span class="material-symbols-outlined text-headline-sm">check_circle</span>
          <p class="font-body-md text-sm">{launchSuccessMessage}</p>
        </div>
      {/if}
    </div>

    <!-- Dashboard Summary Bento Section -->
    <section class="grid grid-cols-2 gap-4" aria-label="Dashboard Overview">
      <div class="bento-card p-4 rounded-xl flex flex-col justify-between aspect-square">
        <div>
          <span class="material-symbols-outlined text-primary mb-2">dashboard</span>
          <p class="font-label-md text-label-md text-on-surface-variant uppercase tracking-wider">Active</p>
        </div>
        <h2 class="font-display-lg text-display-lg text-primary leading-none" id="active-campaigns-stat">{activeCampaigns}</h2>
        <p class="font-body-md text-body-md text-on-surface-variant">Campaigns</p>
      </div>
      <div class="bento-card p-4 rounded-xl flex flex-col justify-between aspect-square">
        <div>
          <span class="material-symbols-outlined text-secondary mb-2">groups</span>
          <p class="font-label-md text-label-md text-on-surface-variant uppercase tracking-wider">Leads</p>
        </div>
        <h2 class="font-display-lg text-display-lg text-on-surface leading-none" id="total-leads-stat">{formattedLeads}</h2>
        <p class="font-body-md text-body-md text-on-surface-variant">Total Sync</p>
      </div>
    </section>

    <!-- Lead Upload Area -->
    <section aria-label="Lead Ingestion">
      <LeadUpload onUploadComplete={handleLeadUpload} />
    </section>

    <!-- Spintax Editor and Preview Section -->
    <section aria-label="Message Template Configuration">
      <SpintaxEditor onTemplateChange={handleTemplateChange} />
    </section>

    <!-- Launch Campaign Primary Action -->
    <section class="mt-8 mb-4">
      <button
        id="launch-campaign-btn"
        disabled={isLaunching}
        onclick={launchCampaign}
        class="w-full bg-primary-container text-on-primary-container py-4 rounded-xl font-headline-sm text-headline-sm shadow-lg active:scale-95 transition-all flex items-center justify-center gap-3 disabled:opacity-50 disabled:cursor-not-allowed focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2"
        aria-busy={isLaunching}
      >
        {#if isLaunching}
          <span class="material-symbols-outlined animate-spin">sync</span>
          Launching Campaign...
        {:else}
          <span class="material-symbols-outlined">rocket</span>
          Launch Campaign
        {/if}
      </button>

      <p class="text-center font-label-md text-label-md text-on-surface-variant mt-4 opacity-70">
        Ready to reach <span class="font-bold text-primary" id="ready-leads-count">{recentUploadsCount || 'all'}</span> newly ingested leads.
      </p>
    </section>
  </main>

  <!-- Bottom Navigation Bar (Mobile) -->
  <nav class="fixed bottom-0 w-full z-50 flex justify-around items-center px-4 py-2 bg-surface border-t border-outline-variant md:hidden rounded-t-xl shadow-2xl">
    <div class="flex flex-col items-center justify-center bg-primary-container text-on-primary-container rounded-full px-4 py-1 active:scale-90 duration-150 cursor-pointer">
      <span class="material-symbols-outlined" style="font-variation-settings: 'FILL' 1;">dashboard</span>
      <span class="font-label-md text-label-md">Campaigns</span>
    </div>
    <div class="flex flex-col items-center justify-center text-on-surface-variant hover:bg-surface-container-highest transition-colors px-4 py-1 rounded-full cursor-pointer">
      <span class="material-symbols-outlined">upload_file</span>
      <span class="font-label-md text-label-md">Leads</span>
    </div>
    <div class="flex flex-col items-center justify-center text-on-surface-variant hover:bg-surface-container-highest transition-colors px-4 py-1 rounded-full cursor-pointer">
      <span class="material-symbols-outlined">rebase_edit</span>
      <span class="font-label-md text-label-md">Spintax</span>
    </div>
    <div class="flex flex-col items-center justify-center text-on-surface-variant hover:bg-surface-container-highest transition-colors px-4 py-1 rounded-full cursor-pointer">
      <span class="material-symbols-outlined">settings</span>
      <span class="font-label-md text-label-md">Settings</span>
    </div>
  </nav>
</div>

<style>
  @keyframes fadeIn {
    from { opacity: 0; transform: translateY(-10px); }
    to { opacity: 1; transform: translateY(0); }
  }
  .animate-fade-in {
    animation: fadeIn 0.3s ease-out forwards;
  }
</style>

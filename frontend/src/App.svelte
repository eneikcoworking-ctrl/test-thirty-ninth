<script>
  import { onMount } from 'svelte';
  import PromptEditor from './lib/PromptEditor.svelte';
  import StopTriggers from './lib/StopTriggers.svelte';

  let systemPrompt = "";
  let stopTriggers = [];
  let intentRules = [];
  let strictAlignment = true;
  let loading = true;
  let saveStatus = ""; // For visual feedback "Changes saved successfully!"

  // Default fallback system prompt
  const DEFAULT_PROMPT = `You are a highly analytical AI core developed for Synthetic Logic.
Your tone is clinical, precise, and devoid of unnecessary emotive flourishes.
Prioritize technical accuracy above all else.

Constraint 01: If a query is ambiguous, ask for technical specifications.
Constraint 02: Do not use bullet points for lists shorter than 3 items.
Constraint 03: Use ISO 8601 for all date references.`;

  onMount(async () => {
    try {
      const res = await fetch('/api/v1/ai-config');
      if (res.ok) {
        const data = await res.json();
        systemPrompt = data.systemPrompt || "";
        stopTriggers = data.stopTriggers || [];
        intentRules = data.intentRules || [];
      }
    } catch (e) {
      console.error("Failed to fetch initial AI configuration:", e);
    } finally {
      loading = false;
    }
  });

  // Overwrites full config on backend
  async function saveFullConfig(updatedPrompt, updatedTriggers, updatedRules) {
    try {
      const res = await fetch('/api/v1/ai-config', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          systemPrompt: updatedPrompt,
          stopTriggers: updatedTriggers,
          intentRules: updatedRules
        })
      });
      if (res.ok) {
        const data = await res.json();
        systemPrompt = data.systemPrompt || "";
        stopTriggers = data.stopTriggers || [];
        intentRules = data.intentRules || [];
        showSaveFeedback("Configuration updated!");
      }
    } catch (e) {
      console.error("Failed to save configuration:", e);
      showSaveFeedback("Error saving changes.");
    }
  }

  function showSaveFeedback(msg) {
    saveStatus = msg;
    setTimeout(() => {
      saveStatus = "";
    }, 3000);
  }

  // Handle system prompt save
  async function handleSavePrompt(newPrompt) {
    await saveFullConfig(newPrompt, stopTriggers, intentRules);
  }

  // Handle system prompt reset to default
  async function handleResetPrompt() {
    systemPrompt = DEFAULT_PROMPT;
    await saveFullConfig(DEFAULT_PROMPT, stopTriggers, intentRules);
  }

  // Add stop trigger instantly to the backend
  async function handleAddTrigger(word) {
    if (!stopTriggers.includes(word)) {
      const updatedTriggers = [...stopTriggers, word];
      // Update local state first for instant reaction
      stopTriggers = updatedTriggers;
      await saveFullConfig(systemPrompt, updatedTriggers, intentRules);
    }
  }

  // Delete stop trigger instantly from the backend
  async function handleDeleteTrigger(word) {
    const updatedTriggers = stopTriggers.filter(t => t !== word);
    stopTriggers = updatedTriggers;
    await saveFullConfig(systemPrompt, updatedTriggers, intentRules);
  }

  // Toggle single intent rule status
  async function handleToggleRule(ruleId) {
    const updatedRules = intentRules.map(rule => {
      if (rule.id === ruleId) {
        return { ...rule, enabled: !rule.enabled };
      }
      return rule;
    });
    intentRules = updatedRules;
    await saveFullConfig(systemPrompt, stopTriggers, updatedRules);
  }
</script>

<!-- TopAppBar -->
<header class="flex items-center justify-between px-margin py-xs w-full z-50 bg-background border-b border-outline-variant sticky top-0">
  <button class="active:opacity-70 transition-opacity p-xs focus:outline-none" aria-label="Go back">
    <span class="material-symbols-outlined text-primary">arrow_back</span>
  </button>
  <h1 class="font-headline-sm text-headline-sm font-bold text-on-background">Persona Configuration</h1>
  <button class="active:opacity-70 transition-opacity p-xs focus:outline-none" aria-label="Settings">
    <span class="material-symbols-outlined text-primary">settings</span>
  </button>
</header>

<main class="max-w-[800px] mx-auto p-margin space-y-xl">
  <!-- Header Info -->
  <section class="space-y-xs">
    <h2 class="font-headline-md text-headline-md text-primary">Architect Settings</h2>
    <p class="text-on-surface-variant font-body-md">Modify the underlying logic structures that define how the AI persona interacts with system nodes and end-users.</p>
  </section>

  {#if loading}
    <div class="flex items-center justify-center p-xl">
      <span class="text-primary font-mono-label animate-pulse">Loading AI core settings...</span>
    </div>
  {:else}
    <!-- Save Status Toast -->
    {#if saveStatus}
      <div class="fixed top-[70px] right-margin bg-primary-container border border-primary text-on-primary-container px-md py-sm rounded shadow-lg z-50 font-semibold animate-fade-in text-xs transition-all">
        {saveStatus}
      </div>
    {/if}

    <!-- System Prompt Section -->
    <section class="space-y-md">
      <PromptEditor
        bind:systemPrompt
        onSave={handleSavePrompt}
        onReset={handleResetPrompt}
      />
    </section>

    <!-- Stop Triggers Section -->
    <section class="space-y-md">
      <StopTriggers
        {stopTriggers}
        onAdd={handleAddTrigger}
        onDelete={handleDeleteTrigger}
      />
    </section>

    <!-- Structured Intent Rules Section -->
    <section class="space-y-md">
      <div>
        <h3 class="font-label-caps text-label-caps text-secondary-fixed-dim uppercase tracking-wider">Intent Rules</h3>
        <p class="text-[11px] text-outline mt-1">Structured JSON objects specifying classification intents and triggered behaviors.</p>
      </div>
      <div class="space-y-sm">
        {#each intentRules as rule}
          <div class="flex items-center justify-between p-md bg-surface-container border border-outline-variant rounded-lg">
            <div class="space-y-xs">
              <div class="flex items-center gap-sm">
                <span class="font-bold text-on-surface font-mono-label text-xs">{rule.intentName}</span>
                <span class="bg-secondary-container text-on-secondary-container text-[10px] px-2 py-0.5 rounded font-bold">{rule.action}</span>
              </div>
              <p class="text-[11px] text-outline">Keywords: {rule.keywords ? rule.keywords.join(', ') : ''}</p>
            </div>
            <button
              type="button"
              on:click={() => handleToggleRule(rule.id)}
              class="w-12 h-6 rounded-full relative p-1 flex items-center transition-colors focus:outline-none {rule.enabled ? 'bg-primary' : 'bg-secondary-container'}"
              aria-label="Toggle rule {rule.intentName}"
            >
              <div class="w-4 h-4 bg-background rounded-full transition-transform {rule.enabled ? 'translate-x-6' : 'translate-x-0'}"></div>
            </button>
          </div>
        {/each}
      </div>
    </section>

    <!-- Security Profile Section -->
    <section class="space-y-md pb-lg">
      <div>
        <h3 class="font-label-caps text-label-caps text-secondary-fixed-dim uppercase tracking-wider">Security Profile</h3>
        <p class="text-[11px] text-outline mt-1">Review the active safety filters and compliance guards.</p>
      </div>
      <div class="flex items-center justify-between p-md bg-surface-container border border-outline-variant rounded-lg">
        <div class="flex items-center gap-md">
          <div class="w-10 h-10 rounded-lg bg-surface-variant flex items-center justify-center">
            <span class="material-symbols-outlined text-primary">shield</span>
          </div>
          <div>
            <p class="font-bold text-on-surface">Strict Alignment</p>
            <p class="text-[12px] text-outline">Compliance Level v4.2</p>
          </div>
        </div>
        <button
          type="button"
          on:click={() => strictAlignment = !strictAlignment}
          class="w-12 h-6 rounded-full relative p-1 flex items-center transition-colors focus:outline-none {strictAlignment ? 'bg-primary' : 'bg-secondary-container'}"
          aria-label="Toggle Strict Alignment"
        >
          <div class="w-4 h-4 bg-background rounded-full transition-transform {strictAlignment ? 'translate-x-6' : 'translate-x-0'}"></div>
        </button>
      </div>
    </section>
  {/if}
</main>

<!-- BottomNavBar -->
<nav class="fixed bottom-0 left-0 w-full z-50 flex justify-around items-center px-sm py-xs bg-surface-container-low border-t border-outline-variant">
  <button class="flex flex-col items-center justify-center bg-secondary-container text-on-secondary-container rounded-full px-sm py-xs active:scale-95 transition-transform duration-150 focus:outline-none">
    <span class="material-symbols-outlined">terminal</span>
    <span class="font-label-caps text-label-caps">Prompts</span>
  </button>
  <button class="flex flex-col items-center justify-center text-on-surface-variant px-sm py-xs hover:text-primary active:scale-95 transition-transform duration-150 focus:outline-none">
    <span class="material-symbols-outlined">maps_ar</span>
    <span class="font-label-caps text-label-caps">Triggers</span>
  </button>
  <button class="flex flex-col items-center justify-center text-on-surface-variant px-sm py-xs hover:text-primary active:scale-95 transition-transform duration-150 focus:outline-none">
    <span class="material-symbols-outlined">psychology</span>
    <span class="font-label-caps text-label-caps">Models</span>
  </button>
  <button class="flex flex-col items-center justify-center text-on-surface-variant px-sm py-xs hover:text-primary active:scale-95 transition-transform duration-150 focus:outline-none">
    <span class="material-symbols-outlined">history</span>
    <span class="font-label-caps text-label-caps">History</span>
  </button>
</nav>

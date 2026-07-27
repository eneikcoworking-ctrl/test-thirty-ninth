<script lang="ts">
  import { onMount } from 'svelte';

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

  // State
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

  // On mount, load configuration from backend
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

  // Save full configuration
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

  // Reset to default settings
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
</script>

<div class="min-h-screen bg-[#0A0A0B] text-[#e3e2e7] flex flex-col">
  <!-- TopAppBar -->
  <header class="fixed top-0 left-0 w-full z-50 flex items-center justify-between px-4 md:px-8 h-16 bg-[#121317]/80 backdrop-blur-xl border-b border-white/10" aria-label="Main Header">
    <div class="flex items-center gap-4">
      <button
        class="flex items-center justify-center p-2 rounded-full hover:bg-white/5 active:scale-95 transition-transform text-[#adc6ff] focus:outline-none focus-visible:ring-2 focus-visible:ring-[#adc6ff]"
        aria-label="Go back"
      >
        <span class="material-symbols-outlined">arrow_back</span>
      </button>
      <h1 class="text-lg md:text-xl font-semibold text-white">AI Settings</h1>
    </div>
    <div class="flex items-center gap-2">
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
        class="flex items-center justify-center p-2 rounded-full hover:bg-white/5 active:scale-95 transition-transform text-[#adc6ff] focus:outline-none focus-visible:ring-2 focus-visible:ring-[#adc6ff]"
        aria-label="More settings"
      >
        <span class="material-symbols-outlined">more_vert</span>
      </button>
    </div>
  </header>

  <main class="pt-24 pb-48 px-4 md:px-8 max-w-5xl mx-auto w-full space-y-8 flex-grow">
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
  </main>

  <!-- Footer Action Bar -->
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

  <!-- BottomNavBar -->
  <nav class="fixed bottom-0 w-full z-50 flex justify-around items-center px-4 py-2 pb-safe bg-[#121317]/80 backdrop-blur-xl border-t border-white/10" aria-label="Bottom Navigation">
    <button class="flex flex-col items-center justify-center text-gray-400 hover:text-[#adc6ff] transition-all active:scale-90 p-1 rounded focus:outline-none focus-visible:text-[#adc6ff]">
      <span class="material-symbols-outlined">psychology</span>
      <span class="font-mono text-[10px] tracking-wider uppercase mt-0.5">Prompts</span>
    </button>
    <button class="flex flex-col items-center justify-center text-[#00285c] bg-[#adc6ff] rounded-full px-4 py-1 active:scale-90 transition-all focus:outline-none focus-visible:ring-2 focus-visible:ring-white">
      <span class="material-symbols-outlined" style="font-variation-settings: 'FILL' 1;">temp_preferences_custom</span>
      <span class="font-mono text-[10px] tracking-wider uppercase mt-0.5">Models</span>
    </button>
    <button class="flex flex-col items-center justify-center text-gray-400 hover:text-[#adc6ff] transition-all active:scale-90 p-1 rounded focus:outline-none focus-visible:text-[#adc6ff]">
      <span class="material-symbols-outlined">dangerous</span>
      <span class="font-mono text-[10px] tracking-wider uppercase mt-0.5">Triggers</span>
    </button>
    <button class="flex flex-col items-center justify-center text-gray-400 hover:text-[#adc6ff] transition-all active:scale-90 p-1 rounded focus:outline-none focus-visible:text-[#adc6ff]">
      <span class="material-symbols-outlined">terminal</span>
      <span class="font-mono text-[10px] tracking-wider uppercase mt-0.5">Logs</span>
    </button>
  </nav>
</div>

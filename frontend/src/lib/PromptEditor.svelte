<script>
  export let systemPrompt = "";
  export let onSave = () => {};
  export let onReset = () => {};

  let textareaEl;
  let highlightEl;

  const placeholderText = "Type system prompt here... Use {placeholder_name} for variables.";

  // Perform custom syntax highlighting
  function highlight(text) {
    if (!text) return "";

    // Escape HTML to prevent injection and formatting breakage
    let escaped = text
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;");

    // Highlight placeholders enclosed in curly braces {name}, {username} etc.
    escaped = escaped.replace(/\{([^}]+)\}/g, (match, p1) => {
      if (p1.includes('|')) {
        // Spintax template options
        return `<span class="text-secondary-fixed bg-secondary-container/80 border border-secondary-fixed/40 rounded px-1 font-semibold">${match}</span>`;
      } else {
        // Dynamic variable placeholders
        return `<span class="text-primary bg-primary-container/20 border border-primary/30 rounded px-1 font-semibold">${match}</span>`;
      }
    });

    // Highlight constraints "Constraint XX:"
    escaped = escaped.replace(/(Constraint \d+:)/gi, '<span class="text-secondary font-bold">$1</span>');

    // Add extra space if ends with newline so scrolling stays perfectly synced
    if (escaped.endsWith('\n')) {
      escaped += ' ';
    }

    return escaped;
  }

  // Synchronize scrolling between textarea and highlight overlay
  function handleScroll() {
    if (textareaEl && highlightEl) {
      highlightEl.scrollTop = textareaEl.scrollTop;
      highlightEl.scrollLeft = textareaEl.scrollLeft;
    }
  }

  // Auto-resize textarea height as content changes
  function autoResize() {
    if (textareaEl) {
      textareaEl.style.height = 'auto';
      textareaEl.style.height = textareaEl.scrollHeight + 'px';
    }
  }

  $: highlightedHtml = highlight(systemPrompt);

  $: {
    if (systemPrompt && textareaEl) {
      // Allow DOM to update then resize
      setTimeout(autoResize, 10);
    }
  }
</script>

<div class="space-y-md">
  <div class="flex items-center justify-between">
    <div>
      <h3 class="font-label-caps text-label-caps text-secondary-fixed-dim uppercase tracking-wider">System Prompt</h3>
      <p class="text-[11px] text-outline mt-1">Defines the core identity and behavioral constraints.</p>
    </div>
    <button
      type="button"
      on:click={onReset}
      class="text-primary font-mono-label text-mono-label active:opacity-60 transition-opacity flex items-center gap-xs focus:outline-none"
    >
      <span class="material-symbols-outlined text-[16px]">restart_alt</span>
      Reset to Default
    </button>
  </div>

  <div class="bg-surface-container border border-outline-variant rounded-lg p-xs focus-glow transition-all duration-200 relative">
    <!-- Overlay for Syntax Highlighting -->
    <div
      bind:this={highlightEl}
      class="absolute inset-0 p-sm font-mono-label text-mono-label text-on-surface min-h-[240px] overflow-auto whitespace-pre-wrap break-words pointer-events-none border border-transparent box-border leading-[1.6]"
      aria-hidden="true"
    >
      {@html highlightedHtml}
    </div>

    <!-- Real Textarea (Transparent text, visible caret) -->
    <textarea
      bind:this={textareaEl}
      bind:value={systemPrompt}
      on:scroll={handleScroll}
      on:input={autoResize}
      class="w-full bg-transparent border-none focus:ring-0 font-mono-label text-mono-label text-transparent caret-on-surface min-h-[240px] resize-y p-sm relative z-10 box-border leading-[1.6]"
      spellcheck="false"
      placeholder={placeholderText}
    ></textarea>

    <div class="flex justify-between items-center px-sm py-xs border-t border-outline-variant/30 mt-xs relative z-20">
      <span class="text-[10px] font-mono-label text-outline">CHARS: {systemPrompt?.length || 0} / 8000</span>
      <div class="flex items-center gap-sm">
        <span class="material-symbols-outlined text-outline text-[18px]">info</span>
      </div>
    </div>
  </div>

  <button
    type="button"
    on:click={() => onSave(systemPrompt)}
    class="w-full bg-primary text-on-primary py-md rounded-lg font-bold hover:brightness-110 active:scale-[0.98] transition-all flex items-center justify-center gap-sm"
  >
    <span class="material-symbols-outlined">save</span>
    Save Changes
  </button>
</div>

<style>
  /* Aligning textarea and highlighting precisely */
  textarea, div[aria-hidden="true"] {
    font-family: 'JetBrains Mono', ui-monospace, monospace;
    font-size: 13px;
    line-height: 1.6;
    letter-spacing: 0px;
    word-break: break-word;
  }
</style>

<script>
  export let stopTriggers = [];
  export let onAdd = () => {};
  export let onDelete = () => {};

  let newTriggerWord = "";

  function handleAdd() {
    if (newTriggerWord && newTriggerWord.trim()) {
      onAdd(newTriggerWord.trim());
      newTriggerWord = "";
    }
  }

  function handleKeyDown(event) {
    if (event.key === 'Enter') {
      event.preventDefault();
      handleAdd();
    }
  }
</script>

<div class="space-y-md">
  <div>
    <h3 class="font-label-caps text-label-caps text-secondary-fixed-dim uppercase tracking-wider">Stop Triggers</h3>
    <p class="text-[11px] text-outline mt-1">Generation will immediately halt if any of these technical strings are detected in the output buffer.</p>
  </div>

  <div class="bg-surface-container border border-outline-variant rounded-lg p-md space-y-md">
    <div class="flex flex-wrap gap-xs">
      {#each stopTriggers as trigger}
        <div class="flex items-center gap-xs bg-surface-variant text-on-surface-variant px-sm py-xs rounded-full font-mono-label text-mono-label border border-outline-variant/40">
          <span>{trigger}</span>
          <button
            type="button"
            on:click={() => onDelete(trigger)}
            class="hover:text-error transition-colors flex items-center justify-center focus:outline-none"
            aria-label="Delete trigger {trigger}"
          >
            <span class="material-symbols-outlined text-[14px]">close</span>
          </button>
        </div>
      {/each}
      {#if stopTriggers.length === 0}
        <span class="text-outline font-body-md text-xs italic">No stop triggers configured.</span>
      {/if}
    </div>

    <div class="relative flex items-center">
      <input
        bind:value={newTriggerWord}
        on:keydown={handleKeyDown}
        class="w-full bg-surface-container-low border border-outline-variant rounded-lg px-md py-sm font-mono-label text-mono-label focus:ring-1 focus:ring-primary focus:border-primary outline-none transition-all pr-[50px] text-on-surface"
        placeholder="Add new trigger keyword..."
        type="text"
      />
      <button
        type="button"
        on:click={handleAdd}
        class="absolute right-sm text-primary hover:text-primary-fixed transition-colors flex items-center justify-center focus:outline-none"
        aria-label="Add trigger"
      >
        <span class="material-symbols-outlined">add_circle</span>
      </button>
    </div>
  </div>
</div>

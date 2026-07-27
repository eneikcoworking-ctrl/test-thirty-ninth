<script lang="ts">
  // Props
  interface Props {
    onUploadComplete?: (filename: string, leadCount: number) => void;
  }
  let { onUploadComplete }: Props = $props();

  // Local state
  let fileInput: HTMLInputElement | null = $state(null);
  let isDragging = $state(false);
  let uploadState = $state<'idle' | 'ingesting' | 'completed' | 'error'>('idle');
  let progress = $state(0);
  let statusText = $state('');
  let uploadedFileName = $state('');
  let parsedLeadCount = $state(0);

  // Drag and drop handlers
  function handleDragOver(e: DragEvent) {
    e.preventDefault();
    isDragging = true;
  }

  function handleDragLeave() {
    isDragging = false;
  }

  function handleDrop(e: DragEvent) {
    e.preventDefault();
    isDragging = false;
    if (e.dataTransfer && e.dataTransfer.files.length > 0) {
      processFile(e.dataTransfer.files[0]);
    }
  }

  function handleFileSelect(e: Event) {
    const target = e.target as HTMLInputElement;
    if (target.files && target.files.length > 0) {
      processFile(target.files[0]);
    }
  }

  function processFile(file: File) {
    uploadedFileName = file.name;
    uploadState = 'ingesting';
    progress = 0;
    statusText = 'Validating file...';

    // Parse the file to get a realistic lead count
    const reader = new FileReader();
    reader.onload = (e) => {
      const text = e.target?.result as string;
      if (text) {
        // Simple CSV line parser
        const lines = text.split(/\r?\n/).filter(line => line.trim().length > 0);
        if (lines.length > 1) {
          parsedLeadCount = lines.length - 1; // Subtract header
        } else {
          parsedLeadCount = lines.length > 0 ? lines.length : 42; // default mock fallback
        }
      } else {
        parsedLeadCount = 150; // Mock count fallback
      }
      startIngestionSimulation();
    };
    reader.onerror = () => {
      parsedLeadCount = 150; // Mock count fallback
      startIngestionSimulation();
    };
    reader.readAsText(file);
  }

  function startIngestionSimulation() {
    let currentProgress = 0;
    const interval = setInterval(() => {
      if (currentProgress < 100) {
        currentProgress += 4;
        if (currentProgress > 100) currentProgress = 100;
        progress = currentProgress;

        // Transition through different ingestion phases based on progress
        if (currentProgress <= 25) {
          statusText = 'Validating CSV format & headers...';
        } else if (currentProgress <= 55) {
          statusText = `Uploading ${parsedLeadCount} leads to platform...`;
        } else if (currentProgress <= 85) {
          statusText = 'Parsing lead metadata & deduplicating...';
        } else {
          statusText = 'Finished!';
        }
      } else {
        clearInterval(interval);
        uploadState = 'completed';
        if (onUploadComplete) {
          onUploadComplete(uploadedFileName, parsedLeadCount);
        }
      }
    }, 80); // ~2.0 seconds total ingestion animation duration
  }

  function triggerFileInput() {
    fileInput?.click();
  }

  function resetUpload() {
    uploadState = 'idle';
    progress = 0;
    statusText = '';
    uploadedFileName = '';
    parsedLeadCount = 0;
    if (fileInput) {
      fileInput.value = '';
    }
  }
</script>

<div class="space-y-4">
  {#if uploadState === 'idle'}
    <!-- Drag & Drop Zone -->
    <div
      role="button"
      tabindex="0"
      onclick={triggerFileInput}
      onkeydown={(e) => { if (e.key === 'Enter' || e.key === ' ') { triggerFileInput(); } }}
      ondragover={handleDragOver}
      ondragleave={handleDragLeave}
      ondrop={handleDrop}
      class="bento-card p-6 rounded-xl border-dashed border-2 bg-surface-container-low group cursor-pointer text-center relative overflow-hidden focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2 transition-all
        {isDragging ? 'border-primary bg-surface-container' : 'border-outline-variant hover:border-primary'}"
      aria-label="Upload leads. Drag and drop CSV or click to browse"
    >
      <input
        bind:this={fileInput}
        type="file"
        accept=".csv,.txt"
        class="hidden"
        onchange={handleFileSelect}
        aria-hidden="true"
      />
      <div class="flex flex-col items-center gap-3">
        <div class="w-12 h-12 rounded-full bg-primary-container text-on-primary-container flex items-center justify-center group-hover:scale-110 transition-transform duration-200">
          <span class="material-symbols-outlined text-headline-md">upload_file</span>
        </div>
        <div>
          <h3 class="font-headline-sm text-headline-sm text-on-surface">Upload Leads</h3>
          <p class="font-body-md text-body-md text-on-surface-variant">Drag and drop CSV files or click to browse</p>
        </div>
      </div>
    </div>
  {:else if uploadState === 'ingesting'}
    <!-- Ingestion Progress Panel -->
    <div class="bento-card p-6 rounded-xl space-y-4" aria-live="polite">
      <div class="flex items-center gap-3">
        <div class="w-10 h-10 rounded-full bg-primary-container text-on-primary-container flex items-center justify-center animate-spin">
          <span class="material-symbols-outlined text-lg">sync</span>
        </div>
        <div class="flex-1 min-w-0">
          <h4 class="font-headline-sm text-base font-semibold text-on-surface truncate" id="upload-filename">
            {uploadedFileName}
          </h4>
          <p class="font-body-md text-sm text-on-surface-variant truncate" id="ingestion-phase-text">
            {statusText}
          </p>
        </div>
        <span class="font-headline-sm text-lg font-bold text-primary" id="ingestion-percentage">
          {progress}%
        </span>
      </div>

      <!-- Custom Progress Bar -->
      <div class="w-full bg-surface-container-highest h-3 rounded-full overflow-hidden" role="progressbar" aria-valuenow={progress} aria-valuemin="0" aria-valuemax="100">
        <div
          class="bg-primary h-full rounded-full transition-all duration-75"
          style="width: {progress}%"
        ></div>
      </div>
    </div>
  {:else if uploadState === 'completed'}
    <!-- Ingestion Complete Panel -->
    <div class="bento-card p-6 rounded-xl space-y-4" aria-live="polite">
      <div class="flex items-center gap-3">
        <div class="w-12 h-12 rounded-full bg-secondary-container text-on-secondary-container flex items-center justify-center">
          <span class="material-symbols-outlined text-headline-md">check_circle</span>
        </div>
        <div class="flex-1 min-w-0">
          <h4 class="font-headline-sm text-base font-semibold text-on-surface truncate">
            Ingestion Complete!
          </h4>
          <p class="font-body-md text-sm text-on-surface-variant">
            Successfully imported <span class="font-bold text-primary">{parsedLeadCount}</span> leads from <span class="italic">{uploadedFileName}</span>.
          </p>
        </div>
      </div>

      <div class="flex justify-end gap-3 pt-2">
        <button
          type="button"
          onclick={resetUpload}
          class="px-4 py-2 bg-surface-container border border-outline-variant rounded-lg text-body-md text-on-surface hover:bg-surface-container-high transition-colors focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2"
        >
          Upload Another File
        </button>
      </div>
    </div>
  {/if}
</div>

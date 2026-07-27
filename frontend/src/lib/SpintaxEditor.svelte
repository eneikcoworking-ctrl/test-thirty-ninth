<script lang="ts">
  import { parseSpintax, calculatePermutations, generateVariation } from './spintax';

  // Props
  interface Props {
    templateText: string;
    onTemplateChange: (text: string) => void;
  }
  let { templateText = 'Hello {first_name|there},\n\nI noticed you\'re interested in {industry|SaaS}. We should {connect|chat|hop on a call} next {Tuesday|Wednesday} to discuss your goals.', onTemplateChange }: Props = $props();

  // Handle local state
  let text = $state(templateText);
  let parsedBlocks = $derived(parseSpintax(text));
  let variablesCount = $derived(parsedBlocks.length);
  let permutationsCount = $derived(calculatePermutations(parsedBlocks));

  // State for the generated preview variation
  let variationPreview = $state('');

  // Update variation on text change
  $effect(() => {
    variationPreview = generateVariation(text);
    if (onTemplateChange) {
      onTemplateChange(text);
    }
  });

  function randomizeVariation() {
    variationPreview = generateVariation(text);
  }

  // Generate HTML for the template preview with highlighted tokens
  let highlightedTemplateHtml = $derived.by(() => {
    // Escape HTML first to prevent XSS
    let escaped = text
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');

    // Format newlines
    escaped = escaped.replace(/\n/g, '<br/>');

    // Replace {a|b} blocks with spans
    const regex = /\{([^{}]+)\}/g;
    return escaped.replace(regex, (match) => {
      return `<span class="spintax-token">${match}</span>`;
    });
  });
</script>

<div class="space-y-6">
  <!-- Campaign Template Input Form -->
  <div class="bento-card p-5 rounded-xl">
    <div class="flex items-center gap-2 mb-3">
      <span class="material-symbols-outlined text-primary">edit_note</span>
      <h3 class="font-headline-sm text-headline-sm text-on-surface">Outreach Message Template</h3>
    </div>

    <label for="template-textarea" class="sr-only">Campaign message template with spintax</label>
    <textarea
      id="template-textarea"
      bind:value={text}
      rows="5"
      class="w-full bg-surface-container-low border border-outline-variant rounded-lg p-3 text-body-md text-on-surface focus:outline-none focus:border-primary transition-colors resize-y"
      placeholder="Type your message. Use curly braces to add Spintax options, e.g. &#123;Hi|Hello&#125; &#123;first_name|there&#125;."
    ></textarea>

    <p class="text-xs text-on-surface-variant mt-2">
      Tip: Spintax syntax uses <code class="text-xs">&#123;option1|option2|...&#125;</code> to randomize outbound messages.
    </p>
  </div>

  <!-- Real-time Spintax Parsing & Preview -->
  <div class="bento-card p-5 rounded-xl">
    <div class="flex justify-between items-center mb-4">
      <div class="flex items-center gap-2">
        <span class="material-symbols-outlined text-primary">rebase_edit</span>
        <h3 class="font-headline-sm text-headline-sm text-on-surface">Template Structure</h3>
      </div>
      <div class="flex gap-2">
        <span class="bg-surface-container text-on-surface-variant font-label-md text-label-md px-2 py-1 rounded" id="variables-count">
          Variables: {variablesCount}
        </span>
        <span class="bg-surface-container text-on-surface-variant font-label-md text-label-md px-2 py-1 rounded" id="permutations-count">
          Permutations: {permutationsCount}
        </span>
      </div>
    </div>

    <!-- Token Highlighting View -->
    <div class="bg-surface-container-lowest p-4 rounded-lg border border-outline-variant min-h-[80px]">
      <p class="font-body-md text-body-md text-on-surface leading-relaxed whitespace-pre-wrap">
        {@html highlightedTemplateHtml}
      </p>
    </div>
  </div>

  <!-- Resolved Variation Preview -->
  <div class="bento-card p-5 rounded-xl">
    <div class="flex justify-between items-center mb-4">
      <div class="flex items-center gap-2">
        <span class="material-symbols-outlined text-primary">preview</span>
        <h3 class="font-headline-sm text-headline-sm text-on-surface">Random Variation Preview</h3>
      </div>
      <button
        type="button"
        onclick={randomizeVariation}
        class="text-primary font-label-md text-label-md uppercase font-bold hover:underline flex items-center gap-1 focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2 rounded"
        aria-label="Generate new preview variation"
      >
        <span class="material-symbols-outlined text-sm">autorenew</span>
        Randomize
      </button>
    </div>

    <div class="bg-surface-container-low p-4 rounded-lg border border-outline-variant min-h-[80px] flex flex-col justify-between">
      <p class="font-body-md text-body-md text-on-surface leading-relaxed whitespace-pre-wrap" id="variation-preview-text">
        {variationPreview || 'No template text entered yet.'}
      </p>
    </div>
  </div>
</div>

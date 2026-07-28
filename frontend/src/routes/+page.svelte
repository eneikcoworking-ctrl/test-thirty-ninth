<script lang="ts">
	import '../app.css';

	let leadsFile = $state<File | null>(null);
	let isUploading = $state(false);
	let uploadProgress = $state(0);

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
        function seededRandom() {
            let x = Math.sin(deterministicSeed++) * 10000;
            return x - Math.floor(x);
        }
        spintaxPreview = parseSpintax(spintaxInput, seededRandom);
    });

	async function handleUploadSubmit(e: SubmitEvent) {
		e.preventDefault();
		if (!leadsFile) return;

		isUploading = true;
		uploadProgress = 0;

		// Simulate file ingestion progress
		const interval = setInterval(() => {
			uploadProgress += 10;
			if (uploadProgress >= 100) {
				clearInterval(interval);
				setTimeout(() => {
					isUploading = false;
					uploadProgress = 0;
					leadsFile = null;
                    const inputElement = document.getElementById('file-upload') as HTMLInputElement;
                    if(inputElement) inputElement.value = '';
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

    // Escape literal curly braces in Svelte 5 by defining them as variables in the script
    const spintaxPlaceholderText = "Enter your spintax message here... e.g. {Hi|Hello} {first_name}";
</script>

<div class="flex h-screen bg-surface text-on-surface overflow-hidden">
	<!-- Desktop Sidebar Navigation (Shared Component styled drawer) -->
	<aside class="hidden md:flex flex-col w-64 bg-surface border-r border-outline-variant py-4 flex-shrink-0">
		<div class="px-6 mb-8 flex items-center gap-3">
			<div class="w-10 h-10 rounded-full bg-primary-fixed flex items-center justify-center overflow-hidden">
				<img class="w-full h-full object-cover" alt="Operator avatar" src="https://lh3.googleusercontent.com/aida-public/AB6AXuAwaobbsOuq48AqHDkU8ILErpzsiQokaMwmZsXoxwOm2xCrQGaJlfVjgqGdpZfV3S1PWxp9GxIPiHh-SZLPpv8wyXXSX3p19Zm7QbwZrIFEe_reyOMxk-BekFID7GQ01ncXgrfTo_M0K8pnEkFeorclkl26uYVtheQWOdwtzd9QwLZgOFR7L2lMYvo9EUu_NZ4LjA4d0iRHRhKKnqDSHbvvFEOKOcUcHsJASEl3JWmsnuGKc9fQfnQacXcdP_F16JAlTwWy4-_vC9Fl" />
			</div>
			<div class="flex flex-col overflow-hidden">
				<span class="text-sm font-semibold text-primary truncate">Operator #402</span>
				<span class="text-xs text-green-600 font-medium flex items-center gap-1">
					<span class="w-1.5 h-1.5 bg-green-500 rounded-full"></span>
					Online
				</span>
			</div>
		</div>

		<nav class="flex flex-col gap-1 px-2">
			<a href="/" class="flex items-center gap-3 px-4 py-3 bg-primary-fixed text-primary rounded-full transition-colors focus-visible:ring-2 focus-visible:ring-primary outline-none">
				<span class="material-symbols-outlined">rocket_launch</span>
				<span class="text-sm font-semibold">Campaigns</span>
			</a>
			<a href="/inbox" class="flex items-center gap-3 px-4 py-3 text-on-surface-variant hover:bg-surface-container-low rounded-full transition-colors focus-visible:ring-2 focus-visible:ring-primary outline-none">
				<span class="material-symbols-outlined">inbox</span>
				<span class="text-sm font-medium">Unified Inbox</span>
			</a>
		</nav>
	</aside>

    <!-- Main Scrollable Area -->
    <div class="flex-1 flex flex-col min-h-screen overflow-y-auto pb-24 md:pb-8">
        <!-- Top App Bar -->
        <header class="bg-surface border-b border-outline-variant px-4 py-3 flex items-center justify-between sticky top-0 z-40 h-16 flex-shrink-0">
            <div class="flex items-center gap-3">
                <span class="material-symbols-outlined text-on-surface" data-icon="menu">menu</span>
                <h1 class="text-title-lg font-title-lg text-on-surface m-0">Campaign Dashboard</h1>
            </div>
            <div class="flex items-center gap-4">
                <span class="material-symbols-outlined text-on-surface-variant" data-icon="notifications">notifications</span>
                <div class="w-8 h-8 bg-surface-container-highest rounded-full flex items-center justify-center border border-outline-variant">
                    <span class="material-symbols-outlined text-on-surface text-sm" data-icon="person">person</span>
                </div>
            </div>
        </header>

        <main class="max-w-7xl w-full mx-auto p-4 lg:p-8 flex-1">
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
                                    disabled={isUploading}
                                    aria-disabled={isUploading}
                                />
                            </div>
                            <button
                                type="submit"
                                class="bg-primary text-surface px-4 py-2 rounded text-label-md font-bold uppercase tracking-wider self-start disabled:opacity-50 min-h-[44px] hover:bg-primary/90 focus:ring-2 focus:ring-primary focus:ring-offset-2 transition-colors"
                                disabled={!leadsFile || isUploading}
                                aria-disabled={!leadsFile || isUploading}
                            >
                                {isUploading ? 'Ingesting...' : 'Import Leads'}
                            </button>
                        </form>

                        <!-- Progress Bar (Visible during upload) -->
                        {#if isUploading}
                        <div class="mt-4" role="progressbar" aria-valuenow={uploadProgress} aria-valuemin="0" aria-valuemax="100">
                            <div class="flex justify-between text-label-sm text-on-surface-variant mb-1">
                                <span>Ingestion Status</span>
                                <span>{uploadProgress}%</span>
                            </div>
                            <div class="w-full bg-surface-container-highest h-2 rounded-full overflow-hidden">
                                <div class="bg-primary h-full transition-all duration-300 ease-out" style="width: {uploadProgress}%"></div>
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
                                    placeholder={spintaxPlaceholderText}
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
                                class="text-primary p-2 hover:bg-surface-container-highest rounded-full transition-colors flex items-center justify-center min-w-[44px] min-h-[44px]"
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
    </div>

    <!-- Bottom Navigation Bar (Mobile) -->
    <nav class="md:hidden fixed bottom-0 left-0 w-full z-50 flex justify-around items-center bg-surface border-t border-outline-variant py-2 px-4 pb-safe flex-shrink-0">
        <a class="flex flex-col items-center justify-center text-primary font-bold transition-transform duration-150 active:scale-90 min-w-[48px] min-h-[48px]" href="/">
            <span class="material-symbols-outlined" data-icon="rocket_launch">rocket_launch</span>
            <span class="text-label-sm font-label-md">Launch</span>
        </a>
        <a class="flex flex-col items-center justify-center text-on-surface-variant hover:bg-surface-container-low transition-transform duration-150 active:scale-90 px-2 py-1 rounded min-w-[48px] min-h-[48px]" href="/inbox">
            <span class="material-symbols-outlined" data-icon="inbox">inbox</span>
            <span class="text-label-sm font-label-md">Inbox</span>
        </a>
    </nav>
</div>

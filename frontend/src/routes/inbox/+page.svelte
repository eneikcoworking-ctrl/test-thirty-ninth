<script lang="ts">
	import '../../app.css';

	// Mock conversations state in Svelte 5 style
	let conversations = $state([
		{
			id: 1,
			name: 'John Doe',
			avatar: 'https://lh3.googleusercontent.com/aida-public/AB6AXuCSWf2zWfZ4wNvVwrZ_O4FW8IfHfTg5stSTtxYHYYpB3bpldZ8q-z4OjyXeD2UKSn1OH3DFCQ1BPB0W3ezrkXaGuxwn4puhV_UdsB-mFnhToVQF9pqnpkEzOof8SPKzt-O_f7OG9Fpy-p_Ia0P97jqVkuUkVzwjKpfhjLcbAjqj5uEr39POLwZ0eFmtagI7GoGwH2E_VynbvRtP-gHmA-rEUWpt6r9-tWq879c6Mo1IUSS0Pyq10iD2oHIkCst9BmbSdz2PJk8bm7X0',
			humanInterventionRequired: false,
			aiPaused: false,
			messages: [
				{ sender: 'user', text: "Hello, I'm having trouble with my subscription renewal. It seems to have failed three times today.", time: '14:20' },
				{ sender: 'ai', text: "I'm sorry to hear that, John. I can see the failed attempts in your billing history. Usually, this happens due to a block from the issuing bank or an expired card. Would you like me to check the specific error code?", time: '14:21' },
				{ sender: 'user', text: "Yes please. The card isn't expired, I used it elsewhere this morning.", time: '14:22' }
			]
		},
		{
			id: 2,
			name: 'Sarah Connor',
			avatar: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150',
			humanInterventionRequired: true,
			aiPaused: false,
			messages: [
				{ sender: 'user', text: "Is there anyone available to talk about my campaign setup?", time: '10:05' },
				{ sender: 'ai', text: "Hi Sarah! Yes, I can help you with your campaign setup. What platform are you trying to integrate?", time: '10:06' },
				{ sender: 'user', text: "I need a human operator. The custom SOCKS5 proxy isn't binding.", time: '10:07' }
			]
		},
		{
			id: 3,
			name: 'Alex Mercer',
			avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150',
			humanInterventionRequired: false,
			aiPaused: true,
			messages: [
				{ sender: 'user', text: "Can I upgrade my plan to include unlimited warmed up sessions?", time: '09:12' },
				{ sender: 'operator', text: "Hi Alex! Sure, I have paused the AI assistant for you. Our Enterprise tier offers up to 50 warm-up slots. Let me send you the pricing page link.", time: '09:15' }
			]
		}
	]);

	// Svelte 5 state and derived values
	let selectedId = $state(1);
	let showMobileChat = $state(false);
	let operatorMessageText = $state('');

	let activeConversation = $derived(conversations.find(c => c.id === selectedId) || conversations[0]);

	// Dispatch operator message and pause the AI
	function handleSendMessage(e: Event) {
		e.preventDefault();
		if (!operatorMessageText.trim()) return;

		const now = new Date();
		const timeString = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

		// Add operator message to the active conversation
		activeConversation.messages = [
			...activeConversation.messages,
			{
				sender: 'operator',
				text: operatorMessageText,
				time: timeString
			}
		];

		// Sending a message instantly pauses the AI
		activeConversation.aiPaused = true;

		// Clear input
		operatorMessageText = '';
	}

	// Toggle Human Intervention Required state
	function toggleHumanIntervention(convId: number) {
		const conv = conversations.find(c => c.id === convId);
		if (conv) {
			conv.humanInterventionRequired = !conv.humanInterventionRequired;
		}
	}

	// Toggle AI Paused state manually
	function toggleAiPaused(convId: number) {
		const conv = conversations.find(c => c.id === convId);
		if (conv) {
			conv.aiPaused = !conv.aiPaused;
		}
	}

	// Handle selection
	function selectConversation(id: number) {
		selectedId = id;
		showMobileChat = true;
	}

	// Svelte 5 - To prevent placeholder literal curly brace interpretation, define in script variable
	const placeholderText = "Type a message to pause the AI and intervene...";
</script>

<div class="flex h-screen bg-surface-container-lowest text-on-surface overflow-hidden">
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
			<a href="/" class="flex items-center gap-3 px-4 py-3 text-on-surface-variant hover:bg-surface-container-low rounded-full transition-colors focus-visible:ring-2 focus-visible:ring-primary outline-none">
				<span class="material-symbols-outlined">rocket_launch</span>
				<span class="text-sm font-medium">Campaigns</span>
			</a>
			<a href="/inbox" class="flex items-center gap-3 px-4 py-3 bg-primary-fixed text-primary rounded-full transition-colors focus-visible:ring-2 focus-visible:ring-primary outline-none">
				<span class="material-symbols-outlined">inbox</span>
				<span class="text-sm font-semibold">Unified Inbox</span>
			</a>
		</nav>
	</aside>

	<!-- Main Inbox Layout -->
	<main class="flex-1 flex flex-col md:flex-row overflow-hidden relative">
		<!-- Left Panel: Conversation List -->
		<section class="w-full md:w-80 flex flex-col bg-surface border-r border-outline-variant flex-shrink-0 {showMobileChat ? 'hidden md:flex' : 'flex'}" aria-label="Conversation List">
			<header class="p-4 border-b border-outline-variant flex justify-between items-center bg-surface-container-lowest">
				<h1 class="text-lg font-bold text-on-surface">Unified Inbox</h1>
				<span class="text-xs bg-primary-fixed text-primary font-semibold px-2 py-0.5 rounded-full">
					{conversations.length} chats
				</span>
			</header>

			<!-- Scrollable Conversation List -->
			<div class="flex-1 overflow-y-auto divide-y divide-outline-variant/50">
				{#each conversations as conv (conv.id)}
					<div
						role="button"
						tabindex="0"
						onclick={() => selectConversation(conv.id)}
						onkeydown={(e) => {
							if (e.key === 'Enter' || e.key === ' ') {
								e.preventDefault();
								selectConversation(conv.id);
							}
						}}
						class="w-full p-4 flex gap-3 text-left relative transition-colors hover:bg-surface-container-low focus-visible:ring-2 focus-visible:ring-primary outline-none cursor-pointer {selectedId === conv.id ? 'bg-surface-container-low border-l-4 border-primary' : 'border-l-4 border-transparent'}"
						aria-label="Chat with {conv.name}"
					>
						<!-- Indicator Strip (Changes to Yellow/Amber if Human Intervention Required) -->
						{#if conv.humanInterventionRequired}
							<div class="absolute {selectedId === conv.id ? 'left-1' : 'left-0'} top-0 bottom-0 w-1 bg-yellow-500" title="Human Intervention Required"></div>
						{/if}

						<!-- Avatar with dynamic status indicator -->
						<div class="relative flex-shrink-0">
							<img class="w-11 h-11 rounded-full object-cover border border-outline-variant" src={conv.avatar} alt="{conv.name}'s portrait" />
							{#if conv.humanInterventionRequired}
								<!-- Golden Yellow glowing badge -->
								<span class="absolute bottom-0 right-0 w-3.5 h-3.5 bg-yellow-500 border-2 border-surface rounded-full shadow-sm" title="Human Intervention Required"></span>
							{:else}
								<span class="absolute bottom-0 right-0 w-3.5 h-3.5 bg-green-500 border-2 border-surface rounded-full shadow-sm" title="AI Active"></span>
							{/if}
						</div>

						<div class="flex-1 min-w-0">
							<div class="flex justify-between items-baseline mb-0.5">
								<h2 class="text-sm font-semibold text-on-surface truncate">{conv.name}</h2>
								<span class="text-xs text-on-surface-variant">
									{conv.messages[conv.messages.length - 1]?.time || ''}
								</span>
							</div>
							<p class="text-xs text-on-surface-variant truncate">
								{conv.messages[conv.messages.length - 1]?.text || ''}
							</p>

							<!-- Status Badges and Quick Toggle inside List Item -->
							<div class="flex items-center justify-between mt-2 flex-wrap gap-1.5">
								<div class="flex gap-1.5 flex-wrap">
									{#if conv.humanInterventionRequired}
										<span class="bg-yellow-100 text-yellow-800 text-[10px] font-bold px-1.5 py-0.5 rounded uppercase tracking-wider">
											Needs Operator
										</span>
									{/if}
									{#if conv.aiPaused}
										<span class="bg-gray-100 text-gray-700 text-[10px] font-bold px-1.5 py-0.5 rounded uppercase tracking-wider">
											AI Paused
										</span>
									{:else}
										<span class="bg-green-50 text-green-700 text-[10px] font-bold px-1.5 py-0.5 rounded uppercase tracking-wider">
											AI Assistant
										</span>
									{/if}
								</div>

								<button
									onclick={(e) => {
										e.stopPropagation();
										toggleHumanIntervention(conv.id);
									}}
									class="text-[10px] font-bold text-primary hover:underline px-1 py-0.5 rounded bg-surface-container-high transition-colors focus-visible:ring-1 focus-visible:ring-primary outline-none"
									aria-label="Toggle Intervention Required for {conv.name}"
								>
									Flag Help
								</button>
							</div>
						</div>
					</div>
				{/each}
			</div>
		</section>

		<!-- Right Panel: Active Chat Pane -->
		<section class="flex-1 flex flex-col bg-surface-container-lowest {showMobileChat ? 'flex' : 'hidden md:flex'}" aria-label="Active Chat Panel">
			<!-- Chat Header Card -->
			<header class="p-4 border-b border-outline-variant bg-surface flex items-center justify-between shadow-sm z-10 flex-shrink-0">
				<div class="flex items-center gap-3">
					<!-- Mobile Back Button -->
					<button
						onclick={() => showMobileChat = false}
						class="md:hidden p-2 rounded-full hover:bg-surface-container-low transition-colors text-primary min-w-[44px] min-h-[44px]"
						aria-label="Back to chat list"
					>
						<span class="material-symbols-outlined">arrow_back</span>
					</button>

					<div class="relative w-11 h-11">
						<img class="w-full h-full rounded-full object-cover border border-outline-variant" src={activeConversation.avatar} alt="{activeConversation.name}'s profile" />
						<span class="absolute bottom-0 right-0 w-3 h-3 border-2 border-surface rounded-full {activeConversation.humanInterventionRequired ? 'bg-yellow-500' : 'bg-green-500'}"></span>
					</div>

					<div>
						<h1 class="text-base font-bold text-on-surface leading-tight">{activeConversation.name}</h1>
						<div class="flex items-center gap-1.5 mt-0.5 flex-wrap">
							<!-- Reactive status indicators -->
							{#if activeConversation.humanInterventionRequired}
								<span class="bg-yellow-100 text-yellow-800 text-[10px] font-bold px-1.5 py-0.5 rounded uppercase">
									Awaiting Operator
								</span>
							{/if}
							{#if activeConversation.aiPaused}
								<span class="bg-red-50 text-red-700 text-[10px] font-bold px-1.5 py-0.5 rounded flex items-center gap-1">
									<span class="w-1.5 h-1.5 bg-red-500 rounded-full"></span>
									AI Dialogue Paused
								</span>
							{:else}
								<span class="bg-green-50 text-green-700 text-[10px] font-bold px-1.5 py-0.5 rounded flex items-center gap-1">
									<span class="w-1.5 h-1.5 bg-green-500 rounded-full animate-pulse"></span>
									AI Dialogue Active
								</span>
							{/if}
						</div>
					</div>
				</div>

				<!-- Quick Simulation Panel for Reviewers & Users -->
				<div class="flex items-center gap-2">
					<button
						onclick={() => toggleHumanIntervention(activeConversation.id)}
						class="text-xs font-semibold px-3 py-2 rounded-xl border border-outline-variant bg-surface transition-all active:scale-95 flex items-center gap-1 focus-visible:ring-2 focus-visible:ring-primary outline-none hover:bg-surface-container-low {activeConversation.humanInterventionRequired ? 'border-yellow-400 bg-yellow-50 text-yellow-800' : 'text-on-surface-variant'}"
						aria-label="Toggle Human Intervention Required status"
					>
						<span class="material-symbols-outlined text-sm {activeConversation.humanInterventionRequired ? 'text-yellow-600' : ''}">warning</span>
						<span class="hidden lg:inline">Operator Assistance</span>
					</button>

					<button
						onclick={() => toggleAiPaused(activeConversation.id)}
						class="text-xs font-semibold px-3 py-2 rounded-xl border border-outline-variant bg-surface transition-all active:scale-95 flex items-center gap-1 focus-visible:ring-2 focus-visible:ring-primary outline-none hover:bg-surface-container-low {activeConversation.aiPaused ? 'border-red-300 bg-red-50 text-red-800' : 'text-on-surface-variant'}"
						aria-label="Toggle AI automation pause"
					>
						<span class="material-symbols-outlined text-sm">{activeConversation.aiPaused ? 'play_arrow' : 'pause'}</span>
						<span>{activeConversation.aiPaused ? 'Resume AI' : 'Pause AI'}</span>
					</button>
				</div>
			</header>

			<!-- AI Dialog Automation State Banner -->
			{#if activeConversation.aiPaused}
				<div class="bg-yellow-50 border-b border-yellow-200 px-4 py-2.5 flex items-center gap-2 text-xs text-yellow-800" role="status">
					<span class="material-symbols-outlined text-sm text-yellow-600">pause_circle</span>
					<span class="font-medium">
						AI assistant is paused. Incoming customer responses will not be evaluated by AI. You are in manual takeover.
					</span>
				</div>
			{:else}
				<div class="bg-green-50 border-b border-green-200 px-4 py-2.5 flex items-center gap-2 text-xs text-green-800" role="status">
					<span class="material-symbols-outlined text-sm text-green-600">smart_toy</span>
					<span class="font-medium">
						AI dialog engine is active. It will automatically evaluate and respond to the customer after standard pauses.
					</span>
				</div>
			{/if}

			<!-- Chat Stream Container -->
			<div class="flex-1 overflow-y-auto p-4 space-y-4 bg-surface-container-lowest">
				<!-- System Transfer notice matching mockup style -->
				{#if activeConversation.humanInterventionRequired}
					<div class="flex justify-center py-2">
						<div class="bg-yellow-50 border border-yellow-200 px-4 py-3 rounded-xl max-w-md flex flex-col items-center text-center gap-1.5 shadow-sm">
							<span class="material-symbols-outlined text-yellow-700 text-lg">transfer_within_a_station</span>
							<p class="text-xs font-bold text-yellow-800 uppercase tracking-wider">Awaiting Human Intervention</p>
							<p class="text-xs text-on-surface-variant italic">
								Reason: Complex or direct request detected. Human assistance flagged as required.
							</p>
						</div>
					</div>
				{/if}

				{#each activeConversation.messages as msg}
					{#if msg.sender === 'user'}
						<!-- User message bubble (incoming) -->
						<div class="flex flex-col items-end gap-1">
							<div class="bg-primary text-white px-4 py-2.5 rounded-2xl rounded-tr-none max-w-[85%] shadow-sm text-sm">
								<p class="whitespace-pre-wrap">{msg.text}</p>
							</div>
							<span class="text-[11px] text-on-surface-variant mr-1">{msg.time}</span>
						</div>
					{:else if msg.sender === 'ai'}
						<!-- AI response bubble -->
						<div class="flex flex-col items-start gap-1">
							<div class="flex items-center gap-1.5 mb-0.5">
								<span class="w-5 h-5 rounded-full bg-green-100 flex items-center justify-center">
									<span class="material-symbols-outlined text-[13px] text-green-700">smart_toy</span>
								</span>
								<span class="text-xs font-bold text-on-surface-variant">AI Assistant</span>
							</div>
							<div class="bg-surface border border-outline-variant px-4 py-2.5 rounded-2xl rounded-tl-none max-w-[85%] shadow-sm text-sm">
								<p class="whitespace-pre-wrap">{msg.text}</p>
							</div>
							<span class="text-[11px] text-on-surface-variant ml-1">{msg.time}</span>
						</div>
					{:else}
						<!-- Operator response bubble -->
						<div class="flex flex-col items-start gap-1">
							<div class="flex items-center gap-1.5 mb-0.5">
								<span class="w-5 h-5 rounded-full bg-primary-fixed flex items-center justify-center">
									<span class="material-symbols-outlined text-[13px] text-primary">person</span>
								</span>
								<span class="text-xs font-bold text-primary">Operator (You)</span>
							</div>
							<div class="bg-surface border-2 border-primary/20 px-4 py-2.5 rounded-2xl rounded-tl-none max-w-[85%] shadow-sm text-sm">
								<p class="whitespace-pre-wrap">{msg.text}</p>
							</div>
							<span class="text-[11px] text-on-surface-variant ml-1">{msg.time}</span>
						</div>
					{/if}
				{/each}
			</div>

			<!-- Quick Suggestions -->
			<div class="px-4 py-2 border-t border-outline-variant/50 flex gap-2 overflow-x-auto bg-surface flex-shrink-0">
				<button
					onclick={() => { operatorMessageText = "Let me look up your payment logs and see why it failed."; }}
					class="whitespace-nowrap px-3 py-1.5 bg-surface-container-low border border-outline-variant rounded-full text-xs font-medium text-on-surface-variant hover:bg-surface-container-high transition-colors focus-visible:ring-2 focus-visible:ring-primary outline-none"
				>
					Check logs
				</button>
				<button
					onclick={() => { operatorMessageText = "Would you like to try with another card?"; }}
					class="whitespace-nowrap px-3 py-1.5 bg-surface-container-low border border-outline-variant rounded-full text-xs font-medium text-on-surface-variant hover:bg-surface-container-high transition-colors focus-visible:ring-2 focus-visible:ring-primary outline-none"
				>
					Request new card
				</button>
				<button
					onclick={() => { operatorMessageText = "I am transferring you to our billing team. One second please."; }}
					class="whitespace-nowrap px-3 py-1.5 bg-surface-container-low border border-outline-variant rounded-full text-xs font-medium text-on-surface-variant hover:bg-surface-container-high transition-colors focus-visible:ring-2 focus-visible:ring-primary outline-none"
				>
					Escalate to Finance
				</button>
			</div>

			<!-- Message Input Footer Area -->
			<footer class="p-4 bg-surface border-t border-outline-variant flex-shrink-0 pb-4 md:pb-4">
				<form onsubmit={handleSendMessage} class="flex items-end gap-2 bg-surface-container-low border border-outline-variant rounded-2xl p-1.5 focus-within:ring-2 focus-within:ring-primary/20 focus-within:border-primary transition-all">
					<button
						type="button"
						class="p-2.5 text-on-surface-variant hover:text-primary transition-colors rounded-full focus-visible:ring-2 focus-visible:ring-primary outline-none"
						aria-label="Add attachment"
					>
						<span class="material-symbols-outlined">add_circle</span>
					</button>

					<textarea
						bind:value={operatorMessageText}
						placeholder={placeholderText}
						rows="1"
						class="flex-1 bg-transparent border-none focus:ring-0 py-2 px-1 text-sm resize-none max-h-32 outline-none"
						onkeydown={(e) => {
							if (e.key === 'Enter' && !e.shiftKey) {
								e.preventDefault();
								handleSendMessage(e);
							}
						}}
					></textarea>

					<div class="flex items-center gap-1">
						<button
							type="button"
							class="p-2.5 text-on-surface-variant hover:text-primary transition-colors rounded-full focus-visible:ring-2 focus-visible:ring-primary outline-none"
							aria-label="Insert emoji"
						>
							<span class="material-symbols-outlined">mood</span>
						</button>

						<button
							type="submit"
							class="bg-primary text-white p-2.5 rounded-xl shadow hover:shadow-md hover:bg-primary/90 active:scale-95 transition-all flex items-center justify-center min-w-[44px] min-h-[44px] focus-visible:ring-2 focus-visible:ring-primary outline-none"
							aria-label="Send message"
						>
							<span class="material-symbols-outlined" style="font-variation-settings: 'FILL' 1;">send</span>
						</button>
					</div>
				</form>
			</footer>
		</section>
	</main>
</div>

<!-- Bottom Navigation Bar (Mobile only, hidden on desktop and hidden when mobile active chat is open to maximize real estate) -->
{#if !showMobileChat}
	<nav class="md:hidden fixed bottom-0 left-0 w-full z-40 flex justify-around items-center bg-surface border-t border-outline-variant py-2 px-4 pb-safe flex-shrink-0">
		<a class="flex flex-col items-center justify-center text-on-surface-variant hover:text-primary transition-transform duration-150 active:scale-90 min-w-[48px] min-h-[48px] focus-visible:ring-2 focus-visible:ring-primary outline-none" href="/">
			<span class="material-symbols-outlined">rocket_launch</span>
			<span class="text-[10px] font-medium">Campaigns</span>
		</a>
		<a class="flex flex-col items-center justify-center text-primary font-bold transition-transform duration-150 active:scale-90 min-w-[48px] min-h-[48px] focus-visible:ring-2 focus-visible:ring-primary outline-none" href="/inbox">
			<span class="material-symbols-outlined">inbox</span>
			<span class="text-[10px] font-medium">Inbox</span>
		</a>
	</nav>
{/if}

<style>
	/* Custom styles for polished micro-interactions */
	:global(html, body) {
		height: 100%;
		margin: 0;
		padding: 0;
		overflow: hidden;
	}
</style>

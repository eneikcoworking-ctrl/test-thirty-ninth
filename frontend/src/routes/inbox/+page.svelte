<script lang="ts">
    import { onMount } from 'svelte';

    interface Message {
        id: string;
        sender: 'OPERATOR' | 'CUSTOMER' | 'AI';
        text: string;
        timestamp: string;
        read?: boolean;
    }

    interface Conversation {
        id: string;
        name: string;
        avatar: string;
        lastActive: string;
        humanInterventionRequired: boolean;
        aiActive: boolean;
        messages: Message[];
    }

    // Reactive State
    let conversations = $state<Conversation[]>([
        {
            id: '1',
            name: 'Sarah Jenkins',
            avatar: 'https://lh3.googleusercontent.com/aida-public/AB6AXuDnDXfHZde9TQzgw4DWUFeAp3WrnVLNA40rW6CDCLMUmndOj_BRWNQCcK03akzclDueqJkbaMslDw6QJuQIWBlVXgSJOT2tv79IveAoRsIgv9COQl9ZvzHzv7QNi02jRM8Dw0vYi9TzEaCsNNJSHAZkTkZcfQCKMhn4ZXSH_ccRHdupzHExuDIEw-QdlgPN6p6BP24RaFsmNLwUkxx6ef9_v3MnOlJQBRIln83rF76XEodDsP4zHStmOJx8rLs-Z4mpvmpfeRSApyU',
            lastActive: 'Active 2m ago',
            humanInterventionRequired: true,
            aiActive: true,
            messages: [
                {
                    id: 'm1',
                    sender: 'OPERATOR',
                    text: "Hello Sarah, I've seen your inquiry about the enterprise account upgrade. How can I help?",
                    timestamp: '2:46 PM',
                    read: true
                },
                {
                    id: 'm2',
                    sender: 'CUSTOMER',
                    text: 'Hi, I\'m trying to process the payment but the system keeps throwing a 402 error. This is critical as our trial ends in 2 hours.',
                    timestamp: '2:48 PM'
                },
                {
                    id: 'm3',
                    sender: 'CUSTOMER',
                    text: 'It also says "Unauthorized" now. Please can someone help urgently? We have 40 seats waiting to be onboarded.',
                    timestamp: '2:49 PM'
                }
            ]
        },
        {
            id: '2',
            name: 'John Doe',
            avatar: 'https://lh3.googleusercontent.com/aida-public/AB6AXuCJtaYuYsqDf4I3Cg-p1W7DH7N4HY76TF7b9zUSjXiaM6rYnblSNPH1G9UDBk844sTVJem05Mf3p9_Y_UPH9JdPlLM07QsTAZh3CDi2RYfz7_4IBC2M7yHLARS2VWO-Sk4IeTsovyVUnbDMH0HcQLItOI2GOWta5TmmhUMlTLbJQ-83nP8Jx0zOxeIG_MsWo6knpc413nnwZgfpUb22i3CiRokqxSv-Muq7PMHr7iGm2k7haTcn6N3yHFvDwwZsSo1QoEomdOhnDGo',
            lastActive: 'Active 10m ago',
            humanInterventionRequired: false,
            aiActive: true,
            messages: [
                {
                    id: 'm4',
                    sender: 'CUSTOMER',
                    text: 'Can you tell me more about your pricing plans?',
                    timestamp: '1:15 PM'
                },
                {
                    id: 'm5',
                    sender: 'AI',
                    text: 'Sure! We have basic, professional, and enterprise tiers starting at $15/seat. I can guide you through the details.',
                    timestamp: '1:16 PM'
                }
            ]
        },
        {
            id: '3',
            name: 'Alice Smith',
            avatar: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=facearea&facepad=2&w=256&h=256&q=80',
            lastActive: 'Active 1h ago',
            humanInterventionRequired: false,
            aiActive: false,
            messages: [
                {
                    id: 'm6',
                    sender: 'CUSTOMER',
                    text: 'Is there a trial extension possible?',
                    timestamp: '11:30 AM'
                },
                {
                    id: 'm7',
                    sender: 'OPERATOR',
                    text: 'Yes Alice, I have extended your trial by another 7 days. Enjoy testing!',
                    timestamp: '11:32 AM',
                    read: true
                }
            ]
        }
    ]);

    let activeId = $state<string>('1');
    let newMessageText = $state<string>('');
    let showMobileChat = $state<boolean>(false);
    let shiftTimer = $state<string>('06:42:15');

    // Derived States
    let activeChat = $derived(conversations.find(c => c.id === activeId) || conversations[0]);

    // Timer Interval
    let intervalId: any;
    onMount(() => {
        intervalId = setInterval(() => {
            const parts = shiftTimer.split(':').map(Number);
            let s = parts[2] + 1;
            let m = parts[1];
            let h = parts[0];
            if (s >= 60) {
                s = 0;
                m += 1;
            }
            if (m >= 60) {
                m = 0;
                h += 1;
            }
            shiftTimer = [h, m, s].map(v => String(v).padStart(2, '0')).join(':');
        }, 1000);

        return () => {
            if (intervalId) clearInterval(intervalId);
        };
    });

    // Take Over Action
    function takeOverChat(id: string) {
        const index = conversations.findIndex(c => c.id === id);
        if (index !== -1) {
            conversations[index].aiActive = false;
        }
    }

    // Toggle Human Intervention Required
    function toggleHumanRequired(id: string) {
        const index = conversations.findIndex(c => c.id === id);
        if (index !== -1) {
            conversations[index].humanInterventionRequired = !conversations[index].humanInterventionRequired;
        }
    }

    // Send Message Handler
    function sendMessage() {
        if (!newMessageText.trim()) return;

        const currentId = activeId;
        const index = conversations.findIndex(c => c.id === currentId);
        if (index === -1) return;

        const date = new Date();
        const timeStr = date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

        const newMsg: Message = {
            id: 'm_gen_' + Date.now(),
            sender: 'OPERATOR',
            text: newMessageText.trim(),
            timestamp: timeStr,
            read: false
        };

        // Append to active chat messages reactively
        conversations[index].messages.push(newMsg);

        // Instant dispatch & pause AI according to AC:
        // "Given an active chat window, When an operator types and sends a message, Then it dispatches instantly and pauses the AI."
        conversations[index].aiActive = false;

        // Clear input
        newMessageText = '';

        // Auto-scroll logic if element is present
        setTimeout(() => {
            const container = document.getElementById('chat-scroll-area');
            if (container) {
                container.scrollTop = container.scrollHeight;
            }
        }, 50);
    }

    // Select Chat Handler
    function selectChat(id: string) {
        activeId = id;
        showMobileChat = true;
    }

    // Simulated Inbound Message (Attractive/Delighter)
    function simulateCustomerMessage() {
        const index = conversations.findIndex(c => c.id === activeId);
        if (index === -1) return;

        const responses = [
            "Wait, does the payment require 3D Secure verification?",
            "Yes, please let me know when it's resolved.",
            "Awesome, looking forward to starting our onboarding session!",
            "Can you also check our API key credentials?"
        ];
        const randomMsg = responses[Math.floor(Math.random() * responses.length)];
        const date = new Date();
        const timeStr = date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

        const newMsg: Message = {
            id: 'm_sim_' + Date.now(),
            sender: 'CUSTOMER',
            text: randomMsg,
            timestamp: timeStr
        };

        conversations[index].messages.push(newMsg);

        // If AI is active, simulate bot analyzing/processing, otherwise mark Human Intervention Required
        if (!conversations[index].aiActive) {
            conversations[index].humanInterventionRequired = true;
        }

        setTimeout(() => {
            const container = document.getElementById('chat-scroll-area');
            if (container) {
                container.scrollTop = container.scrollHeight;
            }
        }, 50);
    }
</script>

<div class="flex h-screen bg-[#f9f9ff] text-[#151c27] font-sans antialiased overflow-hidden">

    <!-- Sidebar Navigation - Desktop Only -->
    <aside class="hidden lg:flex flex-col w-64 bg-white border-r border-[#e7eefe] z-30 shrink-0">
        <!-- Logo & Brand Header -->
        <div class="px-6 py-5 border-b border-[#e7eefe]">
            <h2 class="text-xl font-bold text-[#3525cd] tracking-tight flex items-center gap-2">
                <span class="material-symbols-outlined text-[24px]">forum</span>
                Inbox Console
            </h2>
        </div>

        <!-- Operator Identity Panel -->
        <div class="px-4 py-4 border-b border-[#e7eefe]">
            <div class="flex items-center gap-3 p-3 rounded-lg bg-[#f0f3ff] border border-[#dce2f3]">
                <div class="w-10 h-10 rounded-full overflow-hidden border border-[#c7c4d8]">
                    <img class="w-full h-full object-cover" alt="Alex Rivers" src="https://lh3.googleusercontent.com/aida-public/AB6AXuCJtaYuYsqDf4I3Cg-p1W7DH7N4HY76TF7b9zUSjXiaM6rYnblSNPH1G9UDBk844sTVJem05Mf3p9_Y_UPH9JdPlLM07QsTAZh3CDi2RYfz7_4IBC2M7yHLARS2VWO-Sk4IeTsovyVUnbDMH0HcQLItOI2GOWta5TmmhUMlTLbJQ-83nP8Jx0zOxeIG_MsWo6knpc413nnwZgfpUb22i3CiRokqxSv-Muq7PMHr7iGm2k7haTcn6N3yHFvDwwZsSo1QoEomdOhnDGo" />
                </div>
                <div>
                    <p class="font-semibold text-sm text-[#151c27]">Alex Rivers</p>
                    <p class="text-[10px] uppercase font-bold text-[#006c49] flex items-center gap-1">
                        <span class="w-1.5 h-1.5 rounded-full bg-[#006c49] inline-block animate-pulse"></span>
                        Online
                    </p>
                </div>
            </div>
        </div>

        <!-- Navigation Menus -->
        <nav class="flex-1 px-3 py-4 space-y-1">
            <a href="/" class="flex items-center gap-3 px-4 py-3 text-sm font-medium text-[#464555] hover:bg-[#f0f3ff] rounded-lg transition-colors">
                <span class="material-symbols-outlined text-[20px]">rocket_launch</span>
                Launch Campaign
            </a>
            <a href="/accounts" class="flex items-center gap-3 px-4 py-3 text-sm font-medium text-[#464555] hover:bg-[#f0f3ff] rounded-lg transition-colors">
                <span class="material-symbols-outlined text-[20px]">group</span>
                Account Pool
            </a>
            <button class="w-full flex items-center gap-3 px-4 py-3 text-sm font-medium text-[#3525cd] bg-[#e2dfff] rounded-lg text-left">
                <span class="material-symbols-outlined text-[20px]">chat_bubble</span>
                Unified Inbox
            </button>
            <button class="w-full flex items-center gap-3 px-4 py-3 text-sm font-medium text-[#464555] hover:bg-[#f0f3ff] rounded-lg text-left transition-colors opacity-70">
                <span class="material-symbols-outlined text-[20px]">task_alt</span>
                Resolved
            </button>
        </nav>

        <!-- Current Shift Timer widget -->
        <div class="p-4 mt-auto">
            <div class="p-4 rounded-xl bg-white border border-[#dce2f3]">
                <p class="text-xs text-[#464555] mb-1">Current Operator Shift</p>
                <p class="text-lg font-bold text-[#151c27] font-mono tracking-wider">{shiftTimer}</p>
            </div>
        </div>
    </aside>

    <!-- Main Workspace Split Panel -->
    <div class="flex-1 flex flex-row overflow-hidden relative">

        <!-- Conversation List (Left Panel / Sibling) -->
        <section class="w-full lg:w-80 bg-white border-r border-[#e7eefe] flex flex-col shrink-0 {showMobileChat ? 'hidden lg:flex' : 'flex'}">
            <!-- Header search & count -->
            <div class="px-4 py-5 border-b border-[#e7eefe] flex justify-between items-center bg-white">
                <h3 class="text-lg font-bold text-[#151c27] flex items-center gap-2">
                    Conversations
                    <span class="bg-[#e2dfff] text-[#3525cd] text-xs font-bold px-2 py-0.5 rounded-full">
                        {conversations.length}
                    </span>
                </h3>
                <button onclick={simulateCustomerMessage} class="text-xs bg-[#e2dfff] text-[#3525cd] font-semibold px-2 py-1 rounded hover:bg-[#3525cd] hover:text-white transition-all flex items-center gap-1" title="Simulate a customer incoming message">
                    <span class="material-symbols-outlined text-[14px]">refresh</span>
                    Simulate Msg
                </button>
            </div>

            <!-- Scrollable list of chats -->
            <div class="flex-1 overflow-y-auto divide-y divide-[#e7eefe]">
                {#each conversations as chat}
                    <div
                        role="button"
                        tabindex="0"
                        onclick={() => selectChat(chat.id)}
                        onkeydown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); selectChat(chat.id); } }}
                        class="w-full px-4 py-4 flex gap-3 text-left transition-all hover:bg-[#f9f9ff] focus:bg-[#f0f3ff] outline-none border-l-4 cursor-pointer {activeId === chat.id ? 'border-[#3525cd] bg-[#f0f3ff]' : 'border-transparent'}"
                    >
                        <!-- Profile Pic & Indicator -->
                        <div class="relative shrink-0">
                            <div class="w-12 h-12 rounded-full overflow-hidden border border-[#dce2f3]">
                                <img class="w-full h-full object-cover" alt={chat.name} src={chat.avatar} />
                            </div>
                            <!-- Yellow or Green Indicator Dot -->
                            <!-- Indicator turns yellow if Human Intervention Required, else green / gray -->
                            <span
                                class="absolute bottom-0 right-0 w-3.5 h-3.5 rounded-full border-2 border-white transition-all {chat.humanInterventionRequired ? 'bg-yellow-400 shadow-[0_0_8px_#FBBF24]' : 'bg-green-500'}"
                                title={chat.humanInterventionRequired ? 'Human Intervention Required' : 'AI Active / Connected'}
                            ></span>
                        </div>

                        <!-- Info & Preview -->
                        <div class="flex-1 min-w-0">
                            <div class="flex justify-between items-start mb-1">
                                <h4 class="font-semibold text-sm text-[#151c27] truncate pr-1">{chat.name}</h4>
                                <span class="text-[10px] text-[#464555] shrink-0 font-medium">
                                    {chat.messages[chat.messages.length - 1]?.timestamp || 'Active'}
                                </span>
                            </div>
                            <p class="text-xs text-[#464555] truncate mb-2">
                                {chat.messages[chat.messages.length - 1]?.text || 'No messages yet'}
                            </p>

                            <!-- Labels & Badge Controls -->
                            <div class="flex flex-wrap items-center gap-1.5 mt-2">
                                {#if chat.humanInterventionRequired}
                                    <span class="bg-yellow-100 text-yellow-800 text-[9px] font-bold px-2 py-0.5 rounded-full uppercase tracking-wider border border-yellow-200 flex items-center gap-0.5">
                                        <span class="material-symbols-outlined text-[10px] text-yellow-700">priority_high</span>
                                        Operator Required
                                    </span>
                                {:else if chat.aiActive}
                                    <span class="bg-[#6cf8bb]/20 text-[#00714d] text-[9px] font-bold px-2 py-0.5 rounded-full uppercase tracking-wider border border-[#6cf8bb]/40 flex items-center gap-0.5">
                                        <span class="material-symbols-outlined text-[10px] text-[#00714d]">smart_toy</span>
                                        AI Active
                                    </span>
                                {/if}

                                <button
                                    onclick={(e) => { e.stopPropagation(); toggleHumanRequired(chat.id); }}
                                    class="text-[9px] bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold px-2 py-0.5 rounded border border-slate-200 transition-colors shrink-0 ml-auto"
                                >
                                    Toggle Flag
                                </button>
                            </div>
                        </div>
                    </div>
                {/each}
            </div>
        </section>

        <!-- Active Chat Window (Right Panel) -->
        <main class="flex-1 flex flex-col bg-[#f9f9ff] h-full relative {showMobileChat ? 'flex' : 'hidden lg:flex'}">

            <!-- Chat Window Header -->
            <header class="h-16 px-6 bg-white border-b border-[#e7eefe] flex items-center justify-between z-10 shrink-0 shadow-sm">
                <div class="flex items-center gap-3">
                    <!-- Back Button on Mobile -->
                    <button
                        onclick={() => showMobileChat = false}
                        class="lg:hidden p-2 -ml-2 text-[#464555] hover:bg-[#f0f3ff] rounded-full transition-colors"
                        aria-label="Back to conversations"
                    >
                        <span class="material-symbols-outlined text-[24px]">arrow_back</span>
                    </button>

                    <!-- Avatar & Status info -->
                    <div class="w-10 h-10 rounded-full overflow-hidden border border-[#dce2f3] shrink-0">
                        <img class="w-full h-full object-cover" alt={activeChat.name} src={activeChat.avatar} />
                    </div>
                    <div>
                        <h1 class="font-bold text-base text-[#151c27] leading-tight">{activeChat.name}</h1>
                        <div class="flex items-center gap-1.5 mt-0.5">
                            <span class="w-2 h-2 rounded-full {activeChat.humanInterventionRequired ? 'bg-yellow-400' : 'bg-green-500'}"></span>
                            <span class="text-xs text-[#464555] font-medium">
                                {activeChat.humanInterventionRequired ? 'Human Intervention Required' : activeChat.lastActive}
                            </span>
                        </div>
                    </div>
                </div>

                <!-- Action Controls in Header -->
                <div class="flex items-center gap-2">
                    <button
                        onclick={() => toggleHumanRequired(activeChat.id)}
                        class="text-xs font-semibold px-3 py-1.5 rounded-lg border transition-all flex items-center gap-1 {activeChat.humanInterventionRequired ? 'bg-yellow-50 text-yellow-800 border-yellow-300 hover:bg-yellow-100' : 'bg-white text-[#464555] border-[#dce2f3] hover:bg-[#f9f9ff]'}"
                    >
                        <span class="material-symbols-outlined text-[16px]">
                            {activeChat.humanInterventionRequired ? 'warning' : 'person_pin'}
                        </span>
                        {activeChat.humanInterventionRequired ? 'Operator Needed' : 'Mark Operator Needed'}
                    </button>

                    <span class="hidden md:flex bg-red-100 text-red-800 px-3 py-1 rounded-full text-xs font-bold items-center gap-1">
                        <span class="material-symbols-outlined text-[16px] text-red-600">priority_high</span>
                        Urgent
                    </span>
                </div>
            </header>

            <!-- Chat Message Scroll Area -->
            <div
                id="chat-scroll-area"
                class="flex-1 overflow-y-auto px-6 py-6 space-y-4"
            >
                <div class="flex justify-center my-2">
                    <span class="text-xs text-[#464555] bg-[#e7eefe] px-4 py-1.5 rounded-full font-medium shadow-sm">
                        Today
                    </span>
                </div>

                {#each activeChat.messages as msg}
                    {#if msg.sender === 'OPERATOR'}
                        <!-- Operator Message (Right Align) -->
                        <div class="flex justify-end mb-1">
                            <div class="max-w-[75%]">
                                <div class="bg-[#3525cd] text-white text-sm p-4 rounded-2xl rounded-tr-none shadow-sm font-medium leading-relaxed">
                                    {msg.text}
                                </div>
                                <div class="flex justify-end mt-1 gap-1 items-center">
                                    <span class="text-[10px] text-[#464555]">{msg.timestamp}</span>
                                    <span class="material-symbols-outlined text-[12px] text-[#006c49]">done_all</span>
                                </div>
                            </div>
                        </div>
                    {:else if msg.sender === 'CUSTOMER'}
                        <!-- Customer Message (Left Align) -->
                        <div class="flex justify-start mb-1">
                            <div class="max-w-[75%] flex gap-2">
                                <div class="flex flex-col relative pl-3 border-l-4 {activeChat.humanInterventionRequired ? 'border-yellow-400' : 'border-transparent'}">
                                    <div class="bg-white text-[#151c27] text-sm p-4 rounded-2xl rounded-tl-none border border-[#dce2f3] shadow-sm leading-relaxed">
                                        {msg.text}
                                    </div>
                                    <div class="mt-1">
                                        <span class="text-[10px] text-[#464555]">{msg.timestamp}</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    {:else}
                        <!-- AI Response (Centered notification style or styled bubble) -->
                        <div class="flex justify-start mb-1">
                            <div class="max-w-[75%]">
                                <div class="bg-emerald-50 text-emerald-900 text-sm p-4 rounded-2xl rounded-tl-none border border-emerald-200 shadow-sm leading-relaxed flex gap-2 items-start">
                                    <span class="material-symbols-outlined text-emerald-700 shrink-0 mt-0.5" style="font-variation-settings: 'FILL' 1;">smart_toy</span>
                                    <div>
                                        <p class="font-bold text-xs text-emerald-800 uppercase tracking-wider mb-1">AI Automated Response</p>
                                        <p>{msg.text}</p>
                                    </div>
                                </div>
                                <div class="mt-1">
                                    <span class="text-[10px] text-[#464555]">{msg.timestamp}</span>
                                </div>
                            </div>
                        </div>
                    {/if}
                {/each}

                <!-- Pending/typing indicators or status notices -->
                {#if activeChat.aiActive}
                    <div class="flex justify-center my-4">
                        <div class="flex items-center gap-2 bg-[#f0f3ff] border border-[#dce2f3] px-4 py-2 rounded-full shadow-sm">
                            <span class="material-symbols-outlined text-[16px] text-[#464555] animate-spin">smart_toy</span>
                            <span class="text-xs text-[#464555] italic">AI Assistant is evaluating context...</span>
                        </div>
                    </div>
                {:else}
                    <div class="flex justify-center my-4">
                        <div class="flex items-center gap-2 bg-slate-100 border border-slate-200 px-4 py-2 rounded-full shadow-sm">
                            <span class="material-symbols-outlined text-[16px] text-slate-500">pause_circle</span>
                            <span class="text-xs text-slate-600 italic">AI Auto-Reply paused (Operator in control)</span>
                        </div>
                    </div>
                {/if}
            </div>

            <!-- Sticky Floating Takeover Banner -->
            {#if activeChat.aiActive}
                <div class="absolute bottom-[104px] left-6 right-6 z-20">
                    <div class="bg-white/95 backdrop-blur-md border border-[#3525cd]/20 rounded-2xl p-4 shadow-xl flex flex-col sm:flex-row items-center justify-between gap-4 ring-1 ring-[#3525cd]/5">
                        <div class="flex items-center gap-3">
                            <div class="w-10 h-10 rounded-full bg-[#3525cd]/10 flex items-center justify-center text-[#3525cd] shrink-0">
                                <span class="material-symbols-outlined text-[24px]" style="font-variation-settings: 'FILL' 1;">smart_toy</span>
                            </div>
                            <div>
                                <p class="font-bold text-sm text-[#151c27]">AI Bot is handling this conversation</p>
                                <p class="text-xs text-[#464555]">Recommended to take over the chat to assist customer directly.</p>
                            </div>
                        </div>
                        <button
                            onclick={() => takeOverChat(activeChat.id)}
                            class="w-full sm:w-auto bg-[#3525cd] text-white font-semibold text-sm px-5 py-2.5 rounded-xl shadow-md hover:bg-[#3525cd]/90 hover:scale-[1.02] active:scale-95 transition-all whitespace-nowrap"
                        >
                            Take Over Chat
                        </button>
                    </div>
                </div>
            {/if}

            <!-- Chat Message Input Footer -->
            <footer class="h-[88px] bg-white border-t border-[#e7eefe] p-4 flex items-center gap-3 z-20 shrink-0">
                <button
                    onclick={simulateCustomerMessage}
                    class="w-10 h-10 rounded-full flex items-center justify-center text-[#464555] hover:bg-[#f0f3ff] transition-colors"
                    title="Simulate Customer Reply"
                >
                    <span class="material-symbols-outlined text-[24px]">psychology</span>
                </button>

                <form onsubmit={(e) => { e.preventDefault(); sendMessage(); }} class="flex-1 flex items-center gap-3">
                    <div class="flex-1 bg-[#f9f9ff] border border-[#dce2f3] rounded-full px-4 h-12 flex items-center focus-within:ring-2 focus-within:ring-[#3525cd]/20 focus-within:border-[#3525cd] transition-all">
                        <input
                            bind:value={newMessageText}
                            class="bg-transparent border-none focus:outline-none focus:ring-0 w-full text-sm text-[#151c27] placeholder-[#464555]"
                            placeholder={activeChat.aiActive ? "Type a message to instantly take over and reply..." : "Type a message to reply..."}
                            type="text"
                        />
                        <button type="button" class="text-[#464555] hover:text-[#151c27] px-2">
                            <span class="material-symbols-outlined text-[20px]">sentiment_satisfied</span>
                        </button>
                    </div>

                    <!-- Send Button -->
                    <button
                        type="submit"
                        disabled={!newMessageText.trim()}
                        class="w-12 h-12 rounded-full flex items-center justify-center transition-all shrink-0 {newMessageText.trim() ? 'bg-[#3525cd] text-white shadow-md active:scale-95 hover:scale-105' : 'bg-[#e2dfff] text-[#3525cd] opacity-50 cursor-not-allowed'}"
                        aria-label="Send Message"
                    >
                        <span class="material-symbols-outlined text-[22px]" style="font-variation-settings: 'FILL' 1;">send</span>
                    </button>
                </form>
            </footer>

        </main>
    </div>
</div>

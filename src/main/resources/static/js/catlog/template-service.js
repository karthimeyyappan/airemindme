/**
 * template-service.js
 * Generic reusable AI template services and picker components
 */

const TemplateService = {
    async getTemplatesByModule(moduleCode, category = '') {
        let url = `/api/catalog/templates?module=${encodeURIComponent(moduleCode)}`;
        if (category) {
            url += `&category=${encodeURIComponent(category)}`;
        }
        if (typeof api === 'function') {
            return api(url);
        }
        const res = await fetch(url);
        if (!res.ok) throw new Error(res.statusText);
        return res.json();
    },

    async getTemplate(id) {
        const url = `/api/catalog/templates/${id}`;
        if (typeof api === 'function') {
            return api(url);
        }
        const res = await fetch(url);
        if (!res.ok) throw new Error(res.statusText);
        return res.json();
    },

    async applyTemplate(id) {
        return this.getTemplate(id);
    },

    async createTemplate(data) {
        const url = `/api/catalog/templates`;
        if (typeof api === 'function') {
            return api(url, 'POST', data);
        }
        const res = await fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        if (!res.ok) throw new Error(res.statusText);
        return res.json();
    }
};

// ─────────────────────────────────────────────────────────────────────────────
// LOADER OVERLAYS
// ─────────────────────────────────────────────────────────────────────────────
function showFullLoader(title, subtitle = 'Please wait while we process your request.') {
    let overlay = document.getElementById('aiGlobalLoaderOverlay');
    if (!overlay) {
        overlay = document.createElement('div');
        overlay.id = 'aiGlobalLoaderOverlay';
        overlay.className = 'fixed inset-0 z-[9999] flex items-center justify-center bg-slate-950/70 backdrop-blur-sm transition-opacity duration-300';
        overlay.innerHTML = `
            <div class="relative mx-4 max-w-lg w-full">
              <div class="rounded-[2rem] bg-white/95 border border-slate-200 shadow-2xl p-7 sm:p-10">
                <div class="flex flex-col items-center text-center gap-5">
                  <div class="relative flex h-20 w-20 items-center justify-center rounded-full bg-indigo-50">
                    <div class="absolute inset-0 rounded-full bg-gradient-to-br from-indigo-500/20 to-indigo-600/10 animate-pulse"></div>
                    <div class="relative h-12 w-12 rounded-full border-4 border-indigo-100 border-t-indigo-600 animate-spin"></div>
                  </div>
                  <div class="space-y-2">
                    <p id="aiGlobalLoaderTitle" class="text-xl font-semibold text-slate-900"></p>
                    <p id="aiGlobalLoaderSubtitle" class="text-sm text-slate-500 max-w-xs mx-auto"></p>
                  </div>
                </div>
              </div>
            </div>
        `;
        document.body.appendChild(overlay);
    }
    document.getElementById('aiGlobalLoaderTitle').textContent = title;
    document.getElementById('aiGlobalLoaderSubtitle').textContent = subtitle;
    overlay.classList.remove('hidden');
}

function hideFullLoader() {
    const overlay = document.getElementById('aiGlobalLoaderOverlay');
    if (overlay) overlay.classList.add('hidden');
}

// ─────────────────────────────────────────────────────────────────────────────
// REUSABLE DROPDOWN TRIGGER
// ─────────────────────────────────────────────────────────────────────────────
function openAiAssistant(options, event) {
    if (event) {
        event.stopPropagation();
    }
    
    let dd = document.getElementById('aiAssistantDropdown');
    if (!dd) {
        dd = document.createElement('div');
        dd.id = 'aiAssistantDropdown';
        dd.className = 'fixed mt-1 w-48 rounded-2xl bg-white border border-gray-200 shadow-xl z-[9999] py-1.5 hidden fade-in text-left';
        document.body.appendChild(dd);
    }

    dd.innerHTML = `
        <button type="button" class="w-full text-left px-4 py-2.5 text-xs text-gray-700 hover:bg-indigo-50 hover:text-indigo-600 font-semibold transition flex items-center gap-1.5" id="dd-create-tmpl">
          ✨ Create New Template
        </button>
        <button type="button" class="w-full text-left px-4 py-2.5 text-xs text-gray-700 hover:bg-indigo-50 hover:text-indigo-600 font-semibold transition flex items-center gap-1.5" id="dd-use-tmpl">
          📋 Use Existing Template
        </button>
    `;

    dd.querySelector('#dd-create-tmpl').onclick = () => {
        dd.classList.add('hidden');
        showFullLoader('Opening AI Template Studio...', 'Preparing your AI workspace.');
        sessionStorage.setItem("returnPage", window.location.href);
        sessionStorage.setItem("returnField", options.targetField);
        window.location.href = `/ai-templates.html?source=${options.moduleCode.toLowerCase()}`;
    };

    dd.querySelector('#dd-use-tmpl').onclick = () => {
        dd.classList.add('hidden');
        openTemplatePicker(options.moduleCode, (template) => {
            const titleField = document.getElementById('remTitle');
            if (titleField && (template.name || template.title)) {
                titleField.value = template.name || template.title;
            }
            const field = document.getElementById(options.targetField);
            if (field) {
                field.value = template.content;
                if (typeof updateCharCount === 'function') {
                    updateCharCount(field);
                }
            }
        }, options.category);
    };

    // Position relative to viewport coordinates
    const rect = event.currentTarget.getBoundingClientRect();
    dd.style.top = (rect.bottom + window.scrollY) + 'px';
    dd.style.left = (rect.right + window.scrollX - 192) + 'px';
    
    dd.classList.toggle('hidden');

    const closeDD = (e) => {
        if (!event.currentTarget.contains(e.target) && !dd.contains(e.target)) {
            dd.classList.add('hidden');
            document.removeEventListener('click', closeDD);
        }
    };
    document.addEventListener('click', closeDD);
}

// ─────────────────────────────────────────────────────────────────────────────
// REUSABLE TEMPLATE PICKER MODAL
// ─────────────────────────────────────────────────────────────────────────────
function openTemplatePicker(moduleCode, callback, category = '') {
    let modal = document.getElementById('aiTemplatePickerModal');
    if (!modal) {
        modal = document.createElement('div');
        modal.id = 'aiTemplatePickerModal';
        modal.className = 'fixed inset-0 z-[9990] flex items-center justify-center hidden';
        modal.innerHTML = `
            <div class="absolute inset-0 bg-black/40 backdrop-blur-sm" id="pickerOverlayBg"></div>
            <div class="relative bg-white rounded-3xl w-full max-w-lg max-h-[85vh] overflow-y-auto shadow-2xl p-6 flex flex-col gap-4">
              <div class="flex items-center justify-between border-b border-gray-100 pb-3">
                <div class="flex items-center gap-2.5">
                  <div class="w-10 h-10 rounded-2xl bg-indigo-50 flex items-center justify-center text-lg">📋</div>
                  <div>
                    <h3 class="text-base font-bold text-gray-900">Select Template</h3>
                    <p class="text-[11px] text-gray-400">Choose a professional template for your message</p>
                  </div>
                </div>
                <button type="button" id="closePickerBtn" class="w-8 h-8 rounded-full bg-gray-100 flex items-center justify-center text-gray-500 hover:bg-gray-200 transition">✕</button>
              </div>
              <div id="pickerTemplatesContainer" class="flex flex-col gap-3 overflow-y-auto max-h-[50vh] pr-1">
                <!-- Templates list populated dynamically -->
              </div>
            </div>
        `;
        document.body.appendChild(modal);

        modal.querySelector('#closePickerBtn').onclick = () => modal.classList.add('hidden');
        modal.querySelector('#pickerOverlayBg').onclick = () => modal.classList.add('hidden');
    }

    const container = modal.querySelector('#pickerTemplatesContainer');
    container.innerHTML = `
        <div class="text-center py-8 text-gray-400 text-sm">
          <svg class="animate-spin h-5 w-5 text-indigo-600 mx-auto mb-2" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          Loading templates...
        </div>
    `;
    modal.classList.remove('hidden');

    TemplateService.getTemplatesByModule(moduleCode, category)
        .then(response => {
            console.log("Template API Response:", response);

            let templates = response;
            if (response && typeof response === 'object' && !Array.isArray(response)) {
                if (response.templates) {
                    templates = response.templates;
                } else if (response.content) {
                    templates = response.content;
                }
            }

            if (!Array.isArray(templates)) {
                console.error("Expected templates to be an array", templates);
                container.innerHTML = `
                    <div class="text-center py-12 text-gray-400 text-sm font-medium">
                      No templates available.
                    </div>
                `;
                return;
            }

            if (templates.length === 0) {
                container.innerHTML = `
                    <div class="text-center py-12 text-gray-400 text-sm font-medium">
                      No matching templates found.<br>
                      <span class="text-xs text-gray-300">Create templates in AI Template Studio first.</span>
                    </div>
                `;
                return;
            }

            container.innerHTML = templates.map(t => `
                <div class="group border border-gray-100 rounded-2xl p-4 hover:border-indigo-200 hover:bg-indigo-50/20 cursor-pointer transition flex flex-col gap-2" id="picker-item-${t.id}">
                  <div class="flex items-center justify-between">
                    <p class="text-sm font-bold text-gray-900 group-hover:text-indigo-600 transition">${t.name || t.title}</p>
                    <span class="px-2 py-0.5 rounded-full text-[10px] font-semibold bg-gray-100 text-gray-500">${t.category || t.purpose}</span>
                  </div>
                  <p class="text-xs text-gray-500 line-clamp-2">${t.content}</p>
                </div>
            `).join('');

            templates.forEach(t => {
                const item = container.querySelector(`#picker-item-${t.id}`);
                if (item) {
                    item.onclick = () => {
                        // Apply template workflow
                        modal.classList.add('hidden');
                        showFullLoader('Applying Template...', 'Loading selected template content...');
                        setTimeout(() => {
                            hideFullLoader();
                            callback(t);
                        }, 500);
                    };
                }
            });
        })
        .catch(err => {
            container.innerHTML = `
                <div class="text-center py-8 text-red-500 text-xs font-semibold">
                  Failed to load templates: ${err.message}
                </div>
            `;
        });
}

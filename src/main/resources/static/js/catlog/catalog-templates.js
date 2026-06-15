/**
 * catalog-templates.js
 */

// ─────────────────────────────────────────────────────────────────────────────
// ╔══════════════════════════════════════════════════════════╗
// ║                   MSG TEMPLATES                         ║
// ╚══════════════════════════════════════════════════════════╝
// ─────────────────────────────────────────────────────────────────────────────
function loadTemplates() {
    api(API.templates)
        .then(data => { templates = data; renderTemplates(); })
        .catch(err => showToast('Failed to load templates: ' + err.message, 'error'));
}

function renderTemplates() {
    const q = (document.getElementById('tmplSearch')?.value || '').toLowerCase();
    const filtered = templates.filter(t =>
        (tmplFilter === 'all' || t.status === tmplFilter) &&
        ((t.title || '').toLowerCase().includes(q) ||
            (t.category || '').toLowerCase().includes(q))
    );

    const countEl = document.getElementById('tmplTabCount');
    const badgeEl = document.getElementById('tmplCountBadge');
    if (countEl) countEl.textContent = templates.length;
    if (badgeEl) badgeEl.textContent = templates.filter(t => t.status === 'active').length;

    const grid = document.getElementById('templatesGrid');
    const empty = document.getElementById('templatesEmpty');

    if (!filtered.length) { grid.innerHTML = ''; if (empty) empty.classList.remove('hidden'); return; }
    if (empty) empty.classList.add('hidden');

    const catColors = {
        'Payment Reminder': 'bg-blue-50 text-blue-700',
        'Festival Greeting': 'bg-orange-50 text-orange-700',
        'Promotion Announcement': 'bg-violet-50 text-violet-700',
        'Birthday Wish': 'bg-pink-50 text-pink-700',
        'Policy Renewal': 'bg-red-50 text-red-700',
        'Anniversary': 'bg-rose-50 text-rose-700',
        'General': 'bg-gray-100 text-gray-600'
    };

    const parseChannels = t => {
        if (Array.isArray(t.channelList)) return t.channelList;
        if (typeof t.channels === 'string' && t.channels)
            return t.channels.split(',').map(s => s.trim()).filter(Boolean);
        return [];
    };

    grid.innerHTML = filtered.map(t => {
        const catCls = catColors[t.category] || 'bg-gray-100 text-gray-600';
        const channels = parseChannels(t);
        const escaped = (t.content || '').replace(/</g, '&lt;').replace(/>/g, '&gt;');
        return `
    <div class="bg-white rounded-2xl border border-gray-100 shadow-sm p-5 hover:shadow-md transition group relative">
      <div class="flex items-start justify-between mb-3">
        <div class="flex items-center gap-3">
          <div class="w-9 h-9 rounded-xl bg-emerald-100 flex items-center justify-center flex-shrink-0">
            <svg class="w-5 h-5 text-emerald-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/></svg>
          </div>
          <div>
            <p class="text-sm font-bold text-gray-900">${t.title}</p>
            <span class="inline-block mt-0.5 px-2 py-0.5 rounded-full text-[10px] font-semibold ${catCls}">${t.category}</span>
          </div>
        </div>
        <div class="flex gap-1.5 opacity-0 group-hover:opacity-100 transition">
          <button onclick="openTemplateModal(${t.id})" title="Edit"
            class="w-8 h-8 rounded-xl border border-gray-200 flex items-center justify-center text-gray-400 hover:text-indigo-600 hover:border-indigo-200 hover:bg-indigo-50">
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z"/></svg>
          </button>
          <button onclick="toggleTemplateStatus(${t.id})" title="${t.status === 'active' ? 'Set Inactive' : 'Set Active'}"
            class="w-8 h-8 rounded-xl border border-gray-200 flex items-center justify-center ${t.status === 'active' ? 'text-emerald-500 hover:text-orange-500 hover:border-orange-200 hover:bg-orange-50' : 'text-gray-400 hover:text-emerald-500 hover:border-emerald-200 hover:bg-emerald-50'}">
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="${t.status === 'active' ? 'M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636' : 'M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z'}"/></svg>
          </button>
          <button onclick="deleteTemplate(${t.id})" title="Delete"
            class="w-8 h-8 rounded-xl border border-gray-200 flex items-center justify-center text-gray-400 hover:text-red-500 hover:border-red-200 hover:bg-red-50">
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/></svg>
          </button>
        </div>
      </div>
      ${t.description ? `<p class="text-xs text-gray-500 mb-3 line-clamp-2">${t.description}</p>` : ''}
      <div class="bg-gray-50 rounded-xl p-3 mb-3">
        <p class="text-xs text-gray-600 font-mono leading-relaxed line-clamp-3">${escaped.replace(/\n/g, '<br>')}</p>
      </div>
      <div class="flex items-center justify-between">
        <div class="flex gap-1">${channels.map(c => `<span class="px-2 py-0.5 rounded-full text-[10px] font-semibold bg-indigo-50 text-indigo-600">${c}</span>`).join('')}</div>
        <span class="px-2 py-0.5 rounded-full text-[10px] font-bold ${t.status === 'active' ? 'bg-emerald-50 text-emerald-600' : 'bg-gray-100 text-gray-400'}">${t.status === 'active' ? 'Active' : 'Inactive'}</span>
      </div>
    </div>`;
    }).join('');
}

// ─────────────────────────────────────────────────────────────────────────────
// DYNAMIC OPTIONS DATA
// ─────────────────────────────────────────────────────────────────────────────
const templateTypesByPurpose = {
    'Reminder': [
        'Payment Reminder',
        'Subscription Renewal',
        'Membership Renewal',
        'Policy Renewal',
        'Appointment Reminder',
        'Service Reminder'
    ],
    'Greeting': [
        'Birthday',
        'Anniversary',
        'Festival Greeting',
        'Welcome Message'
    ],
    'Promotion': [
        'Offer Announcement',
        'Discount Campaign',
        'New Product Launch',
        'Reactivation Campaign'
    ],
    'Announcement': [
        'Business Update',
        'Holiday Notice',
        'Timing Change',
        'Service Update'
    ],
    'Follow Up': [
        'Lead Follow Up',
        'Customer Follow Up',
        'Payment Follow Up'
    ],
    'Custom': [
        'Custom'
    ]
};

function onPurposeChange() {
    const purpose = document.getElementById('tm_purpose')?.value || 'Reminder';
    const typeSelect = document.getElementById('tm_type');
    if (!typeSelect) return;
    typeSelect.innerHTML = '';
    const types = templateTypesByPurpose[purpose] || [];
    types.forEach(t => {
        const opt = document.createElement('option');
        opt.value = t;
        opt.textContent = t;
        typeSelect.appendChild(opt);
    });
}

// ── Template Modal ────────────────────────────────────────────────────────────
function openTemplateModal(id) {
    editTemplateId = id || null;
    const purposeSelect = document.getElementById('tm_purpose');
    const typeSelect = document.getElementById('tm_type');
    const langSelect = document.getElementById('tm_language');

    if (editTemplateId) {
        const t = templates.find(x => x.id === editTemplateId);
        if (!t) return;
        document.getElementById('tm_title').value = t.title || '';
        const tmCat = document.getElementById('tm_category');
        if (tmCat) tmCat.value = t.category || 'General';
        document.getElementById('tm_desc').value = t.description || '';
        document.getElementById('tm_content').value = t.content || '';
        
        const reversePurposeMap = {
            'REMINDER': 'Reminder',
            'GREETING': 'Greeting',
            'PROMOTION': 'Promotion',
            'ANNOUNCEMENT': 'Announcement',
            'FOLLOW_UP': 'Follow Up',
            'CUSTOM': 'Custom'
        };
        if (purposeSelect) purposeSelect.value = reversePurposeMap[t.purpose] || t.purpose || 'Reminder';
        onPurposeChange();
        if (typeSelect) typeSelect.value = t.templateType || '';
        if (langSelect) langSelect.value = t.language || 'English';

        const ch = Array.isArray(t.channelList) ? t.channelList
            : (t.channels || '').split(',').map(s => s.trim()).filter(Boolean);
        
        const tmWa = document.getElementById('tm_wa');
        const tmSms = document.getElementById('tm_sms');
        const tmEmail = document.getElementById('tm_email');
        if (tmWa) tmWa.checked = ch.includes('WhatsApp');
        if (tmSms) tmSms.checked = ch.includes('SMS');
        if (tmEmail) tmEmail.checked = ch.includes('Email');

        document.getElementById('templateModalTitle').textContent = 'Edit AI Template';
        document.getElementById('tmplSaveLabel').textContent = 'Save Changes';
    } else {
        ['tm_title', 'tm_desc', 'tm_content'].forEach(i => {
            const el = document.getElementById(i);
            if (el) el.value = '';
        });
        const tmCat = document.getElementById('tm_category');
        if (tmCat) tmCat.value = 'Payment Reminder';
        
        if (purposeSelect) purposeSelect.value = 'Reminder';
        onPurposeChange();
        if (langSelect) langSelect.value = 'English';

        const tmWa = document.getElementById('tm_wa');
        const tmSms = document.getElementById('tm_sms');
        const tmEmail = document.getElementById('tm_email');
        if (tmWa) tmWa.checked = true;
        if (tmSms) tmSms.checked = false;
        if (tmEmail) tmEmail.checked = false;

        document.getElementById('templateModalTitle').textContent = 'New AI Template';
        document.getElementById('tmplSaveLabel').textContent = 'Save AI Template';
    }
    document.getElementById('templateModal').classList.remove('hidden');
}
function closeTemplateModal() { document.getElementById('templateModal').classList.add('hidden'); }

function generateTemplateContent() {
    const cat = document.getElementById('tm_category')?.value || document.getElementById('tm_type')?.value || 'General';
    const desc = document.getElementById('tm_desc').value;
    const map = {
        'Payment Reminder': `Hi {customer_name}! 👋\n\nThis is a friendly reminder that your {plan_name} payment of {amount} is due on {due_date}.\n\nPlease pay to avoid disruption.\nQueries: {business_phone}\n\n— {business_name}`,
        'Festival Greeting': `🎉 {festival} Greetings, {customer_name}!\n\nWishing you joy and prosperity.\n\nSpecial offer: {offer_details}\nValid till: {offer_expiry}\n\n— {business_name}`,
        'Promotion Announcement': `🎊 Special Offer for {customer_name}!\n\n{offer_title}\n{offer_description}\n\nView: {promo_link}\n⏰ Valid till {offer_expiry}\n\n— {business_name}`,
        'Birthday Wish': `🎂 Happy Birthday, {customer_name}!\n\nAs a birthday treat:\n🎁 {offer_details}\n\nWith warm wishes,\n{business_name}`,
        'Policy Renewal': `⚠️ Renewal Reminder, {customer_name}\n\nYour {plan_name} is due on {due_date}.\n\Renew now: {renewal_link}\nHelp: {business_phone}\n\n— {business_name}`,
        'Anniversary': `🎊 Happy Anniversary, {customer_name}!\n\nThank you for being with us.\n🎁 {offer_details}\n\n— {business_name}`,
        'General': `Hi {customer_name},\n\n${desc || 'We have an important update for you.'}\n\nContact us at {business_phone}\n\n— {business_name}`
    };
    document.getElementById('tm_content').value = map[cat] || map['General'];
}

function generateAITemplate() {
    const purpose = document.getElementById('tm_purpose')?.value;
    const templateType = document.getElementById('tm_type')?.value;
    const language = document.getElementById('tm_language')?.value;
    const btn = document.getElementById('aiGenerateBtn');
    const progressSection = document.getElementById('aiProgressSection');
    const progressText = document.getElementById('aiProgressText');
    const progressBar = document.getElementById('aiProgressBar');

    if (!purpose || !templateType || !language) {
        showToast('Please select Purpose, Template Type and Language', 'error');
        return;
    }

    if (btn) {
        btn.disabled = true;
        btn.innerHTML = `
          <svg class="animate-spin -ml-1 mr-2 h-4 w-4 text-white inline-block" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          Generating Template...
        `;
    }

    if (progressSection) {
        progressSection.classList.remove('hidden');
    }

    // Interval to cycle through messages and animate progress bar
    let progressVal = 0;
    const messages = [
        "Analyzing requirements...",
        "Creating template...",
        "Optimizing content..."
    ];
    let msgIdx = 0;

    if (progressBar) progressBar.style.width = '0%';
    if (progressText) progressText.textContent = messages[0];

    const progressInterval = setInterval(() => {
        progressVal = Math.min(progressVal + 8, 95);
        if (progressBar) progressBar.style.width = progressVal + '%';
        
        if (progressVal > 30 && msgIdx === 0) msgIdx = 1;
        if (progressVal > 70 && msgIdx === 1) msgIdx = 2;
        if (progressText) progressText.textContent = messages[msgIdx];
    }, 200);

    api('/api/ai/template/generate', 'POST', { purpose, templateType, language })
        .then(res => {
            const contentArea = document.getElementById('tm_content');
            if (contentArea && res.content) {
                contentArea.value = res.content;
            }
            showToast('AI Template generated!', 'success');
        })
        .catch(err => {
            showToast('Generation failed: ' + err.message, 'error');
        })
        .finally(() => {
            clearInterval(progressInterval);
            if (progressBar) progressBar.style.width = '100%';
            setTimeout(() => {
                if (progressSection) progressSection.classList.add('hidden');
                if (btn) {
                    btn.disabled = false;
                    btn.innerHTML = '✨ Generate With AI';
                }
            }, 300);
        });
}

const purposeToModuleMap = {
    'Reminder': 'REMINDER',
    'Greeting': 'GREETING',
    'Promotion': 'PROMOTION',
    'Announcement': 'CAMPAIGN',
    'Follow Up': 'FOLLOWUP',
    'Custom': 'GENERAL'
};

const typeToCategoryMap = {
    'Payment Reminder': 'PAYMENT',
    'Subscription Renewal': 'EMI',
    'Membership Renewal': 'EMI',
    'Policy Renewal': 'POLICY',
    'Appointment Reminder': 'CUSTOM',
    'Service Reminder': 'CUSTOM',
    'Birthday': 'BIRTHDAY',
    'Anniversary': 'ANNIVERSARY',
    'Festival Greeting': 'CUSTOM',
    'Welcome Message': 'CUSTOM',
    'Offer Announcement': 'PROMOTION',
    'Discount Campaign': 'PROMOTION',
    'New Product Launch': 'PROMOTION',
    'Reactivation Campaign': 'PROMOTION',
    'Business Update': 'CUSTOM',
    'Holiday Notice': 'CUSTOM',
    'Timing Change': 'CUSTOM',
    'Service Update': 'CUSTOM',
    'Lead Follow Up': 'FOLLOWUP',
    'Customer Follow Up': 'FOLLOWUP',
    'Payment Follow Up': 'FOLLOWUP',
    'Custom': 'CUSTOM'
};

function saveTemplate() {
    const title = document.getElementById('tm_title').value.trim();
    const content = document.getElementById('tm_content').value.trim();
    if (!title || !content) { showToast('Title and content are required', 'error'); return; }

    const channels = [];
    const tmWa = document.getElementById('tm_wa');
    const tmSms = document.getElementById('tm_sms');
    const tmEmail = document.getElementById('tm_email');

    if (tmWa && tmWa.checked) channels.push('WhatsApp');
    else if (!tmWa) channels.push('WhatsApp'); // Safe default
    if (tmSms && tmSms.checked) channels.push('SMS');
    if (tmEmail && tmEmail.checked) channels.push('Email');

    const purposeVal = document.getElementById('tm_purpose')?.value || 'Reminder';
    const typeVal = document.getElementById('tm_type')?.value || 'Custom';

    console.log("Selected Purpose:", document.getElementById("tm_purpose").value);

    const purposeToDbMap = {
        'Reminder': 'REMINDER',
        'Greeting': 'GREETING',
        'Promotion': 'PROMOTION',
        'Announcement': 'ANNOUNCEMENT',
        'Follow Up': 'FOLLOW_UP',
        'Custom': 'CUSTOM'
    };

    const payload = {
        title,
        category: typeToCategoryMap[typeVal] || 'CUSTOM',
        description: document.getElementById('tm_desc').value.trim(),
        content,
        channels: channels.join(','),   // backend stores as comma-delimited string
        purpose: purposeToDbMap[purposeVal] || 'CUSTOM',
        templateType: typeVal,
        language: document.getElementById('tm_language')?.value || 'English',
        status: 'active',

        // New properties for dynamic picking
        name: title,
        moduleCode: purposeToModuleMap[purposeVal] || 'GENERAL',
        prompt: document.getElementById('tm_desc').value.trim(),
        active: true
    };

    console.log("Save Payload:", payload);

    const isEdit = editTemplateId !== null;
    api(isEdit ? `${API.templates}/${editTemplateId}` : API.templates, isEdit ? 'PUT' : 'POST', payload)
        .then((savedTemplate) => { 
            showToast(isEdit ? 'Template updated!' : 'Template saved!', 'success'); 
            closeTemplateModal(); 
            loadTemplates(); 
            
            const returnPage = sessionStorage.getItem("returnPage");
            if (returnPage) {
                if (typeof showFullLoader === 'function') {
                    showFullLoader('Saving Template...', 'Redirecting you back to your workspace.');
                }
                setTimeout(() => {
                    sessionStorage.setItem("selectedTemplate", JSON.stringify(savedTemplate));
                    sessionStorage.removeItem("returnPage");
                    window.location.href = returnPage;
                }, 800);
            }
        })
        .catch(err => showToast('Error: ' + err.message, 'error'));
}

function toggleTemplateStatus(id) {
    api(`${API.templates}/${id}/toggle-status`, 'PATCH')
        .then(() => { showToast('Template status updated', 'success'); loadTemplates(); })
        .catch(err => showToast('Error: ' + err.message, 'error'));
}
function deleteTemplate(id) {
    if (!confirm('Delete this template?')) return;
    api(`${API.templates}/${id}`, 'DELETE')
        .then(() => { showToast('Template deleted', 'success'); loadTemplates(); })
        .catch(err => showToast('Error: ' + err.message, 'error'));
}

// ─────────────────────────────────────────────────────────────────────────────
// ╔══════════════════════════════════════════════════════════╗
// ║                  REPORT TEMPLATES                       ║
// ╚══════════════════════════════════════════════════════════╝
// ─────────────────────────────────────────────────────────────────────────────

// Data state - This is now your source of truth
let reportRowsData = [{ test: "", range: "" }];

// ── Modified Preset Function ──────────────────────────────────────────────

function openRTWithPreset(type) {
    if (type === 'lab') {
        document.getElementById('rt_title').value = "Complete Blood Count";
        document.getElementById('rt_category').value = "Blood Test";
        reportRowsData = [
            { test: "Haemoglobin", range: "12-17 g/dL" },
            { test: "WBC Count", range: "4000-11000 /µL" },
            { test: "Platelet Count", range: "150000-400000 /µL" }
        ];
    } else {
        reportRowsData = [{ test: "", range: "" }];
    }
    renderRows();
    document.getElementById('rtModal').classList.remove('hidden');
}

// ── Modified Save Function ────────────────────────────────────────────────
function saveRTTemplate() {
    const title = document.getElementById('rt_title').value.trim();
    if (!title) { showToast('Title required', 'error'); return; }

    // Filter out empty rows and convert to JSON string
    const filteredRows = reportRowsData.filter(r => r.test.trim() !== '');
    const columnsJson = JSON.stringify(filteredRows);

    const payload = {
        title: title,
        accountId: 1, // Change this to your dynamic account ID logic
        category: document.getElementById('rt_category').value,
        description: document.getElementById('rt_desc').value.trim(),
        columns: columnsJson, // Storing structured JSON in the TEXT field
        price: parseFloat(document.getElementById('rt_price').value) || 0,
        showTotal: document.getElementById('rt_total').checked,
        status: 'active'
    };

    const isEdit = editRTId !== null;
    api(isEdit ? `${API.rtemplates}/${editRTId}` : API.rtemplates, isEdit ? 'PUT' : 'POST', payload)
        .then(() => {
            showToast('Template Saved!', 'success');
            closeRTModal();
            loadRTemplates();
        })
        .catch(err => showToast(err.message, 'error'));
}

// ── Updated Modal Opener (for Editing) ────────────────────────────────────
// Use this logic inside your existing edit function to convert string back to rows
function openRTModal(id = null) {
    editRTId = id;
    if (!id) {
        reportRowsData = [{ test: "", range: "" }];
        // clear other fields...
    } else {
        const t = rtemplates.find(x => x.id === id);
        if (t) {
            // ... fill other fields ...
            try {
                // Parse the JSON string from the database
                reportRowsData = JSON.parse(t.columns || "[]");
                if (reportRowsData.length === 0) reportRowsData = [{ test: "", range: "" }];
            } catch (e) {
                // Fallback for old comma-separated data
                reportRowsData = (t.columns || "").split(',').map(c => ({ test: c, range: '' }));
            }
        }
    }
    renderRows();
    document.getElementById('rtModal').classList.remove('hidden');
}

function closeRTModal() {
    // 1. Hide the modal
    const modal = document.getElementById('rtModal');
    if (modal) {
        modal.classList.add('hidden');
    }
    editRTId = null;
    reportRowsData = [{ test: "" }];

    // 4. Clear any specific validation styles or inputs if necessary
    document.getElementById('rt_title').value = '';
    document.getElementById('rt_price').value = '';
}
// ── Row Management Logic (Keep exactly as is) ─────────────────────────────

function renderRows() {
    const container = document.getElementById('reportRows');
    if (!container) return;
    container.innerHTML = '';

    if (reportRowsData.length === 0) {
        container.innerHTML = `<tr><td colspan="3" class="px-4 py-8 text-center text-gray-400 italic">No fields added.</td></tr>`;
        return;
    }

    reportRowsData.forEach((row, index) => {
        const tr = document.createElement('tr');
        tr.className = "fade-in hover:bg-gray-50/50 transition-colors";
        tr.innerHTML = `
      <td class="px-4 py-3">
        <input type="text" value="${row.test}" 
               oninput="updateRow(${index}, 'test', this.value)"
               placeholder="e.g. Haemoglobin"
               class="w-full px-3 py-2 rounded-xl border border-gray-200 text-sm focus:ring-2 focus:ring-indigo-500 bg-transparent">
      </td>
      <td class="px-4 py-3">
        <input type="text" value="${row.range}" 
               oninput="updateRow(${index}, 'range', this.value)"
               placeholder="e.g. 12-17 g/dL"
               class="w-full px-3 py-2 rounded-xl border border-gray-200 text-sm focus:ring-2 focus:ring-indigo-500 bg-transparent">
      </td>
      <td class="px-4 py-3 text-right">
        <button onclick="deleteRow(${index})" class="p-2 text-gray-400 hover:text-red-500">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
          </svg>
        </button>
      </td>
    `;
        container.appendChild(tr);
    });
}

function updateRow(index, field, value) {
    reportRowsData[index][field] = value;
}

function addRow() {
    reportRowsData.push({ test: "", range: "" });
    renderRows();
}

function deleteRow(index) {
    reportRowsData.splice(index, 1);
    renderRows();
}

function loadRTemplates() {
    api(API.rtemplates)
        .then(data => {
            rtemplates = data;
            renderRTemplates();
        })
        .catch(err => showToast('Failed to load report templates: ' + err.message, 'error'));
}

function renderRTemplates() {
    const grid = document.getElementById('rtGrid');
    const empty = document.getElementById('rtEmpty');
    const badge = document.getElementById('rtBadge');

    if (badge) badge.textContent = rtemplates.length;

    if (!rtemplates.length) {
        grid.innerHTML = '';
        if (empty) empty.classList.remove('hidden');
        return;
    }
    if (empty) empty.classList.add('hidden');

    const catColors = {
        'Blood Test': 'bg-red-50 text-red-700',
        'Radiology': 'bg-violet-50 text-violet-700',
        'Cardiology': 'bg-pink-50 text-pink-700',
        'Prescription': 'bg-emerald-50 text-emerald-700',
        'General Checkup': 'bg-blue-50 text-blue-700'
    };

    grid.innerHTML = rtemplates.map(t => {
        let cols = [];
        try {
            // Detect if it's our new JSON format or the old comma-style
            if (t.columns && t.columns.startsWith('[')) {
                cols = JSON.parse(t.columns);
            } else {
                cols = (t.columns || "").split(',').map(c => ({ test: c.trim(), range: '' }));
            }
        } catch (e) {
            cols = (t.columns || "").split(',').map(c => ({ test: c.trim(), range: '' }));
        }

        const catCls = catColors[t.category] || 'bg-gray-100 text-gray-600';
        const isActive = t.status === 'active';

        return `
    <div class="group bg-white rounded-2xl border border-gray-100 shadow-sm p-5 hover:shadow-md transition fade-in">
      <div class="flex items-start justify-between mb-3">
        <div class="flex items-start gap-3">
          <div class="w-10 h-10 rounded-xl bg-indigo-50 flex items-center justify-center flex-shrink-0 text-xl">📋</div>
          <div>
            <p class="text-sm font-bold text-gray-900 leading-snug">${t.title}</p>
            <span class="inline-block mt-0.5 px-2 py-0.5 rounded-full text-[10px] font-semibold ${catCls}">${t.category || 'Custom'}</span>
          </div>
        </div>
        
        <div class="flex gap-1 opacity-0 group-hover:opacity-100 transition flex-shrink-0">
          <button onclick="openRTModal(${t.id})" title="Edit"
            class="w-7 h-7 rounded-lg bg-gray-100 hover:bg-indigo-50 hover:text-indigo-600 flex items-center justify-center text-gray-500 transition">
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z"/></svg>
          </button>
          <button onclick="deleteRT(${t.id})" title="Delete"
            class="w-7 h-7 rounded-lg bg-red-50 hover:bg-red-100 flex items-center justify-center text-red-400 transition">
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/></svg>
          </button>
        </div>
      </div>

      <div class="flex flex-wrap gap-1.5 mb-3">
        ${cols.map(c => `
          <span class="text-[10px] px-2 py-1 rounded-lg bg-gray-100 text-gray-600 font-medium border border-gray-200/50">
            ${c.test} ${c.range ? `<span class="text-indigo-500 ml-1 opacity-70">[${c.range}]</span>` : ''}
          </span>
        `).join('')}
      </div>

      <div class="flex items-center justify-between pt-3 border-t border-gray-50">
        <div class="flex items-center gap-2">
          <span class="text-[10px] text-gray-400">${cols.length} fields</span>
          <span class="px-2 py-0.5 rounded-full text-[10px] font-bold ${isActive ? 'bg-emerald-50 text-emerald-600' : 'bg-gray-100 text-gray-400'}">${isActive ? 'Active' : 'Inactive'}</span>
        </div>
        <p class="text-sm font-bold text-emerald-600">
          ${t.price > 0 ? '₹' + Number(t.price).toLocaleString('en-IN') : 'Free'}
        </p>
      </div>
    </div>`;
    }).join('');
}

function deleteRT(id) {
    // 1. Ask for confirmation to prevent accidental clicks
    if (!confirm('Are you sure you want to delete this report template? This action cannot be undone.')) {
        return;
    }

    // 2. Call the backend API
    // Assumes API.rtemplates is your endpoint (e.g., '/api/catalog/report-templates')
    api(`${API.rtemplates}/${id}`, 'DELETE')
        .then(() => {
            // 3. Show success notification
            showToast('Template deleted successfully', 'success');

            // 4. Refresh the local list and UI
            loadRTemplates();
        })
        .catch(err => {
            // 5. Handle errors (e.g., template in use, network issues)
            console.error('Delete error:', err);
            showToast('Failed to delete template: ' + err.message, 'error');
        });
}

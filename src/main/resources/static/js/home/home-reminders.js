let rem_accountId = null;

async function rem_getAccountId() {
  try {
    const res = await fetch('/api/account/current');
    if (res.ok) {
      const data = await res.json();
      rem_accountId = data.id || data.accountId;
    }
  } catch (err) {
    console.error('Failed to get accountId', err);
  }
  if (!rem_accountId) {
    rem_accountId = parseInt(
      document.querySelector('meta[name="account-id"]')
      ?.content || 0);
  }
}

let rem_allReminders = [];    // all reminders from API
let rem_loaded = false;

// Reminders tab state
let rem_filter = 'all';       // all/pending/sent/failed
let rem_search = '';
let rem_page = 1;
const REM_DASH_PER_PAGE = 6;
let rem_searchTimer = null;

// Followups tab state  
let fu_filter = 'all';
let fu_search = '';
let fu_dateFrom = '';
let fu_dateTo = '';
let fu_page = 1;
const FU_PER_PAGE = 6;
let fu_searchTimer = null;

// Collect modal state
let rem_collectEntryId = null;
let rem_collectMode = 'Cash';
let rem_collectIsLast = false;

// Message preview state
let rem_previewPhone = '';
let rem_previewText = '';

function rem_initReminders() {
  if (rem_loaded && rem_allReminders.length > 0) {
    rem_renderRemindersTab();
    rem_renderFollowupsTab();
    return;
  }
  rem_loaded = true;
  rem_getAccountId().then(() => {
    rem_loadAll();
  });
}

async function rem_loadAll() {
  if (!rem_accountId) {
    console.warn('rem_loadAll: no accountId');
    return;
  }
  
  // Show skeletons
  rem_showSkeleton('reminderList');
  rem_showSkeleton('followupList');

  try {
    const res = await fetch(
      `/api/engagements/reminders/account/` +
      `${rem_accountId}/list`);
    if (!res.ok) throw new Error('Failed');
    rem_allReminders = await res.json();
    
    rem_updateSummaryCards();
    rem_renderRemindersTab();
    rem_renderFollowupsTab();

  } catch (err) {
    console.error(err);
    showToast('Failed to load reminders');
    rem_showError('reminderList');
    rem_showError('followupList');
  }
}

function rem_updateSummaryCards() {
  const sentPeriod = document.getElementById(
    'rem-drop-sent')?.value || 'today';
  const pendingPeriod = document.getElementById(
    'rem-drop-pending')?.value || 'week';
  const upcomingPeriod = document.getElementById(
    'rem-drop-upcoming')?.value || 'week';
  const failedPeriod = document.getElementById(
    'rem-drop-failed')?.value || 'week';

  const today = new Date();
  today.setHours(0,0,0,0);

  function inPeriod(dateStr, period) {
    if (!dateStr) return false;
    const d = new Date(dateStr);
    d.setHours(0,0,0,0);
    const now = new Date();
    now.setHours(0,0,0,0);

    if (period === 'today') {
      return d.toDateString() === now.toDateString();
    }
    if (period === 'week') {
      const mon = new Date(now);
      mon.setDate(now.getDate() - 
        ((now.getDay() + 6) % 7));
      const sun = new Date(mon);
      sun.setDate(mon.getDate() + 6);
      return d >= mon && d <= sun;
    }
    if (period === 'month') {
      return d.getMonth() === now.getMonth() &&
        d.getFullYear() === now.getFullYear();
    }
    if (period === 'year') {
      return d.getFullYear() === now.getFullYear();
    }
    return false;
  }

  const sent = rem_allReminders.filter(r =>
    r.status === 'Sent' && 
    inPeriod(r.reminderDate, sentPeriod)).length;

  const pending = rem_allReminders.filter(r =>
    (r.status === 'Pending' || r.status === 'Scheduled') &&
    inPeriod(r.reminderDate, pendingPeriod)).length;

  const upcoming = rem_allReminders.filter(r => {
    const d = new Date(r.reminderDate);
    d.setHours(0,0,0,0);
    const now = new Date();
    now.setHours(0,0,0,0);
    return d >= now && 
      inPeriod(r.reminderDate, upcomingPeriod);
  }).length;

  const failed = rem_allReminders.filter(r =>
    r.status === 'Failed' &&
    inPeriod(r.reminderDate, failedPeriod)).length;

  const sentEl = document.getElementById('rem-card-sent');
  const pendEl = document.getElementById('rem-card-pending');
  const upEl = document.getElementById('rem-card-upcoming');
  const failEl = document.getElementById('rem-card-failed');

  if (sentEl) sentEl.textContent = sent;
  if (pendEl) pendEl.textContent = pending;
  if (upEl) upEl.textContent = upcoming;
  if (failEl) failEl.textContent = failed;
}

function rem_renderRemindersTab() {
  const filtered = rem_allReminders.filter(r => {
    // Status filter
    if (rem_filter !== 'all') {
      const s = r.status?.toLowerCase() || '';
      if (rem_filter === 'pending' && 
          s !== 'pending' && s !== 'scheduled') 
        return false;
      if (rem_filter === 'sent' && s !== 'sent') 
        return false;
      if (rem_filter === 'failed' && s !== 'failed') 
        return false;
    }
    // Search
    if (rem_search) {
      const q = rem_search.toLowerCase();
      return (r.customerName || '').toLowerCase()
               .includes(q) ||
             (r.title || '').toLowerCase().includes(q);
    }
    return true;
  });

  const total = filtered.length;
  const start = (rem_page - 1) * REM_DASH_PER_PAGE;
  const pageItems = filtered.slice(
    start, start + REM_DASH_PER_PAGE);

  const list = document.getElementById('reminderList');
  if (!list) return;

  if (total === 0) {
    list.innerHTML = rem_emptyState(
      'No reminders found', 
      'Try a different filter or search term');
    rem_renderPagination('rem', 0, 0, 0);
    return;
  }

  list.innerHTML = pageItems.map(r => 
    rem_renderReminderCard(r)).join('');

  rem_renderPagination('rem', rem_page, total, 
    REM_DASH_PER_PAGE);
}

function rem_renderReminderCard(r) {
  const isPayment = r.reminderPurpose === 'payment';
  
  const purposeBadge = {
    payment:  'bg-emerald-100 text-emerald-700',
    service:  'bg-blue-100 text-blue-700',
    followup: 'bg-pink-100 text-pink-700',
    general:  'bg-gray-100 text-gray-600'
  }[r.reminderPurpose] || 'bg-gray-100 text-gray-600';

  const purposeLabel = {
    payment:  '💰 Payment',
    service:  '🔧 Service',
    followup: '📞 Follow-up',
    general:  '📝 General'
  }[r.reminderPurpose] || r.reminderPurpose || 'General';

  const statusBadge = {
    Scheduled: 'bg-blue-100 text-blue-700',
    Pending:   'bg-amber-100 text-amber-700',
    Sent:      'bg-emerald-100 text-emerald-700',
    Failed:    'bg-red-100 text-red-600'
  }[r.status] || 'bg-gray-100 text-gray-600';

  const phone = (r.customerMobile || '')
    .replace(/\D/g, '');

  const amtHtml = isPayment && r.amount
    ? `<span class="text-xs font-bold text-gray-900">
        ₹${r.amount}
       </span>`
    : '';

  const collectBtn = isPayment
    ? `<button onclick="rem_openCollectForReminder(
        ${r.id},'${(r.customerName || '').replace(/'/g, "\\'")}',${r.amount||0})"
        class="px-2.5 py-1.5 text-xs font-semibold 
        bg-emerald-600 text-white rounded-lg 
        hover:bg-emerald-700 transition flex-shrink-0">
        Collect
       </button>`
    : `<button onclick="rem_sendNowReminder(${r.id})"
        class="px-2.5 py-1.5 text-xs font-semibold 
        bg-blue-600 text-white rounded-lg 
        hover:bg-blue-700 transition flex-shrink-0">
        Send Now
       </button>`;

  const waBtn = phone
    ? `<button onclick="rem_openMsgPreview(
        ${r.id},'${phone}','${
          (r.message||'').replace(/'/g,"\\'"
          ).replace(/\n/g,'\\n')}')"
        class="p-1.5 rounded-lg hover:bg-green-50 
        text-green-500 transition" title="WhatsApp">
        <svg class="w-4 h-4" fill="currentColor" 
          viewBox="0 0 24 24">
          <path d="M17.472 14.382c-.297-.149-1.758-.867
            -2.03-.967-.273-.099-.471-.148-.67.15-.197
            .297-.767.966-.94 1.164-.173.199-.347.223
            -.644.075-.297-.15-1.255-.463-2.39-1.475
            -.883-.788-1.48-1.761-1.653-2.059-.173-.297
            -.018-.458.13-.606.134-.133.298-.347.446-.52
            .149-.174.198-.298.298-.497.099-.198.05-.371
            -.025-.52-.075-.149-.669-1.612-.916-2.207
            -.242-.579-.487-.5-.669-.51-.173-.008-.371
            -.01-.57-.01-.198 0-.52.074-.792.372-.272
            .297-1.04 1.016-1.04 2.479 0 1.462 1.065 
            2.875 1.213 3.074.149.198 2.096 3.2 5.077 
            4.487.709.306 1.262.489 1.694.625.712.227 
            1.36.195 1.871.118.571-.085 1.758-.719 
            2.006-1.413.248-.694.248-1.289.173-1.413
            -.074-.124-.272-.198-.57-.347m-5.421 
            7.403h-.004a9.87 9.87 0 01-5.031-1.378l
            -.361-.214-3.741.982.998-3.648-.235-.374a
            9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436
            -9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 
            2.898a9.825 9.825 0 012.893 6.994c-.003 
            5.45-4.437 9.884-9.885 9.884m8.413-18.297
            A11.815 11.815 0 0012.05 0C5.495 0 .16 
            5.335.157 11.892c0 2.096.547 4.142 1.588 
            5.945L.057 24l6.305-1.654a11.882 11.882 0 
            005.683 1.448h.005c6.554 0 11.89-5.335 
            11.893-11.893a11.821 11.821 0 00-3.48-8.413z"
          />
        </svg>
       </button>`
    : '';

  const callBtn = phone
    ? `<a href="tel:+91${phone}"
        class="p-1.5 rounded-lg hover:bg-blue-50 
        text-blue-500 transition" title="Call">
        <svg class="w-4 h-4" fill="none" 
          stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" 
            stroke-linejoin="round" stroke-width="2"
            d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684
            l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13
            a11.042 11.042 0 005.516 5.516l1.13-2.257a1 
            1 0 011.21-.502l4.493 1.498a1 1 0 01.684
            .949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 
            3 6V5z"/>
        </svg>
       </a>`
    : '';

  const dueDate = r.reminderDate
    ? new Date(r.reminderDate).toLocaleDateString(
        'en-IN', {day:'2-digit', month:'short', 
                  year:'numeric'})
    : '—';

  return `
    <div class="flex items-center gap-3 bg-white 
      border border-gray-100 rounded-xl p-3 
      hover:shadow-sm transition cursor-pointer"
      onclick="if(!event.target.closest('button') && 
        !event.target.closest('a')) 
        window.location.href=
        'reminder-detail.html?id=${r.id}'">
      
      <div class="w-9 h-9 rounded-xl bg-violet-50 
        flex items-center justify-center flex-shrink-0">
        <svg class="w-4 h-4 text-violet-600" fill="none"
          stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" 
            stroke-linejoin="round" stroke-width="2"
            d="M15 17h5l-1.405-1.405A2.032 2.032 0 
            0118 14.158V11a6.002 6.002 0 00-4-5.659V5
            a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 
            11v3.159c0 .538-.214 1.055-.595 1.436L4 
            17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"/>
        </svg>
      </div>

      <div class="flex-1 min-w-0">
        <div class="flex items-center gap-2 flex-wrap 
          mb-0.5">
          <p class="text-sm font-semibold text-gray-900 
            truncate">${r.customerName || 'Unknown'}</p>
          <span class="text-[10px] px-1.5 py-0.5 
            rounded-full font-medium ${purposeBadge}">
            ${purposeLabel}
          </span>
          <span class="text-[10px] px-1.5 py-0.5 
            rounded-full font-medium ${statusBadge}">
            ${r.status || 'Pending'}
          </span>
        </div>
        <p class="text-xs text-gray-500 truncate">
          ${r.title || '—'}
        </p>
        <div class="flex items-center gap-3 mt-0.5 
          flex-wrap">
          <span class="text-[10px] text-gray-400">
            📅 ${dueDate}
          </span>
          ${amtHtml}
          ${r.recurring 
            ? `<span class="text-[10px] text-purple-500">
               🔄 ${r.frequency || 'Recurring'}
               </span>` 
            : ''}
        </div>
      </div>

      <div class="flex items-center gap-1 flex-shrink-0">
        ${collectBtn}
        ${waBtn}
        ${callBtn}
      </div>
    </div>`;
}

async function rem_sendNowReminder(reminderId) {
  try {
    const res = await fetch(`/api/engagements/reminders/${reminderId}/send`, {
      method: 'POST'
    });
    if (res.ok) {
      showToast('Reminder sent successfully! 🚀');
      await rem_loadAll();
    } else {
      const errText = await res.text();
      showToast('Failed to send: ' + errText);
    }
  } catch (err) {
    console.error(err);
    showToast('Failed to send reminder');
  }
}

async function rem_openCollectForReminder(
    reminderId, customerName, defaultAmount) {

  try {
    const res = await fetch(
      `/api/engagements/reminders/${reminderId}/tracker`);
    if (!res.ok) throw new Error('Failed');
    const entries = await res.json();

    // Find first pending entry
    const pending = entries
      .filter(e => e.status === 'PENDING' || 
                   e.status === 'OVERDUE')
      .sort((a,b) => new Date(a.occurrenceDate) - 
                     new Date(b.occurrenceDate));

    if (pending.length === 0) {
      showToast('No pending payments for this reminder');
      return;
    }

    const entry = pending[0];
    const isLast = entries[entries.length - 1].id 
      === entry.id;
    const originalAmt = entry.amount || defaultAmount || 0;
    const paidAmt = entry.paidAmount || 0;
    const balance = originalAmt - paidAmt;

    rem_collectEntryId = entry.id;
    rem_collectIsLast = isLast;
    rem_collectMode = 'Cash';

    document.getElementById('rem-collect-customer')
      .textContent = customerName;
    document.getElementById('rem-collect-due')
      .textContent = '₹' + originalAmt;
    document.getElementById('rem-collect-paid')
      .textContent = '₹' + paidAmt;
    document.getElementById('rem-collect-balance')
      .textContent = '₹' + balance;
    document.getElementById('rem-collect-amount')
      .value = '';
    document.getElementById('rem-collect-date')
      .value = new Date().toISOString().split('T')[0];
    document.getElementById('rem-collect-ref').value = '';
    document.getElementById('rem-collect-notes').value = '';
    document.getElementById('rem-collect-error')
      .classList.add('hidden');

    const lastWarn = document.getElementById(
      'rem-collect-last-warn');
    if (lastWarn) {
      lastWarn.classList.toggle('hidden', !isLast);
    }

    rem_selectCollectMode(null, 'Cash');
    document.getElementById('rem-collect-modal')
      .classList.remove('hidden');

  } catch (err) {
    console.error(err);
    showToast('Failed to load payment details');
  }
}

function rem_selectCollectMode(btn, mode) {
  rem_collectMode = mode;
  document.querySelectorAll('.rem-mode-btn')
    .forEach(b => {
      b.className = 'rem-mode-btn py-2 rounded-xl ' +
        'text-xs font-semibold border-2 border-gray-200 ' +
        'text-gray-600 transition';
    });
  if (btn) {
    btn.className = 'rem-mode-btn py-2 rounded-xl ' +
      'text-xs font-semibold border-2 border-emerald-500 ' +
      'bg-emerald-50 text-emerald-700 transition';
  } else {
    const first = document.querySelector('.rem-mode-btn');
    if (first) {
      first.className = 'rem-mode-btn py-2 rounded-xl ' +
        'text-xs font-semibold border-2 border-emerald-500 '+
        'bg-emerald-50 text-emerald-700 transition';
    }
  }
}

async function rem_submitCollect() {
  const amtInput = document.getElementById(
    'rem-collect-amount');
  const errEl = document.getElementById('rem-collect-error');
  const btn = document.getElementById('rem-collect-submit');
  
  const amountCollected = parseFloat(amtInput.value);
  const balText = document.getElementById('rem-collect-balance')
    .textContent.replace('₹','');
  const balance = parseFloat(balText);

  errEl.classList.add('hidden');

  if (!amountCollected || amountCollected <= 0) {
    errEl.textContent = 'Please enter a valid amount';
    errEl.classList.remove('hidden');
    return;
  }
  if (amountCollected > balance) {
    errEl.textContent = 
      'Cannot exceed balance of ₹' + balance;
    errEl.classList.remove('hidden');
    return;
  }
  if (rem_collectIsLast && amountCollected < balance) {
    errEl.textContent = 
      'Last installment — full amount ₹' + 
      balance + ' required';
    errEl.classList.remove('hidden');
    return;
  }

  btn.disabled = true;
  btn.textContent = 'Processing...';

  try {
    const res = await fetch(
      `/api/engagements/schedule-entry/` +
      `${rem_collectEntryId}/collect`,
      {
        method: 'POST',
        headers: {'Content-Type':'application/json'},
        body: JSON.stringify({
          amountCollected,
          paymentMode: rem_collectMode,
          paymentDate: document.getElementById(
            'rem-collect-date').value,
          referenceNo: document.getElementById(
            'rem-collect-ref').value,
          notes: document.getElementById(
            'rem-collect-notes').value
        })
      });

    const data = await res.json();
    if (!res.ok) {
      errEl.textContent = data.error || 'Failed';
      errEl.classList.remove('hidden');
      return;
    }

    document.getElementById('rem-collect-modal')
      .classList.add('hidden');
    showToast('Payment collected ✅');
    await rem_loadAll();

  } catch (err) {
    errEl.textContent = 'Something went wrong';
    errEl.classList.remove('hidden');
  } finally {
    btn.disabled = false;
    btn.textContent = 'Mark as Paid';
  }
}

function rem_openMsgPreview(reminderId, phone, message) {
  rem_previewPhone = phone;
  rem_previewText = message;

  const modal = document.getElementById('rem-msg-modal');
  document.getElementById('rem-msg-phone')
    .textContent = '+' + phone;
  document.getElementById('rem-msg-content')
    .textContent = message;
  document.getElementById('rem-msg-edit').value = message;
  document.getElementById('rem-msg-edit')
    .classList.add('hidden');
  document.getElementById('rem-msg-content')
    .classList.remove('hidden');
  document.getElementById('rem-msg-toggle')
    .textContent = '✏️ Edit message';

  const waLink = document.getElementById('rem-msg-wa-link');
  waLink.href = `https://wa.me/${phone}` +
    `?text=${encodeURIComponent(message)}`;

  modal.classList.remove('hidden');
}

function rem_closeMsgPreview() {
  document.getElementById('rem-msg-modal')
    .classList.add('hidden');
}

function rem_toggleMsgEdit() {
  const preview = document.getElementById('rem-msg-content');
  const edit = document.getElementById('rem-msg-edit');
  const btn = document.getElementById('rem-msg-toggle');
  const isEdit = !edit.classList.contains('hidden');

  if (isEdit) {
    // Switch to preview
    preview.textContent = edit.value;
    preview.classList.remove('hidden');
    edit.classList.add('hidden');
    btn.textContent = '✏️ Edit message';
    const waLink = document.getElementById(
      'rem-msg-wa-link');
    waLink.href = `https://wa.me/${rem_previewPhone}` +
      `?text=${encodeURIComponent(edit.value)}`;
  } else {
    // Switch to edit
    preview.classList.add('hidden');
    edit.classList.remove('hidden');
    btn.textContent = '👁 View preview';
  }
}

function rem_renderFollowupsTab() {
  let filtered = rem_allReminders.filter(r =>
    r.reminderPurpose === 'followup' ||
    r.reminderPurpose === 'service'
  );

  // Apply date range filter
  if (fu_dateFrom) {
    const from = new Date(fu_dateFrom);
    filtered = filtered.filter(r => 
      r.reminderDate && new Date(r.reminderDate) >= from);
  }
  if (fu_dateTo) {
    const to = new Date(fu_dateTo);
    filtered = filtered.filter(r =>
      r.reminderDate && new Date(r.reminderDate) <= to);
  }

  // Apply status filter
  if (fu_filter !== 'all') {
    filtered = filtered.filter(r => {
      if (fu_filter === 'high') return false;
      return true;
    });
  }

  // Apply search
  if (fu_search) {
    const q = fu_search.toLowerCase();
    filtered = filtered.filter(r =>
      (r.customerName||'').toLowerCase().includes(q) ||
      (r.title||'').toLowerCase().includes(q));
  }

  // Sort by date ascending
  filtered.sort((a,b) => 
    new Date(a.reminderDate) - new Date(b.reminderDate));

  const total = filtered.length;
  const start = (fu_page - 1) * FU_PER_PAGE;
  const pageItems = filtered.slice(
    start, start + FU_PER_PAGE);

  const list = document.getElementById('followupList');
  if (!list) return;

  if (total === 0) {
    list.innerHTML = rem_emptyState(
      'No follow-ups found',
      'Try a different filter or date range');
    rem_renderPagination('fu', 0, 0, 0);
    return;
  }

  list.innerHTML = pageItems.map(r => 
    rem_renderFollowupCard(r)).join('');

  rem_renderPagination('fu', fu_page, total, FU_PER_PAGE);
}

function rem_renderFollowupCard(r) {
  const today = new Date();
  today.setHours(0,0,0,0);
  const dueDate = r.reminderDate 
    ? new Date(r.reminderDate) : null;
  const isOverdue = dueDate && dueDate < today;
  const isToday = dueDate && 
    dueDate.toDateString() === today.toDateString();

  const dueDateStr = dueDate
    ? dueDate.toLocaleDateString('en-IN', 
        {day:'2-digit', month:'short', year:'numeric'})
    : '—';

  const dueBadge = isOverdue
    ? 'bg-red-100 text-red-600'
    : isToday
    ? 'bg-amber-100 text-amber-700'
    : 'bg-blue-100 text-blue-700';

  const dueLabel = isOverdue ? 'Overdue' 
    : isToday ? 'Due Today' : dueDateStr;

  const phone = (r.customerMobile || '')
    .replace(/\D/g, '');

  const purposeIcon = r.reminderPurpose === 'service'
    ? '🔧' : '📞';

  return `
    <div class="flex items-start gap-3 bg-white 
      border ${isOverdue ? 'border-red-100 bg-red-50/30' 
        : 'border-gray-100'} 
      rounded-xl p-3 hover:shadow-sm transition 
      cursor-pointer"
      onclick="if(!event.target.closest('button') && 
        !event.target.closest('a'))
        window.location.href=
        'reminder-detail.html?id=${r.id}'">

      <div class="w-9 h-9 rounded-xl 
        ${isOverdue ? 'bg-red-50' : 'bg-pink-50'}
        flex items-center justify-center 
        flex-shrink-0 text-base">
        ${purposeIcon}
      </div>

      <div class="flex-1 min-w-0">
        <div class="flex items-center gap-2 
          flex-wrap mb-0.5">
          <p class="text-sm font-semibold text-gray-900">
            ${r.customerName || 'Unknown'}
          </p>
          <span class="text-[10px] px-1.5 py-0.5 
            rounded-full font-medium ${dueBadge}">
            ${dueLabel}
          </span>
          ${r.recurring 
            ? `<span class="text-[10px] text-purple-500">
               🔄 ${r.frequency}
               </span>` 
            : ''}
        </div>
        <p class="text-xs text-gray-600 font-medium 
          truncate">${r.title || '—'}</p>
        <p class="text-[10px] text-gray-400 mt-0.5 
          line-clamp-1">${r.message || ''}</p>
      </div>

      <div class="flex items-center gap-1 flex-shrink-0">
        ${phone 
          ? `<button onclick="rem_openMsgPreview(
              ${r.id},'${phone}',
              '${(r.message||'').replace(/'/g,"\\'"
                ).replace(/\n/g,'\\n')}')"
              class="p-1.5 rounded-lg hover:bg-green-50 
              text-green-500 transition">
              <svg class="w-4 h-4" fill="currentColor" 
                viewBox="0 0 24 24">
                <path d="M17.472 14.382c-.297-.149
                  -1.758-.867-2.03-.967-.273-.099-.471
                  -.148-.67.15-.197.297-.767.966-.94 
                  1.164-.173.199-.347.223-.644.075-.297
                  -.15-1.255-.463-2.39-1.475-.883-.788
                  -1.48-1.761-1.653-2.059-.173-.297-.018
                  -.458.13-.606.134-.133.298-.347.446-.52
                  .149-.174.198-.298.298-.497.099-.198.05
                  -.371-.025-.52-.075-.149-.669-1.612-.916
                  -2.207-.242-.579-.487-.5-.669-.51-.173
                  -.008-.371-.01-.57-.01-.198 0-.52.074
                  -.792.372-.272.297-1.04 1.016-1.04 
                  2.479 0 1.462 1.065 2.875 1.213 3.074
                  .149.198 2.096 3.2 5.077 4.487.709.306 
                  1.262.489 1.694.625.712.227 1.36.195 
                  1.871.118.571-.085 1.758-.719 2.006
                  -1.413.248-.694.248-1.289.173-1.413
                  -.074-.124-.272-.198-.57-.347m-5.421 
                  7.403h-.004a9.87 9.87 0 01-5.031-1.378
                  l-.361-.214-3.741.982.998-3.648-.235
                  -.374a9.86 9.86 0 01-1.51-5.26c.001
                  -5.45 4.436-9.884 9.888-9.884 2.64 0 
                  5.122 1.03 6.988 2.898a9.825 9.825 0 
                  012.893 6.994c-.003 5.45-4.437 9.884
                  -9.885 9.884m8.413-18.297A11.815 
                  11.815 0 0012.05 0C5.495 0 .16 5.335
                  .157 11.892c0 2.096.547 4.142 1.588 
                  5.945L.057 24l6.305-1.654a11.882 
                  11.882 0 005.683 1.448h.005c6.554 0 
                  11.89-5.335 11.893-11.893a11.821 
                  11.821 0 00-3.48-8.413z"/>
              </svg>
             </button>
             <a href="tel:+91${phone}"
              class="p-1.5 rounded-lg hover:bg-blue-50 
              text-blue-500 transition">
              <svg class="w-4 h-4" fill="none" 
                stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" 
                  stroke-linejoin="round" 
                  stroke-width="2"
                  d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948
                  .684l1.498 4.493a1 1 0 01-.502 1.21l
                  -2.257 1.13a11.042 11.042 0 005.516 
                  5.516l1.13-2.257a1 1 0 011.21-.502l
                  4.493 1.498a1 1 0 01.684.949V19a2 2 0 
                  01-2 2h-1C9.716 21 3 14.284 3 6V5z"/>
              </svg>
             </a>`
          : ''
        }
      </div>
    </div>`;
}

function rem_renderPagination(prefix, page, total, perPage){
  const btnsEl = document.getElementById(
    'pg-' + prefix + '-btns');
  const infoEl = document.getElementById(
    'pg-' + prefix + '-info');
  const prevEl = document.getElementById(
    'pg-' + prefix + '-prev');
  const nextEl = document.getElementById(
    'pg-' + prefix + '-next');

  if (!btnsEl) return;

  const pages = Math.ceil(total / perPage);
  
  if (infoEl) {
    const from = total === 0 ? 0 : 
      (page - 1) * perPage + 1;
    const to = Math.min(page * perPage, total);
    infoEl.textContent = total === 0
      ? 'No results'
      : `Showing ${from}–${to} of ${total}`;
  }

  if (prevEl) prevEl.disabled = page <= 1;
  if (nextEl) nextEl.disabled = page >= pages;

  btnsEl.innerHTML = '';
  for (let p = 1; p <= Math.min(pages, 5); p++) {
    const btn = document.createElement('button');
    btn.textContent = p;
    btn.className = p === page
      ? 'w-7 h-7 rounded-lg text-xs font-medium ' +
        'bg-indigo-600 text-white'
      : 'w-7 h-7 rounded-lg text-xs font-medium ' +
        'border border-gray-200 text-gray-500 ' +
        'hover:bg-gray-50';
    btn.onclick = () => {
      if (prefix === 'rem') {
        rem_page = p;
        rem_renderRemindersTab();
      } else if (prefix === 'fu') {
        fu_page = p;
        rem_renderFollowupsTab();
      }
    };
    btnsEl.appendChild(btn);
  }
}

function rem_paginate(prefix, dir) {
  if (prefix === 'rem') {
    if (dir === 'prev' && rem_page > 1) rem_page--;
    if (dir === 'next') rem_page++;
    rem_renderRemindersTab();
  } else if (prefix === 'fu') {
    if (dir === 'prev' && fu_page > 1) fu_page--;
    if (dir === 'next') fu_page++;
    rem_renderFollowupsTab();
  }
}

function rem_setStatusFilter(filter) {
  rem_filter = filter;
  rem_page = 1;
  rem_renderRemindersTab();
}

function rem_onSearch(val) {
  clearTimeout(rem_searchTimer);
  rem_searchTimer = setTimeout(() => {
    rem_search = val.toLowerCase().trim();
    rem_page = 1;
    rem_renderRemindersTab();
  }, 400);
}

function fu_setFilter(filter) {
  fu_filter = filter;
  fu_page = 1;
  rem_renderFollowupsTab();
}

function fu_onSearch(val) {
  clearTimeout(fu_searchTimer);
  fu_searchTimer = setTimeout(() => {
    fu_search = val.toLowerCase().trim();
    fu_page = 1;
    rem_renderFollowupsTab();
  }, 400);
}

function fu_setDateRange(from, to) {
  fu_dateFrom = from;
  fu_dateTo = to;
  fu_page = 1;
  rem_renderFollowupsTab();
}

function rem_showSkeleton(containerId) {
  const el = document.getElementById(containerId);
  if (!el) return;
  el.innerHTML = [1,2,3].map(() => `
    <div class="animate-pulse bg-white rounded-xl 
      border border-gray-100 p-3">
      <div class="flex items-center gap-3">
        <div class="w-9 h-9 rounded-xl bg-gray-100 
          flex-shrink-0"></div>
        <div class="flex-1 space-y-2">
          <div class="h-3 bg-gray-100 rounded-full 
            w-2/3"></div>
          <div class="h-2.5 bg-gray-100 rounded-full 
            w-1/2"></div>
          <div class="h-2 bg-gray-100 rounded-full 
            w-1/3"></div>
        </div>
        <div class="w-16 h-7 bg-gray-100 rounded-lg 
          flex-shrink-0"></div>
      </div>
    </div>`).join('');
}

function rem_emptyState(title, subtitle) {
  return `
    <div class="text-center py-10 bg-white rounded-xl 
      border border-gray-100">
      <div class="w-12 h-12 rounded-2xl bg-gray-100 
        flex items-center justify-center mx-auto mb-3">
        <svg class="w-6 h-6 text-gray-300" fill="none"
          stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" 
            stroke-linejoin="round" stroke-width="1.5"
            d="M15 17h5l-1.405-1.405A2.032 2.032 0 
            0118 14.158V11a6.002 6.002 0 00-4-5.659V5
            a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 
            11v3.159c0 .538-.214 1.055-.595 1.436L4 
            17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"/>
        </svg>
      </div>
      <p class="text-sm font-semibold text-gray-500">
        ${title}
      </p>
      <p class="text-xs text-gray-400 mt-1">${subtitle}</p>
    </div>`;
}

function rem_showError(containerId) {
  const el = document.getElementById(containerId);
  if (!el) return;
  el.innerHTML = `
    <div class="text-center py-10">
      <p class="text-sm text-red-500 font-medium">
        ⚠️ Failed to load
      </p>
      <button onclick="rem_loadAll()" 
        class="mt-2 text-xs text-indigo-600 
        hover:underline">Try Again</button>
    </div>`;
}

// ── Dashboard Appointments Tab JavaScript Logic ──────────────────────────────

// Global state variables
let dashAppts = [];
let dash_filter = 'all';
let dash_search = '';
let dash_from = '';
let dash_to = '';
let dash_page = 1;
const DASH_PER_PAGE = 6;
let dash_searchTimer = null;
let dash_loaded = false;

// Monday to Sunday (Mon-Sun) of current week
function dash_getWeekRange() {
    const today = new Date();
    const currentDay = today.getDay();
    const distanceToMon = currentDay === 0 ? -6 : 1 - currentDay;
    
    const mon = new Date(today);
    mon.setDate(today.getDate() + distanceToMon);
    
    const sun = new Date(mon);
    sun.setDate(mon.getDate() + 6);
    
    const format = (d) => d.toISOString().split('T')[0];
    return { start: format(mon), end: format(sun) };
}

// 1st to last day of month. offset=0 (this month), offset=1 (next month)
function dash_getMonthRange(offset) {
    const today = new Date();
    const year = today.getFullYear();
    const month = today.getMonth() + offset;
    
    const start = new Date(year, month, 1);
    const end = new Date(year, month + 1, 0);
    
    const format = (d) => {
        const y = d.getFullYear();
        const m = String(d.getMonth() + 1).padStart(2, '0');
        const r = String(d.getDate()).padStart(2, '0');
        return `${y}-${m}-${r}`;
    };
    return { start: format(start), end: format(end) };
}

// Jan 1 to Dec 31 of current year
function dash_getYearRange() {
    const year = new Date().getFullYear();
    return { start: `${year}-01-01`, end: `${year}-12-31` };
}

// Check if dateStr is in start-end boundary
function dash_inRange(dateStr, start, end) {
    return dateStr >= start && dateStr <= end;
}

// Load appointments
function dash_initAppts() {
    const fromInput = document.getElementById('dash-appt-from');
    const toInput = document.getElementById('dash-appt-to');
    const searchInput = document.getElementById('dash-appt-search');

    if (fromInput) fromInput.value = '';
    if (toInput) toInput.value = '';
    if (searchInput) searchInput.value = '';

    dash_filter = 'all';
    dash_search = '';
    dash_from = '';
    dash_to = '';
    dash_page = 1;

    dash_loadAppts();
}

function dash_toggleCardSkeletons(show) {
    const cardTypes = ['today', 'upcoming', 'completed', 'cancelled'];
    cardTypes.forEach(type => {
        const skeletonEl = document.getElementById(`dash-count-${type}-skeleton`);
        const contentEl = document.getElementById(`dash-count-${type}-content`);
        if (skeletonEl && contentEl) {
            if (show) {
                skeletonEl.classList.remove('hidden');
                contentEl.classList.add('hidden');
            } else {
                skeletonEl.classList.add('hidden');
                contentEl.classList.remove('hidden');
            }
        }
    });
}

// Fetch all appointments
async function dash_loadAppts() {
    const listContainer = document.getElementById('dash-appt-list');
    const countLabel = document.getElementById('dash-appt-count');
    const paginationContainer = document.getElementById('dash-appt-pagination');

    if (countLabel) countLabel.textContent = 'Loading...';
    if (paginationContainer) paginationContainer.classList.add('hidden');

    dash_toggleCardSkeletons(true);

    if (listContainer) {
        const itemSkeleton = `
          <div class="animate-pulse space-y-3">
            <div class="bg-white rounded-2xl border border-gray-100 p-4">
              <div class="flex items-start gap-3">
                <div class="w-10 h-10 rounded-xl bg-gray-100 flex-shrink-0"></div>
                <div class="flex-1 space-y-2">
                  <div class="h-3 bg-gray-100 rounded-full w-2/3"></div>
                  <div class="h-2.5 bg-gray-100 rounded-full w-1/2"></div>
                  <div class="h-2 bg-gray-100 rounded-full w-1/3"></div>
                </div>
                <div class="w-16 h-7 bg-gray-100 rounded-lg flex-shrink-0"></div>
              </div>
            </div>
          </div>
        `;
        listContainer.innerHTML = itemSkeleton.repeat(4);
    }

    try {
        const response = await fetch('/appointments');
        if (!response.ok) throw new Error('Network error');
        dashAppts = await response.json();
        dash_loaded = true;

        dash_updateCards();
        dash_toggleCardSkeletons(false);
        dash_renderAppts();
    } catch (error) {
        console.error('Error fetching appointments:', error);
        showToast('Failed to load appointments ❌');
        
        dash_toggleCardSkeletons(false);
        if (listContainer) {
            listContainer.innerHTML = `
                <div class="text-center py-12 bg-white rounded-2xl border border-gray-100">
                    <div class="w-12 h-12 rounded-2xl bg-red-50 flex items-center justify-center mx-auto mb-3 text-red-500">
                        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                        </svg>
                    </div>
                    <p class="text-sm font-semibold text-red-500">Failed to load appointments</p>
                    <p class="text-xs text-gray-400 mt-1">Please try refreshing the page</p>
                </div>
            `;
        }
        if (countLabel) countLabel.textContent = 'Error loading appointments';
    }
}

// Update summary card counts from cached data
function dash_updateCards() {
    const todayStr = new Date().toISOString().split('T')[0];

    // Card 1: Today (status !== CANCELLED and Date === today)
    const todayCount = dashAppts.filter(a => a.appointmentDate === todayStr && a.status !== 'CANCELLED').length;
    const todayEl = document.getElementById('dash-count-today');
    if (todayEl) todayEl.textContent = todayCount;

    // Card 2: Upcoming (status === UPCOMING)
    const upcomingPeriod = document.getElementById('dash-card-upcoming-period')?.value || 'week';
    let upcomingRange = null;
    if (upcomingPeriod === 'week') upcomingRange = dash_getWeekRange();
    else if (upcomingPeriod === 'month') upcomingRange = dash_getMonthRange(0);
    else if (upcomingPeriod === 'next_month') upcomingRange = dash_getMonthRange(1);

    const upcomingCount = dashAppts.filter(a => {
        if (a.status !== 'UPCOMING') return false;
        if (!upcomingRange) return true;
        return dash_inRange(a.appointmentDate, upcomingRange.start, upcomingRange.end);
    }).length;
    const upcomingEl = document.getElementById('dash-count-upcoming');
    if (upcomingEl) upcomingEl.textContent = upcomingCount;

    // Card 3: Completed (status === COMPLETED)
    const completedPeriod = document.getElementById('dash-card-completed-period')?.value || 'week';
    let completedRange = null;
    if (completedPeriod === 'week') completedRange = dash_getWeekRange();
    else if (completedPeriod === 'month') completedRange = dash_getMonthRange(0);
    else if (completedPeriod === 'year') completedRange = dash_getYearRange();

    const completedCount = dashAppts.filter(a => {
        if (a.status !== 'COMPLETED') return false;
        if (!completedRange) return true;
        return dash_inRange(a.appointmentDate, completedRange.start, completedRange.end);
    }).length;
    const completedEl = document.getElementById('dash-count-completed');
    if (completedEl) completedEl.textContent = completedCount;

    // Card 4: Cancelled (status === CANCELLED)
    const cancelledPeriod = document.getElementById('dash-card-cancelled-period')?.value || 'week';
    let cancelledRange = null;
    if (cancelledPeriod === 'week') cancelledRange = dash_getWeekRange();
    else if (cancelledPeriod === 'month') cancelledRange = dash_getMonthRange(0);
    else if (cancelledPeriod === 'year') cancelledRange = dash_getYearRange();

    const cancelledCount = dashAppts.filter(a => {
        if (a.status !== 'CANCELLED') return false;
        if (!cancelledRange) return true;
        return dash_inRange(a.appointmentDate, cancelledRange.start, cancelledRange.end);
    }).length;
    const cancelledEl = document.getElementById('dash-count-cancelled');
    if (cancelledEl) cancelledEl.textContent = cancelledCount;
}

// Set status pill filter
function dash_setFilter(f) {
    dash_filter = f;
    dash_page = 1;

    // Update active pill styles
    const pills = ['all', 'today', 'upcoming', 'completed', 'cancelled'];
    pills.forEach(pill => {
        const el = document.getElementById(`dash-pill-${pill}`);
        if (!el) return;
        if (pill === f) {
            el.className = "px-3.5 py-1.5 rounded-full bg-indigo-600 text-white text-xs font-medium whitespace-nowrap cursor-pointer";
        } else {
            el.className = "px-3.5 py-1.5 rounded-full border border-gray-200 text-gray-600 text-xs font-medium whitespace-nowrap hover:border-gray-300 cursor-pointer";
        }
    });

    dash_renderAppts();
}

// Retrieve filtered appointments
function dash_getFiltered() {
    const todayStr = new Date().toISOString().split('T')[0];

    return dashAppts.filter(a => {
        // 1. Status/Filter Pill
        if (dash_filter === 'today') {
            if (a.appointmentDate !== todayStr || a.status === 'CANCELLED') return false;
        } else if (dash_filter === 'upcoming') {
            if (a.status !== 'UPCOMING') return false;
        } else if (dash_filter === 'completed') {
            if (a.status !== 'COMPLETED') return false;
        } else if (dash_filter === 'cancelled') {
            if (a.status !== 'CANCELLED') return false;
        }

        // 2. Search Text (customerName, serviceName, appointmentNumber)
        if (dash_search) {
            const query = dash_search.toLowerCase();
            const cName = (a.customerName || '').toLowerCase();
            const sName = (a.serviceName || '').toLowerCase();
            const aNum = (a.appointmentNumber || '').toLowerCase();
            if (!cName.includes(query) && !sName.includes(query) && !aNum.includes(query)) return false;
        }

        // 3. Date Range
        if (dash_from && a.appointmentDate < dash_from) return false;
        if (dash_to && a.appointmentDate > dash_to) return false;

        return true;
    });
}

// Render filtered appointments and pagination
function dash_renderAppts() {
    const listContainer = document.getElementById('dash-appt-list');
    const countLabel = document.getElementById('dash-appt-count');
    const paginationContainer = document.getElementById('dash-appt-pagination');
    const pageLabel = document.getElementById('dash-appt-page-label');
    const prevBtn = document.getElementById('dash-appt-prev');
    const nextBtn = document.getElementById('dash-appt-next');
    const numPagesContainer = document.getElementById('dash-appt-pages');

    if (!listContainer) return;

    const filtered = dash_getFiltered();
    const total = filtered.length;

    // Update count label
    if (countLabel) {
        countLabel.textContent = `Showing ${total} appointment(s)`;
    }

    if (total === 0) {
        listContainer.innerHTML = `
            <div class="text-center py-12 bg-white rounded-2xl border border-gray-100">
                <div class="w-12 h-12 rounded-2xl bg-gray-100 flex items-center justify-center mx-auto mb-3 text-gray-400">
                    <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                    </svg>
                </div>
                <p class="text-sm font-semibold text-gray-500">No appointments found</p>
                <p class="text-xs text-gray-400 mt-1">Try a different filter or search term</p>
            </div>
        `;
        if (paginationContainer) paginationContainer.classList.add('hidden');
        return;
    }

    // Pagination bounds
    const totalPages = Math.ceil(total / DASH_PER_PAGE);
    dash_page = Math.max(1, Math.min(totalPages, dash_page));

    const startIdx = (dash_page - 1) * DASH_PER_PAGE;
    const endIdx = Math.min(startIdx + DASH_PER_PAGE, total);
    const paginated = filtered.slice(startIdx, endIdx);

    // Render cards
    listContainer.innerHTML = paginated.map(a => dash_renderCard(a)).join('');

    // Update pagination UI
    if (paginationContainer) {
        paginationContainer.classList.remove('hidden');
    }
    if (pageLabel) {
        pageLabel.textContent = `Showing ${startIdx + 1}–${endIdx} of ${total} appointments`;
    }
    if (prevBtn) {
        prevBtn.disabled = dash_page === 1;
    }
    if (nextBtn) {
        nextBtn.disabled = dash_page === totalPages;
    }

    // Render page number buttons
    if (numPagesContainer) {
        let buttonsHtml = '';
        for (let i = 1; i <= totalPages; i++) {
            const isActive = i === dash_page;
            const btnClass = isActive 
                ? 'w-7 h-7 rounded-lg text-xs font-medium bg-indigo-600 text-white shadow-sm transition' 
                : 'w-7 h-7 rounded-lg border border-gray-200 text-xs text-gray-500 hover:bg-gray-50 transition';
            buttonsHtml += `
                <button onclick="dash_goPage(${i})" class="${btnClass}">
                    ${i}
                </button>
            `;
        }
        numPagesContainer.innerHTML = buttonsHtml;
    }
}

// Generate HTML string for single card
function dash_renderCard(a) {
    const todayStr = new Date().toISOString().split('T')[0];

    // Status colors and background
    let statusBg = 'bg-blue-50';
    let emoji = '📅';
    const statusUpper = (a.status || '').toUpperCase();
    
    if (statusUpper === 'COMPLETED') {
        statusBg = 'bg-emerald-50';
    } else if (statusUpper === 'CANCELLED') {
        statusBg = 'bg-red-50';
    }

    // Emojis by type
    const sType = (a.serviceType || '').toLowerCase();
    if (sType === 'clinic') emoji = '🏥';
    else if (sType === 'salon') emoji = '💇';
    else if (sType === 'gym') emoji = '💪';
    else if (sType === 'lab') emoji = '🧪';

    // Badge Row Items
    const todayBadge = (a.appointmentDate === todayStr) 
        ? `<span class="bg-amber-50 text-amber-800 text-[10px] px-1.5 py-0.5 rounded-md font-bold uppercase tracking-wider">Today</span>` 
        : '';

    let priorityBadge = '';
    if (a.priority) {
        const priUpper = a.priority.toUpperCase();
        if (priUpper === 'HIGH') {
            priorityBadge = `<span class="bg-red-50 text-red-700 text-[10px] px-1.5 py-0.5 rounded-md font-bold">High</span>`;
        } else if (priUpper === 'MEDIUM') {
            priorityBadge = `<span class="bg-orange-50 text-orange-700 text-[10px] px-1.5 py-0.5 rounded-md font-bold">Medium</span>`;
        } else {
            priorityBadge = `<span class="bg-gray-50 text-gray-500 text-[10px] px-1.5 py-0.5 rounded-md font-bold text-gray-500">Low</span>`;
        }
    }

    let statusBadge = '';
    if (statusUpper === 'COMPLETED') {
        statusBadge = `<span class="bg-emerald-50 text-emerald-700 text-[10px] px-1.5 py-0.5 rounded-md font-bold uppercase tracking-wider">Completed</span>`;
    } else if (statusUpper === 'CANCELLED') {
        statusBadge = `<span class="bg-red-50 text-red-700 text-[10px] px-1.5 py-0.5 rounded-md font-bold uppercase tracking-wider">Cancelled</span>`;
    } else {
        statusBadge = `<span class="bg-blue-50 text-blue-700 text-[10px] px-1.5 py-0.5 rounded-md font-bold uppercase tracking-wider">Upcoming</span>`;
    }

    // Actions
    const isUpcoming = statusUpper === 'UPCOMING';
    const actionBtns = isUpcoming ? `
        <div class="flex items-center gap-1">
            <button onclick="dash_markDone(${a.id}, this)" 
                class="px-2.5 py-1 rounded-lg bg-emerald-50 text-emerald-600 text-xs font-semibold hover:bg-emerald-100 transition">
                Done
            </button>
            <button onclick="dash_markCancel(${a.id}, this)"
                class="px-2.5 py-1 rounded-lg bg-red-50 text-red-500 text-xs font-semibold hover:bg-red-100 transition">
                Cancel
            </button>
        </div>
    ` : '';

    // Contact buttons
    const phone = a.customerPhone || '';
    const cleanPhone = phone.replace(/\D/g, '');
    const contactLinks = phone ? `
        <div class="flex items-center gap-1">
            <a href="https://wa.me/91${cleanPhone}" target="_blank"
                class="p-1.5 rounded-lg hover:bg-green-50 text-green-500 transition flex items-center justify-center"
                title="WhatsApp">
                <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 24 24">
                    <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L0 24l6.335-1.662c1.746.953 3.71 1.458 5.705 1.459h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z"/>
                </svg>
            </a>
            <a href="tel:+91${cleanPhone}"
                class="p-1.5 rounded-lg hover:bg-blue-50 text-blue-500 transition flex items-center justify-center"
                title="Call">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                        d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z" />
                </svg>
            </a>
        </div>
    ` : '';

    // Notes
    const notesRow = a.notes 
        ? `<p class="text-xs text-gray-400 mt-2 pt-2 border-t border-gray-50">📝 ${a.notes}</p>` 
        : '';

    // Assignee info
    const assigneeSpan = a.assignee 
        ? `<span class="text-xs text-gray-400">&middot; Assignee: ${a.assignee}</span>` 
        : '';

    // Date formatting
    const formattedDate = a.appointmentDate 
        ? new Date(a.appointmentDate).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }) 
        : '—';

    return `
        <div class="bg-white rounded-2xl border border-gray-100 shadow-sm p-4 hover:shadow-md transition">
            <div class="flex items-start justify-between gap-3">
                <div class="w-10 h-10 rounded-xl ${statusBg} flex items-center justify-center flex-shrink-0 text-lg">
                    ${emoji}
                </div>
                <div class="flex-1 min-w-0">
                    <div class="flex items-center gap-1.5 flex-wrap mb-0.5">
                        <span class="text-[10px] font-mono text-indigo-600 bg-indigo-50 px-2 py-0.5 rounded-md">${a.appointmentNumber || '—'}</span>
                        <p class="text-sm font-bold text-gray-900">${a.serviceName || '—'}</p>
                        ${todayBadge}
                        ${priorityBadge}
                        ${statusBadge}
                    </div>
                    <p class="text-xs text-gray-600 font-medium">
                        ${a.customerName || '—'} 
                        <span class="text-gray-400 font-normal">(CUS-${a.customerId || '—'})</span>
                    </p>
                    <div class="flex items-center gap-3 mt-1 flex-wrap">
                        <span class="text-xs text-gray-400 flex items-center gap-1">
                            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                            </svg>
                            ${formattedDate}
                        </span>
                        <span class="text-xs text-gray-400 flex items-center gap-1">
                            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                            </svg>
                            ${a.appointmentTime || '—'} &middot; ${a.durationMinutes || 0}min
                        </span>
                        ${assigneeSpan}
                    </div>
                </div>
                <div class="flex flex-col items-end gap-1.5 flex-shrink-0">
                    ${actionBtns}
                    ${contactLinks}
                </div>
            </div>
            ${notesRow}
        </div>
    `;
}

// Pagination page switch
function dash_goPage(p) {
    dash_page = p;
    dash_renderAppts();
}

// Pagination Prev/Next triggers
function dash_prevPage() {
    if (dash_page > 1) {
        dash_page--;
        dash_renderAppts();
    }
}

function dash_nextPage() {
    const filtered = dash_getFiltered();
    const totalPages = Math.ceil(filtered.length / DASH_PER_PAGE);
    if (dash_page < totalPages) {
        dash_page++;
        dash_renderAppts();
    }
}

// Handle debounced search on input
function dash_onSearchInput() {
    clearTimeout(dash_searchTimer);
    dash_searchTimer = setTimeout(() => {
        const searchInput = document.getElementById('dash-appt-search');
        dash_search = searchInput ? searchInput.value.trim() : '';
        dash_page = 1;
        dash_renderAppts();
    }, 400);
}

// Handle custom date filters change
function dash_onFilterChange() {
    dash_from = document.getElementById('dash-appt-from').value;
    dash_to = document.getElementById('dash-appt-to').value;
    dash_page = 1;
    dash_renderAppts();
}

// Mark Done API POST request
async function dash_markDone(id, buttonEl) {
    if (buttonEl) {
        buttonEl.disabled = true;
        buttonEl.textContent = 'Saving...';
    }
    try {
        const res = await fetch(`/appointments/${id}/status?status=COMPLETED`, { method: 'POST' });
        if (!res.ok) throw new Error('API failed');
        showToast("Appointment Completed ✅");
        await dash_loadAppts();
    } catch (e) {
        console.error(e);
        showToast("Failed to complete appointment ❌");
        if (buttonEl) {
            buttonEl.disabled = false;
            buttonEl.textContent = 'Done';
        }
    }
}

// Cancel appointment API POST request
async function dash_markCancel(id, buttonEl) {
    if (!confirm("Cancel this appointment?")) return;
    if (buttonEl) {
        buttonEl.disabled = true;
        buttonEl.textContent = 'Saving...';
    }
    try {
        const reason = encodeURIComponent("Cancelled via dashboard");
        const res = await fetch(`/appointments/${id}/status?status=CANCELLED&reason=${reason}`, { method: 'POST' });
        if (!res.ok) throw new Error('API failed');
        showToast("Appointment Cancelled ❌");
        await dash_loadAppts();
    } catch (e) {
        console.error(e);
        showToast("Failed to cancel appointment ❌");
        if (buttonEl) {
            buttonEl.disabled = false;
            buttonEl.textContent = 'Cancel';
        }
    }
}

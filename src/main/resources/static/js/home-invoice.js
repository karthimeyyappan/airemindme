// ── Invoice Payments Tab JavaScript Logic ────────────────────────────────

// Global state variables
let invoiceSearchQuery = '';
let invoiceStatus = '';
let invoiceDateFrom = '';
let invoiceDateTo = '';
let invoicePage = 0;
const invoiceSize = 10;
let invoiceSearchTimeout = null;

let selectedPaymentMode = 'Cash';
let activeInvoice = null;

// Initialize when DOM loads
document.addEventListener('DOMContentLoaded', () => {
    // Check if we are currently on the payments tab or if it's default
    initInvoiceTab();
});

// Setup tab initialization
function initInvoiceTab() {
    // Set default dates and dropdown values
    const dateFromInput = document.getElementById('invoiceDateFrom');
    const dateToInput = document.getElementById('invoiceDateTo');
    const searchInput = document.getElementById('invoiceSearch');

    if (dateFromInput) dateFromInput.value = '';
    if (dateToInput) dateToInput.value = '';
    if (searchInput) searchInput.value = '';

    // Load dynamic summaries with defaults
    loadInvoiceSummary('outstanding', 'overall');
    loadInvoiceSummary('dueToday', 'today');
    loadInvoiceSummary('overdue', 'today'); // Overdue defaults to "Today" filter
    loadInvoiceSummary('collectedToday', 'today');

    // Load initial table data
    loadInvoicesTable();
}

// Load individual summary card values
async function loadInvoiceSummary(cardType, period) {
    const skeletonEl = document.getElementById(`card-${cardType}-skeleton`);
    const contentEl = document.getElementById(`card-${cardType}-content`);
    const amountEl = document.getElementById(`card-${cardType}-amount`);
    const subtextEl = document.getElementById(`card-${cardType}-subtext`);
    
    if (skeletonEl && contentEl) {
        skeletonEl.classList.remove('hidden');
        contentEl.classList.add('hidden');
    }

    try {
        const response = await fetch(`/api/invoices/summary?period=${period}`);
        if (!response.ok) throw new Error('Failed to load summary');
        const data = await response.json();

        // Format amount using INR format
        const formatCurrency = (val) => {
            const num = parseFloat(val || 0);
            if (num >= 10000000) {
                return '₹' + (num / 10000000).toFixed(2) + 'Cr';
            } else if (num >= 100000) {
                return '₹' + (num / 100000).toFixed(2) + 'L';
            } else if (num >= 1000) {
                return '₹' + (num / 1000).toFixed(1) + 'K';
            }
            return '₹' + num.toLocaleString('en-IN');
        };

        if (amountEl && subtextEl) {
            if (cardType === 'outstanding') {
                amountEl.textContent = formatCurrency(data.totalOutstanding);
                subtextEl.textContent = 'Active outstanding balance';
            } else if (cardType === 'dueToday') {
                amountEl.textContent = formatCurrency(data.dueToday);
                subtextEl.textContent = `${data.dueTodayCount} invoice(s) due today`;
            } else if (cardType === 'overdue') {
                amountEl.textContent = formatCurrency(data.overdue);
                subtextEl.textContent = `${data.overdueCount} invoice(s) overdue`;
            } else if (cardType === 'collectedToday') {
                amountEl.textContent = formatCurrency(data.collectedToday);
                subtextEl.textContent = 'Collected payments';
            }
        }

        if (skeletonEl && contentEl) {
            skeletonEl.classList.add('hidden');
            contentEl.classList.remove('hidden');
        }
    } catch (error) {
        console.error(`Error loading summary for ${cardType}:`, error);
        if (amountEl) amountEl.textContent = '₹—';
        if (subtextEl) subtextEl.textContent = 'Error loading stats';
        if (skeletonEl && contentEl) {
            skeletonEl.classList.add('hidden');
            contentEl.classList.remove('hidden');
        }
    }
}

// Reload table data with active filters
async function loadInvoicesTable() {
    const tableBody = document.getElementById('payTable');
    if (!tableBody) return;

    // Show 5 skeleton rows immediately
    const skeletonRow = `
      <tr class="animate-pulse">
        <td class="px-4 py-3">
          <div class="flex items-center gap-2.5">
            <div class="w-8 h-8 rounded-full bg-gray-100"></div>
            <div class="space-y-1.5">
              <div class="h-2.5 bg-gray-100 rounded w-24"></div>
              <div class="h-2 bg-gray-100 rounded w-16"></div>
            </div>
          </div>
        </td>
        <td class="px-4 py-3 hidden md:table-cell">
          <div class="h-2.5 bg-gray-100 rounded w-20"></div>
        </td>
        <td class="px-4 py-3">
          <div class="h-2.5 bg-gray-100 rounded w-14"></div>
        </td>
        <td class="px-4 py-3">
          <div class="h-2.5 bg-gray-100 rounded w-16 ml-auto"></div>
        </td>
        <td class="px-4 py-3">
          <div class="h-5 bg-gray-100 rounded-full w-16"></div>
        </td>
        <td class="px-4 py-3">
          <div class="flex gap-1">
            <div class="h-6 bg-gray-100 rounded-lg w-14"></div>
            <div class="h-6 bg-gray-100 rounded-lg w-6"></div>
            <div class="h-6 bg-gray-100 rounded-lg w-6"></div>
          </div>
        </td>
      </tr>
    `;
    tableBody.innerHTML = skeletonRow.repeat(5);

    try {
        const queryParams = new URLSearchParams({
            search: invoiceSearchQuery,
            status: invoiceStatus,
            dateFrom: invoiceDateFrom,
            dateTo: invoiceDateTo,
            page: invoicePage,
            size: invoiceSize
        });

        const response = await fetch(`/api/invoices?${queryParams.toString()}`);
        if (!response.ok) throw new Error('Failed to load invoices');
        
        const data = await response.json();
        renderInvoiceTableRows(data.content || []);
        renderInvoicePagination(data);
    } catch (error) {
        console.error('Error fetching invoices:', error);
        showToast(error.message || 'Error fetching invoice data');
        tableBody.innerHTML = `
            <tr>
                <td colspan="6" class="px-4 py-8 text-center text-red-500 font-medium">
                    Failed to load invoices. Please try again.
                </td>
            </tr>
        `;
    }
}

// Render the individual rows of the invoice table
function renderInvoiceTableRows(invoices) {
    const tableBody = document.getElementById('payTable');
    if (!tableBody) return;

    if (invoices.length === 0) {
        tableBody.innerHTML = `
            <tr>
                <td colspan="6" class="px-4 py-12 text-center text-gray-500 font-medium bg-white">
                    <div class="flex flex-col items-center justify-center gap-2">
                        <svg class="w-8 h-8 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                        </svg>
                        <span>No invoices found</span>
                    </div>
                </td>
            </tr>
        `;
        return;
    }

    const todayStr = new Date().toISOString().split('T')[0];

    tableBody.innerHTML = invoices.map(invoice => {
        const grandTotal = parseFloat(invoice.grandTotal || 0);
        const paidAmount = parseFloat(invoice.paidAmount || 0);
        const balance = Math.max(0, grandTotal - paidAmount);
        const phone = invoice.customerPhone || '';
        const cleanPhone = phone.replace(/[^0-9]/g, '');

        // Determine status badge colors and text
        let statusBadge = '';
        const statusUpper = (invoice.status || '').toUpperCase();
        
        if (statusUpper === 'PAID') {
            statusBadge = `<span class="bg-emerald-50 text-emerald-700 border border-emerald-100 px-2 py-1 rounded-full text-[10px] font-semibold uppercase tracking-wider">Paid</span>`;
        } else if (statusUpper === 'CANCELLED') {
            statusBadge = `<span class="bg-gray-100 text-gray-600 border border-gray-200 px-2 py-1 rounded-full text-[10px] font-semibold uppercase tracking-wider">Cancelled</span>`;
        } else {
            const dueDateStr = invoice.dueDate ? invoice.dueDate.toString() : '';
            if (dueDateStr && dueDateStr < todayStr) {
                statusBadge = `<span class="bg-red-50 text-red-700 border border-red-100 px-2 py-1 rounded-full text-[10px] font-semibold uppercase tracking-wider">Overdue</span>`;
            } else if (dueDateStr === todayStr) {
                statusBadge = `<span class="bg-blue-50 text-blue-700 border border-blue-100 px-2 py-1 rounded-full text-[10px] font-semibold uppercase tracking-wider">Due Today</span>`;
            } else {
                statusBadge = `<span class="bg-gray-50 text-gray-500 border border-gray-200 px-2 py-1 rounded-full text-[10px] font-semibold uppercase tracking-wider">Upcoming</span>`;
            }
        }

        // Action collect button
        const showCollect = statusUpper !== 'PAID' && balance > 0;
        const collectBtn = showCollect ? `
            <button onclick='openInvoiceCollectModal(${JSON.stringify(invoice)})' 
                class="px-3 py-1 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-semibold shadow-sm transition">
                Collect
            </button>
        ` : '';

        // Contacts actions
        const whatsappAction = phone ? `
            <a href="https://wa.me/91${cleanPhone}" target="_blank" title="WhatsApp Customer"
                class="p-1.5 rounded-lg hover:bg-green-50 text-green-600 transition flex items-center justify-center">
                <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 24 24">
                    <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L0 24l6.335-1.662c1.746.953 3.71 1.458 5.705 1.459h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z"/>
                </svg>
            </a>
        ` : '';

        const telAction = phone ? `
            <a href="tel:+91${cleanPhone}" title="Call Customer"
                class="p-1.5 rounded-lg hover:bg-blue-50 text-blue-500 transition flex items-center justify-center">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                        d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z" />
                </svg>
            </a>
        ` : '';

        return `
            <tr class="hover:bg-gray-50/50 transition">
                <td class="px-4 py-3">
                    <div class="font-medium text-gray-900">${invoice.customerName || '—'}</div>
                    <div class="text-xs text-gray-400">${phone || '—'}</div>
                </td>
                <td class="px-4 py-3 text-gray-700 font-semibold hidden md:table-cell">
                    ${invoice.invoiceNumber || '—'}
                </td>
                <td class="px-4 py-3 text-gray-600">
                    ${invoice.dueDate ? new Date(invoice.dueDate).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }) : '—'}
                </td>
                <td class="px-4 py-3 text-right font-bold text-gray-900">
                    ₹${grandTotal.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                </td>
                <td class="px-4 py-3">
                    ${statusBadge}
                </td>
                <td class="px-4 py-3">
                    <div class="flex items-center gap-1.5">
                        ${collectBtn}
                        ${whatsappAction}
                        ${telAction}
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

// Render pagination info and buttons
function renderInvoicePagination(pageData) {
    const prevBtn = document.getElementById('pg-pay-prev');
    const nextBtn = document.getElementById('pg-pay-next');
    const btnsContainer = document.getElementById('pg-pay-btns');
    const infoEl = document.getElementById('pg-pay-info');

    if (!pageData) return;

    const totalElements = pageData.totalElements || 0;
    const totalPages = pageData.totalPages || 0;
    const currentPage = pageData.number || 0;

    // Update Showing text
    if (totalElements === 0) {
        if (infoEl) infoEl.textContent = 'Showing 0–0 of 0 results';
        if (prevBtn) prevBtn.disabled = true;
        if (nextBtn) nextBtn.disabled = true;
        if (btnsContainer) btnsContainer.innerHTML = '';
        return;
    }

    const startIdx = currentPage * invoiceSize + 1;
    const endIdx = Math.min(startIdx + invoiceSize - 1, totalElements);
    if (infoEl) {
        infoEl.textContent = `Showing ${startIdx}–${endIdx} of ${totalElements} results`;
    }

    // Toggle Prev/Next buttons
    if (prevBtn) {
        prevBtn.disabled = currentPage === 0;
        prevBtn.onclick = () => {
            if (invoicePage > 0) {
                invoicePage--;
                loadInvoicesTable();
            }
        };
    }

    if (nextBtn) {
        nextBtn.disabled = currentPage >= totalPages - 1;
        nextBtn.onclick = () => {
            if (invoicePage < totalPages - 1) {
                invoicePage++;
                loadInvoicesTable();
            }
        };
    }

    // Render numbered page buttons
    if (btnsContainer) {
        let buttonsHtml = '';
        for (let i = 0; i < totalPages; i++) {
            const isActive = i === currentPage;
            const btnClass = isActive 
                ? 'px-3 py-1 rounded-lg text-xs font-semibold bg-indigo-600 text-white shadow-sm transition' 
                : 'px-3 py-1 rounded-lg border border-gray-200 text-xs text-gray-600 hover:bg-gray-50 transition';
            
            buttonsHtml += `
                <button onclick="gotoInvoicePage(${i})" class="${btnClass}">
                    ${i + 1}
                </button>
            `;
        }
        btnsContainer.innerHTML = buttonsHtml;
    }
}

// Navigate to a specific page
function gotoInvoicePage(pageNo) {
    invoicePage = pageNo;
    loadInvoicesTable();
}

// Handle debounced search on input
function onInvoiceSearchInput() {
    clearTimeout(invoiceSearchTimeout);
    invoiceSearchTimeout = setTimeout(() => {
        const searchInput = document.getElementById('invoiceSearch');
        invoiceSearchQuery = searchInput ? searchInput.value.trim() : '';
        invoicePage = 0;
        loadInvoicesTable();
    }, 400);
}

// Handle generic filter dropdown changes
function onInvoiceFilterChange() {
    invoiceDateFrom = document.getElementById('invoiceDateFrom').value;
    invoiceDateTo = document.getElementById('invoiceDateTo').value;
    invoicePage = 0;
    loadInvoicesTable();
}

// Handle status pill selection
function setInvoiceStatusFilter(btn) {
    // Remove active styles from all status pills
    document.querySelectorAll('.invoice-status-pill').forEach(b => {
        b.classList.remove('bg-indigo-600', 'text-white', 'shadow-sm');
        b.classList.add('bg-white', 'border', 'border-gray-200', 'text-gray-600');
    });

    // Add active style to the selected pill
    btn.classList.remove('bg-white', 'border', 'border-gray-200', 'text-gray-600');
    btn.classList.add('bg-indigo-600', 'text-white', 'shadow-sm');

    invoiceStatus = btn.getAttribute('data-status') || '';
    invoicePage = 0;
    loadInvoicesTable();
}

// Handle summary dropdown changes dynamically
function updateSummaryCard(cardType, period) {
    loadInvoiceSummary(cardType, period);
}

// Modal System Integration
function openInvoiceCollectModal(invoice) {
    activeInvoice = invoice;

    const grandTotal = parseFloat(invoice.grandTotal || 0);
    const paidAmount = parseFloat(invoice.paidAmount || 0);
    const balance = Math.max(0, grandTotal - paidAmount);

    // Prefill details
    const nameEl = document.getElementById('collect-name');
    const invoiceEl = document.getElementById('collect-invoice');
    const amountInput = document.getElementById('collect-amount');
    const dateInput = document.getElementById('collect-date');
    const refInput = document.getElementById('collect-ref');
    const notesInput = document.getElementById('collect-notes');

    if (nameEl) nameEl.textContent = invoice.customerName || '—';
    if (invoiceEl) invoiceEl.textContent = invoice.invoiceNumber || '—';
    
    // Amount Received: default to balance due
    if (amountInput) {
        amountInput.value = balance.toFixed(2);
        amountInput.max = balance;
    }

    // Default payment date: today
    if (dateInput) {
        dateInput.value = new Date().toISOString().split('T')[0];
    }

    // Clear optional inputs
    if (refInput) refInput.value = '';
    if (notesInput) notesInput.value = '';

    // Reset payment method buttons
    selectedPaymentMode = 'Cash';
    document.querySelectorAll('.method-btn').forEach(btn => {
        const text = btn.textContent.trim().toUpperCase();
        if (text === 'CASH') {
            btn.classList.remove('bg-gray-100', 'text-gray-600');
            btn.classList.add('bg-indigo-600', 'text-white');
        } else {
            btn.classList.remove('bg-indigo-600', 'text-white');
            btn.classList.add('bg-gray-100', 'text-gray-600');
        }
    });

    // Open modal
    openModal('collect');
}

// Validate and submit recorded payment to backend
async function submitPayment() {
    if (!activeInvoice) return;

    const amountInput = document.getElementById('collect-amount');
    const dateInput = document.getElementById('collect-date');
    const refInput = document.getElementById('collect-ref');
    const notesInput = document.getElementById('collect-notes');

    const amount = parseFloat(amountInput ? amountInput.value : 0);
    const paymentDate = dateInput ? dateInput.value : '';
    const referenceNo = refInput ? refInput.value.trim() : '';
    const notes = notesInput ? notesInput.value.trim() : '';

    const grandTotal = parseFloat(activeInvoice.grandTotal || 0);
    const paidAmount = parseFloat(activeInvoice.paidAmount || 0);
    const balance = Math.max(0, grandTotal - paidAmount);

    // Front-end validations
    if (isNaN(amount) || amount <= 0) {
        showToast('Payment amount must be greater than zero.');
        return;
    }

    if (amount > balance) {
        showToast('Payment amount cannot exceed the remaining balance due.');
        return;
    }

    if (!paymentDate) {
        showToast('Please select a payment date.');
        return;
    }

    const selectedDate = new Date(paymentDate);
    selectedDate.setHours(0, 0, 0, 0);
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    if (selectedDate > today) {
        showToast('Payment date cannot be in the future.');
        return;
    }

    // Call API
    try {
        const response = await fetch(`/api/invoices/${activeInvoice.id}/payments`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                amount,
                paymentMode: selectedPaymentMode,
                paymentDate,
                referenceNo,
                notes
            })
        });

        if (!response.ok) {
            const errData = await response.json().catch(() => ({}));
            throw new Error(errData.message || 'Failed to record payment');
        }

        // Close modal, show toast, and reload components
        closeModal();
        showToast('Payment recorded successfully');

        // Reload invoice summary cards
        const outstandingDropdown = document.querySelector('select[onchange*="outstanding"]');
        const dueTodayDropdown = document.querySelector('select[onchange*="dueToday"]');
        const overdueDropdown = document.querySelector('select[onchange*="overdue"]');
        const collectedTodayDropdown = document.querySelector('select[onchange*="collectedToday"]');

        loadInvoiceSummary('outstanding', outstandingDropdown ? outstandingDropdown.value : 'overall');
        loadInvoiceSummary('dueToday', dueTodayDropdown ? dueTodayDropdown.value : 'today');
        loadInvoiceSummary('overdue', overdueDropdown ? overdueDropdown.value : 'today');
        loadInvoiceSummary('collectedToday', collectedTodayDropdown ? collectedTodayDropdown.value : 'today');

        // Reload invoices list table
        loadInvoicesTable();
    } catch (error) {
        console.error('Error submitting payment:', error);
        showToast(error.message || 'Error occurred while saving payment');
    }
}

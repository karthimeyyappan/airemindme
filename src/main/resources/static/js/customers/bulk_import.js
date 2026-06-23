// Premium Bulk Customer Import JS Workflow

let uploadedRowsData = [];
let validationSummary = null;
let currentPreviewPage = 0;
const previewPageSize = 25;
let activeStatusFilter = 'ALL'; // 'ALL', 'Ready', 'Warning', 'Failed'

/* ================= MODAL TRIGGERS ================= */

function showImportModal() {
    document.getElementById('importModal').classList.remove('hidden');
    resetImport();
}

function hideImportModal() {
    document.getElementById('importModal').classList.add('hidden');
}

function resetImport() {
    uploadedRowsData = [];
    validationSummary = null;
    currentPreviewPage = 0;
    activeStatusFilter = 'ALL';

    document.getElementById('csvFileInput').value = '';
    document.getElementById('fileSelected').classList.add('hidden');
    document.getElementById('parseBtn').disabled = true;

    document.getElementById('importStep1').classList.remove('hidden');
    document.getElementById('importStep2').classList.add('hidden');
    document.getElementById('importStep3').classList.add('hidden');
    hideLoader();
}

/* ================= FILE DRAG & DROP ================= */

function handleFileSelect(input) {
    if (input.files && input.files[0]) {
        setCSVFile(input.files[0]);
    }
}

function handleFileDrop(event) {
    event.preventDefault();
    document.getElementById('dropZone').classList.remove('bg-indigo-50/50');
    const file = event.dataTransfer.files[0];
    if (file && (file.name.endsWith('.csv') || file.type === 'text/csv')) {
        setCSVFile(file);
    } else {
        showToast('Please upload a valid CSV file', 'error');
    }
}

function setCSVFile(file) {
    window.selectedImportFile = file;
    document.getElementById('fileSelected').classList.remove('hidden');
    document.getElementById('selectedFileName').textContent = file.name;
    document.getElementById('selectedFileSize').textContent = (file.size / 1024).toFixed(1) + ' KB';
    document.getElementById('parseBtn').disabled = false;
}

function clearFile() {
    window.selectedImportFile = null;
    document.getElementById('fileSelected').classList.add('hidden');
    document.getElementById('parseBtn').disabled = true;
    document.getElementById('csvFileInput').value = '';
}

/* ================= CSV PARSER ================= */

function parseCSVLine(line) {
    const result = [];
    let insideQuote = false;
    let currentField = '';
    for (let i = 0; i < line.length; i++) {
        const char = line[i];
        if (char === '"') {
            insideQuote = !insideQuote;
        } else if (char === ',' && !insideQuote) {
            result.push(currentField.trim());
            currentField = '';
        } else {
            currentField += char;
        }
    }
    result.push(currentField.trim());
    return result;
}

function parseCSV() {
    const file = window.selectedImportFile;
    if (!file) return;

    showLoader(0, 'Reading file...');

    const reader = new FileReader();
    reader.onload = function(e) {
        const text = e.target.result;
        const lines = text.split(/\r?\n/).filter(line => line.trim().length > 0);

        if (lines.length < 2) {
            showToast('CSV file is empty or missing header', 'error');
            hideLoader();
            return;
        }

        const dataLines = lines.slice(1);
        uploadedRowsData = [];

        dataLines.forEach(line => {
            const cols = parseCSVLine(line);
            if (cols.length < 2) return; // Skip completely corrupt empty rows

            uploadedRowsData.push({
                name: cols[0] || '',
                mobile: cols[1] || '',
                email: cols[2] || '',
                customerGroup: cols[3] || '',
                city: cols[4] || '',
                address: cols[5] || '',
                dob: cols[6] || '',
                weddingDate: cols[7] || '',
                gstNo: cols[8] || '',
                whatsAppOptIn: cols[9] || '',
                customerField1: cols[10] || '',
                customerField2: cols[11] || '',
                customerField3: cols[12] || '',
                customerField4: cols[13] || '',
                customerField5: cols[14] || ''
            });
        });

        validateImport();
    };
    reader.readAsText(file);
}

/* ================= BACKEND VALIDATION API ================= */

async function validateImport() {
    try {
        showLoader(20, 'Analyzing data against validation rules...');
        
        const response = await fetch('/api/customers/import/validate', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(uploadedRowsData)
        });

        if (!response.ok) {
            throw new Error('Validation failed on server side');
        }

        validationSummary = await response.json();
        hideLoader();
        
        showPreview();
    } catch (err) {
        console.error(err);
        showToast(err.message || 'Validation request failed', 'error');
        hideLoader();
    }
}

/* ================= INTERACTIVE PREVIEW UI ================= */

function showPreview() {
    document.getElementById('importStep1').classList.add('hidden');
    document.getElementById('importStep2').classList.remove('hidden');

    currentPreviewPage = 0;
    renderSummaryCards();
    renderPreviewTable();
}

function renderSummaryCards() {
    if (!validationSummary) return;

    const total = validationSummary.totalRows;
    const ready = validationSummary.readyCount;
    const warnings = validationSummary.warningCount;
    const failed = total - ready - warnings;

    document.getElementById('summary-total').textContent = total;
    document.getElementById('summary-ready').textContent = ready;
    document.getElementById('summary-warnings').textContent = warnings;
    document.getElementById('summary-failed').textContent = failed;
}

function setFilterFilter(status) {
    activeStatusFilter = status;
    currentPreviewPage = 0;
    
    // Highlight active card
    const cards = ['ALL', 'Ready', 'Warning', 'Failed'];
    cards.forEach(c => {
        const cardEl = document.getElementById(`card-filter-${c}`);
        if (cardEl) {
            if (c === status) {
                cardEl.classList.add('border-indigo-600', 'ring-2', 'ring-indigo-100');
            } else {
                cardEl.classList.remove('border-indigo-600', 'ring-2', 'ring-indigo-100');
            }
        }
    });
    
    renderPreviewTable();
}

function getFilteredResults() {
    if (!validationSummary) return [];
    if (activeStatusFilter === 'ALL') return validationSummary.results;
    if (activeStatusFilter === 'Failed') {
        return validationSummary.results.filter(r => r.status === 'Failed');
    }
    return validationSummary.results.filter(r => r.status === activeStatusFilter);
}

function renderPreviewTable() {
    const list = getFilteredResults();
    const startIdx = currentPreviewPage * previewPageSize;
    const endIdx = Math.min(startIdx + previewPageSize, list.length);
    const paginatedItems = list.slice(startIdx, endIdx);

    const tbody = document.getElementById('previewTable');
    tbody.innerHTML = '';

    if (paginatedItems.length === 0) {
        const colCount = 5 + (window.importCustomFields ? window.importCustomFields.length : 0);
        tbody.innerHTML = `
            <tr>
                <td colspan="${colCount}" class="px-4 py-8 text-center text-gray-400">
                    No records found matching filter.
                </td>
            </tr>
        `;
        document.getElementById('previewPagination').innerHTML = '';
        return;
    }

    const badgeClasses = {
        Ready: 'bg-emerald-50 text-emerald-700 border-emerald-100',
        Warning: 'bg-amber-50 text-amber-700 border-amber-100',
        Failed: 'bg-red-50 text-red-700 border-red-100'
    };

    paginatedItems.forEach(r => {
        let customCells = '';
        if (window.importCustomFields) {
            window.importCustomFields.forEach(cf => {
                const rawVal = r.rawData ? (r.rawData[cf.key] || '') : '';
                customCells += `<td class="px-4 py-3 text-gray-500">${escapeHtml(rawVal || '—')}</td>`;
            });
        }

        tbody.innerHTML += `
            <tr class="hover:bg-gray-50/50 transition border-b border-gray-100">
                <td class="px-4 py-3 font-semibold text-gray-800">${escapeHtml(r.name)}</td>
                <td class="px-4 py-3 text-gray-600">${escapeHtml(r.mobile)}</td>
                <td class="px-4 py-3 text-gray-500">${escapeHtml(r.email || '—')}</td>
                <td class="px-4 py-3 text-gray-500">${escapeHtml(r.customerGroup)}</td>
                ${customCells}
                <td class="px-4 py-3">
                    <span class="inline-flex px-2 py-0.5 rounded-full text-xs font-semibold border ${badgeClasses[r.status]}">
                        ${r.status}
                    </span>
                    <p class="text-[10px] text-gray-400 mt-0.5">${escapeHtml(r.reason)}</p>
                </td>
            </tr>
        `;
    });

    renderPaginationControls(list.length);
}

function renderPaginationControls(totalItems) {
    const totalPages = Math.ceil(totalItems / previewPageSize);
    const container = document.getElementById('previewPagination');
    container.innerHTML = '';

    if (totalPages <= 1) return;

    container.innerHTML = `
        <div class="flex items-center gap-2 mt-4 justify-between w-full text-xs text-gray-500 px-4">
            <button onclick="changePage(-1)" ${currentPreviewPage === 0 ? 'disabled' : ''} class="px-3 py-1.5 rounded-xl border border-gray-200 hover:bg-gray-50 disabled:opacity-50 transition">
                Previous
            </button>
            <span>Page ${currentPreviewPage + 1} of ${totalPages}</span>
            <button onclick="changePage(1)" ${currentPreviewPage === totalPages - 1 ? 'disabled' : ''} class="px-3 py-1.5 rounded-xl border border-gray-200 hover:bg-gray-50 disabled:opacity-50 transition">
                Next
            </button>
        </div>
    `;
}

function changePage(direction) {
    currentPreviewPage += direction;
    renderPreviewTable();
}

/* ================= IMPORT API CALL ================= */

async function confirmImport() {
    if (!validationSummary) return;

    // Filter failed rows out, only import Ready or Warning rows
    const validRows = validationSummary.results
        .filter(r => r.status !== 'Failed')
        .map(r => r.rawData);

    if (validRows.length === 0) {
        showToast('No valid rows available to import', 'error');
        return;
    }

    try {
        showLoader(40, 'Uploading and batch inserting customer records...');

        // Simulate progress bar updates
        let pct = 40;
        const interval = setInterval(() => {
            pct = Math.min(pct + 5, 95);
            updateLoaderProgress(pct, `${Math.round(validRows.length * (pct / 100))} / ${validRows.length} imported...`);
        }, 150);

        const response = await fetch('/api/customers/import', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(validRows)
        });

        clearInterval(interval);

        if (!response.ok) {
            throw new Error('Customer batch import failed on server side');
        }

        const stats = await response.json();
        hideLoader();

        showSummary(stats);
    } catch (err) {
        console.error(err);
        showToast(err.message || 'Bulk import failed', 'error');
        hideLoader();
    }
}

function showSummary(stats) {
    document.getElementById('importStep2').classList.add('hidden');
    document.getElementById('importStep3').classList.remove('hidden');

    document.getElementById('summary-total-imported').textContent = stats.imported;
    document.getElementById('summary-default-group').textContent = stats.assignedToDefault;
    document.getElementById('summary-skipped').textContent = stats.skipped;
    document.getElementById('summary-failed-db').textContent = stats.failed;

    // Reload window list after small delay to show new items
    setTimeout(() => {
        showToast('Import completed successfully!');
    }, 500);
}

/* ================= BONUS: ERROR CSV DOWNLOAD ================= */

function downloadErrorCSV() {
    if (!validationSummary) return;
    
    // Filter warnings and failures
    const issueRows = validationSummary.results.filter(r => r.status !== 'Ready');
    
    if (issueRows.length === 0) {
        showToast('No warnings or errors found to export!', 'success');
        return;
    }

    let csvContent = 'data:text/csv;charset=utf-8,';
    csvContent += 'Name,Mobile,Email,Status,Issue Reason\n';

    issueRows.forEach(r => {
        const rowString = `"${r.name}","${r.mobile}","${r.email}","${r.status}","${r.reason}"`;
        csvContent += rowString + '\n';
    });

    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `customer_import_issues_${new Date().toISOString().slice(0,10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
}

/* ================= LOADER OVERLAY ================= */

function showLoader(percentage, message) {
    const overlay = document.getElementById('importLoaderOverlay');
    if (overlay) {
        overlay.classList.remove('hidden');
        overlay.classList.add('flex');
    }
    updateLoaderProgress(percentage, message);
}

function hideLoader() {
    const overlay = document.getElementById('importLoaderOverlay');
    if (overlay) {
        overlay.classList.add('hidden');
        overlay.classList.remove('flex');
    }
}

function updateLoaderProgress(percentage, message) {
    const bar = document.getElementById('loaderProgressBar');
    const pctLabel = document.getElementById('loaderPercentageText');
    const msgLabel = document.getElementById('loaderMessageText');

    if (bar) bar.style.width = percentage + '%';
    if (pctLabel) pctLabel.textContent = percentage + '%';
    if (msgLabel) msgLabel.textContent = message;
}

/* ================= HELPERS ================= */

function downloadTemplate() {
    window.location.href = '/api/customers/template';
}

function escapeHtml(str) {
    if (!str) return '';
    return str
         .replace(/&/g, "&amp;")
         .replace(/</g, "&lt;")
         .replace(/>/g, "&gt;")
         .replace(/"/g, "&quot;")
         .replace(/'/g, "&#039;");
}

window.importCustomFields = [];

async function loadImportSettings() {
    try {
        const response = await fetch('/api/settings/customer-fields');
        if (!response.ok) throw new Error('Failed to load settings');
        const settings = await response.json();
        
        window.importCustomFields = [];
        for (let i = 1; i <= 5; i++) {
            const fieldName = settings[`field${i}Name`] || '';
            if (fieldName.trim() !== '') {
                window.importCustomFields.push({
                    key: `customerField${i}`,
                    name: fieldName.trim()
                });
            }
        }
        
        const headerRow = document.getElementById('previewTableHeaderRow');
        if (headerRow) {
            let html = `
                <th class="px-4 py-3 font-semibold text-gray-500">Name</th>
                <th class="px-4 py-3 font-semibold text-gray-500">Phone</th>
                <th class="px-4 py-3 font-semibold text-gray-500">Email</th>
                <th class="px-4 py-3 font-semibold text-gray-500">Mapped Group</th>
            `;
            window.importCustomFields.forEach(cf => {
                html += `<th class="px-4 py-3 font-semibold text-gray-500">${escapeHtml(cf.name)}</th>`;
            });
            html += `<th class="px-4 py-3 font-semibold text-gray-500">Status</th>`;
            headerRow.innerHTML = html;
        }
    } catch (err) {
        console.error('Error loading import settings:', err);
    }
}

window.addEventListener('DOMContentLoaded', () => {
    loadImportSettings();
});

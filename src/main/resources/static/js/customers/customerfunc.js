function showToast(message, type = "success") {

    let bgColor = "#10b981"; // green

    if (type === "error") {
        bgColor = "#ef4444";
    }

    Toastify({
        text: message,
        duration: 3000,
        gravity: "top",
        position: "right",
        close: true,
        stopOnFocus: true,
        style: {
            background: bgColor,
            borderRadius: "12px"
        }
    }).showToast();
}


/* =================  ================= */
const phoneInput = document.getElementById("cPhone");
if (phoneInput) {
    phoneInput.addEventListener("input", function () {
        this.value = this.value.replace(/\D/g, "").slice(0, 11);
    });
}
function getFullPhone() {
    const code = document.getElementById("countryCode").value;
    const phone = document.getElementById("cPhone").value;

    if (!phone) {
        alert("Enter phone number");
        return null;
    }

    return code + phone;
}

/* ================= SAVE CUSTOMER ================= */

function saveCustomer(e) {
    e.preventDefault();

    // Advance to Step 2 if user hits Enter/submits on Step 1
    if (typeof currentCustomerStep !== 'undefined' && currentCustomerStep === 1) {
        nextCustomerStep();
        return;
    }

    const id = document.getElementById("cId").value;
    const groupIdString = document.getElementById("cSegment").value;

    // ── PHONE VALIDATION ──
    const phoneValue = document.getElementById("cPhone").value.trim();
    const countryCode = document.getElementById("countryCode").value;

    if (!phoneValue) {
        showToast("Phone number is required", "error");
        return;
    }

    // India (+91) must be exactly 10 digits
    if (countryCode === "+91" && !/^\d{10}$/.test(phoneValue)) {
        showToast("Please enter a valid 10-digit phone number", "error");
        return;
    }

    // Other countries — at least 7 digits
    if (countryCode !== "+91" && !/^\d{7,15}$/.test(phoneValue)) {
        showToast("Please enter a valid phone number", "error");
        return;
    }

    // ── EMAIL VALIDATION ──
    const emailValue = document.getElementById("cEmail").value.trim();
    if (emailValue !== "") {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(emailValue)) {
            showToast("Please enter a valid email address", "error");
            return;
        }
    }

    // ── GST VALIDATION ──
    const gstNo = document.getElementById("cGstNo").value.trim().toUpperCase();
    if (gstNo !== "") {
        const gstRegex = /^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$/;
        if (!gstRegex.test(gstNo)) {
            showToast("Please enter a valid GST number", "error");
            return;
        }
    }

    const address = document.getElementById("cAddress").value.trim();
    const city = document.getElementById("cCity").value.trim();
    const state = document.getElementById("cState").value.trim();
    const country = document.getElementById("cCountry").value.trim();

    const fullPhone = getFullPhone();
    if (!fullPhone) return;

    const customer = {
        name: document.getElementById("cName").value,
        mobile: fullPhone,
        email: document.getElementById("cEmail").value,
        dob: document.getElementById("cBirthday").value || null,
        weddingDate: document.getElementById("cAnniversary").value || null,
        channel: selectedChannels.join(","),
        customerGroupId: groupIdString,
        gstNo: gstNo || null,
        address: address || null,
        city: city || null,
        state: state || null,
        country: country || null,
        customerField1: document.getElementById("cField1") ? document.getElementById("cField1").value : null,
        customerField2: document.getElementById("cField2") ? document.getElementById("cField2").value : null,
        customerField3: document.getElementById("cField3") ? document.getElementById("cField3").value : null,
        customerField4: document.getElementById("cField4") ? document.getElementById("cField4").value : null,
        customerField5: document.getElementById("cField5") ? document.getElementById("cField5").value : null
    };

    const url = id ? "/api/customers/" + id : "/api/customers";
    const method = id ? "PUT" : "POST";

    fetch(url, {
        method: method,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(customer)
    })
        .then(async res => {
            if (!res.ok) {
                const errorData = await res.json();
                throw new Error(errorData.message || "Save failed");
            }
            return res.json();
        })
        .then(() => {
            showToast(
                id
                    ? "Customer updated successfully"
                    : "Customer added successfully"
            );
            setTimeout(() => {
                const url = new URL(window.location.href);
                url.searchParams.delete("editId");
                window.location.href = url.pathname + url.search;
            }, 1200);
        })
        .catch(err => {
            showToast(err.message, "error");
        });
}


// import .js

/* ================= IMPORT STATE ================= */
let csvParsedRows = [];
let csvFile = null;

/* ================= IMPORT MODAL UI ================= */

function showImportModal() {
    document.getElementById('importModal').classList.remove('hidden');
    resetImport();
}

function hideImportModal() {
    document.getElementById('importModal').classList.add('hidden');
}

function resetImport() {
    csvParsedRows = [];
    csvFile = null;

    document.getElementById('csvFileInput').value = '';
    document.getElementById('fileSelected').classList.add('hidden');
    document.getElementById('parseBtn').disabled = true;

    document.getElementById('importStep1').classList.remove('hidden');
    document.getElementById('importStep2').classList.add('hidden');
    document.getElementById('importStep3').classList.add('hidden');
}

/* ================= FILE HANDLING ================= */

function handleFileSelect(input) {
    if (input.files && input.files[0]) setCSVFile(input.files[0]);
}

function handleFileDrop(event) {
    event.preventDefault();
    const file = event.dataTransfer.files[0];
    if (file && file.name.endsWith('.csv')) setCSVFile(file);
    else alert('Please upload CSV');
}

function setCSVFile(file) {
    csvFile = file;
    document.getElementById('fileSelected').classList.remove('hidden');
    document.getElementById('selectedFileName').textContent = file.name;
    document.getElementById('selectedFileSize').textContent = (file.size / 1024).toFixed(1) + ' KB';
    document.getElementById('parseBtn').disabled = false;
}

function clearFile() {
    csvFile = null;
    document.getElementById('fileSelected').classList.add('hidden');
    document.getElementById('parseBtn').disabled = true;
}

/* ================= DATA PARSING ================= */

function formatDate(dateStr) {
    if (!dateStr) return null;
    dateStr = dateStr.trim();
    if (dateStr === "") return null;

    if (dateStr.includes("-")) {
        const parts = dateStr.split("-");
        if (parts[0].length === 2) {
            return `${parts[2]}-${parts[1]}-${parts[0]}`;
        }
    }
    return dateStr;
}

function parseCSV() {
    if (!csvFile) return;

    const reader = new FileReader();
    reader.onload = function (e) {
        const text = e.target.result;
        const lines = text.split('\n').filter(l => l.trim());

        if (lines.length < 2) {
            alert("CSV empty");
            return;
        }

        const dataLines = lines.slice(1);
        csvParsedRows = [];

        dataLines.forEach(line => {
            const cols = line.split(',').map(c => c.trim());
            if (cols.length < 2) return;

            csvParsedRows.push({
                name: cols[0] || '',
                phone: cols[1] || '',
                email: cols[2] || '',
                segment: (cols[3] || 'regular').toLowerCase(),
                channel: (cols[4] || 'whatsapp').toLowerCase(),
                birthday: formatDate(cols[5]),
                anniversary: formatDate(cols[6]),
                valid: (cols[0] && cols[1])
            });
        });

        showPreview();
    };
    reader.readAsText(csvFile);
}

/* ================= PREVIEW UI ================= */

function showPreview() {
    document.getElementById('importStep1').classList.add('hidden');
    document.getElementById('importStep2').classList.remove('hidden');

    const valid = csvParsedRows.filter(r => r.valid);
    document.getElementById('previewCount').textContent = valid.length + " rows";

    const tbody = document.getElementById('previewTable');
    tbody.innerHTML = "";

    valid.slice(0, 20).forEach(r => {
        tbody.innerHTML += `
<tr>
        <td class="px-3 py-2">${r.name}</td>
        <td class="px-3 py-2">${r.phone}</td>
        <td class="px-3 py-2">${r.email || '—'}</td>
        <td class="px-3 py-2">
    <span class="px-2 py-0.5 rounded ${segBadge[r.segment]}">${r.segment}</span>
        </td>
        <td class="px-3 py-2 text-green-600">✓ Valid</td>
</tr>
    `;
    });
}

/* ================= BULK IMPORT ================= */

function confirmImport() {
    const valid = csvParsedRows.filter(r => r.valid);
    const groupMap = { vip: 1, regular: 2, new: 3, inactive: 4 };

    const customers = valid.map(r => ({
        name: r.name,
        mobile: r.phone,
        email: r.email || null,
        dob: r.birthday || null,
        weddingDate: r.anniversary || null,
        channel: r.channel,
        customerGroupId: groupMap[r.segment] || 2
    }));

    fetch("/api/customers/import", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(customers)
    })
        .then(res => res.json())
        .then(data => {
            showToast(
                `${data.imported} customers imported successfully`
            );
            document.getElementById('importStep2').classList.add('hidden');
            document.getElementById('importStep3').classList.remove('hidden');
            document.getElementById('importSuccessMsg').innerText =
                `${data.imported} imported, ${data.skipped} skipped, ${data.failed || 0} failed`;
        })
        .catch(err => {
            console.error(err);
            alert("Import failed");
        });
}

//search.js

/* ================= FILTER ================= */

function setFilter(filter) {
    const url = new URL(window.location.href);

    if (filter === 'all') url.searchParams.delete('segment');
    else url.searchParams.set('segment', filter);

    url.searchParams.set('page', '0');
    window.location.href = url.toString();
}

/* ================= SEARCH ================= */

let searchTimer;

function updateClearSearchButton() {
    const searchInput = document.getElementById("searchInput");
    const clearSearchBtn = document.getElementById("clearSearchBtn");

    if (!searchInput || !clearSearchBtn) return;

    clearSearchBtn.classList.toggle("hidden", searchInput.value.trim() === "");
}

function searchCustomers() {
    const searchInput = document.getElementById("searchInput");
    if (!searchInput) return;

    const search = searchInput.value.trim();
    const url = new URL(window.location.href);

    if (search !== "") {
        url.searchParams.set("search", search);
    } else {
        url.searchParams.delete("search");
    }

    url.searchParams.set("page", "0");
    window.location.href = url.toString();
}

function clearSearch() {
    const searchInput = document.getElementById("searchInput");
    const clearSearchBtn = document.getElementById("clearSearchBtn");

    if (searchInput) {
        searchInput.value = "";
    }

    if (clearSearchBtn) {
        clearSearchBtn.classList.add("hidden");
    }

    clearTimeout(searchTimer);

    const url = new URL(window.location.href);
    url.searchParams.delete("search");
    url.searchParams.set("page", "0");

    window.location.href = url.toString();
}

window.addEventListener("DOMContentLoaded", function () {
    const searchInput = document.getElementById("searchInput");
    const clearSearchBtn = document.getElementById("clearSearchBtn");

    updateClearSearchButton();

    if (searchInput) {
        searchInput.addEventListener("input", function () {
            updateClearSearchButton();

            clearTimeout(searchTimer);
            searchTimer = setTimeout(function () {
                searchCustomers();
            }, 1000);
        });
    }

    if (clearSearchBtn) {
        clearSearchBtn.classList.toggle("hidden", !searchInput || searchInput.value.trim() === "");
    }
});

async function loadCustomerFields(customerData = null) {
    try {
        const response = await fetch("/api/settings/customer-fields");
        if (!response.ok) throw new Error("Failed to load settings");
        const settings = await response.json();

        // 1. Check if we are on the customers list/modal page
        const container = document.getElementById("customFieldsContainer");
        if (container) {
            container.innerHTML = "";
            for (let i = 1; i <= 5; i++) {
                const fieldName = settings[`field${i}Name`] || "";
                if (fieldName.trim() !== "") {
                    const value = customerData ? (customerData[`customerField${i}`] || "") : "";
                    
                    const fieldDiv = document.createElement("div");
                    fieldDiv.innerHTML = `
                        <label class="text-sm font-medium text-gray-700 mb-1.5 block">${fieldName}</label>
                        <input type="text" id="cField${i}" placeholder="Enter ${fieldName}" value="${value}"
                            class="w-full px-4 py-3 rounded-2xl border border-gray-200 bg-white text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 placeholder-gray-400">
                    `;
                    container.appendChild(fieldDiv);
                }
            }
        }

        // 2. Check if we are on the customer details page
        const detailsGrid = document.getElementById("customCustomerInfoGrid");
        const detailsCard = document.getElementById("customCustomerInfoCard");
        if (detailsGrid && detailsCard) {
            detailsGrid.innerHTML = "";
            let hasAnyVisibleField = false;

            if (customerData) {
                for (let i = 1; i <= 5; i++) {
                    const fieldName = settings[`field${i}Name`] || "";
                    const fieldValue = customerData[`customerField${i}`] || "";

                    if (fieldName.trim() !== "" && fieldValue.trim() !== "") {
                        hasAnyVisibleField = true;
                        const itemDiv = document.createElement("div");
                        itemDiv.innerHTML = `
                            <span class="text-slate-400 font-medium">${fieldName}:</span>
                            <span class="text-slate-800 ml-1 font-semibold">${fieldValue}</span>
                        `;
                        detailsGrid.appendChild(itemDiv);
                    }
                }
            }

            if (hasAnyVisibleField) {
                detailsCard.classList.remove("hidden");
            } else {
                detailsCard.classList.add("hidden");
            }
        }
    } catch (err) {
        console.error("Error loading custom customer fields:", err);
    }
}
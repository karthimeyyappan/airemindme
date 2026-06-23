function formatCountry(option) {
    if (!option.id) return option.text;

    const flag = $(option.element).data("flag");

    return $(`
        <span style="display:flex;align-items:center;gap:8px;white-space:nowrap;">
            <img src="https://flagcdn.com/w20/${flag}.png"
                 style="width:18px;height:12px;border-radius:2px;" />
            <span>${option.text}</span>
        </span>
    `);
}
$(document).ready(function () {

    $('#countryCode').select2({
        templateResult: formatCountry,
        templateSelection: formatCountry,
        minimumResultsForSearch: 0,
        width: '100%',
        escapeMarkup: function (markup) {
            return markup;
        }
    });

});

/* ================= CUSTOMER MODAL ================= */


function toggleGroupSelection(groupId) {
    if (!groupId) return;
    groupId = groupId.toString();

    const index = selectedGroupIds.indexOf(groupId);
    const btn = document.getElementById('sb-' + groupId);

    if (index > -1) {
        // Deselecting
        selectedGroupIds.splice(index, 1);
        if (btn) {
            btn.classList.remove('border-indigo-500', 'bg-indigo-50');
            btn.classList.add('border-gray-200', 'bg-white');
            const label = btn.querySelector('.sbl');
            if (label) label.classList.replace('text-indigo-700', 'text-gray-800');
        }
    } else {
        // Selecting
        selectedGroupIds.push(groupId);
        if (btn) {
            btn.classList.add('border-indigo-500', 'bg-indigo-50');
            btn.classList.remove('border-gray-200', 'bg-white');
            const label = btn.querySelector('.sbl');
            if (label) label.classList.replace('text-gray-800', 'text-indigo-700');
        }
    }
    // Sync the hidden field that goes to Spring Boot
    document.getElementById('cSegment').value = selectedGroupIds.join(',');
}

function resetGroupUI() {
    selectedGroupIds = [];
    document.getElementById('cSegment').value = "";
    document.querySelectorAll('.group-btn').forEach(btn => {
        btn.classList.remove('border-indigo-500', 'bg-indigo-50');
        btn.classList.add('border-gray-200', 'bg-white');
        const label = btn.querySelector('.sbl');
        if (label) label.classList.replace('text-indigo-700', 'text-gray-800');
    });
}
let currentCustomerStep = 1;

function nextCustomerStep() {
    const nameInput = document.getElementById("cName");
    const phoneInput = document.getElementById("cPhone");
    if (nameInput && !nameInput.checkValidity()) {
        nameInput.reportValidity();
        return;
    }
    if (phoneInput && !phoneInput.checkValidity()) {
        phoneInput.reportValidity();
        return;
    }

    document.getElementById("customerWizardStep1").classList.add("hidden");
    document.getElementById("customerWizardStep2").classList.remove("hidden");
    document.getElementById("wizardStepLabel").innerText = "Step 2 of 2: Additional Info";
    
    const ind1 = document.getElementById("stepIndicator1");
    const ind2 = document.getElementById("stepIndicator2");
    if (ind1 && ind2) {
        ind1.classList.remove("bg-indigo-600");
        ind1.classList.add("bg-indigo-100");
        ind2.classList.remove("bg-gray-200");
        ind2.classList.add("bg-indigo-600");
    }
    currentCustomerStep = 2;
}

function prevCustomerStep() {
    document.getElementById("customerWizardStep2").classList.add("hidden");
    document.getElementById("customerWizardStep1").classList.remove("hidden");
    document.getElementById("wizardStepLabel").innerText = "Step 1 of 2: Basic Info";
    
    const ind1 = document.getElementById("stepIndicator1");
    const ind2 = document.getElementById("stepIndicator2");
    if (ind1 && ind2) {
        ind1.classList.remove("bg-indigo-100");
        ind1.classList.add("bg-indigo-600");
        ind2.classList.remove("bg-indigo-600");
        ind2.classList.add("bg-gray-200");
    }
    currentCustomerStep = 1;
}

function resetCustomerWizard() {
    document.getElementById("cGstNo").value = "";
    document.getElementById("cAddress").value = "";
    document.getElementById("cCity").value = "";
    document.getElementById("cState").value = "";
    document.getElementById("cCountry").value = "";
    
    document.getElementById("customerWizardStep2").classList.add("hidden");
    document.getElementById("customerWizardStep1").classList.remove("hidden");
    document.getElementById("wizardStepLabel").innerText = "Step 1 of 2: Basic Info";
    
    const ind1 = document.getElementById("stepIndicator1");
    const ind2 = document.getElementById("stepIndicator2");
    if (ind1 && ind2) {
        ind1.className = "w-6 h-1.5 rounded-full bg-indigo-600 transition-colors";
        ind2.className = "w-6 h-1.5 rounded-full bg-gray-200 transition-colors";
    }
    currentCustomerStep = 1;
}

function showAddCustomerModal() {
    document.getElementById("cId").value = "";
    document.getElementById("cName").value = "";
    document.getElementById("cPhone").value = "";
    document.getElementById("cEmail").value = "";
    document.getElementById("cBirthday").value = "";
    document.getElementById("cAnniversary").value = "";

    resetGroupUI();
    resetCustomerWizard();

    document.getElementById("modalTitle").innerText = "Add Customer";
    document.getElementById("saveCustomerBtn").innerText = "Add Customer";

    // ✅ Load dynamic custom fields empty inputs
    loadCustomerFields();

    // ✅ Show modal first
    document.getElementById("addCustomerModal").classList.remove("hidden");
}

function hideAddCustomerModal() {
    document.getElementById("addCustomerModal").classList.add("hidden");
}


function openEditCustomer(id, name, phone, email, dob, weddingDate, groupIds, channel, gstNo, address, city, state, country, field1, field2, field3, field4, field5) {
    resetGroupUI(); // Clear previous highlights
    resetCustomerWizard();

    document.getElementById("cId").value = id;
    document.getElementById("cName").value = name;
    let fullPhone = phone || "";

    // default
    let localPhone = fullPhone;

    const select = document.getElementById("countryCode");

    for (const option of select.options) {
        if (fullPhone.startsWith(option.value)) {
            select.value = option.value;
            $('#countryCode').trigger('change');
            localPhone = fullPhone.substring(option.value.length);
            break;
        }
    }

    document.getElementById("cPhone").value = localPhone;
    document.getElementById("cEmail").value = email;
    document.getElementById("cBirthday").value = dob ? dob.substring(0, 10) : "";
    document.getElementById("cAnniversary").value = weddingDate ? weddingDate.substring(0, 10) : "";
    
    // Step 2 Fields
    document.getElementById("cGstNo").value = gstNo || "";
    document.getElementById("cAddress").value = address || "";
    document.getElementById("cCity").value = city || "";
    document.getElementById("cState").value = state || "";
    document.getElementById("cCountry").value = country || "";

    console.log("DOB:", dob);
    console.log("Wedding:", weddingDate);
    // FIX: Handle the comma-separated group string
    if (groupIds) {
        const ids = groupIds.toString().split(',');
        ids.forEach(gid => {
            // Use toggleGroupSelection to highlight each button
            toggleGroupSelection(gid);
        });
    }

    // Handle Channels
    selectedChannels = channel ? channel.toString().split(",").map(Number) : [];
    updateChannelButtons();

    // Populate custom fields dynamically
    const customerData = {
        customerField1: field1 || "",
        customerField2: field2 || "",
        customerField3: field3 || "",
        customerField4: field4 || "",
        customerField5: field5 || ""
    };
    loadCustomerFields(customerData);

    document.getElementById("modalTitle").innerText = "Edit Customer";
    document.getElementById("saveCustomerBtn").innerText = "Update Customer";
    document.getElementById("addCustomerModal").classList.remove("hidden");
}
/* ================= CHANNEL BUTTONS ================= */

function updateChannelButtons() {
    const buttons = {
        1: document.getElementById("ch-wa"),
        2: document.getElementById("ch-sms"),
        3: document.getElementById("ch-email")
    };

    Object.keys(buttons).forEach(id => {
        const btn = buttons[id];
        if (selectedChannels.includes(parseInt(id))) {
            btn.classList.remove("bg-gray-100", "text-gray-600");
            btn.classList.add("text-white");

            if (id == 1) btn.classList.add("bg-green-500");
            if (id == 2) btn.classList.add("bg-blue-500");
            if (id == 3) btn.classList.add("bg-purple-500");
        } else {
            btn.classList.remove("bg-green-500", "bg-blue-500", "bg-purple-500", "text-white");
            btn.classList.add("bg-gray-100", "text-gray-600");
        }
    });

    document.getElementById("channelInput").value = selectedChannels.join(",");
}

function toggleCh(channelId) {
    const index = selectedChannels.indexOf(channelId);
    if (index > -1) {
        selectedChannels.splice(index, 1);
    } else {
        selectedChannels.push(channelId);
    }
    updateChannelButtons();
}

/* ================= SEGMENT ================= */

function selectSeg(groupId) {
    if (!groupId) return;

    // 1. Update Hidden Input
    const input = document.getElementById('cSegment');
    if (input) input.value = groupId;

    // 2. Clear all "active" states
    document.querySelectorAll('.group-btn').forEach(btn => {
        btn.classList.remove('border-indigo-500', 'bg-indigo-50');
        btn.classList.add('border-gray-200', 'bg-white');

        const label = btn.querySelector('.sbl');
        if (label) label.classList.replace('text-indigo-700', 'text-gray-800');
    });

    // 3. Apply "active" state to chosen group
    const activeBtn = document.getElementById('sb-' + groupId);
    if (activeBtn) {
        activeBtn.classList.add('border-indigo-500', 'bg-indigo-50');
        activeBtn.classList.remove('border-gray-200', 'bg-white');

        const label = activeBtn.querySelector('.sbl');
        if (label) label.classList.replace('text-gray-800', 'text-indigo-700');
    }
}

/* ================= VIEW SWITCH ================= */

function setView(view) {
    const grid = document.getElementById("customerGrid");
    const list = document.getElementById("customerList");
    const cardBtn = document.getElementById("v-card");
    const listBtn = document.getElementById("v-list");

    if (!grid || !list) return; // required

    if (view === "card") {
        grid.classList.remove("hidden");
        list.classList.add("hidden");

        if (cardBtn) cardBtn.classList.add("bg-indigo-50", "text-indigo-600");
        if (listBtn) listBtn.classList.remove("bg-indigo-50", "text-indigo-600");

    } else {
        grid.classList.add("hidden");
        list.classList.remove("hidden");

        if (listBtn) listBtn.classList.add("bg-indigo-50", "text-indigo-600");
        if (cardBtn) cardBtn.classList.remove("bg-indigo-50", "text-indigo-600");
    }
}

function viewCustomer(id) {
    window.location.href = "/customer-detail.html?id=" + id;
}

/* ================= PAGINATION INITIALIZATION ================= */

document.addEventListener("DOMContentLoaded", () => {
    const pagination = document.getElementById("pagination");

    if (pagination && totalPages > 1) {
        pagination.innerHTML = "";
        for (let i = 0; i < totalPages; i++) {
            const urlParams = new URLSearchParams(window.location.search);
            urlParams.set("page", i);

            const btn = document.createElement("a");
            btn.href = "customers.html?" + urlParams.toString();
            btn.innerText = i + 1;
            btn.className = "px-3 py-1 rounded border text-sm " +
                (i === currentPage ? "bg-indigo-600 text-white" : "bg-white text-gray-700 hover:bg-gray-50");

            pagination.appendChild(btn);
        }
    }

    // Set default view on load
    setView("card");
});

function downloadTemplate() {
    window.location.href = "/api/customers/template";
}
// ============================================================
//  settings.js  –  Numen | Settings Page
// ============================================================

// ─── Toast Notification ──────────────────────────────────────
function showToast(message, type = "success") {
    const existing = document.getElementById("rm-toast");
    if (existing) existing.remove();

    const colors = {
        success: "bg-emerald-600",
        error: "bg-red-500",
        info: "bg-indigo-600",
    };

    const icons = {
        success: `<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7"/>`,
        error: `<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M6 18L18 6M6 6l12 12"/>`,
        info: `<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M12 2a10 10 0 100 20A10 10 0 0012 2z"/>`,
    };

    const toast = document.createElement("div");
    toast.id = "rm-toast";
    toast.className = `fixed bottom-24 md:bottom-6 right-4 z-[999] flex items-center gap-3 px-4 py-3 rounded-2xl text-white text-sm font-semibold shadow-xl ${colors[type]} transition-all duration-300 translate-y-4 opacity-0`;
    toast.innerHTML = `
    <div class="w-6 h-6 flex-shrink-0">
      <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" class="w-6 h-6">
        ${icons[type]}
      </svg>
    </div>
    <span>${message}</span>
  `;
    document.body.appendChild(toast);

    // Animate in
    requestAnimationFrame(() => {
        requestAnimationFrame(() => {
            toast.classList.remove("translate-y-4", "opacity-0");
        });
    });

    // Auto dismiss after 3s
    setTimeout(() => {
        toast.classList.add("translate-y-4", "opacity-0");
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

// ─── Button Loading State ─────────────────────────────────────
function setButtonLoading(btn, loading, defaultText) {
    if (loading) {
        btn.disabled = true;
        btn.innerHTML = `
      <svg class="animate-spin w-4 h-4 inline mr-2" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"></path>
      </svg>Saving...`;
        btn.classList.add("opacity-75", "cursor-not-allowed");
    } else {
        btn.disabled = false;
        btn.innerHTML = defaultText;
        btn.classList.remove("opacity-75", "cursor-not-allowed");
    }
}

// ─── Save Profile ─────────────────────────────────────────────
async function saveProfile() {
    const fullName = document.getElementById("fullName")?.value?.trim();
    const email = document.getElementById("email")?.value?.trim();
    const phone = document.getElementById("phone")?.value?.trim();
   

    // Basic validation
    if (!fullName || !email) {
        showToast("Full name and email are required.", "error");
        return;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
        showToast("Please enter a valid email address.", "error");
        return;
    }

    const btn = document.querySelector('[onclick="saveProfile()"]');
    if (btn) setButtonLoading(btn, true, "Save Changes");

    try {
        const res = await fetch("/api/account/settings/update", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                name: fullName,
                email: email,
                mobile: phone,
            }),
        });

        if (!res.ok) {
            const err = await res.json();
            throw new Error(err.message || `Server error: ${res.status}`);
        }

        const data = await res.json();
        showToast(data.message || "Profile updated successfully!", "success");

        // Reflect name change in sidebar avatar text if present
        const avatarSpans = document.querySelectorAll(".w-9.h-9.rounded-full span, .w-8.h-8.rounded-full span");
        avatarSpans.forEach((s) => {
            if (s.classList.contains("font-bold") && fullName.length > 0) {
                s.textContent = fullName.charAt(0).toUpperCase();
            }
        });

    } catch (err) {
        console.error("saveProfile error:", err);
        showToast(err.message || "Failed to update profile. Try again.", "error");
    } finally {
        if (btn) setButtonLoading(btn, false, "Save Changes");
    }
}


async function saveBusinessDetails() {
    const payload = {
        businessName: document.getElementById("businessName")?.value,
        gstNumber: document.getElementById("gstNumber")?.value,
        businessEmail: document.getElementById("businessEmail")?.value,
        businessPhone: document.getElementById("businessPhone")?.value,
        category: document.getElementById("businessCategory")?.value,
        subcategory: document.getElementById("businessSubcategory")?.value,
        address: document.getElementById("businessAddress")?.value
    };

    const btn = document.querySelector('[onclick="saveBusinessDetails()"]');
    if (btn) setButtonLoading(btn, true, "Save Business Details");

    try {
        const response = await fetch("/api/account/settings/update", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            const err = await response.json();
            throw new Error(err.message || "Update failed");
        }

        const data = await response.json();
        showToast(data.message || "Business details updated successfully!");

    } catch (err) {
        console.error(err);
        showToast(err.message || "Something went wrong", "error");
    } finally {
        if (btn) setButtonLoading(btn, false, "Save Business Details");
    }
}

// ─── Save Localization ─────────────────────────────────────────
async function saveLocalization() {
    const payload = {
        country: document.getElementById("country")?.value,
        timezone: document.getElementById("timezone")?.value,
        language: document.getElementById("language")?.value,
        currency: document.getElementById("currency")?.value
    };

    const btn = document.querySelector('[onclick="saveLocalization()"]');
    if (btn) setButtonLoading(btn, true, "Save Regional Settings");

    try {
        const response = await fetch("/api/account/settings/update", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            const err = await response.json();
            throw new Error(err.message || "Update failed");
        }

        const data = await response.json();
        showToast(data.message || "Localization settings updated successfully!");

    } catch (err) {
        console.error(err);
        showToast(err.message || "Something went wrong", "error");
    } finally {
        if (btn) setButtonLoading(btn, false, "Save Regional Settings");
    }
}

// ─── Update Password ──────────────────────────────────────────
async function updatePassword() {
    const currentPassword = document.getElementById("currentPassword")?.value;
    const newPassword = document.getElementById("newPassword")?.value;
    const confirmPassword = document.getElementById("confirmPassword")?.value;

    // Validation
    if (!currentPassword || !newPassword || !confirmPassword) {
        showToast("Please fill in all password fields.", "error");
        return;
    }

    if (newPassword.length < 8) {
        showToast("New password must be at least 8 characters.", "error");
        return;
    }

    if (newPassword !== confirmPassword) {
        showToast("New passwords do not match.", "error");
        // Highlight mismatch fields
        document.getElementById("newPassword").classList.add("border-red-400", "ring-1", "ring-red-400");
        document.getElementById("confirmPassword").classList.add("border-red-400", "ring-1", "ring-red-400");
        setTimeout(() => {
            document.getElementById("newPassword")?.classList.remove("border-red-400", "ring-1", "ring-red-400");
            document.getElementById("confirmPassword")?.classList.remove("border-red-400", "ring-1", "ring-red-400");
        }, 3000);
        return;
    }

    const btn = document.querySelector('[onclick="updatePassword()"]');
    if (btn) setButtonLoading(btn, true, "Update Password");

    try {
        const res = await fetch("/api/settings/password", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                currentPassword: currentPassword,
                newPassword: newPassword,
            }),
        });

        if (!res.ok) {
            const err = await res.text();
            if (res.status === 401 || res.status === 403) {
                throw new Error("Current password is incorrect.");
            }
            throw new Error(err || `Server error: ${res.status}`);
        }

        showToast("Password updated successfully!", "success");

        // Clear password fields
        ["currentPassword", "newPassword", "confirmPassword"].forEach((id) => {
            const el = document.getElementById(id);
            if (el) el.value = "";
        });

    } catch (err) {
        console.error("updatePassword error:", err);
        showToast(err.message || "Failed to update password. Try again.", "error");
    } finally {
        if (btn) setButtonLoading(btn, false, "Update Password");
    }
}

// Update existing switch function to include 'groups'
function switchSettingsTab(t) {
    ['general', 'gateways', 'catalog', 'users', 'groups'].forEach(x => {
        const p = document.getElementById('sp-' + x);
        const b = document.getElementById('st-' + x);
        if (!p || !b) return;
        if (x === t) {
            p.classList.remove('hidden');
            b.classList.add('border-indigo-600', 'text-indigo-600');
            b.classList.remove('border-transparent', 'text-gray-500');
        } else {
            p.classList.add('hidden');
            b.classList.remove('border-indigo-600', 'text-indigo-600');
            b.classList.add('border-transparent', 'text-gray-500');
        }
    });
}

// Group Management Logic - FETCH FROM DATABASE
async function renderGroups() {
    try {
        const response = await fetch(`/api/customer-groups/my-groups`);
        const groups = await response.json();

        const grid = document.getElementById('groupListGrid');
        grid.innerHTML = '';

        if (groups.length === 0) {
            grid.innerHTML = `<p class="col-span-full text-center py-10 text-gray-400 text-sm italic">No custom groups created yet.</p>`;
            return;
        }

        groups.forEach(g => {
            grid.innerHTML += `
                <div class="bg-white rounded-2xl border border-gray-100 p-5 shadow-sm hover:shadow-md transition">
                    <div class="flex items-center justify-between mb-3">
                        <div class="w-10 h-10 rounded-xl bg-indigo-50 flex items-center justify-center text-indigo-600 font-bold">
                            ${g.name.charAt(0).toUpperCase()}
                        </div>
                        <button onclick="deleteGroup(${g.id})" class="text-gray-300 hover:text-red-500 transition">
                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
                            </svg>
                        </button>
                    </div>
                    <p class="text-sm font-bold text-gray-900">${g.name}</p>
                    <p class="text-[10px] text-gray-400 mt-1 uppercase tracking-wider">Group ID: #G-${g.id}</p>
                </div>`;
        });
    } catch (error) {
        console.error("Failed to load groups:", error);
    }
}

function openGroupModal() { document.getElementById('groupModal').classList.remove('hidden'); }
function closeGroupModal() { document.getElementById('groupModal').classList.add('hidden'); }

// SAVE TO DATABASE
async function saveNewGroup() {
    const nameInput = document.getElementById('newGroupName');
    const name = nameInput.value.trim();
    if (!name) return;

    const response = await fetch('/api/customer-groups/save', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: name })
    });

    if (response.ok) {
        window.location.reload();
    } else {
        const msg = await response.text();
        alert(msg);
    }
}

// DELETE FROM DATABASE
async function deleteGroup(id) {
    if (!confirm("Are you sure? Customers in this group will be moved to 'Default'.")) return;

    try {
        const response = await fetch(`/api/customer-groups/${id}`, {
            method: 'DELETE'
        });

        if (response.ok) {
            renderGroups();
            if (typeof showSettingsToast === 'function') showSettingsToast('Group deleted');
        }
    } catch (error) {
        console.error("Delete error:", error);
    }
}

// Load customer fields settings on page load
async function loadCustomerFieldsConfig() {
    try {
        const response = await fetch("/api/settings/customer-fields");
        if (!response.ok) throw new Error("Failed to fetch settings");
        const data = await response.json();

        for (let i = 1; i <= 5; i++) {
            const input = document.getElementById(`cfgField${i}`);
            if (input) {
                input.value = data[`field${i}Name`] || "";
            }
        }
    } catch (error) {
        console.error("Failed to load customer field configurations:", error);
    }
}

async function saveCustomFieldsConfig(event) {
    if (event) event.preventDefault();

    const payload = {
        field1Name: document.getElementById("cfgField1")?.value?.trim() || "",
        field2Name: document.getElementById("cfgField2")?.value?.trim() || "",
        field3Name: document.getElementById("cfgField3")?.value?.trim() || "",
        field4Name: document.getElementById("cfgField4")?.value?.trim() || "",
        field5Name: document.getElementById("cfgField5")?.value?.trim() || ""
    };

    const btn = document.querySelector("#customFieldsConfigForm button[type='submit']");
    if (btn) setButtonLoading(btn, true, "Save Fields");

    try {
        const response = await fetch("/api/settings/customer-fields", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            throw new Error(`Server error: ${response.status}`);
        }

        const data = await response.json();
        showToast("Customer fields updated successfully!", "success");

        for (let i = 1; i <= 5; i++) {
            const input = document.getElementById(`cfgField${i}`);
            if (input) {
                input.value = data[`field${i}Name`] || "";
            }
        }
    } catch (error) {
        console.error("Failed to save customer field configurations:", error);
        showToast("Failed to save configurations. Please try again.", "error");
    } finally {
        if (btn) setButtonLoading(btn, false, "Save Fields");
    }
}

// Initial call when page loads
document.addEventListener('DOMContentLoaded', () => {
    renderGroups();
    loadCustomerFieldsConfig();
});
// ======================================================
// LAYOUT.JS — Global UI Controller
// ======================================================

// ===== MOBILE NAV =====
function openMobileNav() {
    const drawer = document.getElementById("mobileNavDrawer");
    const bg = document.getElementById("mobileNavBg");

    if (drawer) {
        drawer.style.transform = "translateX(0)";
    }

    if (bg) {
        bg.classList.remove("hidden");
    }
}

function closeMobileNav() {
    const drawer = document.getElementById("mobileNavDrawer");
    const bg = document.getElementById("mobileNavBg");

    if (drawer) {
        drawer.style.transform = "translateX(-100%)";
    }

    if (bg) {
        bg.classList.add("hidden");
    }
}


// ===== AUTO ACTIVE NAV (Bottom + Sidebar) =====
document.addEventListener("DOMContentLoaded", () => {
    const currentPage = window.location.pathname.split("/").pop();

    const links = document.querySelectorAll("a[href]");

    links.forEach(link => {
        const href = link.getAttribute("href");

        if (!href) return;

        // Normalize (remove leading /)
        const cleanHref = href.replace("/", "");

        if (cleanHref === currentPage) {

            // Reset inactive style
            link.classList.remove("text-gray-400");
            link.classList.add("text-indigo-600");

            // Optional: highlight parent (sidebar items)
            link.classList.add("bg-indigo-50");

            // Make label bold
            const span = link.querySelector("span");
            if (span) {
                span.classList.add("font-semibold");
            }
        }
    });
});


// ===== CLOSE NAV WHEN CLICK OUTSIDE =====
document.addEventListener("click", function (e) {
    const drawer = document.getElementById("mobileNavDrawer");

    if (!drawer) return;

    const isClickInside = drawer.contains(e.target);
    const isMenuButton = e.target.closest("[onclick='openMobileNav()']");

    if (!isClickInside && !isMenuButton) {
        closeMobileNav();
    }
});


// ===== ESC KEY CLOSE (PRO UX) =====
document.addEventListener("keydown", function (e) {
    if (e.key === "Escape") {
        closeMobileNav();
    }
});



function toggleTheme() {
    document.documentElement.classList.toggle("dark");
}


(function() {
    let cachedSettings = null;

    try {
        const stored = sessionStorage.getItem('numen_localization');
        if (stored) {
            cachedSettings = JSON.parse(stored);
        }
    } catch (e) {}

    window.getLocalizationSettings = async function() {
        if (cachedSettings) return cachedSettings;
        try {
            const res = await fetch('/api/settings/localization');
            if (res.ok) {
                const data = await res.json();
                cachedSettings = data;
                try {
                    sessionStorage.setItem('numen_localization', JSON.stringify(data));
                } catch(e) {}
                updateDOMElements(data);
                return cachedSettings;
            }
        } catch (e) {
            console.error("Failed to load localization settings:", e);
        }
        const fallback = {
            country: 'India',
            timezone: 'Asia/Kolkata',
            language: 'English',
            currencyCode: 'INR',
            locale: 'en-IN'
        };
        updateDOMElements(fallback);
        return fallback;
    };

    function updateDOMElements(settings) {
        const code = settings.currencyCode || 'INR';
        const symbols = { INR: '₹', USD: '$', EUR: '€', GBP: '£', AED: 'AED ', SGD: 'S$' };
        const symbol = symbols[code] || '$';
        document.querySelectorAll('.currency-symbol').forEach(el => {
            el.textContent = symbol;
        });
    }

    window.formatMoney = function(amount, currencyCode, localeTag) {
        const value = Number(amount || 0);
        const code = currencyCode || (cachedSettings ? cachedSettings.currencyCode : 'INR');
        const locale = localeTag || (cachedSettings ? cachedSettings.locale : 'en-IN');
        try {
            return new Intl.NumberFormat(locale, { style: 'currency', currency: code }).format(value);
        } catch (e) {
            const symbols = { INR: '₹', USD: '$', EUR: '€', GBP: '£', AED: 'AED ', SGD: 'S$' };
            return (symbols[code] || '') + value.toFixed(2);
        }
    };

    window.formatDate = function(dateStr, localeTag) {
        if (!dateStr) return '';
        const date = new Date(dateStr);
        if (isNaN(date.getTime())) return dateStr;
        const locale = localeTag || (cachedSettings ? cachedSettings.locale : 'en-IN');
        return new Intl.DateTimeFormat(locale, { dateStyle: 'medium' }).format(date);
    };
    
    document.addEventListener('DOMContentLoaded', async () => {
        const settings = await window.getLocalizationSettings();
        updateDOMElements(settings);
    });
    window.getLocalizationSettings();
})();

console.log("layout.js loaded ✅");
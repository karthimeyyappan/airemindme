let promoList = [];
let promo_filter = 'all';
let promo_search = '';
let promo_page = 1;
const PROMO_PER_PAGE = 6;
let promo_searchTimer = null;
let promo_loaded = false;
let promo_deleteTargetId = null;

function promo_initPromos() {
  promo_showLoadingSkeleton();
  promo_toggleCardSkeletons(true);
  fetch('/api/promotions')
    .then(res => {
      if (!res.ok) throw new Error('Failed to fetch promotions');
      return res.json();
    })
    .then(data => {
      promoList = data;
      promo_updateCards();
      promo_toggleCardSkeletons(false);
      promo_renderPromos();
    })
    .catch(err => {
      console.error(err);
      promo_toggleCardSkeletons(false);
      if (typeof showToast === 'function') {
        showToast('Failed to load promotions');
      }
      const container = document.getElementById('promo-list');
      if (container) {
        container.innerHTML = `
          <div class="text-center py-12 bg-white rounded-2xl border border-gray-100 col-span-full">
            <div class="w-12 h-12 rounded-2xl bg-gray-100 flex items-center justify-center mx-auto mb-3">
              <svg class="w-6 h-6 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5.882V19.24a1.76 1.76 0 01-3.417.592l-2.147-6.15M18 13a3 3 0 100-6M5.436 13.683A4.001 4.001 0 017 6h1.832c4.1 0 7.625-1.234 9.168-3v14c-1.543-1.766-5.067-3-9.168-3H7a3.988 3.988 0 01-1.564-.317z"/>
              </svg>
            </div>
            <p class="text-sm font-semibold text-gray-500">No promotions found</p>
            <p class="text-xs text-gray-400 mt-1">
              Create a promotion from the Promotions page
            </p>
          </div>
        `;
      }
      const countLabel = document.getElementById('promo-count-label');
      if (countLabel) countLabel.textContent = 'Showing 0 promotions';
      const pag = document.getElementById('promo-pagination');
      if (pag) pag.classList.add('hidden');
    });
}

function promo_toggleCardSkeletons(show) {
  const cardTypes = ['active', 'paused', 'scheduled', 'reach'];
  cardTypes.forEach(type => {
    const skeletonEl = document.getElementById(`promo-card-${type}-skeleton`);
    const contentEl = document.getElementById(`promo-card-${type}-content`);
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

function promo_showLoadingSkeleton() {
  const container = document.getElementById('promo-list');
  if (container) {
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
    container.innerHTML = itemSkeleton.repeat(4);
  }
}

function promo_updateCards() {
  const active = promoList.filter(p => p.status === 'ACTIVE').length;
  const paused = promoList.filter(p => p.status === 'PAUSED').length;
  const scheduled = promoList.filter(p => p.status === 'SCHEDULED').length;
  const reach = promoList.reduce((s, p) => s + (p.recipientCount || 0), 0);

  const activeEl = document.getElementById('promo-card-active');
  const pausedEl = document.getElementById('promo-card-paused');
  const scheduledEl = document.getElementById('promo-card-scheduled');
  const reachEl = document.getElementById('promo-card-reach');

  if (activeEl) activeEl.textContent = active;
  if (pausedEl) pausedEl.textContent = paused;
  if (scheduledEl) scheduledEl.textContent = scheduled;
  if (reachEl) reachEl.textContent = reach;
}

function promo_setFilter(f) {
  promo_filter = f;
  promo_page = 1;

  ['all', 'active', 'paused', 'scheduled'].forEach(status => {
    const pill = document.getElementById(`promo-pill-${status}`);
    if (pill) {
      if (status === f) {
        pill.className = "bg-indigo-600 text-white rounded-full px-3.5 py-1.5 text-xs font-medium whitespace-nowrap cursor-pointer shadow-sm";
      } else {
        pill.className = "border border-gray-200 text-gray-600 rounded-full px-3.5 py-1.5 text-xs font-medium whitespace-nowrap cursor-pointer hover:border-gray-300 transition";
      }
    }
  });

  promo_renderPromos();
}

function promo_getFiltered() {
  return promoList.filter(p => {
    if (promo_filter !== 'all') {
      if (!p.status || p.status.toLowerCase() !== promo_filter) {
        return false;
      }
    }
    if (promo_search) {
      const q = promo_search.toLowerCase();
      const name = (p.name || '').toLowerCase();
      const desc = (p.description || '').toLowerCase();
      if (!name.includes(q) && !desc.includes(q)) {
        return false;
      }
    }
    return true;
  });
}

function promo_renderPromos() {
  const filtered = promo_getFiltered();
  const countLabel = document.getElementById('promo-count-label');
  if (countLabel) {
    countLabel.textContent = `Showing ${filtered.length} promotions`;
  }

  const container = document.getElementById('promo-list');
  const pag = document.getElementById('promo-pagination');

  if (filtered.length === 0) {
    if (container) {
      container.innerHTML = `
        <div class="text-center py-12 bg-white rounded-2xl border border-gray-100 col-span-full">
          <div class="w-12 h-12 rounded-2xl bg-gray-100 flex items-center justify-center mx-auto mb-3">
            <svg class="w-6 h-6 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5.882V19.24a1.76 1.76 0 01-3.417.592l-2.147-6.15M18 13a3 3 0 100-6M5.436 13.683A4.001 4.001 0 017 6h1.832c4.1 0 7.625-1.234 9.168-3v14c-1.543-1.766-5.067-3-9.168-3H7a3.988 3.988 0 01-1.564-.317z"/>
            </svg>
          </div>
          <p class="text-sm font-semibold text-gray-500">No promotions found</p>
          <p class="text-xs text-gray-400 mt-1">
            Create a promotion from the Promotions page
          </p>
        </div>
      `;
    }
    if (pag) pag.classList.add('hidden');
    return;
  }

  if (pag) pag.classList.remove('hidden');

  const totalPages = Math.ceil(filtered.length / PROMO_PER_PAGE);
  if (promo_page > totalPages) {
    promo_page = totalPages;
  }
  if (promo_page < 1) {
    promo_page = 1;
  }

  const startIdx = (promo_page - 1) * PROMO_PER_PAGE;
  const endIdx = startIdx + PROMO_PER_PAGE;
  const pageItems = filtered.slice(startIdx, endIdx);

  if (container) {
    container.innerHTML = pageItems.map(p => promo_renderCard(p)).join('');
  }

  promo_renderPagination(filtered.length);
}

function promo_renderCard(p) {
  const name = escapeHtml(p.name);
  const truncatedDesc = promo_truncate(p.description, 80);
  const desc = escapeHtml(truncatedDesc);
  const totalViews = p.totalViews || 0;
  const totalLikes = p.totalLikes || 0;
  const totalEnquiries = p.totalEnquiries || 0;
  const recipientCount = p.recipientCount || 0;

  let iconBg = 'bg-gray-50';
  let iconText = 'text-gray-400';
  let badgeCls = 'bg-gray-100 text-gray-600';

  if (p.status === 'ACTIVE') {
    iconBg = 'bg-emerald-50';
    iconText = 'text-emerald-600';
    badgeCls = 'bg-emerald-100 text-emerald-700';
  } else if (p.status === 'PAUSED') {
    iconBg = 'bg-amber-50';
    iconText = 'text-amber-500';
    badgeCls = 'bg-amber-100 text-amber-700';
  } else if (p.status === 'SCHEDULED') {
    iconBg = 'bg-blue-50';
    iconText = 'text-blue-600';
    badgeCls = 'bg-blue-100 text-blue-700';
  } else if (p.status === 'PENDING') {
    badgeCls = 'bg-gray-100 text-gray-600';
  }

  const badgeSpan = `<span class="text-[10px] px-1.5 py-0.5 rounded-full font-medium ${badgeCls}">${escapeHtml(p.status)}</span>`;

  // Action Buttons
  let actionBtn = '';
  if (p.status === 'ACTIVE' || p.status === 'SCHEDULED') {
    actionBtn = `
      <button onclick="promo_pause(${p.id})"
        class="promo-pause-btn-${p.id} px-2.5 py-1.5 text-xs font-medium bg-amber-50 border border-amber-200 text-amber-600 rounded-lg hover:bg-amber-100 transition">
        Pause
      </button>
    `;
  } else if (p.status === 'PAUSED') {
    actionBtn = `
      <button onclick="promo_resume(${p.id})"
        class="promo-resume-btn-${p.id} px-2.5 py-1.5 text-xs font-medium bg-emerald-50 border border-emerald-200 text-emerald-600 rounded-lg hover:bg-emerald-100 transition">
        Resume
      </button>
    `;
  }

  // Item tags
  let itemTags = '';
  if (p.itemNames && Array.isArray(p.itemNames)) {
    itemTags = p.itemNames.map(iname => `
      <span class="text-[10px] px-2 py-0.5 rounded-full bg-gray-100 text-gray-600 font-medium">${escapeHtml(iname)}</span>
    `).join('');
  }

  // Footer Right Content
  let footerRight = '';
  if (p.status === 'PAUSED') {
    footerRight = `
      <span class="text-[10px] text-amber-600 bg-amber-50 px-2 py-0.5 rounded-full font-medium">
        ⏸ Paused — not visible to customers
      </span>
    `;
  } else if (p.status === 'ACTIVE') {
    footerRight = `
      <a href="/promotions.html" class="text-xs text-indigo-600 hover:underline font-medium">
        Manage →
      </a>
    `;
  }

  // SVGs
  const megaphoneSvg = `<svg class="w-5 h-5 ${iconText}" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5.882V19.24a1.76 1.76 0 01-3.417.592l-2.147-6.15M18 13a3 3 0 100-6M5.436 13.683A4.001 4.001 0 017 6h1.832c4.1 0 7.625-1.234 9.168-3v14c-1.543-1.766-5.067-3-9.168-3H7a3.988 3.988 0 01-1.564-.317z"/></svg>`;
  const eyeSvg = `<svg class="w-3.5 h-3.5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/></svg>`;
  const heartSvg = `<svg class="w-3.5 h-3.5 text-pink-400" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M3.172 5.172a4 4 0 015.656 0L10 6.343l1.172-1.171a4 4 0 115.656 5.656L10 17.657l-6.828-6.829a4 4 0 010-5.656z" clip-rule="evenodd"/></svg>`;
  const chatSvg = `<svg class="w-3.5 h-3.5 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"/></svg>`;
  const usersSvg = `<svg class="w-3.5 h-3.5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"/></svg>`;

  const escapedNameJsLiteral = name.replace(/'/g, "\\'");

  return `
    <div class="bg-white rounded-2xl border border-gray-100 shadow-sm p-4 hover:shadow-md transition">
      <!-- Row 1: icon + name + status + action buttons -->
      <div class="flex items-start justify-between gap-3 mb-3">
        <div class="flex items-start gap-3 flex-1 min-w-0">
          <!-- Status-colored icon -->
          <div class="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0 ${iconBg}">
            ${megaphoneSvg}
          </div>
          <div class="flex-1 min-w-0">
            <!-- Name + status badge -->
            <div class="flex items-center gap-2 flex-wrap mb-0.5">
              <p class="text-sm font-bold text-gray-900 truncate">${name}</p>
              ${badgeSpan}
            </div>
            <!-- Description truncated -->
            <p class="text-xs text-gray-500 line-clamp-2">
              ${desc}
            </p>
          </div>
        </div>

        <!-- Action buttons top-right -->
        <div class="flex items-center gap-1 flex-shrink-0">
          <button onclick="promo_openStats(${p.id})"
            class="px-2.5 py-1.5 text-xs font-medium bg-indigo-50 border border-indigo-200 text-indigo-600 rounded-lg hover:bg-indigo-100 transition">
            Stats
          </button>
          ${actionBtn}
          <button onclick="promo_confirmDelete(${p.id}, '${escapedNameJsLiteral}')"
            class="px-2.5 py-1.5 text-xs font-medium bg-red-50 border border-red-200 text-red-500 rounded-lg hover:bg-red-100 transition">
            Delete
          </button>
        </div>
      </div>

      <!-- Row 2: item name tags -->
      <div class="flex flex-wrap gap-1.5 mb-3">
        ${itemTags}
      </div>

      <!-- Row 3: stats strip -->
      <div class="flex items-center gap-4 py-2 px-3 bg-gray-50 rounded-xl mb-3 flex-wrap">
        <span class="flex items-center gap-1 text-xs text-gray-600">
          ${eyeSvg}
          ${totalViews} Views
        </span>
        <span class="flex items-center gap-1 text-xs text-gray-600">
          ${heartSvg}
          ${totalLikes} Likes
        </span>
        <span class="flex items-center gap-1 text-xs text-gray-600">
          ${chatSvg}
          ${totalEnquiries} Enquiries
        </span>
        <span class="ml-auto flex items-center gap-1 text-xs text-gray-500">
          ${usersSvg}
          ${recipientCount} recipients
        </span>
      </div>

      <!-- Row 4: footer -->
      <div class="flex items-center justify-between">
        <p class="text-[10px] text-gray-400">
          Created: ${promo_formatDate(p.createdAt)}
        </p>
        ${footerRight}
      </div>
    </div>
  `;
}

function promo_goPage(n) {
  const filtered = promo_getFiltered();
  const totalPages = Math.ceil(filtered.length / PROMO_PER_PAGE);
  if (n < 1 || n > totalPages) return;
  promo_page = n;
  promo_renderPromos();
}

function promo_renderPagination(total) {
  const totalPages = Math.ceil(total / PROMO_PER_PAGE);
  const startIdx = total === 0 ? 0 : (promo_page - 1) * PROMO_PER_PAGE + 1;
  const endIdx = Math.min(total, promo_page * PROMO_PER_PAGE);

  const label = document.getElementById('promo-page-label');
  if (label) {
    label.textContent = `Showing ${startIdx}–${endIdx} of ${total} promotions`;
  }

  const prevBtn = document.getElementById('promo-prev');
  const nextBtn = document.getElementById('promo-next');
  if (prevBtn) prevBtn.disabled = (promo_page === 1);
  if (nextBtn) nextBtn.disabled = (promo_page === totalPages || totalPages === 0);

  const pagesContainer = document.getElementById('promo-pages');
  if (pagesContainer) {
    let pagesHtml = '';
    for (let i = 1; i <= totalPages; i++) {
      if (i === promo_page) {
        pagesHtml += `
          <button onclick="promo_goPage(${i})"
            class="w-7 h-7 rounded-lg bg-indigo-600 text-white text-xs font-semibold flex items-center justify-center transition cursor-pointer">
            ${i}
          </button>
        `;
      } else {
        pagesHtml += `
          <button onclick="promo_goPage(${i})"
            class="w-7 h-7 rounded-lg border border-gray-200 text-gray-600 text-xs font-medium hover:border-indigo-400 hover:text-indigo-600 flex items-center justify-center transition cursor-pointer">
            ${i}
          </button>
        `;
      }
    }
    pagesContainer.innerHTML = pagesHtml;
  }
}

function promo_prevPage() {
  promo_goPage(promo_page - 1);
}

function promo_nextPage() {
  promo_goPage(promo_page + 1);
}

function promo_pause(id) {
  const btn = document.querySelector('.promo-pause-btn-' + id);
  if (btn) {
    btn.classList.add('opacity-50', 'pointer-events-none');
  }

  // Show loading skeleton in the list area while pausing
  promo_showLoadingSkeleton();

  fetch(`/api/promotions/${id}/pause`, {
    method: 'POST'
  })
  .then(res => {
    if (!res.ok) throw new Error('Failed to pause');
    return res.json();
  })
  .then(data => {
    if (typeof showToast === 'function') {
      showToast('Promotion paused ⏸');
    }
    const promo = promoList.find(p => p.id === id);
    if (promo) {
      promo.status = 'PAUSED';
    }
    promo_updateCards();
    promo_renderPromos();
  })
  .catch(err => {
    console.error(err);
    if (typeof showToast === 'function') {
      showToast('Failed to pause promotion');
    }
    // Restore original list rendering if failed
    promo_renderPromos();
  });
}

function promo_resume(id) {
  const btn = document.querySelector('.promo-resume-btn-' + id);
  if (btn) {
    btn.classList.add('opacity-50', 'pointer-events-none');
  }

  // Show loading skeleton in the list area while resuming
  promo_showLoadingSkeleton();

  fetch(`/api/promotions/${id}/resume`, {
    method: 'POST'
  })
  .then(res => {
    if (!res.ok) throw new Error('Failed to resume');
    return res.json();
  })
  .then(data => {
    if (typeof showToast === 'function') {
      showToast('Promotion resumed ▶️');
    }
    const promo = promoList.find(p => p.id === id);
    if (promo) {
      promo.status = 'ACTIVE';
    }
    promo_updateCards();
    promo_renderPromos();
  })
  .catch(err => {
    console.error(err);
    if (typeof showToast === 'function') {
      showToast('Failed to resume promotion');
    }
    // Restore original list rendering if failed
    promo_renderPromos();
  });
}

function promo_confirmDelete(id, name) {
  promo_deleteTargetId = id;
  const nameSpan = document.getElementById('promo-delete-name');
  if (nameSpan) {
    nameSpan.textContent = name;
  }
  const modal = document.getElementById('promo-delete-modal');
  if (modal) {
    modal.classList.remove('hidden');
  }
}

function promo_closeDeleteModal() {
  const modal = document.getElementById('promo-delete-modal');
  if (modal) {
    modal.classList.add('hidden');
  }
  promo_deleteTargetId = null;
}

function promo_executeDelete() {
  if (!promo_deleteTargetId) return;
  const btn = document.getElementById('promo-delete-confirm-btn');
  if (btn) {
    btn.disabled = true;
    btn.textContent = 'Deleting...';
  }

  fetch(`/api/promotions/${promo_deleteTargetId}`, {
    method: 'DELETE'
  })
  .then(res => {
    if (!res.ok) throw new Error('Failed to delete');
    return res.json();
  })
  .then(data => {
    promo_closeDeleteModal();
    if (typeof showToast === 'function') {
      showToast('Promotion deleted permanently 🗑️');
    }
    
    // After delete: show skeleton briefly while promoList updates
    promo_showLoadingSkeleton();
    setTimeout(() => {
      promoList = promoList.filter(p => p.id !== promo_deleteTargetId);
      promo_updateCards();
      promo_renderPromos();
    }, 300);
  })
  .catch(err => {
    console.error(err);
    if (typeof showToast === 'function') {
      showToast('Failed to delete promotion');
    }
  })
  .finally(() => {
    if (btn) {
      btn.disabled = false;
      btn.textContent = 'Delete Permanently';
    }
  });
}

function promo_onSearchInput() {
  if (promo_searchTimer) clearTimeout(promo_searchTimer);
  promo_searchTimer = setTimeout(() => {
    const input = document.getElementById('promo-search-input');
    promo_search = input ? input.value.trim() : '';
    promo_page = 1;
    promo_renderPromos();
  }, 400);
}

function promo_openStats(id) {
  const modal = document.getElementById('promo-stats-modal');
  if (modal) {
    modal.classList.remove('hidden');
  }

  const p = promoList.find(x => x.id === id);
  const nameEl = document.getElementById('promo-stats-name');
  if (nameEl) {
    nameEl.textContent = p ? p.name : '';
  }

  const bodyEl = document.getElementById('promo-stats-body');
  if (bodyEl) {
    bodyEl.innerHTML = `
      <div class="animate-pulse space-y-4">
        <div class="grid grid-cols-3 gap-2">
          <div class="bg-gray-100 h-16 rounded-xl"></div>
          <div class="bg-gray-100 h-16 rounded-xl"></div>
          <div class="bg-gray-100 h-16 rounded-xl"></div>
        </div>
        <div class="space-y-3">
          <div class="h-8 bg-gray-100 rounded-lg w-full"></div>
          <div class="h-8 bg-gray-100 rounded-lg w-full"></div>
          <div class="h-8 bg-gray-100 rounded-lg w-full"></div>
          <div class="h-8 bg-gray-100 rounded-lg w-full"></div>
        </div>
      </div>
    `;
  }

  fetch(`/api/promotions/${id}/analytics`)
    .then(res => {
      if (!res.ok) throw new Error('Failed to load analytics');
      return res.json();
    })
    .then(data => {
      if (!bodyEl) return;

      const rec = data.recipientCount || 0;
      const views = data.totalViews || 0;
      const likes = data.totalLikes || 0;
      const enquiries = data.totalEnquiries || 0;
      const wa = data.totalWhatsappClicks || 0;
      const phone = data.totalPhoneClicks || 0;
      const email = data.totalEmailClicks || 0;

      const enqPct = rec === 0 ? 0 : Math.min(100, Math.round(enquiries / rec * 100));
      const waPct = rec === 0 ? 0 : Math.min(100, Math.round(wa / rec * 100));
      const phonePct = rec === 0 ? 0 : Math.min(100, Math.round(phone / rec * 100));
      const emailPct = rec === 0 ? 0 : Math.min(100, Math.round(email / rec * 100));

      bodyEl.innerHTML = `
        <div class="grid grid-cols-3 gap-2 mb-4">
          <div class="bg-gray-50 border border-gray-100 rounded-xl p-2.5 text-center">
            <p class="text-[10px] font-semibold text-gray-500">Recipients</p>
            <p class="text-base font-bold text-gray-800">${rec}</p>
          </div>
          <div class="bg-blue-50 border border-blue-100 rounded-xl p-2.5 text-center">
            <p class="text-[10px] font-semibold text-blue-600">Total Views</p>
            <p class="text-base font-bold text-blue-800">${views}</p>
          </div>
          <div class="bg-pink-50 border border-pink-100 rounded-xl p-2.5 text-center">
            <p class="text-[10px] font-semibold text-pink-600">Total Likes</p>
            <p class="text-base font-bold text-pink-800">${likes}</p>
          </div>
        </div>

        <div class="space-y-4">
          <div class="space-y-1">
            <div class="flex justify-between text-xs">
              <span class="text-gray-600">Enquiry Rate</span>
              <span class="font-semibold text-gray-800">${enqPct}% (${enquiries})</span>
            </div>
            <div class="bg-gray-100 rounded-full h-2">
              <div class="bg-indigo-500 h-2 rounded-full transition-all duration-500" style="width:${enqPct}%"></div>
            </div>
          </div>

          <div class="space-y-1">
            <div class="flex justify-between text-xs">
              <span class="text-gray-600">WhatsApp Clicks</span>
              <span class="font-semibold text-gray-800">${wa}</span>
            </div>
            <div class="bg-gray-100 rounded-full h-2">
              <div class="bg-indigo-500 h-2 rounded-full transition-all duration-500" style="width:${waPct}%"></div>
            </div>
          </div>

          <div class="space-y-1">
            <div class="flex justify-between text-xs">
              <span class="text-gray-600">Phone Clicks</span>
              <span class="font-semibold text-gray-800">${phone}</span>
            </div>
            <div class="bg-gray-100 rounded-full h-2">
              <div class="bg-indigo-500 h-2 rounded-full transition-all duration-500" style="width:${phonePct}%"></div>
            </div>
          </div>

          <div class="space-y-1">
            <div class="flex justify-between text-xs">
              <span class="text-gray-600">Email Clicks</span>
              <span class="font-semibold text-gray-800">${email}</span>
            </div>
            <div class="bg-gray-100 rounded-full h-2">
              <div class="bg-indigo-500 h-2 rounded-full transition-all duration-500" style="width:${emailPct}%"></div>
            </div>
          </div>
        </div>
      `;
    })
    .catch(err => {
      console.error(err);
      if (bodyEl) {
        bodyEl.innerHTML = `<p class="text-xs text-red-500 text-center py-4">Failed to load analytics</p>`;
      }
    });
}

function promo_closeStats() {
  const modal = document.getElementById('promo-stats-modal');
  if (modal) {
    modal.classList.add('hidden');
  }
}

function promo_formatDate(dateStr) {
  if (!dateStr) return '—';
  const d = new Date(dateStr);
  return d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
}

function promo_truncate(str, n) {
  if (!str) return '';
  return str.length > n ? str.substring(0, n) + '...' : str;
}

function escapeHtml(str) {
  if (typeof str !== 'string') return '';
  return str.replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
}

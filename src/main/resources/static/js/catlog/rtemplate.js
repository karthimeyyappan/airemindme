let rtRowSeq = 0;

const RT_FIELD_TYPES = ['Text','Number','Date','Currency','Phone','Email','Status','Dropdown','Textarea'];

function addRow(data = {}) {
  const id = 'row_' + (rtRowSeq++);
  const tr = document.createElement('tr');
  tr.id = id;
  tr.innerHTML = `
    <td class="px-2 py-1.5"><input type="text" class="f-name w-full px-2 py-1.5 rounded-lg border border-gray-200 text-xs" value="${data.fieldName || ''}" placeholder="e.g. Customer Name"></td>
    <td class="px-2 py-1.5">
      <select class="f-type w-full px-2 py-1.5 rounded-lg border border-gray-200 text-xs bg-white">
        ${RT_FIELD_TYPES.map(t => `<option value="${t}" ${data.fieldType===t?'selected':''}>${t}</option>`).join('')}
      </select>
    </td>
    <td class="px-2 py-1.5"><input type="text" class="f-hint w-full px-2 py-1.5 rounded-lg border border-gray-200 text-xs" value="${data.hint || ''}" placeholder="e.g. John / TN09AB1234"></td>
    <td class="px-2 py-1.5 text-right">
      <button type="button" onclick="document.getElementById('${id}').remove()" class="text-red-400 hover:text-red-600">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
      </button>
    </td>`;
  document.getElementById('reportRows').appendChild(tr);
}

function collectColumns() {
  return Array.from(document.querySelectorAll('#reportRows tr')).map(tr => {
    const fieldName = tr.querySelector('.f-name').value.trim();
    const fieldType = tr.querySelector('.f-type').value;
    const hintVal = tr.querySelector('.f-hint').value.trim();
    return {
      fieldName: fieldName,
      fieldType: fieldType,
      description: hintVal,
      hint: hintVal
    };
  }).filter(c => c.fieldName);
}

const RT_EXAMPLES = {
  finance:  { title: 'Loan Collection Report', category: 'Finance', fields: [
              {fieldName:'Customer Name', fieldType:'Text', hint:'John'},
              {fieldName:'Loan Amount', fieldType:'Currency', hint:'250000'},
              {fieldName:'Outstanding', fieldType:'Currency', hint:'45000'},
              {fieldName:'Status', fieldType:'Status', hint:'Pending'}]},
  insurance:{ title: 'Insurance Renewal Report', category: 'Insurance', fields: [
              {fieldName:'Customer Name', fieldType:'Text', hint:'John'},
              {fieldName:'Policy Number', fieldType:'Text', hint:'POL-10293'},
              {fieldName:'Renewal Date', fieldType:'Date', hint:''},
              {fieldName:'Premium', fieldType:'Currency', hint:'5400'},
              {fieldName:'Status', fieldType:'Status', hint:'Active'}]},
  vehicle:  { title: 'Vehicle Service Report', category: 'Vehicle Service', fields: [
              {fieldName:'Customer Name', fieldType:'Text', hint:'John'},
              {fieldName:'Vehicle Number', fieldType:'Text', hint:'TN09AB1234'},
              {fieldName:'Service Type', fieldType:'Text', hint:'General Service'},
              {fieldName:'Next Service Date', fieldType:'Date', hint:''},
              {fieldName:'Amount', fieldType:'Currency', hint:'1500'}]},
  school:   { title: 'School Fee Report', category: 'Education', fields: [
              {fieldName:'Student Name', fieldType:'Text', hint:'John'},
              {fieldName:'Fee Due', fieldType:'Currency', hint:'12000'},
              {fieldName:'Due Date', fieldType:'Date', hint:''},
              {fieldName:'Status', fieldType:'Status', hint:'Pending'}]},
  medical:  { title: 'Lab Test Report', category: 'Medical', fields: [
              {fieldName:'Patient Name', fieldType:'Text', hint:'John'},
              {fieldName:'Test Name', fieldType:'Text', hint:'Hemoglobin'},
              {fieldName:'Result', fieldType:'Number', hint:'14.5'},
              {fieldName:'Reference Range', fieldType:'Text', hint:'13-17'}]},
  amc:      { title: 'AMC Service Report', category: 'AMC Service', fields: [
              {fieldName:'Customer Name', fieldType:'Text', hint:'John'},
              {fieldName:'Equipment / Asset', fieldType:'Text', hint:'AC Unit 1'},
              {fieldName:'Service Due Date', fieldType:'Date', hint:''},
              {fieldName:'Status', fieldType:'Status', hint:'Due'}]}
};

function loadRTExample(key) {
  const ex = RT_EXAMPLES[key];
  if (!ex) return;
  document.getElementById('rt_title').value = ex.title;
  document.getElementById('rt_category').value = ex.category;
  document.getElementById('reportRows').innerHTML = '';
  ex.fields.forEach(f => addRow(f));
  document.getElementById('rtPreviewPanel').classList.add('hidden');
}

function openRTWithPreset(key) {
  document.getElementById('rtModalTitle').textContent = 'Create Report Template';
  document.getElementById('rtSaveLabel').textContent = 'Save Template';
  document.getElementById('rt_title').value = '';
  document.getElementById('rt_category').value = 'Custom';
  document.getElementById('rt_price').value = '';
  document.getElementById('rt_total').checked = true;
  document.getElementById('rt_desc').value = '';
  document.getElementById('reportRows').innerHTML = '';
  document.getElementById('rtPreviewPanel').classList.add('hidden');
  if (key && key !== 'blank') loadRTExample(key);
  document.getElementById('rtModal').classList.remove('hidden');
  window.rtEditingId = null;
  if (typeof editRTId !== 'undefined') {
    editRTId = null;
  }
}

function previewRTTemplate() {
  const cols = collectColumns();
  const panel = document.getElementById('rtPreviewPanel');
  if (!cols.length) {
    panel.innerHTML = `<p class="text-gray-400">Add a few fields above to see the preview.</p>`;
    panel.classList.remove('hidden');
    return;
  }
  panel.innerHTML = cols.map(c =>
    `<div class="flex justify-between border-b border-gray-100 pb-1.5"><span class="font-medium text-gray-700">${c.fieldName}</span><span class="text-gray-400">${c.hint || '__________'}</span></div>`
  ).join('');
  panel.classList.remove('hidden');
}

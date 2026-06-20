// theme.js - Universal Dark Mode Toggle
// Inject this before </body> in all pages

// (function () {
//     ── Apply saved theme immediately to prevent flash ──
//     const saved = localStorage.getItem('rm_theme') || 'light';
//     document.documentElement.setAttribute('data-theme', saved);

//     function applyTheme(theme) {
//         document.documentElement.setAttribute('data-theme', theme);
//         localStorage.setItem('rm_theme', theme);

//         document.querySelectorAll('.theme-toggle-btn').forEach(btn => {
//             btn.setAttribute(
//                 'aria-label',
//                 theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'
//             );
//         });
//     }

//     window.toggleTheme = function () {
//         const current = document.documentElement.getAttribute('data-theme') || 'light';
//         applyTheme(current === 'dark' ? 'light' : 'dark');
//     };


//     document.addEventListener('DOMContentLoaded', function () {
//         applyTheme(saved);
//     });
// })();
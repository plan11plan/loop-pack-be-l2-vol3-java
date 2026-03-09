import { ProductApi, BrandApi } from '../api.js';

let currentPage = 0;
let currentBrandId = null;
let currentSort = null;
let brands = [];

export async function initHome() {
    const app = document.getElementById('app');
    app.innerHTML = `
        <div class="filter-bar" id="filter-bar">
            <select id="filter-brand"><option value="">전체 브랜드</option></select>
            <select id="filter-sort">
                <option value="">기본 정렬</option>
                <option value="price_asc">가격 낮은순</option>
                <option value="price_desc">가격 높은순</option>
                <option value="likes_desc">좋아요순</option>
            </select>
            <button class="btn btn-sm btn-primary" id="filter-apply">적용</button>
            <span class="result-count" id="result-count"></span>
        </div>
        <div class="product-grid" id="product-grid"></div>
        <div class="pagination" id="pagination"></div>`;

    await loadBrands();

    document.getElementById('filter-apply').addEventListener('click', () => {
        currentBrandId = document.getElementById('filter-brand').value || null;
        currentSort = document.getElementById('filter-sort').value || null;
        loadProducts(0);
    });

    // restore filter state
    if (currentBrandId) document.getElementById('filter-brand').value = currentBrandId;
    if (currentSort) document.getElementById('filter-sort').value = currentSort;

    await loadProducts(currentPage);
}

async function loadBrands() {
    try {
        const data = await BrandApi.list(0, 200);
        brands = data.items;
        const sel = document.getElementById('filter-brand');
        brands.forEach(b => {
            sel.insertAdjacentHTML('beforeend', `<option value="${b.id}">${esc(b.name)}</option>`);
        });
    } catch { /* ignore */ }
}

async function loadProducts(page) {
    currentPage = page;
    try {
        const data = await ProductApi.list(page, 20, currentBrandId, currentSort);
        const grid = document.getElementById('product-grid');

        document.getElementById('result-count').textContent =
            `총 ${data.totalElements.toLocaleString()}개 상품`;

        if (data.items.length === 0) {
            grid.innerHTML = '<div class="empty-state"><div class="icon">📦</div><p>상품이 없습니다</p></div>';
        } else {
            grid.innerHTML = data.items.map(p => `
                <a href="#product/${p.id}" class="product-card">
                    <div class="product-card-img">${getInitial(p.name)}</div>
                    <div class="product-card-body">
                        <div class="product-card-brand">${esc(p.brandName)}</div>
                        <div class="product-card-name">${esc(p.name)}</div>
                        <div class="product-card-footer">
                            <span class="product-card-price">${p.price.toLocaleString()}원</span>
                            <span class="product-card-likes">&hearts; ${formatCount(p.likeCount)}</span>
                        </div>
                    </div>
                </a>`).join('');
        }

        renderPagination(data);
    } catch (e) { Toast.error(e.message); }
}

function renderPagination(data) {
    const c = document.getElementById('pagination');
    if (data.totalPages <= 1) { c.innerHTML = ''; return; }
    c.innerHTML = `
        <button class="btn btn-sm btn-secondary" id="pg-prev" ${data.page === 0 ? 'disabled' : ''}>이전</button>
        <span class="page-info">${data.page + 1} / ${data.totalPages}</span>
        <button class="btn btn-sm btn-secondary" id="pg-next" ${data.page >= data.totalPages - 1 ? 'disabled' : ''}>다음</button>`;
    document.getElementById('pg-prev').addEventListener('click', () => loadProducts(data.page - 1));
    document.getElementById('pg-next').addEventListener('click', () => loadProducts(data.page + 1));
}

function getInitial(name) {
    return (name || '?').charAt(0);
}

function formatCount(n) {
    if (n >= 10000) return (n / 10000).toFixed(1) + '만';
    if (n >= 1000) return (n / 1000).toFixed(1) + '천';
    return n;
}

function esc(s) { return (s || '').replace(/</g, '&lt;').replace(/>/g, '&gt;'); }

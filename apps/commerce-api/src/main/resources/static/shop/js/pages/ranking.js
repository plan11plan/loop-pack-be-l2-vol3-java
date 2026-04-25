import { RankingApi } from '../api.js';

let currentPage = 1;
let currentDate = null;
let currentPeriod = 'WEEKLY';

const PERIOD_LABELS = {
    DAILY:   { subtitle: '오늘의 인기상품',   windowDays: 1  },
    WEEKLY:  { subtitle: '주간 인기상품',     windowDays: 7  },
    MONTHLY: { subtitle: '월간 인기상품',     windowDays: 30 },
};

export async function initRanking() {
    const app = document.getElementById('app');
    const today = new Date().toISOString().slice(0, 10);
    app.innerHTML = `
        <div class="ranking-container">
            <div class="ranking-header">
                <div class="ranking-title-area">
                    <h1 class="ranking-title">POPULAR RANKING</h1>
                    <p class="ranking-subtitle" id="ranking-subtitle">${PERIOD_LABELS[currentPeriod].subtitle}</p>
                </div>
                <div class="ranking-date-picker">
                    <input type="date" id="ranking-date" class="ranking-date-input" value="${today}" max="${today}">
                </div>
            </div>
            <div class="ranking-tabs" id="ranking-tabs">
                <button class="ranking-tab" data-period="DAILY">일간</button>
                <button class="ranking-tab active" data-period="WEEKLY">주간</button>
                <button class="ranking-tab" data-period="MONTHLY">월간</button>
            </div>
            <div class="ranking-list" id="ranking-list"></div>
            <div class="pagination" id="ranking-pagination"></div>
        </div>`;

    document.getElementById('ranking-date').addEventListener('change', (e) => {
        currentDate = e.target.value || null;
        loadRanking(1);
    });

    document.querySelectorAll('.ranking-tab').forEach(tab => {
        tab.addEventListener('click', () => {
            if (tab.dataset.period === currentPeriod) return;
            currentPeriod = tab.dataset.period;
            document.querySelectorAll('.ranking-tab').forEach(t =>
                t.classList.toggle('active', t.dataset.period === currentPeriod));
            loadRanking(1);
        });
    });

    currentDate = null;
    currentPage = 1;
    currentPeriod = 'WEEKLY';
    await loadRanking(currentPage);
}

function formatDate(dateStr) {
    return dateStr.replace(/-/g, '');
}

function rangeText(anchorDateStr, period) {
    const anchor = anchorDateStr ? new Date(anchorDateStr) : new Date();
    const to = anchor.toISOString().slice(0, 10);
    const windowDays = PERIOD_LABELS[period].windowDays;
    if (windowDays === 1) return to;
    const from = new Date(anchor);
    from.setDate(from.getDate() - (windowDays - 1));
    return `${from.toISOString().slice(0, 10)} ~ ${to}`;
}

async function loadRanking(page) {
    currentPage = page;

    const list = document.getElementById('ranking-list');
    let data;
    try {
        const apiDate = currentDate ? formatDate(currentDate) : null;
        data = await RankingApi.list(apiDate, page, 20, currentPeriod);
    } catch (e) {
        updateSubtitle();
        list.innerHTML = `<div class="ranking-empty">랭킹 조회에 실패했습니다: ${esc(e.message || '알 수 없는 오류')}</div>`;
        document.getElementById('ranking-pagination').innerHTML = '';
        return;
    }

    updateSubtitle();

    if (!data.items || data.items.length === 0) {
        list.innerHTML = `<div class="ranking-empty">해당 기간의 랭킹 데이터가 없습니다.</div>`;
        document.getElementById('ranking-pagination').innerHTML = '';
        return;
    }

    list.innerHTML = data.items.map(item => `
        <a href="#product/${item.productId}" class="ranking-item">
            <div class="ranking-rank ${item.rank <= 3 ? 'ranking-rank-top' : ''} ${item.rank === 1 ? 'ranking-rank-1' : ''}">
                ${item.rank}
            </div>
            <div class="ranking-thumb">
                ${item.thumbnailUrl && item.thumbnailUrl.startsWith('http')
                    ? `<img src="${esc(item.thumbnailUrl)}" alt="${esc(item.productName)}">`
                    : `<span>${(item.productName || '?').charAt(0)}</span>`}
            </div>
            <div class="ranking-info">
                <div class="ranking-brand">${esc(item.brandName)}</div>
                <div class="ranking-name">${esc(item.productName)}</div>
            </div>
            <div class="ranking-right">
                <div class="ranking-price">${item.price.toLocaleString()}원</div>
                <div class="ranking-score">score ${Number(item.score).toFixed(1)}</div>
            </div>
        </a>`).join('');

    renderPagination(data);
}

function updateSubtitle() {
    const sub = document.getElementById('ranking-subtitle');
    const base = PERIOD_LABELS[currentPeriod].subtitle;
    const range = rangeText(currentDate, currentPeriod);
    sub.innerHTML = `${base} · ${range}`;
}

function renderPagination(data) {
    const c = document.getElementById('ranking-pagination');
    const totalPages = Math.ceil(data.totalElements / data.size);
    if (totalPages <= 1) { c.innerHTML = ''; return; }
    c.innerHTML = `
        <button class="btn btn-sm btn-secondary" id="rk-prev" ${data.page <= 1 ? 'disabled' : ''}>이전</button>
        <span class="page-info">${data.page} / ${totalPages}</span>
        <button class="btn btn-sm btn-secondary" id="rk-next" ${data.page >= totalPages ? 'disabled' : ''}>다음</button>`;
    document.getElementById('rk-prev').addEventListener('click', () => loadRanking(data.page - 1));
    document.getElementById('rk-next').addEventListener('click', () => loadRanking(data.page + 1));
}

function esc(s) { return (s || '').replace(/</g, '&lt;').replace(/>/g, '&gt;'); }

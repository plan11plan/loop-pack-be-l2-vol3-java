import { DataGenApi } from '../api.js';

export async function initDashboard() {
    const panel = document.getElementById('tab-dashboard');
    panel.innerHTML = `
        <h1 style="font-size:22px;font-weight:700;margin-bottom:20px">대시보드</h1>
        <div class="stats-grid" id="dash-stats">
            <div class="stat-card"><div class="label">로딩 중...</div><div class="value">-</div></div>
        </div>
        <div class="panel">
            <div class="panel-header"><h2>바로가기</h2></div>
            <div style="display:flex;gap:10px;flex-wrap:wrap">
                <button class="btn btn-primary" onclick="document.querySelector('[data-tab=brands]').click()">브랜드 관리</button>
                <button class="btn btn-primary" onclick="document.querySelector('[data-tab=products]').click()">상품 관리</button>
                <button class="btn btn-success" onclick="document.querySelector('[data-tab=\\'data-generator\\']').click()">데이터 생성기</button>
            </div>
        </div>`;

    try {
        const stats = await DataGenApi.stats();
        document.getElementById('dash-stats').innerHTML = `
            <div class="stat-card"><div class="label">유저</div><div class="value">${stats.userCount.toLocaleString()}</div></div>
            <div class="stat-card"><div class="label">브랜드</div><div class="value">${stats.brandCount.toLocaleString()}</div></div>
            <div class="stat-card"><div class="label">상품</div><div class="value">${stats.productCount.toLocaleString()}</div></div>
            <div class="stat-card"><div class="label">좋아요</div><div class="value">${stats.likeCount.toLocaleString()}</div></div>
            <div class="stat-card"><div class="label">주문</div><div class="value">${stats.orderCount.toLocaleString()}</div></div>
            <div class="stat-card"><div class="label">쿠폰</div><div class="value">${stats.couponCount.toLocaleString()}</div></div>
            <div class="stat-card"><div class="label">발급 쿠폰</div><div class="value">${stats.ownedCouponCount.toLocaleString()}</div></div>`;
    } catch (e) {
        document.getElementById('dash-stats').innerHTML = `<div class="stat-card"><div class="label">에러</div><div class="value">${e.message}</div></div>`;
    }
}

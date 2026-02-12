// WishKart JavaScript Application

// API Base URL
const API_BASE = '/api';

// Get stored token
function getToken() {
    return localStorage.getItem('token');
}

// Check if user is authenticated
function isAuthenticated() {
    return !!getToken();
}

// Fetch with authentication
async function fetchWithAuth(url, options = {}) {
    const token = getToken();
    const headers = {
        'Content-Type': 'application/json',
        ...options.headers
    };

    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    return fetch(url, { ...options, headers });
}

// Initialize on page load
document.addEventListener('DOMContentLoaded', function() {
    updateCartBadge();
    initAddToCartButtons();
});

// Update cart badge
async function updateCartBadge(count) {
    const badge = document.getElementById('cartBadge');
    if (!badge) return;

    if (count !== undefined) {
        badge.textContent = count;
        badge.style.display = count > 0 ? 'block' : 'none';
        return;
    }

    if (!isAuthenticated()) {
        badge.style.display = 'none';
        return;
    }

    try {
        const response = await fetchWithAuth('/api/cart/count');
        const data = await response.json();
        if (data.success) {
            badge.textContent = data.data;
            badge.style.display = data.data > 0 ? 'block' : 'none';
        }
    } catch (error) {
        console.error('Failed to update cart badge:', error);
    }
}

// Initialize add to cart buttons
function initAddToCartButtons() {
    document.querySelectorAll('.add-to-cart-btn').forEach(btn => {
        btn.addEventListener('click', function(e) {
            e.preventDefault();
            const productId = this.dataset.productId;
            addToCart(productId, 1);
        });
    });
}

// Add to cart
async function addToCart(productId, quantity = 1) {
    if (!isAuthenticated()) {
        window.location.href = '/auth/login';
        return;
    }

    try {
        const response = await fetchWithAuth('/api/cart/add', {
            method: 'POST',
            body: JSON.stringify({ productId: parseInt(productId), quantity })
        });

        const data = await response.json();

        if (data.success) {
            showToast('Added to cart!', 'success');
            updateCartBadge(data.data.totalItems);

            // Animate cart icon
            const cartLink = document.getElementById('cartLink');
            if (cartLink) {
                cartLink.classList.add('cart-bounce');
                setTimeout(() => cartLink.classList.remove('cart-bounce'), 300);
            }
        } else {
            showToast(data.message || 'Failed to add to cart', 'error');
        }
    } catch (error) {
        showToast('Failed to add to cart', 'error');
    }
}

// Show toast notification
function showToast(message, type = 'success') {
    // Create toast container if it doesn't exist
    let container = document.querySelector('.toast-container');
    if (!container) {
        container = document.createElement('div');
        container.className = 'toast-container';
        document.body.appendChild(container);
    }

    const toastId = 'toast-' + Date.now();
    const bgClass = type === 'error' ? 'bg-danger' : type === 'warning' ? 'bg-warning' : 'bg-success';

    const toastHtml = `
        <div id="${toastId}" class="toast align-items-center text-white ${bgClass} border-0" role="alert">
            <div class="d-flex">
                <div class="toast-body">
                    <i class="bi ${type === 'error' ? 'bi-x-circle' : 'bi-check-circle'} me-2"></i>
                    ${message}
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
            </div>
        </div>
    `;

    container.insertAdjacentHTML('beforeend', toastHtml);

    const toastElement = document.getElementById(toastId);
    const toast = new bootstrap.Toast(toastElement, { autohide: true, delay: 3000 });
    toast.show();

    // Remove from DOM after hiding
    toastElement.addEventListener('hidden.bs.toast', () => {
        toastElement.remove();
    });
}

// Logout
function logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');

    fetch('/api/auth/logout', { method: 'POST' })
        .finally(() => {
            window.location.href = '/';
        });
}

// Add to wishlist
async function addToWishlist(productId) {
    if (!isAuthenticated()) {
        window.location.href = '/auth/login';
        return;
    }

    try {
        const response = await fetchWithAuth(`/api/wishlist/add/${productId}`, {
            method: 'POST'
        });

        const data = await response.json();

        if (data.success) {
            showToast('Added to wishlist!');
        } else {
            showToast(data.message || 'Failed to add to wishlist', 'error');
        }
    } catch (error) {
        showToast('Failed to add to wishlist', 'error');
    }
}

// Share product
function shareProduct() {
    if (navigator.share) {
        navigator.share({
            title: document.title,
            url: window.location.href
        });
    } else {
        // Fallback: copy to clipboard
        navigator.clipboard.writeText(window.location.href);
        showToast('Link copied to clipboard!');
    }
}

// Sort products
function sortProducts(sortBy) {
    const url = new URL(window.location.href);
    url.searchParams.set('sort', sortBy);
    window.location.href = url.toString();
}

// Apply coupon
async function applyCoupon() {
    const code = document.getElementById('couponCode').value.trim();
    if (!code) {
        showToast('Please enter a coupon code', 'warning');
        return;
    }

    try {
        const response = await fetchWithAuth(`/api/cart/apply-coupon?code=${code}`, {
            method: 'POST'
        });

        const data = await response.json();

        if (data.success) {
            showToast('Coupon applied!');
            location.reload();
        } else {
            showToast(data.message || 'Invalid coupon code', 'error');
        }
    } catch (error) {
        showToast('Failed to apply coupon', 'error');
    }
}

// Format currency (INR)
function formatCurrency(amount) {
    return new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        maximumFractionDigits: 0
    }).format(amount);
}

// Format price helper
function formatPrice(amount) {
    return '₹' + new Intl.NumberFormat('en-IN').format(Math.round(amount));
}

// Format date
function formatDate(dateString) {
    return new Date(dateString).toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
    });
}

// Debounce function
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

// Search with debounce
const searchProducts = debounce(async (query) => {
    if (query.length < 2) return;

    try {
        const response = await fetch(`/api/products/search?q=${encodeURIComponent(query)}`);
        const data = await response.json();

        // Handle search results (e.g., show in dropdown)
        console.log('Search results:', data);
    } catch (error) {
        console.error('Search failed:', error);
    }
}, 300);

// Load categories
async function loadCategories() {
    try {
        const response = await fetch('/api/categories/root');
        const data = await response.json();

        if (data.success) {
            return data.data;
        }
    } catch (error) {
        console.error('Failed to load categories:', error);
    }
    return [];
}

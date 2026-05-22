/**
 * Основное приложение облачного хранилища
 * Чистый JavaScript без фреймворков
 */

// Состояние приложения
const state = {
    user: null,
    currentFolderId: null,
    files: [],
    currentFolders: [],
    folders: [],
    isRegisterMode: false,
    contextMenuItem: null
};

// API утилиты
const api = {
    async request(url, options = {}) {
        const response = await fetch(url, {
            ...options,
            headers: {
                'Content-Type': 'application/json',
                ...options.headers
            }
        });

        if (response.status === 401) {
            window.location.href = '/login';
            throw new Error('401');
        }

        const contentType = response.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
            const data = await response.json();
            if (!response.ok) {
                throw new Error(data.error || 'Ошибка запроса');
            }
            return data;
        }

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        return response;
    },

    get: (url) => api.request(url, { method: 'GET' }),
    post: (url, data) => api.request(url, { method: 'POST', body: JSON.stringify(data) }),
    put: (url, data) => api.request(url, { method: 'PUT', body: JSON.stringify(data) }),
    delete: (url) => api.request(url, { method: 'DELETE' })
};

// Инициализация приложения
document.addEventListener('DOMContentLoaded', () => {
    initAuth();
    initEventListeners();
    initDragAndDrop();
});

// ==================== Аутентификация ====================

function initAuth() {
    // Проверка существующей сессии
    checkSession();
}

// ==================== Навигация ====================

function updateBackButtonVisibility() {
    const backBtn = document.getElementById('backButton');
    if (backBtn) {
        backBtn.style.display = state.currentFolderId === null ? 'none' : 'inline-block';
    }
}

async function goBack() {
    if (state.currentFolderId === null) {
        showMessage('Вы уже в корневой папке', 'info');
        return;
    }

    try {
        const folder = await api.get(`/api/folders/${state.currentFolderId}`);
        state.currentFolderId = folder.parentId || folder.parent_id || null;
        await loadFiles();
        updateBackButtonVisibility();
    } catch (err) {
        console.error('Ошибка навигации назад:', err);
        showMessage('Ошибка навигации. Возврат в корень.', 'error');
        state.currentFolderId = null;
        await loadFiles();
        updateBackButtonVisibility();
    }
}

function showMessage(text, type = 'error', elementId = 'errorMessage') {
    const el = document.getElementById(elementId);
    if (!el) return;

    el.textContent = text;
    if (elementId === 'errorMessage') {
        el.className = `error-message ${type === 'info' ? 'info' : 'error'}`;
    }
    el.style.display = 'block';

    setTimeout(() => {
        el.style.display = 'none';
    }, 3000);
}

async function checkSession() {
    try {
        const response = await fetch('/api/auth/check', { method: 'GET' });
        if (response.ok) {
            state.user = await response.json();
            showApp();
        } else {
            showAuthModal();
        }
    } catch {
        showAuthModal();
    }
}

function showAuthModal() {
    document.getElementById('authModal').style.display = 'flex';
    document.getElementById('app').style.display = 'none';
}

function showApp() {
    document.getElementById('authModal').style.display = 'none';
    document.getElementById('app').style.display = 'block';
    document.getElementById('userInfo').textContent = state.user.login;
    updateQuotaInfo();
    loadFiles();
    loadFolderTree();
}

async function handleAuthSubmit(e) {
    e.preventDefault();
    const login = document.getElementById('authLogin').value;
    const email = document.getElementById('authEmail').value;
    const password = document.getElementById('authPassword').value;
    const errorEl = document.getElementById('authError');
    
    try {
        if (state.isRegisterMode) {
            state.user = await api.post('/api/auth/register', { login, email, password });
        } else {
            state.user = await api.post('/api/auth/login', { login, password });
        }
        showApp();
    } catch (err) {
        errorEl.textContent = err.message;
    }
}

async function handleLogout() {
    try {
        await api.post('/api/auth/logout', {});
        state.user = null;
        showAuthModal();
        document.getElementById('authForm').reset();
        document.getElementById('authError').textContent = '';
    } catch (err) {
        console.error('Ошибка выхода:', err);
    }
}

function toggleAuthMode() {
    state.isRegisterMode = !state.isRegisterMode;
    document.getElementById('authTitle').textContent = state.isRegisterMode ? 'Регистрация' : 'Вход в систему';
    document.getElementById('authEmail').style.display = state.isRegisterMode ? 'block' : 'none';
    document.getElementById('authSubmitBtn').textContent = state.isRegisterMode ? 'Зарегистрироваться' : 'Войти';
    document.getElementById('authToggle').innerHTML = state.isRegisterMode
        ? 'Уже есть аккаунт? <a href="#" id="switchToLogin">Войти</a>'
        : 'Нет аккаунта? <a href="#" id="switchToRegister">Зарегистрироваться</a>';
    document.getElementById('authError').textContent = '';
}

// ==================== Файлы и папки ====================

async function loadFiles() {
    try {
        // Загружаем файлы
        const url = state.currentFolderId
            ? `/api/files?folderId=${state.currentFolderId}`
            : '/api/files';
        state.files = await api.get(url);

        // Загружаем папки текущей директории
        const foldersUrl = state.currentFolderId
            ? `/api/folders?parentId=${state.currentFolderId}`
            : '/api/folders';
        state.currentFolders = await api.get(foldersUrl);

        renderFiles();
        updateBackButtonVisibility();
    } catch (err) {
        console.error('Ошибка загрузки файлов:', err);
        showMessage('Ошибка загрузки файлов', 'error');
    }
}

async function loadFolderTree() {
    try {
        state.folders = await api.get('/api/folders/tree');
        updateBreadcrumb();
    } catch (err) {
        console.error('Ошибка загрузки дерева папок:', err);
    }
}

function renderFiles() {
    const container = document.getElementById('filesList');
    container.innerHTML = '';
    
    // Папки (из отдельного запроса)
    state.currentFolders.forEach(folder => {
        const el = createFolderElement(folder);
        container.appendChild(el);
    });
    
    // Файлы
    state.files.forEach(item => {
        const el = createFileElement(item);
        container.appendChild(el);
    });
}

function createFolderElement(folder) {
    const div = document.createElement('div');
    div.className = 'file-item folder';
    div.innerHTML = `
        <button class="file-menu-btn" data-id="${folder.id}" data-type="folder">⋮</button>
        <div class="file-icon">📁</div>
        <div class="file-name">${escapeHtml(folder.name)}</div>
        <div class="file-size"></div>
    `;

    div.addEventListener('click', (e) => {
        if (e.target.classList.contains('file-menu-btn')) {
            showContextMenu(e, folder, 'folder');
        } else {
            state.currentFolderId = folder.id;
            loadFiles();
            updateBreadcrumb();
        }
    });

    return div;
}

function createFileElement(item) {
    const div = document.createElement('div');
    div.className = 'file-item';
    div.innerHTML = `
        <button class="file-menu-btn" data-id="${item.id}" data-type="file">⋮</button>
        <div class="file-icon">${getFileIcon(item.mimeType)}</div>
        <div class="file-name">${escapeHtml(item.originalName || item.name)}</div>
        <div class="file-size">${formatSize(item.size)}</div>
    `;
    
    div.addEventListener('click', (e) => {
        if (e.target.classList.contains('file-menu-btn')) {
            showContextMenu(e, item, 'file');
        } else {
            downloadFile(item.id);
        }
    });
    
    return div;
}

function getFileIcon(mimeType) {
    if (!mimeType) return '📄';
    if (mimeType.startsWith('image/')) return '🖼️';
    if (mimeType.startsWith('video/')) return '🎬';
    if (mimeType.startsWith('audio/')) return '🎵';
    if (mimeType.includes('pdf')) return '📕';
    if (mimeType.includes('word') || mimeType.includes('document')) return '📝';
    if (mimeType.includes('excel') || mimeType.includes('spreadsheet')) return '📊';
    if (mimeType.includes('powerpoint') || mimeType.includes('presentation')) return '📽️';
    return '📄';
}

function formatSize(bytes) {
    if (!bytes) return '';
    const units = ['Б', 'КБ', 'МБ', 'ГБ'];
    let i = 0;
    while (bytes >= 1024 && i < units.length - 1) {
        bytes /= 1024;
        i++;
    }
    return bytes.toFixed(1) + ' ' + units[i];
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// ==================== Загрузка файлов ====================

function initDragAndDrop() {
    const dropZone = document.getElementById('dropZone');
    
    ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
        dropZone.addEventListener(eventName, (e) => {
            e.preventDefault();
            e.stopPropagation();
        });
    });
    
    ['dragenter', 'dragover'].forEach(eventName => {
        dropZone.addEventListener(eventName, () => {
            dropZone.classList.add('dragover');
        });
    });
    
    ['dragleave', 'drop'].forEach(eventName => {
        dropZone.addEventListener(eventName, () => {
            dropZone.classList.remove('dragover');
        });
    });
    
    dropZone.addEventListener('drop', (e) => {
        const files = e.dataTransfer.files;
        uploadFiles(files);
    });
    
    document.getElementById('uploadBtn').addEventListener('click', () => {
        document.getElementById('fileInput').click();
    });
    
    document.getElementById('fileInput').addEventListener('change', (e) => {
        uploadFiles(e.target.files);
        e.target.value = '';
    });
}

async function uploadFiles(files) {
    for (const file of files) {
        try {
            const formData = new FormData();
            formData.append('file', file);
            if (state.currentFolderId) {
                formData.append('parentFolderId', state.currentFolderId);
            }
            
            const response = await fetch('/api/files/upload', {
                method: 'POST',
                body: formData
            });
            
            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.error);
            }
            
            await loadFiles();
            updateQuotaInfo();
        } catch (err) {
            showMessage(`Ошибка загрузки ${file.name}: ${err.message}`);
        }
    }
}

async function downloadFile(fileId) {
    window.open(`/api/files/download?id=${fileId}`, '_blank');
}

async function deleteFile(itemId, itemType) {
    if (!confirm('Вы уверены, что хотите удалить этот элемент?')) return;
    
    try {
        const url = itemType === 'folder' 
            ? `/api/folders?id=${itemId}`
            : `/api/files?id=${itemId}`;
        await api.delete(url);
        await loadFiles();
        await loadFolderTree();
        updateQuotaInfo();
    } catch (err) {
        showMessage(`Ошибка удаления: ${err.message}`);
    }
}

// ==================== Создание папки ====================

function showFolderModal() {
    document.getElementById('folderModal').style.display = 'flex';
    document.getElementById('folderName').focus();
}

function hideFolderModal() {
    document.getElementById('folderModal').style.display = 'none';
    document.getElementById('folderName').value = '';
    document.getElementById('folderError').textContent = '';
}

async function createFolder() {
    const name = document.getElementById('folderName').value.trim();
    const errorEl = document.getElementById('folderError');
    
    if (!name) {
        errorEl.textContent = 'Введите название папки';
        return;
    }
    
    try {
        await api.post('/api/folders', {
            name: name,
            parentId: state.currentFolderId
        });
        hideFolderModal();
        await loadFiles();
        await loadFolderTree();
    } catch (err) {
        errorEl.textContent = err.message;
    }
}

// ==================== Контекстное меню ====================

function showContextMenu(e, item, type) {
    e.stopPropagation();
    state.contextMenuItem = { item, type };
    
    const menu = document.getElementById('contextMenu');
    menu.style.display = 'block';
    menu.style.left = e.pageX + 'px';
    menu.style.top = e.pageY + 'px';
    
    // Показываем/скрываем кнопку скачивания для папок
    document.getElementById('ctxDownload').style.display = type === 'file' ? 'block' : 'none';
}

function hideContextMenu() {
    document.getElementById('contextMenu').style.display = 'none';
    state.contextMenuItem = null;
}

function handleContextAction(action) {
    const { item, type } = state.contextMenuItem;
    if (!item) return;
    
    switch (action) {
        case 'download':
            downloadFile(item.id);
            break;
        case 'share':
            showShareModal(item.id, type);
            break;
        case 'delete':
            deleteFile(item.id, type);
            break;
    }
    hideContextMenu();
}

// ==================== Шаринг ====================

function showShareModal(itemId, itemType) {
    document.getElementById('shareModal').style.display = 'flex';
    document.getElementById('shareModal').dataset.itemId = itemId;
    document.getElementById('shareModal').dataset.itemType = itemType;
    document.getElementById('shareError').textContent = '';
    document.getElementById('publicLinkResult').innerHTML = '';
}

function hideShareModal() {
    document.getElementById('shareModal').style.display = 'none';
}

function switchShareTab(tab) {
    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    document.getElementById(tab === 'public' ? 'tabPublic' : 'tabUser').classList.add('active');
    
    document.getElementById('publicShareContent').style.display = tab === 'public' ? 'block' : 'none';
    document.getElementById('userShareContent').style.display = tab === 'user' ? 'block' : 'none';
}

async function createPublicShare() {
    const itemId = parseInt(document.getElementById('shareModal').dataset.itemId);
    const itemType = document.getElementById('shareModal').dataset.itemType;
    const errorEl = document.getElementById('shareError');
    const resultEl = document.getElementById('publicLinkResult');
    
    try {
        const data = await api.post('/api/shares/public', {
            itemId: itemId,
            itemType: itemType.toUpperCase(),
            expiresAt: null
        });
        
        const fullUrl = window.location.origin + data.shareUrl;
        resultEl.innerHTML = `
            <p><strong>Ссылка создана:</strong></p>
            <a href="${data.shareUrl}" target="_blank">${fullUrl}</a>
            <p style="margin-top: 0.5rem; font-size: 0.8rem; color: #7f8c8d;">
                Кликните для открытия или скопируйте ссылку
            </p>
        `;
        errorEl.textContent = '';
    } catch (err) {
        errorEl.textContent = err.message;
    }
}

async function createUserShare() {
    const itemId = parseInt(document.getElementById('shareModal').dataset.itemId);
    const itemType = document.getElementById('shareModal').dataset.itemType;
    const targetUserId = parseInt(document.getElementById('targetUserId').value);
    const permission = document.getElementById('sharePermission').value;
    const errorEl = document.getElementById('shareError');
    
    if (!targetUserId) {
        errorEl.textContent = 'Введите ID пользователя';
        return;
    }
    
    try {
        await api.post('/api/shares/user', {
            itemId: itemId,
            itemType: itemType.toUpperCase(),
            targetUserId: targetUserId,
            permission: permission
        });
        
        errorEl.textContent = 'Доступ предоставлен!';
        setTimeout(() => errorEl.textContent = '', 3000);
    } catch (err) {
        errorEl.textContent = err.message;
    }
}

// ==================== Поиск ====================

async function handleSearch() {
    const query = document.getElementById('searchInput').value.trim();
    const searchErrorEl = document.getElementById('searchError');

    // Скрываем предыдущие ошибки
    if (searchErrorEl) searchErrorEl.style.display = 'none';

    // Пустой запрос — показываем содержимое текущей папки
    if (!query) {
        loadFiles();
        return;
    }
    
    try {
        const results = await api.get(`/api/search?query=${encodeURIComponent(query)}`);
        renderSearchResults(results);
        // При поиске скрываем кнопку «Назад», так как мы не в папке
        const backBtn = document.getElementById('backButton');
        if (backBtn) backBtn.style.display = 'none';
    } catch (err) {
        console.error('Ошибка поиска:', err);
        showMessage('Не удалось выполнить поиск. Попробуйте позже.', 'error', 'searchError');
    }
}

function renderSearchResults(results) {
    const container = document.getElementById('filesList');
    container.innerHTML = '<h3>Результаты поиска</h3>';
    
    if (results.length === 0) {
        container.innerHTML += '<p>Ничего не найдено</p>';
        return;
    }
    
    results.forEach(item => {
        const el = createFileElement(item);
        container.appendChild(el);
    });
}

// ==================== Фотографии ====================

async function loadPhotos() {
    try {
        const photos = await api.get('/api/photos');
        renderPhotos(photos);
    } catch (err) {
        console.error('Ошибка загрузки фотографий:', err);
    }
}

function renderPhotos(photos) {
    const container = document.getElementById('photosGallery');
    container.innerHTML = '';
    
    photos.forEach(photo => {
        const div = document.createElement('div');
        div.className = 'photo-item';
        div.innerHTML = `
            <img src="${photo.thumbnailUrl}" alt="${escapeHtml(photo.originalName)}" loading="lazy">
            <div class="photo-info">
                <div class="photo-name">${escapeHtml(photo.originalName)}</div>
                <div class="photo-meta">
                    ${photo.dateTaken ? new Date(photo.dateTaken).toLocaleDateString() : ''}
                    ${photo.cameraModel ? ' • ' + escapeHtml(photo.cameraModel) : ''}
                </div>
            </div>
        `;
        container.appendChild(div);
    });
}

// ==================== Навигация ====================

function switchView(view) {
    document.querySelectorAll('.nav-btn').forEach(btn => btn.classList.remove('active'));
    document.getElementById('nav' + view.charAt(0).toUpperCase() + view.slice(1)).classList.add('active');
    
    document.getElementById('filesList').style.display = view === 'files' ? 'grid' : 'none';
    document.getElementById('photosGallery').style.display = view === 'photos' ? 'grid' : 'none';
    document.getElementById('sharedList').style.display = view === 'shared' ? 'block' : 'none';
    
    if (view === 'files') {
        loadFiles();
    } else if (view === 'photos') {
        loadPhotos();
    } else if (view === 'shared') {
        loadSharedItems();
    }
}

async function loadSharedItems() {
    // Загрузка элементов, которыми поделились с пользователем
    const container = document.getElementById('sharedList');
    container.innerHTML = '<h3>Доступно мне</h3>';
    // TODO: реализовать загрузку шарингов
}

function updateBreadcrumb() {
    const container = document.getElementById('breadcrumb');
    if (!state.currentFolderId) {
        container.textContent = 'Все файлы';
        return;
    }
    
    // TODO: построить хлебные крошки на основе дерева папок
    container.textContent = 'Папка #' + state.currentFolderId;
}

async function updateQuotaInfo() {
    // Обновление информации о квоте (нужен эндпоинт для получения информации о пользователе)
    // Пока используем данные из сессии
    if (state.user) {
        const percent = (state.user.usedSpace / state.user.quota) * 100;
        document.getElementById('quotaUsed').style.width = Math.min(percent, 100) + '%';
        document.getElementById('quotaText').textContent = 
            `${formatSize(state.user.usedSpace)} из ${formatSize(state.user.quota)}`;
    }
}

// ==================== Обработчики событий ====================

function initEventListeners() {
    // Аутентификация
    document.getElementById('authForm').addEventListener('submit', handleAuthSubmit);
    document.getElementById('authToggle').addEventListener('click', (e) => {
        if (e.target.tagName === 'A') {
            e.preventDefault();
            toggleAuthMode();
        }
    });
    document.getElementById('logoutBtn').addEventListener('click', handleLogout);
    
    // Создание папки
    document.getElementById('createFolderBtn').addEventListener('click', showFolderModal);
    document.getElementById('createFolderConfirm').addEventListener('click', createFolder);
    document.getElementById('createFolderCancel').addEventListener('click', hideFolderModal);
    
    // Контекстное меню
    document.addEventListener('click', (e) => {
        if (!e.target.closest('#contextMenu')) {
            hideContextMenu();
        }
    });
    
    document.getElementById('ctxDownload').addEventListener('click', () => handleContextAction('download'));
    document.getElementById('ctxShare').addEventListener('click', () => handleContextAction('share'));
    document.getElementById('ctxDelete').addEventListener('click', () => handleContextAction('delete'));
    
    // Шаринг
    document.getElementById('tabPublic').addEventListener('click', () => switchShareTab('public'));
    document.getElementById('tabUser').addEventListener('click', () => switchShareTab('user'));
    document.getElementById('createPublicLink').addEventListener('click', createPublicShare);
    document.getElementById('createUserShare').addEventListener('click', createUserShare);
    document.getElementById('shareClose').addEventListener('click', hideShareModal);
    
    // Поиск
    document.getElementById('searchBtn').addEventListener('click', handleSearch);
    document.getElementById('searchInput').addEventListener('keypress', (e) => {
        if (e.key === 'Enter') handleSearch();
    });
    
    // Навигация назад
    document.getElementById('backButton').addEventListener('click', goBack);

    // Навигация
    document.getElementById('navFiles').addEventListener('click', () => switchView('files'));
    document.getElementById('navPhotos').addEventListener('click', () => switchView('photos'));
    document.getElementById('navShared').addEventListener('click', () => switchView('shared'));
}

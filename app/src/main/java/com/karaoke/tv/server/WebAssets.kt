package com.karaoke.tv.server

object WebAssets {
    fun getMobileHtml(adminPin: String): String {
        return """
<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>Karaoke TV — Пульт управления</title>
    <style>
        :root {
            --bg-dark: #0f0f1a;
            --surface: #181829;
            --surface-card: #222238;
            --primary: #8b5cf6;
            --primary-glow: #a78bfa;
            --secondary: #06b6d4;
            --accent: #ec4899;
            --gold: #fbbf24;
            --text: #f8fafc;
            --text-muted: #94a3b8;
            --border: #33334d;
            --success: #10b981;
            --danger: #ef4444;
        }
        * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; -webkit-tap-highlight-color: transparent; }
        body { background: var(--bg-dark); color: var(--text); padding-bottom: 75px; min-height: 100vh; }
        
        /* Header */
        header { background: var(--surface); padding: 14px 16px; border-bottom: 1px solid var(--border); display: flex; align-items: center; justify-content: space-between; position: sticky; top: 0; z-index: 100; box-shadow: 0 4px 12px rgba(0,0,0,0.4); }
        .logo { font-size: 1.15rem; font-weight: 900; background: linear-gradient(135deg, var(--primary), var(--secondary)); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
        .status-dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; background: var(--success); margin-right: 6px; box-shadow: 0 0 6px var(--success); }
        .singer-badge { background: var(--surface-card); border: 1.5px solid var(--primary); padding: 5px 12px; border-radius: 14px; font-size: 0.85rem; font-weight: 600; cursor: pointer; display: flex; align-items: center; gap: 4px; }
        
        /* Now Playing Bar */
        .now-playing { background: linear-gradient(90deg, #2e1065, #083344); padding: 14px 16px; display: flex; align-items: center; justify-content: space-between; border-bottom: 2px solid var(--primary); }
        .now-title { font-weight: bold; font-size: 1rem; color: #fff; }
        .now-artist { font-size: 0.85rem; color: var(--secondary); margin-top: 2px; }
        .now-singer { font-size: 0.8rem; color: var(--gold); margin-top: 2px; font-weight: 600; }
        .playback-controls { display: flex; gap: 8px; }
        .btn-ctrl { background: rgba(255,255,255,0.15); border: 1px solid rgba(255,255,255,0.25); color: #fff; padding: 8px 12px; border-radius: 8px; font-size: 0.85rem; font-weight: bold; cursor: pointer; transition: 0.2s; }
        .btn-ctrl:active { transform: scale(0.95); background: var(--primary); }
        
        /* Tabs */
        .tab-content { display: none; padding: 16px; }
        .tab-content.active { display: block; }
        
        /* Bottom Navigation */
        nav { position: fixed; bottom: 0; left: 0; right: 0; background: var(--surface); border-top: 1px solid var(--border); display: flex; height: 60px; z-index: 100; box-shadow: 0 -4px 12px rgba(0,0,0,0.3); }
        .nav-item { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; font-size: 0.75rem; color: var(--text-muted); cursor: pointer; text-decoration: none; font-weight: 500; }
        .nav-item.active { color: var(--primary-glow); font-weight: 700; }
        .nav-item svg { width: 22px; height: 22px; margin-bottom: 3px; fill: currentColor; }
        
        /* Search Box */
        .search-container { display: flex; gap: 8px; margin-bottom: 12px; }
        .search-input { flex: 1; background: var(--surface); border: 1.5px solid var(--border); color: var(--text); padding: 12px 14px; border-radius: 12px; font-size: 0.95rem; outline: none; }
        .search-input:focus { border-color: var(--primary); }
        .search-btn { background: var(--primary); color: white; border: none; padding: 0 18px; border-radius: 12px; font-weight: bold; cursor: pointer; font-size: 0.95rem; }
        .search-btn:active { opacity: 0.85; }
        
        /* Cards */
        .card { background: var(--surface-card); border-radius: 14px; padding: 12px; margin-bottom: 10px; border: 1px solid var(--border); transition: 0.2s; }
        .card:hover { border-color: var(--primary); }
        .song-row { display: flex; gap: 12px; align-items: center; }
        .song-thumb { width: 72px; height: 48px; border-radius: 8px; object-fit: cover; background: #000; flex-shrink: 0; }
        .song-info { flex: 1; min-width: 0; }
        .song-title { font-weight: bold; font-size: 0.95rem; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
        .song-artist { font-size: 0.8rem; color: var(--secondary); margin-top: 2px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
        .queue-singer { color: var(--gold); font-size: 0.8rem; margin-top: 4px; font-weight: 600; }
        
        /* Buttons */
        .btn-add { background: linear-gradient(135deg, var(--primary), var(--secondary)); color: white; border: none; padding: 8px 14px; border-radius: 10px; font-size: 0.85rem; font-weight: bold; cursor: pointer; flex-shrink: 0; }
        .btn-add:active { transform: scale(0.96); opacity: 0.9; }
        .btn-action { background: var(--surface); border: 1px solid var(--border); color: var(--text); width: 32px; height: 32px; border-radius: 8px; display: flex; align-items: center; justify-content: center; cursor: pointer; font-weight: bold; font-size: 0.85rem; }
        .btn-action:active { background: var(--primary); }
        .btn-del { border-color: var(--danger); color: var(--danger); }
        .btn-del:active { background: var(--danger); color: white; }
        
        /* Remote Panel */
        .remote-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-top: 16px; }
        .big-btn { background: var(--surface-card); border: 1.5px solid var(--border); color: #fff; padding: 18px 14px; border-radius: 14px; font-size: 1rem; font-weight: bold; text-align: center; cursor: pointer; transition: 0.2s; }
        .big-btn:active { background: var(--primary); border-color: var(--primary); transform: scale(0.98); }
        .big-btn-primary { background: linear-gradient(135deg, #7c3aed, #4f46e5); border: none; grid-column: span 2; padding: 20px; font-size: 1.15rem; }
    </style>
</head>
<body>

    <header>
        <div class="logo">
            <span class="status-dot"></span>🎤 Karaoke TV
        </div>
        <div class="singer-badge" onclick="changeSingerName()" id="singerBadge">👤 Гость</div>
    </header>

    <div class="now-playing" id="nowPlayingBar">
        <div style="flex: 1; min-width: 0; padding-right: 10px;">
            <div class="now-title" id="nowTitle">Сейчас никто не поёт</div>
            <div class="now-artist" id="nowArtist">Добавьте песню из каталога Google Drive!</div>
            <div class="now-singer" id="nowSinger"></div>
        </div>
        <div class="playback-controls">
            <button class="btn-ctrl" onclick="controlPlayback('toggle')">⏯</button>
            <button class="btn-ctrl" onclick="controlPlayback('skip')">⏭</button>
        </div>
    </div>

    <!-- TAB 1: ОЧЕРЕДЬ -->
    <div id="tabQueue" class="tab-content active">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
            <h3 style="font-size: 1.1rem;">Очередь песен</h3>
            <span id="queueCount" style="color: var(--secondary); font-size: 0.85rem; font-weight: bold;">0 треков</span>
        </div>
        <div id="queueList">Загрузка очереди...</div>
    </div>

    <!-- TAB 2: ПОИСК GOOGLE DRIVE -->
    <div id="tabSearch" class="tab-content">
        <div class="search-container">
            <input type="text" id="searchInput" class="search-input" placeholder="Название песни или артист..." onkeydown="if(event.key==='Enter') searchSongs()">
            <button class="search-btn" onclick="searchSongs()">Найти</button>
        </div>
        <div style="margin-bottom: 14px; font-size: 0.85rem; color: var(--secondary);">
            ⭐ Источник: <b>Google Диск (караоке MP4)</b>
        </div>
        <div id="searchResults">Введите название и нажмите «Найти»</div>
    </div>

    <!-- TAB 3: ИЗБРАННОЕ И ИСТОРИЯ -->
    <div id="tabFavs" class="tab-content">
        <h3 style="margin-bottom: 12px; font-size: 1.1rem;">Быстрый выбор</h3>
        <div style="display: flex; gap: 8px; margin-bottom: 14px;">
            <button class="btn-ctrl" style="flex: 1; padding: 10px;" onclick="loadQuickTab('popular')">🔥 Популярные</button>
            <button class="btn-ctrl" style="flex: 1; padding: 10px;" onclick="loadQuickTab('history')">🕒 История</button>
        </div>
        <div id="quickList">Загрузка...</div>
    </div>

    <!-- TAB 4: ПУЛЬТ ДИСТАНЦИОННОГО УПРАВЛЕНИЯ -->
    <div id="tabRemote" class="tab-content">
        <h3 style="margin-bottom: 14px; font-size: 1.1rem;">ТВ-Пульт управления</h3>
        <div class="remote-grid">
            <button class="big-btn big-btn-primary" onclick="controlPlayback('toggle')">⏯ ПАУЗА / ПЛЕЙ</button>
            <button class="big-btn" onclick="controlPlayback('skip')">⏭ СЛЕДУЮЩАЯ</button>
            <button class="big-btn" onclick="changeSingerName()">👤 СМЕНИТЬ ИМЯ</button>
            <button class="big-btn" style="grid-column: span 2; border-color: var(--danger); color: var(--danger);" onclick="clearAllQueue()">🗑 Очистить очередь</button>
        </div>
        <div style="margin-top: 24px; padding: 14px; background: var(--surface); border-radius: 12px; border: 1px solid var(--border); font-size: 0.85rem; color: var(--text-muted); text-align: center;">
            🟢 Подключено по локальной сети к Karaoke TV<br>
            Управление синхронизируется в реальном времени.
        </div>
    </div>

    <!-- BOTTOM NAV -->
    <nav>
        <div class="nav-item active" onclick="switchTab('tabQueue', this)">
            <svg viewBox="0 0 24 24"><path d="M4 6h16v2H4zm0 5h16v2H4zm0 5h16v2H4z"/></svg>
            Очередь
        </div>
        <div class="nav-item" onclick="switchTab('tabSearch', this)">
            <svg viewBox="0 0 24 24"><path d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/></svg>
            Поиск
        </div>
        <div class="nav-item" onclick="switchTab('tabFavs', this); loadQuickTab('popular');">
            <svg viewBox="0 0 24 24"><path d="M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z"/></svg>
            Каталог
        </div>
        <div class="nav-item" onclick="switchTab('tabRemote', this)">
            <svg viewBox="0 0 24 24"><path d="M17 1H7c-1.1 0-2 .9-2 2v18c0 1.1.9 2 2 2h10c1.1 0 2-.9 2-2V3c0-1.1-.9-2-2-2zm0 18H7V5h10v14z"/></svg>
            Пульт
        </div>
    </nav>

    <script>
        const urlParams = new URLSearchParams(window.location.search);
        const sessionToken = urlParams.get('token') || '';
        let mySingerName = localStorage.getItem('karaoke_singer_name') || 'Гость';
        document.getElementById('singerBadge').innerText = '👤 ' + mySingerName;

        function switchTab(tabId, el) {
            document.querySelectorAll('.tab-content').forEach(t => t.classList.remove('active'));
            document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
            document.getElementById(tabId).classList.add('active');
            if (el) el.classList.add('active');
        }

        function changeSingerName() {
            const name = prompt('Ваше имя для караоке:', mySingerName);
            if (name && name.trim()) {
                mySingerName = name.trim();
                localStorage.setItem('karaoke_singer_name', mySingerName);
                document.getElementById('singerBadge').innerText = '👤 ' + mySingerName;
                sendRegisterName();
            }
        }

        async function sendRegisterName() {
            try {
                await fetch('/api/register?token=' + sessionToken, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ name: mySingerName })
                });
            } catch(e) {}
        }
        sendRegisterName();

        // WebSockets Live Synchronization
        let ws;
        function connectWebSocket() {
            const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
            ws = new WebSocket(proto + '//' + window.location.host + '/?token=' + sessionToken);
            ws.onmessage = function(event) {
                try {
                    const data = JSON.parse(event.data);
                    if (data.type === 'queue_updated' || data.type === 'init') {
                        renderQueue(data.queue);
                        if (data.playback) renderPlayback(data.playback);
                    } else if (data.type === 'playback_updated') {
                        renderPlayback(data.playback);
                    }
                } catch(e) {}
            };
            ws.onclose = function() {
                setTimeout(connectWebSocket, 2000);
            };
        }
        connectWebSocket();

        function renderQueue(queue) {
            const container = document.getElementById('queueList');
            const countLabel = document.getElementById('queueCount');
            if (!queue || queue.length === 0) {
                container.innerHTML = '<div style="color: var(--text-muted); text-align: center; padding: 28px;">Очередь пуста.<br>Найдите песню на вкладке «Поиск»!</div>';
                if (countLabel) countLabel.innerText = '0 треков';
                return;
            }
            if (countLabel) countLabel.innerText = queue.length + ' трек(ов)';
            let html = '';
            queue.forEach((item, index) => {
                const isCurrent = (index === 0 && item.status === 'CURRENT');
                const badge = isCurrent ? '<span style="background: #10b981; color: white; padding: 2px 6px; border-radius: 4px; font-size: 0.7rem; font-weight: bold;">ПОЁТ</span>' : '#' + (index + 1);
                
                html += '<div class="card" style="' + (isCurrent ? 'border-color: #10b981; background: #1c2333;' : '') + '">';
                html += '<div class="song-row">';
                html += '<div style="font-weight: bold; width: 22px; color: var(--secondary); font-size: 0.85rem;">' + badge + '</div>';
                if (item.coverUrl) {
                    html += '<img src="' + item.coverUrl + '" class="song-thumb" onerror="this.src=\'\'">';
                }
                html += '<div class="song-info">';
                html += '<div class="song-title">' + item.title + '</div>';
                html += '<div class="song-artist">' + item.artist + '</div>';
                html += '<div class="queue-singer">🎤 ' + item.participant + '</div>';
                html += '</div>';
                html += '<div style="display: flex; gap: 4px;">';
                if (index > 0) {
                    html += '<button class="btn-action" onclick="moveQueue(' + item.id + ', \'up\')">▲</button>';
                }
                html += '<button class="btn-action btn-del" onclick="removeFromQueue(' + item.id + ')">✕</button>';
                html += '</div>';
                html += '</div></div>';
            });
            container.innerHTML = html;
        }

        function renderPlayback(playback) {
            if (playback && playback.title) {
                document.getElementById('nowTitle').innerText = playback.title;
                document.getElementById('nowArtist').innerText = playback.artist;
                document.getElementById('nowSinger').innerText = '🎤 Поёт: ' + (playback.participant || 'Гость');
            } else {
                document.getElementById('nowTitle').innerText = 'Сейчас никто не поёт';
                document.getElementById('nowArtist').innerText = 'Добавьте песню из каталога Google Drive!';
                document.getElementById('nowSinger').innerText = '';
            }
        }

        async function controlPlayback(action) {
            try {
                await fetch('/api/playback/action?token=' + sessionToken, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ action: action })
                });
            } catch(e) {
                console.error('Ошибка:', e);
            }
        }

        async function moveQueue(itemId, direction) {
            try {
                const action = direction === 'up' ? 'move_up' : 'move_down';
                await fetch('/api/queue/' + action + '?token=' + sessionToken, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ itemId: itemId })
                });
            } catch(e) {}
        }

        async function removeFromQueue(itemId) {
            if (!confirm('Удалить песню из очереди?')) return;
            try {
                await fetch('/api/queue/remove?token=' + sessionToken, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ itemId: itemId })
                });
            } catch(e) {}
        }

        async function clearAllQueue() {
            if (!confirm('Очистить всю караоке-очередь?')) return;
            try {
                await fetch('/api/queue/clear?token=' + sessionToken, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({})
                });
            } catch(e) {}
        }

        function showToast(msg) {
            let toast = document.getElementById('toast');
            if (!toast) {
                toast = document.createElement('div');
                toast.id = 'toast';
                toast.style.cssText = 'position:fixed;bottom:72px;left:50%;transform:translateX(-50%);background:rgba(15,23,42,0.96);color:#fff;padding:12px 22px;border-radius:24px;border:1.5px solid #38bdf8;font-size:0.9rem;font-weight:600;z-index:9999;box-shadow:0 8px 30px rgba(0,0,0,0.6);text-align:center;max-width:90%;transition:opacity 0.3s ease;pointer-events:none;';
                document.body.appendChild(toast);
            }
            toast.innerText = msg;
            toast.style.opacity = '1';
            clearTimeout(window._toastTimer);
            window._toastTimer = setTimeout(() => { toast.style.opacity = '0'; }, 3500);
        }

        async function searchSongs() {
            const query = document.getElementById('searchInput').value.trim();
            if (!query) return;

            const results = document.getElementById('searchResults');
            results.innerHTML = '<div style="color: var(--text-muted); padding: 16px;">Поиск караоке в Google Drive...</div>';
            
            try {
                const resp = await fetch('/api/search?q=' + encodeURIComponent(query) + '&token=' + sessionToken);
                const songs = await resp.json();
                if (!songs || songs.length === 0) {
                    results.innerHTML = '<div style="color: var(--text-muted); padding: 16px;">Ничего не найдено в каталоге.</div>';
                    return;
                }
                let html = '';
                songs.forEach(song => {
                    const bestVer = song.versions && song.versions[0];
                    const thumbUrl = song.coverUrl || (bestVer ? bestVer.thumbnailUrl : '');
                    const vId = bestVer ? bestVer.id : '';
                    html += '<div class="card">';
                    html += '<div class="song-row">';
                    if (thumbUrl) {
                        html += '<img src="' + thumbUrl + '" class="song-thumb">';
                    }
                    html += '<div class="song-info">';
                    html += '<div class="song-title">' + song.title + '</div>';
                    html += '<div class="song-artist">' + song.artist + '</div>';
                    if (bestVer) {
                        html += '<div style="font-size: 0.75rem; color: var(--secondary); margin-top: 2px;">Google Drive MP4 • ' + (bestVer.duration || 'Караоке') + '</div>';
                    }
                    html += '</div>';
                    html += '<button class="btn-add" onclick="addSongToQueue(\'' + song.id + '\', \'' + escapeHtml(song.title) + '\', \'' + escapeHtml(song.artist) + '\', \'' + vId + '\', \'' + thumbUrl + '\')">+ В очередь</button>';
                    html += '</div></div>';
                });
                results.innerHTML = html;
            } catch(e) {
                results.innerHTML = '<div style="color: var(--danger); padding: 16px;">Ошибка сети при поиске.</div>';
            }
        }

        async function addSongToQueue(songId, title, artist, versionId, coverUrl) {
            try {
                const res = await fetch('/api/queue/add?token=' + sessionToken, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        songId: songId,
                        songTitle: title,
                        songArtist: artist,
                        versionId: versionId,
                        coverUrl: coverUrl,
                        participantName: mySingerName
                    })
                });
                if (res.ok) {
                    showToast('✓ «' + title + '» добавлена в очередь!');
                    switchTab('tabQueue', document.querySelectorAll('.nav-item')[0]);
                }
            } catch(e) {
                showToast('Не удалось добавить песню в очередь');
            }
        }

        async function loadQuickTab(type) {
            const container = document.getElementById('quickList');
            container.innerHTML = '<div style="color: var(--text-muted); padding: 16px;">Загрузка...</div>';
            try {
                const url = type === 'history' ? '/api/history?token=' + sessionToken : '/api/popular?token=' + sessionToken;
                const resp = await fetch(url);
                const items = await resp.json();
                if (!items || items.length === 0) {
                    container.innerHTML = '<div style="color: var(--text-muted); padding: 16px;">Список пуст.</div>';
                    return;
                }
                let html = '';
                items.forEach(it => {
                    html += '<div class="card"><div class="song-row">';
                    if (it.coverUrl) html += '<img src="' + it.coverUrl + '" class="song-thumb">';
                    html += '<div class="song-info"><div class="song-title">' + it.title + '</div><div class="song-artist">' + it.artist + '</div></div>';
                    html += '<button class="btn-add" onclick="addSongToQueue(\'' + it.id + '\', \'' + escapeHtml(it.title) + '\', \'' + escapeHtml(it.artist) + '\', \'' + (it.versionId || '') + '\', \'' + (it.coverUrl || '') + '\')">+ Спеть</button>';
                    html += '</div></div>';
                });
                container.innerHTML = html;
            } catch(e) {
                container.innerHTML = '<div style="color: var(--danger); padding: 16px;">Ошибка загрузки.</div>';
            }
        }

        function escapeHtml(str) {
            if (!str) return '';
            return str.replace(/&/g, '&amp;').replace(/'/g, "\\'").replace(/"/g, '&quot;');
        }
    </script>
</body>
</html>
        """.trimIndent()
    }
}

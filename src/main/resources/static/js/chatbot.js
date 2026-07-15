(function () {
    var chatOpen = false;
    var conv = [];
    var sending = false;
    var panel, toggle, body, input, form, closeBtn;
    var processedMessages = new WeakSet();

    function markupToHTML(text) {
        text = text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
        text = text.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
        text = text.replace(/\n/g, '<br>');
        return text;
    }

    function addBubble(html, cls) {
        var bubble = document.createElement('div');
        bubble.className = 'chat-bubble ' + cls;
        bubble.innerHTML = html;
        body.appendChild(bubble);
        body.scrollTop = body.scrollHeight;
        return bubble;
    }

    function showTyping() {
        var el = document.createElement('div');
        el.className = 'chat-bubble chat-typing';
        el.innerHTML = '<span></span><span></span><span></span>';
        body.appendChild(el);
        body.scrollTop = body.scrollHeight;
        return el;
    }

    function removeTyping(el) {
        if (el && el.parentNode) el.parentNode.removeChild(el);
    }

    function sendMessage(msg) {
        if (sending || !msg.trim()) return;
        sending = true;
        addBubble(markupToHTML(msg.trim()), 'chat-user');
        var typing = showTyping();

        fetch('/api/chatbot', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ message: msg.trim() })
        })
            .then(function (r) { return r.json(); })
            .then(function (data) {
                removeTyping(typing);
                var html = markupToHTML(data.text || 'Sem resposta.');
                if (data.link) {
                    html += '<br><a class="chat-link" href="' + data.link + '">' + (data.linkLabel || 'Acessar') + ' →</a>';
                }
                addBubble(html, 'chat-bot');
                sending = false;
            })
            .catch(function () {
                removeTyping(typing);
                addBubble('Erro ao conectar com o servidor. Tente novamente.', 'chat-bot');
                sending = false;
            });
    }

    function initQuickReply(text, btn) {
        btn.addEventListener('click', function (e) {
            e.stopPropagation();
            if (!chatOpen) openChat();
            sendMessage(text);
        });
    }

    function openChat() {
        chatOpen = true;
        panel.classList.add('open');
        toggle.classList.add('hidden');
        setTimeout(function () { input.focus(); }, 200);
    }

    function closeChat() {
        chatOpen = false;
        panel.classList.remove('open');
        toggle.classList.remove('hidden');
    }

    function buildUI() {
        toggle = document.createElement('button');
        toggle.className = 'chat-toggle';
        toggle.setAttribute('aria-label', 'Abrir chat');
        toggle.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="5" width="18" height="14" rx="3"/><circle cx="9" cy="11" r="2" fill="currentColor" stroke="none"/><circle cx="15" cy="11" r="2" fill="currentColor" stroke="none"/><rect x="8" y="15" width="8" height="2" rx="1"/><line x1="12" y1="2" x2="12" y2="5"/><circle cx="12" cy="1.5" r="1.5"/></svg>';
        toggle.addEventListener('click', function (e) { e.stopPropagation(); openChat(); });

        panel = document.createElement('div');
        panel.className = 'chat-panel';

        var header = document.createElement('div');
        header.className = 'chat-header';
        header.innerHTML = '<div class="chat-header-left"><div class="chat-avatar"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="5" width="18" height="14" rx="3"/><circle cx="9" cy="11" r="2" fill="currentColor" stroke="none"/><circle cx="15" cy="11" r="2" fill="currentColor" stroke="none"/><rect x="8" y="15" width="8" height="2" rx="1"/><line x1="12" y1="2" x2="12" y2="5"/><circle cx="12" cy="1.5" r="1.5"/></svg></div><div><strong>Assistente BAT</strong><small>Online</small></div></div>';

        closeBtn = document.createElement('button');
        closeBtn.className = 'chat-close';
        closeBtn.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M18 6L6 18M6 6l12 12"></path></svg>';
        closeBtn.setAttribute('aria-label', 'Fechar chat');
        closeBtn.addEventListener('click', function (e) { e.stopPropagation(); closeChat(); });
        header.appendChild(closeBtn);

        body = document.createElement('div');
        body.className = 'chat-body';

        form = document.createElement('form');
        form.className = 'chat-input-wrap';
        form.addEventListener('submit', function (e) {
            e.preventDefault();
            var msg = input.value;
            input.value = '';
            sendMessage(msg);
        });

        input = document.createElement('input');
        input.type = 'text';
        input.className = 'chat-input';
        input.placeholder = 'Digite sua mensagem...';
        input.autocomplete = 'off';

        var sendBtn = document.createElement('button');
        sendBtn.type = 'submit';
        sendBtn.className = 'chat-send';
        sendBtn.setAttribute('aria-label', 'Enviar');
        sendBtn.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="22" y1="2" x2="11" y2="13"></line><polygon points="22 2 15 22 11 13 2 9 22 2"></polygon></svg>';

        form.appendChild(input);
        form.appendChild(sendBtn);

        panel.appendChild(header);
        panel.appendChild(body);
        panel.appendChild(form);

        document.body.appendChild(toggle);
        document.body.appendChild(panel);

        var welcome = markupToHTML('Olá! Sou o assistente virtual do **BAT Manutenção**. Pergunte sobre chamados, máquinas, MTBF, ranking ou digite **ajuda**.');
        addBubble(welcome, 'chat-bot');
    }

    function handleAllQuickReplies() {
        document.querySelectorAll('[data-chat-msg]').forEach(function (el) {
            if (!processedMessages.has(el)) {
                processedMessages.add(el);
                initQuickReply(el.getAttribute('data-chat-msg'), el);
            }
        });
    }

    document.addEventListener('click', function (e) {
        if (!chatOpen) return;
        if (panel && !panel.contains(e.target) && !(toggle && toggle.contains(e.target))) {
            closeChat();
        }
    });

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', function () { buildUI(); handleAllQuickReplies(); });
    } else {
        buildUI();
        handleAllQuickReplies();
    }

    window.addEventListener('load', function () { handleAllQuickReplies(); });
    var observer = new MutationObserver(function () { handleAllQuickReplies(); });
    observer.observe(document.documentElement, { childList: true, subtree: true });
})();

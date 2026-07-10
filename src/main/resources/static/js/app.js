function toggleNotif(event) {
    event.stopPropagation();
    var panel = document.getElementById('notifPanel');
    if (panel) {
        panel.classList.toggle('open');
    }
}

document.addEventListener('click', function (e) {
    var panel = document.getElementById('notifPanel');
    var btn = document.querySelector('.notif-btn');
    if (panel && panel.classList.contains('open')) {
        if (!panel.contains(e.target) && !(btn && btn.contains(e.target))) {
            panel.classList.remove('open');
        }
    }
});

function toggleSidebar() {
    var sb = document.querySelector('.sidebar');
    if (sb) sb.classList.toggle('open');
}

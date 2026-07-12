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

function toggleTopnav(event) {
    event.stopPropagation();
    var links = document.querySelector('.topnav-links');
    if (links) links.classList.toggle('open');
}

document.addEventListener('click', function (e) {
    var links = document.querySelector('.topnav-links');
    var toggle = document.querySelector('.topnav-toggle');
    if (links && links.classList.contains('open')) {
        if (!links.contains(e.target) && !(toggle && toggle.contains(e.target))) {
            links.classList.remove('open');
        }
    }
});

function renderPosts(resp_json, els) {
    let validPost = 0;
    for (let i = 0; i < resp_json.length; i++) {
        if (i === els.cards.length - 1) {
            document.getElementById("card-block").appendChild(els.cards[i].cloneNode(true));
        }
        const post_json = resp_json[i];
        els.cards[i].style.display = "block";
        els.texts[i].innerHTML = replaceMentionWithHTMLLinks(post_json["text"]);
        els.times[i].innerText = getTime(post_json["timestamp"]);
        els.creators[i].innerText = post_json["creator"]["username"];
        for (const media of post_json["media"]) {
            els.images[i].src = "http://" + globalThis.location.hostname +
                ":8081/get-media/?filename=" + media["media_id"] + "." + media["media_type"];
        }
        validPost += 1;
    }
    if (validPost === 0) {
        document.getElementById("card-block").appendChild(els.cards[0].cloneNode(true));
        els.cards[0].style.display = "block";
        els.texts[0].style.fontSize = "x-large";
        els.footer[0].style.display = "none";
    }
}

function handleUnauthorized() {
    console.log("unauthorized user login");
    globalThis.location.href = 'http://' + globalThis.location.hostname + ":8080/index.html";
    localStorage.clear();
}

function showTimeline(type) {
    const start = 0;
    const stop = 100;
    const url = 'http://' + globalThis.location.hostname + ':8080/api/' + type + '/read';
    const Http = new XMLHttpRequest();
    Http.open("GET", url + "?start=" + start + "&stop=" + stop, true);
    Http.onreadystatechange = function () {
        if (this.readyState === 2 && this.status === 401) {
            handleUnauthorized();
        } else if (this.readyState === 4 && this.status === 200) {
            const getFromUrl = new URL(location.href);
            const curUser = getFromUrl.searchParams.get("username");
            if (localStorage.getItem("username") == null) {
                localStorage.setItem("username", curUser);
                followUsername();
            }
            if (type === "user-timeline") {
                document.getElementById("mentioned_user").innerText = localStorage.getItem("username");
            }
            showUsername();
            renderPosts(JSON.parse(Http.responseText), collectPostElements());
        }
    };
    Http.send(null);
}

function collectPostElements() {
    return {
        cards:    document.getElementsByClassName("post-card"),
        texts:    document.getElementsByClassName("post-text"),
        times:    document.getElementsByClassName("post-time"),
        creators: document.getElementsByClassName("post-creator"),
        images:   document.getElementsByClassName("post-img"),
        footer:   document.getElementsByClassName("post-footer"),
    };
}

function renderMentionedPosts(resp_json, mentioned_user, els) {
    let validPost = 0;
    for (let i = 0; i < resp_json.length; i++) {
        if (i === els.cards.length - 1) {
            document.getElementById("card-block").appendChild(els.cards[i].cloneNode(true));
        }
        const post_json = resp_json[i];
        if (post_json["creator"]["username"].localeCompare(mentioned_user) !== 0) {
            continue;
        }
        els.cards[i].style.display = "block";
        els.texts[i].innerHTML = replaceMentionWithHTMLLinks(post_json["text"]);
        els.times[i].innerText = getTime(post_json["timestamp"]);
        els.creators[i].innerText = post_json["creator"]["username"];
        for (const media of post_json["media"]) {
            els.images[i].src = "http://" + globalThis.location.hostname +
                ":8081/get-media/?filename=" + media["media_id"] + "." + media["media_type"];
        }
        validPost += 1;
    }
    if (validPost === 0) {
        document.getElementById("card-block").appendChild(els.cards[0].cloneNode(true));
        els.cards[0].style.display = "block";
        els.texts[0].innerHTML = "The user has not post since you followed!";
        els.texts[0].style.fontSize = "x-large";
        els.footer[0].style.display = "none";
    }
}

function show_Mentioned_User_Timeline(mentioned_user) {
    const url = 'http://' + globalThis.location.hostname + ':8080/api/home-timeline/read';
    const Http = new XMLHttpRequest();
    Http.open("GET", url + "?start=0&stop=100", true);
    Http.onreadystatechange = function () {
        if (this.readyState === 2 && this.status === 401) {
            handleUnauthorized();
        } else if (this.readyState === 4 && this.status === 200) {
            document.getElementById("mentioned_user").innerText = mentioned_user;
            showUsername();
            renderMentionedPosts(JSON.parse(Http.responseText), mentioned_user, collectPostElements());
        }
    };
    Http.send(null);
}

function showUsername() {
    const username = localStorage.getItem("username");
    if (username != null) {
        document.getElementById("username").innerText = username;
    }
}

function getTime(time) {
    const date = new Date(Number(time));
    const hour = ("0" + date.getHours()).slice(-2);
    const min = ("0" + date.getMinutes()).slice(-2);
    return hour + ':' + min + ' ' + date.toDateString();
}

function replaceMentionWithHTMLLinks(text) {
    return text.replace(/(^|\s)@(\w+)/g, '$1<a href="profile.html?username=$2">@$2</a>');
}

function get_follower() {
    const Http = new XMLHttpRequest();
    const url = 'http://' + globalThis.location.hostname + ':8080/api/user/get_follower';
    Http.open("GET", url, true);
    Http.onreadystatechange = function () {
        if (this.readyState === 4 && this.status === 200) {
            console.log(JSON.parse(Http.responseText));
        }
    };
    Http.send(null);
}

function logout() {
    localStorage.clear();
    document.cookie = "login_token=;";
    globalThis.location.reload();
}

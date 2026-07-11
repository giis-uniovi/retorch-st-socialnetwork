function showUsername() {
    const username = localStorage.getItem("username");
    if (username != null) {
        document.getElementById("username").textContent = username;
    }
}

function uploadPost(media_json) {
    if (document.getElementById('post-content').value !== "") {
        const Http = new XMLHttpRequest();
        const url = globalThis.location.origin + '/api/post/compose';
        Http.open("POST", url, true);
        let body = "post_type=0&text=" + document.getElementById('post-content').value;
        Http.onreadystatechange = function () {
            if (this.readyState === 4 && this.status === 200) {
                console.log(Http.responseText);
            }
        };
        if (media_json === undefined) {
            Http.send(body);
        } else {
            body += "&media_ids=[\"" + media_json.media_id + "\"]&media_types=[\"" + media_json.media_type + "\"]";
            Http.send(body);
        }
        globalThis.location.reload();
    }
}

function fetchAndRenderList(apiUrl, containerClass, itemClass, listId, fieldName) {
    const Http = new XMLHttpRequest();
    Http.open("GET", apiUrl, true);
    Http.onreadystatechange = function () {
        if (this.readyState === 4 && this.status === 200) {
            const resp_json = JSON.parse(Http.responseText);
            const item_div = document.getElementsByClassName(containerClass);
            const item_id = document.getElementsByClassName(itemClass);
            for (let i = 0; i < resp_json.length; i++) {
                if (i === item_div.length - 1) {
                    document.getElementById(listId).appendChild(item_div[i].cloneNode(true));
                }
                item_div[i].style.display = "block";
                item_id[i].innerText = resp_json[i][fieldName];
            }
        }
    };
    Http.send(null);
}

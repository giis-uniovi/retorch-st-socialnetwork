const freq_con_btn = document.querySelectorAll(".freq-c");
const unfollow_btn = document.querySelectorAll(".unfollow");

for (const btn of freq_con_btn) {
    btn.addEventListener("click", function () {
        this.classList.toggle("freq_clicked");
        $(this).text($(this).hasClass("freq_clicked") ? "Remove from Frequent Contact" : "Set Frequent Contact");
    });
}

for (const btn of unfollow_btn) {
    btn.addEventListener("click", function () {
        this.classList.toggle("unfollow_clicked");
        $(this).text($(this).hasClass("unfollow_clicked") ? "Follow" : "Unfollow");
    });
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

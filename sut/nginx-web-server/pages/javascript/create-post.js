function clickEvent() {
    if (document.getElementById('media').value === "") {
        uploadPost();
    } else {
        const formData = new FormData(document.getElementById('media-form'));
        const Http = new XMLHttpRequest();
        const url = globalThis.location.protocol + '//' + globalThis.location.hostname + ':8081/upload-media';
        Http.onreadystatechange = function () {
            if (this.readyState === 4 && this.status === 200) {
                const resp = JSON.parse(Http.responseText);
                uploadPost(resp);
            }
        };
        Http.open("POST", url, true);
        Http.send(formData);
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

const hide = document.getElementById('hide-post');
const show = document.getElementById('show-post');

hide.addEventListener("click", function () {
    $("#compose").hide();
});

show.addEventListener("click", function () {
    $("#compose").toggle();
});

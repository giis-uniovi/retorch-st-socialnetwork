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

// uploadPost is defined in utils.js

const hide = document.getElementById('hide-post');
const show = document.getElementById('show-post');

hide.addEventListener("click", function () {
    $("#compose").hide();
});

show.addEventListener("click", function () {
    $("#compose").toggle();
});

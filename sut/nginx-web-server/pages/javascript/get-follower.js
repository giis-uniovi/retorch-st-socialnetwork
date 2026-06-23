function showFollowers() {
    const Http = new XMLHttpRequest();
    const url = globalThis.location.origin + '/api/user/get_follower';
    Http.open("GET", url, true);
    Http.onreadystatechange = function () {
        if (this.readyState === 4 && this.status === 200) {
            const resp_json = JSON.parse(Http.responseText);
            const follower_div = document.getElementsByClassName("follower-div");
            const follower_id = document.getElementsByClassName("follower-id");
            for (let i = 0; i < resp_json.length; i++) {
                if (i === follower_div.length - 1) {
                    document.getElementById("follower-list").appendChild(follower_div[i].cloneNode(true));
                }
                follower_div[i].style.display = "block";
                follower_id[i].innerText = resp_json[i]["follower_id"];
            }
        }
    };
    Http.send(null);
}

showFollowers();

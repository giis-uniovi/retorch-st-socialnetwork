function showfollowees() {
    const Http = new XMLHttpRequest();
    const url = 'http://' + globalThis.location.hostname + ':8080/api/user/get_followee';
    Http.open("GET", url, true);
    Http.onreadystatechange = function () {
        if (this.readyState === 4 && this.status === 200) {
            const resp_json = JSON.parse(Http.responseText);
            const followee_div = document.getElementsByClassName("followee-div");
            const followee_id = document.getElementsByClassName("followee-id");
            for (let i = 0; i < resp_json.length; i++) {
                if (i === followee_div.length - 1) {
                    document.getElementById("followee-list").appendChild(followee_div[i].cloneNode(true));
                }
                followee_div[i].style.display = "block";
                followee_id[i].innerText = resp_json[i]["followee_id"];
            }
        }
    };
    Http.send(null);
}

showfollowees();

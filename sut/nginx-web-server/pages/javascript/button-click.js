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

// uploadPost is defined in utils.js

const followUsername = () => {
    const username = localStorage.getItem("username");
    document.querySelectorAll(".follow-username").forEach(function (element) {
        element.setAttribute("value", username);
    });
};

function showUsername() {
    const username = localStorage.getItem("username");
    if (username != null) {
        document.getElementById("username").textContent = username;
    }
}

followUsername();

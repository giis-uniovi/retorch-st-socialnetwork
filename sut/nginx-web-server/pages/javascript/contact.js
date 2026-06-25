const followUsername = () => {
    const username = localStorage.getItem("username");
    document.querySelectorAll(".follow-username").forEach(function (element) {
        element.setAttribute("value", username);
    });
};

// showUsername is defined in utils.js

followUsername();

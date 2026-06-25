function renderNavbar(activePostHref) {
    const container = document.getElementById("navbar-container");
    if (!container) { return; }
    container.innerHTML = `
        <nav class="navbar navbar-expand-lg navbar-dark" style="background-color: #1F85DE;">
            <a class="navbar-brand" href="main.html">
                <img src="./death-star.svg"
                    width="27" height="27" class="d-inline-block align-top" alt="" />
                DeathStar
            </a>
            <button class="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarSupportedContent"
                aria-controls="navbarSupportedContent" aria-expanded="false" aria-label="Toggle navigation">
                <span class="navbar-toggler-icon"></span>
            </button>

            <div class="collapse navbar-collapse" id="navbarSupportedContent">
                <form class="form-inline my-4 my-lg-0 col-md-6 offset-md-2">
                    <input class="form-control mr-sm-2" type="text" placeholder="Search" aria-label="Search"
                        id="input_user_name">
                    <button class="btn btn-success my-2 my-sm-0" type="submit">
                        Search
                    </button>
                </form>

                <ul class="navbar-nav col-md-4">
                    <li class="nav-item">
                        <a class="nav-link text-white" href="${activePostHref}" id="show-post">
                            <i class="fas fa-edit"></i>
                            Post </a>
                    </li>

                    <li class="nav-item">
                        <a class="nav-link text-white" href="contact.html">
                            <i class="fas fa-users"></i>
                            Contacts</a>
                    </li>

                    <li class="nav-item dropdown ">
                        <button class="btn btn-link nav-link dropdown-toggle text-white user-name p-0"
                            id="navbarDropdown" type="button"
                            data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                            <i class="fas fa-user"></i>
                            <div id="username" style="display: inline;"></div>
                        </button>

                        <div class="dropdown-menu" aria-labelledby="navbarDropdown">
                            <a class="dropdown-item" href="./profile.html">Profile &amp; Setting</a>
                            <div class="dropdown-divider"></div>
                            <button type="button" class="dropdown-item text-danger" onclick="logout();">Logout</button>
                        </div>
                    </li>
                </ul>
            </div>
        </nav>`;
}

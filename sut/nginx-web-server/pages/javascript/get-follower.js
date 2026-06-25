// fetchAndRenderList is defined in utils.js

function showFollowers() {
    const url = globalThis.location.origin + '/api/user/get_follower';
    fetchAndRenderList(url, "follower-div", "follower-id", "follower-list", "follower_id");
}

showFollowers();

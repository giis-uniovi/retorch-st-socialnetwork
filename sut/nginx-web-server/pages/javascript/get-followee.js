// fetchAndRenderList is defined in utils.js

function showfollowees() {
    const url = globalThis.location.origin + '/api/user/get_followee';
    fetchAndRenderList(url, "followee-div", "followee-id", "followee-list", "followee_id");
}

showfollowees();


function prepareAlert() {
    alertRef = $('#feedback-email');
}

function resetPhoneValue() {
    phComponent.value = "";
    updatePhoneValue();
}

//This is called when the send button is pushed
function tempDisable(id, timeout, next){
    let button = zk.$("$" + id);
    button.setDisabled(true);
    setTimeout(function(w) { w.setDisabled(false) }, timeout, button);

    if (next) {
        var next = $("#" + next);
        if (next) {
            setTimeout(function(e) {
                try {
                    e.focus();
                } catch (ex) {
                }
            }, 100, next);
        }
    }

}
//A bootstrap alert to be used when receiving a response from server
var alertRef;

function initTooltips() {
    setTimeout(function() {
        try {
            //Doing this inside document.ready callback throws error, thus we resort to timer
            $('[data-toggle="tooltip"]').tooltip();
            //Calling tooltip apparently does not always do its job very well
        } catch (e) {
        }
    }, 2000, null);
}

function regenerateFooter() {
    try {
        var foot = $('.cust-footer-msg-rule');
        var content = foot.css('content');
        if (content && (content.charAt(0) == '"' || content.charAt(0) == "'")) {
            content = eval(content); //This helps deal with escaped quotes
            if (content.length == 0) {
                foot.remove();
            } else {
                foot.html(content);
            }
        }
    } catch (e) {}
}

function sendToServer(data) {
    var widget =  zk.$('$message');
    zAu.send(new zk.Event(widget, "onData", data, {toServer:true}));
}

//Maps a ZK notification constant with a bootstrap alert class
function alertClassForType(type) {

    //See java class org.zkoss.zk.ui.util.Clients
    switch (type) {
        case "info":
            return "alert-success";
        case "warning":
            return "alert-danger";
        case "error":
            return "alert-danger";
    }
    return "";

}

function markupIconForType(type) {

    var cls = "";
    switch (type) {
        case "info":
            cls = "fa-check-circle";
        break;
        case "warning":
            cls = "fa-exclamation-triangle";
        break;
        case "error":
            cls ="fa-exclamation-circle";
        break;
    }
    return cls == "" ? cls : "<i class=\"fas " + cls + "\"></i>";

}

function showAlert(message, type, delay) {

    if (alertRef) {
        var cls = alertClassForType(type);
        cls = cls == "" ? cls : " " + cls;

        alertRef.removeClass();
        alertRef.addClass('alert' + cls);

        alertRef.html(markupIconForType(type) + "&nbsp;" + message);
        alertRef.show();
        alertRef.delay(delay).slideUp(200, function() {});
    }

}

/*
 This couple of functions control behaviour when the hamburguer icon is clicked
 They are heavily coupled to each other, also they are dependant on HTML and CSS
 used in markup.
 Edit carefully
 */
function partialCollapse() {
    $(".collapsible-menu-item").toggleClass("di dn")
    var aside = $("aside")
    if (!aside.is(":visible")) {
        aside.show()
    }
    aside.toggleClass("w-14r")
}

function collapse() {
    var items = $(".collapsible-menu-item")
    if (items.hasClass("dn")) {
        //revert to original state
        items.toggleClass("di dn")
    }
    var aside = $("aside")
    if (!aside.hasClass("w-14r")) {
        aside.toggleClass("w-14r")
    }
    aside.is(":visible") ? aside.hide() : aside.show()
}

//Computes the strength of password entered in the input element whose ID is passed and sends the score back to server
function updateStrength(id){
    var widget = zk.$('$' + id);
    var strength = zxcvbn(document.getElementById(id).value);
    zAu.send(new zk.Event(widget, "onData", strength.score, {toServer:true}));
}

function togglePass(icon, elemID) {
    $(icon).toggleClass('fa-eye-slash').toggleClass('fa-eye');
    $('#' + elemID).togglePassword();
}

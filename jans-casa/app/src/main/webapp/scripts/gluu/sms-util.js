//Initializes the flag+phone mockery
let phComponent = document.querySelector("#phone")
let widgetId = "phWidget"

let iti = intlTelInput(phComponent, {
    separateDialCode: true,
    preferredCountries: [ "us" ]
})

phComponent.addEventListener("countrychange", function() { updatePhoneValue() });

function updatePhoneValue() {
    let widget = zk.$("$" + widgetId);
    widget.setValue(iti.getSelectedCountryData().dialCode + phComponent.value);
    widget.fireOnChange({});
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

function prepareAlert() {
    alertRef = $('#feedback-phone-edit');
}

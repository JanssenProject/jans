function fillAnswer(name, choices, nameText, text) {
    
    let queries = [ "input[type='radio']", "input[type='checkbox']" ]
    for (q of queries) {
        let elems = document.querySelectorAll(q)

        for (let el of elems) {
            if (el.name == name) {
                for (let val of choices) {
                    if (el.value == val) {
                        console.log("Autochecking form control " + name)
                        el.checked = true
                    }
                }
            }
        }
    }
    
    elems = document.getElementsByName(nameText)
    if (elems && elems.length > 0) {
        console.log("Autofilling form control " + nameText)
        elems[0].value = text
    }
    
}

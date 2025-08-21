<#import "commons.ftlh" as com>
<@com.main>

    <#assign assets=cache.casa_assets grabLocation=(captureLocation!false)>

    <section class="pa4 shadow-4 bg-blank gray cust-section">
        <h2 class="f3 dark-blue2">${labels("casa.welcome")}</h2>
        
        <#if message??>
            <p class="w5 dark-red tc pv2 ph0 ma0 f6">${message}</p>
        </#if>
        
        <form name="login" method="post" enctype="application/x-www-form-urlencoded" class="pt2">
            <div class="relative w5 mt4 pb2">
                <input name="username" value="${username!}" type="text" class="focused-text w-100 pb1 dark-gray" required>
                <label class="focused-label-big">${labels("casa.username")}</label>
            </div>
            <div class="relative w5 mt4 pb1">
                <input name="password" type="password" class="focused-text w-100 pb1 dark-gray" required>
                <label class="focused-label-big">${labels("casa.password")}</label>
            </div>
            <div class="mt5">
                <input type="submit" value="${labels("casa.login")}"
                    class="f7-cust bw0 br1 ph4 pv2 bg-bsgreen-success white hover-bsgreen-success hover-white cust-primary-button">
            </div>

            <input name="device" type="hidden">
            <#if grabLocation>
                <input name="location" type="hidden">
            </#if>
        </form>
    </section>

    <script src="${assets.contextPath}/scripts/ua-parser.pack-2.0.0.js"></script>
    <script>
        function attachHeaders(xhr, headers) {
            for (var i = 0; i < headers.length; i++) {
                xhr.setRequestHeader(headers[i].name, headers[i].value)
            }
        }
        
        function attachEvents(xhr, res, rej) {
        
            xhr.onload = () => {
                if (xhr.status < 300) {
                    return res(xhr.responseText)
                } else {
                    return rej(xhr.responseText)
                }
            }
            xhr.onerror = () => rej(xhr.statusText)
        
        }
        
        function genericGET(url, headers) {
        
          return new Promise((resolve, reject) => {
            const xhr = new XMLHttpRequest()
            xhr.open("GET", url)
            if (headers) {
                attachHeaders(xhr, headers)
            }
            attachEvents(xhr, resolve, reject)
            xhr.send()
          })
        
        }
        
        function processLocation(obj, zoneProp, cityProp, stateProp, countryProp) {
            
            let country = obj[countryProp], state = obj[stateProp], city = obj[cityProp], zone = obj[zoneProp]
            
            if (!country) country = null
            if (!state) state = null
            if (!city) city = null
            if (!zone) zone = null
            
            return { country, state, city, zone } 
            
        }
    
        function fillLocationData(field) {

            genericGET("https://api.ipapi.is", [])
                .then(result => {
        
                        let obj = JSON.parse(result)
                        if (!obj.location) throw new Error("ipapi - no location object")
                            
                        obj = processLocation(obj.location, "timezone", "city", "state", "country")   //"countryCode"
                        field.value = JSON.stringify(obj)
                        //console.log(field.value)
                    })
                .catch(err => {
                        console.log("IPAPI call failed:" + err)
                        
                        genericGET("https://freeipapi.com/api/json/", [])
                            .then(result => {
                                    let obj = JSON.parse(result)
                                    
                                    obj = processLocation(obj, "timeZone", "cityName", "regionName", "countryName")   //"country_code"
                                    field.value = JSON.stringify(obj)
                            })
                            .catch(err => console.log("FREEIPAPI call failed:" + err))                
                    })
        }

        function fillPlatformData(field) {

            let uapr = (new UAParser()).getResult()
            
            // Enhanced device fingerprinting with additional validation
            let platform = { 
                name: uapr.browser.name || "Unknown", 
                version: uapr.browser.version || "0.0.0", 
                osName: uapr.os.name || "Unknown", 
                osVersion: uapr.os.version || "0.0.0",
                userAgent: navigator.userAgent,
                timestamp: new Date().getTime()
            }
            
            // Additional validation for Chrome
            if (platform.name === "Chrome") {
                // Ensure we have a proper version string
                if (!platform.version || platform.version === "0.0.0") {
                    // Try to extract version from user agent as fallback
                    const chromeMatch = navigator.userAgent.match(/Chrome\/(\d+\.\d+\.\d+)/)
                    if (chromeMatch) {
                        platform.version = chromeMatch[1]
                    }
                }
                
                // Additional Chrome-specific data
                platform.chromeVersion = platform.version
                platform.isChrome = true
            }
            
            // Validate that we have meaningful data
            if (platform.name === "Unknown" || platform.osName === "Unknown") {
                console.warn("Unable to detect browser or OS, using fallback values")
                // Use fallback values based on user agent
                if (navigator.userAgent.includes("Chrome")) {
                    platform.name = "Chrome"
                    platform.version = "100.0.0" // Fallback version
                } else if (navigator.userAgent.includes("Firefox")) {
                    platform.name = "Firefox"
                    platform.version = "100.0.0" // Fallback version
                } else if (navigator.userAgent.includes("Safari")) {
                    platform.name = "Safari"
                    platform.version = "15.0.0" // Fallback version
                }
                
                if (navigator.userAgent.includes("Windows")) {
                    platform.osName = "Windows"
                    platform.osVersion = "10.0.0"
                } else if (navigator.userAgent.includes("Mac")) {
                    platform.osName = "macOS"
                    platform.osVersion = "12.0.0"
                } else if (navigator.userAgent.includes("Linux")) {
                    platform.osName = "Linux"
                    platform.osVersion = "5.0.0"
                }
            }
            
            field.value = JSON.stringify(platform)
            console.log("Device fingerprint:", field.value)

        }

        window.onload = (event) => { 
            
            let f = document.forms.login
            if (${grabLocation?c}) fillLocationData(f.location)
            fillPlatformData(f.device)

        }
    </script>

    <#-- "preload" font awesome used later in selector.ftlh -->
    <script src="${assets.contextPath}/scripts/font-awesome-5.12.1.all.min.js" defer></script>

</@com.main>

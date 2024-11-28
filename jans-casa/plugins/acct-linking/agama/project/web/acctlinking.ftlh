<#import "../kz1vc3/commons.ftlh" as com>
<@com.main>

    <#assign assets=cache.casa_assets grabLocation=(captureLocation!false) providers=cache.acctlinking_providers>

    <section class="pa4 shadow-4 bg-blank gray cust-section">
        <h2 class="f3 dark-blue2">Welcome</h2>
        
        <#if message??>
            <p class="w5 dark-red tc pv2 ph0 ma0 f6">${message}</p>
        </#if>
        
        <form name="login" method="post" enctype="application/x-www-form-urlencoded" class="pt2">
            <div class="relative w5 mt4 pb2">
                <input name="username" value="${username!}" type="text" class="focused-text w-100 pb1 dark-gray" required>
                <label class="focused-label-big">Username</label>
            </div>
            <div class="relative w5 mt4 pb1">
                <input name="password" type="password" class="focused-text w-100 pb1 dark-gray" required>
                <label class="focused-label-big">Password</label>
            </div>
            <div class="mt4 flex justify-between items-center">
                <input type="submit" value="Login"
                    class="f7-cust bw0 br1 ph4 pv2 bg-bsgreen-success white hover-bsgreen-success hover-white cust-primary-button">

                <#if providers?size gt 0>
                    <p class="f7 lh-title tc w4 ma0">OR use an external service to sign in:</p>
                </#if>
            </div>
            <div class="pt4 ph2 flex justify-center dn">
                <table class="table table-hover table-sm">
                    <tbody class="table-striped">
                    
                        <#list providers as p>
                            <tr class="pointer" onClick="javascript:idp_redirect('${p.id}')">
                                <td class="w2 tc">
                                    <img style="max-width:2rem" alt="${p.displayName!""}"
                                        src="${p.logoImg!"../t505fp/inboundID/none.png"}" />
                                </td>
                                <td class="f7-cust w4 v-mid">${p.displayName!""}</td>
                            </tr>
                        </#list>

                    </tbody>
                </table>
            </div>
				
            <input name="device" type="hidden">
            <#if grabLocation>
                <input name="location" type="hidden">
            </#if>
        </form>
    </section>
    
    <form name="externalLogin" method="post" enctype="application/x-www-form-urlencoded">
        <input type="hidden" name="_abort">
        <input type="hidden" name="providerId">
    </form>
    <script>
        function idp_redirect(id) {
            let form = document.forms.externalLogin
			form.providerId.value = id
			form.submit()
		}
    </script>
    
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
            let platform = { name: uapr.browser.name, version: uapr.browser.version, 
                    osName: uapr.os.name, osVersion: uapr.os.version }                        
            field.value = JSON.stringify(platform)
            console.log(field.value)

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

<#macro main>

<#assign assets=cache.casa_assets>

<!doctype html>
<html xmlns="http://www.w3.org/1999/xhtml">
    <head>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <link rel="icon" href="${assets.contextPath}${assets.faviconUrl}">
        <!-- order of stylesheets is important, do not change -->
        <link rel="stylesheet" href="${assets.contextPath}/styles/bootstrap.min.css">
        <link rel="stylesheet" href="${assets.contextPath}${assets.prefix}/styles/gluu/style.css">
        <link rel="stylesheet" href="${assets.contextPath}/styles/tachyons.min.css">

        <!-- load custom branding css snippets -->
        <style>${assets.extraCss!""}</style>

        <style>
            .focused-label-big {
                top: 0;
                left: 0;
                right: 0;
                display: flex;
                align-items: center;
                position: absolute;
                font-size: .9rem;
                cursor: text;
                transition: .2s ease-in-out;
                box-sizing: border-box;
                height: .6rem;
                width: 100%;
                pointer-events: none;
                margin-top: .25rem;
            }
            .focused-text:valid + .focused-label-big,
            .focused-text:focus + .focused-label-big {
                top: -28px;
                font-size: .7rem;
                color: #15b565;
            }
        </style>

        <script src="${assets.contextPath}/scripts/gluu/main-util.js"></script>
    </head>
    <body class="bg-near-white cust-body-rule">
        <div class="pa0 flex justify-center gray f6">
            <div class="flex flex-column min-vh-100 miw-16r mw-60r w-100 bg-washed-blue2 cust-content-rule">
                <header class="pv2 shadow-4 bg-blank cust-header">
                    <div class="w1-ns"></div>
                    <div class="w4 mh4-ns tc">
                        <img src="${assets.contextPath}${assets.logoUrl}">
                    </div>
                </header>
                <!-- grow horizontally and vertically as much as possible -->
                <div class="flex flex-column justify-between w-100 h-100">
                    <div class="flex flex-column items-center justify-center w-100 h-100 mv4">                    
                        <noscript><p class="red">Please enable javascript to use this page!</p></noscript>
                        
                        <#nested>

                    </div>
                    <footer class="pa3 f7 dark-gray shadow-4 bg-blank cust-footer-rule">
                        <div class="cust-footer-msg-rule">
                            Powered by <a href="https://jans.io" class="link blue">Janssen</a>.
                        </div>
                    </footer>
                </div>
            </div>
        </div>
        
        <script>regenerateFooter()</script>
    </body>

</html>

</#macro>

<#macro alternative>

<form method="post" enctype="application/x-www-form-urlencoded">
    <div class="flex flex-column items-center pt3 mt2">
        <span class="f7">Stuck or changed your mind?</span>
        <button class="f7-cust pa0 bw0 br0 blue hover-dark-blue underline-hover bg-transparent cust-link-button"
                type="submit" id="skipped" name="skipped" value="">
            Use a different authentication method
        </button>
    </div>
</form> 

</#macro>

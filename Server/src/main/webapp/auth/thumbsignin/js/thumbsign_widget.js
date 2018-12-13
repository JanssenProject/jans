(function (root, factory) {
    if (typeof define === 'function' && define.amd) {
        // AMD. Register as an anonymous module.
        define(['jquery'], factory);
    } else if (typeof module === 'object' && module.exports) {
        // Node. Does not work with strict CommonJS, but
        // only CommonJS-like environments that support module.exports,
        // like Node.
        module.exports = factory(require('jquery'));
    } else {
        // Browser globals (root is window)
        root.thumbSignIn = factory(root.jquery);
    }
}(this, function () {
    'use strict';
    var getElem = function (url) {
        return {
            elem: `
    <div class="tsmodal-content ts-content" style="display: none;">
        <div class="tsmodal-body ts-body">
            <p class="ts_initiated ts-initiated" id="tsInitiated"/>
            <div class="item html" >
                <div class="ts-timeout overlay ts-overlay" style="display: none;">
                    <div class="refreshImg ts-refresh-img"></div>
                    <div class="refreshTxt ts-refresh-txt" id="refreshTXT"></div>
                </div>
                <p class="sucessMsg ts-sucess-msg"/>
                <div id="qrcode" class="qrcode ts-qrcode">
                    <div data-type="mobile" class="hidden">
                        <div style="margin-top: 10px;">
                            <img id="openApp" class="open-app-icon ts-app-icon" src="${url}/styles/img/icon_old.png" style="display:none;"/>
                            <div class="open-app ts-app-link"> <a style="font-size:16px;font-family:'Roboto';" class="btn deeplink" target="_blank" href="#"></a></div>
                        </div>
                    </div>
                    <div data-type="desktop" class="hidden">
                        <div>
                            <div class="logoIcon ts-logo-icon"/><img id="openQR" src=""/></div>
                        </div>
                    </div>
                    <div class="circle-loader ts-circle-loader">
                        <div class="checkmark draw ts-checkmark"/>
                    </div>
                </div>
            </div>
            <div class="intro-conatiner ts-intro-conatiner" style="margin-top:30px">
                <p id="intro_msg" class="intro_msg ts-intro-msg"/>
                <p id="toggle_view ts-toggle-view" style="display:none;">
                    <div id="toggle_view_text" class="toggle_view_text"></div>
                    <a id="toggle_view_link" class="toggle_view_link"></a>
                    <span id="toggle_view_span_text"></span>
                </p>
            </div>
            <div id="module-wrapper" class="ts-module-wrapper"/>
        </div>
    </div>
            `,
            style: ""
        }
    };
    
    var defaultConfig = {
        'widgetUrlParameterPart': '#ThumbsignId',
        'widgetUrlParameterSeparator': "----",
        'ALREADY_REGISTERED': "You have already enabled passwordless authentication for this account using this device.",
        'NOT_REGISTERED': "Can't login because the passwordless account doesn't exist.",
        'NO_SUITABLE_AUTHENTICATOR': "You have not added fingerprint in your device.",
        'DECLINED': "Cancelled",
        'CANCELLED': "Cancelled",
        'TIMEOUT': "Please try again.",
        'TECHNICAL_REASON_ERROR': "Please try again.",
        'TECHNICAL_REASON': "Please try again.",
        'COMPLETED_FAILURE': "",
        'DEEP_LINK': "Open in Thumbsignin app",
        'COMPLETED_SUCCESSFUL': 'Finished successfully',
        'desktop': {
            'REFRESH_MSG': "QR Code expired.<br/> Click to regenerate.",
            'switch-msg': "Show QR Code",
            'INTRO_MSG"': "Scan the QR code above using the ThumbSignIn app on your phone.",
            'TOGGLE_MSG1': "Have app on this phone?",
            'TOGGLE_MSG2': "Open app",
            'INITIATED': "QR Code Scanned.",
            'TIMEOUT': "Click to regenerate the QR code "
        },
        'mobile': {
        	'REFRESH_MSG': "Click on refresh icon <br/> and try again.",
        	'switch-msg': "Show on App",
        	'INTRO_MSG"': "",
        	'TOGGLE_MSG1': "Have app on a different phone?",
        	'TOGGLE_MSG2': "Show QR Code",
        	'INITIATED': "Authentication Initiated.",
        	'TIMEOUT': "Click to regenerate the QR code "
        	}
    }

    var data = {
        curentConfig: defaultConfig,
        config: {},
        module: {},
        store: {},
        rootURL: 'https://thumbsignin.com',
    }

    var api = {
        init: function (options) {
            var name = options.id;
            if (window.name) {
                delete window[id];
            }
            window[name] = {};
            return preLoader(options.style)
                .then(function () {
                    data.config[options.config].desktop = Object.assign(data.curentConfig.desktop, data.config[options.config].desktop || {});
                    data.config[options.config].mobile = Object.assign(data.curentConfig.mobile, data.config[options.config].mobile || {});
                    var _conf = Object.assign(data.curentConfig, data.config[options.config] || {});
                    _conf.rootURL = data.rootURL;
                    //make builder object
                    var buildObj = new Builder(options, _conf, getElem(data.rootURL));
                    data.store[options.id] = buildObj;
                    buildObj.initBuilder();
                    window[name] = buildObj;
                    return buildObj;
                })
                .catch(function (err) {
                    throw err;
                })
        },

        setConfig: function (id) {
            if (id !== undefined && data.config[id] !== undefined) {
                data.curentConfig = data.config[id];
            }
        },
        addModule: function (id, config, fn) {
            if (id !== undefined && config !== undefined && typeof fn === "function") {
                data.module[id] = {
                    config: config.config,
                    getElem: config.getElem,
                    fn: fn,
                }
            }
            return
        },
        addConfig: function (id, config) {
            if (id !== undefined && typeof config === 'object') {
                data.config[id] = config
            }
            return
        },

    }

    function preLoader(file) {
        var addOn = {
            'axios': {
                url: 'https://cdnjs.cloudflare.com/ajax/libs/axios/0.15.3/axios.min.js'
            },
            'jquery': {
                url: 'https://cdnjs.cloudflare.com/ajax/libs/jquery/3.2.1/jquery.min.js'
            },
            'style': {
                url: './js/thumbsignin_widget.css'
            }
        }
        if (file && typeof file == "string") {
            addOn['customStyle'] = {url:file};
        }
        return loadplugins(addOn);
    }

    function loadplugins(preLoadObj) {
        var _this = this;
        var list = Object.values(preLoadObj);
        if (list.length === 0) {
            return Promise.resolve();
        }
        var _ps = [];
        for (var j = 0; j < list.length; j++) {
            var files = list[j];
            if (!Array.isArray(files.url)) {
                files.url = [files.url];
            }
            for (var i = 0; i < files.url.length; i++) {
                _ps.push(new Promise(function (resolve) {
                    if (files.url[i].endsWith('.js')) {
                        var script = document.createElement('script');
                        script.src = files.url[i];
                        script.setAttribute('async', '');
                        script.onerror = function (evt) {
                            console.log("Script Error", evt);
                        };
                        script.onload = function () {
                            resolve()
                            if (typeof files.done === 'function') {
                                files.done.apply(_this, _this.DOM)
                            }
                        }
                        document.getElementsByTagName('head')[0].appendChild(script);
                    } else {
                        var link = document.createElement("link");
                        link.rel = "stylesheet";
                        link.href = files.url[i];
                        link.onerror = function (evt) {
                            console.log("LINK Error", evt);
                        }
                        link.onload = function () {
                            resolve()
                        }
                        document.getElementsByTagName('head')[0].appendChild(link);
                    }
                }));
            }
        }
        return Promise.all(_ps)
            .catch(function (err) {
                if (this.events && this.events['ERROR'] !== null) {
                    var _event = this.events['ERROR']
                    _event.fn.call(_event.scope, err);
                } else {
                    throw err;
                }
            });
    }

    var Builder = (function (options, defaultConfig, element) {
        var resource = {
            config: defaultConfig,
            name: options.id,
            rootID: $('#' + options.container),
            elem: element.elem,
            style: element.style,
            loadedPlugins: {},
            isMobile: ('ontouchstart' in document.documentElement && navigator.userAgent.match(/Mobi/))
        }

        var transId;

        var utils = {
            getBrowser: function () {
                var userAgent = navigator.userAgent.toLowerCase(),
                    list = ['msie', 'chrome', 'crios', 'firefox', 'fxios', 'opera', 'opios', 'ucbrowser'],
                    browser;
                for (var i = 0; i < list.length; i++) {
                    if (userAgent.search(list[i]) >= 0) {
                        browser = list[i];
                    }
                    if (userAgent.search("safari") >= 0 &&
                        userAgent.search("chrome") < 0 &&
                        userAgent.search("fxios") < 0 &&
                        userAgent.search("crios") < 0) {
                        browser = "safari";
                    }
                    
                }
                return browser;
            },

            getReturnURL: function () {

                return encodeURI(window.location.href.substr(0,
                    (-1 === window.location.href.indexOf('#')) ?
                        window.location.href.length :
                        window.location.href.indexOf('#')));
            },

            isIOS: function () {
                var iDevices = [
                    'iPad Simulator',
                    'iPhone Simulator',
                    'iPod Simulator',
                    'iPad',
                    'iPhone',
                    'iPod'
                ];

                if (!!navigator.platform) {
                    while (iDevices.length) {
                        if (navigator.platform === iDevices.pop()) {
                            return true;
                        }
                    }
                }

                return false;
            },

            isIOSWebView: function () {
                if (utils.isIOS()) {
                    var userAgent = navigator.userAgent.toLowerCase();
                    var webviewstrings = ['crios', 'safari'];
                    var inx=0;
                    for(inx =0; inx < webviewstrings.length; inx++) {
                        if(userAgent.search(webviewstrings[inx]) !== -1) {
                            return false;
                        }
                    }
                    return true;
                }
                return false;
            }
        }
        /*****WIP for Reset API*******/
        function reset() {
            this.refresh();
        }
        /*****WIP for Reload API*******/
        function reload() {
            var _this = this;
            $(resource.rootID).empty();
            this.DOM = $(resource.elem).appendTo(resource.rootID);
            this.DOM.data('id', this.id);
            this.DOM.data('name', resource.name);
            //this.DOM.find('.fs18').html(resource.config.title);
            this.DOM.find('.circle-loader').show();
            // ADD DOM EVENTS
            //this.DOM.find(".closeIcon").click(this.close.bind(this));
            if (resource.isMobile) {
                this.DOM.find('#toggle_view_link').click(toggleView.bind(this));
                this.DOM.find('#toggle_view').show();
            }
            this.DOM.find('.ts-timeout').click(function () {
                //_this.refresh();
                //fetchData.call(_this);
            	
            	var reinitiazeButton = document.getElementById("initForm:reinitializeCurrentStep");
            	reinitiazeButton.click();
            });
        }

        function refresh(hardReset, config) {

            if (transId) {
                axios.get(resource.config.statusUrl + transId + '?cancelled=true');
            }
            if (hardReset === true) {
                //hard refresh, reset to get new configs updated
                //reset.call(this, config);
                fetchData.call(this);
            } else {
                this.DOM.find('.ts-timeout').hide();
                this.DOM.find('#intro_msg')
                    .text(resource.isMobile ? resource.config.mobile['INTRO_MSG"'] :
                        resource.config.desktop['INTRO_MSG"'])
                    .show();
                this.DOM.find('.checkmark').hide();
                this.DOM.find('.sucessMsg').hide();
                this.DOM.find("#qrcode").hide();
                this.DOM.find('#openQR').attr('src', '');
                this.DOM.find('#qrcode [data-type="mobile"] a').attr('href', '#');
                this.DOM.find('.circle-loader')
                    .removeClass('load-complete')
                    .css('opacity', '1')
                    .show();
                fetchData.call(this);
            }
        }

        function toggleView(eventObj) {
            if (this.DOM.find('.ts-timeout').is(':visible')) {
                eventObj.preventDefault();
                return;
            }
            var elem = this.DOM.find('#toggle_view_link');
            if (elem.data('curent') === 'mobile') {
                elem.data('curent', 'qr');
                elem.text(resource.config.desktop["TOGGLE_MSG2"]);
                this.DOM.find('#toggle_view_text').text(resource.config.desktop["TOGGLE_MSG1"]);
                eventObj.preventDefault();
                this.DOM.find('#qrcode [data-type="mobile"]').addClass('hidden');
                this.DOM.find('#qrcode [data-type="desktop"]').removeClass('hidden');
                //resource.isMobile = !resource.isMobile;
                this.DOM.find('#intro_msg')
                    .text(resource.config.desktop['INTRO_MSG"'])
                    .show();
            } else {
                elem.data('curent', 'mobile');
                //resource.isMobile = !resource.isMobile;
                elem.text(resource.config.mobile["TOGGLE_MSG2"]);
                this.DOM.find('#toggle_view_text').text(resource.config.mobile["TOGGLE_MSG1"]);
                this.DOM.find('#qrcode [data-type="mobile"]').removeClass('hidden');
                this.DOM.find('#qrcode [data-type="desktop"]').addClass('hidden');
                this.DOM.find('#intro_msg')
                    .text(resource.config.mobile['INTRO_MSG"'])
                    .show();
                if ( utils.isIOS() && utils.isIOSWebView() === false ) {
                    stopCheckingTransaction();
                }
            }

            // this.refresh();
            // fetchData.call(this);
        }

        function stopCheckingTransaction() {
            transId = undefined;
            clearTimeout(xhrTimer);
        }

        function initBuilder() {
            var _this = this,
                head = document.getElementsByTagName('head')[0],
                link = document.createElement('style');
            $(resource.rootID).empty();
            this.DOM = $(resource.elem).appendTo(resource.rootID);
            this.DOM.data('id', this.id);
            this.DOM.data('name', resource.name);
            //this.DOM.find('.fs18').html(resource.config.title);
            this.DOM.find('.circle-loader').show();
            // ADD DOM EVENTS
            //this.DOM.find(".closeIcon").click(this.close.bind(this));
            if (resource.isMobile) {
                this.DOM.find('#toggle_view_link').click(toggleView.bind(this));
                this.DOM.find('#toggle_view').show();
            }
            var deeplinkElem = _this.DOM.find('#qrcode [data-type="mobile"] a');
            if ( utils.isIOS() && utils.isIOSWebView() === false ) {
                deeplinkElem.bind("click", function () {
                    stopCheckingTransaction();
                });
            }

            this.DOM.find('.ts-timeout').click(function () {
                //_this.refresh();
                //fetchData.call(_this);
            	
            	var reinitiazeButton = document.getElementById("initForm:reinitializeCurrentStep");
            	reinitiazeButton.click();
            });

            link.rel = 'stylesheet';
            link.type = 'text/css';
            if (link.styleSheet) {
                link.styleSheet.cssText = resource.style
            } else {
                link.appendChild(document.createTextNode(resource.style));
            }
            head.appendChild(link);

            loadplugins(this.plugins)
                .then(function () {
                    //clean all loaded plugins
                    resource.loadedPlugins = Object.keys(_this.plugins);
                    _this.plugins = {};
                    console.log('plugins Loaded');
                });

            if (!resource.isMobile) {
                loadplugins({
                    'intlTelInputCss': {
                        url: `${data.rootURL}/styles/intlTelInput.css`
                    },
                    'intlTelInputJs': {
                        url: `${data.rootURL}/intlTelInput.min.js`
                    }
                }).then(function () {
                    if (_this.require && Array.isArray(_this.require)) {
                        //load the modules required
                        var moduleStyles = "", link;
                        for (var i = 0; i < _this.require.length; i++) {
                            var moduleName = _this.require[i];
                            if (moduleName && data.module[moduleName]) {
                                var mdlData,
                                    mdl = data.module[moduleName];
                                data.config[options.config][moduleName] = Object.assign(mdl.config, data.config[options.config][moduleName] || {});
                                mdlData = mdl.getElem(data.config[options.config], data.rootURL);
                                moduleStyles += mdlData.style.trim();
                                mdl.fn.call(_this, $(mdlData.elem), data.config[options.config]);
                            } else {
                                console.warn("could not fing module: " + moduleName);
                            }
                        }
                        if (moduleStyles.trim().length > 0) {
                            link = document.createElement('style');
                            link.rel = 'stylesheet';
                            link.type = 'text/css';
                            link.appendChild(document.createTextNode(moduleStyles));
                            document.getElementsByTagName('head')[0].appendChild(link);
                        }
                    }
                });
            }

            if ( utils.isIOS() && utils.isIOSWebView() === false ) {
                $(window).on('hashchange', function () {
                    startCheckingTransaction.call(_this);
                });
            }
        }

        function startCheckingTransaction() {
            if (window.location.hash.indexOf(resource.config.widgetUrlParameterPart) > -1 && resource.isMobile) {
                var infoArray = window.location.hash.substr(1).split(resource.config.widgetUrlParameterSeparator);
                transId = infoArray[1];
                this.DOM.find('#qrcode [data-type="desktop"]').removeClass('hidden');
                this.DOM.find('#qrcode [data-type="mobile"]').removeClass('hidden');
                this.DOM.find('#qrcode [data-type="desktop"]').addClass('hidden');
                this.DOM.find('#qrcode [data-type="mobile"]').addClass('hidden');
                this.DOM.find('.circle-loader').show();
                this.DOM.find('#toggle_view_text').text(resource.config.mobile["TOGGLE_MSG1"]);
                this.DOM.find('#toggle_view_link').hide();
                this.DOM.find('#toggle_view_span_text')
                    .text(resource.config.mobile["TOGGLE_MSG2"]);
                var elem = this.DOM.find('#qrcode [data-type="mobile"] a');
                elem.html(resource.config["DEEP_LINK"]);
                elem.attr('href', '#');

                this.DOM.find('#toggle_view_span_text').show();
                this.DOM.find('#toggle_view').show();
                checkTransaction.call(this, resource.config.statusUrl + transId);
                return true;
            }
            return false;
        }

        function addPlugin(id, fn) {
            if (id !== undefined && typeof fn === 'function') {
                this.plugins[id] = fn(); //fn returns config as per to integrate with,
                var _this = this;
                loadplugins.apply(this, [this.plugins]).then(function () {
                    //clean all loaded plugins
                    resource.loadedPlugins = Object.keys(_this.plugins);
                    _this.plugins = {};
                });
            }
        }

        function registerEvent(id, cb, scope) {
            if (id !== undefined && typeof cb === 'function') {
                this.events[id] = {
                    fn: cb,
                    scope: scope
                };
            }
        }

        function loadConfig(config) {
            var cnf = config || resource.config;
        }

        function fetchData() {
            var _this = this;
            var response = resource.config.actionResponse;
            
            if(response == ""){
            	var expirePageLink = document.getElementById("expireLink");
            	expirePageLink.click();
            	return;
            }
            
            var res;
        	if (typeof response === "string") {
        		res = JSON.parse(response);
        	} else {
        		res = response;
        	}
        	
        	console.log("Action Response:", res);
        	console.log("X-Ts-date Header:", resource.config.xTsDate);
    		console.log("Authorization Header:", resource.config.authHeader);

            _this.DOM.find('.circle-loader').hide();
            _this.DOM.find("#qrcode").show();

            var time = 0,
                initialOffset = '440',
                i = res.expireInSeconds;

            _this.DOM.find('#intro_msg')
                .text(resource.isMobile ? resource.config.mobile['INTRO_MSG"'] :
                    resource.config.desktop['INTRO_MSG"'])
                .show();

            _this.DOM.find('.refreshTxt')
                .html(resource.isMobile ? resource.config.mobile['REFRESH_MSG'] :
                    resource.config.desktop['REFRESH_MSG']);

            if (resource.isMobile && res.deepLinkUrl) {
                _this.DOM.find('#qrcode [data-type="desktop"]').addClass('hidden');
                if( utils.isIOS() && utils.isIOSWebView() === false ) {
                    res.deepLinkUrl += '?transactionId=' + res.transactionId +
                        '&browser=' + utils.getBrowser() + '&actionUrl=' + resource.config.actionUrl +
                        '&statusUrl=' + resource.config.statusUrl +
                        '&widgetUrlParameterSeparator=' + resource.config.widgetUrlParameterSeparator +
                        '&returnurl=' + utils.getReturnURL(res.transactionId);
                } else if(utils.isIOSWebView() === false) {
                	res.deepLinkUrl += '?browser=' + utils.getBrowser()
                }
                var elem = _this.DOM.find('#qrcode [data-type="mobile"] a');
                elem.attr('href', res.deepLinkUrl)
                elem.html(resource.config["DEEP_LINK"]);
                if ( 'safari' === utils.getBrowser() && utils.isIOSWebView() === false ) {
                    elem.attr("target", "_blank");
                }

                _this.DOM.find('#toggle_view_text').text(resource.config.mobile["TOGGLE_MSG1"]);
                _this.DOM.find('#toggle_view_link')
                    .text(resource.config.mobile["TOGGLE_MSG2"])
                    .data('curent', 'mobile');
                _this.DOM.find('#toggle_view_span_text')
                    .text('');
                _this.DOM.find('#toggle_view_span_text').hide();
                _this.DOM.find('#toggle_view').show();
                _this.DOM.find('#toggle_view_link').show();

                _this.DOM.find('#qrcode [data-type="mobile"]').removeClass('hidden');
                _this.DOM.find('#openQR').attr('src', '\data:image/png;base64,' + res.qrImage);
                _this.DOM.find('#toggle_view_link').attr("href", res.deepLinkUrl);
                if ( 'safari' === utils.getBrowser() && utils.isIOSWebView() === false ) {
                    _this.DOM.find('#toggle_view_link').attr("target", "_blank");
                }
            } else {
                _this.DOM.find('#qrcode [data-type="mobile"]').addClass('hidden');
                _this.DOM.find('#openQR').attr('src', '\data:image/png;base64,' + res.qrImage);
                _this.DOM.find('#qrcode').attr('data-deeplink-url', res.deepLinkUrl ? res.deepLinkUrl : '');
                _this.DOM.find('#qrcode [data-type="desktop"]').removeClass('hidden');

            }
            transId = res.transactionId;
            checkTransaction.call(_this, resource.config.statusUrl + res.transactionId);

            if (_this.events && _this.events['DATA_FETCHED'] !== null) {
                var _event = _this.events['DATA_FETCHED']
                _event.fn.call(_event.scope, res);
            }
        }
        var xhrTimer;

        function checkTransaction(url) {
            var _this = this;
            makeRequest(url, resource.config.authHeader, resource.config.xTsDate)
            .then(function (response) {
                
            	var res;
            	if (typeof response === "string") {
            		res = JSON.parse(response);
            	} else {
            		res = response;
            	}
                status = res.status;

                //RISE COMPLETED_SUCCESSFUL/PENDING/INTIATED?FAILURE EVENT LISTENER
                if (status === "COMPLETED_SUCCESSFUL") {
                    _this.DOM.find('.ts_initiated')
                        .text('');
                    transId = undefined;
                    clearTimeout(xhrTimer);
                    xhrTimer = undefined;
                    
                    if (resource.isMobile && resource.config.loginFlow === "Authentication") {
                    	_this.DOM.find('.item').css({"height": "200px", "width":"auto"});                  	
                    }
                    
                    _this.DOM.find('#qrcode').fadeOut('slow', function () {
                        _this.DOM.find('.qr-time').hide();
                        _this.DOM.find('.circle-loader').addClass('load-complete').show();
                        _this.DOM.find('.checkmark').show();
                        _this.DOM.find('#intro_msg').text(resource.config['COMPLETED_SUCCESSFUL']).show();

                        _this.DOM.find('.ts_initiated').hide();
                        setTimeout(function () {
                            if (_this.events && _this.events['SUCCESS'] !== null) {
                                var _event = _this.events['SUCCESS']
                                _event.fn.call(_event.scope, res);
                            }
                        }, 1000);

                    });
                } else if (status === "PENDING") {
                    if (_this.events && _this.events['PENDING'] !== null) {
                        var _event = _this.events['PENDING']
                        _event.fn.call(_event.scope, res);
                    }
                    xhrTimer = setTimeout(function () {
                        if (resource.config.statusUrl + transId === url) {
                            checkTransaction.call(_this, url);
                        } else {
                            console.log('exiting race condirtion');
                        }

                    }, 1500);
                } else if (status === "INITIATED") {
                    if (_this.events && _this.events['INITIATED'] !== null) {
                        var _event = _this.events['INITIATED']
                        _event.fn.call(_event.scope, res);
                    }
                    _this.DOM.find('#qrcode').css('opacity', '.1');

                    _this.DOM.find('.ts_initiated')
                        .text(resource.isMobile ? resource.config.mobile['INITIATED'] : resource.config.desktop['INITIATED'])
                        .show();

                    xhrTimer = setTimeout(function () {
                        if (resource.config.statusUrl + transId === url) {
                            checkTransaction.call(_this, url);
                        } else {
                            console.log('exiting race condirtion');
                        }

                    }, 1500);
                } else {
                    _this.DOM.find('.ts_initiated')
                        .text('');
                    if (_this.events && _this.events['FAILURE'] !== null) {
                        var _event = _this.events['FAILURE']
                        _event.fn.call(_event.scope, res);
                    }
                    transId = undefined;
                    clearTimeout(xhrTimer);
                    xhrTimer = undefined;
                    //status === "COMPLETED_FAILURE"
                    if (res.failureReason) {
                    	
                    	//Won't show the refresh icon after timeout
                        /*if (res.failureReason === 'TIMEOUT') {
                            //_this.DOM.find('#intro_msg')
                                //.text(resource.isMobile ? resource.config.mobile['TIMEOUT'] : resource.config.desktop['TIMEOUT']);
                        	_this.refresh();
                        } else {
                            _this.DOM.find('#intro_msg')
                                .text(resource.config[res.failureReason] || res.failureReason);
                            _this.DOM.find('.circle-loader').hide();
                            _this.DOM.find('.ts-timeout').fadeIn(function () {
                                _this.DOM.find('#qrcode').css('opacity', '1');
                            });
                        }*/
                        
                    	var counter;
                        //Will show the refresh icon after timeout
                        if (res.failureReason === 'TIMEOUT') {
                        	
                        	counter = 145;
                        	//Page will be expired after 145 seconds of user in-action
                            setInterval(function() {
                            	
                            	if (resource.config.loginFlow === "Registration" || counter === 0) {
                            		                            		
                            		if (resource.config.loginFlow === "Authentication") {
                            			//Hiding the refresh icon once the timer has expired
                            			_this.DOM.find('.ts-refresh-img').hide();
                            			_this.DOM.find('.ts-refresh-txt').hide();
                            		} else {
                            			if (resource.isMobile) {
                            				_this.DOM.find('.circle-loader').hide();
                            				_this.DOM.find('.ts-refresh-txt').hide();
                            				_this.DOM.find('.ts-refresh-img').hide();
                            			} else {
                            				_this.DOM.find('.circle-loader').hide();
                                			_this.DOM.find('.ts-timeout').fadeIn(function () {
                                				_this.DOM.find('.ts-refresh-img').hide();
                                    			_this.DOM.find('.ts-refresh-txt').hide();
                                                _this.DOM.find('#qrcode').css('opacity', '1');
                                            });
                            			}
                            		}
                            		                            		
                            		_this.DOM.find('#intro_msg')
                            		.text("Your session has expired. Redirecting to expiration page..");
                            		
                            		var expirePageLink = document.getElementById("expireLink");
                                	expirePageLink.click();
                                	
                                	
                            	} else {
                            		
                            		_this.DOM.find('#intro_msg')
                            		.text(resource.isMobile ? resource.config.mobile['TIMEOUT']+"before session gets expired in "+counter+" seconds..." : resource.config.desktop['TIMEOUT']+"before session gets expired in "+counter+" seconds...");
                            	
                            		counter--;
                            	}                            	                            	
                            	
                            }, 1000);
                        	
                        } else {
                        	
                        	//counter = (resource.config.loginFlow === "Authentication") ? (235 -  res.expireInSeconds) : 0;
                        	counter = (resource.config.loginFlow === "Authentication") ? 145 : 3;
                        	
                        	//Page will be expired after 145 secs of user inaction
                            setInterval(function() {
                            	
                            	if (resource.config.loginFlow === "Registration" || counter === 0) {
                            		
                            		if (resource.config.loginFlow === "Authentication") {
                            			//Hiding the refresh icon once the timer has expired
                            			_this.DOM.find('.ts-refresh-img').hide();
                            			_this.DOM.find('.ts-refresh-txt').hide();
                            		} else {
                            			if (resource.isMobile) {
                            				_this.DOM.find('.circle-loader').hide();
                            				_this.DOM.find('.ts-refresh-txt').hide();
                            				_this.DOM.find('.ts-refresh-img').hide();
                            			} else {
                            				_this.DOM.find('.circle-loader').hide();
                                			_this.DOM.find('.ts-timeout').fadeIn(function () {
                                				_this.DOM.find('.ts-refresh-img').hide();
                                    			_this.DOM.find('.ts-refresh-txt').hide();
                                                _this.DOM.find('#qrcode').css('opacity', '1');
                                            });
                            			}                           			
                            		}
                            		
                            		_this.DOM.find('#intro_msg')
                                	.text(resource.config[res.failureReason] || res.failureReason);
                            		
                            		if (counter === 0) {
                            			var expirePageLink = document.getElementById("expireLink");
                                    	expirePageLink.click();
                            		} else {
                            			counter--;
                            		}                          		
                                	
                            	} else {

                            		if (resource.config[res.failureReason] !== null && resource.config[res.failureReason].substr(resource.config[res.failureReason].length - 1) == '.' ) {
                            			_this.DOM.find('#intro_msg')
                                    	.text(resource.config[res.failureReason]+" Session expiring in "+counter+" seconds..." || res.failureReason);
                            		} else {
                            			_this.DOM.find('#intro_msg')
                                    	.text(resource.config[res.failureReason]+". Session expiring in "+counter+" seconds..." || res.failureReason);
                            		}
                            		                                                        	
                            		counter--;
                            	}
                            	
                            }, 1000);                            
                        }
                        
                        //Showing the refresh icon (.ts-timeout) only during Authentication Flow
                        if (resource.config.loginFlow === "Authentication") {
                        	_this.DOM.find('.circle-loader').hide();
                        	_this.DOM.find('.ts-timeout').fadeIn(function () {
                                _this.DOM.find('#qrcode').css('opacity', '1');
                            });
                        }
                                                
                        //If refresh icon is not clicked for more than 25 secs, below action is performed..
                        /*xhrTimer = setTimeout(function () {
                        	var expirePageLink = document.getElementById("expireLink");
                        	expirePageLink.click();
                        }, 25000);*/
                        
                    }
                }

            }).catch(function (err) {
                _this.DOM.find('.ts_initiated').text('');
                _this.DOM.find('#intro_msg')
                    .text(resource.config['TECHNICAL_REASON'] || 'Please try again');
                _this.DOM.find('.ts-timeout').fadeIn(function () {
                    _this.DOM.find('#qrcode').css('opacity', '1');
                });
                if (_this.events && _this.events['ERROR'] !== null) {
                    var _event = _this.events['ERROR']
                    _event.fn.call(_event.scope, err);
                } else {
                    throw err;
                }
                transId = undefined;
                xhrTimer = undefined;
                clearTimeout(xhrTimer);
            });
        }

        function open(callback) {
            this.DOM.fadeIn();
            if (!startCheckingTransaction.call(this)) {
                fetchData.apply(this);
            }
            if (callback && typeof callback === "function") {
                callback();
            }
        }

        function close(callback) {
            this.DOM.fadeOut();
            clearTimeout(xhrTimer);
            xhrTimer = undefined;
            terminateTransaction.apply(this);
            if (callback && typeof callback === "function") {
                callback();
            }
        }

        function terminateTransaction() {
            if (transId) {
                checkTransaction.apply(this, [resource.config.statusUrl + transId + '?cancelled=true']);
            }
        }

        return {
            plugins: {},
            DOM: null,
            events: {
                'INITIATED': null,
                'PENDING': null,
                'ERROR': null,
                'SUCCESS': null,
                'FAILURE': null,
                'FETCH_DATA_ERROR': null,
                'DATA_FETCHED': null,
            },
            id: options.config,
            initBuilder: initBuilder,
            open: open,
            close: close,
            refresh: refresh,
            require: (options.require && options.require.length > 0) ? options.require.concat(['sms', 'logo']) : ['sms', 'logo'],
            addPlugin: addPlugin,
            registerEvent: registerEvent,
            terminateTransaction: terminateTransaction
        }
    });

    return {
        init: function () {
            return api.init.apply(this, arguments)
        },
        setConfig: function () {
            api.setConfig.apply(this, arguments)
        },
        addConfig: function () {
            api.addConfig.apply(this, arguments)
        },
        addModule: function () {
            api.addModule.apply(this, arguments)
        }
    };
}));

thumbSignIn.addModule('sms', {
    config: {
        'smsContent': "",
        'smsTitle': "Or get link via SMS"
    },
    getElem: function (config, url) {
        return {
            elem: `<div class="tsmodal-footer"><p class="more-info more-info2" >${config.sms.smsTitle}</p><div class="more-info"><div class="d-box"><p id="sms_result" class="more-info"></p><div class="db-b"><input type="text" placeholder="123 456 7890" id="phone" maxlength="10"><img class="d-arrow-b" src="${url}/styles/img/icons-8-sent-filled.png"/></div></div></div></div>`,
            style: `.more-info{margin:0 auto;max-width:248px}.db-b img.d-arrow-b{position:absolute;right:50px;padding:10px 5px;cursor:pointer}`
        }
    }
}, function (dom, config) {
    this.DOM.find("#module-wrapper").append(dom);
    var _this = this;

    this.DOM.find('#sms_result').html("");
    this.DOM.find('#sms_result').hide();
    this.DOM.find('.more-info').show()
    this.DOM.find("#phone").intlTelInput({
        initialCountry: "auto",
        geoIpLookup: function (callback) {
            $.get('https://ipinfo.io', function () { }, "jsonp").always(function (resp) {
                var countryCode = (resp && resp.country) ? resp.country : "";
                callback(countryCode);
            });
        }
    });
    this.DOM.find('#phone').keyup(function (e) {
        var reg = new RegExp(/^\d+$/);
        if (false === reg.test(_this.DOM.find('#phone').val())) {
            _this.DOM.find('#phone').val('');
        }
        return;
    });
    this.DOM.find('#phone').keydown(function (e) {
        var key = String.fromCharCode(e.keyCode);
        var reg = new RegExp(/^\d+$/);
        return (reg.test(key) || e.keyCode == '8' || e.keyCode == '39' || e.keyCode == '46' || e.keyCode == '37');
    });

    // Call API for sending SMS
    this.DOM.find('.d-arrow-b').click(function () {
        var val = _this.DOM.find("#phone").intlTelInput("getNumber");
        if (val) {
            if (_this.DOM.find("#phone").val().length === 10) {

                var deeplinkUrl = _this.DOM.find('#qrcode').attr('data-deeplink-url');
                if (deeplinkUrl.length > 0) {
                    deeplinkUrl = 'Please download App from ' + deeplinkUrl;
                } else {
                    deeplinkUrl = 'Please download App';
                }

                var message = (config.sms.smsContent && config.sms.smsContent.length > 0) ? config.sms.smsContent : deeplinkUrl;

                axios.post('/sendSMS', {
                    to: val,
                    message: message
                }).then(function (response) {
                    _this.DOM.find('#sms_result').html("SMS sent to your mobile");
                    _this.DOM.find('#sms_result').show();
                    setTimeout(function () {
                        _this.DOM.find('#sms_result').hide();
                    }, 2000);
                }).catch(function (err) {
                    _this.DOM.find('#sms_result').html("Something wrong happened");
                    _this.DOM.find('#sms_result').show();
                    setTimeout(function () {
                        _this.DOM.find('#sms_result').hide();
                    }, 2000);
                    console.log('Error: ' + err);
                });
            } else {
                _this.DOM.find('#sms_result').html("Please provide 10 digit phone number");
                _this.DOM.find('#sms_result').show();
                setTimeout(function () {
                    _this.DOM.find('#sms_result').hide();
                }, 2000);
            }
        } else {
            _this.DOM.find('#sms_result').html("Please provide 10 digit phone number");
            _this.DOM.find('#sms_result').show();
            setTimeout(function () {
                _this.DOM.find('#sms_result').hide();
            }, 2000);
        }
    });
})


thumbSignIn.addModule('logo', {
    config: {
        'appIcon': 'https://thumbsignin.com/styles/img/icon_old.png',
        'playStoreIcon': 'https://thumbsignin.com/styles/img/playstore.png',
        'playStoreURL': 'https://play.google.com/store/apps/details?id=com.pramati.thumbsignin.app',
        'appStoreIcon': 'https://thumbsignin.com/styles/img/appstore.png',
        'appStoreURL': 'https://itunes.apple.com/us/app/thumbsignin/id1122364132'
    },
    getElem: function (config, url) {
        return {
            elem: '<div class="ts-logo-wrapper"><p>Don’t have the app yet? <span id="get_link_by_sms"> Get it now</span></p><div style="display: flex;"><p class="icons"><img src="' + config.logo.appIcon + '" height="30px" width="30px"/><a target="_blank" href="' + config.logo.appStoreURL + '"><img src="' + config.logo.appStoreIcon + '" height="30px" width="90px" style="padding-left:10px;"/></a><a target="_blank" href="' + config.logo.playStoreURL + '"><img src="' + config.logo.playStoreIcon + '" height="30px" width="90px" style="padding-left:10px;"/></a></p></div></div>',
            style: '.ts-logo-wrapper span{font-weight:500;font-family:Montserrat-Bold;color:#19b2e6}.ts-logo-wrapper p{font-family:Montserrat-Medium;font-size:12px;color:#1a1a39}'
        }
    }
}, function (dom) {
    this.DOM.find("#module-wrapper").append(dom);
})

thumbSignIn.addModule('tsModal', {
    config: {
        'modalTitle': 'Scan the QR Code',
    },
    getElem: function (config, url) {
        return {
            elem: `
            <button id="tsModalBtn">Open Modal</button>
            <div id="tsModal" class="modal">
            <div class="modal-content">
                <h4 class="text-center">${config.tsModal.modalTitle}</h4>
                <span class="close">&times;</span>
                <div class="modal-content-wrapper"></div>
            </div>
            </div>
            `,
            style: '#tsModal{display:none;position:fixed;z-index:1000;left:0;top:0;width:100%;height:100%;overflow:hidden;background-color:rgba(0,0,0,.4)}#tsModal .modal-content{background-color:#fefefe;margin:10% auto;padding:20px;box-shadow:0 3px 6px rgba(0,0,0,.16),0 3px 6px rgba(0,0,0,.23);border:1px solid #888;width:450px}#tsModal .close{color:#aaa;float:right;font-size:28px;font-weight:700}#tsModal .close:focus,#tsModal .close:hover{color:#000;text-decoration:none;cursor:pointer}'
        }
    }
}, function (dom) {
    var widgetObj = this;
    $('body').append(dom);
    var modal = $('#tsModal');
    modal.find('.modal-content-wrapper').empty().append(this.DOM.detach());
    $("#tsModalBtn").click(function () {
        modal.show();
        widgetObj.open();
    });
    $("#tsModal .close").click(function () {
        widgetObj.close();
        modal.hide();
    });

    window.onclick = function (event) {
        if (event.target == modal[0]) {
            widgetObj.close();
            modal.hide();
        }
    }
})

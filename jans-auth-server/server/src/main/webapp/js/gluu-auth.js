// Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
//
// Copyright (c) 2020, Janssen Project

'use strict';

/** Gluu Authentiocation API.
 */
var gluu_auth = {

	//--------------------------------------------------------------------------------
	// Utility methods
	//--------------------------------------------------------------------------------
	cloneObject: function(obj) {
		var clone = {};
		for (var i in obj) {
			if ((typeof (obj[i]) == "object") && (obj[i] != null)) {
				clone[i] = this.cloneObject(obj[i]);
			} else {
				clone[i] = obj[i];
			}
		}
	
		return clone;
	},

	updateObject: function(obj, updObj) {
		for (var i in updObj) {
			obj[i] = updObj[i];
		}
	
		return obj;
	},

	//--------------------------------------------------------------------------------
	// QR code
	//--------------------------------------------------------------------------------
	QR_CODE_DEFAULT_OPTIONS: {
		// render method: 'canvas', 'image' or 'div'
		render : 'canvas',
	
		// version range somewhere in 1 . 40
		minVersion : 1,
		maxVersion : 40,
	
		// error correction level: 'L', 'M', 'Q' or 'H'
		ecLevel : 'M',
	
		// offset in pixel if drawn onto existing canvas
		left : 0,
		top : 0,
	
		// size in pixel
		size : 400,
	
		// code color or image element
		fill : '#000',
	
		// background color or image element, null for transparent background
		background : null,
	
		// content
	    text: '',
	
		// corner radius relative to module width: 0.0 . 0.5
		radius : 0.3,
	
		// quiet zone in modules
		quiet : 1,
	
		// modes
		// 0: normal
		// 1: label strip
		// 2: label box
		// 3: image strip
		// 4: image box
		mode : 2,
	
		mSize : 0.1,
		mPosX : 0.5,
		mPosY : 0.5,
	
		label : 'Gluu Auth',
		fontname : 'sans',
		fontcolor : '#000',
	
		image : null
	},
	
	getQrCodeOptions:  function(request, custom_qr_options, label) {
		var options = this.cloneObject(this.QR_CODE_DEFAULT_OPTIONS);
		var options = this.updateObject(options, custom_qr_options);

		options.text = request;
		
		if ((typeof label !== 'undefined') && (label != null) && (label != '')) {
			options.label = label
		}
		
		return options;
	},
	
	renderQrCode: function(container, request, custom_qr_options, label) {
	    var options = this.getQrCodeOptions(request, custom_qr_options, label);
	    $(container).qrcode(options);
	},

	//--------------------------------------------------------------------------------
	// Progress bar
	//--------------------------------------------------------------------------------
	progress: {
		value: 0,
		maxValue: 4*60,
		container: null,
		timer: null
	},
	
	startProgressBar: function(container, timeout, callback) {
		this.progress.value = 0;
		this.progress.maxValue = 4 * timeout;
		this.progress.container = $(container);
	
		this.progress.container.progressbar({
			value : gluu_auth.progress.value,
			max : gluu_auth.progress.maxValue
		});

		function worker() {
			gluu_auth.progress.container.progressbar({
				value : ++gluu_auth.progress.value
			});
		
			if (gluu_auth.progress.value >= gluu_auth.progress.maxValue) {
				clearInterval(gluu_auth.progress.timer);
				gluu_auth.progress.timer = null;
				gluu_auth.progress.container = null;
				
				if (callback !== undefined) {
					callback.call(this, "timeout");
				}
			}
		};

		this.progress.timer = setInterval(worker, 1000 / 4);
	},

	//--------------------------------------------------------------------------------
	// Session status checker
	//--------------------------------------------------------------------------------
	checker: {
		stop: false,
		endTime: null,
		poolInterval: 5 * 1000,
		timeout: 10 * 1000
	},

	startSessionChecker : function(callback, timeout) {
		gluu_auth.checker.stop = false;
		gluu_auth.endTime = (new Date()).getTime() + timeout * 1000;

		(function worker() {
			$.ajax({
				url: '/jans-auth/restv1/session_status',
				cache: false,
				timeout: gluu_auth.checker.timeout,
				success: function(result, status, xhr) {
					if ((result.state == 'unknown') || ((result.state == 'unauthenticated') && ((result.custom_state == 'declined') || (result.custom_state == 'expired')))) {
						callCallback(callback, 'error');
					} else if ((result.state == 'authenticated') || ((result.state == 'unauthenticated') && (result.custom_state == 'approved'))) {
						callCallback(callback, 'success');
					}
				},
				error: function(xhr, status, error) {
					if(status == 'timeout')
					{
						callCallback(callback, 'timeout');
					}
					else
					{
						// Stop status checker on error
						callCallback(callback, 'error');
					}
				},
				complete: function(xhr, status) {
					if (gluu_auth.endTime < (new Date()).getTime()) {
						callCallback(callback, 'timeout');
					}
					// Schedule the next request when the current one's complete
					if (!gluu_auth.checker.stop) {
						setTimeout(worker, gluu_auth.checker.poolInterval);
					}
				}
			});
		})();
		
		function callCallback(callback, status) {
			gluu_auth.checker.stop = true
			callback.call(this, status);
		};
	},
};

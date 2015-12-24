// oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
//
// Copyright (c) 2015, Gluu

'use strict';

/** oxPush2 API.
 */
var oxpush2 = {
		
	//--------------------------------------------------------------------------------
	// Utility methods
	//--------------------------------------------------------------------------------
	cloneObject: function(obj) {
		var clone = {};
		for ( var i in obj) {
			if ((typeof (obj[i]) == "object") && (obj[i] != null)) {
				clone[i] = this.cloneObject(obj[i]);
			} else {
				clone[i] = obj[i];
			}
		}
	
		return clone;
	},

	//--------------------------------------------------------------------------------
	// QR code
	//--------------------------------------------------------------------------------
	QR_CODE_DEFAULT_OPTIONS: {
		// render method: 'canvas', 'image' or 'div'
		render : 'canvas',
	
		// version range somewhere in 1 .. 40
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
	
		// corner radius relative to module width: 0.0 .. 0.5
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
	
		label : 'oxPush2',
		fontname : 'sans',
		fontcolor : '#000',
	
		image : null
	},
	
	getQrCodeOptions:  function(request) {
		var options = this.cloneObject(this.QR_CODE_DEFAULT_OPTIONS);
		options.text = request;
		
		return options;
	},
	
	renderQrCode: function(container, request) {
		var options = this.getQrCodeOptions(request);
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
	
	startProgressBar: function(container, timeout) {
		this.progress.value = 0;
		this.progress.maxValue = 4 * timeout;
		this.progress.container = $(container);
	
		this.progress.container.progressbar({
			value : oxpush2.progress.value,
			max : oxpush2.progress.maxValue
		});

		function worker() {
			oxpush2.progress.container.progressbar({
				value : ++oxpush2.progress.value
			});
		
			if (oxpush2.progress.value >= oxpush2.progress.maxValue) {
				clearInterval(oxpush2.progress.timer);
				oxpush2.progress.timer = null;
				oxpush2.progress.container = null;
			}
		}

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
		oxpush2.checker.stop = false;
		oxpush2.endTime = (new Date()).getTime() + timeout * 1000;

		(function worker() {
			$.ajax({
				url: '/oxauth/seam/resource/restv1/oxauth/session_status',
				timeout: oxpush2.checker.timeout,
				success: function(result, status, xhr) {
					$('#result').html(result.state);
					if ((result.state == 'unknown') || ((result.state == 'unauthenticated') && (((result.auth_state == 'declined') || (result.auth_state == 'expired'))))) {
						callCallback(callback, 'error');
					} else if ((result.state == 'authenticated') || ((result.state == 'unauthenticated') && ((result.auth_state == 'approved')))) {
						callCallback(callback, 'success');
					}
				},
				error: function(xhr, status, error) {
					// Stop status checker on error
					callCallback(callback, 'error');
				},
				complete: function(xhr, status) {
					if (oxpush2.endTime < (new Date()).getTime()) {
						callCallback(callback, 'error');
					}
					// Schedule the next request when the current one's complete
					if (!oxpush2.checker.stop) {
						setTimeout(worker, oxpush2.checker.poolInterval);
					}
				}
			});
		})();
		
		function callCallback(callback, status) {
			oxpush2.checker.stop = true
			callback.call(status);
		}
	},
};

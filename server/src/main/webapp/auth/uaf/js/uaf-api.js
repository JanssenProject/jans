// Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
//
// Copyright (c) 2020, Janssen Project

'use strict';

/** UAF API.
 */
var uaf_api = {

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
		width : 300,
		height : 300
	},

	getQrCodeOptions:  function(custom_qr_options) {
		var options = this.cloneObject(this.QR_CODE_DEFAULT_OPTIONS);
		var options = this.updateObject(options, custom_qr_options);
		
		return options;
	},

	renderQrCode: function(container, qr_image, qr_options) {
	    var options = this.getQrCodeOptions(qr_options);

		$("<img>", {
			"src" : "data:image/png;base64," + qr_image,
			"width" : options.width + "px",
			"height" : options.height + "px"
		}).appendTo(container);
	},

	// --------------------------------------------------------------------------------
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
			value : uaf_api.progress.value,
			max : uaf_api.progress.maxValue
		});

		function worker() {
			uaf_api.progress.container.progressbar({
				value : ++uaf_api.progress.value
			});
		
			if (uaf_api.progress.value >= uaf_api.progress.maxValue) {
				clearInterval(uaf_api.progress.timer);
				uaf_api.progress.timer = null;
				uaf_api.progress.container = null;
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

	startStatusChecker : function(callback, server_uri, status_request, timeout) {
		uaf_api.checker.stop = false;
		uaf_api.endTime = (new Date()).getTime() + timeout * 1000;

		(function worker() {
			$.ajax({
				url: server_uri,
				type: "POST",
				timeout: uaf_api.checker.timeout,
				headers: {
					 "Accept": "application/json",
					 "Content-Type": "application/json; charset=UTF-8",
				},
				data: JSON.stringify(status_request),
				dataType: "json",
				success: function(result, status, xhr) {
					if (result.statusCode == 4000) {
						callCallback(callback, 'success', result.additionalInfo.authenticatorsResult[0].handle);
					}
				},
				error: function(xhr, status, error) {
					// Stop status checker on error
					callCallback(callback, 'error');
				},
				complete: function(xhr, status) {
					if (uaf_api.endTime < (new Date()).getTime()) {
						callCallback(callback, 'error');
					}
					// Schedule the next request when the current one's complete
					if (!uaf_api.checker.stop) {
						setTimeout(worker, uaf_api.checker.poolInterval);
					}
				}
			});
		})();
		
		function callCallback(callback, status, handle) {
			uaf_api.checker.stop = true;
			callback.call(this, status, handle);
		}
	},
};

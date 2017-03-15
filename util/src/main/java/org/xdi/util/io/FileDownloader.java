/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.util.io;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author: Yuriy Movchan Date: 11.20.2010
 */
public class FileDownloader {

	private static Logger log = LoggerFactory.getLogger(FileDownloader.class);

	public static enum ContentDisposition {
		INLINE, ATTACHEMENT, NONE
	};

    // DateFormats are not safe in multi-threaded env.
    public static SimpleDateFormat responseHeaderDateFormat() {
        return new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.ENGLISH);
    }

	public static void sendError(HttpServletResponse response) {
		sendError(response, null);
	}

	public static void sendError(HttpServletResponse response, String message) {
		try {
			// Send Not Found Error
			if (message == null) {
				response.sendError(404);
			} else {
				response.sendError(404, message);
			}
		} catch (IOException ex) {
			log.error("Error while sending page 404", ex);
		}
	}

	public static int writeOutput(DownloadWrapper downloadWrapper, ContentDisposition contentDisposition, HttpServletResponse response) throws IOException {
		return writeOutputImpl(downloadWrapper, contentDisposition, response);
	}

	@Deprecated
	public static int writeOutput(DownloadWrapper downloadWrapper, boolean inline, HttpServletResponse response) throws IOException {
		return writeOutputImpl(downloadWrapper, inline ? ContentDisposition.INLINE : ContentDisposition.ATTACHEMENT, response);
	}

	private static int writeOutputImpl(DownloadWrapper downloadWrapper, ContentDisposition contentDisposition, HttpServletResponse response) throws IOException {
		log.debug("Downloading File: fileName='{}',mimeType='{}', size='{}'",
				new Object[] { downloadWrapper.getName(), downloadWrapper.getContentType(), downloadWrapper.getContentLength() });

		if ((response == null) || !downloadWrapper.isReady()) {
			throw new IOException(String.format("Invalid OutputStream or FileDownloadWrapper '%s' specified", downloadWrapper));
		}

		if (!((contentDisposition == null) || (ContentDisposition.NONE == contentDisposition))) {
			StringBuilder contentDispositionValue = new StringBuilder();
			if (ContentDisposition.INLINE == contentDisposition) {
				contentDispositionValue.append("inline; ");
			} else if (ContentDisposition.ATTACHEMENT == contentDisposition) {
				contentDispositionValue.append("attachment; ");
			}
			contentDispositionValue.append("filename=\"").append(downloadWrapper.getName()).append('\"');
			response.setHeader("Content-Disposition", contentDispositionValue.toString());
		}

		response.setContentLength(downloadWrapper.getContentLength());
		response.setContentType(downloadWrapper.getContentType());
		response.setHeader("Last-Modified", responseHeaderDateFormat().format(downloadWrapper.getModificationDate()));

		log.debug("Writing file to output stream");
		int bytesTransfered = 0;
		try {
			OutputStream out = response.getOutputStream();
			try {
				bytesTransfered = IOUtils
						.copy(downloadWrapper.getStream(), out);
				out.flush();
			} finally {
				IOUtils.closeQuietly(out);
			}

			log.debug("Send " + bytesTransfered + " bytes from file");

			return bytesTransfered;
		} catch (IOException ex) {
			throw ex;
		}
	}

}

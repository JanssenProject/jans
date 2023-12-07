package io.jans.casa.plugins.sample;

import java.util.ArrayList;
import java.util.List;

import io.jans.casa.credential.BasicCredential;
import io.jans.casa.misc.Utils;
import io.jans.casa.service.ISessionContext;
import io.jans.casa.ui.UIUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.BindUtils;
import org.zkoss.bind.annotation.Init;
import org.zkoss.bind.annotation.NotifyChange;
import org.zkoss.json.JavaScriptValue;
import org.zkoss.util.Pair;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.au.out.AuInvoke;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Messagebox;

/**
 * @author madhumita
 *
 */

public class SampleVM {

	private Logger logger = LoggerFactory.getLogger(getClass());
	private static final int QR_SCAN_TIMEOUT = 60;
	@WireVariable
	private ISessionContext sessionContext;

	private List<BasicCredential> devices;
	private BasicCredential newDevice;
	private String activationCode; // this is the code that gets projected inside the QR code
	private boolean uiQRShown;

	private String editingId;

	public boolean isUiQRShown() {
		return uiQRShown;
	}

	public void setUiQRShown(boolean uiQRShown) {
		this.uiQRShown = uiQRShown;
	}

	public String getActivationCode() {
		return activationCode;
	}

	public void setActivationCode(String activationCode) {
		this.activationCode = activationCode;
	}

	public BasicCredential getNewDevice() {
		return newDevice;
	}

	public void setNewDevice(BasicCredential newDevice) {
		this.newDevice = newDevice;
	}

	public String getEditingId() {
		return editingId;
	}

	public void setEditingId(String editingId) {
		this.editingId = editingId;
	}

	public List<BasicCredential> getDevices() {
		return devices;
	}

	/**
	 * Initialization method for this ViewModel.
	 */
	@Init
	public void init() {
		sessionContext = Utils.managedBean(ISessionContext.class);
		// fetch devices from SampleService
		devices = SampleService.getInstance().getDevices(sessionContext.getLoggedUser().getUserName());

	}

	public void delete(BasicCredential device) {
		Pair<String, String> delMessages = getDeleteMessages(device.getNickName(), null);

		Messagebox.show(delMessages.getY(), delMessages.getX(), Messagebox.YES | Messagebox.NO,
				true ? Messagebox.EXCLAMATION : Messagebox.QUESTION, event -> {
					if (Messagebox.ON_YES.equals(event.getName())) {
						try {

							boolean result = SampleService.getInstance().deleteSampleDevice(
									sessionContext.getLoggedUser().getUserName(), device.getNickName());
							if (result == false) {
								UIUtils.showMessageUI(false);
							} else {
								devices.remove(device);
								// trigger refresh (this method is asynchronous...)
								BindUtils.postNotifyChange(SampleVM.this, "devices");
								UIUtils.showMessageUI(true);
							}

						} catch (Exception e) {
							UIUtils.showMessageUI(false);
							logger.error(e.getMessage(), e);
						}
					}
				});
	}

	Pair<String, String> getDeleteMessages(String nick, String extraMessage) {

		StringBuilder text = new StringBuilder();
		if (extraMessage != null) {
			text.append(extraMessage).append("\n\n");
		}
		text.append(Labels.getLabel("sample_del_confirm",
				new String[] { nick == null ? Labels.getLabel("general.no_named") : nick }));
		if (extraMessage != null) {
			text.append("\n");
		}

		return new Pair<>(Labels.getLabel("sample_del_title"), text.toString());
	}

	@NotifyChange({ "devices", "editingId", "newDevice" })
	public void update() {

		String newName = newDevice.getNickName();
		if (Utils.isNotEmpty(newName)) {
			// Find the index of the current device in the device list
			int i = Utils.firstTrue(devices, dev -> String.valueOf(dev.getNickName()).equalsIgnoreCase(editingId));
			BasicCredential dev = devices.get(i);

			// TODO:set the new name
			// dev.setNickName(newName);
			cancelUpdate(null); // This doesn't undo anything we already did (just controls UI aspects)

			try {
				boolean result = SampleService.getInstance()
						.updateSampleDevice(sessionContext.getLoggedUser().getUserName(), dev.getNickName(), newName);
				if (result == false) {
					UIUtils.showMessageUI(false);
				} else {
					UIUtils.showMessageUI(true);
					devices = SampleService.getInstance().getDevices(sessionContext.getLoggedUser().getUserName());
					// devices.remove(i);
					// devices.add( dev);
					// trigger for refresh
					BindUtils.postNotifyChange(SampleVM.this, "devices");
					UIUtils.showMessageUI(true);
				}
			} catch (Exception e) {
				UIUtils.showMessageUI(false);
				logger.error(e.getMessage(), e);
			}
		}

	}

	@NotifyChange({ "editingId", "newDevice" })
	public void cancelUpdate(Event event) {
		editingId = null;
		if (event != null && event.getName().equals(Events.ON_CLOSE)) {
            event.stopPropagation();
        }
	}

	public void showQR() {

		try {

			//TODO: compute the logic for the contents of the QR code
			activationCode = "1234";
			uiQRShown = true;
			BindUtils.postNotifyChange(this, "uiQRShown");

			// Passing screen width as max allowed size for QR code allows showing QRs even
			// in small mobile devices
			// TODO:remove hardcoding (screen width)
			JavaScriptValue jvalue = new JavaScriptValue(getFormattedQROptions(30));
			// Calls the startQR javascript function supplying suitable params
			Clients.response(new AuInvoke("startQR", activationCode, "", jvalue));
			Clients.scrollBy(0, 10);

		} catch (Exception e) {
			UIUtils.showMessageUI(false);
			logger.error(e.getMessage(), e);
		}

	}

	public String getFormattedQROptions(int maxWidth) {

		List<String> list = new ArrayList<>();
		int size = 20;// getQrSize();
		int ival = maxWidth > 0 ? Math.min(size, maxWidth - 30) : size;

		if (ival > 0) {
			list.add("size:" + ival);
		}

		double dval = 0.05; // getQrMSize();
		if (dval > 0) {
			list.add("mSize: " + dval);
		}

		return list.toString().replaceFirst("\\[", "{").replaceFirst("\\]", "}");

	}
	
	@NotifyChange({"newDevice", "editingId"})
    public void prepareForUpdate(BasicCredential dev) {
        //This will make the modal window to become visible
        editingId = String.valueOf(dev.getNickName());
        newDevice = new BasicCredential(dev.getNickName(),0);
        //newDevice.setNickName(dev.getNickName());
        
    }
}

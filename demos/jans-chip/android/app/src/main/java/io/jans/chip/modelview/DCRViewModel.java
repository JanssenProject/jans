package io.jans.chip.modelview;

import android.content.Context;

import androidx.lifecycle.ViewModel;

import io.jans.chip.modal.OIDCClient;
import io.jans.chip.repository.DCRRepository;
import io.jans.chip.services.SingleLiveEvent;

public class DCRViewModel extends ViewModel {
    DCRRepository dcrRepository;
    public DCRViewModel(Context context) {
        dcrRepository = DCRRepository.getInstance(context);
    }
    public SingleLiveEvent<OIDCClient> doDCR(String scopeText) {
        return dcrRepository.doDCR(scopeText);
    }
}

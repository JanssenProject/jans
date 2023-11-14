package io.jans.chip.modelview;

import android.content.Context;

import androidx.lifecycle.ViewModel;

import io.jans.chip.modal.OPConfiguration;
import io.jans.chip.repository.OPConfigurationRepository;
import io.jans.chip.modal.SingleLiveEvent;

public class OPConfigurationViewModel extends ViewModel {
    OPConfigurationRepository opConfigurationRepository;
    public OPConfigurationViewModel(Context context) {
        opConfigurationRepository = OPConfigurationRepository.getInstance(context);
    }
    public SingleLiveEvent<OPConfiguration> fetchOPConfiguration(String configurationUrl) {
        return opConfigurationRepository.fetchOPConfiguration(configurationUrl);
    }
}

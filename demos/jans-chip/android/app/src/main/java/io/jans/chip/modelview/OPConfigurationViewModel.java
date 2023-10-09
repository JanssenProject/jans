package io.jans.chip.modelview;

import android.content.Context;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import io.jans.chip.modal.OPConfiguration;
import io.jans.chip.repository.OPConfigurationRepository;

public class OPConfigurationViewModel extends ViewModel {
    OPConfigurationRepository opConfigurationRepository;
    public OPConfigurationViewModel(Context context) {
        opConfigurationRepository = OPConfigurationRepository.getInstance(context);
    }
    public MutableLiveData<OPConfiguration> fetchOPConfiguration(String configurationUrl) {
        return opConfigurationRepository.fetchOPConfiguration(configurationUrl);
    }
}

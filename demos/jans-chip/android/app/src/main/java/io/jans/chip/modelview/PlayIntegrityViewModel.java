package io.jans.chip.modelview;

import android.content.Context;

import androidx.lifecycle.ViewModel;

import io.jans.chip.modal.appIntegrity.AppIntegrityResponse;
import io.jans.chip.repository.PlayIntegrityRepository;
import io.jans.chip.services.SingleLiveEvent;

public class PlayIntegrityViewModel extends ViewModel {
    PlayIntegrityRepository playIntegrityRepository;

    public PlayIntegrityViewModel(Context context) {
        playIntegrityRepository = PlayIntegrityRepository.getInstance(context);
    }

    public SingleLiveEvent<AppIntegrityResponse> checkAppIntegrity() {
        return playIntegrityRepository.checkAppIntegrity();
    }
}

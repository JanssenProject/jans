package io.jans.chip.modelview;

import android.content.Context;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import io.jans.chip.modal.appIntegrity.AppIntegrityResponse;
import io.jans.chip.repository.PlayIntegrityRepository;
public class PlayIntegrityViewModel extends ViewModel {
    PlayIntegrityRepository playIntegrityRepository;

    public PlayIntegrityViewModel(Context context) {
        playIntegrityRepository = PlayIntegrityRepository.getInstance(context);
    }

    public MutableLiveData<AppIntegrityResponse> checkAppIntegrity() {
        return playIntegrityRepository.checkAppIntegrity();
    }
}

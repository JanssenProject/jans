package io.jans.chip.modelview;

import android.content.Context;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import io.jans.chip.modal.LogoutResponse;
import io.jans.chip.repository.LogoutRepository;

public class LogoutViewModel extends ViewModel {
    LogoutRepository logoutRepository;

    public LogoutViewModel(Context context) {
        logoutRepository = LogoutRepository.getInstance(context);
    }

    public MutableLiveData<LogoutResponse> logout() {
        return logoutRepository.logout();
    }
}

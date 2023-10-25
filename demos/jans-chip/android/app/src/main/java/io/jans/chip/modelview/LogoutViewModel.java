package io.jans.chip.modelview;

import android.content.Context;

import androidx.lifecycle.ViewModel;

import io.jans.chip.modal.LogoutResponse;
import io.jans.chip.repository.LogoutRepository;
import io.jans.chip.modal.SingleLiveEvent;

public class LogoutViewModel extends ViewModel {
    LogoutRepository logoutRepository;

    public LogoutViewModel(Context context) {
        logoutRepository = LogoutRepository.getInstance(context);
    }

    public SingleLiveEvent<LogoutResponse> logout() {
        return logoutRepository.logout();
    }
}

package io.jans.chip.modelview;

import android.content.Context;

import androidx.lifecycle.ViewModel;

import io.jans.chip.modal.LoginResponse;
import io.jans.chip.repository.LoginResponseRepository;
import io.jans.chip.modal.SingleLiveEvent;

public class LoginViewModel extends ViewModel {
    LoginResponseRepository loginResponseRepository;

    public LoginViewModel(Context context) {
        loginResponseRepository = LoginResponseRepository.getInstance(context);
    }

    public SingleLiveEvent<LoginResponse> processlogin(String usernameText, String passwordText) {
        return loginResponseRepository.processlogin(usernameText, passwordText);
    }
}

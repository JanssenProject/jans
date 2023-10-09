package io.jans.chip.modelview;

import android.content.Context;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import io.jans.chip.modal.LoginResponse;
import io.jans.chip.repository.LoginResponseRepository;
public class LoginViewModel extends ViewModel {
    LoginResponseRepository loginResponseRepository;

    public LoginViewModel(Context context) {
        loginResponseRepository = LoginResponseRepository.getInstance(context);
    }

    public MutableLiveData<LoginResponse> processlogin(String usernameText, String passwordText) {
        return loginResponseRepository.processlogin(usernameText, passwordText);
    }
}

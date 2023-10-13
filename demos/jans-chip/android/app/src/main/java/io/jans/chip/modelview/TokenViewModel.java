package io.jans.chip.modelview;

import android.content.Context;

import androidx.lifecycle.ViewModel;

import io.jans.chip.modal.TokenResponse;
import io.jans.chip.repository.TokenResponseRepository;
import io.jans.chip.services.SingleLiveEvent;

public class TokenViewModel extends ViewModel {
    TokenResponseRepository tokenResponseRepository;

    public TokenViewModel(Context context) {
        tokenResponseRepository = TokenResponseRepository.getInstance(context);
    }

    public SingleLiveEvent<TokenResponse> getToken(String authorizationCode, String usernameText, String passwordText) {
        return tokenResponseRepository.getToken(authorizationCode, usernameText, passwordText);
    }
}

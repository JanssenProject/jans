package io.jans.chip.modelview;

import android.content.Context;

import androidx.lifecycle.ViewModel;

import io.jans.chip.modal.UserInfoResponse;
import io.jans.chip.repository.UserInfoResponseRepository;
import io.jans.chip.modal.SingleLiveEvent;

public class UserInfoViewModel extends ViewModel {
    UserInfoResponseRepository userInfoResponseRepository;

    public UserInfoViewModel(Context context) {
        userInfoResponseRepository = UserInfoResponseRepository.getInstance(context);
    }

    public SingleLiveEvent<UserInfoResponse> getUserInfo(String accessToken, boolean silentOnError) {
        return userInfoResponseRepository.getUserInfo(accessToken, silentOnError);
    }

    public SingleLiveEvent<UserInfoResponse> getUserInfo(String accessToken) {
        return userInfoResponseRepository.getUserInfo(accessToken);
    }
}

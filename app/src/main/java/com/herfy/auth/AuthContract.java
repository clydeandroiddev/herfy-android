package com.herfy.auth;

import com.herfy.base.BasePresenter;
import com.herfy.base.BaseView;

public interface AuthContract {

    interface View extends BaseView<Presenter> {

        void signInEmail(String email, String password, int mobileFormatIndex);

        void forgotPassword(String phoneNumber, int mobileFormatIndex);

        void signInFacebook();

        void signInGoogle();

        void signUp(String fullName, String phoneNumber, String email, String password, int index);

        void showErrorEmail(String error);

        void showErrorPassword(String error);

        void showErrorUsername(String error);

        void showMessage(String message);

        void showError(String error);

        void showSuccess(String success);

        void showErrorFirstName(String error);

        void showErrorPhoneNumber(String error);

        void showGuest();

        void showPhoneNumber(String customerId, String email, String password);

        void setupCodeBroadcastListener();

        void showCode(boolean guest, String customerId, String email, String password, String phoneNumber);

        void showMain();

        void changeLanguage(String language);

        void logSigninEvent(String customerId);

        void logRegistrationEvent(String registrationMethod);

    }

    interface Presenter extends BasePresenter {

        void signIn(String email, String password, int mobileFormatIndex);

        void signInFacebook(String email, String token);

        void signInGoogle(String email, String token);

        void sendVerification(boolean guest, String customerId, String email, String password, String phoneNumber);

        void forgotPassword(String phoneNumber, int mobileFormatIndex);

        void getCustomer(String id);

        void getConsumerData();

        void getFavorites(int page);

        void signUp(String fullName, String phoneNumber, String email, String password, int index);

        void guest(String fullName, String phoneNumber, int mobileFormatIndex);

        void changeLanguage();

        String getRegistrationMethod();

    }
}

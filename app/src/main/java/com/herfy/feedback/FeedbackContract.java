package com.herfy.feedback;

import com.herfy.base.BasePresenter;
import com.herfy.base.BaseView;

import java.io.File;

public interface FeedbackContract {

    interface View extends BaseView<Presenter> {

        void showErrorFullName(String error);

        void showErrorEmailAddress(String error);

        void showErrorPhoneNumber(String error);

        void showErrorComment(String error);

        void showMessage(String message);

        void showError(String error);

        void showSuccess(String success);

    }

    interface Presenter extends BasePresenter {

        void post(String fullName, String emailAddress, String phoneNumber, String comment, File image);

    }

}

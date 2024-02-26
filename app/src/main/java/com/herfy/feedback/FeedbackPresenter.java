package com.herfy.feedback;

import com.herfy.utils.constants.Text;
import com.herfy.util.AppLogger;
import com.herfy.utils.analytics.MixpanelUtil;
import com.herfy.utils.handler.ErrorHandler;
import com.herfy.util.StringUtil;
import com.herfy.utils.helper.CacheUtil;
import com.skylinedynamics.solosdk.api.ApiRequestListener;
import com.skylinedynamics.solosdk.api.calls.FeedbackApiCall;

import java.io.File;
import java.util.HashMap;

public class FeedbackPresenter implements FeedbackContract.Presenter {

    private final FeedbackContract.View feedbackView;

    public FeedbackPresenter(FeedbackContract.View feedbackView) {
        this.feedbackView = feedbackView;
        this.feedbackView.setPresenter(this);
    }

    @Override
    public void start() {
        feedbackView.setupViews();
        feedbackView.setupFonts();
        feedbackView.setupTranslations();
    }

    @Override
    public void post(String fullName, String emailAddress, String phoneNumber, String comment, File image) {

        boolean valid = true;

        if(fullName.isEmpty()) {
            valid = false;
            feedbackView.showErrorFullName(CacheUtil.getInstance().getTranslations(Text.FULL_NAME_REQUIRED));
        } else if(!StringUtil.isValidName(fullName)) {
            valid = false;
            feedbackView.showErrorFullName(CacheUtil.getInstance().getTranslations(Text.NAME_VALIDATION, "Name fields should not accept numbers and special character."));
        } else {
            feedbackView.showErrorFullName("");
        }

        if(emailAddress.isEmpty()) {
            valid = false;
            feedbackView.showErrorEmailAddress(CacheUtil.getInstance().getTranslations(Text.EMAIL_ADDRESS_REQUIRED));
        } else if(!StringUtil.isValidEmail(emailAddress)) {
            valid = false;
            feedbackView.showErrorEmailAddress(CacheUtil.getInstance().getTranslations(Text.EMAIL_ADDRESS_INVALID));
        } else {
            feedbackView.showErrorEmailAddress("");
        }

        if(phoneNumber.isEmpty()) {
            valid = false;
            feedbackView.showErrorPhoneNumber(CacheUtil.getInstance().getTranslations(Text.PHONE_NUMBER_REQUIRED));
        } else if(!StringUtil.isValidPhoneNumber(phoneNumber)) {
            valid = false;
            feedbackView.showErrorPhoneNumber(CacheUtil.getInstance().getTranslations(Text.PHONE_NUMBER_INVALID));
        } else {
            feedbackView.showErrorPhoneNumber("");
        }

        if(comment.isEmpty()) {
            valid = false;
            feedbackView.showErrorComment(CacheUtil.getInstance().getTranslations(Text.COMMENT_REQUIRED));
        } else {
            feedbackView.showErrorComment("");
        }

        if(valid) {
            String subject = CacheUtil.getInstance().getConcept().getLabel() + ": Customer Feedback";
			FeedbackApiCall.getInstance().postFeedbackWithSubject(fullName, emailAddress, phoneNumber, subject, comment, image, new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {

                    MixpanelUtil.Companion.trackEvent(MixpanelUtil.EVENT_SEND_FEEDBACK);

                    HashMap<String, String> eventAttributes = new HashMap<>();
                    eventAttributes.put("Name", fullName);
                    eventAttributes.put("Email", emailAddress);
                    eventAttributes.put("Phone Number", phoneNumber);
                    eventAttributes.put("Subject", subject);
                    eventAttributes.put("Comment", comment);
                    feedbackView.logEvent("LeaveReview", eventAttributes, null, 0.0);

                    feedbackView.showSuccess(CacheUtil.getInstance().getTranslations(Text.FEEDBACK_SENT));
                }

                @Override
                public void onError(Object o) {
                    AppLogger.printRetrofitError(o);
                    feedbackView.showError(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.FEEDBACK_ERROR), null));
                }
            });
        }
    }
}

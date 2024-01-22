package com.herfy.language;

import com.herfy.base.BasePresenter;
import com.herfy.base.BaseView;

public interface LanguageContract {

    interface View extends BaseView<Presenter> {

        void selectArabic();

        void selectEnglish();

    }

    interface Presenter extends BasePresenter {

        void getLanguage();

        void changeLanguage();

    }

}

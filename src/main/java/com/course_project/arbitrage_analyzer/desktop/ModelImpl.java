package com.course_project.arbitrage_analyzer.desktop;

import com.course_project.arbitrage_analyzer.interfaces.ArbitrageModel;
import com.course_project.arbitrage_analyzer.interfaces.ArbitragePresenter;
import com.course_project.arbitrage_analyzer.model.SettingsContainer;

public class ModelImpl implements ArbitrageModel {

    ArbitragePresenter presenter;

    ModelImpl(ArbitragePresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void updateSettings(SettingsContainer settings) {

    }

    @Override
    public void cancelBackgroundTask() {

    }

    @Override
    public void startBackgroundTask() {

    }

    @Override
    public void onDestroy() {

    }
}

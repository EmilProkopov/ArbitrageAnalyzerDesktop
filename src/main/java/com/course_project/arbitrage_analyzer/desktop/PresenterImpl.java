package com.course_project.arbitrage_analyzer.desktop;

import com.course_project.arbitrage_analyzer.interfaces.ArbitrageModel;
import com.course_project.arbitrage_analyzer.interfaces.ArbitragePresenter;
import com.course_project.arbitrage_analyzer.interfaces.ArbitrageView;
import com.course_project.arbitrage_analyzer.model.OutputDataSet;
import com.course_project.arbitrage_analyzer.model.SettingsContainer;

public class PresenterImpl implements ArbitragePresenter {

    private ArbitrageView view;
    private ArbitrageModel model;

    PresenterImpl(ArbitrageView view) {
        this.view = view;
        this.model = new ModelImpl(this);
        this.model.startBackgroundTask();
    }

    @Override
    public void onDestroy() {
        model.onDestroy();
        model = null;
        view = null;
    }

    @Override
    public void onWorkerResult(OutputDataSet dataSet) {
        view.updateData(dataSet);
    }

    @Override
    public void showToast(String msg) {
        view.showToast(msg);
    }

    @Override
    public void onSettingsChanged(SettingsContainer settings) {
        model.updateSettings(settings);
    }

    @Override
    public void onPauseResumeClick() {}

    @Override
    public void onViewStop() {}

    @Override
    public void onViewRestart() {}
}

package com.course_project.arbitrage_analyzer.desktop;

import com.course_project.arbitrage_analyzer.interfaces.ArbitrageModel;
import com.course_project.arbitrage_analyzer.interfaces.ArbitragePresenter;
import com.course_project.arbitrage_analyzer.model.OutputDataSet;
import com.course_project.arbitrage_analyzer.model.SettingsContainer;
import com.course_project.arbitrage_analyzer.workers.ArbitrageWorker;

public class ModelImpl implements ArbitrageModel {

    ArbitragePresenter presenter;
    ArbitrageWorker worker = null;

    ModelImpl(ArbitragePresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void updateSettings(SettingsContainer settings) {
        worker.updateSettings(settings);
    }

    @Override
    public void cancelBackgroundTask() {
        if (worker != null) {
            worker.stopThread();
            worker = null;
        }
    }

    @Override
    public void startBackgroundTask() {
        cancelBackgroundTask();
        worker = new ArbitrageWorker(presenter);
        worker.start();
    }

    @Override
    public void onDestroy() {
        presenter = null;
        cancelBackgroundTask();
    }
}

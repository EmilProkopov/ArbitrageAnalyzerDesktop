package com.course_project.arbitrage_analyzer.workers;

import com.course_project.arbitrage_analyzer.interfaces.ArbitrageModel;
import com.course_project.arbitrage_analyzer.interfaces.ArbitragePresenter;
import com.course_project.arbitrage_analyzer.model.CompiledOrderBook;
import com.course_project.arbitrage_analyzer.model.OutputDataSet;
import com.course_project.arbitrage_analyzer.model.SettingsContainer;

public class ArbitrageWorker extends Thread {

    private boolean continueRunning = true;
    private ArbitragePresenter presenter;

    public ArbitrageWorker(ArbitragePresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void run() {

        int i = 1;
        while (continueRunning) {

        }
    }

    public void updateSettings(SettingsContainer settings) {

    }

    public void stopThread() {
        continueRunning = false;
    }
}

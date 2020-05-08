package com.course_project.arbitrage_analyzer.desktop;

import com.course_project.arbitrage_analyzer.interfaces.ArbitragePresenter;
import com.course_project.arbitrage_analyzer.interfaces.ArbitrageView;
import com.course_project.arbitrage_analyzer.model.OutputDataSet;

public class ViewImpl implements ArbitrageView {

    ArbitragePresenter presenter;

    public ViewImpl(String[] args) {
        presenter = new PresenterImpl(this);
    }


    @Override
    public void updateData(OutputDataSet dataSet) {

    }


    @Override
    public void updateProgressBar(Integer progress) {}

    @Override
    public void updateResumePauseView(boolean paused) {}

    @Override
    public void showToast(String msg) {}
}

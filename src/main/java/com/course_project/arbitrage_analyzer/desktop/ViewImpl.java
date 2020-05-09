package com.course_project.arbitrage_analyzer.desktop;

import com.course_project.arbitrage_analyzer.Log;
import com.course_project.arbitrage_analyzer.interfaces.ArbitragePresenter;
import com.course_project.arbitrage_analyzer.interfaces.ArbitrageView;
import com.course_project.arbitrage_analyzer.model.OutputDataSet;
import com.course_project.arbitrage_analyzer.model.SettingsContainer;
import com.course_project.arbitrage_analyzer.model.disbalance_minimization.minimizers.MinimizerType;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

public class ViewImpl implements ArbitrageView {

    private final static String logTag = "Presenter";
    private final String xmlHeader = "timestamp,minV,minAmount,minTime,minProfit,realV,realAmount,realProfit,AmountImbalance";

    private ArbitragePresenter presenter;
    private FileWriter outputFile;

    public ViewImpl(String[] args) {

        File file = new File(args[0]);
        try {
            //file.getParentFile().mkdirs();
            file.createNewFile();
        } catch(Exception e) {
            Log.e(logTag, "FileWriter error;");
        } finally {
            try {
                outputFile = new FileWriter(file, true);
            } catch (IOException e) {
                Log.e(logTag, "FileWriter error;");
            }
        }

        StringBuilder paramString = new StringBuilder();
        for (int i=0; i<args.length; ++i) {
            paramString.append(i).append(": ").append(args[i]).append(";  ");
        }
        write(paramString.append("\n\n").toString());
        write(xmlHeader + '\n');

        presenter = new PresenterImpl(this);
        SettingsContainer settings = readSettings(args);
        presenter.onSettingsChanged(settings);
    }


    @Override
    public void updateData(OutputDataSet dataSet) {

        long timestamp = System.currentTimeMillis();

        StringBuilder s = new StringBuilder();
        s.append(timestamp).append(",");
        s.append(dataSet.getMinimizerResult().getOptimalV()).append(",");
        s.append(dataSet.getMinimizerResult().getOptimalAmount()).append(",");
        s.append(dataSet.getMinimizerResult().getTime()).append(",");
        s.append(dataSet.getOptimalProfit()).append(",");
        s.append(dataSet.getEstimate().getUsedSecondCurrencyAmount()).append(",");
        s.append(dataSet.getRealFirstCurrencyAmount()).append(",");
        s.append(dataSet.getRealProfit()).append(",");
        s.append(dataSet.getEstimate().getFirstCurrencyDisbalance()).append(",");
        s.append('\n');
        write(s.toString());

        System.out.print(Long.toString(timestamp)+"\n");
    }


    @Override
    public void updateProgressBar(Integer progress) {}

    @Override
    public void updateResumePauseView(boolean paused) {}

    @Override
    public void showToast(String msg) {}


    private void write(String s) {
        try {
            outputFile.write(s);
            outputFile.flush();
        } catch(Exception e) {
            Log.e(logTag, "FileWriter error;" + "\n" + e.toString());
        }
    }


    private SettingsContainer readSettings(String[] args) {

        SettingsContainer settings = new SettingsContainer();

        settings.setUpdateRateSeconds(Integer.parseInt(args[1]));
        settings.setCurrencyPare(args[2]); // "BTC/USD"
        settings.setDepthLimit(Integer.parseInt(args[3]));
        settings.setBitfinex(Boolean.parseBoolean(args[4]));
        settings.setCex(Boolean.parseBoolean(args[5]));
        settings.setExmo(Boolean.parseBoolean(args[6]));
        settings.setGdax(Boolean.parseBoolean(args[7]));

        settings.setHistorySize(Short.parseShort(args[8]));
        settings.setRiskConst(Double.parseDouble(args[9]));
        settings.setNumberOfLaunches(Integer.parseInt(args[10]));
        settings.setMaxIterations(Integer.parseInt(args[11]));

        String minimizerTypeString = args[12];
        MinimizerType minType;
        switch (minimizerTypeString) {
            case ("BayesLaplace"):
                minType = MinimizerType.BayesLaplace;
                break;

            case ("Expected_regret"):
                minType = MinimizerType.ExpectedRegret;
                break;

            case ("Cyclic_coordinate_descent"):
                minType = MinimizerType.CyclicCoordinateDescent;
                break;

            case ("Pattern_search"):
                minType = MinimizerType.PatternSearch;
                break;

            case ("Nelder_Mead"):
                minType = MinimizerType.NelderMead;
                break;

            default:
                minType = MinimizerType.Simple;
        }
        settings.setMinimizerType(minType);

        return settings;
    }
}

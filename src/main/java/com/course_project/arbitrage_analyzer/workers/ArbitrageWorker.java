package com.course_project.arbitrage_analyzer.workers;

import com.course_project.arbitrage_analyzer.Log;
import com.course_project.arbitrage_analyzer.interfaces.ArbitragePresenter;
import com.course_project.arbitrage_analyzer.model.*;
import com.course_project.arbitrage_analyzer.model.disbalance_minimization.DisbalanceEstimator;
import com.course_project.arbitrage_analyzer.model.disbalance_minimization.EstimatorResult;
import com.course_project.arbitrage_analyzer.model.disbalance_minimization.MinimizerResult;
import com.course_project.arbitrage_analyzer.model.disbalance_minimization.minimizers.DisbalanceMinimizer;
import com.course_project.arbitrage_analyzer.model.disbalance_minimization.minimizers.MinimizerType;
import com.course_project.arbitrage_analyzer.model.disbalance_minimization.minimizers.SimpleMinimizer;
import com.course_project.arbitrage_analyzer.model.disbalance_minimization.minimizers.SurfaceMinimizers.CyclicCoordinateDescentMinimizer;
import com.course_project.arbitrage_analyzer.model.disbalance_minimization.minimizers.SurfaceMinimizers.NelderMeadMinimizer;
import com.course_project.arbitrage_analyzer.model.disbalance_minimization.minimizers.SurfaceMinimizers.PatternSearchMinimizer;
import com.course_project.arbitrage_analyzer.model.disbalance_minimization.minimizers.table_minimizers.BayesLaplaceMinimizer;
import com.course_project.arbitrage_analyzer.model.disbalance_minimization.minimizers.table_minimizers.ExpectedRegretMinimizer;
import com.course_project.arbitrage_analyzer.model.disbalance_minimization.minimizers.target_functions.SurfaceTargetFunction;
import com.course_project.arbitrage_analyzer.model.order_book_getters.OrderBookGetter;
import com.course_project.arbitrage_analyzer.model.order_book_getters.OrderBookGetterSync;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ArbitrageWorker extends Thread {

    private static final String logTag = "ArbitrageWorker";
    private boolean continueRunning = true;
    private ArbitragePresenter presenter;
    private SettingsContainer settings;
    private String firstCurrency;
    private String secondCurrency; //Second currency in the pair.
    private OrderBookGetter orderBookGetter;
    private MarketInfoGetter infoGetter;
    private DisbalanceMinimizer minimizer;
    private DisbalanceEstimator estimator;

    private boolean firstLoop = true;
    private long lastTRPSUpdateTime;
    private long trpsUpdatePeriod = 1800000;


    public ArbitrageWorker(ArbitragePresenter presenter) {
        Log.e(logTag, "WORKER CREATED");
        this.presenter = presenter;
        settings = new SettingsContainer();
        orderBookGetter = new OrderBookGetterSync((OrderBookGetter.OrderBookGetterProgressListener) presenter);
        infoGetter = new MarketInfoGetter();
        estimator = new DisbalanceEstimator();
        initializeMinimizer();
        lastTRPSUpdateTime = System.currentTimeMillis();
    }

    public void updateSettings(SettingsContainer newSettings) {
        this.settings = newSettings;
        firstCurrency = settings.getCurrencyPare().split("/")[0];
        secondCurrency = settings.getCurrencyPare().split("/")[1];
        initializeMinimizer();
    }


    private void initializeMinimizer() {

        firstLoop = true;

        MinimizerType mt = settings.getMinimizerType();

        if (mt.equals(MinimizerType.BayesLaplace)) {
            minimizer = new BayesLaplaceMinimizer(settings.getHistorySize()
                    , settings.getRiskConst());
        }
        else if (mt.equals(MinimizerType.ExpectedRegret)) {
            minimizer = new ExpectedRegretMinimizer(settings.getHistorySize()
                    , settings.getRiskConst());
        }
        else if (mt.equals(MinimizerType.CyclicCoordinateDescent)) {
            minimizer = new CyclicCoordinateDescentMinimizer((short) settings.getNumberOfLaunches()
                    , settings.getHistorySize()
                    , settings.getMaxIterations());

            SurfaceTargetFunction tf = new SurfaceTargetFunction(settings.getRiskConst()
                    , minimizer);

            minimizer.setTargetFunction(tf);
        }
        else if (mt.equals(MinimizerType.PatternSearch)) {
            minimizer = new PatternSearchMinimizer((short) settings.getNumberOfLaunches()
                    , settings.getHistorySize()
                    , settings.getMaxIterations());

            SurfaceTargetFunction tf = new SurfaceTargetFunction(settings.getRiskConst()
                    , minimizer);

            minimizer.setTargetFunction(tf);
        }
        else if (mt.equals(MinimizerType.NelderMead)) {
            minimizer = new NelderMeadMinimizer((short) settings.getNumberOfLaunches()
                    , settings.getHistorySize()
                    , settings.getMaxIterations());

            SurfaceTargetFunction tf = new SurfaceTargetFunction(settings.getRiskConst()
                    , minimizer);

            minimizer.setTargetFunction(tf);
        }

        else {
            minimizer = new SimpleMinimizer();
        }
    }


    private void updateTRPS() {
        firstLoop = false;
        double trps = infoGetter.getTradeRatePerSeoond(settings);
        minimizer.setTradeRatePerSecond(trps);
    }

    private OutputDataSet analyzeMarkets() {

        long curTime = System.currentTimeMillis();

        if (firstLoop || curTime - lastTRPSUpdateTime >= trpsUpdatePeriod) {
            updateTRPS();
            lastTRPSUpdateTime = curTime;
        }
        //Get orderBook with top orders from all markets.
        CompiledOrderBook orderBook = orderBookGetter.getCompiledOrderBook(settings, true);
        // CompiledOrderBook orderBook = genTestOB1();
        CompiledOrderBook orderBookCopy = orderBook.clone();

        MinimizerResult minResult = minimizer.getResult(orderBookCopy);
        CompiledOrderBook actualOrderBook = orderBookGetter.getCompiledOrderBook(settings, false);
        // CompiledOrderBook actualOrderBook = genTestOB1();
        EstimatorResult estimate = estimator.getEstimate(actualOrderBook, minResult.getResultOrderBook());

        return formOutputDataSet(orderBook, minResult, estimate);
    }

    private OutputDataSet formOutputDataSet(CompiledOrderBook orderBook, MinimizerResult minResult
            , EstimatorResult estimate) {

        double optimalV = minResult.getOptimalV();
        double realV = estimate.getUsedSecondCurrencyAmount();

        double profit = 0.0; //Profit that we can get.
        Double firstCurrencyAmount = 0.0;
        Double secondCurrencyAmount = 0.0;
        //Points of the plot.
        ArrayList<Double> profitPoints = new ArrayList<>();
        ArrayList<Double> amountPoints = new ArrayList<>();
        //List of deals to make.
        ArrayList<Deal> deals = new ArrayList<>(); //List of deals that should be made.
        double optimalProfit = 0.0;

        double realFirstCurrencyAmount = 0.0;
        double realProfit = 0.0;

        boolean optimalPointPassed = false;
        boolean realPointPassed = false;
        //iterators.
        int bx = 0, ax = 0;

        OutputDataSet outputDataSet = new OutputDataSet();

        while ((ax < orderBook.getAsks().size())
                && (bx < orderBook.getBids().size())
                //While we can make profit from the deal.
                && (orderBook.getBids().get(bx).getPrice() > orderBook.getAsks().get(ax).getPrice())) {


            Double bidAmount = orderBook.getBids().get(bx).getAmount(); //Amount of top bid.
            Double askAmount = orderBook.getAsks().get(ax).getAmount(); //Amount of top ask.
            if (bidAmount.equals(0.0)) {
                bx += 1;
                continue;
            }
            if (askAmount.equals(0.0)) {
                ax += 1;
                continue;
            }
            //Amount of currency to buy (sell).
            Double m = Math.min(bidAmount, askAmount);

            //Check if we have achieved the optimal point.
            if (secondCurrencyAmount + orderBook.getAsks().get(ax).getPrice() * m >= optimalV
                    && !optimalPointPassed) {

                optimalPointPassed = true;
                double deltaSecond = optimalV - secondCurrencyAmount;
                double deltaFirst = deltaSecond / orderBook.getAsks().get(ax).getPrice();
                optimalProfit = profit + (orderBook.getBids().get(bx).getPrice()
                        - orderBook.getAsks().get(ax).getPrice()) * deltaFirst;

                deals.add(new Deal(DealType.BUY, orderBook.getAsks().get(ax).getMarketName()
                        , deltaFirst, orderBook.getAsks().get(ax).getPrice()));
                deals.add(new Deal(DealType.SELL, orderBook.getBids().get(bx).getMarketName()
                        , deltaFirst, orderBook.getBids().get(bx).getPrice()));
            }

            if (secondCurrencyAmount + orderBook.getAsks().get(ax).getPrice() * m >= realV
                    && !realPointPassed) {

                realPointPassed = true;
                double deltaSecond = realV - secondCurrencyAmount;
                double deltaFirst = deltaSecond / orderBook.getAsks().get(ax).getPrice();
                realFirstCurrencyAmount = firstCurrencyAmount + deltaFirst;
                realProfit = profit + (orderBook.getBids().get(bx).getPrice()
                        - orderBook.getAsks().get(ax).getPrice()) * deltaFirst;
            }

            Double currentProfit = (orderBook.getBids().get(bx).getPrice()
                    - orderBook.getAsks().get(ax).getPrice()) * m;

            profit += currentProfit;
            secondCurrencyAmount += orderBook.getAsks().get(ax).getPrice() * m;
            firstCurrencyAmount += m;

            profitPoints.add(profit);
            amountPoints.add(secondCurrencyAmount);

            if (!optimalPointPassed) {
                deals.add(new Deal(DealType.BUY, orderBook.getAsks().get(ax).getMarketName()
                        , m, orderBook.getAsks().get(ax).getPrice()));
                deals.add(new Deal(DealType.SELL, orderBook.getBids().get(bx).getMarketName()
                        , m, orderBook.getBids().get(bx).getPrice()));
            }

            //Take into account that we have made a deal and top bid and ask are changed.
            Double oldBidAmount = orderBook.getBids().get(bx).getAmount();
            Double oldAskAmount = orderBook.getAsks().get(ax).getAmount();
            orderBook.getBids().get(bx).setAmount(oldBidAmount - m);
            orderBook.getAsks().get(ax).setAmount(oldAskAmount - m);
        }

        ArrayList<Double> bitAmountPoints = new ArrayList<>();
        ArrayList<Double> askAmountPoints = new ArrayList<>();
        ArrayList<Double> bidPricePoints = new ArrayList<>();
        ArrayList<Double> askPricePoints = new ArrayList<>();

        fillAskBidChartPoints(bitAmountPoints, askAmountPoints, bidPricePoints, askPricePoints,
                orderBook);

        //Put data into the resulting data set.
        outputDataSet.setProfit(profit);
        outputDataSet.setMinimizerResult(minResult);
        outputDataSet.setOptimalProfit(optimalProfit);

        outputDataSet.setRealFirstCurrencyAmount(realFirstCurrencyAmount);
        outputDataSet.setEstimate(estimate);
        outputDataSet.setRealProfit(realProfit);

        outputDataSet.setAmountPoints(amountPoints);
        outputDataSet.setProfitPoints(profitPoints);
        outputDataSet.setDeals(deals);
        outputDataSet.setFirstCurrency(firstCurrency);
        outputDataSet.setSecondCurrency(secondCurrency);

        outputDataSet.setFirstCurrencyAmount(firstCurrencyAmount);
        outputDataSet.setSecondCurrencyAmount(secondCurrencyAmount);

        outputDataSet.setBidAmountPoints(bitAmountPoints);
        outputDataSet.setAskAmountPoints(askAmountPoints);
        outputDataSet.setBidPricePoints(bidPricePoints);
        outputDataSet.setAskPricePoints(askPricePoints);

        //Unite buy and sell deals made on same market into one deal.
        outputDataSet.uniteDealsMadeOnSameMarkets();

        return outputDataSet;
    }


    private void fillAskBidChartPoints(List<Double> bidAmountPoints ,
                                       List<Double> askAmountPoints ,
                                       List<Double> bidPricePoints ,
                                       List<Double> askPricePoints ,
                                       CompiledOrderBook orderBook) {

        List<PriceAmountPair> asks = orderBook.getAsks();
        List<PriceAmountPair> bids = orderBook.getBids();

        double y = 0.0;

        for (int i = 0; i < asks.size(); ++i) {
            if (asks.get(i).getAmount() > 0) {
                y += asks.get(i).getAmount() * asks.get(i).getPrice();
                askAmountPoints.add(y);
                askPricePoints.add(asks.get(i).getPrice());
            }
        }

        y = 0.0;
        for (int i = 0; i < bids.size(); ++i) {
            if (bids.get(i).getAmount() > 0) {
                y += bids.get(i).getAmount() * bids.get(i).getPrice();
                bidAmountPoints.add(y);
                bidPricePoints.add(bids.get(i).getPrice());
            }
        }
    }





    @Override
    public void run() {

        while (continueRunning) {
            OutputDataSet outputDataSet = analyzeMarkets();
            //Display data.
            publishProgress(outputDataSet);
            //Wait before next data updating.
            try {
                TimeUnit.SECONDS.sleep(settings.getUpdateRateSeconds());
            } catch (InterruptedException e) {
                Log.e(logTag, e.getMessage());
            }
        }

        presenter = null;
        settings = null;
        orderBookGetter = null;
        infoGetter = null;
        minimizer = null;
        estimator = null;
    }


    public void stopThread() {
        continueRunning = false;
    }


    private void publishProgress(OutputDataSet dataSet) {

        Log.i(logTag, "SoloAsyncTask RUNNING");

        if (dataSet.getDeals().size() <= 1) {
            presenter.showToast("No profit can be made.\nCheck Internet connection\n" +
                    "and number of active markets.");
        } else {
            presenter.onWorkerResult(dataSet);
        }
    }
}

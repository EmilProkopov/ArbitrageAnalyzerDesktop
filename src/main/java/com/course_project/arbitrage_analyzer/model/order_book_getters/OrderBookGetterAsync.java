package com.course_project.arbitrage_analyzer.model.order_book_getters;

import com.course_project.arbitrage_analyzer.Log;
import com.course_project.arbitrage_analyzer.model.CompiledOrderBook;
import com.course_project.arbitrage_analyzer.model.MarketName;
import com.course_project.arbitrage_analyzer.model.SettingsContainer;

import java.util.concurrent.TimeUnit;

public class OrderBookGetterAsync extends OrderBookGetter {

    private final static String logtag = "OBGetterAsync";
    private int exchangeCount, processedExchangeCount;
    private final long maxWaitTimeMS = 60000;
    private boolean showProgress;
    SingleMarketOBGetter bitfinexGetter, cexGetter, exmoGetter, gdaxGetter;
    CompiledOrderBook resultOB;

    public OrderBookGetterAsync(OrderBookGetter.OrderBookGetterProgressListener listener) {
        super(listener);
    }

    @Override
    public CompiledOrderBook getCompiledOrderBook(SettingsContainer settings, boolean showProgress) {

        this.showProgress = showProgress;
        boolean bitfinex = settings.getBitfinex();
        boolean cex = settings.getCex();
        boolean exmo = settings.getExmo();
        boolean gdax = settings.getGdax();
        int limit = settings.getDepthLimit();
        String currencyPair = settings.getCurrencyPare();


        exchangeCount = 0;
        if (bitfinex) exchangeCount++;
        if (cex) exchangeCount++;
        if (exmo) exchangeCount++;
        if (gdax) exchangeCount++;

        if (exchangeCount == 0) {
            return new CompiledOrderBook();
        }

        processedExchangeCount = 0;

        resultOB = new CompiledOrderBook();

        if (bitfinex) {
            bitfinexGetter = new SingleMarketOBGetter(this, MarketName.Bitfinex, limit, currencyPair);
            bitfinexGetter.start();
        }
        if (cex) {
            cexGetter = new SingleMarketOBGetter(this, MarketName.Cex, limit, currencyPair);
            cexGetter.start();
        }
        if (exmo) {
            exmoGetter = new SingleMarketOBGetter(this, MarketName.Exmo, limit, currencyPair);
            exmoGetter.start();
        }
        if (gdax) {
            gdaxGetter = new SingleMarketOBGetter(this, MarketName.Gdax, limit, currencyPair);
            gdaxGetter.start();
        }

        long startTime = System.currentTimeMillis();

        while ((processedExchangeCount < exchangeCount) && (System.currentTimeMillis()-startTime < maxWaitTimeMS)) {
            Log.e(logtag, "WAITING");
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        resultOB.sort();

        if (showProgress) {
            progressListener.onUpdateOrderBookGetterProgress(0);
        }

        Log.e(logtag, "GOT ALL");

        return resultOB;
    }

    synchronized public void onMarketOBGet(CompiledOrderBook ob, MarketName mn) {
        resultOB.addAll(ob);
        processedExchangeCount++;
        updateListener(showProgress, processedExchangeCount, exchangeCount);

        if (mn == MarketName.Bitfinex) {
            bitfinexGetter = null;
        } else if (mn == MarketName.Cex) {
            cexGetter = null;
        } else if (mn == MarketName.Exmo) {
            exmoGetter = null;
        } else {
            gdaxGetter = null;
        }
        Log.e(logtag, "Finish "+mn.toString());
    }
}

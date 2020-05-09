package com.course_project.arbitrage_analyzer.model.order_book_getters;

import com.course_project.arbitrage_analyzer.Log;
import com.course_project.arbitrage_analyzer.model.MarketName;

public class SingleMarketOBGetter extends Thread {

    private static final String logTag = "SingleMarketObGetter";
    private OrderBookGetterAsync getter;
    private MarketName mn;
    int limit;
    String currencyPair;

    SingleMarketOBGetter(OrderBookGetterAsync getter, MarketName mn, int limit, String currencyPair) {
        this.getter = getter;
        this.mn = mn;
        this.limit = limit;
        this.currencyPair = currencyPair;
    }

    @Override
    public void run() {
        Log.e(logTag, "Start "+mn.toString());
        if (mn == MarketName.Bitfinex) {
            getter.onMarketOBGet(getter.getBitfinexCleanOrderBook(limit, currencyPair), mn);
        }
        if (mn == MarketName.Cex) {
            getter.onMarketOBGet(getter.getCexPartCleanOrderBook(limit, currencyPair), mn);
        }
        if (mn == MarketName.Exmo) {
            getter.onMarketOBGet(getter.getExmoCleanOrderBook(limit, currencyPair), mn);
        }
        if (mn == MarketName.Gdax) {
            getter.onMarketOBGet(getter.getGdaxTop50CleanOrderBook(limit, currencyPair), mn);
        }

        getter = null;
        mn = null;
        currencyPair = null;
    }
}

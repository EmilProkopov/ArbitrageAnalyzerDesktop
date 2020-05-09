package com.course_project.arbitrage_analyzer.model.order_book_getters;

import com.course_project.arbitrage_analyzer.model.CompiledOrderBook;
import com.course_project.arbitrage_analyzer.model.SettingsContainer;

public class OrderBookGetterAsync extends OrderBookGetter {

    public OrderBookGetterAsync(OrderBookGetter.OrderBookGetterProgressListener listener) {
        super(listener);
    }

    @Override
    public CompiledOrderBook getCompiledOrderBook(SettingsContainer settings, boolean showProgress) {

        boolean bitfinex = settings.getBitfinex();
        boolean cex = settings.getCex();
        boolean exmo = settings.getExmo();
        boolean gdax = settings.getGdax();
        int limit = settings.getDepthLimit();
        String currencyPair = settings.getCurrencyPare();


        int exchangeCount = 0;
        if (bitfinex) exchangeCount++;
        if (cex) exchangeCount++;
        if (exmo) exchangeCount++;
        if (gdax) exchangeCount++;

        if (exchangeCount == 0) {
            return new CompiledOrderBook();
        }

        int processedExchangeCount = 0;

        CompiledOrderBook result = new CompiledOrderBook();

        if (bitfinex) {
            processedExchangeCount++;
            updateListener(showProgress, processedExchangeCount, exchangeCount);
            //Add all it's orders into the order book.
            result.addAll(getBitfinexCleanOrderBook(limit, currencyPair));
        }
        if (cex) {
            processedExchangeCount++;
            updateListener(showProgress, processedExchangeCount, exchangeCount);
            result.addAll(getCexPartCleanOrderBook(limit, currencyPair));
        }
        if (exmo) {
            processedExchangeCount++;
            updateListener(showProgress, processedExchangeCount, exchangeCount);
            result.addAll(getExmoCleanOrderBook(limit, currencyPair));
        }
        if (gdax) {
            processedExchangeCount++;
            updateListener(showProgress, processedExchangeCount, exchangeCount);
            result.addAll(getGdaxTop50CleanOrderBook(limit, currencyPair));
        }

        result.sort();

        if (showProgress) {
            progressListener.onUpdateOrderBookGetterProgress(0);
        }

        return result;
    }
}

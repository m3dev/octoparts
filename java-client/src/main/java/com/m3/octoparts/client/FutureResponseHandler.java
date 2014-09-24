package com.m3.octoparts.client;

import com.m3.octoparts.model.config.json.HttpPartConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public enum FutureResponseHandler {
    ;

    private static final Logger Log = LoggerFactory.getLogger(FutureResponseHandler.class);

    private static <T> T awaitResult(@Nonnull Future<T> future, @Nonnegative long timeoutMs, @Nonnull T defaultValue) {

        try {
            T result = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            if (result != null) {
                return result;
            }
        } catch (ExecutionException ee) {
            Log.warn("API call failed", ee.getCause());
        } catch (TimeoutException te) {
            Log.warn("API call timed out after " + timeoutMs + "ms");
            future.cancel(true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return defaultValue;
    }

    @Nonnull
    public static ResponseWrapper awaitResponse(@Nonnull Future<ResponseWrapper> future, @Nonnegative long timeoutMs) {
        return awaitResult(future, timeoutMs, EmptyResponseWrapper$.MODULE$);
    }


    @Nonnull
    public static List<HttpPartConfig> awaitListEndpoints(@Nonnull Future<List<HttpPartConfig>> future, @Nonnegative long timeoutMs) {
        return awaitResult(future, timeoutMs, Collections.<HttpPartConfig>emptyList());
    }

    @Nonnull
    public static Boolean awaitCacheInvalidationResponse(@Nonnull Future<Boolean> future, @Nonnegative long timeoutMs) {
        return awaitResult(future, timeoutMs, Boolean.FALSE);
    }
}

package com.m3.octoparts.client;

import com.m3.octoparts.model.Cookie;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public interface PartResponseWrapper {
    @Nullable
    String getContents(@Nullable String defaultContents);

    @Nonnull
    List<Cookie> getCookies();

    @Nonnull
    List<String> getErrors();

    @Nullable
    String getPartId();

    @Nullable
    Integer getStatusCode();

    @Nullable
    Boolean isRetrievedFromCache();
}
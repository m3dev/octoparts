package com.m3.octoparts.client;

import com.m3.octoparts.model.ResponseMeta;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ResponseWrapper {
    @Nonnull
    PartResponseWrapper getPartResponse(@Nonnull String partName);

    @Nonnull
    Iterable<PartResponseWrapper> getPartResponses();

    @Nullable
    ResponseMeta getResponseMeta();

    @Nullable
    String getMetaId();
}
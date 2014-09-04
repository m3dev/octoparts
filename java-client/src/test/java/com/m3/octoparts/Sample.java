package com.m3.octoparts;

import com.m3.octoparts.client.FutureResponseHandler;
import com.m3.octoparts.client.OctopartsApiBuilder;
import com.m3.octoparts.client.RequestBuilder;
import com.m3.octoparts.client.ResponseWrapper;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Future;

public class Sample implements Closeable {
    private final OctopartsApiBuilder apiBuilder = new OctopartsApiBuilder("http://octoparts/", "m3.com");

    public void makeACall() throws IOException {
        RequestBuilder request = apiBuilder.newRequest("123", "cafebabe", null, "/index.jsp", null);
        request.newPart("part1", null).addParam("q", "lookForThis").build();
        request.newPart("part2", null).addParam("b", "c").addParam("d", "e").build();

        Future<ResponseWrapper> future = apiBuilder.submit(request.build());
        ResponseWrapper responseWrapper = FutureResponseHandler.awaitResponse(future, 1000L);
        String part1 = responseWrapper.getPartResponse("part1").getContents("<script>alert('an error happened!');</script>");
        String part2 = responseWrapper.getPartResponse("part2").getContents("");
    }

    @Override
    public void close() throws IOException {
        apiBuilder.close();
    }
}

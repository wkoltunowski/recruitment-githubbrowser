package com.falco.recruitment.githubbrowser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;
import java.util.List;

import static java.util.Arrays.asList;

@Slf4j
public class ErrorStatusInResponseErrorHandler implements ResponseErrorHandler {
    private static final List<HttpStatus.Series> ERROR_SERIES = asList(
            HttpStatus.Series.CLIENT_ERROR,
            HttpStatus.Series.SERVER_ERROR);

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        LOG.error("Response error: {} {}", response.getStatusCode(), response.getStatusText());
    }

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        return ERROR_SERIES.contains(response.getStatusCode().series());
    }
}

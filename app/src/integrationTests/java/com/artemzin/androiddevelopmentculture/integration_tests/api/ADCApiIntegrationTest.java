package com.artemzin.androiddevelopmentculture.integration_tests.api;

import android.support.annotation.NonNull;

import com.artemzin.androiddevelopmentculture.ADCRobolectricTestRunner;
import com.artemzin.androiddevelopmentculture.api.ADCApi;
import com.artemzin.androiddevelopmentculture.api.entities.Item;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.List;

import retrofit.HttpException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@RunWith(ADCRobolectricTestRunner.class)
public class ADCApiIntegrationTest {

    @SuppressWarnings("NullableProblems") // Initialized in @Before.
    @NonNull
    private MockWebServer mockWebServer;

    @SuppressWarnings("NullableProblems") // Initialized in @Before.
    @NonNull
    private ADCApi adcApi;

    @Before
    public void beforeEachTest() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // Change base url to the mocked
        ADCRobolectricTestRunner.adcApp().applicationComponent().changeableBaseUrl().setBaseUrl(mockWebServer.url("").toString());

        adcApi = ADCRobolectricTestRunner.adcApp().applicationComponent().adcApi();
    }

    @After
    public void afterEachTest() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    public void items_shouldHandleCorrectResponse() {
        mockWebServer.enqueue(new MockResponse().setBody("["
                + "{ \"id\": \"test_id_1\", \"title\": \"Test title 1\"},"
                + "{ \"id\": \"test_id_2\", \"title\": \"Test title 2\"},"
                + "{ \"id\": \"test_id_3\", \"title\": \"Test title 3\"}"
                + "]"));

        // Get items from the API
        List<Item> items = adcApi.items().toBlocking().value();

        assertThat(items).hasSize(3);

        assertThat(items.get(0).id()).isEqualTo("test_id_1");
        assertThat(items.get(0).title()).isEqualTo("Test title 1");

        assertThat(items.get(1).id()).isEqualTo("test_id_2");
        assertThat(items.get(1).title()).isEqualTo("Test title 2");

        assertThat(items.get(2).id()).isEqualTo("test_id_3");
        assertThat(items.get(2).title()).isEqualTo("Test title 3");
    }

    // Such tests assert that no matter how we implement our REST api:
    // Retrofit or not
    // OkHttp or not
    // It should handle error responses too.
    @Test
    public void items_shouldThrowExceptionIfWebServerRespondError() {
        for (Integer errorCode : HttpCodes.clientAndServerSideErrorCodes()) {
            mockWebServer.enqueue(new MockResponse().setStatus("HTTP/1.1 " + errorCode + " Not today"));

            try {
                adcApi.items().toBlocking().value();
                fail("HttpException should be thrown for error code: " + errorCode);
            } catch (RuntimeException expected) {
                HttpException httpException = (HttpException) expected.getCause();
                assertThat(httpException.code()).isEqualTo(errorCode);
                assertThat(httpException.message()).isEqualTo("Not today");
            }
        }
    }
}

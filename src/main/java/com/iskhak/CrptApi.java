package com.iskhak;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.IOException;
import java.util.Date;

@Data
public class CrptApi {
    private final TimeUnit timeUnit;
    private final int requestLimit;
    private final AtomicInteger requestCounter = new AtomicInteger(0);
    private Date lastResetTime = new Date();
    private final String URI = "https://ismp.crpt.ru/api/v3/lk/documents/create";

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
    }

    public void createDocument(Object document, String signature)
            throws IOException, InterruptedException {

        synchronized (this) {
            long currentTime = System.currentTimeMillis();
            long timePassed = currentTime - lastResetTime.getTime();
            if (timePassed >= timeUnit.toMillis(1)) {
                requestCounter.set(0);
                lastResetTime = new Date(currentTime);
            }

            while (requestCounter.get() >= requestLimit) {
                wait(timeUnit.toMillis(1) - timePassed);
                currentTime = System.currentTimeMillis();
                timePassed = currentTime - lastResetTime.getTime();

                if (timePassed >= timeUnit.toMillis(1)) {
                    requestCounter.set(0);
                    lastResetTime = new Date(currentTime);
                }
            }

            HttpClient httpClient = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost(URI);

            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writeValueAsString(document);

            StringEntity entity = new StringEntity(json);
            httpPost.setEntity(entity);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Signature", signature);

            CloseableHttpResponse response = (CloseableHttpResponse) httpClient.execute(httpPost);
            HttpEntity responseEntity = response.getEntity();
            String responseString = EntityUtils.toString(responseEntity, "UTF-8");

            requestCounter.incrementAndGet();
            System.out.println("Document creation response: " + responseString);
        }
    }

    @Data
    public static class Description {
        private String participantInn;
    }
    @Data
    public static class Product {
        private String certificate_document;
        private String certificate_document_date;
        private String certificate_document_number;
        private String owner_inn;
        private String producer_inn;
        private String production_date;
        private String tnved_code;
        private String uit_code;
        private String uitu_code;
    }
    @Data
    public static class Root {
        private Description description;
        private String doc_id;
        private String doc_status;
        private String doc_type;
        private boolean importRequest;
        private String owner_inn;
        private String participant_inn;
        private String producer_inn;
        private String production_date;
        private String production_type;
        private ArrayList<Product> products;
        private String reg_date;
        private String reg_number;

    }

    public static void main(String[] args)
            throws IOException, InterruptedException {

        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 5);

        Object document = new Root();
        String signature = "signature";

        crptApi.createDocument(document, signature);
    }
}

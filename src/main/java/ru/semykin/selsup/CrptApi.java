package ru.semykin.selsup;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import okhttp3.*;
import org.apache.commons.lang3.concurrent.TimedSemaphore;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class CrptApi {

    private final TimedSemaphore semaphore;
    private final DocumentService documentService;
    private final HttpService httpService;
    private final int period = 1;

    public CrptApi(final TimeUnit timeUnit, final int maxValue) {
        this.documentService = new DocumentService();
        this.httpService = new HttpService();
        semaphore = new TimedSemaphore(period, timeUnit, maxValue);
    }

    public void sendDocumentFromFile(final String path, final String token, final String url) {
        if (semaphore.tryAcquire()) {
            final DocumentDto documentDto = documentService.setDocumentFromFile(path);
            httpService.createDocumentFromDto(documentDto, token, url);
        }
    }

    private static class DocumentService {
        private DocumentDto setDocumentFromFile(final String path) {
            final ObjectMapper objectMapper = new ObjectMapper();
            DocumentDto documentDto = new DocumentDto();
            try {
                documentDto = objectMapper.readValue(new File(path), DocumentDto.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return documentDto;
        }
    }

    private static class HttpService {

        private final OkHttpClient client = new OkHttpClient();
        private final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json");

        private String createDocumentFromDto(final DocumentDto documentDto, final String token, final String url) {
            final ObjectMapper objectMapper = new ObjectMapper();
            String body = "";
            try {
                body = objectMapper.writeValueAsString(documentDto);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            final Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer " + token)
                    .post(RequestBody.create(body, MEDIA_TYPE_JSON))
                    .build();
            String result = "";
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Запрос не прошел: " + response);
                }
                result = Objects.requireNonNull(response.body()).string();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }
    }

    @Getter
    @Setter
    private static class DescriptionDto {
        @JsonProperty("participantInn")
        private String participantInn;
    }

    @Getter
    @Setter
    private static class DocumentDto {
        @JsonProperty("description")
        private DescriptionDto description;
        @JsonProperty("doc_id")
        private String docId;
        @JsonProperty("doc_status")
        private String docStatus;
        @JsonProperty("doc_type")
        private String docType;
        @JsonProperty("importRequest")
        private boolean importRequest;
        @JsonProperty("owner_inn")
        private String ownerInn;
        @JsonProperty("participant_inn")
        private String participantInn;
        @JsonProperty("producer_inn")
        private String producerInn;
        @JsonProperty("production_date")
        private String productionDate;
        @JsonProperty("production_type")
        private String productionType;
        @JsonProperty("products")
        private ArrayList<ProductDto> products;
        @JsonProperty("reg_date")
        private String regDate;
        @JsonProperty("reg_number")
        private String regNumber;
    }

    @Getter
    @Setter
    private static class ProductDto {
        @JsonProperty("certificate_document")
        private String certificateDocument;
        @JsonProperty("certificate_document_date")
        private String certificateDocumentDate;
        @JsonProperty("certificate_document_number")
        private String certificateDocumentNumber;
        @JsonProperty("owner_inn")
        private String ownerInn;
        @JsonProperty("producer_inn")
        private String producerInn;
        @JsonProperty("production_date")
        private String productionDate;
        @JsonProperty("tnved_code")
        private String tnvedCode;
        @JsonProperty("uit_code")
        private String uitCode;
        @JsonProperty("uitu_code")
        private String uituCode;
    }
}

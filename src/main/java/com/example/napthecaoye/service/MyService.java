package com.example.napthecaoye.service;

import com.example.napthecaoye.model.threquest;
import com.fasterxml.jackson.databind.JsonNode;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.SslProvider;
import reactor.netty.transport.ProxyProvider;

import javax.annotation.PostConstruct;
import javax.net.ssl.SSLException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class MyService {

    private WebClient webClient;
    private threquest request;
    public List<String> cookies;
    public String requestVerificationToken;
    private final List<String[]> proxyList = new ArrayList<>();
    AtomicInteger currentProxyIndex = new AtomicInteger(0);
    private final AtomicInteger requestCount = new AtomicInteger(0);
    private static final int REQUEST_LIMIT = 2;
    @PostConstruct
    private void init() {
        loadProxies("fileproxy.txt");
        configureWebClient();
    }
    private void loadProxies(String filename) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                proxyList.add(line.split(":"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void configureWebClient() {
        String[] currentProxy = proxyList.get(currentProxyIndex.get());
        HttpClient httpClient = HttpClient.create()
                // Cấu hình SSL với mặc định (kiểm tra chứng chỉ hợp lệ)
                .secure(sslContextSpec -> {
                    try {
                        sslContextSpec.sslContext(SslContextBuilder.forClient().build()); // Dùng cấu hình SSL mặc định
                    } catch (SSLException e) {
                        throw new RuntimeException(e);
                    }
                })
                .proxy(proxy -> proxy.type(ProxyProvider.Proxy.HTTP)
                        .address(new InetSocketAddress(currentProxy[0], Integer.parseInt(currentProxy[1])))
                        .username(currentProxy[2])
                        .password(pass -> currentProxy[3]));

        webClient = WebClient.builder()
                .baseUrl("https://quatangyogurt.thmilk.vn")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
    @Autowired
    public MyService(WebClient.Builder webClientBuilder) {
        webClient = webClientBuilder.baseUrl("https://quatangyogurt.thmilk.vn").build();
        ResponseEntity<String> response = webClient.get()
                .uri("/")
                .retrieve()
                .toEntity(String.class)
                .block();
        cookies = response.getHeaders().get("Set-Cookie");
        String htmlContent = response.getBody();
        Document document = Jsoup.parse(htmlContent);
        Element tokenInput = document.selectFirst("input[name=__RequestVerificationToken]");
        requestVerificationToken = tokenInput.attr("value");
    }

    private void switchProxyIfNeeded() {
        if (requestCount.incrementAndGet() >= REQUEST_LIMIT) {
            currentProxyIndex.updateAndGet(i -> (i + 1) % proxyList.size());
            requestCount.set(0);
            configureWebClient();
        }
    }

    public String sendPostRequest(threquest requestObject) throws InterruptedException {
        switchProxyIfNeeded();
        request= requestObject;
        String value = "Name="+requestObject.getName()+ "&" + "Phone=0" + requestObject.getPhone()+"&ProvinceCode=01"+"&Code=" + requestObject.getCode() ;
        String firstCookie = cookies.get(0).split(";")[0];
        try {

            String response = webClient.post()
                    .uri("/Home/IndexAjax")
                    .header("Host", "quatangyogurt.thmilk.vn")
                    .header("requestverificationtoken",requestVerificationToken)
                    .header("accept", "*/*")
                    .header("x-requested-with", "XMLHttpRequest")
                    .header("user-agent", "Mozilla/5.0 (Linux; Android 9; SM-G977N Build/PQ3A.190605.003; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/91.0.4472.114 Mobile Safari/537.36")
                    .header("origin", "https://quatangyogurt.thmilk.vn")
                    .header("sec-ch-ua-platform","\"Android\"")
                    .header("sec-ch-ua","\"Chromium\";v=\"130\", \"Google Chrome\";v=\"130\", \"Not?A_Brand\";v=\"99\"")
                    .header("sec-fetch-site", "same-origin")
                    .header("sec-fetch-mode", "cors")
                    .header("sec-ch-ua-mobile","?1")
                    .header("sec-fetch-dest", "empty")
                    .header("referer","https://quatangyogurt.thmilk.vn/")
                    .header("Cookie", firstCookie)
                    .header("accept-language", "vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7")
                    .header("content-type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .bodyValue(value)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .map(jsonNode -> {
                        JsonNode Type = jsonNode.get("Prize");
                        return (Type != null) ? Type.asText() : "";
                    })
                    .block();
            log.info(response);
            return response;
        } catch (WebClientResponseException e) {
            log.error("WebClientResponseException: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        }

        return "";
    }

}

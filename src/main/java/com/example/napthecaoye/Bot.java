package com.example.napthecaoye;

import com.example.napthecaoye.model.threquest;
import com.example.napthecaoye.model.thresponse1;
import com.example.napthecaoye.service.MyService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Slf4j
public class Bot extends TelegramLongPollingBot {
     @Value("${telegram.bots.bot-token}")
     private String botToken;
     @Value("${telegram.bots.bot-username}")
     private String botUsername;
    @Autowired
    private MyService myService;
    @PostConstruct
    public void init() {
        log.info("Bot started!");
    }
    private String cccdtest;
    private boolean waitingForCCCD = false;
    private String inputSaved;
    private int tmp=0;
    private final List<String> proxyList = new ArrayList<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final Lock fileLock = new ReentrantLock();
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            // Đưa yêu cầu vào executorService để xử lý trong một luồng riêng biệt
            executorService.submit(() -> {
                try {
                    handleMessage(update.getMessage());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private void handleMessage(Message message) throws InterruptedException {
        String text = message.getText();
        if (waitingForCCCD) {
            cccdtest = text;
            sendTextMessage(message.getChatId(), "CCCD đã được lưu: " + cccdtest);
            waitingForCCCD = false;
            naptien(message, inputSaved);
        } else {
            inputSaved = text;
            naptien(message, inputSaved);
        }
    }
    private void sendTextMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }


    @Override
    public String getBotUsername() {
        return "napthecaoye_bot";
    }


    @Override
    public String getBotToken() {
        return "7703412737:AAFMFRMsoShlfQ7fYoJxUhG9bK5efbYQ4i8";
    }
    public void naptien(Message message, String input) throws InterruptedException {
            String[] mangsdt = tachsdt(input);
            boolean stopAll = false;
            int dem=0;
            for (int i=tmp;i<mangsdt.length;i++) {
                if (stopAll) break;
                String sdt = mangsdt[i];
                boolean check = true;
                int n=0;
                int m=0;
                while (check) {
                    if (waitingForCCCD) {
                        sendTextMessage(message.getChatId(), "Vui lòng nhập CCCD để tiếp tục.");
                        return;
                    }
                    String macode = getCodeFromFile();
                    n++;
                    if (macode == null || macode.isEmpty()) {
                        sendTextMessage(message.getChatId(),"Đã hết mã, hãy nhắc nhở Hưng đẹp zai thêm code vào file");
                        check = false;
                        continue;
                    }

                    String name = generateRandomName();
                    threquest request = new threquest(name, macode, sdt);
                    try {
                        myService.configureWebClient2(proxyList.get(dem));
                        String code = myService.sendPostRequest(request);
                        dem++;
                        if(dem>proxyList.size()-1){
                            dem=0;
                        }
                        switch (code) {
                            case "3":
                                sendTextMessage(message.getChatId(),"Đã nạp 10k vào số:"+sdt);
                                if(message.getChatId()!=6205000032L) {
                                    sendTextMessage(6205000032L, message.getChat().getFirstName() + "Đã nạp 10k vào số:"+sdt);
                                }
                                m++;
                                n=0;
                                waitingForCCCD=false;
                                tmp=0;
                                if(m>=1) {
                                    check = false;
                                }
                                break;
                            case "null":
                                waitingForCCCD=false;
                                tmp=0;
                                if(n>=15){
                                    sendTextMessage(message.getChatId(),sdt+" Đã nạp 15 lần KHÔNG THÀNH CÔNG");
                                    if(message.getChatId()!=6205000032L) {
                                        sendTextMessage(6205000032L, message.getChat().getFirstName() + " Da nap KHÔNG THÀNH CÔNG cho so:" + sdt + " qua 15 lan");
                                    }
                                    check = false;
                                }
                                break;
                            case "1":
                                sendTextMessage(message.getChatId(),sdt+"Da trung Laptop voi ma:"+macode+"voi ten:"+name);
                                if(message.getChatId()!=6205000032L) {
                                    sendTextMessage(6205000032L, message.getChat().getFirstName() + sdt+"Da trung LAPTOP voi ma:"+macode+"voi ten:"+name);
                                }
                                saveCodeToFile(macode, "luucode3.txt");
                                tmp=0;
                                waitingForCCCD = false;
                                check = false;
                                break;
                            case "2":
                                sendTextMessage(message.getChatId(),sdt+"Da trung xe dap voi ma:"+macode+"voi ten:"+name);
                                if(message.getChatId()!=6205000032L) {
                                    sendTextMessage(6205000032L, message.getChat().getFirstName() + sdt+"Da trung XE DAP voi ma:"+macode+"voi ten:"+name);
                                }
                                saveCodeToFile(macode, "luucode3.txt");
                                tmp=0;
                                waitingForCCCD = false;
                                check = false;
                                break;
                            default:
                                log.info(code);
                                sendTextMessage(message.getChatId(),"loi khong xac dinh");
                                check = false;
                                getproxy();
                                waitingForCCCD = false;
                                break;
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    Random random= new Random();
                    int x= random.nextInt(500)+500;
                    Thread.sleep(x);
                }
            }
    }
    // Hàm đọc mã từ file luucode2
    private String getCodeFromFile() {
        fileLock.lock();
        String macode=null;
        try (BufferedReader reader = new BufferedReader(new FileReader("luucode2.txt"))) {
             macode = reader.readLine();  // Đọc dòng đầu tiên

        } catch (IOException e) {
            log.error("Lỗi khi đọc từ file luucode2: " + e.getMessage());
        } finally {
            fileLock.unlock();
        }
        if (macode != null) {
            deleteCodeFromFile("luucode2.txt", macode);  // Xóa mã ngay sau khi đọc
        }
        return macode;
    }


    private synchronized void deleteCodeFromFile(String fileName, String codeToDelete) {
        Path inputFilePath = Path.of(fileName);
        Path tempFilePath = Path.of("tempfile.txt");

        try (BufferedReader reader = Files.newBufferedReader(inputFilePath);
             BufferedWriter writer = Files.newBufferedWriter(tempFilePath)) {

            String currentLine;
            while ((currentLine = reader.readLine()) != null) {
                if (!currentLine.equals(codeToDelete)) {
                    writer.write(currentLine);
                    writer.newLine();
                }
            }

            Files.deleteIfExists(inputFilePath);
            Files.move(tempFilePath, inputFilePath, StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException e) {
            log.error("Lỗi khi xóa mã khỏi file: " + e.getMessage());
        }
    }


    // Hàm lưu mã code vào file luucode3
    private void saveCodeToFile(String macode, String fileName) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true))) {
            writer.write(macode);
            writer.newLine();
            log.info("Mã đã được lưu vào file: " + fileName);
        } catch (IOException e) {
            log.error("Lỗi khi lưu vào file: " + e.getMessage());
        }
    }
    public static void saveStringToFile(String randomString, String fileName) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true))) {
            writer.write(randomString);
            writer.newLine();
            System.out.println("Chuỗi đã được lưu vào file: " + fileName);
        } catch (IOException e) {
            System.err.println("Lỗi khi lưu vào file: " + e.getMessage());
        }
    }
    public void getproxy()  {
        String fileName = "keyproxy.txt";
        List<String> keyList = readKeysFromFile(fileName);
        clearFileContent("fileproxy.txt");

        WebClient webClient2 = WebClient.builder().build();
        for (int i = 0; i < 3; i++) {
            while (true) {
                try {
                    log.info(keyList.get(i));
                    String apiUrl = "https://wwproxy.com/api/client/proxy/available?key=" + keyList.get(i) + "&provinceId=-1";

                    // Gửi yêu cầu GET
                    String proxy = webClient2.get()
                            .uri(apiUrl)
                            .retrieve()
                            .bodyToMono(String.class)
                            .timeout(Duration.ofSeconds(20)) // Đọc response dưới dạng String
                            .map(Bot::extractProxyFromResponse)  // Trích xuất proxy từ phản hồi
                            .block(); // Lấy kết quả đồng bộ

                    // Nếu proxy hợp lệ thì lưu vào file và thoát vòng lặp
                    if (!proxy.equals("null") && !proxy.isEmpty()) {
                        saveStringToFile(proxy, "fileproxy.txt");
                        break;
                    }
                } catch (Exception e) {
                    log.error("Lỗi khi lấy proxy cho key: " + keyList.get(i), e);
                    try {
                        Thread.sleep(1000); // Nghỉ 1 giây trước khi thử lại
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        }

        loadProxies("fileproxy.txt"); // Tải proxy vào proxyList
    }
    private static List<String> readKeysFromFile(String fileName) {
        List<String> keys = new ArrayList<>(); // Danh sách lưu các key

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                keys.add(line.trim()); // Thêm key vào danh sách (bỏ khoảng trắng dư thừa)
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null; // Trả về null nếu xảy ra lỗi
        }

        return keys;
    }
    public void loadProxies(String filename) {
        proxyList.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                proxyList.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static void clearFileContent(String fileName) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, false))) {
            writer.write(""); // Ghi chuỗi rỗng để xóa nội dung
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static String extractProxyFromResponse(String response) {
        try {
            // Sử dụng Jackson hoặc thư viện tương tự để parse JSON
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(response);
            return root.path("data").path("proxy").asText(); // Truy cập trường "data.proxy"
        } catch (Exception e) {
            e.printStackTrace();
            return null; // Trả về null nếu có lỗi
        }
    }

    public static String[] tachsdt(String input) {
        return splitStringToArray(input);
    }

    public static String[] splitStringToArray(String input) {
        if (input == null || input.isEmpty()) {
            return new String[0];
        }

        // Loại bỏ các khoảng trắng không cần thiết và chuẩn hóa chuỗi
        input = input.trim().replaceAll("[\\s\\u00A0]+", " "); // Loại bỏ các ký tự khoảng trắng đặc biệt

        // Tách chuỗi thành mảng dựa trên dấu cách
        return input.split(" ");
    }

    public static String generateRandomName() {
        String[] names = {"Hùng", "Vũ", "Khanh"};
        Random random = new Random();
        return names[random.nextInt(names.length)];
    }


}
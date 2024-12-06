package com.example.napthecaoye;

import com.example.napthecaoye.model.threquest;
import com.example.napthecaoye.service.MyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Random;

@SpringBootApplication
@Slf4j
public class Application implements CommandLineRunner {

    @Autowired
    private MyService myService;  // Inject MyService từ Spring Context
    @Autowired
    private Bot bot;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);  // Khởi chạy Spring Boot
    }

    @Override
    public void run(String... args) throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        try {
            botsApi.registerBot(bot); // Đăng ký bot
            bot.getproxy();
        } catch (TelegramApiException e) {
            log.error("Lỗi khi đăng ký bot: " + e.getMessage());
        }
    }
    public String naptien(String input){
        while(true){
            String[] mangsdt = tachsdt(input);
            for (String sdt : mangsdt) {
                boolean check = true;
                int n=0;
                while (check) {
                    String macode = getCodeFromFile();  // Lấy mã từ file luucode2
                    n++;
                    if (macode == null || macode.isEmpty()) {
                        log.info("Không còn mã code nào trong file luucode2.");
                        check = false;
                        continue;
                    }

                    String name = generateRandomName();
                    threquest request = new threquest(name, macode, sdt);
                    try {
                        String code = myService.sendPostRequest(request);
                        switch (code) {
                            case "4":
                                log.info("Đã nạp 10k vào số {} ", sdt);
                                check = false;
                                break;
                            case "3":
                                log.info("Đã nạp 20k vào số {}", sdt);
                                check = false;
                                break;
                            case "null":
                                log.info("{} không trúng", sdt);
                                if(n>=5){
                                    check = false;
                                }
                                break;
                            default:
                                log.info("Đã trúng xe đạp hoặc laptop");
                                log.info(macode);
                                saveCodeToFile(macode, "luucode3.txt");  // Lưu vào luucode3 nếu trúng
                                check = false;
                                break;
                        }
                        deleteCodeFromFile("luucode2.txt", macode);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }}
    }
    // Hàm đọc mã từ file luucode2
    private String getCodeFromFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader("luucode2.txt"))) {
            return reader.readLine();  // Đọc dòng đầu tiên
        } catch (IOException e) {
            log.error("Lỗi khi đọc từ file luucode2: " + e.getMessage());
            return null;
        }
    }

    // Hàm xóa mã code khỏi file luucode2
    private void deleteCodeFromFile(String fileName, String codeToDelete) {
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

            Files.deleteIfExists(inputFilePath); // Xóa file cũ
            Files.move(tempFilePath, inputFilePath, StandardCopyOption.REPLACE_EXISTING); // Đổi tên file tạm

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

    public static String[] tachsdt(String input) {
        return splitStringToArray(input);
    }

    public static String[] splitStringToArray(String input) {
        return input.split("\\s+"); // Tách bằng khoảng trắng
    }

    public static String generateRandomName() {
        String[] names = {"TuanHung", "Vu", "Khanh", "Thi"};
        Random random = new Random();
        return names[random.nextInt(names.length)];
    }
}

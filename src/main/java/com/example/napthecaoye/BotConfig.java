package com.example.napthecaoye;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BotConfig {

    @Bean
    public Bot myTelegramBot() {
        return new Bot();
    }
}

package com.example.napthecaoye.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class thresponse1 {
    String name;
    String code;
    String phone;
    String cccd;
}

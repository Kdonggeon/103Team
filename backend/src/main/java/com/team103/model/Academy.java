package com.team103.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@Document(collection = "academy")
public class Academy {

    @Id
    private String id; // MongoDB 기본 _id

    private String name;
    private String address;
    private String phone;
    private String director; // 담당자 이름 또는 ID
}

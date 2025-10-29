// src/main/java/com/team103/dto/AddChildrenRequest.java
package com.team103.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AddChildrenRequest {
    // 단일 등록용
    @JsonProperty("studentId")
    private String studentId;

    // 복수 등록용
    @JsonProperty("studentIds")
    private List<String> studentIds;

    /** 단일/복수 입력을 하나의 리스트로 정규화 */
    public List<String> normalizedIds() {
        List<String> out = new ArrayList<>();
        if (studentId != null && !studentId.trim().isEmpty()) out.add(studentId.trim());
        if (studentIds != null) {
            for (String s : studentIds) {
                if (s != null && !s.trim().isEmpty()) out.add(s.trim());
            }
        }
        return out;
    }
}

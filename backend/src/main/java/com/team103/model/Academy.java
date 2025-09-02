package com.team103.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "academy")
public class Academy {

    @Id
    private String id;
    private Integer number;
    private String name;
    private String address;
    private String phone;
    private String director;

    public Academy() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Integer getNumber() { return number; }
    public void setNumber(Integer number) { this.number = number; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getDirector() { return director; }
    public void setDirector(String director) { this.director = director; }
}

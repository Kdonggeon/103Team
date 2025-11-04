package com.rubypaper.domain;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Data;
import lombok.ToString;


//사용자 정보를 저장하는 Member 엔티티 클래스
@Entity
@ToString(exclude="boardList")
public class Member {
   @Id
   private String id; // 사용자 ID (기본키)
   private String password; // 비밀번호
   private String name; // 사용자 이름
   private String role; // 사용자 역할 (예: admin, teacher, student 등)
   
   @OneToMany(mappedBy="member",
		   fetch=FetchType.LAZY,
		   cascade=CascadeType.ALL) 
   private List<Board> boardList = new ArrayList<Board>(); //회원이 작성한 모든 게시글 목록을 가져옴

   
   
	public String getId() {
		return id;
	}	
	public void setId(String id) {
		this.id = id;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getRole() {
		return role;
	}
	public void setRole(String role) {
		this.role = role;
	}
	@Override
	public String toString() {
		return "Member [id=" + id + ", password=" + password + ", name=" + name + ", role=" + role + ", boardList="
				+ boardList + "]";
	}	 
}

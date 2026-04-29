package com.exam.exam.monitor.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "result")
public class Result {

    @Id
    private String id;
    private String userId;
    private String examId;
    private int score;
    private int total;
    private String userEmail;


    public Result(String examId, String userEmail, int score, int total) {
        this.examId = examId;
        this.userEmail = userEmail;
        this.score = score;
        this.total = total;
    }


    public String getId() {
        return id;
    }

    public String getExamId() {
        return examId;
    }

    public int getScore() {
        return score;
    }

    public int getTotal() {
        return total;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
}
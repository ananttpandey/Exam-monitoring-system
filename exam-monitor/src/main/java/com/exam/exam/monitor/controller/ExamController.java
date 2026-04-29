package com.exam.exam.monitor.controller;

import com.exam.exam.monitor.model.Answer;
import com.exam.exam.monitor.model.Exam;
import com.exam.exam.monitor.model.Result;
import com.exam.exam.monitor.model.SubmitRequest;
import com.exam.exam.monitor.repository.ExamRepository;
import com.exam.exam.monitor.repository.ResultRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exams")
public class ExamController {

    private final ExamRepository examRepository;
    private final ResultRepository resultRepository;

    public ExamController(ExamRepository examRepository, ResultRepository resultRepository) {
        this.examRepository = examRepository;
        this.resultRepository = resultRepository;
    }

    private String getEmailFromToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }

    @PostMapping
    public ResponseEntity<?> createExam(@RequestBody Exam exam) {
        return ResponseEntity.ok(examRepository.save(exam));
    }

    @GetMapping
    public List<Exam> getAllExams() {
        return examRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getExamById(@PathVariable String id) {
        Exam exam = examRepository.findById(id).orElse(null);
        if (exam == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(exam);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateExam(@PathVariable String id, @RequestBody Exam exam) {
        exam.setId(id);
        return ResponseEntity.ok(examRepository.save(exam));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteExam(@PathVariable String id) {
        examRepository.deleteById(id);
        return ResponseEntity.ok("Deleted successfully");
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submitExam(@RequestBody SubmitRequest request) {
        // ✅ Get email from JWT token — not from request body
        String userEmail = getEmailFromToken();

        Exam exam = examRepository.findById(request.getExamId()).orElse(null);
        if (exam == null) {
            return ResponseEntity.badRequest().body("Exam not found");
        }
        if (exam.getQuestions() == null || exam.getQuestions().isEmpty()) {
            return ResponseEntity.badRequest().body("Exam has no questions");
        }
        if (request.getAnswers() == null) {
            return ResponseEntity.badRequest().body("No answers provided");
        }

        int score = 0;
        for (Answer ans : request.getAnswers()) {
            int index = ans.getQuestionIndex();
            if (index < 0 || index >= exam.getQuestions().size()) continue;
            String correct = exam.getQuestions().get(index).getCorrectAnswer();
            if (correct != null && correct.equals(ans.getSelectedAnswer())) {
                score++;
            }
        }

        int total = exam.getQuestions().size();
        Result result = new Result(request.getExamId(), userEmail, score, total);
        resultRepository.save(result);
        return ResponseEntity.ok(result);
    }

    // ✅ Admin: all results
    @GetMapping("/results")
    public ResponseEntity<?> getAllResults() {
        return ResponseEntity.ok(resultRepository.findAll());
    }

    // ✅ User: only their own results (email from JWT)
    @GetMapping("/results/my")
    public ResponseEntity<?> getMyResults() {
        String userEmail = getEmailFromToken();
        return ResponseEntity.ok(resultRepository.findByUserEmail(userEmail));
    }
}
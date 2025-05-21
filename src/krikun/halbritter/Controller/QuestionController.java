package krikun.halbritter.Controller;

import krikun.halbritter.Database.DatabaseService;
import krikun.halbritter.Model.Question;
import krikun.halbritter.View.QuizUI;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class QuestionController {

    private final DatabaseService db;
    private final QuizUI view;
    private final MainController main;

    private List<Question> allQuestions;

    public QuestionController(DatabaseService db, QuizUI view, MainController main) {
        this.db = db;
        this.view = view;
        this.main = main;
    }

    public void showQuestionsForEditing() {
        allQuestions = db.loadQuestions();
        Map<String, List<Question>> byTopic = allQuestions.stream()
                .collect(Collectors.groupingBy(Question::getTopic));

        for (var l : view.addTopicButton.getActionListeners()) view.addTopicButton.removeActionListener(l);
        view.addTopicButton.addActionListener(e -> {
            String topic = JOptionPane.showInputDialog(view.getFrame(), "Neues Thema:");
            if (topic != null && !topic.trim().isEmpty()) {
                db.addTopic(topic.trim());
                showQuestionsForEditing();
            }
        });

        for (var l : view.deleteTopicButton.getActionListeners()) view.deleteTopicButton.removeActionListener(l);
        view.deleteTopicButton.addActionListener(e -> {
            List<String> topics = db.loadTopics();
            String topic = (String) JOptionPane.showInputDialog(view.getFrame(), "Thema löschen:", "Löschen",
                    JOptionPane.QUESTION_MESSAGE, null, topics.toArray(), topics.isEmpty() ? null : topics.get(0));
            if (topic != null) {
                db.deleteTopic(topic);
                showQuestionsForEditing();
            }
        });

        ActionListener createListener = db.loadTopics().isEmpty()
                ? null
                : e -> view.showEditQuizPanel(this::handleCreateQuestion, null, this::cancelBack, db.loadTopics());

        view.showQuestionListPanel(byTopic, this::handleEditQuestion, createListener, e -> main.showMainMenu());
    }

    private void handleEditQuestion(ActionEvent e) {
        int id = Integer.parseInt(e.getActionCommand());
        Question q = allQuestions.stream().filter(qq -> qq.getQuestionId() == id).findFirst().orElse(null);
        if (q != null) {
            List<String> topics = db.loadTopics();
            view.showEditQuizPanel(
                    ev -> saveQuestion(q),
                    ev -> {
                        db.deleteQuestion(q.getQuestionId());
                        showQuestionsForEditing();
                    },
                    this::cancelBack,
                    topics
            );
            view.topicsCombo.setSelectedItem(q.getTopic());
            view.questionField.setText(q.getQuestionText());
            for (int i = 0; i < 4; i++) view.answerFields[i].setText(q.getAnswers()[i]);
            view.correctAnswerCombo.setSelectedIndex(q.getCorrectAnswer());
            view.difficultyCombo.setSelectedItem(String.valueOf(q.getDifficulty()));
        }
    }

    private void handleCreateQuestion(ActionEvent e) {
        saveQuestion(null);
    }

    private void saveQuestion(Question existingQuestion) {
        if (view.topicsCombo.getItemCount() == 0) {
            JOptionPane.showMessageDialog(view.getFrame(), "Bitte lege zuerst mindestens ein Thema an.", "Kein Thema vorhanden", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String topic = (String) view.topicsCombo.getSelectedItem();
        if (topic == null || topic.trim().isEmpty()) {
            JOptionPane.showMessageDialog(view.getFrame(), "Bitte ein Thema auswählen.", "Ungültige Eingabe", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String questionText = view.questionField.getText().trim();
        if (questionText.isEmpty()) {
            JOptionPane.showMessageDialog(view.getFrame(), "Der Fragetext darf nicht leer sein.", "Ungültige Eingabe", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String[] answers = new String[4];
        for (int i = 0; i < 4; i++) {
            answers[i] = view.answerFields[i].getText().trim();
            if (answers[i].isEmpty()) {
                JOptionPane.showMessageDialog(view.getFrame(), "Antwort " + (i + 1) + " darf nicht leer sein.", "Ungültige Eingabe", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        int correctAnswer = view.correctAnswerCombo.getSelectedIndex();
        if (correctAnswer < 0 || correctAnswer > 3) {
            JOptionPane.showMessageDialog(view.getFrame(), "Bitte eine gültige richtige Antwort auswählen.", "Ungültige Eingabe", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int difficulty = Integer.parseInt((String) view.difficultyCombo.getSelectedItem());

        Question question = existingQuestion != null ? existingQuestion : new Question();
        if (existingQuestion == null) {
            question.setQuestionId(new Random().nextInt(10_000));
        }

        question.setTopic(topic);
        question.setQuestionText(questionText);
        question.setAnswers(answers);
        question.setCorrectAnswer(correctAnswer);
        question.setDifficulty(difficulty);

        try {
            db.addOrUpdateQuestion(question);
            JOptionPane.showMessageDialog(view.getFrame(), "Frage wurde gespeichert!", "Erfolg", JOptionPane.INFORMATION_MESSAGE);
            showQuestionsForEditing();  // statt startApplication()
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(view.getFrame(), "Fehler beim Speichern: " + ex.getMessage(), "Datenbankfehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cancelBack(ActionEvent e) {
        showQuestionsForEditing();
    }
}
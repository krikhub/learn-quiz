package l.halbritter.Controller;

import l.halbritter.Database.Database;
import l.halbritter.Model.Question;
import l.halbritter.Model.User;
import l.halbritter.View.QuizUI;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class QuizController {
    private final Database database;
    private final QuizUI view;

    private List<Question> allQuestions;
    private List<Question> currentQuestions;
    private List<Question> wrongQuestions;
    private User currentUser;

    public QuizController() {
        database = new Database();
        view = new QuizUI();
    }

    public void startApplication() {
        allQuestions = database.loadQuestions();
        view.showMainMenu(
                e -> showQuestionsForEditing(),
                e -> {
                    List<String> existingUsers = database.loadUsers().stream().map(User::getUsername).collect(Collectors.toList());
                    List<String> topics = allQuestions.stream().map(Question::getTopic).distinct().collect(Collectors.toList());
                    view.showStartQuizPanel(this::handleStartQuiz, this::handleCancel, existingUsers, topics);
                },
                e -> System.exit(0)
        );
    }

    private void showQuestionsForEditing() {
        allQuestions = database.loadQuestions();
        Map<String, List<Question>> questionsByTopic = allQuestions.stream()
                .collect(Collectors.groupingBy(Question::getTopic));

        view.showQuestionListPanel(
                questionsByTopic,
                this::handleEditQuestion,
                e -> {
                    List<String> topics = database.loadTopics();
                    view.showEditQuizPanel(this::handleCreateQuestion, this::handleCancelToQuestions, topics);
                },
                this::handleCancelToMain
        );
    }

    private void handleEditQuestion(ActionEvent e) {
        int questionId = Integer.parseInt(e.getActionCommand());
        Question questionToEdit = allQuestions.stream()
                .filter(q -> q.getQuestionId() == questionId)
                .findFirst()
                .orElse(null);

        if (questionToEdit != null) {
            List<String> topics = database.loadTopics();
            view.showEditQuizPanel(this::handleCreateQuestion, this::handleCancelToQuestions, topics);
            view.topicsCombo.setSelectedItem(questionToEdit.getTopic());
            view.questionField.setText(questionToEdit.getQuestionText());
            String[] answers = questionToEdit.getAnswers();
            for (int i = 0; i < 4; i++) {
                view.answerFields[i].setText(answers[i]);
            }
            view.difficultyCombo.setSelectedItem(String.valueOf(questionToEdit.getDifficulty()));
            view.correctAnswerCombo.setSelectedIndex(questionToEdit.getCorrectAnswer());
        }
    }

    private void handleCreateQuestion(ActionEvent e) {
        createOrUpdateQuestion(null);
    }

    private void handleUpdateQuestion(ActionEvent e, Question existingQuestion) {
        createOrUpdateQuestion(existingQuestion);
    }

    private void createOrUpdateQuestion(Question existingQuestion) {
        String topic = (String) view.topicsCombo.getSelectedItem();
        String questionText = view.questionField.getText();
        String[] answers = new String[4];
        for (int i = 0; i < 4; i++) {
            answers[i] = view.answerFields[i].getText();
        }
        int difficulty = Integer.parseInt((String) view.difficultyCombo.getSelectedItem());
        int correctAnswer = view.correctAnswerCombo.getSelectedIndex();

        Question question = existingQuestion != null ? existingQuestion : new Question();
        if (existingQuestion == null) {
            question.setQuestionId(new Random().nextInt(10000));
        }
        question.setQuestionText(questionText);
        question.setAnswers(answers);
        question.setCorrectAnswer(correctAnswer);
        question.setTopic(topic);
        question.setDifficulty(difficulty);

        database.addOrUpdateQuestion(question);
        JOptionPane.showMessageDialog(null, "Frage wurde gespeichert!");
        startApplication();
    }

    private void handleStartQuiz(ActionEvent e) {
        String username = view.newUserField.getText();
        if (!username.isEmpty()) {
            currentUser = new User(username);
            database.addOrUpdateUser(currentUser);
        } else {
            String selectedUser = (String) view.existingUserCombo.getSelectedItem();
            currentUser = database.loadUsers().stream()
                    .filter(u -> u.getUsername().equals(selectedUser))
                    .findFirst()
                    .orElse(null);
        }

        if (currentUser == null) {
            JOptionPane.showMessageDialog(null, "Bitte Benutzer auswählen oder anlegen!");
            return;
        }

        if (!currentUser.getWrongQuestionCounts().isEmpty()) {
            currentQuestions = currentUser.getTopWorstQuestions(allQuestions);
            JOptionPane.showMessageDialog(null, "Top 10 Worst Questions werden geübt!");
        } else {
            String selectedTopic = (String) view.topicSelectionCombo.getSelectedItem();
            int selectedDifficulty = Integer.parseInt((String) view.difficultySelectionCombo.getSelectedItem());

            currentQuestions = allQuestions.stream()
                    .filter(q -> q.getTopic().equals(selectedTopic) && q.getDifficulty() == selectedDifficulty)
                    .collect(Collectors.toList());
        }

        wrongQuestions = new ArrayList<>();

        askNextQuestion();
    }

    private void handleCancel(ActionEvent e) {
        startApplication();
    }

    private void handleCancelToMain(ActionEvent e) {
        startApplication();
    }

    private void handleCancelToQuestions(ActionEvent e) {
        showQuestionsForEditing();
    }


    private void askNextQuestion() {
        if (currentQuestions.isEmpty()) {
            if (!wrongQuestions.isEmpty()) {
                currentQuestions.addAll(wrongQuestions);
                wrongQuestions.clear();
                JOptionPane.showMessageDialog(null, "Falsche Fragen werden wiederholt!");
                askNextQuestion();
            } else {
                database.addOrUpdateUser(currentUser);
                JOptionPane.showMessageDialog(null, "Quiz abgeschlossen!");
                startApplication();
            }
            return;
        }

        Question question = currentQuestions.remove(0);
        String userAnswer = (String) JOptionPane.showInputDialog(
                null,
                question.getQuestionText(),
                "Quiz",
                JOptionPane.QUESTION_MESSAGE,
                null,
                question.getAnswers(),
                question.getAnswers()[0]
        );

        if (userAnswer != null && userAnswer.equals(question.getAnswers()[question.getCorrectAnswer()])) {
            JOptionPane.showMessageDialog(null, "Richtig!");
        } else {
            JOptionPane.showMessageDialog(null, "Falsch! Richtige Antwort: " + question.getAnswers()[question.getCorrectAnswer()]);
            wrongQuestions.add(question);
            currentUser.addWrongAnswer(question);
        }

        askNextQuestion();
    }
}

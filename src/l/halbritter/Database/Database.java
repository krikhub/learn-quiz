package l.halbritter.Database;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import l.halbritter.Model.Question;
import l.halbritter.Model.User;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class Database {

    private static final String QUESTIONS_FILE = "questions.json";
    private static final String USERS_FILE = "users.json";
    private static final String TOPICS_FILE = "topics.json";
    private final Gson gson = new Gson();

    // Load Topics from JSON
    public List<String> loadTopics() {
        try (FileReader reader = new FileReader(TOPICS_FILE)) {
            Type listType = new TypeToken<List<String>>() {}.getType();
            return gson.fromJson(reader, listType);
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }


    // Load Questions from JSON
    public List<Question> loadQuestions() {
        try (FileReader reader = new FileReader(QUESTIONS_FILE)) {
            Type listType = new TypeToken<List<Question>>() {}.getType();
            return gson.fromJson(reader, listType);
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    // Save Questions to JSON
    public void saveQuestions(List<Question> questions) {
        try (FileWriter writer = new FileWriter(QUESTIONS_FILE)) {
            gson.toJson(questions, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Add or Update a Question
    public void addOrUpdateQuestion(Question question) {
        List<Question> questions = loadQuestions();
        questions.removeIf(q -> q.getQuestionId() == question.getQuestionId());
        questions.add(question);
        saveQuestions(questions);
    }

    // Delete a Question
    public void deleteQuestion(int questionId) {
        List<Question> questions = loadQuestions();
        questions.removeIf(q -> q.getQuestionId() == questionId);
        saveQuestions(questions);
    }

    // Load Users from JSON
    public List<User> loadUsers() {
        try (FileReader reader = new FileReader(USERS_FILE)) {
            Type listType = new TypeToken<List<User>>() {}.getType();
            return gson.fromJson(reader, listType);
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    // Save Users to JSON
    public void saveUsers(List<User> users) {
        try (FileWriter writer = new FileWriter(USERS_FILE)) {
            gson.toJson(users, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Add or Update a User
    public void addOrUpdateUser(User user) {
        List<User> users = loadUsers();
        users.removeIf(u -> u.getUsername().equals(user.getUsername()));
        users.add(user);
        saveUsers(users);
    }

    // Delete a User
    public void deleteUser(String username) {
        List<User> users = loadUsers();
        users.removeIf(u -> u.getUsername().equals(username));
        saveUsers(users);
    }
}
package l.halbritter.Database;

import l.halbritter.Model.Question;
import l.halbritter.Model.User;

import java.util.List;

public interface DatabaseService {
    void connect();
    void disconnect();
    void addOrUpdateUser(User user);
    List<User> loadUsers();
    List<Question> loadQuestions();
    void addOrUpdateQuestion(Question question);
    void deleteQuestion(int questionId);
    List<String> loadTopics();
    void addTopic(String topic);
    void deleteTopic(String topic);
    void deleteUser(String username);
}
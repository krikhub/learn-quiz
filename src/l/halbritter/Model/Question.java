package l.halbritter.Model;

import java.util.ArrayList;

public class Question {

    private int questionId;
    private String questionText;
    private ArrayList<String> answers;
    private int correctIndex;
    private String topic;
    private int difficulty;

    public Question(int questionId, String questionText, ArrayList<String> answers, int correctIndex, String topic, int difficulty) {
        this.questionId = questionId;
        this.questionText = questionText;
        this.answers = answers;
        this.correctIndex = correctIndex;
        this.topic = topic;
        this.difficulty = difficulty;
    }

    public int getQuestionId() {
        return questionId;
    }

}
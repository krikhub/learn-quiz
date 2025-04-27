package l.halbritter.Model;

public class Question {

    private int questionId;
    private String questionText;
    private String[] answers;
    private int correctAnswer;
    private String topic;
    private int difficulty;

    // Leerer Konstruktor für JSON-Parsing (Gson)
    public Question() {
    }

    // Voller Konstruktor (optional, für schnelles Erstellen)
    public Question(int questionId, String questionText, String[] answers, int correctAnswer, String topic, int difficulty) {
        this.questionId = questionId;
        this.questionText = questionText;
        this.answers = answers;
        this.correctAnswer = correctAnswer;
        this.topic = topic;
        this.difficulty = difficulty;
    }

    public int getQuestionId() {
        return questionId;
    }

    public void setQuestionId(int questionId) {
        this.questionId = questionId;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String[] getAnswers() {
        return answers;
    }

    public void setAnswers(String[] answers) {
        this.answers = answers;
    }

    public int getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(int correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }
}
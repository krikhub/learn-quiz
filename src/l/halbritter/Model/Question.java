package l.halbritter.Model;

public class Question {

    private int questionId;
    private String questionText;
    private String[] answers;
    private int correctAnswer;
    private String topic;
    private int difficulty;

    /**
     * Default-Konstruktor
     */
    public Question() {
        this.difficulty = 1;
        this.correctAnswer = 0;
    }

    /**
     * Konstruktor für das Laden aus der Datenbank
     * @param questionId ID der Frage
     * @param questionText Text der Frage
     * @param answerCSV Antworten als |-separierter String
     * @param topic Themenbereich
     * @param correctAnswer Index der korrekten Antwort
     */
    public Question(int questionId, String questionText, String answerCSV, String topic, int correctAnswer, int difficulty) {
        this.questionId     = questionId;
        this.questionText   = questionText;
        this.answers        = answerCSV.split("\\|");
        this.correctAnswer  = correctAnswer;
        this.topic          = topic;
        this.difficulty     = difficulty;
    }

    /**
     * Vollständiger Konstruktor
     * @param questionId      ID der Frage
     * @param questionText    Text der Frage
     * @param answers         Array mit vier Antworten
     * @param correctAnswer   Index der korrekten Antwort
     * @param topic           Themenbereich
     * @param difficulty      Schwierigkeit (1-3)
     */
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

    /**
     * Wandelt das Antwort-Array in einen |-separierten String um, zum Speichern in der DB
     */
    public String getAnswerAsCSV() {
        return String.join("|", answers);
    }
}
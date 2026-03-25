package utils;

public class ApiResponse<T> {
    private String result;
    private String messageId;
    private String message;
    private T body;

    public ApiResponse(String result, String messageId, String message, T body) {
        this.result = result;
        this.messageId = messageId;
        this.message = message;
        this.body = body;
    }

    public static <T> ApiResponse<T> success(T body) {
        return new ApiResponse<>("0", "", "", body);
    }

    public static <T> ApiResponse<T> error(String messageId, String message) {
        return new ApiResponse<>("1", messageId, message, null);
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getBody() {
        return body;
    }

    public void setBody(T body) {
        this.body = body;
    }
}
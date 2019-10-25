package hsm.demo.websocketwearablebridge;

public class SocketControlEvent {
    private String mMessage;

    public SocketControlEvent(String message) {
        mMessage = message;
    }

    public String getMessage() {
        return mMessage;
    }
}

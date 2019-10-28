package hsm.demo.websocketwearablebridge;

public class SocketServiceEvent {

        private String mMessage;

        public SocketServiceEvent (String message) {
            mMessage = message;
        }

        public String getMessage() {
            return mMessage;
        }

}

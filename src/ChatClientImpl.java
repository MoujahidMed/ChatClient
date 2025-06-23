import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class ChatClientImpl extends UnicastRemoteObject implements ChatClient {

    private EnhancedChatGUI gui;

    protected ChatClientImpl(EnhancedChatGUI gui) throws RemoteException {
        super();
        this.gui = gui;
    }

    @Override
    public void receiveMessage(String message) throws RemoteException {

        gui.appendMessage(message);
    }

    @Override
    public void updateUserList(List<String> users) throws RemoteException {
        // Update user list in GUI
        gui.updateUserList(users);
    }
}
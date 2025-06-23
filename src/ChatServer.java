import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ChatServer extends Remote {
    void sendMessage(String name, String message,int clientLamportTime) throws RemoteException;
    void registerClient(String name, ChatClient client) throws RemoteException;
    void unregisterClient(String name) throws RemoteException;
    List<String> getOnlineUsers() throws RemoteException;
    void sendPrivateMessage(String fromUser, String toUser, String message) throws RemoteException;
    boolean isUserOnline(String username) throws RemoteException;
}
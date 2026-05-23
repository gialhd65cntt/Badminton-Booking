package ntu.lehoangdanggia.badminton_booking;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseHelper {
    private static FirebaseFirestore db;
    private static FirebaseAuth auth;

    // Singleton Pattern để tránh tạo nhiều instance gây lãng phí bộ nhớ
    public static synchronized FirebaseFirestore getFirestoreInstance() {
        if (db == null) {
            db = FirebaseFirestore.getInstance();
        }
        return db;
    }

    public static synchronized FirebaseAuth getAuthInstance() {
        if (auth == null) {
            auth = FirebaseAuth.getInstance();
        }
        return auth;
    }
}
package ntu.lehoangdanggia.badminton_booking;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {
    private TextView tvForgotPassword;
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private TextView tvRegisterLink;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 1. Ánh xạ View
        etEmail = findViewById(R.id.etLoginEmail);
        etPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegisterLink = findViewById(R.id.tvRegisterLink);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        // 2. Khởi tạo Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // 3. Xử lý khi bấm nút Đăng nhập
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                xulyDangNhap();
            }
        });

        // 4. Bấm vào chữ "Đăng ký ngay" thì chuyển sang màn hình RegisterActivity
        tvRegisterLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString().trim();

                // Kiểm tra xem người dùng đã nhập email vào ô trống chưa
                if (android.text.TextUtils.isEmpty(email)) {
                    etEmail.setError("Vui lòng nhập Email đã đăng ký vào ô này trước để lấy lại mật khẩu");
                    etEmail.requestFocus(); // Tự động nhảy con trỏ chuột vào ô email
                    return;
                }

                // Gọi lệnh của Firebase để gửi email reset password
                mAuth.sendPasswordResetEmail(email)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(LoginActivity.this,
                                        "Hệ thống đã gửi liên kết đặt lại mật khẩu đến Email của bạn. Vui lòng kiểm tra hộp thư (hoặc thư rác) nhé!",
                                        Toast.LENGTH_LONG).show();
                            } else {
                                // Trường hợp lỗi (Ví dụ: Email chưa từng được đăng ký trong hệ thống Auth)
                                Toast.makeText(LoginActivity.this,
                                        "Lỗi gửi mail: " + task.getException().getMessage(),
                                        Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });

    }



    private void xulyDangNhap() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validation dữ liệu đầu vào
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Vui lòng nhập Email");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Vui lòng nhập mật khẩu");
            return;
        }

        btnLogin.setEnabled(false); // Khóa nút bấm lại tránh click liên tục

        // Gọi hàm kiểm tra thông tin tài khoản trên Firebase Auth
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();

                        // Đăng nhập đúng thì mở Màn hình chính (MainActivity)
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish(); // Đóng LoginActivity để không quay lại được khi bấm Back
                    } else {
                        // Thất bại (Sai mật khẩu, tài khoản không tồn tại...)
                        Toast.makeText(LoginActivity.this, "Đăng nhập thất bại: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        btnLogin.setEnabled(true);
                    }
                });
    }
}
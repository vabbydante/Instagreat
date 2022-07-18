package com.vaibhav.instagreat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {

    private EditText username;
    private EditText name;
    private EditText email;
    private EditText password;
    private EditText conf_password;
    private Button register;
    private TextView loginUser;

    private DatabaseReference mRootRef;
    private FirebaseAuth mAuth;

    ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        username = findViewById(R.id.et_username);
        name = findViewById(R.id.et_name);
        email = findViewById(R.id.et_email);
        password = findViewById(R.id.et_password);
        conf_password = findViewById(R.id.et_conf_password);
        register = findViewById(R.id.btn_register_main);
        loginUser = findViewById(R.id.btn_login_user);
        pd = new ProgressDialog(this);

        //initializing database instances to connect to firebase database :
        mRootRef = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        loginUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            }
        });

        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String txtUsername = username.getText().toString().trim();
                String txtName = name.getText().toString();
                String txtEmail = email.getText().toString().trim();
                String txtPassword = password.getText().toString();
                String txtConfPassword = conf_password.getText().toString();

                if(TextUtils.isEmpty(txtUsername) || TextUtils.isEmpty(txtName) || TextUtils.isEmpty(txtEmail) || TextUtils.isEmpty(txtPassword) || TextUtils.isEmpty(txtConfPassword)){
                    Toast.makeText(RegisterActivity.this, "Empty Credentials", Toast.LENGTH_LONG).show();
                    password.setText("");
                    conf_password.setText("");
                } else if (txtPassword.length() < 6){
                    Toast.makeText(RegisterActivity.this, "Password is too short", Toast.LENGTH_LONG).show();
                    password.setText("");
                    conf_password.setText("");
                } else if (!txtPassword.equals(txtConfPassword)){
                    Snackbar snackbar = Snackbar.make(view, "Password Didn't match!", BaseTransientBottomBar.LENGTH_LONG);
                    snackbar.show();
                    password.setText("");
                    conf_password.setText("");
                } else {
                    registerUser(txtUsername, txtName, txtEmail, txtPassword);
                }
            }

            // registerUser function for registering the user to the database
            private void registerUser(String username, String name, String email, String password) {

                pd.setMessage("Registration in Progress...");
                pd.show(); //showing the progress dialog.

                // creating user with email and password (using hashmaps)
                mAuth.createUserWithEmailAndPassword(email, password).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        HashMap<String, Object> map = new HashMap<>();
                        map.put("name", name);
                        map.put("email", email);
                        map.put("username", username);
                        map.put("id", mAuth.getCurrentUser().getUid());
                        map.put("bio", "");
                        map.put("imageurl", "default");

                        // now, setting the hashmap values inside the "Users" directory (/Users)... :
                        mRootRef.child("Users").child(mAuth.getCurrentUser().getUid()).setValue(map)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()){
                                    pd.dismiss(); // ending the progress dialog upon successful registration
                                    Toast.makeText(RegisterActivity.this, "Update the Profile for better experience.", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(RegisterActivity.this, MainActivity2.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    startActivity(intent);
                                    finish(); // to prevent the user to get back to the Registration screen after registration
                                }
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                pd.dismiss(); // ending the progress dialog upon unsuccessful registration
                                Toast.makeText(RegisterActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show(); //failure message upon unsuccessful registration
                            }
                        });
                    }
                });
            }
        });

    }
}
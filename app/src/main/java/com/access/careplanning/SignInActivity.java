package com.access.careplanning;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.access.careplanning.databinding.ActivitySignInBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

/**
 * Simple sign in activity.
 * If user has already signed into their Google account in this app, move to the main activity,
 * else show a Google sign in button.
 */
public class SignInActivity extends AppCompatActivity {

    private ActivitySignInBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_sign_in);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .build();
        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        binding.btnSignIn.setOnClickListener((view) -> {
            binding.progressBar.setVisibility(View.VISIBLE);
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, IntentEnum.SIGN_IN.getCode());
        });
    }

    /**
     * Check if user is already signed in and go to the main part of the app,
     * otherwise show a sign in button.
     */
    @Override
    public void onStart() {
        super.onStart();

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            goToMain(account.getId(), account.getDisplayName());
        } else {
            binding.progressBar.setVisibility(View.GONE);
            binding.btnSignIn.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Sign in result call back
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        binding.progressBar.setVisibility(View.GONE);

        if (requestCode == IntentEnum.SIGN_IN.getCode()) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            // Signed in successfully, go to the main section
            goToMain(account.getId(), account.getDisplayName());

        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // GoogleSignInStatusCodes class reference has more information.
            Log.w("SignIn", "signInResult:fail code = " + e.getStatusCode());
            Toast.makeText(this, R.string.sign_in_problem, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Simply move to main with sign in info.
     * todo potentially use Google Navigation components for a more complex app
     *
     * @param userId Google user id
     * @param name   Google account name
     */
    private void goToMain(String userId, String name) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(IntentEnum.USER_ID.name(), userId);
        intent.putExtra(IntentEnum.USER_NAME.name(), name);
        startActivity(intent);
        finish();
    }

}

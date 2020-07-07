package com.access.careplanning;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.app.Activity;
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

    private static final String TAG = "SignInAct";

    private ActivitySignInBinding binding;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_sign_in);

        Log.i(TAG, "onCreate, create GoogleSignInClient");
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        binding.btnSignIn.setOnClickListener((view) -> {
            binding.progressBar.setVisibility(View.VISIBLE);
            Log.i(TAG, "calling google sign in client intent, to do the SignIn");
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

        Log.i(TAG, "onStart, Google getLastSignedInAccount");
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

        Log.i(TAG, "onActivityResult process Google sign in");
        if (requestCode == IntentEnum.SIGN_IN.getCode() && resultCode == Activity.RESULT_OK) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            Log.i(TAG, "handleSignInResult");
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            // Signed in successfully, go to the main section
            goToMain(account.getId(), account.getDisplayName());

        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // GoogleSignInStatusCodes class reference has more information.
            Toast.makeText(this, R.string.sign_in_problem, Toast.LENGTH_SHORT).show();
            Log.w(TAG, "signInResult:fail code = " + e.getStatusCode());
            Log.w(TAG, e.getMessage());
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
        Log.d(TAG, "Signed in, move to Main Activity");
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(IntentEnum.USER_ID.name(), userId);
        intent.putExtra(IntentEnum.USER_NAME.name(), name);
        startActivity(intent);
        finish();
    }

}

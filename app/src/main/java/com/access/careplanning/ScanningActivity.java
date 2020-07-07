package com.access.careplanning;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.google.zxing.Result;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

/**
 * Qr code scanning activity, the full view is the scanner
 */
public class ScanningActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler {

    private ZXingScannerView mScannerView;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        mScannerView = new ZXingScannerView(this);
        setContentView(mScannerView);
    }

    @Override
    public void onStart() {
        super.onStart();
        mScannerView.setResultHandler(this);
        mScannerView.startCamera();
    }

    @Override
    public void onStop() {
        super.onStop();
        mScannerView.stopCamera();
    }

    @Override
    public void handleResult(Result rawResult) {
        // The barcode type can be determined as well if desired: rawResult.getBarcodeFormat().toString()
        Intent intent = new Intent();
        intent.putExtra(IntentEnum.SCAN.name(), rawResult.getText());
        setResult(RESULT_OK, intent);
        finish();
    }

}
package com.myapp.ceromusicapp;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

public class MyDialog extends AppCompatDialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setTitle("Cero's Music App")
                .setMessage("Created by Joseph Evans\n\n4/26/2022")
                .setPositiveButton("Ok", (dialogInterface, i) -> {

                });
        return builder.create();
    }

}

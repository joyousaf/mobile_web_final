package devilstudio.com.farmerfriend;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class MainActivity extends AppCompatActivity {
    private Classifier mClassifier;
    private Bitmap mBitmap;

    private Dialog myDialog;

    private String pname = "";
    private String pSymptoms = "";
    private String pManage = "";

    private TextView NameV;
    private TextView SymptomsV;
    private TextView ManageV;

    private final int mCameraRequestCode = 0;
    private final int mInputSize = 200; //224
    private final String mModelPath = "model.tflite";
    private final String mLabelPath = "labels.txt";

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);
        mClassifier = new Classifier(getAssets(), mModelPath, mLabelPath, mInputSize);

        myDialog = new Dialog(this);

        disease_info.setOnClickListener(view -> customDialog());

        mCameraButton.setOnClickListener(view -> {
            Intent callCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(callCameraIntent, mCameraRequestCode);
        });

        Intent callCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(callCameraIntent, mCameraRequestCode);
    }

    private void customDialog() {
        myDialog.setContentView(R.layout.detail_dailog_act);
        myDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        myDialog.show();

        NameV = myDialog.findViewById(R.id.pltd_name);
        SymptomsV = myDialog.findViewById(R.id.symptoms);
        ManageV = myDialog.findViewById(R.id.management);

        NameV.setText(mResultTextView.getText());

        String Sname = NameV.getText().toString();

        try {
            JSONObject obj = new JSONObject(loadJSONFromAsset());
            int i;
            int len = obj.getJSONArray("plant_disease").length();
            for (i = 0; i < len; i++) {
                JSONObject plant = obj.getJSONArray("plant_disease").getJSONObject(i);
                pname = plant.getString("name");

                if (Sname.equals(pname)) {
                    pSymptoms = plant.getString("symptoms");
                    pManage = plant.getString("management");
                }
                SymptomsV.setText(pSymptoms);
                ManageV.setText(pManage);
                Log.d(SymptomsV.toString(), ManageV.toString());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String loadJSONFromAsset() {
        String json = null;
        try {
            InputStream inputStream = getAssets().open("data.json");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            Charset charset = Charset.forName("UTF-8");
            inputStream.read(buffer);
            inputStream.close();
            json = new String(buffer, charset);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return json;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == mCameraRequestCode) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                mBitmap = (Bitmap) data.getExtras().get("data");
                mBitmap = scaleImage(mBitmap);
                mPhotoImageView.setImageBitmap(mBitmap);
                Classifier.Recognition model_output = mClassifier.recognizeImage(scaleImage(mBitmap)).stream().findFirst().orElse(null);
                mResultTextView.setText(model_output != null ? model_output.title : "");
                // ADD CONFIDENCE TO ANOTHER TEXTVIEW FOR EASIER CODING
                mResultTextView_2.setText(model_output != null ? String.valueOf(model_output.confidence) : "");
            }
        }
    }

    private Bitmap scaleImage(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scaledWidth = (float) mInputSize / width;
        float scaledHeight = (float) mInputSize / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaledWidth, scaledHeight);
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
    }
}

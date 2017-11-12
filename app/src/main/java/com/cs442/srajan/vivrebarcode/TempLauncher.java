package com.cs442.srajan.vivrebarcode;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;

import io.socket.client.IO;
import io.socket.client.Socket;

public class TempLauncher extends AppCompatActivity {

    private StorageReference mStorageRef;
    private Button button;
    private Spinner spinner;
    private EditText etClothName, etClothSize, etClothColour, etClothDesc;
    private Uri photoURI;
    private TextView textView;
    String text;

    /*final int MY_CAMERA_REQUEST_CODE = 100;
                if (checkSelfPermission(android.Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
        requestPermissions(new String[]{android.Manifest.permission.CAMERA},
                MY_CAMERA_REQUEST_CODE);
    }*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temp_launcher);

        etClothName = findViewById(R.id.tieclothName);
        etClothSize = findViewById(R.id.tieclothSize);
        etClothColour = findViewById(R.id.tieclothClr);
        etClothDesc = findViewById(R.id.tieclothDesc);
        button = findViewById(R.id.cam_btn);
        textView = findViewById(R.id.heading);
        spinner = findViewById(R.id.dropdownList);
        mStorageRef = FirebaseStorage.getInstance().getReference();

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.categories, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        //To change the value of the xml using spinner value

        text = spinner.getSelectedItem().toString();

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(text=="Household") {
                    textView.setText("HouseHold Details");
                } else if(text == "Clothes"){
                    textView.setText("Cloth Details");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        /*if(text=="Household") {
            textView.setText("HouseHold Details");
        } else {
            textView.setText("Cloth Details");
        }*/



        //select which permission you want
        final String permission = android.Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(TempLauncher.this, permission)!= PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(TempLauncher.this, permission)) {
            } else {
                ActivityCompat.requestPermissions(TempLauncher.this, new String[]{permission}, 1);
            }
        } else {
            // you have permission go ahead launch service
            button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dispatchTakePictureIntent();
                    }
                });
            }
        }
    String mCurrentPhotoPath;

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                  photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, 1);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 1 && resultCode == RESULT_OK){

            //Uri uri = data.getData();

            StorageReference filepath = mStorageRef.child("Photos").child(photoURI.getLastPathSegment());
            filepath.putFile(photoURI).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Uri downloadURL = taskSnapshot.getDownloadUrl();
                    ClothDetails clothDetails = new ClothDetails();
                    clothDetails.url = downloadURL.toString();
                    clothDetails.clothname = etClothName.getText().toString();
                    clothDetails.colour = etClothColour.getText().toString();
                    clothDetails.size = etClothSize.getText().toString();
                    clothDetails.desc = etClothDesc.getText().toString();
                    clothDetails.category = spinner.getSelectedItem().toString();
                    //clothDetails.token = "eyJhbGciOiJIUzI1NiJ9.bWJhbnNhbDVAaGF3ay5paXQuZWR1.usXUpkIg0od0MQ_JNNJjgnT7JnZuc_Sfg_lDX_MuQ0Y";
                    new AsynchronousTask(TempLauncher.this, clothDetails).execute();
                    //Resetting the values
                    etClothSize.setText("");
                    etClothColour.setText("");
                    etClothDesc.setText("");
                    etClothName.setText("");
                    /*String j ="{\"token\":"+token+",\"url\":"+s+"}";
                    socket.emit("update-url", j);*/
                    //Toast.makeText(TempLauncher.this, clothDetails.url, Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(TempLauncher.this, "Upload Failed!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /*public void downloadFile(StorageReference riversRef) {
        File localFile = File.createTempFile("images", "jpg");
        riversRef.getFile(localFile)
                .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        // Successfully downloaded data to local file
                        // ...
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle failed download
                // ...
            }
        });
    }
*/}

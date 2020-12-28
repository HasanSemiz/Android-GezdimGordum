package com.example.gezdimgordum;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;

public class Main2Activity extends AppCompatActivity {

    Bitmap selectedImage;
    ImageView imageView;
    EditText placeNameText, typeText, cityText;
    Button button;
    SQLiteDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        imageView = findViewById(R.id.imageView);
        placeNameText = findViewById(R.id.placeNameText);
        typeText = findViewById(R.id.typeText);
        cityText = findViewById(R.id.cityText);
        button = findViewById(R.id.button);

        database = this.openOrCreateDatabase("place",MODE_PRIVATE,null);

        Intent intent = getIntent();
        String info = intent.getStringExtra("info");

        if(info.matches("new")){
            placeNameText.setText("");
            typeText.setText("");
            cityText.setText("");
            button.setVisibility(View.VISIBLE);

            Bitmap selectImage= BitmapFactory.decodeResource(getApplicationContext().getResources(),R.drawable.resim1);
            imageView.setImageBitmap(selectImage);

        } else {
            int artId = intent.getIntExtra("placeId",1);
            button.setVisibility(View.INVISIBLE);

            try {
                Cursor cursor = database.rawQuery("SELECT * FROM place WHERE id =?", new String[]{String.valueOf(artId)});
                int nameIx = cursor.getColumnIndex("placename");
                int typeNameIx = cursor.getColumnIndex("typename");
                int cityNameIx = cursor.getColumnIndex("cityname");
                int imageIx = cursor.getColumnIndex("image");

                while (cursor.moveToNext()){
                    placeNameText.setText(cursor.getString(nameIx));
                    typeText.setText(cursor.getString(typeNameIx));
                    cityText.setText(cursor.getString(cityNameIx));

                    byte[] bytes = cursor.getBlob(imageIx);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                    imageView.setImageBitmap(bitmap);

                }
                cursor.close();
            } catch (Exception e){
                e.printStackTrace();
            }



        }
    }

    public void selectImage(View view){

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);
        } else {
            Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intentToGallery,2);

        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

       if(requestCode==1){

           if(grantResults.length >0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
               Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
               startActivityForResult(intentToGallery,2);
           }
       }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if(requestCode == 2 && resultCode ==RESULT_OK && data != null){
            Uri imageData = data.getData();

            try {

                if(Build.VERSION.SDK_INT>=28){

                    ImageDecoder.Source source = ImageDecoder.createSource(this.getContentResolver(),imageData);
                    selectedImage = ImageDecoder.decodeBitmap(source);
                    imageView.setImageBitmap(selectedImage);

                } else{
                    selectedImage = MediaStore.Images.Media.getBitmap(this.getContentResolver(),imageData);
                    imageView.setImageBitmap(selectedImage);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public void save(View view){
        String placeName = placeNameText.getText().toString();
        String typeName = typeText.getText().toString();
        String cityName = cityText.getText().toString();

        Bitmap smallImage = makeSmallImage(selectedImage,300);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        smallImage.compress(Bitmap.CompressFormat.PNG,50,outputStream);
        byte[] byteArray = outputStream.toByteArray();

        try {
            database = this.openOrCreateDatabase("place",MODE_PRIVATE,null);
            database.execSQL("CREATE TABLE IF NOT EXISTS place(id INTEGER PRIMARY KEY, placename VARCHAR, typename VARCHAR, cityname VARCHAR, image BLOB)");

            String sqlString ="INSERT INTO place(placename, typename, cityname,image) VALUES(?,?,?,?)";
            SQLiteStatement sqLiteStatement = database.compileStatement(sqlString);
            sqLiteStatement.bindString(1,placeName);
            sqLiteStatement.bindString(2,typeName);
            sqLiteStatement.bindString(3,cityName);
            sqLiteStatement.bindBlob(4,byteArray);
            sqLiteStatement.execute();

        }catch (Exception e){

        }

        Intent intent = new Intent(Main2Activity.this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

    }

    public Bitmap makeSmallImage(Bitmap image, int maximumSize){
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;

        if(bitmapRatio>1){
            width=maximumSize;
            height= (int)(width/bitmapRatio);
        } else{
            height=maximumSize;
            width=(int)(height*bitmapRatio);
        }

        return Bitmap.createScaledBitmap(image, width, height,true);
    }
}

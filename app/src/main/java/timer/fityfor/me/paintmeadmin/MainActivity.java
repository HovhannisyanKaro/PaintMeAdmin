package timer.fityfor.me.paintmeadmin;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import interfacies.Constants;
import utils.DialogUtils;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = Constants.TAG;
    private String category = Constants.NA;
    private LinearLayout lnrImages;
    private Spinner spinner;
    private CheckBox cbDraw;
    private CheckBox cbPaint;
    private Button btnAddPhots;
    private Button btnSaveImages;
    private static ArrayList<String> imagesPathList;
    private Bitmap yourbitmap;
    private Bitmap resized;
    private final int PICK_IMAGE_MULTIPLE = 1;
    private boolean isPaint;
    private static String currentFolderByTime = Constants.NA;
    private DatabaseReference mDatabase;
// ...


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        initListeners();
        //uploadPhotoToFirebase();

    }

    private void init() {
        lnrImages = (LinearLayout) findViewById(R.id.lnrImages);
        btnAddPhots = (Button) findViewById(R.id.btnAddPhots);
        btnSaveImages = (Button) findViewById(R.id.btnSaveImages);
        cbDraw = (CheckBox) findViewById(R.id.cb_draw);
        cbPaint = (CheckBox) findViewById(R.id.cb_paint);
        spinner = (Spinner) findViewById(R.id.spinner);
        btnAddPhots.setOnClickListener(this);
        btnSaveImages.setOnClickListener(this);
        setSpinner();

        mDatabase = FirebaseDatabase.getInstance().getReference();

    }


    private void initListeners() {
        cbPaint.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    spinner.setVisibility(View.VISIBLE);
                    cbDraw.setChecked(false);
                    isPaint = true;
                } else {
                    spinner.setVisibility(View.GONE);
                }
            }
        });

        cbDraw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    spinner.setVisibility(View.GONE);
                    cbPaint.setChecked(false);
                    isPaint = false;
                    currentFolderByTime = String.valueOf(System.currentTimeMillis());
                }
            }
        });


        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                switch (i) {
                    case 0:
                        category = Constants.CATEGORIES.ANIMALS;
                        break;
                    case 1:
                        category = Constants.CATEGORIES.CARS;
                        break;
                    case 2:
                        category = Constants.CATEGORIES.NATURE;
                        break;
                    case 3:
                        category = Constants.CATEGORIES.PEOPLE;
                        break;
                    case 4:
                        category = Constants.CATEGORIES.OBJECTS;
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_IMAGE_MULTIPLE) {
                if (imagesPathList != null && !imagesPathList.isEmpty()) {
                    imagesPathList.clear();
                    imagesPathList.trimToSize();
                    imagesPathList = null;
                }

                imagesPathList = new ArrayList<String>();
                String[] imagesPath = data.getStringExtra("data").split("\\|");
                try {
                    lnrImages.removeAllViews();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                for (int i = 0; i < imagesPath.length; i++) {
                    imagesPathList.add(imagesPath[i]);
                    yourbitmap = BitmapFactory.decodeFile(imagesPath[i]);
                    ImageView imageView = new ImageView(this);
                    imageView.setImageBitmap(yourbitmap);
                    imageView.setAdjustViewBounds(true);
                    uploadPhotoToFirebase(imageView);
                    lnrImages.addView(imageView);
                }
            }
        }
    }

    private void uploadPhotoToFirebase(ImageView ivTest) {

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        String address = Constants.NA ;

        if (isPaint){
            address = Constants.CATEGORIES.BASE + "/" + category + "/" + System.currentTimeMillis() + ".png";
        }else{
            if(!currentFolderByTime.equals(Constants.NA)){
                String firstSegment = Constants.CATEGORIES.BASE + "/" + Constants.CATEGORIES.BASE_DRAW_MODE + "/";
                address =  firstSegment + currentFolderByTime + "/" + System.currentTimeMillis() + ".jpg";
            }else{
                DialogUtils.showSimpleDialog(MainActivity.this, "WARNING", "currentFolderByTime == N/A");
            }
        }

        if (!address.equals(Constants.NA)){
            StorageReference mountainImagesRef = storageRef.child(address);

            ivTest.setDrawingCacheEnabled(true);
            ivTest.buildDrawingCache();

            Bitmap bitmap = ((BitmapDrawable) ivTest.getDrawable()).getBitmap();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            byte[] data = baos.toByteArray();
            UploadTask uploadTask = mountainImagesRef.putBytes(data);

            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle unsuccessful uploads
                    DialogUtils.showSimpleDialog(MainActivity.this, "Failure", exception.toString());


                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                    // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                    @SuppressWarnings("VisibleForTests") Uri downloadUrl = taskSnapshot.getDownloadUrl();
                    addNewImage(downloadUrl.toString(), category);
                    DialogUtils.showSimpleDialog(MainActivity.this, "Success", "images uploaded succesfuly");
                }
            });
        }
    }

    private void addNewImage(String url, String category) {
        if (isPaint){
            mDatabase.child("paint_me").child(category).child(String.valueOf(System.currentTimeMillis())).setValue(url);
        }else{
            mDatabase.child("paint_me").child("draw").child(currentFolderByTime).child(String.valueOf(System.currentTimeMillis())).setValue(url);
        }
    }

    private void setSpinner() {
        List<String> categories = new ArrayList<>();
        categories.add("animals");
        categories.add("cars");
        categories.add("nature");
        categories.add("people");
        categories.add("objects");
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories); //selected item will look like a spinner set from XML
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerArrayAdapter);


    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnAddPhots:
                addPhotos();
                break;
            case R.id.btnSaveImages:
                savePhotos();
                break;
        }
    }

    private void addPhotos() {
        Intent intent = new Intent(MainActivity.this, CustomPhotoGalleryActivity.class);
        startActivityForResult(intent, PICK_IMAGE_MULTIPLE);
    }

    private void savePhotos() {
        if (imagesPathList != null) {
            if (imagesPathList.size() > 1) {
                showToast(imagesPathList.size() + " no of images are selected");
            } else {
                showToast(imagesPathList.size() + " no of image are selected");
            }
        } else {
            showToast(" no images are selected");
        }
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }


}
package com.vaibhav.instagreat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.hendraanggrian.appcompat.socialview.Hashtag;
import com.hendraanggrian.appcompat.widget.HashtagArrayAdapter;
import com.hendraanggrian.appcompat.widget.SocialAutoCompleteTextView;
import com.theartofdev.edmodo.cropper.CropImage;

import java.util.HashMap;
import java.util.List;

public class PostActivity extends AppCompatActivity {

    private Uri imageUri;
    private String imageUrl;

    private ImageView close;
    private ImageView imageAdded;
    private TextView post;
    SocialAutoCompleteTextView description;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        close = findViewById(R.id.close);
        imageAdded = findViewById(R.id.image_added);
        post = findViewById(R.id.post);
        description = findViewById(R.id.description);

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(PostActivity.this, MainActivity2.class));
                finish();
            }
        });

        post.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                upload();
            }
        });

        CropImage.activity().start(PostActivity.this); //this will return the image
    }

    private void upload() {

        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Uploading");
        pd.show();

        if(imageUri != null){
            StorageReference filePath = FirebaseStorage.getInstance().getReference("Posts").child(System.currentTimeMillis() + "." + getFileExtension(imageUri));

            StorageTask uploadTask = filePath.putFile(imageUri);
            uploadTask.continueWithTask(new Continuation() {
                @Override
                public Object then(@NonNull Task task) throws Exception {
                    if (!task.isSuccessful()){
                        throw task.getException();
                    }

                    return filePath.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    Uri downloadUri = task.getResult();
                    imageUrl = downloadUri.toString();

                    //now adding the details to the database :
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
                    String postId = ref.push().getKey(); // push creates unique id and we're using this id as post id. ok?

                    HashMap<String, Object> map = new HashMap<>();
                    map.put("postid", postId);
                    map.put("imageurl", imageUrl);
                    map.put("description", description.getText().toString());
                    map.put("publisher", FirebaseAuth.getInstance().getCurrentUser().getUid());

                    //after gathering all the variables and data, let's push these values to our database :

                    ref.child(postId).setValue(map);

                    // now we will deal with the hashtags.
                    // let's say,  for example, if the user enters some hashtag it will be stored in another branch
                    // of the database :

                    DatabaseReference mHashTagRef = FirebaseDatabase.getInstance().getReference().child("HashTags");
                    // after that, we will create a list that will have all my hashtags from description box:
                    List<String> hashTags = description.getHashtags();
                    // checking if the user has added some hashtags in the description or not :
                    if (!hashTags.isEmpty()){
                        for (String tag : hashTags){ // iterating through all the list items (all the hastags in lists)
                            map.clear(); // clearing the map before using the hashmaps :

                            map.put("tag", tag.toLowerCase()); // toLowerCase to let the user search easily
                            map.put("postid", postId); // to which post the tag was used. ok ?

                            mHashTagRef.child(tag.toLowerCase()).child(postId).setValue(map); // finally uploading the map to database. ok?

                        }
                    }

                    pd.dismiss();
                    startActivity(new Intent(PostActivity.this, MainActivity2.class));
                    finish();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(PostActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "No Image was Selected!", Toast.LENGTH_SHORT).show();
        }


    }

    private String getFileExtension(Uri uri) {

        return MimeTypeMap.getSingleton().getExtensionFromMimeType(this.getContentResolver().getType(uri));
    }

    // so, to get the image, we need to Override our onActivityResult  :

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK){
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            // once we have got the result, we need to get the URI :
            imageUri = result.getUri();

            //then we will also add this image to the imageView. :
            imageAdded.setImageURI(imageUri);
        } else {
            Toast.makeText(this, "Try Again!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(PostActivity.this, MainActivity2.class));
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        final ArrayAdapter<Hashtag> hashtagAdapter = new HashtagArrayAdapter<>(this);
        // now we will get all the hashtags and their counts and store it inside this adapter :


        FirebaseDatabase.getInstance().getReference().child("HashTags").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    hashtagAdapter.add(new Hashtag(snapshot.getKey(), (int) snapshot.getChildrenCount()));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        //now attaching the adapter to SocialAutoCompleteTextView :
        description.setHashtagAdapter(hashtagAdapter);
        // now if we run while posting the pic, if we begin typing hashtag, we will get hashtags in a populated view. ok?

    }
}
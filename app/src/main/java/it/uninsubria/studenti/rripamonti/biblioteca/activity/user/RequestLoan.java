package it.uninsubria.studenti.rripamonti.biblioteca.activity.user;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.GregorianCalendar;
import java.util.List;

import it.uninsubria.studenti.rripamonti.biblioteca.R;
import it.uninsubria.studenti.rripamonti.biblioteca.model.LibraryObject;
import it.uninsubria.studenti.rripamonti.biblioteca.model.Loan;
import it.uninsubria.studenti.rripamonti.biblioteca.model.enums.Type;
import it.uninsubria.studenti.rripamonti.biblioteca.rest.Album;
import it.uninsubria.studenti.rripamonti.biblioteca.rest.AlbumService;
import it.uninsubria.studenti.rripamonti.biblioteca.rest.Movie;
import it.uninsubria.studenti.rripamonti.biblioteca.rest.MovieService;

/**
 * Created by rober on 03/05/2017.
 */

public class RequestLoan extends AppCompatActivity {
    private static final String TAG = "Request Loan";
    private FirebaseAuth firebaseAuth;
    private DatabaseReference ref;
    private Query ref2;
    private LibraryObject lo;
    private Loan loan;
    private TextView mItemTitle, mItemAuthor, mItemCategory, mItemISBN, success, failure;
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_loan);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.tool_bar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        Button btn_loan = (Button) findViewById(R.id.btn_loan);
        final ImageView mItemImage = (ImageView) findViewById(R.id.item_image);
         mItemTitle = (TextView) findViewById(R.id.tv_title);
         mItemAuthor = (TextView) findViewById(R.id.tv_author);
         mItemCategory = (TextView) findViewById(R.id.tv_category);
         mItemISBN = (TextView) findViewById(R.id.tv_isbn);
        mItemISBN.setVisibility(View.VISIBLE);
          success = (TextView) findViewById(R.id.tv_success);
          failure = (TextView) findViewById(R.id.tv_failure);
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras!=null){
            lo = (LibraryObject) extras.getSerializable("key");
        }
        switch (lo.getType().toString()) {
            case "BOOK":
                //immagine libro


                Picasso.with(getApplicationContext()).load("http://covers.openlibrary.org/b/isbn/"+lo.getIsbn()+"-M.jpg?default=false").placeholder(R.drawable.ic_action_book).error(R.drawable.ic_action_book).into(mItemImage);

                break;
            case "FILM":
                MovieService.getInstance(getApplicationContext()).getMovie(lo.getIsbn(), new MovieService.Callback() {
                    @Override
                    public void onLoad(Movie movie) {
                        if (movie != null) {
                            Picasso.with(getApplicationContext()).load(movie.getPosterUrl()).placeholder(R.drawable.ic_action_movie).error(R.drawable.ic_action_movie).into(mItemImage);
                        }
                    }
                    @Override
                    public void onFailure() {
                        mItemImage.setImageResource(R.drawable.ic_action_movie);
                    }
                });

                break;
            case "MUSIC":
                mItemImage.setImageResource(R.drawable.ic_action_music_1);
                AlbumService.getInstance(getApplicationContext()).getAlbum(lo.getAuthor(), lo.getTitle(), new AlbumService.Callback() {
                    @Override
                    public void onLoad(Album album) {
                        if (album != null) {
                            List<Album.Image> list = album.getImages();
                            Log.d(TAG, album.getArtist());
                            for (Album.Image image : list) {
                                if (image.getSize().equals("medium")) {
                                    Picasso.with(getApplicationContext()).load(image.getText()).placeholder(R.drawable.ic_action_music_1).error(R.drawable.ic_action_music_1).into(mItemImage);
                                }
                            }

                        }
                    }


                    @Override
                    public void onFailure() {
                    }
                });

                break;
        }
        mItemISBN.setText(lo.getIsbn());
        mItemTitle.setText(lo.getTitle());
        mItemAuthor.setText(lo.getAuthor());
        mItemCategory.setText(lo.getCategory());

        btn_loan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ref2 = database.getReference("loans").orderByChild("libraryObjectId").equalTo(lo.getId());
                ref2.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        if (dataSnapshot.getValue()==null){
                            insertLoan();
                        }else {
                            for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                                Loan loan1 = childSnapshot.getValue(Loan.class);
                                Log.d(TAG, String.valueOf(loan1.getIdLoan()));
                                if (loan1.getUserId().equals(FirebaseAuth.getInstance().getCurrentUser().getEmail())){
                                    failure.setVisibility(View.VISIBLE);
                                    //prestito già richiesto dall'utente corrente
                                    break;
                                } else {
                                    Query ref3 = database.getReference("loans/" + loan1.getIdLoan()).child("start");
                                    ref3.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            Log.d(TAG, dataSnapshot.toString());
                                            if (Boolean.parseBoolean(dataSnapshot.getValue().toString())) {
                                                //oggetto in prestito da un altro utente
                                                Log.d(TAG, dataSnapshot.getValue().toString());
                                                failure.setVisibility(View.VISIBLE);
                                            } else {
                                                //oggetto non in prestito
                                                Log.d(TAG, "null");
                                                insertLoan();
                                            }
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {

                                        }
                                    });
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

            }
        });

    }

    private void insertLoan() {
        String id = String.valueOf((new GregorianCalendar()).getTimeInMillis());
        loan = new Loan(lo.getId(), FirebaseAuth.getInstance().getCurrentUser().getEmail(), id, lo.getType(), lo.getTitle(), lo.getIsbn(), lo.getAuthor());
        ref = database.getReference("loans");
        ref.child(id).setValue(loan);
        success.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onSupportNavigateUp() {
        Log.d(TAG,"onSupportNavigateUp()");
        finish();
        return true;
    }
}
